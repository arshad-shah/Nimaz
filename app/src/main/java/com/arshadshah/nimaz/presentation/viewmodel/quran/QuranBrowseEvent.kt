package com.arshadshah.nimaz.presentation.viewmodel.quran

sealed interface QuranBrowseEvent {
    data class QueryChanged(val text: String) : QuranBrowseEvent
    data object ClearQuery : QuranBrowseEvent
}
