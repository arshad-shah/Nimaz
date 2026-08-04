package com.arshadshah.nimaz.presentation.viewmodel.quran

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.domain.model.DailyLogEntry
import com.arshadshah.nimaz.domain.model.JuzProgressInfo
import com.arshadshah.nimaz.domain.model.Khatam
import com.arshadshah.nimaz.domain.model.KhatamInsights
import com.arshadshah.nimaz.domain.model.KhatamStats
import com.arshadshah.nimaz.domain.model.KhatamStatus
import com.arshadshah.nimaz.domain.usecase.KhatamUseCases
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
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
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KhatamListUiState(
    val activeKhatam: Khatam? = null,
    val activeInsights: KhatamInsights? = null,
    val inProgressKhatams: List<Khatam> = emptyList(),
    val completedKhatams: List<Khatam> = emptyList(),
    val abandonedKhatams: List<Khatam> = emptyList(),
    val stats: KhatamStats? = null,
    val nextUnreadSurah: Int? = null,
    val nextUnreadAyah: Int? = null,
    val nextUnreadSurahName: String? = null,
    val isLoading: Boolean = true
) {
    val hasAnyKhatam: Boolean
        get() = inProgressKhatams.isNotEmpty() ||
                completedKhatams.isNotEmpty() ||
                abandonedKhatams.isNotEmpty()
}

data class KhatamDetailUiState(
    val khatam: Khatam? = null,
    val juzProgress: List<JuzProgressInfo> = emptyList(),
    val dailyLogs: List<DailyLogEntry> = emptyList(),
    val insights: KhatamInsights = KhatamInsights(),
    val nextUnreadSurah: Int? = null,
    val nextUnreadAyah: Int? = null,
    val nextUnreadSurahName: String? = null,
    val isLoading: Boolean = true,
    /** True once the khatam is known to be gone, so the screen can pop instead of spinning. */
    val notFound: Boolean = false
)

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

data class KhatamFormUiState(
    val mode: KhatamFormMode = KhatamFormMode.Create,
    val name: String = "",
    val dailyTarget: Int = DEFAULT_DAILY_TARGET,
    val preset: KhatamPacePreset = KhatamPacePreset.CUSTOM,
    val notes: String = "",
    val deadline: Long? = null,
    val reminderEnabled: Boolean = false,
    val reminderTime: String? = null,
    /** Ayahs already read — shown on edit so the reader can see progress is untouched. */
    val totalAyahsRead: Int = 0,
    /** Drives whether the overflow menu offers "set as active". */
    val isActiveKhatam: Boolean = false,
    val isSaving: Boolean = false,
    val isLoading: Boolean = false,
    /**
     * Validation failure as a resource id, not a resolved string: a ViewModel has no
     * Compose context, and a string resolved here would not follow a locale change.
     */
    @StringRes val errorRes: Int? = null,
    /** Set once the write has actually committed, so the screen navigates on success only. */
    val saveComplete: Boolean = false
) {
    val isEdit: Boolean get() = mode is KhatamFormMode.Edit

    /** Ayahs still to read — the whole Quran when creating. */
    val remainingAyahs: Int
        get() = (Khatam.TOTAL_QURAN_AYAHS - totalAyahsRead).coerceAtLeast(0)

    /** Days to finish the remainder at the chosen target. */
    val projectedDays: Int?
        get() = if (dailyTarget > 0 && remainingAyahs > 0) {
            Math.ceil(remainingAyahs.toDouble() / dailyTarget).toInt()
        } else null

    companion object {
        const val DEFAULT_DAILY_TARGET = 20
    }
}

sealed interface KhatamEvent {
    // List
    data class SetActiveKhatam(val khatamId: Long) : KhatamEvent
    data class DeleteKhatam(val khatamId: Long) : KhatamEvent
    data class AbandonKhatam(val khatamId: Long) : KhatamEvent
    data class ReactivateKhatam(val khatamId: Long) : KhatamEvent

    // Detail
    data class LoadKhatamDetail(val khatamId: Long) : KhatamEvent

    // Form (create + edit)
    data class StartCreate(val unit: Unit = Unit) : KhatamEvent
    data class StartEdit(val khatamId: Long) : KhatamEvent
    data class UpdateName(val name: String) : KhatamEvent
    data class UpdateDailyTarget(val target: Int) : KhatamEvent
    data class SelectPreset(val preset: KhatamPacePreset) : KhatamEvent
    data class UpdateNotes(val notes: String) : KhatamEvent
    data class UpdateDeadline(val deadline: Long?) : KhatamEvent
    data class UpdateReminderEnabled(val enabled: Boolean) : KhatamEvent
    data class UpdateReminderTime(val time: String?) : KhatamEvent
    data object SaveKhatam : KhatamEvent
    data object ConsumeSaveComplete : KhatamEvent
}

@HiltViewModel
class KhatamViewModel @Inject constructor(
    private val khatamUseCases: KhatamUseCases,
    // Only used to turn the next-unread surah number into its name for the continue label.
    private val quranUseCases: QuranUseCases
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
        viewModelScope.launch {
            khatamUseCases.observeKhatamStats().collect { stats ->
                _listState.update { it.copy(stats = stats) }
            }
        }

        // The active khatam's insights come from the detail snapshot so the list hero
        // shows exactly the same pace and streak numbers as the detail screen.
        viewModelScope.launch {
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
        viewModelScope.launch {
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
        viewModelScope.launch {
            val next = khatamUseCases.getNextUnreadPosition(khatamId)
            val name = next?.first?.let { surahName(it) }
            _detailState.update {
                if (it.khatam?.id != khatamId) it
                else it.copy(
                    nextUnreadSurah = next?.first,
                    nextUnreadAyah = next?.second,
                    nextUnreadSurahName = name,
                )
            }
        }
    }

    private fun refreshListNextUnread(khatamId: Long) {
        viewModelScope.launch {
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
        viewModelScope.launch {
            val khatam = runCatching {
                khatamUseCases.observeKhatamById(khatamId).filterNotNull().first()
            }.getOrNull()
            if (khatam == null) {
                _formState.update { it.copy(isLoading = false) }
                return@launch
            }
            _formState.value = KhatamFormUiState(
                mode = KhatamFormMode.Edit(khatamId),
                name = khatam.name,
                dailyTarget = khatam.dailyTarget,
                preset = KhatamPacePreset.forTarget(khatam.dailyTarget),
                notes = khatam.notes.orEmpty(),
                deadline = khatam.deadline,
                reminderEnabled = khatam.reminderEnabled,
                reminderTime = khatam.reminderTime,
                totalAyahsRead = khatam.totalAyahsRead,
                isActiveKhatam = khatam.isActive,
                isLoading = false
            )
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

        viewModelScope.launch {
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
        viewModelScope.launch { runCatching { block() } }
    }

    private companion object {
        const val MIN_DAILY_TARGET = 1
        const val MAX_DAILY_TARGET = 1000
    }
}
