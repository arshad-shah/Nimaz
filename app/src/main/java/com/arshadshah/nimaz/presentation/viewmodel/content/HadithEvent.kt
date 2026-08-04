package com.arshadshah.nimaz.presentation.viewmodel.content

import com.arshadshah.nimaz.domain.model.HadithGrade

sealed interface HadithEvent {
    data class LoadBook(val bookId: String) : HadithEvent
    data class LoadChapter(val chapterId: String) : HadithEvent
    data class LoadHadithById(val hadithId: String) : HadithEvent
    data class LoadHadithByNumber(val bookId: String, val hadithNumber: Int) : HadithEvent
    data class FilterByGrade(val grade: HadithGrade) : HadithEvent
    data class ToggleBookmark(val hadithId: String, val bookId: String, val hadithNumber: Int) :
        HadithEvent

    data class NavigateToHadith(val index: Int) : HadithEvent
    data class SetFontSize(val size: Float) : HadithEvent
    data class SetArabicFontSize(val size: Float) : HadithEvent
    data object ToggleArabic : HadithEvent
    data object ClearSearch : HadithEvent
    data object LoadAllBooks : HadithEvent
    data object LoadBookmarks : HadithEvent
}
