package com.arshadshah.nimaz.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.services.CurrencyInfo
import com.arshadshah.nimaz.services.NisabType
import com.arshadshah.nimaz.services.PreciousMetalService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject

data class ZakatState(
    val currencyInfo: CurrencyInfo = CurrencyInfo("USD", "$"),
    val isManualCurrencyOverride: Boolean = false,
    val zakatStartDate: String = "",
    val zakatEndDate: String = "",
    // Assets
    val goldAndSilverAmount: String = "",
    val cashAmount: String = "",
    val otherSavings: String = "",
    val investmentsAmount: String = "",
    val moneyOwedToYou: String = "",
    val stockValue: String = "",

    // Liabilities
    val moneyYouOwe: String = "",
    val otherOutgoingsDue: String = "",

    // Calculations
    val netAssets: Double = 0.0,
    val totalZakat: Double = 0.0,
    val isEligible: Boolean = false,

    // Nisab thresholds
    val goldNisabThreshold: Double = PreciousMetalService.DEFAULT_GOLD_NISAB,
    val silverNisabThreshold: Double = PreciousMetalService.DEFAULT_SILVER_NISAB,
    val selectedNisabType: NisabType = NisabType.GOLD,

    // UI State
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val currentNisabThreshold: Double
        get() = when (selectedNisabType) {
            NisabType.GOLD -> goldNisabThreshold
            NisabType.SILVER -> silverNisabThreshold
        }
}

