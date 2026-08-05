package com.arshadshah.nimaz.presentation.viewmodel.settings

sealed interface SearchSettingsEvent {
    /** Master toggle tapped. Enabling first opens the consent sheet. */
    data object ToggleAiRequested : SearchSettingsEvent
    data object ConsentAccepted : SearchSettingsEvent
    data object ConsentDismissed : SearchSettingsEvent
    data class SetHistoryEnabled(val enabled: Boolean) : SearchSettingsEvent
    data object ClearHistory : SearchSettingsEvent
}
