package com.arshadshah.nimaz.presentation.viewmodel.more

import androidx.lifecycle.ViewModel
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.core.monitoring.catchAndReport
import com.arshadshah.nimaz.core.monitoring.launchBestEffort
import com.arshadshah.nimaz.domain.time.TodayProvider
import com.arshadshah.nimaz.domain.calendar.HijriDateCalculator
import com.arshadshah.nimaz.domain.model.PinnedShortcut
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.WorshipReminderOccurrence
import com.arshadshah.nimaz.domain.model.ZakatDefaults
import com.arshadshah.nimaz.domain.model.ZakatHistoryEntry
import com.arshadshah.nimaz.domain.repository.settings.MoreSettings
import com.arshadshah.nimaz.domain.usecase.KhatamRowProgress
import com.arshadshah.nimaz.domain.usecase.MoreUseCases
import com.arshadshah.nimaz.domain.usecase.QaidaRowProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * More's first ViewModel.
 *
 * The screen had none, because it had nothing to report: every subtitle was a static
 * `stringResource` restating its own title. Making them true is therefore not a copy change but a
 * new presentation slice, and this is it.
 *
 * Four things about the shape are deliberate:
 *
 * **The day is a dependency.** Three subtitles are scoped to today, and one source
 * (`todayPrayerRecords`) resolves "today" at *call* time and returns a Flow bound to that epoch —
 * so collecting it once means More reports yesterday after midnight, which is the family of defects
 * #363 removed. Everything hangs off `todayProvider.todayChanges` through `flatMapLatest`, so
 * rollover **re-invokes** the queries rather than merely re-reading them.
 *
 * **A failing source costs one subtitle, not the screen.** Each source is wrapped in
 * `catchAndReport` with a neutral fallback *inside* the combine, so a broken makeup-fast query
 * leaves the fasting row bare and the other nineteen rows untouched. Applying it outside the
 * `flatMapLatest` would end the whole chain on the first transient error and never recover.
 * There is no `UiError` on the state for the same reason — see [MoreUiState] and spec §2.4.
 *
 * **`launchBestEffort`, not `launchSafely`.** More has nowhere to *show* a failure, so choosing the
 * explicitly-silent launcher puts that decision on the record instead of leaving a `launchSafely`
 * with a missing `onFailure`, which looks identical at the call site.
 *
 * **No `Context`, and the clock is injected.** Per #448/#441 and #363: the countdown to the next
 * worship window is measured against an injected `Clock`, so a test can decide what time it is.
 */
