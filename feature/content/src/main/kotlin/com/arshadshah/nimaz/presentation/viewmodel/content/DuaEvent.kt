package com.arshadshah.nimaz.presentation.viewmodel.content

import com.arshadshah.nimaz.domain.model.Dua
import com.arshadshah.nimaz.domain.model.DuaOccasion

sealed interface DuaEvent {
    data class LoadCategory(val categoryId: String) : DuaEvent
    data class LoadDua(val duaId: String) : DuaEvent
    data class LoadDuasByOccasion(val occasion: DuaOccasion) : DuaEvent
    data class ToggleFavorite(val duaId: String, val categoryId: String) : DuaEvent
    data class SetFontSize(val size: Float) : DuaEvent
    data class SetArabicFontSize(val size: Float) : DuaEvent
    data class LoadProgressForDate(val date: Long) : DuaEvent

    /**
     * "Add to tasbih" from the dua reader — saves the dua as a custom tasbih preset.
     *
     * The screen used to do this by calling `TasbihViewModel.onEvent(CreateCustomPreset(...))`
     * through a `hiltViewModel()` of its own, which is a cross-feature reach: `viewmodel/tracker`
     * becomes `:feature:tracker` in PR 18 of #551, and `moduleBoundary` forbids the edge. It also
     * never held the tasbih screen's instance — `hiltViewModel()` scopes to the destination's
     * `NavBackStackEntry` — and only worked because the operation is a fire-and-forget write.
     *
     * Writing goes through this feature's own ViewModel and `TasbihUseCases` instead, which is
     * what rule 2 asks for anyway.
     */
    data class AddToTasbih(val dua: Dua) : DuaEvent
    data object ToggleArabic : DuaEvent
    data object ToggleTransliteration : DuaEvent
    data object ToggleTranslation : DuaEvent
    data object ToggleCategoriesSort : DuaEvent
    data object ClearSearch : DuaEvent
    data object LoadAllCategories : DuaEvent
    data object LoadFavorites : DuaEvent
    data object LoadTodayProgress : DuaEvent
}
