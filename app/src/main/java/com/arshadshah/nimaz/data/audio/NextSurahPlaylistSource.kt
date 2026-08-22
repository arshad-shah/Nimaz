// The playlist item type belongs to QuranAudioManager, which is @UnstableApi because Media3 is.
// Same opt-in as QuranAudioService, and for the same reason: this file names that type.
@file:androidx.annotation.OptIn(UnstableApi::class)

package com.arshadshah.nimaz.data.audio

import com.arshadshah.nimaz.domain.repository.AyahAudioItem
import androidx.media3.common.util.UnstableApi
import com.arshadshah.nimaz.domain.repository.QuranRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What to play after a surah finishes.
 *
 * Continuous playback is described to the reader as playing "on to the next verse **and the next
 * surah**", and the player could only ever honour the first half of that: its playlist is one
 * surah's verses, so the end of the playlist was the end of the sitting. Carrying on needs the
 * next surah's verses, which the player has no way to ask for — hence this seam.
 *
 * Small on purpose, and shaped like [AyahAudioDownloader]: everything about *when* to advance
 * (the setting, the repeat mode, whether this playlist was a surah at all) stays in
 * [QuranAudioManager]. This only answers "what comes after 17?".
 */
interface NextSurahPlaylistSource {

    /**
     * The playlist for [surahNumber], or `null` when there is nothing to play — past the last
     * surah, or a surah whose verses are not in the database yet.
     */
    suspend fun playlistFor(surahNumber: Int): SurahPlaylist?

    /** A surah's recitation, ready to queue: what to call it and which files to fetch. */
    data class SurahPlaylist(
        val title: String,
        val items: List<AyahAudioItem>,
    )

    companion object {
        /** There is no 115th. */
        const val LAST_SURAH = 114
    }
}

@Singleton
class QuranNextSurahPlaylistSource @Inject constructor(
    private val quranRepository: QuranRepository,
) : NextSurahPlaylistSource {

    override suspend fun playlistFor(surahNumber: Int): NextSurahPlaylistSource.SurahPlaylist? {
        if (surahNumber !in 1..NextSurahPlaylistSource.LAST_SURAH) return null

        // Verses without a translation: the playlist needs ayah ids and nothing else, and asking
        // for the reader's translation here would join a table for text no one is going to read.
        val ayahs = quranRepository.getAyahsBySurah(surahNumber).first()
        if (ayahs.isEmpty()) return null

        val surah = quranRepository.getSurahByNumber(surahNumber)
        return NextSurahPlaylistSource.SurahPlaylist(
            title = surah?.nameEnglish.orEmpty(),
            items = ayahs.map { ayah ->
                AyahAudioItem(
                    ayahGlobalId = ayah.id,
                    surahNumber = ayah.surahNumber,
                    ayahNumber = ayah.ayahNumber,
                )
            },
        )
    }
}
