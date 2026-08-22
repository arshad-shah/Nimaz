package com.arshadshah.nimaz.presentation.viewmodel.settings

import com.arshadshah.nimaz.data.sync.ConnectionState
import com.arshadshah.nimaz.presentation.viewmodel.UiError

data class SyncUiState(
    val mode: SyncMode = SyncMode.NONE,
    val connectionState: ConnectionState = ConnectionState.Idle,
    val statusMessage: String = "",
    val currentStep: String = "",
    val stepsCompleted: Int = 0,
    val totalSteps: Int = 0,
    val transferProgress: Float = 0f,
    val dataSummary: SyncDataSummary? = null,
    val error: UiError? = null,
    val activityLog: List<ActivityLogEntry> = emptyList(),
)
