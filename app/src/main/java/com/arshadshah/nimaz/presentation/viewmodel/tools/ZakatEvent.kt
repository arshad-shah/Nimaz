package com.arshadshah.nimaz.presentation.viewmodel.tools

import com.arshadshah.nimaz.domain.model.NisabType

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
