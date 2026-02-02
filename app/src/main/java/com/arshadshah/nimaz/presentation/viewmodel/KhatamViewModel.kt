package com.arshadshah.nimaz.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.domain.model.DailyLogEntry
import com.arshadshah.nimaz.domain.model.JuzProgressInfo
import com.arshadshah.nimaz.domain.model.Khatam
import com.arshadshah.nimaz.domain.model.KhatamStatus
import com.arshadshah.nimaz.domain.usecase.KhatamUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KhatamListUiState(
    val activeKhatam: Khatam? = null,
    val inProgressKhatams: List<Khatam> = emptyList(),
    val completedKhatams: List<Khatam> = emptyList(),
    val abandonedKhatams: List<Khatam> = emptyList(),
    val isLoading: Boolean = true
)

data class KhatamDetailUiState(
    val khatam: Khatam? = null,
    val juzProgress: List<JuzProgressInfo> = emptyList(),
    val dailyLogs: List<DailyLogEntry> = emptyList(),
    val readAyahCount: Int = 0,
    val daysActive: Int = 0,
    val averagePace: Float = 0f,
    val nextUnreadSurah: Int? = null,
    val nextUnreadAyah: Int? = null,
    val isLoading: Boolean = true
)

data class CreateKhatamUiState(
    val name: String = "",
    val dailyTarget: Int = 20,
    val notes: String = "",
    val deadline: Long? = null,
    val isCreating: Boolean = false,
    val error: String? = null
)

sealed interface KhatamEvent {
    // List events
    data class SetActiveKhatam(val khatamId: Long) : KhatamEvent
    data class DeleteKhatam(val khatamId: Long) : KhatamEvent
    data class AbandonKhatam(val khatamId: Long) : KhatamEvent
    data class ReactivateKhatam(val khatamId: Long) : KhatamEvent

    // Detail events
    data class LoadKhatamDetail(val khatamId: Long) : KhatamEvent

    // Create events
    data class UpdateName(val name: String) : KhatamEvent
    data class UpdateDailyTarget(val target: Int) : KhatamEvent
    data class UpdateNotes(val notes: String) : KhatamEvent
    data class UpdateDeadline(val deadline: Long?) : KhatamEvent
    data object CreateKhatam : KhatamEvent
}

@HiltViewModel
class KhatamViewModel @Inject constructor(
    private val khatamUseCases: KhatamUseCases
) : ViewModel() {

    private val _listState = MutableStateFlow(KhatamListUiState())
    val listState: StateFlow<KhatamListUiState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow(KhatamDetailUiState())
    val detailState: StateFlow<KhatamDetailUiState> = _detailState.asStateFlow()

    private val _createState = MutableStateFlow(CreateKhatamUiState())
    val createState: StateFlow<CreateKhatamUiState> = _createState.asStateFlow()

    init {
        observeKhatams()
    }

    fun onEvent(event: KhatamEvent) {
        when (event) {
            is KhatamEvent.SetActiveKhatam -> setActiveKhatam(event.khatamId)
            is KhatamEvent.DeleteKhatam -> deleteKhatam(event.khatamId)
            is KhatamEvent.AbandonKhatam -> abandonKhatam(event.khatamId)
            is KhatamEvent.ReactivateKhatam -> reactivateKhatam(event.khatamId)
            is KhatamEvent.LoadKhatamDetail -> loadKhatamDetail(event.khatamId)
            is KhatamEvent.UpdateName -> _createState.update { it.copy(name = event.name) }
            is KhatamEvent.UpdateDailyTarget -> _createState.update { it.copy(dailyTarget = event.target) }
            is KhatamEvent.UpdateNotes -> _createState.update { it.copy(notes = event.notes) }
            is KhatamEvent.UpdateDeadline -> _createState.update { it.copy(deadline = event.deadline) }
            KhatamEvent.CreateKhatam -> createKhatam()
        }
    }

    private fun observeKhatams() {
        viewModelScope.launch {
            khatamUseCases.observeActiveKhatam().collect { active ->
                _listState.update { it.copy(activeKhatam = active) }
            }
        }
        viewModelScope.launch {
            khatamUseCases.observeInProgressKhatams().collect { list ->
                _listState.update { it.copy(inProgressKhatams = list, isLoading = false) }
            }
        }
        viewModelScope.launch {
            khatamUseCases.observeCompletedKhatams().collect { list ->
                _listState.update { it.copy(completedKhatams = list) }
            }
        }
        viewModelScope.launch {
            khatamUseCases.observeAbandonedKhatams().collect { list ->
                _listState.update { it.copy(abandonedKhatams = list) }
            }
        }
    }

    private fun setActiveKhatam(khatamId: Long) {
        viewModelScope.launch {
            khatamUseCases.setActiveKhatam(khatamId)
        }
    }

    private fun deleteKhatam(khatamId: Long) {
        viewModelScope.launch {
            khatamUseCases.deleteKhatam(khatamId)
        }
    }

    private fun abandonKhatam(khatamId: Long) {
        viewModelScope.launch {
            khatamUseCases.abandonKhatam(khatamId)
        }
    }

    private fun reactivateKhatam(khatamId: Long) {
        viewModelScope.launch {
            khatamUseCases.reactivateKhatam(khatamId)
        }
    }

    private fun loadKhatamDetail(khatamId: Long) {
        _detailState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            khatamUseCases.observeKhatamById(khatamId).collect { khatam ->
                if (khatam != null) {
                    val juzProgress = khatamUseCases.getJuzProgress(khatamId)
                    val daysActive = if (khatam.startedAt != null) {
                        val diffMs = System.currentTimeMillis() - khatam.startedAt
                        maxOf(1, (diffMs / (24 * 60 * 60 * 1000)).toInt())
                    } else 0
                    val avgPace = if (daysActive > 0) khatam.totalAyahsRead.toFloat() / daysActive else 0f
                    val nextUnread = khatamUseCases.getNextUnreadPosition(khatamId)

                    _detailState.update {
                        it.copy(
                            khatam = khatam,
                            juzProgress = juzProgress,
                            readAyahCount = khatam.totalAyahsRead,
                            daysActive = daysActive,
                            averagePace = avgPace,
                            nextUnreadSurah = nextUnread?.first,
                            nextUnreadAyah = nextUnread?.second,
                            isLoading = false
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            khatamUseCases.observeDailyLogs(khatamId).collect { logs ->
                _detailState.update { it.copy(dailyLogs = logs) }
            }
        }
    }

    private fun createKhatam() {
        val state = _createState.value
        if (state.name.isBlank()) {
            _createState.update { it.copy(error = "Name is required") }
            return
        }

        _createState.update { it.copy(isCreating = true, error = null) }

        viewModelScope.launch {
            val khatam = Khatam(
                name = state.name.trim(),
                dailyTarget = state.dailyTarget,
                notes = state.notes.ifBlank { null },
                deadline = state.deadline,
                status = KhatamStatus.ACTIVE,
                isActive = true
            )
            val id = khatamUseCases.createKhatam(khatam)
            khatamUseCases.setActiveKhatam(id)
            _createState.update { CreateKhatamUiState() } // Reset
        }
    }
}
