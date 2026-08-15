package com.arshadshah.nimaz.presentation.viewmodel.quran

import androidx.lifecycle.ViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.core.monitoring.launchSafely
import com.arshadshah.nimaz.core.text.StringProvider
import com.arshadshah.nimaz.domain.model.Khatam
import com.arshadshah.nimaz.domain.model.KhatamStatus
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.usecase.KhatamUseCases
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import com.arshadshah.nimaz.domain.usecase.khatam.GetTodaysPortion
import com.arshadshah.nimaz.domain.usecase.khatam.KhatamPortion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** Whether the shared form is creating a new khatam or editing an existing one. */
sealed interface KhatamFormMode {
    data object Create : KhatamFormMode
    data class Edit(val khatamId: Long) : KhatamFormMode
}

/**
 * Preset reading paces, expressed as ayahs per day.
 *
 * Values are derived from [Khatam.TOTAL_QURAN_AYAHS] rather than hardcoded so they
 * stay correct if the constant ever changes.
 */
enum class KhatamPacePreset(val divisor: Int?) {
    /** One juz a day — finishes in about a month. */
    JUZ_DAILY(Khatam.TOTAL_JUZ),

    /** Half a juz a day — about two months. */
    HALF_JUZ_DAILY(Khatam.TOTAL_JUZ * 2),

    /** A quarter juz a day — about four months. */
    QUARTER_JUZ_DAILY(Khatam.TOTAL_JUZ * 4),

    /** Whatever the reader dials in by hand. */
    CUSTOM(null);

    fun targetAyahs(): Int? = divisor?.let {
        Math.ceil(Khatam.TOTAL_QURAN_AYAHS.toDouble() / it).toInt()
    }

    companion object {
        /** The preset matching an exact target, or [CUSTOM] when none does. */
        fun forTarget(target: Int): KhatamPacePreset =
            entries.firstOrNull { it.targetAyahs() == target } ?: CUSTOM
    }
}

