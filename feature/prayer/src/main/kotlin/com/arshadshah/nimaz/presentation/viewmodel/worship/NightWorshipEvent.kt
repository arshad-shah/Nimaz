package com.arshadshah.nimaz.presentation.viewmodel.worship

sealed interface NightWorshipEvent {
    /** Night prayer is offered in pairs, so the counter moves two at a time. */
    data object AddRakahPair : NightWorshipEvent
    data object ResetRakahs : NightWorshipEvent
    data object Refresh : NightWorshipEvent
}
