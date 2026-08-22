package com.arshadshah.nimaz.presentation.viewmodel.tracker

import androidx.lifecycle.ViewModel
import com.arshadshah.nimaz.core.feedback.CounterFeedback
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.core.monitoring.catchAndReport
import com.arshadshah.nimaz.core.monitoring.launchSafely
import com.arshadshah.nimaz.domain.time.TodayProvider
import com.arshadshah.nimaz.core.common.toUtcMidnightMillis
import com.arshadshah.nimaz.domain.model.TasbihPreset
import com.arshadshah.nimaz.domain.model.TasbihSession
import com.arshadshah.nimaz.domain.repository.settings.TasbihSettings
import com.arshadshah.nimaz.domain.usecase.TasbihUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** How the counter is presented: the classic tap-circle or the tasbih beads. */
enum class TasbihCounterStyle { CLASSIC, BEADS }

@HiltViewModel
class TasbihViewModel @Inject constructor(
    private val tasbihUseCases: TasbihUseCases,
    private val tasbihSettings: TasbihSettings,
    private val feedback: CounterFeedback,
    private val todayProvider: TodayProvider,
    private val telemetry: Telemetry,
) : ViewModel() {

    private val _presetsState = MutableStateFlow(TasbihPresetsUiState())
    val presetsState: StateFlow<TasbihPresetsUiState> = _presetsState.asStateFlow()

    private val _counterState = MutableStateFlow(TasbihCounterUiState())
    val counterState: StateFlow<TasbihCounterUiState> = _counterState.asStateFlow()

    private val _historyState = MutableStateFlow(TasbihHistoryUiState())
    val historyState: StateFlow<TasbihHistoryUiState> = _historyState.asStateFlow()

    private val _statsState = MutableStateFlow(TasbihStatsUiState())
    val statsState: StateFlow<TasbihStatsUiState> = _statsState.asStateFlow()

    private var sessionStartTime: Long = 0

    /**
     * One handle per collector. `loadHistory()` is reachable from init, clearPreset,
     * selectPreset, completeSession and an event, and each call used to launch two
     * fresh Room collectors that were never cancelled. Four sessions in a sitting left
     * ten live collectors writing the same state — each having captured its **own**
     * `today`, so after midnight the oldest kept re-writing yesterday's sessions and
     * whichever emitted last won. ARCHITECTURE.md §4.1: one handle per identity.
     */
    private var historyTodayJob: Job? = null
    private var historyWeekJob: Job? = null
    private var presetsDefaultJob: Job? = null
    private var presetsCustomJob: Job? = null

    /**
     * Set synchronously before the session insert is launched.
     *
     * Two taps 20 ms apart both saw `currentSession == null` and both inserted a
     * session; the second's update hard-set `count = 1`, so the user tapped twice and
     * the counter read 1 — leaving an orphan row that inflated the totals.
     */
    private var startingSession = false

    /** Taps that landed while the session insert was still in flight. */
    private var pendingTaps = 0

    init {
        loadPresets()
        loadHistory()
        loadStats()
        checkForActiveSession()
        launchSafely(telemetry, DOMAIN, "launch") {
            tasbihSettings.tasbihBeadMode.collect { beads ->
                _counterState.update {
                    it.copy(counterStyle = if (beads) TasbihCounterStyle.BEADS else TasbihCounterStyle.CLASSIC)
                }
            }
        }
        launchSafely(telemetry, DOMAIN, "launch") {
            tasbihSettings.tasbihBeadDesign.collect { key ->
                _counterState.update { it.copy(beadDesignKey = key) }
            }
        }
        // Selection can be driven from the Choose-Dhikr screen (a separate VM
        // instance) via DataStore; keep this counter in sync with it.
        launchSafely(telemetry, DOMAIN, "launch") {
            tasbihSettings.tasbihSelectedPresetId.collect { id ->
                applyPersistedSelection(id)
            }
        }
        launchSafely(telemetry, DOMAIN, "launch") {
            tasbihSettings.tasbihFavorites.collect { ids ->
                _presetsState.update {
                    it.copy(favorites = ids.mapNotNull { s -> s.toLongOrNull() }.toSet())
                }
            }
        }
        launchSafely(telemetry, DOMAIN, "launch") {
            tasbihSettings.tasbihLeftHanded.collect { left ->
                _counterState.update { it.copy(leftHanded = left) }
            }
        }
        // Seed default adhkar added after the prepackaged DB shipped (one-time per version).
        launchSafely(telemetry, DOMAIN, "launch") {
            if (tasbihSettings.tasbihPresetSeedVersion.first() < LATEST_PRESET_SEED_VERSION) {
                tasbihUseCases.seedMissingDefaults()
                tasbihSettings.setTasbihPresetSeedVersion(LATEST_PRESET_SEED_VERSION)
            }
        }
    }

    fun onEvent(event: TasbihEvent) {
        when (event) {
            is TasbihEvent.SelectPreset -> {
                telemetry.featureUsed(DOMAIN, "select_preset")
                selectPreset(event.preset)
            }

            TasbihEvent.ClearPreset -> clearPreset()
            is TasbihEvent.SetTargetCount -> setTargetCount(event.count)
            is TasbihEvent.CreateCustomPreset -> {
                telemetry.featureUsed(DOMAIN, "preset_created")
                createCustomPreset(event.preset)
            }

            is TasbihEvent.UpdateCustomPreset -> {
                telemetry.featureUsed(DOMAIN, "preset_updated")
                updateCustomPreset(event.preset)
            }

            is TasbihEvent.DeleteCustomPreset -> {
                telemetry.featureUsed(DOMAIN, "preset_deleted")
                deleteCustomPreset(event.presetId)
            }

            is TasbihEvent.ToggleVibration -> {
                telemetry.settingChanged("tasbih_vibration", event.enabled.toString())
                _counterState.update { it.copy(vibrationEnabled = event.enabled) }
            }

            is TasbihEvent.ToggleSound -> {
                telemetry.settingChanged("tasbih_sound", event.enabled.toString())
                _counterState.update { it.copy(soundEnabled = event.enabled) }
            }
            // The counter style, bead design and handedness all write DataStore, so they are
            // settings and belong on the settings dashboard rather than in the feature's usage
            // counter — the bead design in particular is the one place the app asks the user
            // for a purely aesthetic preference, and nothing recorded which one they picked.
            is TasbihEvent.SetCounterStyle -> {
                telemetry.settingChanged("tasbih_counter_style", event.style.name)
                _counterState.update { it.copy(counterStyle = event.style) }
                launchSafely(telemetry, DOMAIN, "on_event") {
                    tasbihSettings.setTasbihBeadMode(event.style == TasbihCounterStyle.BEADS)
                }
            }

            is TasbihEvent.SetBeadDesign -> {
                telemetry.settingChanged("tasbih_bead_design", event.key)
                _counterState.update { it.copy(beadDesignKey = event.key) }
                launchSafely(telemetry, DOMAIN, "on_event") {
                    tasbihSettings.setTasbihBeadDesign(
                        event.key
                    )
                }
            }

            is TasbihEvent.ToggleFavorite -> {
                telemetry.featureUsed(DOMAIN, AppAnalytics.Action.TOGGLE_FAVORITE)
                toggleFavorite(event.presetId)
            }

            is TasbihEvent.SetLeftHanded -> {
                telemetry.settingChanged("tasbih_left_handed", event.enabled.toString())
                _counterState.update { it.copy(leftHanded = event.enabled) }
                launchSafely(telemetry, DOMAIN, "on_event") {
                    tasbihSettings.setTasbihLeftHanded(
                        event.enabled
                    )
                }
            }

            TasbihEvent.Increment -> increment()
            TasbihEvent.Reset -> {
                telemetry.featureUsed(DOMAIN, "reset")
                reset()
            }

            TasbihEvent.LoadPresets -> loadPresets()
            TasbihEvent.LoadHistory -> loadHistory()
            TasbihEvent.LoadStats -> loadStats()
        }
    }

    private fun loadPresets() {
        presetsDefaultJob?.cancel()
        presetsDefaultJob = launchSafely(telemetry, DOMAIN, "load_presets") {
            tasbihUseCases.getDefaultPresets()
                .catchAndReport(telemetry, DOMAIN, "load_presets") {
                    _presetsState.update { s -> s.copy(isLoading = false) }
                }
                .collect { defaults ->
                    _presetsState.update { it.copy(defaultPresets = defaults) }
                }
        }
        presetsCustomJob?.cancel()
        presetsCustomJob = launchSafely(telemetry, DOMAIN, "load_presets") {
            tasbihUseCases.getCustomPresets()
                .catchAndReport(telemetry, DOMAIN, "load_presets") {
                    _presetsState.update { s -> s.copy(isLoading = false) }
                }
                .collect { customs ->
                    _presetsState.update { it.copy(customPresets = customs, isLoading = false) }
                }
        }
    }

    private fun clearPreset() {
        val currentSession = _counterState.value.currentSession
        val currentCount =
            _counterState.value.count + (_counterState.value.laps * _counterState.value.targetCount)

        if (currentSession != null && currentCount > 0) {
            val completedAt = System.currentTimeMillis()
            val duration = completedAt - currentSession.startedAt

            launchSafely(telemetry, DOMAIN, "clear_preset") {
                tasbihUseCases.completeSession(currentSession.id, completedAt, duration)
                loadHistory()
                loadStats()
            }
        }

        _counterState.update {
            it.copy(
                selectedPreset = null,
                targetCount = 33,
                count = 0,
                laps = 0,
                currentSession = null,
                isActive = false,
            )
        }
        launchSafely(
            telemetry,
            DOMAIN,
            "clear_preset"
        ) { tasbihSettings.setTasbihSelectedPresetId(-1L) }
    }

    private fun toggleFavorite(id: Long) {
        launchSafely(telemetry, DOMAIN, "toggle_favorite") {
            val current = tasbihSettings.tasbihFavorites.first().toMutableSet()
            val key = id.toString()
            if (!current.add(key)) current.remove(key)
            tasbihSettings.setTasbihFavorites(current)
        }
    }

    /** Apply a selection persisted by another screen (idempotent — guarded by id). */
    private fun applyPersistedSelection(id: Long) {
        val current = _counterState.value.selectedPreset?.id
        if (id <= 0L) {
            if (current != null) clearPreset()
            return
        }
        if (current == id) return
        launchSafely(telemetry, DOMAIN, "apply_persisted_selection") {
            tasbihUseCases.getPresetById(id)?.let { selectPreset(it) }
        }
    }

    private fun setTargetCount(newTarget: Int) {
        val safeTarget = newTarget.coerceAtLeast(1)

        _counterState.update { state ->
            val currentCount = state.count
            var newCount = currentCount
            var newLaps = state.laps

            // If count >= new target and autoLap is on, convert overflow into laps
            if (state.autoLap && currentCount >= safeTarget) {
                val extraLaps = currentCount / safeTarget
                newLaps += extraLaps
                newCount = currentCount % safeTarget
            }

            state.copy(
                targetCount = safeTarget,
                count = newCount,
                laps = newLaps
            )
        }

        // Persist the updated state to the database
        _counterState.value.currentSession?.let { session ->
            launchSafely(telemetry, DOMAIN, "set_target_count") {
                val state = _counterState.value
                // Store the within-lap count; the DB sums currentCount + laps*target.
                tasbihUseCases.updateSessionCount(session.id, state.count, state.laps)
            }
        }
    }

    private fun selectPreset(preset: TasbihPreset) {
        // Auto-complete the current session if switching to a different preset with some count
        val currentSession = _counterState.value.currentSession
        val currentCount =
            _counterState.value.count + (_counterState.value.laps * _counterState.value.targetCount)

        if (currentSession != null && currentCount > 0 && currentSession.presetId != preset.id) {
            // Complete the current session before switching
            val completedAt = System.currentTimeMillis()
            val duration = completedAt - currentSession.startedAt

            launchSafely(telemetry, DOMAIN, "select_preset") {
                tasbihUseCases.completeSession(currentSession.id, completedAt, duration)
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
            )
        }
        launchSafely(telemetry, DOMAIN, "select_preset") {
            tasbihSettings.setTasbihSelectedPresetId(
                preset.id
            )
        }
    }

    private fun createCustomPreset(preset: TasbihPreset) {
        launchSafely(telemetry, DOMAIN, "create_custom_preset") {
            tasbihUseCases.insertPreset(preset)
        }
    }

    private fun updateCustomPreset(preset: TasbihPreset) {
        launchSafely(telemetry, DOMAIN, "update_custom_preset") {
            tasbihUseCases.updatePreset(preset)
        }
    }

    private fun deleteCustomPreset(presetId: Long) {
        launchSafely(telemetry, DOMAIN, "delete_custom_preset") {
            tasbihUseCases.deleteCustomPreset(presetId)
        }
    }

    private fun increment() {
        // Feedback first, so the tick stays in step with the finger.
        val state = _counterState.value
        feedback.tick(vibrate = state.vibrationEnabled, sound = state.soundEnabled)

        // Auto-start a session if none exists. The guard is checked and set in the same
        // synchronous block, so a second tap during the insert is counted rather than
        // starting a second session.
        if (state.currentSession == null) {
            if (startingSession) {
                pendingTaps++
            } else {
                startingSession = true
                startSessionAndIncrement()
            }
            return
        }

        val previousLaps = _counterState.value.laps

        // NOT logged per tap. #359 asks for `Increment` — "the single most-used action in the
        // feature" — to be instrumented, and instrumenting it literally would emit one event
        // per finger tap on a counter people run to 100 and beyond: the same firehose §4 of
        // that issue objects to on search keystrokes, at a worse rate. The unit that answers
        // "is the tasbih used" is a **completed lap**, logged below, and a started session,
        // logged in `startSessionAndIncrement`. Both are bounded by real activity.
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
            launchSafely(telemetry, DOMAIN, "increment") {
                // Store the within-lap count; the DB sums currentCount + laps*target.
                tasbihUseCases.updateSessionCount(
                    session.id,
                    _counterState.value.count,
                    _counterState.value.laps
                )

                // Refresh stats when a lap completes to update sessions count
                if (_counterState.value.laps > previousLaps) {
                    telemetry.featureUsed(DOMAIN, "lap_completed")
                    loadStats()
                }
            }
        }
    }

    private fun startSessionAndIncrement() {
        val preset = _counterState.value.selectedPreset

        telemetry.featureUsed(DOMAIN, "session_started")
        sessionStartTime = System.currentTimeMillis()

        launchSafely(telemetry, DOMAIN, "start_session_and_increment") {
            val session = TasbihSession(
                id = 0,
                presetId = preset?.id,
                // Null, not "Free Count": presetName is nullable all the way down and
                // the history screen already renders a localized fallback. Storing the
                // English string put untranslatable text in every user's database.
                presetName = preset?.name,
                date = getTodayEpoch(),
                currentCount = 1,
                targetCount = _counterState.value.targetCount,
                totalLaps = 0,
                isCompleted = false,
                duration = null,
                startedAt = sessionStartTime,
                completedAt = null,
                note = null
            )
            val sessionId = tasbihUseCases.insertSession(session)
            val insertedSession = tasbihUseCases.getSessionById(sessionId)

            // Taps that arrived while the insert was in flight are added rather than
            // discarded, so a fast double-tap reads 2 and not 1.
            val startCount = 1 + pendingTaps
            pendingTaps = 0
            startingSession = false

            _counterState.update {
                it.copy(
                    currentSession = insertedSession,
                    isActive = true,
                    count = startCount,
                    laps = 0,
                )
            }
            insertedSession?.let {
                tasbihUseCases.updateSessionCount(it.id, startCount, 0)
            }

            // Refresh stats to include the new session
            loadStats()
        }
    }

    private fun reset() {
        _counterState.update { it.copy(count = 0, laps = 0) }

        // Also update the session in the database if there's an active one
        _counterState.value.currentSession?.let { session ->
            launchSafely(telemetry, DOMAIN, "reset") {
                tasbihUseCases.updateSessionCount(session.id, 0, 0)
                loadStats()
            }
        }
    }

    private fun completeSession() {

        _counterState.value.currentSession?.let { session ->
            val completedAt = System.currentTimeMillis()
            val duration = completedAt - session.startedAt

            launchSafely(telemetry, DOMAIN, "complete_session") {
                tasbihUseCases.completeSession(session.id, completedAt, duration)

                _counterState.update {
                    it.copy(
                        currentSession = null,
                        isActive = false,
                        count = 0,
                        laps = 0,
                    )
                }

                loadHistory()
                loadStats()
            }
        }
    }

    private fun checkForActiveSession() {
        launchSafely(telemetry, DOMAIN, "check_for_active_session") {
            val activeSession = tasbihUseCases.getActiveSession()
            activeSession?.let { session ->
                val preset = session.presetId?.let { tasbihUseCases.getPresetById(it) }
                _counterState.update {
                    it.copy(
                        currentSession = session,
                        selectedPreset = preset,
                        // targetCount comes off a stored row, and `%` by 0 throws. Every
                        // writer coerces to >= 1 (see setTargetCount), but a legacy or
                        // imported row need not have, and this runs at init — so a single
                        // bad row would take the counter down on open rather than degrade.
                        count = session.currentCount % session.targetCount.coerceAtLeast(1),
                        laps = session.totalLaps,
                        targetCount = session.targetCount,
                        isActive = true,
                    )
                }
            }
        }
    }

    private fun loadHistory() {
        val today = getTodayEpoch()
        val weekAgo = today - MILLIS_PER_WEEK

        historyTodayJob?.cancel()
        historyTodayJob = launchSafely(telemetry, DOMAIN, "load_history") {
            tasbihUseCases.getSessionsForDate(today)
                .catchAndReport(telemetry, DOMAIN, "load_history")
                .collect { todaySessions ->
                    _historyState.update { it.copy(todaySessions = todaySessions) }
                }
        }
        historyWeekJob?.cancel()
        historyWeekJob = launchSafely(telemetry, DOMAIN, "load_history") {
            tasbihUseCases.getSessionsInRange(weekAgo, today + MILLIS_PER_DAY)
                .catchAndReport(telemetry, DOMAIN, "load_history") {
                    _historyState.update { s -> s.copy(isLoading = false) }
                }
                .collect { weekSessions ->
                    _historyState.update { it.copy(weekSessions = weekSessions, isLoading = false) }
                }
        }
    }

    private fun loadStats() {
        val today = getTodayEpoch()
        val weekAgo = today - (7 * 24 * 60 * 60 * 1000)
        val endOfToday = today + (24 * 60 * 60 * 1000)

        launchSafely(telemetry, DOMAIN, "load_stats") {
            val stats = tasbihUseCases.getTasbihStats(weekAgo, endOfToday)
            val totalToday = tasbihUseCases.getTotalCountInRange(today, endOfToday)
            val totalWeek = tasbihUseCases.getTotalCountInRange(weekAgo, endOfToday)
            val completedSessions =
                tasbihUseCases.getCompletedSessionsInRange(weekAgo, endOfToday)

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
        return todayProvider.today().toUtcMidnightMillis()
    }

    companion object {
        /** Bump when new default presets are added to DefaultTasbihPresets. */
        private const val LATEST_PRESET_SEED_VERSION = 1
        private const val DOMAIN = AppAnalytics.Feature.TASBIH
        private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
        private const val MILLIS_PER_WEEK = 7 * MILLIS_PER_DAY
    }

    override fun onCleared() {
        super.onCleared()
        feedback.release()
        // Note: Active session is preserved in database and can be resumed when user returns
    }
}
