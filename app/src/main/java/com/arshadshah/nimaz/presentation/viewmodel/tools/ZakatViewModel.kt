package com.arshadshah.nimaz.presentation.viewmodel.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.core.monitoring.catchAndReport
import com.arshadshah.nimaz.core.monitoring.launchSafely
import com.arshadshah.nimaz.domain.model.NisabType
import com.arshadshah.nimaz.domain.model.ZakatAssets
import com.arshadshah.nimaz.domain.model.ZakatCalculation
import com.arshadshah.nimaz.domain.model.ZakatCalculator
import com.arshadshah.nimaz.domain.model.ZakatDefaults
import com.arshadshah.nimaz.domain.model.ZakatHistoryEntry
import com.arshadshah.nimaz.domain.model.ZakatLiabilities
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.usecase.ZakatUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class ZakatCalculatorUiState(
    val assets: ZakatAssets = ZakatAssets(),
    val liabilities: ZakatLiabilities = ZakatLiabilities(),
    val nisabType: NisabType = NisabType.GOLD,
    val goldPricePerGram: Double = ZakatDefaults.GOLD_PRICE_PER_GRAM,
    val silverPricePerGram: Double = ZakatDefaults.SILVER_PRICE_PER_GRAM,
    val currency: String = ZakatDefaults.CURRENCY,
    val calculation: ZakatCalculation? = null,
    val isCalculating: Boolean = false,
    val showBreakdown: Boolean = false,
    val error: String? = null
)

data class ZakatHistoryUiState(
    val history: List<ZakatHistoryEntry> = emptyList(),
    val totalZakatPaid: Double = 0.0,
    val isLoading: Boolean = true
)

sealed interface ZakatEvent {
    data class UpdateCash(val amount: Double) : ZakatEvent
    data class UpdateBankBalance(val amount: Double) : ZakatEvent
    data class UpdateGold(val grams: Double) : ZakatEvent
    data class UpdateSilver(val grams: Double) : ZakatEvent
    data class UpdateInvestments(val amount: Double) : ZakatEvent
    data class UpdateBusinessInventory(val amount: Double) : ZakatEvent
    data class UpdateReceivables(val amount: Double) : ZakatEvent
    data class UpdateRentalIncome(val amount: Double) : ZakatEvent
    data class UpdateOtherAssets(val amount: Double) : ZakatEvent
    data class UpdateDebts(val amount: Double) : ZakatEvent
    data class UpdateLoans(val amount: Double) : ZakatEvent
    data class UpdateBillsDue(val amount: Double) : ZakatEvent
    data class UpdateOtherLiabilities(val amount: Double) : ZakatEvent
    data class SetNisabType(val nisabType: NisabType) : ZakatEvent
    data class UpdateGoldPrice(val pricePerGram: Double) : ZakatEvent
    data class UpdateSilverPrice(val pricePerGram: Double) : ZakatEvent
    data class SetCurrency(val currency: String) : ZakatEvent
    data object Calculate : ZakatEvent
    data object ClearAll : ZakatEvent
    data object ToggleBreakdown : ZakatEvent
    data object SaveCalculation : ZakatEvent
    data class MarkAsPaid(val entryId: Long) : ZakatEvent
    data class DeleteCalculation(val entryId: Long) : ZakatEvent
    data object LoadHistory : ZakatEvent
}

