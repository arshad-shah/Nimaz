package com.arshadshah.nimaz.presentation.viewmodel.worship

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.core.util.PrayerTimeCalculator
import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.HighLatitudeRule
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.Telemetry
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

@HiltViewModel
class NightWorshipViewModel @Inject constructor(
    private val prayerTimeCalculator: PrayerTimeCalculator,
    private val settingsRepository: SettingsRepository,
    private val telemetry: Telemetry,
) : ViewModel() {

    private val _state = MutableStateFlow(NightWorshipUiState())
    val state: StateFlow<NightWorshipUiState> = _state.asStateFlow()

    init {
        loadNightTimes()
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