@HiltViewModel
class ZakatViewModel @Inject constructor(
    private val metalService: PreciousMetalService
) : ViewModel() {
    private val _zakatState = MutableStateFlow(ZakatState())
    val zakatState: StateFlow<ZakatState> = _zakatState.asStateFlow()

    init {
        updateNisabThresholds()
        calculateZakatPeriod()
    }

    fun updateCurrency(locale: Locale) {
        viewModelScope.launch {
            updateNisabThresholds(locale)
        }
    }

    private fun updateNisabThresholds(locale: Locale = Locale.getDefault()) {
        viewModelScope.launch {
            _zakatState.update { it.copy(isLoading = true, error = null) }
            try {
                val thresholds = metalService.getNisabThresholdsWithCache(locale)
                val currentState = _zakatState.value
                _zakatState.update {
                    it.copy(
                        goldNisabThreshold = thresholds.goldNisab,
                        silverNisabThreshold = thresholds.silverNisab,
                        // Preserve manually selected currency, otherwise use API currency
                        currencyInfo = if (currentState.isManualCurrencyOverride)
                            currentState.currencyInfo
                        else
                            thresholds.currencyInfo,
                        isLoading = false
                    )
                }
                calculateZakat()
            } catch (e: Exception) {
                _zakatState.update {
                    it.copy(
                        error = "Failed to update Nisab thresholds: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun calculateZakatPeriod() {
        val today = LocalDate.now()
        val todayHijri = HijrahDate.from(today)

        // Calculate start date (one lunar year ago in Hijri)
        val startDateHijri = todayHijri.minus(1, ChronoUnit.YEARS)
        val startDateGregorian = LocalDate.from(startDateHijri)

        // Format dates using DateTimeFormatter for consistent formatting
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

        val startDate = startDateGregorian.format(formatter)
        val endDate = today.format(formatter)

        // Also get Hijri dates for additional context
        val hijriFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
        val startDateHijriFormatted = startDateHijri.format(hijriFormatter)
        val endDateHijriFormatted = todayHijri.format(hijriFormatter)

        _zakatState.update { currentState ->
            currentState.copy(
                zakatStartDate = "$startDate ($startDateHijriFormatted H)",
                zakatEndDate = "$endDate ($endDateHijriFormatted H)"
            )
        }
    }

    // Optional: Add helper function to format dual calendar dates
    private fun formatDualCalendarDate(gregorianDate: LocalDate): String {
        val hijriDate = HijrahDate.from(gregorianDate)
        val gregorianFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
        val hijriFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

        return "${gregorianDate.format(gregorianFormatter)} (${hijriDate.format(hijriFormatter)} H)"
    }

    fun refreshNisabThresholds() {
        updateNisabThresholds()
    }

    fun setNisabType(type: NisabType) {
        _zakatState.update { it.copy(selectedNisabType = type) }
        calculateZakat()
    }

    // Asset update functions
    fun updateGoldAndSilver(amount: String) {
        _zakatState.update { it.copy(goldAndSilverAmount = amount) }
        calculateZakat()
    }

    fun updateCashAmount(amount: String) {

        _zakatState.update { it.copy(cashAmount = amount) }
        calculateZakat()
    }

    fun updateOtherSavings(amount: String) {
        _zakatState.update { it.copy(otherSavings = amount) }
        calculateZakat()
    }

    fun updateInvestments(amount: String) {
        _zakatState.update { it.copy(investmentsAmount = amount) }
        calculateZakat()
    }

    fun updateMoneyOwedToYou(amount: String) {
        _zakatState.update { it.copy(moneyOwedToYou = amount) }
        calculateZakat()
    }

    fun updateStockValue(amount: String) {
        _zakatState.update { it.copy(stockValue = amount) }
        calculateZakat()
    }

    fun updateMoneyYouOwe(amount: String) {
        _zakatState.update { it.copy(moneyYouOwe = amount) }
        calculateZakat()
    }

    fun updateOtherOutgoingsDue(amount: String) {
        _zakatState.update { it.copy(otherOutgoingsDue = amount) }
        calculateZakat()
    }

    // Then modify the calculateZakat function to use this helper
    private fun calculateZakat() {
        val state = _zakatState.value

        // Convert all string inputs to non-negative doubles, defaulting to 0.0 if empty, invalid, or negative
        val goldAndSilverValue = state.goldAndSilverAmount.toNonNegativeDoubleOrNull() ?: 0.0
        val cashValue = state.cashAmount.toNonNegativeDoubleOrNull() ?: 0.0
        val otherSavingsValue = state.otherSavings.toNonNegativeDoubleOrNull() ?: 0.0
        val investmentsValue = state.investmentsAmount.toNonNegativeDoubleOrNull() ?: 0.0
        val moneyOwedToYouValue = state.moneyOwedToYou.toNonNegativeDoubleOrNull() ?: 0.0
        val stockValue = state.stockValue.toNonNegativeDoubleOrNull() ?: 0.0

        // Liabilities
        val moneyYouOweValue = state.moneyYouOwe.toNonNegativeDoubleOrNull() ?: 0.0
        val otherOutgoingsDueValue = state.otherOutgoingsDue.toNonNegativeDoubleOrNull() ?: 0.0

        // Rest of the calculation remains the same
        val totalAssets = goldAndSilverValue + cashValue + otherSavingsValue +
                investmentsValue + moneyOwedToYouValue + stockValue

        val totalLiabilities = moneyYouOweValue + otherOutgoingsDueValue
        val netAssets = totalAssets - totalLiabilities
        val isEligible = netAssets >= state.currentNisabThreshold
        val zakatAmount = if (isEligible) netAssets * 0.025 else 0.0

        _zakatState.update {
            it.copy(
                netAssets = netAssets,
                totalZakat = zakatAmount,
                isEligible = isEligible
            )
        }
    }

    // Format currency for display
    fun formatCurrency(amount: Double): String {
        return String.format("%.2f", amount)
    }

    private fun String.toNonNegativeDoubleOrNull(): Double? {
        return this.toDoubleOrNull()?.let { if (it < 0) 0.0 else it }
    }

    fun setCurrencyOverride(code: String, symbol: String) {
        _zakatState.update {
            it.copy(
                currencyInfo = CurrencyInfo(
                    code = code.uppercase(Locale.getDefault()),
                    symbol = symbol,
                    exchangeRate = it.currencyInfo.exchangeRate
                ),
                isManualCurrencyOverride = true
            )
        }
    }

    fun updateCurrencyManually(code: String, symbol: String) {
        viewModelScope.launch {
            _zakatState.update { it.copy(isLoading = true, error = null, isManualCurrencyOverride = true) }
            try {
                val currencyInfo = CurrencyInfo(
                    code = code.uppercase(Locale.getDefault()),
                    symbol = symbol
                )
                val thresholds = metalService.getNisabThresholdsForCurrency(currencyInfo)
                _zakatState.update {
                    it.copy(
                        goldNisabThreshold = thresholds.goldNisab,
                        silverNisabThreshold = thresholds.silverNisab,
                        currencyInfo = thresholds.currencyInfo,
                        isLoading = false
                    )
                }
                calculateZakat()
            } catch (e: Exception) {
                _zakatState.update {
                    it.copy(
                        error = "Failed to update for currency $code: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun resetCurrencyToLocale() {
        _zakatState.update { it.copy(isManualCurrencyOverride = false) }
        updateNisabThresholds(Locale.getDefault())
    }
}