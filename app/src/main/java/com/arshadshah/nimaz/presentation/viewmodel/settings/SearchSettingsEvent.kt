package com.arshadshah.nimaz.presentation.viewmodel.settings

import com.arshadshah.nimaz.domain.model.LibrarySource
import com.arshadshah.nimaz.domain.model.MatchStrictness

sealed interface SearchSettingsEvent {
    data class SetResultsPerSource(val count: Int) : SearchSettingsEvent

    /** Switch one source on or off. Switching off the last one is refused, not obeyed. */
    data class ToggleSource(val source: LibrarySource) : SearchSettingsEvent
    data class SetStrictness(val strictness: MatchStrictness) : SearchSettingsEvent

    /** `null` opens search unfiltered. */
    data class SetDefaultScope(val source: LibrarySource?) : SearchSettingsEvent

    /** Master toggle tapped. Enabling first opens the consent sheet. */
    data object ToggleAiRequested : SearchSettingsEvent
    data object ConsentAccepted : SearchSettingsEvent
    data object ConsentDismissed : SearchSettingsEvent
    data class SetHistoryEnabled(val enabled: Boolean) : SearchSettingsEvent
    data object ClearHistory : SearchSettingsEvent
}
