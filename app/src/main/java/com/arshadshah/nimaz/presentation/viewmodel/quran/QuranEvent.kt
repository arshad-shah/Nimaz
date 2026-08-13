@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.arshadshah.nimaz.presentation.viewmodel.quran

import com.arshadshah.nimaz.domain.model.RecitationRepeat
import com.arshadshah.nimaz.domain.model.RecitationSpeed

sealed interface QuranEvent {
    data class LoadSurah(val surahNumber: Int) : QuranEvent
    data class LoadJuz(val juzNumber: Int) : QuranEvent
    data class LoadPage(val pageNumber: Int) : QuranEvent

    /**
     * Fetch a page's ayahs into [QuranReaderUiState.pageCache] without making it the page the
     * reader is *on*. The pager keeps neighbouring pages composed so a swipe lands on content
     * that is already there; those neighbours must not retitle the reader or move the saved
     * reading position, which is what [LoadPage] does.
     */
    data class PrefetchPage(val pageNumber: Int) : QuranEvent

    /** Load the line-accurate 16-line IndoPak layout for a page (used by the 16-line view). */
    data class LoadMushafPageLayout(val pageNumber: Int) : QuranEvent
    data class ToggleBookmark(val ayahId: Int, val surahNumber: Int, val ayahNumber: Int) :
        QuranEvent

    data class ToggleFavorite(val ayahId: Int, val surahNumber: Int, val ayahNumber: Int) :
        QuranEvent

    data class UpdateReadingPosition(val surah: Int, val ayah: Int, val page: Int, val juz: Int) :
        QuranEvent

    data object ToggleTranslation : QuranEvent

    // Audio events
    data class PlayAyahAudio(val ayahGlobalId: Int, val surahNumber: Int, val ayahNumber: Int) :
        QuranEvent

    /**
     * Play the opening ayah in [reciterId] so the user can hear a reciter before choosing
     * them, without changing the persisted selection.
     *
     * The reciter screen used to reach through the ViewModel and call
     * `quranViewModel.audioManager.setReciter(...)` itself. That is the UI mutating the audio
     * engine directly — the exact coupling `ARCHITECTURE.md` §9 permits for *observing*
     * `audioState` and not for driving it, and it is untestable from a ViewModel test.
     */
    data class PreviewReciter(val reciterId: String) : QuranEvent

    /** Scrub to a position in the *whole surah*, which is what the seek rail measures. */
    data class SeekAudioTo(val positionMs: Long) : QuranEvent
    data object NextAyahAudio : QuranEvent
    data object PreviousAyahAudio : QuranEvent

    /** Repeat a verse N times, a range, or the whole surah. */
    data class SetRecitationRepeat(val repeat: RecitationRepeat) : QuranEvent

    /** 0.75x / 1x / 1.25x / 1.5x. Not persisted — it belongs to the sitting. */
    data class SetPlaybackSpeed(val speed: RecitationSpeed) : QuranEvent

    /** Whether the reader scrolls (or turns pages) to keep the recited verse visible. */
    data class SetFollowAlong(val enabled: Boolean) : QuranEvent

    data object PauseAudio : QuranEvent
    data object ResumeAudio : QuranEvent
    data object StopAudio : QuranEvent
    data class PlaySurahFromInfo(val surahNumber: Int) : QuranEvent
    data class LoadSurahInfo(val surahNumber: Int) : QuranEvent
    data class ToggleKhatamAyah(val ayahId: Int) : QuranEvent
    data class MarkSurahAsReadForKhatam(val surahNumber: Int) : QuranEvent

    data class TogglePageKhatam(val ayahIds: List<Int>) : QuranEvent
}
