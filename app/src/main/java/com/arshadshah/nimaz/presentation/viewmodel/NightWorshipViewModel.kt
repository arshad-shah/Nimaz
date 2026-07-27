package com.arshadshah.nimaz.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.core.util.PrayerTimeCalculator
import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.HighLatitudeRule
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * State for the night worship hub.
 *
 * Deliberately **free of "now"**: it publishes the night's *instants* and lets the screen derive
 * every countdown and open/closed judgement at the leaf from the shared ticker
 * (`rememberNow`). This is the same rule the Home rework established — a ViewModel that pushes
 * elapsed time as state is what produced the frozen countdowns in the first place.
 *
 * @param lastThirdAt when the last third of the *live* night begins (adhan2 `SunnahTimes`).
 * @param fajrAt the Fajr that closes the live night — the morning after it began, which is *today's*
 *   Fajr in the pre-dawn hours and *tomorrow's* once today's Fajr has passed.
 * @param ishaAt the live night's Isha, the earliest sensible start for Witr.
 * @param rakahCount in-memory tally for the current visit. Not persisted: we have no data on how
 *   people actually use this yet, and inventing a "completed night" model before that would be
 *   guessing. If the count turns out to be worth keeping, that is an easy follow-up.
 */
data class NightWorshipUiState(
    val isLoading: Boolean = true,
    val lastThirdAt: Instant? = null,
    val fajrAt: Instant? = null,
    val ishaAt: Instant? = null,
    val rakahCount: Int = 0,
    val error: String? = null,
)

sealed interface NightWorshipEvent {
    /** Night prayer is offered in pairs, so the counter moves two at a time. */
    data object AddRakahPair : NightWorshipEvent
    data object ResetRakahs : NightWorshipEvent
    data object Refresh : NightWorshipEvent
}

@HiltViewModel
class NightWorshipViewModel @Inject constructor(
    private val prayerTimeCalculator: PrayerTimeCalculator,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(NightWorshipUiState())
    val state: StateFlow<NightWorshipUiState> = _state.asStateFlow()

    init {
        loadNightTimes()
    }

    fun onEvent(event: NightWorshipEvent) {
        when (event) {
            NightWorshipEvent.AddRakahPair -> _state.update { it.copy(rakahCount = it.rakahCount + 2) }
            NightWorshipEvent.ResetRakahs -> _state.update { it.copy(rakahCount = 0) }
            NightWorshipEvent.Refresh -> loadNightTimes()
        }
    }

    /**
     * Resolve the night window that is *live right now*.
     *
     * The last third of the night straddles midnight: [PrayerTimeCalculator.getSunnahTimes] for a
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
     * [PrayerTimeCalculator] is Android-free and pure, so each date is one cheap astronomical pass.
     */
    private fun loadNightTimes() {
        viewModelScope.launch {
            runCatching {
                val latitude = settingsRepository.latitude.first()
                val longitude = settingsRepository.longitude.first()
                val method = CalculationMethod.fromString(settingsRepository.calculationMethod.first())
                val asr = AsrCalculation.fromString(settingsRepository.asrCalculation.first())
                val highLat = HighLatitudeRule.fromString(settingsRepository.highLatitudeRule.first())

                val today = LocalDate.now()
                val now = Clock.System.now()

                fun prayerOn(date: LocalDate, type: PrayerType): Instant? = prayerTimeCalculator
                    .getPrayerTimes(latitude, longitude, date, method, asr, highLat)
                    .find { it.type == type }?.time

                val fajrToday = prayerOn(today, PrayerType.FAJR)
                // Pre-dawn (before today's Fajr) → the live night began yesterday; otherwise it is
                // tonight's, still to come. This is the fix for the after-midnight "opens in 23h" bug.
                val nightStart = if (fajrToday != null && now < fajrToday) today.minusDays(1) else today

                val sunnah = prayerTimeCalculator.getSunnahTimes(
                    latitude, longitude, nightStart, method, asr, highLat
                )
                // Close the window at the Fajr that *ends* this night — the morning after it starts.
                val fajr = if (nightStart == today) prayerOn(today.plusDays(1), PrayerType.FAJR) else fajrToday
                val isha = prayerOn(nightStart, PrayerType.ISHA)

                Triple(sunnah.lastThirdOfTheNight, fajr, isha)
            }.onSuccess { (lastThird, fajr, isha) ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        lastThirdAt = lastThird,
                        fajrAt = fajr,
                        ishaAt = isha,
                        error = null,
                    )
                }
            }.onFailure { e ->
                CrashReporter.recordException(e)
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
