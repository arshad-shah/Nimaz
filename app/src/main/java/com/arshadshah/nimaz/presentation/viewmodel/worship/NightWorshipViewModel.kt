package com.arshadshah.nimaz.presentation.viewmodel.worship

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.domain.usecase.PrayerUseCases
import com.arshadshah.nimaz.core.di.DefaultDispatcher
import com.arshadshah.nimaz.core.time.TodayProvider
import com.arshadshah.nimaz.domain.model.PrayerCalculationSettings
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.core.monitoring.launchSafely
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Instant

@HiltViewModel
class NightWorshipViewModel @Inject constructor(
    private val prayerUseCases: PrayerUseCases,
    private val todayProvider: TodayProvider,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    private val telemetry: Telemetry,
) : ViewModel() {

    private fun refresh() {
        launchSafely(
            telemetry,
            AppAnalytics.Feature.NIGHT_WORSHIP,
            "refresh",
            onFailure = { throwable ->
                _state.update { it.copy(isLoading = false, error = throwable.message) }
            },
        ) {
            _state.update { it.copy(isLoading = true, error = null) }
            computeNightTimes(
                prayerUseCases.observeCalculationSettings().first(),
                todayProvider.today(),
            )
        }
    }

    private val _state = MutableStateFlow(NightWorshipUiState())
    val state: StateFlow<NightWorshipUiState> = _state.asStateFlow()

    init {
        observeNightTimes()
    }

    fun onEvent(event: NightWorshipEvent) {
        when (event) {
            // The screen's primary interaction, and it was unrecorded. That matters more here
            // than usual: the rakah count is deliberately **not persisted**, and this file's own
            // KDoc asks whether the counter is worth keeping. Usage is the only way to answer
            // that, and it was not being collected.
            NightWorshipEvent.AddRakahPair -> {
                telemetry.featureUsed(AppAnalytics.Feature.NIGHT_WORSHIP, "add_rakah_pair")
                _state.update { it.copy(rakahCount = it.rakahCount + 2) }
            }

            NightWorshipEvent.ResetRakahs -> {
                telemetry.featureUsed(AppAnalytics.Feature.NIGHT_WORSHIP, "reset_rakahs")
                _state.update { it.copy(rakahCount = 0) }
            }

            // Still handled, and now genuinely a refresh rather than the only way to get
            // current data: the settings and the day are observed, so this is the manual retry
            // the error state offers.
            NightWorshipEvent.Refresh -> refresh()
        }
    }

    /**
     * Resolve the night window that is *live right now*.
     *
     * The last third of the night straddles midnight: the Sunnah times for a
     * given date measures from that date's Maghrib to the *next* morning's Fajr, so its last-third
     * instant lands in the small hours of the following calendar day. That makes "which date owns
     * the current night" depend on the time of day:
     *
     *  - **Before today's Fajr** you are still inside the night that began *yesterday*. Its last
     *    third is happening *this* morning, and it closes at *today's* Fajr. Anchoring on `today`
     *    here — as the original code did — pointed the whole card at *tomorrow* night, so at 12:47am
     *    the open window read "opens in 23h". That was the reported time bug.
     *  - **After today's Fajr** tonight's window is the next one: it begins this evening and its last
     *    third lands tomorrow morning, closing at tomorrow's Fajr.
     *
     * So we pick the night's starting date from where `now` sits relative to today's Fajr, then read
     * the last third from that date and the closing Fajr from the morning after it.
     * The calculation behind these is Android-free and pure, so each date is one cheap pass.
     */
    /**
     * Recompute the live night whenever its inputs change: the user's calculation settings, and
     * the day itself.
     *
     * This ran once from `init` and read every setting with `.first()` — a one-shot snapshot.
     * `NightWorshipEvent.Refresh` existed and **no screen emitted it**, so:
     *
     *  - changing the calculation method or moving location left the card on the old city's
     *    times, because the ViewModel instance survives on the same back-stack entry and `init`
     *    does not run again;
     *  - keeping the app open past Fajr left the window on `CLOSED` for ever, with nothing able
     *    to advance it to the next night.
     *
     * `ARCHITECTURE.md` records this exact class of defect as resolved for `SettingsViewModel`,
     * and the documented fix there is the one applied here: **collect**, do not `.first()`.
     */
    private fun observeNightTimes() {
        launchSafely(
            telemetry,
            AppAnalytics.Feature.NIGHT_WORSHIP,
            "observe_night_times",
            onFailure = { throwable ->
                _state.update { it.copy(isLoading = false, error = throwable.message) }
            },
        ) {
            combine(
                prayerUseCases.observeCalculationSettings(),
                todayProvider.todayChanges,
            ) { settings, today -> settings to today }
                .collectLatest { (settings, today) -> computeNightTimes(settings, today) }
        }
    }

    /**
     * Resolve the night window that is *live right now*.
     *
     * The last third of the night straddles midnight: the Sunnah times for a given date measure
     * from that date's Maghrib to the *next* morning's Fajr, so its last-third instant lands in
     * the small hours of the following calendar day. That makes "which date owns the current
     * night" depend on the time of day:
     *
     *  - **Before today's Fajr** you are still inside the night that began *yesterday*. Its last
     *    third is happening *this* morning, and it closes at *today's* Fajr. Anchoring on `today`
     *    here — as the original code did — pointed the whole card at *tomorrow* night, so at
     *    12:47am the open window read "opens in 23h". That was the reported time bug.
     *  - **After today's Fajr** tonight's window is the next one: it begins this evening and its
     *    last third lands tomorrow morning, closing at tomorrow's Fajr.
     *
     * The calculation behind these is Android-free and pure, so each date is one cheap pass — but
     * three of them plus a settings read is still not main-thread work, hence the injected default dispatcher.
     */
    private suspend fun computeNightTimes(
        settings: PrayerCalculationSettings,
        today: LocalDate,
    ) {
        runCatching {
            withContext(defaultDispatcher) {
                val now = Clock.System.now()

                fun prayerOn(date: LocalDate, type: PrayerType): Instant? =
                    prayerUseCases.getDaySchedule(date, settings).find { it.type == type }?.time

                val fajrToday = prayerOn(today, PrayerType.FAJR)
                // Pre-dawn (before today's Fajr) → the live night began yesterday; otherwise it
                // is tonight's, still to come. This is the fix for the after-midnight
                // "opens in 23h" bug.
                val nightStart =
                    if (fajrToday != null && now < fajrToday) today.minusDays(1) else today

                val sunnah = prayerUseCases.getSunnahNightTimes(nightStart, settings)
                // Close the window at the Fajr that *ends* this night — the morning after it starts.
                val fajr =
                    if (nightStart == today) prayerOn(today.plusDays(1), PrayerType.FAJR)
                    else fajrToday

                sunnah.lastThirdOfTheNight to fajr
            }
        }.onSuccess { (lastThird, fajr) ->
            _state.update {
                it.copy(
                    isLoading = false,
                    lastThirdAt = lastThird,
                    fajrAt = fajr,
                    error = null,
                )
            }
        }.onFailure { e ->
            // `failure`, not a bare `recordException`: the stack trace reached Crashlytics
            // and the frequency reached nothing, so how often this fails was not visible.
            telemetry.failure(AppAnalytics.Feature.NIGHT_WORSHIP, "load", e)
            _state.update { it.copy(isLoading = false, error = e.message) }
        }
    }
}
