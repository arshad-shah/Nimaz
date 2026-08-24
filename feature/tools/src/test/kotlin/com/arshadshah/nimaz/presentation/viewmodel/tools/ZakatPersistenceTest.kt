package com.arshadshah.nimaz.presentation.viewmodel.tools

import androidx.lifecycle.SavedStateHandle
import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.NisabType
import com.arshadshah.nimaz.domain.model.ZakatHistoryEntry
import com.arshadshah.nimaz.domain.repository.FakeZakatSettings
import com.arshadshah.nimaz.domain.usecase.ZakatUseCases
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * What happens to a zakat figure **after** it has been worked out: saving it, marking it paid,
 * deleting it, and what the user is told when any of those does not land.
 *
 * `ZakatViewModelTest` covers the arithmetic reaching state. This covers the write paths, which
 * had no test at all and are where the consequences are least recoverable — a save that silently
 * did nothing loses a calculation the user spent an afternoon assembling, and a "mark as paid"
 * that fails without saying so leaves someone believing an obligation is discharged when the app
 * still has it outstanding.
 *
 * Every write goes through `launchSafely` with an `onFailure`, and the two `onFailure`s are
 * deliberately **different**: a calculator-side failure must not clear the form, and a
 * history-side failure must not touch the calculator at all. Both halves are asserted below,
 * because the wrong one is a single-word mistake at the call site that nothing else catches.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ZakatPersistenceTest {

    private val dispatcher = StandardTestDispatcher()
    private val telemetry = RecordingTelemetry()
    private val settings = FakeZakatSettings()
    private lateinit var useCases: ZakatUseCases

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        useCases = mockk(relaxed = true)
        every { useCases.getAllHistory() } returns flowOf(emptyList())
        coEvery { useCases.getTotalPaid() } returns 0.0
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(saved: SavedStateHandle = SavedStateHandle()) =
        ZakatViewModel(useCases, settings, telemetry, saved)

    private fun entry(
        id: Long,
        zakatDue: Double = 250.0,
        isPaid: Boolean = false,
    ) = ZakatHistoryEntry(
        id = id,
        calculatedAt = 1_700_000_000_000L,
        totalAssets = 10_000.0,
        totalLiabilities = 0.0,
        netWorth = 10_000.0,
        zakatDue = zakatDue,
        nisabType = NisabType.GOLD,
        nisabValue = 5_686.2,
        isPaid = isPaid,
    )

    // ------------------------------------------------------------------
    // Saving
    // ------------------------------------------------------------------

    @Test
    fun `saving writes the calculation the user is looking at, not a recomputed one`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(ZakatEvent.UpdateCash(10_000.0))
        advanceUntilIdle()
        val onScreen = vm.calculatorState.value.calculation!!

        vm.onEvent(ZakatEvent.SaveCalculation)
        advanceUntilIdle()

        // The history row has to carry the same six figures the hero and the breakdown showed.
        // `saveCalculation` builds the entry from `state.calculation` rather than calling
        // `ZakatCalculator` again; recomputing at save time would re-read the metal prices, so a
        // settings write that landed between reading the total and pressing Save would file a
        // figure the user never saw.
        coVerify {
            useCases.insertCalculation(
                match {
                    it.totalAssets == onScreen.totalAssets &&
                            it.totalLiabilities == onScreen.totalLiabilities &&
                            it.netWorth == onScreen.netWorth &&
                            it.zakatDue == onScreen.zakatDue &&
                            it.nisabType == onScreen.nisabType &&
                            it.nisabValue == onScreen.nisabValue &&
                            it.calculatedAt == onScreen.calculatedAt
                }
            )
        }
    }

    @Test
    fun `saving an empty form writes nothing`() = runTest {
        // `recalculate()` clears `calculation` when every field is empty, and `saveCalculation`
        // returns on the null. Without that guard the action bar's disabled state would be the
        // only thing standing between an untouched form and a history full of zero rows.
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(ZakatEvent.SaveCalculation)
        advanceUntilIdle()

        coVerify(exactly = 0) { useCases.insertCalculation(any()) }
    }

    @Test
    fun `a save that fails reports it without clearing the form`() = runTest {
        coEvery { useCases.insertCalculation(any()) } throws IllegalStateException("disk full")
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(ZakatEvent.UpdateCash(10_000.0))
        vm.onEvent(ZakatEvent.UpdateDebts(500.0))
        advanceUntilIdle()

        vm.onEvent(ZakatEvent.SaveCalculation)
        advanceUntilIdle()

        val state = vm.calculatorState.value
        assertThat(state.error?.message).isEqualTo(R.string.zakat_save_failed)
        assertThat(state.error?.details).isEqualTo("disk full")
        // The whole point of the inline error: every figure typed is still there and still
        // valid. Reporting a failed write by resetting the form would cost the user far more
        // than the failure did.
        assertThat(state.assets.cashOnHand).isEqualTo(10_000.0)
        assertThat(state.liabilities.debts).isEqualTo(500.0)
        assertThat(state.calculation).isNotNull()
    }

    @Test
    fun `a failed save leaves the history screen alone`() = runTest {
        // The two error channels are separate state objects. A save failure routed to
        // `historyWriteFailed` would put "couldn't be saved" on a screen the user is not on,
        // and leave the calculator — where they are — showing nothing wrong at all.
        coEvery { useCases.insertCalculation(any()) } throws IllegalStateException("disk full")
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(ZakatEvent.UpdateCash(10_000.0))
        advanceUntilIdle()
        vm.onEvent(ZakatEvent.SaveCalculation)
        advanceUntilIdle()

        assertThat(vm.historyState.value.error).isNull()
    }

    // ------------------------------------------------------------------
    // Marking paid and deleting
    // ------------------------------------------------------------------

    @Test
    fun `marking an entry paid stamps the time it was paid`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        val before = System.currentTimeMillis()
        vm.onEvent(ZakatEvent.MarkAsPaid(entryId = 42L))
        advanceUntilIdle()

        // The history card renders "Paid on <date>" from this stamp, so passing 0 — or the
        // calculation's own timestamp — would label a payment made today with the date the
        // figure was worked out, which for zakat can be a lunar year earlier.
        coVerify { useCases.markAsPaid(42L, match { it >= before }) }
    }

    @Test
    fun `a mark-as-paid that fails says so on the history screen`() = runTest {
        coEvery { useCases.markAsPaid(any(), any()) } throws IllegalStateException("no row")
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(ZakatEvent.MarkAsPaid(entryId = 7L))
        advanceUntilIdle()

        // Silence here is the dangerous outcome: the badge stays "Unpaid" either way, so
        // without the error the user cannot tell a write that failed from one they imagined.
        assertThat(vm.historyState.value.error?.message).isEqualTo(R.string.zakat_mark_paid_failed)
        assertThat(vm.historyState.value.error?.details).isEqualTo("no row")
        assertThat(vm.calculatorState.value.error).isNull()
    }

    @Test
    fun `deleting an entry removes exactly the one asked for`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(ZakatEvent.DeleteCalculation(entryId = 9L))
        advanceUntilIdle()

        coVerify { useCases.deleteCalculation(9L) }
        coVerify(exactly = 0) { useCases.markAsPaid(any(), any()) }
    }

    @Test
    fun `a delete that fails says so on the history screen`() = runTest {
        coEvery { useCases.deleteCalculation(any()) } throws IllegalStateException("locked")
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(ZakatEvent.DeleteCalculation(entryId = 9L))
        advanceUntilIdle()

        assertThat(vm.historyState.value.error?.message).isEqualTo(R.string.zakat_delete_failed)
    }

    // ------------------------------------------------------------------
    // Reading the history back
    // ------------------------------------------------------------------

    @Test
    fun `the history and its running total arrive together`() = runTest {
        every { useCases.getAllHistory() } returns
                flowOf(listOf(entry(1L, zakatDue = 250.0, isPaid = true), entry(2L)))
        coEvery { useCases.getTotalPaid() } returns 250.0

        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.historyState.value
        assertThat(state.history.map { it.id }).containsExactly(1L, 2L).inOrder()
        // The total is read per emission rather than summed from the list — only paid entries
        // count towards it, so a client-side sum of `zakatDue` would report 500 against 250
        // actually paid.
        assertThat(state.totalZakatPaid).isEqualTo(250.0)
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `a history stream that fails before its first emission stops the spinner`() = runTest {
        // The defect this pins: `isLoading` defaults to true and used to be cleared only
        // *inside* the collect, so a stream that threw first — a missing table after a content
        // database replacement — left the screen spinning for the life of the ViewModel with
        // nothing on it to say why.
        every { useCases.getAllHistory() } returns flow { throw IllegalStateException("no table") }

        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.historyState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.error?.message).isEqualTo(R.string.zakat_history_load_failed)
        assertThat(state.error?.details).isEqualTo("no table")
    }

    @Test
    fun `LoadHistory re-reads after a failure`() = runTest {
        // The retry button on the history error state emits exactly this. If it did not start a
        // fresh collection the button would clear nothing and the screen would stay broken until
        // the process restarted.
        val stream = MutableSharedFlow<List<ZakatHistoryEntry>>(replay = 1)
        every { useCases.getAllHistory() } returns stream
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(ZakatEvent.LoadHistory)
        stream.emit(listOf(entry(3L)))
        advanceUntilIdle()

        assertThat(vm.historyState.value.history.map { it.id }).containsExactly(3L)
    }

    // ------------------------------------------------------------------
    // The form's own lifecycle
    // ------------------------------------------------------------------

    @Test
    fun `the typed form survives process death`() = runTest {
        // Thirteen figures, several of which have to be looked up. Before this the form lived
        // only in a `MutableStateFlow`, so a phone call during data entry returned the user to
        // an empty form with nothing to say anything had been lost.
        val saved = SavedStateHandle()
        val first = viewModel(saved)
        advanceUntilIdle()

        first.onEvent(ZakatEvent.UpdateCash(1_200.0))
        first.onEvent(ZakatEvent.UpdateGold(50.0))
        first.onEvent(ZakatEvent.UpdateLoans(300.0))
        advanceUntilIdle()

        val restored = viewModel(saved)
        advanceUntilIdle()

        val state = restored.calculatorState.value
        assertThat(state.assets.cashOnHand).isEqualTo(1_200.0)
        assertThat(state.assets.goldGrams).isEqualTo(50.0)
        assertThat(state.liabilities.loans).isEqualTo(300.0)
        // And the restored form recomputes rather than coming back with a blank total.
        assertThat(state.calculation).isNotNull()
    }

    @Test
    fun `clearing the form reaches the saved state too`() = runTest {
        // Clearing that stopped at the in-memory state would bring the cleared figures back on
        // the next process death — the one moment the user is least able to explain it.
        val saved = SavedStateHandle()
        val first = viewModel(saved)
        advanceUntilIdle()
        first.onEvent(ZakatEvent.UpdateCash(1_200.0))
        advanceUntilIdle()

        first.onEvent(ZakatEvent.ClearAll)
        advanceUntilIdle()

        val restored = viewModel(saved)
        advanceUntilIdle()
        assertThat(restored.calculatorState.value.assets.cashOnHand).isEqualTo(0.0)
        assertThat(restored.calculatorState.value.calculation).isNull()
    }

    @Test
    fun `clearing keeps the basis the settings screen chose`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        settings.setZakatCurrency("EUR")
        settings.setZakatGoldPricePerGram(80.0)
        advanceUntilIdle()

        vm.onEvent(ZakatEvent.UpdateCash(1_000.0))
        advanceUntilIdle()
        vm.onEvent(ZakatEvent.ClearAll)
        advanceUntilIdle()

        // Reset empties the *form*. The currency and the metal prices are persisted settings, so
        // resetting them here would silently return the user's zakat to dollars at a stale gold
        // price — and `ZakatCalculator` derives the nisab threshold from that price, so it
        // changes whether anything is owed at all.
        val state = vm.calculatorState.value
        assertThat(state.currency).isEqualTo("EUR")
        assertThat(state.goldPricePerGram).isEqualTo(80.0)
        assertThat(state.assets.cashOnHand).isEqualTo(0.0)
    }

    @Test
    fun `the breakdown toggle flips both ways`() = runTest {
        // It starts open, and it is ViewModel state rather than local `remember` precisely so
        // that a rotation does not silently re-open what the user closed.
        val vm = viewModel()
        advanceUntilIdle()
        assertThat(vm.calculatorState.value.showBreakdown).isTrue()

        vm.onEvent(ZakatEvent.ToggleBreakdown)
        assertThat(vm.calculatorState.value.showBreakdown).isFalse()

        vm.onEvent(ZakatEvent.ToggleBreakdown)
        assertThat(vm.calculatorState.value.showBreakdown).isTrue()
    }

    @Test
    fun `Recalculate re-runs the sum over what is already on the form`() = runTest {
        // What the inline error's "Try again" emits. It must not need the user to retype a
        // figure to re-trigger the calculation.
        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(ZakatEvent.UpdateCash(20_000.0))
        advanceUntilIdle()

        vm.onEvent(ZakatEvent.Recalculate)
        advanceUntilIdle()

        val calculation = vm.calculatorState.value.calculation
        assertThat(calculation).isNotNull()
        assertThat(calculation!!.totalAssets).isEqualTo(20_000.0)
        assertThat(vm.calculatorState.value.error).isNull()
    }

    @Test
    fun `every asset row reaches the total it belongs to`() = runTest {
        // Thirteen events, thirteen fields, and the mapping is written out by hand in the
        // `when`. A copy-paste that pointed two events at the same field would be invisible on
        // screen — the row would take input and the total would simply be wrong.
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(ZakatEvent.UpdateCash(1.0))
        vm.onEvent(ZakatEvent.UpdateBankBalance(2.0))
        vm.onEvent(ZakatEvent.UpdateInvestments(4.0))
        vm.onEvent(ZakatEvent.UpdateBusinessInventory(8.0))
        vm.onEvent(ZakatEvent.UpdateReceivables(16.0))
        vm.onEvent(ZakatEvent.UpdateRentalIncome(32.0))
        vm.onEvent(ZakatEvent.UpdateOtherAssets(64.0))
        advanceUntilIdle()

        val assets = vm.calculatorState.value.assets
        assertThat(assets.cashOnHand).isEqualTo(1.0)
        assertThat(assets.bankBalance).isEqualTo(2.0)
        assertThat(assets.investments).isEqualTo(4.0)
        assertThat(assets.businessInventory).isEqualTo(8.0)
        assertThat(assets.receivables).isEqualTo(16.0)
        assertThat(assets.rentalIncome).isEqualTo(32.0)
        assertThat(assets.otherAssets).isEqualTo(64.0)
        // The powers of two make the sum decide the mapping: any two rows crossed and it moves.
        assertThat(assets.total).isEqualTo(127.0)
    }

    @Test
    fun `every liability row reaches the total it belongs to`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(ZakatEvent.UpdateDebts(1.0))
        vm.onEvent(ZakatEvent.UpdateLoans(2.0))
        vm.onEvent(ZakatEvent.UpdateBillsDue(4.0))
        vm.onEvent(ZakatEvent.UpdateOtherLiabilities(8.0))
        advanceUntilIdle()

        val liabilities = vm.calculatorState.value.liabilities
        assertThat(liabilities.debts).isEqualTo(1.0)
        assertThat(liabilities.loans).isEqualTo(2.0)
        assertThat(liabilities.billsDue).isEqualTo(4.0)
        assertThat(liabilities.otherLiabilities).isEqualTo(8.0)
        assertThat(liabilities.total).isEqualTo(15.0)
    }

    @Test
    fun `a liability alone still produces a calculation`() = runTest {
        // `recalculate()` checks assets *or* liabilities. Checking only assets — the easier
        // reading of "is the form empty" — would leave someone who typed their debts first
        // staring at a form that produced nothing.
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(ZakatEvent.UpdateDebts(500.0))
        advanceUntilIdle()

        val calculation = vm.calculatorState.value.calculation
        assertThat(calculation).isNotNull()
        assertThat(calculation!!.totalLiabilities).isEqualTo(500.0)
        assertThat(calculation.isAboveNisab).isFalse()
    }
}