@HiltViewModel
class ZakatViewModel @Inject constructor(
    private val zakatUseCases: ZakatUseCases,
    private val settingsRepository: SettingsRepository,
    private val telemetry: Telemetry,
) : ViewModel() {

    private val _calculatorState = MutableStateFlow(ZakatCalculatorUiState())
    val calculatorState: StateFlow<ZakatCalculatorUiState> = _calculatorState.asStateFlow()

    private val _historyState = MutableStateFlow(ZakatHistoryUiState())
    val historyState: StateFlow<ZakatHistoryUiState> = _historyState.asStateFlow()

    init {
        loadHistory()
        observeMetalPrices()
    }

    /**
     * The metal prices and currency are persisted, not constants: [ZakatCalculator]
     * derives the nisab threshold from the gold price as well as the metal valuation,
     * so a stale price changes whether zakat is owed at all, not merely how much.
     *
     * Recalculates on every change so an edited price is reflected immediately.
     */
    private fun observeMetalPrices() {
        combine(
            settingsRepository.zakatGoldPricePerGram,
            settingsRepository.zakatSilverPricePerGram,
            settingsRepository.zakatCurrency,
        ) { gold, silver, currency ->
            Triple(gold, silver, currency)
        }
            .onEach { (gold, silver, currency) ->
                _calculatorState.update {
                    it.copy(
                        goldPricePerGram = gold,
                        silverPricePerGram = silver,
                        currency = currency,
                    )
                }
                recalculate()
            }
            .catchAndReport(telemetry, DOMAIN, "observe_prices")
            .launchIn(viewModelScope)
    }

    fun onEvent(event: ZakatEvent) {
        // Log only the actions, never the monetary amounts (financial data stays
        // out of analytics).
        when (event) {
            is ZakatEvent.UpdateCash -> updateAsset { it.copy(cashOnHand = event.amount) }
            is ZakatEvent.UpdateBankBalance -> updateAsset { it.copy(bankBalance = event.amount) }
            is ZakatEvent.UpdateGold -> updateAsset { it.copy(goldGrams = event.grams) }
            is ZakatEvent.UpdateSilver -> updateAsset { it.copy(silverGrams = event.grams) }
            is ZakatEvent.UpdateInvestments -> updateAsset { it.copy(investments = event.amount) }
            is ZakatEvent.UpdateBusinessInventory -> updateAsset { it.copy(businessInventory = event.amount) }
            is ZakatEvent.UpdateReceivables -> updateAsset { it.copy(receivables = event.amount) }
            is ZakatEvent.UpdateRentalIncome -> updateAsset { it.copy(rentalIncome = event.amount) }
            is ZakatEvent.UpdateOtherAssets -> updateAsset { it.copy(otherAssets = event.amount) }
            is ZakatEvent.UpdateDebts -> updateLiability { it.copy(debts = event.amount) }
            is ZakatEvent.UpdateLoans -> updateLiability { it.copy(loans = event.amount) }
            is ZakatEvent.UpdateBillsDue -> updateLiability { it.copy(billsDue = event.amount) }
            is ZakatEvent.UpdateOtherLiabilities -> updateLiability { it.copy(otherLiabilities = event.amount) }
            is ZakatEvent.SetNisabType -> {
                _calculatorState.update { it.copy(nisabType = event.nisabType) }
                recalculate()
            }

            // Persisted, not just held in state: these survived only until process death
            // before, and the observer above feeds the new value back in.
            is ZakatEvent.UpdateGoldPrice -> persist("gold_price") {
                settingsRepository.setZakatGoldPricePerGram(event.pricePerGram)
            }

            is ZakatEvent.UpdateSilverPrice -> persist("silver_price") {
                settingsRepository.setZakatSilverPricePerGram(event.pricePerGram)
            }

            is ZakatEvent.SetCurrency -> persist("currency") {
                settingsRepository.setZakatCurrency(event.currency)
            }

            ZakatEvent.Calculate -> {
                telemetry.featureUsed(DOMAIN, "calculate")
                calculate()
            }

            ZakatEvent.ClearAll -> clearAll()
            ZakatEvent.ToggleBreakdown -> _calculatorState.update { it.copy(showBreakdown = !it.showBreakdown) }
            ZakatEvent.SaveCalculation -> {
                telemetry.featureUsed(DOMAIN, "save")
                saveCalculation()
            }

            is ZakatEvent.MarkAsPaid -> {
                telemetry.featureUsed(DOMAIN, "mark_paid")
                markAsPaid(event.entryId)
            }

            is ZakatEvent.DeleteCalculation -> deleteCalculation(event.entryId)
            ZakatEvent.LoadHistory -> loadHistory()
        }
    }

    private fun updateAsset(update: (ZakatAssets) -> ZakatAssets) {
        _calculatorState.update { state ->
            state.copy(assets = update(state.assets))
        }
        recalculate()
    }

    private fun updateLiability(update: (ZakatLiabilities) -> ZakatLiabilities) {
        _calculatorState.update { state ->
            state.copy(liabilities = update(state.liabilities))
        }
        recalculate()
    }

    /**
     * Recalculates, or clears the result when the form is empty.
     *
     * Clearing matters: without it, emptying the last field left the *previous*
     * calculation on screen, and the breakdown card renders whenever `calculation`
     * is non-null — so the user saw a zakat figure over a blank form. The Zakat
     * redesign pins that total to a sticky hero, which would have made it permanent.
     */
    private fun recalculate() {
        val state = _calculatorState.value
        if (state.assets.hasAnyValue() || state.liabilities.hasAnyValue()) {
            calculate()
        } else {
            _calculatorState.update { it.copy(calculation = null, error = null) }
        }
    }

    private fun persist(type: String, write: suspend () -> Unit) {
        launchSafely(telemetry, DOMAIN, type) { write() }
    }

    private fun calculate() {
        _calculatorState.update { it.copy(isCalculating = true, error = null) }

        run {
            try {
                val state = _calculatorState.value
                // The calculation itself lives in the domain (ZakatCalculator), not here.
                // It used to be written out inline in this ViewModel while a second, wrong
                // copy sat unused in the domain — see ZakatCalculatorTest.
                val calculation = ZakatCalculator.calculate(
                    assets = state.assets,
                    liabilities = state.liabilities,
                    nisabType = state.nisabType,
                    goldPricePerGram = state.goldPricePerGram,
                    silverPricePerGram = state.silverPricePerGram,
                    currency = state.currency
                )

                _calculatorState.update {
                    it.copy(calculation = calculation, isCalculating = false)
                }
            } catch (e: Exception) {
                telemetry.failure(DOMAIN, "calculate", e)
                _calculatorState.update {
                    it.copy(error = e.message, isCalculating = false)
                }
            }
        }
    }

    private fun clearAll() {
        _calculatorState.update {
            ZakatCalculatorUiState(
                goldPricePerGram = it.goldPricePerGram,
                silverPricePerGram = it.silverPricePerGram,
                currency = it.currency
            )
        }
    }

    private fun saveCalculation() {
        val calculation = _calculatorState.value.calculation ?: return

        launchSafely(telemetry, DOMAIN, "save") {
            val entry = ZakatHistoryEntry(
                calculatedAt = calculation.calculatedAt,
                totalAssets = calculation.totalAssets,
                totalLiabilities = calculation.totalLiabilities,
                netWorth = calculation.netWorth,
                zakatDue = calculation.zakatDue,
                nisabType = calculation.nisabType,
                nisabValue = calculation.nisabValue
            )
            zakatUseCases.insertCalculation(entry)
        }
    }

    private fun markAsPaid(entryId: Long) {
        launchSafely(telemetry, DOMAIN, "mark_paid") {
            zakatUseCases.markAsPaid(entryId, System.currentTimeMillis())
        }
    }

    private fun deleteCalculation(entryId: Long) {
        launchSafely(telemetry, DOMAIN, "delete") {
            zakatUseCases.deleteCalculation(entryId)
        }
    }

    private fun loadHistory() {
        launchSafely(telemetry, DOMAIN, "load_history") {
            zakatUseCases.getAllHistory().collect { entries ->
                val totalPaid = zakatUseCases.getTotalPaid()
                _historyState.update {
                    it.copy(
                        history = entries,
                        totalZakatPaid = totalPaid,
                        isLoading = false
                    )
                }
            }
        }
    }

    private companion object {
        const val DOMAIN = "zakat"
    }

    private fun ZakatAssets.hasAnyValue(): Boolean {
        return cashOnHand > 0 || bankBalance > 0 || goldGrams > 0 || silverGrams > 0 ||
                investments > 0 || businessInventory > 0 || receivables > 0 ||
                rentalIncome > 0 || otherAssets > 0
    }

    private fun ZakatLiabilities.hasAnyValue(): Boolean {
        return debts > 0 || loans > 0 || billsDue > 0 || otherLiabilities > 0
    }
}
