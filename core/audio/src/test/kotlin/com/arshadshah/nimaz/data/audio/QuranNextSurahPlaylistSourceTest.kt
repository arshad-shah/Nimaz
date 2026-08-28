package com.arshadshah.nimaz.data.audio

import androidx.media3.common.util.UnstableApi
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.repository.QuranRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * What continuous playback rolls into.
 *
 * The seam exists because `QuranAudioManager`'s playlist is one surah's verses, so the end of
 * the playlist was the end of the sitting — the reader was told the app plays "on to the next
 * verse and the next surah" and it only ever did the first half.
 *
 * All three refusals matter and none of them is visible from the player: past the last surah,
 * a surah whose verses are not on the device yet, and a number that is not a surah at all.
 */
@UnstableApi
class QuranNextSurahPlaylistSourceTest {

    private val repository: QuranRepository = mockk(relaxed = true)
    private val source = QuranNextSurahPlaylistSource(repository)

    private fun ayah(id: Int, surah: Int, number: Int) = Ayah(
        id = id,
        surahNumber = surah,
        ayahNumber = number,
        textArabic = "نص",
        textSimple = "nas",
        juzNumber = 1,
        hizbNumber = 1,
        rubNumber = 1,
        pageNumber = 1,
        sajdaType = null,
        sajdaNumber = null,
    )

    private fun surah(number: Int, name: String) = Surah(
        number = number,
        nameArabic = "سورة",
        nameEnglish = name,
        nameTransliteration = name,
        revelationType = RevelationType.MECCAN,
        ayahCount = 3,
        orderInMushaf = number,
    )

    @Test
    fun `a surah on the device becomes a playlist of its verses, in order`() = runTest {
        every { repository.getAyahsBySurah(19) } returns flowOf(
            listOf(ayah(2141, 19, 1), ayah(2142, 19, 2), ayah(2143, 19, 3))
        )
        coEvery { repository.getSurahByNumber(19) } returns surah(19, "Maryam")

        val playlist = source.playlistFor(19)!!

        assertThat(playlist.title).isEqualTo("Maryam")
        assertThat(playlist.items.map { it.ayahGlobalId }).containsExactly(2141, 2142, 2143).inOrder()
        assertThat(playlist.items.map { it.surahNumber }.distinct()).containsExactly(19)
    }

    @Test
    fun `a surah whose verses are not on the device yields nothing rather than an empty player`() {
        // Better than a playlist of zero items: the manager treats null as "the sitting ends
        // here" and finishes cleanly instead of leaving a live-looking notification over a
        // player with nothing in it.
        runTest {
            every { repository.getAyahsBySurah(19) } returns flowOf(emptyList())

            assertThat(source.playlistFor(19)).isNull()
        }
    }

    @Test
    fun `there is no surah after An-Nas`() = runTest {
        assertThat(source.playlistFor(NextSurahPlaylistSource.LAST_SURAH + 1)).isNull()
    }

    @Test
    fun `a number that is not a surah is refused before the database is asked`() = runTest {
        assertThat(source.playlistFor(0)).isNull()
        assertThat(source.playlistFor(-3)).isNull()
    }

    @Test
    fun `a surah with verses but no row in the surah table still plays, unnamed`() {
        // The verses are what the player needs; the title is decoration. Refusing here would
        // stop playback over a missing label.
        runTest {
            every { repository.getAyahsBySurah(19) } returns flowOf(listOf(ayah(2141, 19, 1)))
            coEvery { repository.getSurahByNumber(19) } returns null

            val playlist = source.playlistFor(19)!!

            assertThat(playlist.title).isEmpty()
            assertThat(playlist.items).hasSize(1)
        }
    }

    @Test
    fun `the last surah itself is a perfectly good playlist`() = runTest {
        every { repository.getAyahsBySurah(NextSurahPlaylistSource.LAST_SURAH) } returns
            flowOf(listOf(ayah(6231, 114, 1)))
        coEvery { repository.getSurahByNumber(NextSurahPlaylistSource.LAST_SURAH) } returns
            surah(114, "An-Nas")

        assertThat(source.playlistFor(NextSurahPlaylistSource.LAST_SURAH)).isNotNull()
    }
}
