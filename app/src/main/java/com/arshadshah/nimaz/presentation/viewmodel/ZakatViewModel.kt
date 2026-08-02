package com.arshadshah.nimaz.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.domain.model.NisabType
import com.arshadshah.nimaz.domain.model.ZakatAssets
import com.arshadshah.nimaz.domain.model.ZakatCalculation
import com.arshadshah.nimaz.domain.model.ZakatCalculator
import com.arshadshah.nimaz.domain.model.ZakatHistoryEntry
import com.arshadshah.nimaz.domain.model.ZakatLiabilities
import com.arshadshah.nimaz.domain.usecase.ZakatUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ZakatCalculatorUiState(
    val assets: ZakatAssets = ZakatAssets(),
    val liabilities: ZakatLiabilities = ZakatLiabilities(),
    val nisabType: NisabType = NisabType.GOLD,
    val goldPricePerGram: Double = 65.0,
    val silverPricePerGram: Double = 0.80,
    val currency: String = "USD",
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
    private val zakatUseCases: ZakatUseCases
) : ViewModel() {

    private val _calculatorState = MutableStateFlow(ZakatCalculatorUiState())
    val calculatorState: StateFlow<ZakatCalculatorUiState> = _calculatorState.asStateFlow()

    private val _historyState = MutableStateFlow(ZakatHistoryUiState())
    val historyState: StateFlow<ZakatHistoryUiState> = _historyState.asStateFlow()

    init {
        loadHistory()
    }

    fun onEvent(event: ZakatEvent) {
        // Log only the actions, never the monetary amounts (financial data stays
        // out of analytics).
        when (event) {
            ZakatEvent.Calculate -> AppAnalytics.logFeatureUsed("zakat", "calculate")
            ZakatEvent.SaveCalculation -> AppAnalytics.logFeatureUsed("zakat", "save")
            is ZakatEvent.MarkAsPaid -> AppAnalytics.logFeatureUsed("zakat", "mark_paid")
            else -> {}
        }
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

            is ZakatEvent.UpdateGoldPrice -> {
                _calculatorState.update { it.copy(goldPricePerGram = event.pricePerGram) }
                recalculate()
            }

            is ZakatEvent.UpdateSilverPrice -> {
                _calculatorState.update { it.copy(silverPricePerGram = event.pricePerGram) }
                recalculate()
            }

            is ZakatEvent.SetCurrency -> _calculatorState.update { it.copy(currency = event.currency) }
            ZakatEvent.Calculate -> calculate()
            ZakatEvent.ClearAll -> clearAll()
            ZakatEvent.ToggleBreakdown -> _calculatorState.update { it.copy(showBreakdown = !it.showBreakdown) }
            ZakatEvent.SaveCalculation -> saveCalculation()
            is ZakatEvent.MarkAsPaid -> markAsPaid(event.entryId)
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

    private fun recalculate() {
        val state = _calculatorState.value
        if (state.assets.hasAnyValue() || state.liabilities.hasAnyValue()) {
            calculate()
        }
    }

    private fun calculate() {
        _calculatorState.update { it.copy(isCalculating = true, error = null) }

        viewModelScope.launch {
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
                CrashReporter.recordException(e)
                AppAnalytics.logError("zakat", "calculate", e.message)
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

        viewModelScope.launch {
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
        viewModelScope.launch {
            zakatUseCases.markAsPaid(entryId, System.currentTimeMillis())
        }
    }

    private fun deleteCalculation(entryId: Long) {
        viewModelScope.launch {
            zakatUseCases.deleteCalculation(entryId)
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
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

    private fun ZakatAssets.hasAnyValue(): Boolean {
        return cashOnHand > 0 || bankBalance > 0 || goldGrams > 0 || silverGrams > 0 ||
                investments > 0 || businessInventory > 0 || receivables > 0 ||
                rentalIncome > 0 || otherAssets > 0
    }

    private fun ZakatLiabilities.hasAnyValue(): Boolean {
        return debts > 0 || loans > 0 || billsDue > 0 || otherLiabilities > 0
    }
}
