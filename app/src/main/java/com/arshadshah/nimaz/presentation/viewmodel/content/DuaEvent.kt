package com.arshadshah.nimaz.presentation.viewmodel.content

import com.arshadshah.nimaz.domain.model.DuaOccasion

sealed interface DuaEvent {
    data class LoadCategory(val categoryId: String) : DuaEvent
    data class LoadDua(val duaId: String) : DuaEvent
    data class LoadDuasByOccasion(val occasion: DuaOccasion) : DuaEvent
    data class ToggleFavorite(val duaId: String, val categoryId: String) : DuaEvent
    data class SetFontSize(val size: Float) : DuaEvent
    data class SetArabicFontSize(val size: Float) : DuaEvent
    data class LoadProgressForDate(val date: Long) : DuaEvent
    data object ToggleArabic : DuaEvent
    data object ToggleTransliteration : DuaEvent
    data object ToggleTranslation : DuaEvent
    data object ToggleCategoriesSort : DuaEvent
    data object ClearSearch : DuaEvent
    data object LoadAllCategories : DuaEvent
    data object LoadFavorites : DuaEvent
    data object LoadTodayProgress : DuaEvent
}
