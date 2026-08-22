package com.arshadshah.nimaz.presentation.viewmodel.quran

sealed interface KhatamEvent {
    // List
    data class SetActiveKhatam(val khatamId: Long) : KhatamEvent
    data class DeleteKhatam(val khatamId: Long) : KhatamEvent
    data class AbandonKhatam(val khatamId: Long) : KhatamEvent
    data class ReactivateKhatam(val khatamId: Long) : KhatamEvent

    // Detail
    data class LoadKhatamDetail(val khatamId: Long) : KhatamEvent

    // Form (create + edit)
    data class StartCreate(val unit: Unit = Unit) : KhatamEvent
    data class StartEdit(val khatamId: Long) : KhatamEvent
    data class UpdateName(val name: String) : KhatamEvent
    data class UpdateDailyTarget(val target: Int) : KhatamEvent
    data class SelectPreset(val preset: KhatamPacePreset) : KhatamEvent
    data class UpdateNotes(val notes: String) : KhatamEvent
    data class UpdateDeadline(val deadline: Long?) : KhatamEvent
    data class UpdateReminderEnabled(val enabled: Boolean) : KhatamEvent
    data class UpdateReminderTime(val time: String?) : KhatamEvent
    data object SaveKhatam : KhatamEvent
    data object ConsumeSaveComplete : KhatamEvent
}
