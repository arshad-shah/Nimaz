package com.arshadshah.nimaz.presentation.viewmodel.tools

import androidx.lifecycle.SavedStateHandle
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
import com.arshadshah.nimaz.domain.repository.settings.ZakatSettings
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

@HiltViewModel
class ZakatViewModel @Inject constructor(
    private val zakatUseCases: ZakatUseCases,
    private val zakatSettings: ZakatSettings,
    private val telemetry: Telemetry,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    /**
     * The form starts from whatever the user had typed before the process died.
     *
     * This is the longest form in the app — up to thirteen monetary figures, several of which
     * a user has to look up — and it lived entirely in a `MutableStateFlow`. A phone call
     * during data entry returned them to an empty form with no indication anything had been
     * lost. The metal prices and currency are not restored here: those are persisted
     * settings, and [observeMetalPrices] re-reads them.
     */
    private val _calculatorState = MutableStateFlow(savedState.restoreForm())
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
            zakatSettings.zakatGoldPricePerGram,
            zakatSettings.zakatSilverPricePerGram,
            zakatSettings.zakatCurrency,
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
            // The amount-entry events above are deliberately **not** logged. #359 asks for
            // "every amount entry"; each is dispatched from a text field on every character,
            // so instrumenting them literally would emit a stream of events per figure typed
            // — the firehose §4 of the same issue objects to. The once-per-filled-form signal
            // from `calculate()` is what says a calculation happened; these say a digit did.
            is ZakatEvent.SetNisabType -> {
                telemetry.settingChanged("zakat_nisab_type", event.nisabType.name)
                _calculatorState.update { it.copy(nisabType = event.nisabType) }
                saveForm()
                recalculate()
            }

            // Persisted, not just held in state: these survived only until process death
            // before, and the observer above feeds the new value back in.
            is ZakatEvent.UpdateGoldPrice -> persist("gold_price") {
                zakatSettings.setZakatGoldPricePerGram(event.pricePerGram)
            }

            is ZakatEvent.UpdateSilverPrice -> persist("silver_price") {
                zakatSettings.setZakatSilverPricePerGram(event.pricePerGram)
            }

            is ZakatEvent.SetCurrency -> persist("currency") {
                zakatSettings.setZakatCurrency(event.currency)
            }

            ZakatEvent.ClearAll -> {
                telemetry.featureUsed(DOMAIN, "clear_all")
                hasLoggedCalculation = false
                clearAll()
            }
            ZakatEvent.ToggleBreakdown -> {
                telemetry.featureUsed(DOMAIN, "toggle_breakdown")
                _calculatorState.update { it.copy(showBreakdown = !it.showBreakdown) }
            }
            ZakatEvent.SaveCalculation -> {
                telemetry.featureUsed(DOMAIN, "save")
                saveCalculation()
            }

            is ZakatEvent.MarkAsPaid -> {
                telemetry.featureUsed(DOMAIN, "mark_paid")
                markAsPaid(event.entryId)
            }

            is ZakatEvent.DeleteCalculation -> {
                telemetry.featureUsed(DOMAIN, "delete_calculation")
                deleteCalculation(event.entryId)
            }
            ZakatEvent.LoadHistory -> loadHistory()
        }
    }

    private fun updateAsset(update: (ZakatAssets) -> ZakatAssets) {
        _calculatorState.update { state ->
            state.copy(assets = update(state.assets))
        }
        saveForm()
        recalculate()
    }

    private fun updateLiability(update: (ZakatLiabilities) -> ZakatLiabilities) {
        _calculatorState.update { state ->
            state.copy(liabilities = update(state.liabilities))
        }
        saveForm()
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

    /**
     * Whether this form has already reported a calculation.
     *
     * `recalculate()` runs on every keystroke, so logging inside [calculate] unguarded would
     * emit one event per digit typed. One event per filled-in form is what a funnel wants; the
     * flag clears when the form does.
     */
    private var hasLoggedCalculation = false

    private fun calculate() {
        if (!hasLoggedCalculation) {
            hasLoggedCalculation = true
            telemetry.featureUsed(DOMAIN, "calculate")
        }
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
        // Clearing has to reach the saved state too, or a cleared form comes back on the
        // next process death.
        saveForm()
    }

    /**
     * Mirror the typed figures into [SavedStateHandle].
     *
     * Written field by field rather than as one blob: the state types are domain models, and
     * making them `Serializable`/`Parcelable` to fit a `Bundle` would push a presentation
     * storage concern down into `domain/model`. Only what the user typed is saved — the
     * result, the spinner and the error are all derived and are recomputed on restore.
     */
    private fun saveForm() {
        val state = _calculatorState.value
        savedState[KEY_CASH] = state.assets.cashOnHand
        savedState[KEY_BANK] = state.assets.bankBalance
        savedState[KEY_GOLD_GRAMS] = state.assets.goldGrams
        savedState[KEY_SILVER_GRAMS] = state.assets.silverGrams
        savedState[KEY_INVESTMENTS] = state.assets.investments
        savedState[KEY_INVENTORY] = state.assets.businessInventory
        savedState[KEY_RECEIVABLES] = state.assets.receivables
        savedState[KEY_RENTAL] = state.assets.rentalIncome
        savedState[KEY_OTHER_ASSETS] = state.assets.otherAssets
        savedState[KEY_DEBTS] = state.liabilities.debts
        savedState[KEY_LOANS] = state.liabilities.loans
        savedState[KEY_BILLS] = state.liabilities.billsDue
        savedState[KEY_OTHER_LIABILITIES] = state.liabilities.otherLiabilities
        savedState[KEY_NISAB_TYPE] = state.nisabType.name
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

    /**
     * `ZakatHistoryUiState.isLoading` defaults to `true` and was only ever cleared *inside*
     * the collect, so a stream that failed before its first emission — a missing table after
     * a content-database replacement, say — left the history screen spinning for the life of
     * the ViewModel, with nothing on screen to say why.
     *
     * The failure clears the spinner so the empty state shows instead. It does not set
     * `error`: `ZakatScreen` never reads that field, so writing it would be error production
     * with nothing rendering it. Reporting still happens through `launchSafely`.
     */
    private fun loadHistory() {
        launchSafely(
            telemetry,
            DOMAIN,
            "load_history",
            onFailure = { _historyState.update { it.copy(isLoading = false) } },
        ) {
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

    /**
     * Rebuild the form from [SavedStateHandle], falling back to an empty form.
     *
     * Every figure defaults to `0.0` and the nisab basis to whatever the state class defaults
     * to, so a saved state written by an older build that is missing a key restores a blank
     * field rather than failing.
     */
    private fun SavedStateHandle.restoreForm(): ZakatCalculatorUiState {
        val default = ZakatCalculatorUiState()
        return default.copy(
            assets = ZakatAssets(
                cashOnHand = get<Double>(KEY_CASH) ?: 0.0,
                bankBalance = get<Double>(KEY_BANK) ?: 0.0,
                goldGrams = get<Double>(KEY_GOLD_GRAMS) ?: 0.0,
                silverGrams = get<Double>(KEY_SILVER_GRAMS) ?: 0.0,
                investments = get<Double>(KEY_INVESTMENTS) ?: 0.0,
                businessInventory = get<Double>(KEY_INVENTORY) ?: 0.0,
                receivables = get<Double>(KEY_RECEIVABLES) ?: 0.0,
                rentalIncome = get<Double>(KEY_RENTAL) ?: 0.0,
                otherAssets = get<Double>(KEY_OTHER_ASSETS) ?: 0.0,
            ),
            liabilities = ZakatLiabilities(
                debts = get<Double>(KEY_DEBTS) ?: 0.0,
                loans = get<Double>(KEY_LOANS) ?: 0.0,
                billsDue = get<Double>(KEY_BILLS) ?: 0.0,
                otherLiabilities = get<Double>(KEY_OTHER_LIABILITIES) ?: 0.0,
            ),
            nisabType = get<String>(KEY_NISAB_TYPE)
                ?.let { name -> NisabType.entries.firstOrNull { it.name == name } }
                ?: default.nisabType,
        )
    }

    private companion object {
        const val DOMAIN = "zakat"

        const val KEY_CASH = "zakat_cash_on_hand"
        const val KEY_BANK = "zakat_bank_balance"
        const val KEY_GOLD_GRAMS = "zakat_gold_grams"
        const val KEY_SILVER_GRAMS = "zakat_silver_grams"
        const val KEY_INVESTMENTS = "zakat_investments"
        const val KEY_INVENTORY = "zakat_business_inventory"
        const val KEY_RECEIVABLES = "zakat_receivables"
        const val KEY_RENTAL = "zakat_rental_income"
        const val KEY_OTHER_ASSETS = "zakat_other_assets"
        const val KEY_DEBTS = "zakat_debts"
        const val KEY_LOANS = "zakat_loans"
        const val KEY_BILLS = "zakat_bills_due"
        const val KEY_OTHER_LIABILITIES = "zakat_other_liabilities"
        const val KEY_NISAB_TYPE = "zakat_nisab_type"
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
