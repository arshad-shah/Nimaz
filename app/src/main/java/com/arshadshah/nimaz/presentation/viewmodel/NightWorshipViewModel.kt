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
import kotlin.time.Instant

/**
 * State for the night worship hub.
 *
 * Deliberately **free of "now"**: it publishes the night's *instants* and lets the screen derive
 * every countdown and open/closed judgement at the leaf from the shared ticker
 * (`rememberNow`). This is the same rule the Home rework established — a ViewModel that pushes
 * elapsed time as state is what produced the frozen countdowns in the first place.
 *
 * @param lastThirdAt when the last third of the night begins (adhan2 `SunnahTimes`).
 * @param fajrAt tomorrow's Fajr — the instant the night window closes.
 * @param ishaAt tonight's Isha, the earliest sensible start for Witr.
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
     * Resolve tonight's window.
     *
     * The last third and Isha come from *today*, but Fajr must come from **tomorrow**: after
     * midnight the night in progress belongs to the previous calendar day, and taking today's Fajr
     * would close the window at a time that has already passed and render the hub permanently
     * "shut". [PrayerTimeCalculator] is Android-free and pure, so this is one astronomical pass.
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
                val sunnah = prayerTimeCalculator.getSunnahTimes(
                    latitude, longitude, today, method, asr, highLat
                )
                val isha = prayerTimeCalculator
                    .getPrayerTimes(latitude, longitude, today, method, asr, highLat)
                    .find { it.type == PrayerType.ISHA }?.time
                val fajr = prayerTimeCalculator
                    .getPrayerTimes(latitude, longitude, today.plusDays(1), method, asr, highLat)
                    .find { it.type == PrayerType.FAJR }?.time

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
