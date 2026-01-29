package com.arshadshah.nimaz.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.data.local.database.entity.ZakatHistoryEntity
import com.arshadshah.nimaz.domain.model.NisabType
import com.arshadshah.nimaz.domain.model.ZakatAssets
import com.arshadshah.nimaz.domain.model.ZakatCalculation
import com.arshadshah.nimaz.domain.model.ZakatCalculator
import com.arshadshah.nimaz.domain.model.ZakatLiabilities
import com.arshadshah.nimaz.domain.repository.ZakatRepository
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

data class ZakatHistoryEntry(
    val id: Long,
    val calculatedAt: Long,
    val totalAssets: Double,
    val totalLiabilities: Double,
    val netWorth: Double,
    val zakatDue: Double,
    val nisabType: NisabType,
    val nisabValue: Double,
    val isPaid: Boolean = false,
    val paidAt: Long? = null,
    val notes: String? = null
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
    private val zakatRepository: ZakatRepository
) : ViewModel() {

    private val _calculatorState = MutableStateFlow(ZakatCalculatorUiState())
    val calculatorState: StateFlow<ZakatCalculatorUiState> = _calculatorState.asStateFlow()

    private val _historyState = MutableStateFlow(ZakatHistoryUiState())
    val historyState: StateFlow<ZakatHistoryUiState> = _historyState.asStateFlow()

    init {
        loadHistory()
    }

    fun onEvent(event: ZakatEvent) {
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
                val goldValue = state.assets.goldGrams * state.goldPricePerGram
                val silverValue = state.assets.silverGrams * state.silverPricePerGram

                val totalAssets = state.assets.cashOnHand +
                        state.assets.bankBalance +
                        goldValue +
                        silverValue +
                        state.assets.investments +
                        state.assets.businessInventory +
                        state.assets.receivables +
                        state.assets.rentalIncome +
                        state.assets.otherAssets

                val totalLiabilities = state.liabilities.debts +
                        state.liabilities.loans +
                        state.liabilities.billsDue +
                        state.liabilities.otherLiabilities

                val netWorth = totalAssets - totalLiabilities

                val nisabValue = when (state.nisabType) {
                    NisabType.GOLD -> ZakatCalculator.GOLD_NISAB_GRAMS * state.goldPricePerGram
                    NisabType.SILVER -> ZakatCalculator.SILVER_NISAB_GRAMS * state.silverPricePerGram
                }

                val isAboveNisab = netWorth >= nisabValue
                val zakatDue = if (isAboveNisab) {
                    netWorth * ZakatCalculator.ZAKAT_RATE
                } else {
                    0.0
                }

                val calculation = ZakatCalculation(
                    totalAssets = totalAssets,
                    totalLiabilities = totalLiabilities,
                    netWorth = netWorth,
                    nisabType = state.nisabType,
                    nisabValue = nisabValue,
                    isAboveNisab = isAboveNisab,
                    zakatDue = zakatDue,
                    goldValue = goldValue,
                    silverValue = silverValue,
                    calculatedAt = System.currentTimeMillis()
                )

                _calculatorState.update {
                    it.copy(calculation = calculation, isCalculating = false)
                }
            } catch (e: Exception) {
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
            val entity = ZakatHistoryEntity(
                calculatedAt = calculation.calculatedAt,
                totalAssets = calculation.totalAssets,
                totalLiabilities = calculation.totalLiabilities,
                netWorth = calculation.netWorth,
                zakatDue = calculation.zakatDue,
                nisabType = calculation.nisabType.name,
                nisabValue = calculation.nisabValue
            )
            zakatRepository.insertCalculation(entity)
        }
    }

    private fun markAsPaid(entryId: Long) {
        viewModelScope.launch {
            zakatRepository.markAsPaid(entryId, System.currentTimeMillis())
        }
    }

    private fun deleteCalculation(entryId: Long) {
        viewModelScope.launch {
            zakatRepository.deleteCalculation(entryId)
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            zakatRepository.getAllHistory().collect { entities ->
                val entries = entities.map { entity ->
                    ZakatHistoryEntry(
                        id = entity.id,
                        calculatedAt = entity.calculatedAt,
                        totalAssets = entity.totalAssets,
                        totalLiabilities = entity.totalLiabilities,
                        netWorth = entity.netWorth,
                        zakatDue = entity.zakatDue,
                        nisabType = try { NisabType.valueOf(entity.nisabType) } catch (_: Exception) { NisabType.GOLD },
                        nisabValue = entity.nisabValue,
                        isPaid = entity.isPaid,
                        paidAt = entity.paidAt,
                        notes = entity.notes
                    )
                }
                val totalPaid = zakatRepository.getTotalPaid()
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