@HiltViewModel
class KhatamViewModel @Inject constructor(
    private val khatamUseCases: KhatamUseCases,
    // The surah list, for the next-unread name and for turning a portion's global ayah ids
    // back into "Al-Kahf 18:1 → An-Nur 24:12".
    private val quranUseCases: QuranUseCases,
    private val getTodaysPortion: GetTodaysPortion,
    private val strings: StringProvider,
    private val telemetry: Telemetry,
) : ViewModel() {

    private val _listState = MutableStateFlow(KhatamListUiState())
    val listState: StateFlow<KhatamListUiState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow(KhatamDetailUiState())
    val detailState: StateFlow<KhatamDetailUiState> = _detailState.asStateFlow()

    private val _formState = MutableStateFlow(KhatamFormUiState())
    val formState: StateFlow<KhatamFormUiState> = _formState.asStateFlow()

    /**
     * The khatam the detail screen is showing. Held as state rather than captured in a
     * coroutine so switching khatams cancels the previous observation via [flatMapLatest]
     * instead of stacking a second, never-cancelled collector onto the same state.
     */
    private val detailKhatamId = MutableStateFlow<Long?>(null)

    init {
        observeKhatams()
        observeDetail()
    }

    fun onEvent(event: KhatamEvent) {
        logAnalytics(event)
        when (event) {
            is KhatamEvent.SetActiveKhatam -> setActiveKhatam(event.khatamId)
            is KhatamEvent.DeleteKhatam -> launchAction { khatamUseCases.deleteKhatam(event.khatamId) }
            is KhatamEvent.AbandonKhatam -> launchAction { khatamUseCases.abandonKhatam(event.khatamId) }
            is KhatamEvent.ReactivateKhatam -> launchAction { khatamUseCases.reactivateKhatam(event.khatamId) }
            is KhatamEvent.LoadKhatamDetail -> detailKhatamId.value = event.khatamId
            is KhatamEvent.StartCreate -> _formState.value = KhatamFormUiState()
            is KhatamEvent.StartEdit -> startEdit(event.khatamId)
            is KhatamEvent.UpdateName ->
                _formState.update { it.copy(name = event.name, errorRes = null) }

            is KhatamEvent.UpdateDailyTarget -> _formState.update {
                it.copy(
                    dailyTarget = event.target.coerceIn(MIN_DAILY_TARGET, MAX_DAILY_TARGET),
                    preset = KhatamPacePreset.forTarget(event.target)
                )
            }

            is KhatamEvent.SelectPreset -> _formState.update { state ->
                val target = event.preset.targetAyahs() ?: state.dailyTarget
                state.copy(preset = event.preset, dailyTarget = target)
            }

            is KhatamEvent.UpdateNotes -> _formState.update { it.copy(notes = event.notes) }
            is KhatamEvent.UpdateDeadline -> _formState.update { it.copy(deadline = event.deadline) }
            is KhatamEvent.UpdateReminderEnabled ->
                _formState.update { it.copy(reminderEnabled = event.enabled) }

            is KhatamEvent.UpdateReminderTime ->
                _formState.update { it.copy(reminderTime = event.time) }

            KhatamEvent.SaveKhatam -> saveKhatam()
            KhatamEvent.ConsumeSaveComplete ->
                _formState.update { it.copy(saveComplete = false) }
        }
    }

    private fun logAnalytics(event: KhatamEvent) {
        val action = when (event) {
            is KhatamEvent.SetActiveKhatam -> "set_active"
            is KhatamEvent.DeleteKhatam -> "delete"
            is KhatamEvent.AbandonKhatam -> "abandon"
            is KhatamEvent.ReactivateKhatam -> "reactivate"
            is KhatamEvent.LoadKhatamDetail -> "open_detail"
            is KhatamEvent.StartEdit -> "open_edit"
            KhatamEvent.SaveKhatam -> "save"
            else -> null
        }
        action?.let { AppAnalytics.logFeatureUsed(AppAnalytics.Feature.KHATAM, it) }
    }

    private fun observeKhatams() {
        launchSafely(telemetry, AppAnalytics.Feature.KHATAM, "observe_khatams") {
            khatamUseCases.observeInProgressKhatams().collect { list ->
                _listState.update { it.copy(inProgressKhatams = list, isLoading = false) }
            }
        }
        launchSafely(telemetry, AppAnalytics.Feature.KHATAM, "observe_khatams") {
            khatamUseCases.observeCompletedKhatams().collect { list ->
                _listState.update { it.copy(completedKhatams = list) }
            }
        }
        launchSafely(telemetry, AppAnalytics.Feature.KHATAM, "observe_khatams") {
            khatamUseCases.observeAbandonedKhatams().collect { list ->
                _listState.update { it.copy(abandonedKhatams = list) }
            }
        }
        launchSafely(telemetry, AppAnalytics.Feature.KHATAM, "observe_khatams") {
            khatamUseCases.observeKhatamStats().collect { stats ->
                _listState.update { it.copy(stats = stats) }
            }
        }

        // The active khatam's insights come from the detail snapshot so the list hero
        // shows exactly the same pace and streak numbers as the detail screen.
        launchSafely(telemetry, AppAnalytics.Feature.KHATAM, "observe_khatams") {
            khatamUseCases.observeActiveKhatam()
                .flatMapLatest { active ->
                    if (active == null) {
                        flowOf(null)
                    } else {
                        khatamUseCases.observeKhatamDetail(active.id)
                    }
                }
                .collect { snapshot ->
                    _listState.update {
                        it.copy(
                            activeKhatam = snapshot?.khatam,
                            activeInsights = snapshot?.insights
                        )
                    }
                    snapshot?.khatam?.let { refreshListNextUnread(it.id) }
                }
        }
    }

    /**
     * Switching khatam cancels the previous snapshot subscription. The old code launched
     * a fresh, never-cancelled collector per call and left `isLoading` stuck true when the
     * khatam was null, so a deleted khatam spun forever.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeDetail() {
        launchSafely(telemetry, AppAnalytics.Feature.KHATAM, "observe_detail") {
            detailKhatamId
                .filterNotNull()
                .flatMapLatest { id -> khatamUseCases.observeKhatamDetail(id) }
                .collect { snapshot ->
                    if (snapshot == null) {
                        _detailState.update {
                            it.copy(isLoading = false, notFound = true, khatam = null)
                        }
                    } else {
                        _detailState.update {
                            it.copy(
                                khatam = snapshot.khatam,
                                juzProgress = snapshot.juzProgress,
                                dailyLogs = snapshot.dailyLogs,
                                insights = snapshot.insights,
                                isLoading = false,
                                notFound = false
                            )
                        }
                        refreshDetailNextUnread(snapshot.khatam.id)
                    }
                }
        }
    }

    /**
     * The next unread position needs a join against the ayah table, so it stays a
     * one-shot query refreshed whenever the khatam's progress changes.
     */
    private fun refreshDetailNextUnread(khatamId: Long) {
        launchSafely(telemetry, AppAnalytics.Feature.KHATAM, "refresh_detail_next_unread") {
            val next = khatamUseCases.getNextUnreadPosition(khatamId)
            val name = next?.first?.let { surahName(it) }
            val surahs = runCatching { quranUseCases.getSurahList().first() }.getOrDefault(
                emptyList()
            )
            val startId = next?.let { globalAyahId(surahs, it.first, it.second) }
            _detailState.update { state ->
                if (state.khatam?.id != khatamId) state
                else {
                    val portion = state.khatam.let { getTodaysPortion(it, startId) }
                    state.copy(
                        nextUnreadSurah = next?.first,
                        nextUnreadAyah = next?.second,
                        nextUnreadSurahName = name,
                        todaysPortion = portion,
                        todaysPortionLabel = portion?.let { portionLabel(surahs, it) },
                    )
                }
            }
        }
    }

    private fun refreshListNextUnread(khatamId: Long) {
        launchSafely(telemetry, AppAnalytics.Feature.KHATAM, "refresh_list_next_unread") {
            val next = khatamUseCases.getNextUnreadPosition(khatamId)
            val name = next?.first?.let { surahName(it) }
            _listState.update {
                if (it.activeKhatam?.id != khatamId) it
                else it.copy(
                    nextUnreadSurah = next?.first,
                    nextUnreadAyah = next?.second,
                    nextUnreadSurahName = name,
                )
            }
        }
    }

    private suspend fun surahName(surahNumber: Int): String? =
        runCatching { quranUseCases.getSurahByNumber(surahNumber)?.nameEnglish }.getOrNull()

    /**
     * The global id of a verse, from the cumulative verse counts.
     *
     * `getNextUnreadPosition` speaks surah/ayah and [GetTodaysPortion] speaks global ids,
     * because a portion is a span of the *book* and crosses surah boundaries as a matter of
     * course. This is the one place the two meet.
     */
    private fun globalAyahId(surahs: List<Surah>, surahNumber: Int, ayahNumber: Int): Int =
        surahs.asSequence().filter { it.number < surahNumber }.sumOf { it.ayahCount } + ayahNumber

    /** "Al-Kahf 18:1 → An-Nur 24:12", or the one surah's name when it does not cross one. */
    private fun portionLabel(surahs: List<Surah>, portion: KhatamPortion): String? {
        if (surahs.isEmpty()) return null
        val from = positionOf(surahs, portion.fromAyahId) ?: return null
        val to = positionOf(surahs, portion.toAyahId) ?: return null
        val fromName = surahs.firstOrNull { it.number == from.first }?.nameEnglish ?: return null
        val toName = surahs.firstOrNull { it.number == to.first }?.nameEnglish ?: return null
        return if (from.first == to.first) {
            strings.get(R.string.khatam_portion_within, fromName, from.second, to.second)
        } else {
            strings.get(
                R.string.khatam_portion_across,
                fromName, from.second, toName, to.second
            )
        }
    }

    /** The surah and verse a global id names, or null when it is past the end of the book. */
    private fun positionOf(surahs: List<Surah>, ayahId: Int): Pair<Int, Int>? {
        var remaining = ayahId
        for (surah in surahs.sortedBy { it.number }) {
            if (remaining <= surah.ayahCount) return surah.number to remaining
            remaining -= surah.ayahCount
        }
        return null
    }

    private fun setActiveKhatam(khatamId: Long) = launchAction {
        khatamUseCases.setActiveKhatam(khatamId)
        // Reflect it immediately in the form so the menu entry disappears; the list and
        // detail screens pick the change up from their own Flows.
        _formState.update {
            if ((it.mode as? KhatamFormMode.Edit)?.khatamId == khatamId) {
                it.copy(isActiveKhatam = true)
            } else it
        }
    }

    private fun startEdit(khatamId: Long) {
        _formState.value = KhatamFormUiState(
            mode = KhatamFormMode.Edit(khatamId),
            isLoading = true
        )
        launchSafely(
            telemetry, AppAnalytics.Feature.KHATAM, "start_edit",
            // Without this, a failed read left the edit form on its spinner for good: the
            // only `isLoading = false` was inside the block, past the throw.
            onFailure = { _formState.update { it.copy(isLoading = false) } },
        ) {
            val khatam = runCatching {
                khatamUseCases.observeKhatamById(khatamId).filterNotNull().first()
            }.getOrNull()
            if (khatam == null) {
                _formState.update { it.copy(isLoading = false) }
                return@launchSafely
            }
            _formState.update { form ->
                // A whole-object assign here threw away whatever the reader had typed while the
                // first Room read was in flight: open Edit on a cold start, type "Ramadan 1447"
                // into the name, and the load landing afterwards reverted it to the stored name.
                //
                // A field the reader has not touched is still at its blank default, so that is
                // what decides. Untouched fields take the stored value; anything they have
                // already changed is theirs and stays. The read-only facts below are not
                // editable at all, so they always come from the record.
                if ((form.mode as? KhatamFormMode.Edit)?.khatamId != khatamId) return@update form
                val blank = KhatamFormUiState()
                val dailyTarget =
                    if (form.dailyTarget == blank.dailyTarget) khatam.dailyTarget
                    else form.dailyTarget
                form.copy(
                    name = if (form.name == blank.name) khatam.name else form.name,
                    dailyTarget = dailyTarget,
                    preset = if (form.preset == blank.preset) {
                        KhatamPacePreset.forTarget(khatam.dailyTarget)
                    } else {
                        form.preset
                    },
                    notes = if (form.notes == blank.notes) khatam.notes.orEmpty() else form.notes,
                    deadline = if (form.deadline == blank.deadline) {
                        khatam.deadline
                    } else {
                        form.deadline
                    },
                    reminderEnabled = if (form.reminderEnabled == blank.reminderEnabled) {
                        khatam.reminderEnabled
                    } else {
                        form.reminderEnabled
                    },
                    reminderTime = if (form.reminderTime == blank.reminderTime) {
                        khatam.reminderTime
                    } else {
                        form.reminderTime
                    },
                    totalAyahsRead = khatam.totalAyahsRead,
                    isActiveKhatam = khatam.isActive,
                    isLoading = false
                )
            }
        }
    }

    /**
     * Creates or updates, then flags completion only after the write returns — the
     * previous create screen navigated back optimistically, before the insert committed.
     */
    private fun saveKhatam() {
        val state = _formState.value
        if (state.name.isBlank()) {
            _formState.update { it.copy(errorRes = R.string.khatam_error_name_required) }
            return
        }

        _formState.update { it.copy(isSaving = true, errorRes = null) }

        launchSafely(telemetry, AppAnalytics.Feature.KHATAM, "save_khatam") {
            runCatching {
                when (val mode = state.mode) {
                    is KhatamFormMode.Create -> {
                        val id = khatamUseCases.createKhatam(
                            Khatam(
                                name = state.name.trim(),
                                dailyTarget = state.dailyTarget,
                                notes = state.notes.ifBlank { null },
                                deadline = state.deadline,
                                reminderEnabled = state.reminderEnabled,
                                reminderTime = state.reminderTime,
                                status = KhatamStatus.ACTIVE,
                                isActive = true
                            )
                        )
                        khatamUseCases.setActiveKhatam(id)
                    }

                    is KhatamFormMode.Edit -> {
                        val existing = khatamUseCases.observeKhatamById(mode.khatamId)
                            .filterNotNull()
                            .first()
                        // Progress fields are deliberately carried over untouched:
                        // editing a khatam must never alter what has been read.
                        khatamUseCases.updateKhatam(
                            existing.copy(
                                name = state.name.trim(),
                                dailyTarget = state.dailyTarget,
                                notes = state.notes.ifBlank { null },
                                deadline = state.deadline,
                                reminderEnabled = state.reminderEnabled,
                                reminderTime = state.reminderTime
                            )
                        )
                    }
                }
            }.onSuccess {
                _formState.update { it.copy(isSaving = false, saveComplete = true) }
            }.onFailure { error ->
                // Was "khatam_save" — a second domain for one feature, which split its
                // dashboards. The operation moves to the `type` where it belongs.
                AppAnalytics.logError(
                    AppAnalytics.Feature.KHATAM,
                    "save",
                    error.message
                )
                _formState.update {
                    it.copy(isSaving = false, errorRes = R.string.khatam_error_save_failed)
                }
            }
        }
    }

    private fun launchAction(block: suspend () -> Unit) {
        launchSafely(
            telemetry,
            AppAnalytics.Feature.KHATAM,
            "launch_action"
        ) { runCatching { block() } }
    }

    private companion object {
        const val MIN_DAILY_TARGET = 1
        const val MAX_DAILY_TARGET = 1000
    }
}
