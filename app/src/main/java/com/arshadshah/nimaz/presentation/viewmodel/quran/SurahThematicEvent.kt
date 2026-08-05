package com.arshadshah.nimaz.presentation.viewmodel.quran

sealed interface SurahThematicEvent {
    /** Load a surah's background and outline. Idempotent — safe to re-send. */
    data class Load(val surahNumber: Int) : SurahThematicEvent

    data class Filter(val query: String) : SurahThematicEvent
    data object ClearFilter : SurahThematicEvent
}