@HiltViewModel
class MoreViewModel @Inject constructor(
    private val useCases: MoreUseCases,
    private val moreSettings: MoreSettings,
    private val todayProvider: TodayProvider,
    private val clock: Clock,
    private val telemetry: Telemetry,
) : ViewModel() {

    private val _state = MutableStateFlow(MoreUiState())
    val state: StateFlow<MoreUiState> = _state.asStateFlow()

    init {
        observePins()
        observeReportedState()
        refreshNextWorship()
    }

    fun onEvent(event: MoreEvent) {
        when (event) {
            is MoreEvent.SetPins -> setPins(event.pins)
            MoreEvent.Refresh -> refreshNextWorship()
        }
    }

    private fun observePins() {
        launchBestEffort(telemetry, FEATURE, "observe_pins") {
            moreSettings.pinnedShortcuts.collectLatest { pins ->
                _state.update { it.copy(pinnedShortcuts = pins) }
            }
        }
    }

    private fun setPins(pins: List<PinnedShortcut>) {
        telemetry.featureUsed(FEATURE, "set_pins")
        launchBestEffort(telemetry, FEATURE, "set_pins") {
            moreSettings.setPinnedShortcuts(pins)
        }
    }

    /**
     * The flow-backed rows, re-armed at every rollover.
     *
     * Two nested combines rather than one: Kotlin's typed `combine` stops at five flows, and the
     * untyped vararg form needs every source to share a type, which these do not. Grouping them
     * as *what the tracker rows report* and *what the tools rows report* keeps each side readable.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeReportedState() {
        launchBestEffort(telemetry, FEATURE, "observe") {
            todayProvider.todayChanges.flatMapLatest { today ->
                val practice = combine(
                    useCases.todayPrayerRecords()
                        .catchAndReport(telemetry, FEATURE, "prayer_records") { emit(emptyMap()) },
                    useCases.pendingMakeupFasts()
                        .catchAndReport(telemetry, FEATURE, "makeup_fasts") { emit(emptyList()) },
                    useCases.khatamRowProgress()
                        .catchAndReport(telemetry, FEATURE, "khatam") { emit(null) },
                    useCases.qaidaRowProgress()
                        .catchAndReport(telemetry, FEATURE, "qaida") { emit(null) },
                ) { prayers, makeup, khatam, qaida ->
                    Practice(prayers, makeup.size, khatam, qaida)
                }
                val tools = combine(
                    useCases.zakatHistory()
                        .catchAndReport(telemetry, FEATURE, "zakat_history") { emit(emptyList()) },
                    useCases.zakatCurrency()
                        .catchAndReport(telemetry, FEATURE, "zakat_currency") { emit("") },
                    useCases.hijriDayOffset()
                        .catchAndReport(telemetry, FEATURE, "hijri_offset") { emit(0) },
                ) { history, currency, offset -> Tools(history, currency, offset) }

                combine(practice, tools) { p, t -> Reported(today, p, t) }
            }.collectLatest(::apply)
        }
    }

    private fun apply(reported: Reported) {
        val hijri = useCases.hijriToday(reported.today, reported.tools.hijriOffset)
        // Sunrise is in the record map and is not a prayer anyone logs, so it must not inflate the
        // denominator — "4 of 6 logged today" would be wrong every single day.
        val trackable = reported.practice.prayers.keys.count { it != PrayerName.SUNRISE }
        val logged = reported.practice.prayers
            .filterKeys { it != PrayerName.SUNRISE }
            .count { (_, status) -> status.isLogged() }
        _state.update {
            it.copy(
                prayersLogged = logged,
                prayersTrackable = trackable,
                pendingMakeupFasts = reported.practice.pendingMakeupFasts,
                khatamJuz = reported.practice.khatam?.juz,
                khatamDaysAgainstPace = reported.practice.khatam?.daysAgainstPace,
                qaidaLesson = reported.practice.qaida?.currentLesson,
                qaidaTotalLessons = reported.practice.qaida?.totalLessons,
                zakatHistoryLoaded = true,
                zakatDueThisYear = reported.tools.zakatHistory.dueInHijriYear(hijri.year),
                zakatCurrency = reported.tools.zakatCurrency.ifBlank { ZakatDefaults.CURRENCY },
                hijriToday = hijri.formatted(),
            )
        }
    }

    /**
     * Re-resolve the nearest worship reminder.
     *
     * Its own launch, not part of the combine: the resolver is a suspend computation over a dozen
     * settings, and folding it into a flow that re-emits whenever any other figure changes would
     * run it far more often than a menu subtitle is worth. The screen asks again on resume.
     */
    private fun refreshNextWorship() {
        launchBestEffort(telemetry, FEATURE, "next_worship") {
            val now = LocalDateTime.now(clock)
            val occurrence = useCases.nextWorship(now)
            _state.update {
                it.copy(
                    nextWorship = occurrence?.type,
                    minutesUntilNextWorship = occurrence?.minutesFrom(now),
                )
            }
        }
    }

    /** What the daily-practice rows report, in one emission. */
    private data class Practice(
        val prayers: Map<PrayerName, PrayerStatus>,
        val pendingMakeupFasts: Int,
        val khatam: KhatamRowProgress?,
        val qaida: QaidaRowProgress?,
    )

    /** What the tools rows report, in one emission. */
    private data class Tools(
        val zakatHistory: List<ZakatHistoryEntry>,
        val zakatCurrency: String,
        val hijriOffset: Int,
    )

    private data class Reported(
        val today: LocalDate,
        val practice: Practice,
        val tools: Tools,
    )

    private companion object {
        const val FEATURE = AppAnalytics.Feature.MORE
    }
}

/**
 * Whether a status counts as "logged".
 *
 * `PRAYED`, `LATE` and `QADA` are answers someone gave, and so is `MISSED` — it is a deliberate
 * record, not an absence. Only `PENDING` and `NOT_PRAYED` mean nothing has been said, which is what
 * the tracker row counts the absence of. An exhaustive `when` rather than a set membership test, so
 * a new status has to be classified here rather than silently counting as unlogged.
 */
private fun PrayerStatus.isLogged(): Boolean = when (this) {
    PrayerStatus.PRAYED, PrayerStatus.LATE, PrayerStatus.QADA, PrayerStatus.MISSED -> true
    PrayerStatus.PENDING, PrayerStatus.NOT_PRAYED -> false
}

/**
 * The zakat saved for Hijri year [hijriYear], or null if none was.
 *
 * Newest first by calculation time, so re-saving within a year reports the latest figure rather
 * than whichever row the query happened to return first.
 */
private fun List<ZakatHistoryEntry>.dueInHijriYear(hijriYear: Int): Double? =
    filter { HijriDateCalculator.toHijri(it.calculatedAt.toLocalDate()).year == hijriYear }
        .maxByOrNull { it.calculatedAt }
        ?.zakatDue

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(java.time.ZoneId.systemDefault()).toLocalDate()

/** Whole minutes from [now] until this occurrence's event; negative once its window has opened. */
private fun WorshipReminderOccurrence.minutesFrom(now: LocalDateTime): Long =
    Duration.between(now, eventAt).toMinutes()
