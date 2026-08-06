package com.arshadshah.nimaz.presentation.viewmodel.tools

/**
 * What the zakat **calculator** can do.
 *
 * The nisab basis, the two metal prices and the currency used to live here too. They are
 * persisted preferences rather than figures typed per calculation, and they moved with their
 * controls to `ZakatSettingsEvent` — leaving this interface as exactly the form plus the four
 * things you do with a finished calculation.
 */
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
    data object ClearAll : ZakatEvent
    data object ToggleBreakdown : ZakatEvent
    data object SaveCalculation : ZakatEvent
    data class MarkAsPaid(val entryId: Long) : ZakatEvent
    data class DeleteCalculation(val entryId: Long) : ZakatEvent
    data object LoadHistory : ZakatEvent

    /** Runs the sum again over the figures already entered, after a failed calculation. */
    data object Recalculate : ZakatEvent
}
