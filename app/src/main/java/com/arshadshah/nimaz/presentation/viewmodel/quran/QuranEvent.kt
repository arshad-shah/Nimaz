@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.arshadshah.nimaz.presentation.viewmodel.quran

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
    data class Search(val query: String) : QuranEvent
    data class SetTopTab(val index: Int) : QuranEvent
    data class SetTab(val index: Int) : QuranEvent
    data class ToggleBookmark(val ayahId: Int, val surahNumber: Int, val ayahNumber: Int) :
        QuranEvent

    data class ToggleFavorite(val ayahId: Int, val surahNumber: Int, val ayahNumber: Int) :
        QuranEvent

    /** Remove a favourite from the Favourites tab; captures it for an Undo snackbar. */
    data class RemoveFavorite(val favorite: FavoriteAyahUi) : QuranEvent
    data object UndoRemoveFavorite : QuranEvent
    data object DismissFavoriteUndo : QuranEvent

    data class UpdateReadingPosition(val surah: Int, val ayah: Int, val page: Int, val juz: Int) :
        QuranEvent

    data object ToggleTranslation : QuranEvent
    data object ClearSearch : QuranEvent

    // Audio events
    data class PlayAyahAudio(val ayahGlobalId: Int, val surahNumber: Int, val ayahNumber: Int) :
        QuranEvent

    data object PauseAudio : QuranEvent
    data object ResumeAudio : QuranEvent
    data object StopAudio : QuranEvent
    data class PlaySurahFromInfo(val surahNumber: Int) : QuranEvent
    data class LoadSurahInfo(val surahNumber: Int) : QuranEvent
    data class ToggleKhatamAyah(val ayahId: Int) : QuranEvent
    data class MarkSurahAsReadForKhatam(val surahNumber: Int) : QuranEvent

    data class TogglePageKhatam(val ayahIds: List<Int>) : QuranEvent
}
