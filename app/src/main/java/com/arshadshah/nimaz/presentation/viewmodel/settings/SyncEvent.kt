package com.arshadshah.nimaz.presentation.viewmodel.settings

sealed interface SyncEvent {
    data object StartSend : SyncEvent
    data object StartReceive : SyncEvent
    data class AcceptConnection(val endpointId: String) : SyncEvent
    data class RejectConnection(val endpointId: String) : SyncEvent
    data object Cancel : SyncEvent
}
