package com.arshadshah.nimaz.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.util.DiagnosticsCollector
import com.arshadshah.nimaz.domain.model.BugCategory
import com.arshadshah.nimaz.domain.model.BugDiagnostics
import com.arshadshah.nimaz.domain.model.BugReportSubmission
import com.arshadshah.nimaz.domain.repository.BugReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface BugSubmitStatus {
    data object Idle : BugSubmitStatus
    data object Submitting : BugSubmitStatus
    data class Success(val queuedOffline: Boolean) : BugSubmitStatus
    data object Error : BugSubmitStatus
}

data class BugReportUiState(
    val diagnostics: BugDiagnostics? = null,
    val status: BugSubmitStatus = BugSubmitStatus.Idle,
)

@HiltViewModel
class BugReportViewModel @Inject constructor(
    private val diagnosticsCollector: DiagnosticsCollector,
    private val bugReportRepository: BugReportRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BugReportUiState())
    val uiState: StateFlow<BugReportUiState> = _uiState.asStateFlow()

    init {
        loadDiagnostics()
    }

    private fun loadDiagnostics() {
        viewModelScope.launch {
            val diagnostics = runCatching { diagnosticsCollector.collect() }.getOrNull()
            _uiState.update { it.copy(diagnostics = diagnostics) }
        }
    }

    fun submit(
        category: BugCategory,
        description: String,
        stepsToReproduce: String,
        contactEmail: String,
        includeDiagnostics: Boolean,
        screenshotUri: Uri?,
    ) {
        if (_uiState.value.status == BugSubmitStatus.Submitting) return

        _uiState.update { it.copy(status = BugSubmitStatus.Submitting) }

        viewModelScope.launch {
            val submission = BugReportSubmission(
                category = category,
                description = description.trim(),
                stepsToReproduce = stepsToReproduce.trim(),
                contactEmail = contactEmail.trim(),
                diagnostics = if (includeDiagnostics) _uiState.value.diagnostics else null,
                screenshotUri = screenshotUri,
            )

            bugReportRepository.submit(submission)
                .onSuccess { result ->
                    AppAnalytics.logFeatureUsed("bug_report", "submitted")
                    _uiState.update {
                        it.copy(status = BugSubmitStatus.Success(result.queuedOffline))
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(status = BugSubmitStatus.Error) }
                }
        }
    }

    /** Resets a terminal error so the user can retry after fixing connectivity. */
    fun clearError() {
        if (_uiState.value.status == BugSubmitStatus.Error) {
            _uiState.update { it.copy(status = BugSubmitStatus.Idle) }
        }
    }
}
