package com.arshadshah.nimaz.presentation.viewmodel

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.domain.model.TasbihCategory
import com.arshadshah.nimaz.domain.model.TasbihPreset
import com.arshadshah.nimaz.domain.model.TasbihSession
import com.arshadshah.nimaz.domain.model.TasbihStats
import com.arshadshah.nimaz.domain.repository.TasbihRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

data class TasbihPresetsUiState(
    val defaultPresets: List<TasbihPreset> = emptyList(),
    val customPresets: List<TasbihPreset> = emptyList(),
    val selectedCategory: TasbihCategory? = null,
    val filteredPresets: List<TasbihPreset> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

data class TasbihCounterUiState(
    val selectedPreset: TasbihPreset? = null,
    val currentSession: TasbihSession? = null,
    val count: Int = 0,
    val laps: Int = 0,
    val targetCount: Int = 33,
    val isActive: Boolean = false,
    val elapsedTimeMs: Long = 0,
    val vibrationEnabled: Boolean = true,
    val soundEnabled: Boolean = false,
    val autoLap: Boolean = true
)

data class TasbihHistoryUiState(
    val todaySessions: List<TasbihSession> = emptyList(),
    val weekSessions: List<TasbihSession> = emptyList(),
    val isLoading: Boolean = true
)

data class TasbihStatsUiState(
    val stats: TasbihStats? = null,
    val totalToday: Int = 0,
    val baseTotalToday: Int = 0, // Total excluding current session, for real-time display
    val totalThisWeek: Int = 0,
    val completedSessions: Int = 0,
    val isLoading: Boolean = true
)

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

@HiltViewModel
class TasbihViewModel @Inject constructor(
    private val tasbihRepository: TasbihRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private var toneGenerator: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 50)  // 50% volume
    } catch (e: Exception) {
        null
    }

    private val _presetsState = MutableStateFlow(TasbihPresetsUiState())
    val presetsState: StateFlow<TasbihPresetsUiState> = _presetsState.asStateFlow()

    private val _counterState = MutableStateFlow(TasbihCounterUiState())
    val counterState: StateFlow<TasbihCounterUiState> = _counterState.asStateFlow()

    private val _historyState = MutableStateFlow(TasbihHistoryUiState())
    val historyState: StateFlow<TasbihHistoryUiState> = _historyState.asStateFlow()

    private val _statsState = MutableStateFlow(TasbihStatsUiState())
    val statsState: StateFlow<TasbihStatsUiState> = _statsState.asStateFlow()

    private var timerJob: Job? = null
    private var sessionStartTime: Long = 0

    init {
        loadPresets()
        loadHistory()
        loadStats()
        checkForActiveSession()
    }

    fun onEvent(event: TasbihEvent) {
        when (event) {
            is TasbihEvent.SelectPreset -> selectPreset(event.preset)
            is TasbihEvent.FilterByCategory -> filterByCategory(event.category)
            is TasbihEvent.SetTargetCount -> _counterState.update { it.copy(targetCount = event.count) }
            is TasbihEvent.CreateCustomPreset -> createCustomPreset(event.preset)
            is TasbihEvent.UpdateCustomPreset -> updateCustomPreset(event.preset)
            is TasbihEvent.DeleteCustomPreset -> deleteCustomPreset(event.presetId)
            is TasbihEvent.ToggleVibration -> _counterState.update { it.copy(vibrationEnabled = event.enabled) }
            is TasbihEvent.ToggleSound -> _counterState.update { it.copy(soundEnabled = event.enabled) }
            is TasbihEvent.ToggleAutoLap -> _counterState.update { it.copy(autoLap = event.enabled) }
            TasbihEvent.Increment -> increment()
            TasbihEvent.Reset -> reset()
            TasbihEvent.StartSession -> startSession()
            TasbihEvent.PauseSession -> pauseSession()
            TasbihEvent.ResumeSession -> resumeSession()
            TasbihEvent.CompleteSession -> completeSession()
            TasbihEvent.LoadPresets -> loadPresets()
            TasbihEvent.LoadHistory -> loadHistory()
            TasbihEvent.LoadStats -> loadStats()
        }
    }

    private fun loadPresets() {
        viewModelScope.launch {
            tasbihRepository.getDefaultPresets().collect { defaults ->
                _presetsState.update {
                    it.copy(defaultPresets = defaults, filteredPresets = defaults + it.customPresets)
                }
            }
        }
        viewModelScope.launch {
            tasbihRepository.getCustomPresets().collect { customs ->
                _presetsState.update {
                    it.copy(
                        customPresets = customs,
                        filteredPresets = it.defaultPresets + customs,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun selectPreset(preset: TasbihPreset) {
        // Auto-complete the current session if switching to a different preset with some count
        val currentSession = _counterState.value.currentSession
        val currentCount = _counterState.value.count + (_counterState.value.laps * _counterState.value.targetCount)

        if (currentSession != null && currentCount > 0 && currentSession.presetId != preset.id) {
            // Complete the current session before switching
            timerJob?.cancel()
            val completedAt = System.currentTimeMillis()
            val duration = completedAt - currentSession.startedAt

            viewModelScope.launch {
                tasbihRepository.completeSession(currentSession.id, completedAt, duration)
                loadHistory()
                loadStats()
            }
        }

        _counterState.update {
            it.copy(
                selectedPreset = preset,
                targetCount = preset.targetCount,
                count = 0,
                laps = 0,
                currentSession = null,
                isActive = false,
                elapsedTimeMs = 0
            )
        }
    }

    private fun filterByCategory(category: TasbihCategory?) {
        _presetsState.update { state ->
            val allPresets = state.defaultPresets + state.customPresets
            val filtered = if (category == null) {
                allPresets
            } else {
                allPresets.filter { it.category == category }
            }
            state.copy(selectedCategory = category, filteredPresets = filtered)
        }
    }

    private fun createCustomPreset(preset: TasbihPreset) {
        viewModelScope.launch {
            tasbihRepository.insertPreset(preset)
        }
    }

    private fun updateCustomPreset(preset: TasbihPreset) {
        viewModelScope.launch {
            tasbihRepository.updatePreset(preset)
        }
    }

    private fun deleteCustomPreset(presetId: Long) {
        viewModelScope.launch {
            tasbihRepository.deleteCustomPreset(presetId)
        }
    }

    private fun increment() {
        // Trigger feedback immediately for responsive feel
        triggerVibration()
        playClickSound()

        // Auto-start a session if none exists and a preset is selected
        if (_counterState.value.currentSession == null && _counterState.value.selectedPreset != null) {
            startSessionAndIncrement()
            return
        }

        val previousLaps = _counterState.value.laps

        _counterState.update { state ->
            var newCount = state.count + 1
            var newLaps = state.laps

            if (state.autoLap && newCount >= state.targetCount) {
                newLaps++
                newCount = 0
            }

            state.copy(count = newCount, laps = newLaps)
        }

        // Update session if active
        _counterState.value.currentSession?.let { session ->
            viewModelScope.launch {
                val totalCount = _counterState.value.count + (_counterState.value.laps * _counterState.value.targetCount)
                tasbihRepository.updateSessionCount(
                    session.id,
                    totalCount,
                    _counterState.value.laps
                )

                // Refresh stats when a lap completes to update sessions count
                if (_counterState.value.laps > previousLaps) {
                    loadStats()
                }
            }
        }
    }

    private fun startSessionAndIncrement() {
        val preset = _counterState.value.selectedPreset ?: return

        sessionStartTime = System.currentTimeMillis()

        viewModelScope.launch {
            val session = TasbihSession(
                id = 0,
                presetId = preset.id,
                presetName = preset.name,
                date = getTodayEpoch(),
                currentCount = 1, // Start with count of 1
                targetCount = _counterState.value.targetCount,
                totalLaps = 0,
                isCompleted = false,
                duration = null,
                startedAt = sessionStartTime,
                completedAt = null,
                note = null
            )
            val sessionId = tasbihRepository.insertSession(session)
            val insertedSession = tasbihRepository.getSessionById(sessionId)

            _counterState.update {
                it.copy(
                    currentSession = insertedSession,
                    isActive = true,
                    count = 1, // Start with count of 1
                    laps = 0,
                    elapsedTimeMs = 0
                )
            }

            // Refresh stats to include the new session
            loadStats()
            startTimer()
        }
    }

    private fun triggerVibration() {
        if (!_counterState.value.vibrationEnabled) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(30)
        }
    }

    private fun playClickSound() {
        if (!_counterState.value.soundEnabled) return
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 50)  // 50ms beep
    }

    private fun reset() {
        _counterState.update { it.copy(count = 0, laps = 0) }

        // Also update the session in the database if there's an active one
        _counterState.value.currentSession?.let { session ->
            viewModelScope.launch {
                tasbihRepository.updateSessionCount(session.id, 0, 0)
                loadStats()
            }
        }
    }

    private fun startSession() {
        val preset = _counterState.value.selectedPreset ?: return

        sessionStartTime = System.currentTimeMillis()

        viewModelScope.launch {
            val session = TasbihSession(
                id = 0,
                presetId = preset.id,
                presetName = preset.name,
                date = getTodayEpoch(),
                currentCount = 0,
                targetCount = _counterState.value.targetCount,
                totalLaps = 0,
                isCompleted = false,
                duration = null,
                startedAt = sessionStartTime,
                completedAt = null,
                note = null
            )
            val sessionId = tasbihRepository.insertSession(session)
            val insertedSession = tasbihRepository.getSessionById(sessionId)

            _counterState.update {
                it.copy(
                    currentSession = insertedSession,
                    isActive = true,
                    count = 0,
                    laps = 0,
                    elapsedTimeMs = 0
                )
            }

            // Refresh stats to include the new session
            loadStats()
            startTimer()
        }
    }

    private fun pauseSession() {
        timerJob?.cancel()
        _counterState.update { it.copy(isActive = false) }
    }

    private fun resumeSession() {
        _counterState.update { it.copy(isActive = true) }
        startTimer()
    }

    private fun completeSession() {
        timerJob?.cancel()

        _counterState.value.currentSession?.let { session ->
            val completedAt = System.currentTimeMillis()
            val duration = completedAt - session.startedAt

            viewModelScope.launch {
                tasbihRepository.completeSession(session.id, completedAt, duration)

                _counterState.update {
                    it.copy(
                        currentSession = null,
                        isActive = false,
                        count = 0,
                        laps = 0,
                        elapsedTimeMs = 0
                    )
                }

                loadHistory()
                loadStats()
            }
        }
    }

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (_counterState.value.isActive) {
                delay(1000)
                _counterState.update {
                    it.copy(elapsedTimeMs = it.elapsedTimeMs + 1000)
                }
            }
        }
    }

    private fun checkForActiveSession() {
        viewModelScope.launch {
            val activeSession = tasbihRepository.getActiveSession()
            activeSession?.let { session ->
                val preset = session.presetId?.let { tasbihRepository.getPresetById(it) }
                _counterState.update {
                    it.copy(
                        currentSession = session,
                        selectedPreset = preset,
                        count = session.currentCount % session.targetCount,
                        laps = session.totalLaps,
                        targetCount = session.targetCount,
                        isActive = true,
                        elapsedTimeMs = System.currentTimeMillis() - session.startedAt
                    )
                }
                startTimer()
            }
        }
    }

    private fun loadHistory() {
        val today = getTodayEpoch()
        val weekAgo = today - (7 * 24 * 60 * 60 * 1000)

        viewModelScope.launch {
            tasbihRepository.getSessionsForDate(today).collect { todaySessions ->
                _historyState.update { it.copy(todaySessions = todaySessions) }
            }
        }
        viewModelScope.launch {
            tasbihRepository.getSessionsInRange(weekAgo, today + (24 * 60 * 60 * 1000)).collect { weekSessions ->
                _historyState.update { it.copy(weekSessions = weekSessions, isLoading = false) }
            }
        }
    }

    private fun loadStats() {
        val today = getTodayEpoch()
        val weekAgo = today - (7 * 24 * 60 * 60 * 1000)
        val endOfToday = today + (24 * 60 * 60 * 1000)

        viewModelScope.launch {
            val stats = tasbihRepository.getTasbihStats(weekAgo, endOfToday)
            val totalToday = tasbihRepository.getTotalCountInRange(today, endOfToday)
            val totalWeek = tasbihRepository.getTotalCountInRange(weekAgo, endOfToday)
            val completedSessions = tasbihRepository.getCompletedSessionsInRange(weekAgo, endOfToday)

            // Calculate base total (excluding current session's count) for real-time display
            val currentSessionCount = _counterState.value.currentSession?.let {
                _counterState.value.count + (_counterState.value.laps * _counterState.value.targetCount)
            } ?: 0
            val baseTotalToday = (totalToday - currentSessionCount).coerceAtLeast(0)

            _statsState.update {
                it.copy(
                    stats = stats,
                    totalToday = totalToday,
                    baseTotalToday = baseTotalToday,
                    totalThisWeek = totalWeek,
                    completedSessions = completedSessions,
                    isLoading = false
                )
            }
        }
    }

    private fun getTodayEpoch(): Long {
        return LocalDate.now().atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        toneGenerator?.release()
        toneGenerator = null
        // Note: Active session is preserved in database and can be resumed when user returns
    }
}
