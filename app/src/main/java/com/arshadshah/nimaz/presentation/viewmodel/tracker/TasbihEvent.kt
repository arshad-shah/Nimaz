package com.arshadshah.nimaz.presentation.viewmodel.tracker

import com.arshadshah.nimaz.domain.model.TasbihCategory
import com.arshadshah.nimaz.domain.model.TasbihPreset

sealed interface TasbihEvent {
    data class SelectPreset(val preset: TasbihPreset) : TasbihEvent
    data class FilterByCategory(val category: TasbihCategory?) : TasbihEvent
    data class SetTargetCount(val count: Int) : TasbihEvent
    data class CreateCustomPreset(val preset: TasbihPreset) : TasbihEvent
    data class UpdateCustomPreset(val preset: TasbihPreset) : TasbihEvent
    data class DeleteCustomPreset(val presetId: Long) : TasbihEvent
    data class ToggleVibration(val enabled: Boolean) : TasbihEvent
    data class ToggleSound(val enabled: Boolean) : TasbihEvent
    data class ToggleAutoLap(val enabled: Boolean) : TasbihEvent
    data class SetCounterStyle(val style: TasbihCounterStyle) : TasbihEvent
    data class SetBeadDesign(val key: String) : TasbihEvent
    data class ToggleFavorite(val presetId: Long) : TasbihEvent
    data class SetLeftHanded(val enabled: Boolean) : TasbihEvent
    data object ClearPreset : TasbihEvent
    data object Increment : TasbihEvent
    data object Reset : TasbihEvent
    data object StartSession : TasbihEvent
    data object PauseSession : TasbihEvent
    data object ResumeSession : TasbihEvent
    data object CompleteSession : TasbihEvent
    data object LoadPresets : TasbihEvent
    data object LoadHistory : TasbihEvent
    data object LoadStats : TasbihEvent
}
