package com.arshadshah.nimaz.presentation.viewmodel.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.core.monitoring.catchAndReport
import com.arshadshah.nimaz.core.monitoring.launchSafely
import com.arshadshah.nimaz.domain.model.NisabType
import com.arshadshah.nimaz.domain.repository.settings.ZakatSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * The zakat basis, edited on its own screen.
 *
 * Its own ViewModel rather than a slice of `SettingsViewModel` for the same reason
 * `SearchSettingsViewModel` is: there is already a feature-scoped seam ([ZakatSettings], six
 * members) that says exactly what this screen may touch, and reaching for the 179-member
 * `SettingsRepository` to write four preferences would give the screen the whole app's
 * configuration as collateral.
 *
 * `ZakatViewModel` observes the same four preferences, so a change here reaches an open
 * calculator through DataStore — there is no shared state between the two ViewModels and none
 * is needed. Nothing is held locally and written back later: every value on screen is whatever
 * the flows last emitted, which is the only way a failed write can be visible at all.
 */
@HiltViewModel
class ZakatSettingsViewModel @Inject constructor(
    private val zakatSettings: ZakatSettings,
    private val telemetry: Telemetry,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ZakatSettingsUiState())
    val uiState: StateFlow<ZakatSettingsUiState> = _uiState.asStateFlow()

    init {
        combine(
            zakatSettings.zakatNisabType,
            zakatSettings.zakatGoldPricePerGram,
            zakatSettings.zakatSilverPricePerGram,
            zakatSettings.zakatCurrency,
        ) { nisab, gold, silver, currency ->
            ZakatSettingsUiState(
                // An unknown name — an older build's sync payload, say — lands on the
                // default basis rather than throwing on a settings screen.
                nisabType = NisabType.fromName(nisab),
                goldPricePerGram = gold,
                silverPricePerGram = silver,
                currency = currency,
            )
        }
            .onEach { next -> _uiState.update { next } }
            .catchAndReport(telemetry, DOMAIN, "observe_settings")
            .launchIn(viewModelScope)
    }

    fun onEvent(event: ZakatSettingsEvent) {
        when (event) {
            is ZakatSettingsEvent.SetNisabType -> write("nisab_type", event.nisabType.name) {
                zakatSettings.setZakatNisabType(event.nisabType.name)
            }

            // The *value* of a price is not logged — a gold rate is a market fact, but paired
            // with the amounts on the calculator it is somebody's finances. Only the fact that
            // the setting was edited is recorded.
            is ZakatSettingsEvent.SetGoldPrice -> write("gold_price", EDITED) {
                zakatSettings.setZakatGoldPricePerGram(event.pricePerGram)
            }

            is ZakatSettingsEvent.SetSilverPrice -> write("silver_price", EDITED) {
                zakatSettings.setZakatSilverPricePerGram(event.pricePerGram)
            }

            is ZakatSettingsEvent.SetCurrency -> write("currency", event.currency) {
                zakatSettings.setZakatCurrency(event.currency)
            }
        }
    }

    /**
     * Persists one preference and records that it changed.
     *
     * The write is reported to telemetry on failure and otherwise left alone: the screen shows
     * what the flows emit, so a write that did not land shows the old value back — which is the
     * truth, and a better report than a toast over a field that already says the right thing.
     */
    private fun write(setting: String, value: String, block: suspend () -> Unit) {
        launchSafely(telemetry, DOMAIN, setting) {
            block()
            telemetry.settingChanged("zakat_$setting", value)
        }
    }

    private companion object {
        const val DOMAIN = "zakat"

        /** Stands in for a monetary value that must not reach analytics. */
        const val EDITED = "edited"
    }
}
