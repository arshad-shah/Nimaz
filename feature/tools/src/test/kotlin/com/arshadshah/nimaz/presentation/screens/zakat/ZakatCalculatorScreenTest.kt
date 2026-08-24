package com.arshadshah.nimaz.presentation.screens.zakat

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.common.currencySymbolOf
import com.arshadshah.nimaz.core.common.formatCurrency
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.NisabType
import com.arshadshah.nimaz.domain.model.ZakatAssets
import com.arshadshah.nimaz.domain.model.ZakatCalculation
import com.arshadshah.nimaz.domain.model.ZakatLiabilities
import com.arshadshah.nimaz.presentation.viewmodel.UiError
import com.arshadshah.nimaz.presentation.viewmodel.tools.ZakatCalculatorUiState
import com.arshadshah.nimaz.presentation.viewmodel.tools.ZakatEvent
import com.arshadshah.nimaz.presentation.viewmodel.tools.ZakatViewModel
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The zakat calculator: thirteen inputs, one derived total, and a threshold that flips the answer
 * between "you owe this" and "you owe nothing".
 *
 * **The arithmetic is not what is tested here** — `ZakatCalculator` has its own tests in
 * `:core:domain`, and this screen never does a sum. What is tested is everything between the user
 * and that calculation, none of which any ViewModel test can see:
 *
 *  - that a figure typed into a row reaches the event for *that* row (thirteen near-identical
 *    `InputCard` call sites, where a copy-paste sends someone's gold into their bank balance and
 *    nothing on screen looks wrong);
 *  - that the symbol on the fields is the one beside the total, and that gold and silver are
 *    weights rather than money — the field this replaced picked between the two by comparing its
 *    suffix against the string `"$"`, so every non-dollar currency rendered a dollar sign;
 *  - that the nisab basis is *reported* on the form, because a reader who cannot see the
 *    threshold cannot tell why the total says zero;
 *  - that a failed calculation is reported **inline**, with the form intact, rather than by
 *    replacing a screen holding an afternoon of typed figures.
 *
 * The ViewModel is a relaxed mock over real `MutableStateFlow`s, per #604: the screen takes it as
 * a parameter, so nothing here needs Hilt. The tall `@Config` is the LazyColumn — a phone-height
 * viewport composes a screenful and the asset rows below the fold never exist.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class ZakatCalculatorScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val state = MutableStateFlow(ZakatCalculatorUiState())
    private val events = mutableListOf<ZakatEvent>()
    private var backs = 0
    private var historyOpens = 0
    private var settingsOpens = 0

    private val viewModel: ZakatViewModel = mockk(relaxed = true) {
        every { calculatorState } returns this@ZakatCalculatorScreenTest.state
        every { onEvent(any()) } answers { events += firstArg<ZakatEvent>() }
    }

    private fun str(@StringRes id: Int): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id)

    private fun str(@StringRes id: Int, vararg args: Any): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id, *args)

    private fun render() {
        composeRule.setThemedContent {
            ZakatCalculatorScreen(
                onNavigateBack = { backs++ },
                onNavigateToHistory = { historyOpens++ },
                onNavigateToSettings = { settingsOpens++ },
                viewModel = viewModel,
            )
        }
        composeRule.waitForIdle()
    }

    /** A finished calculation, above the gold nisab unless told otherwise. */
    private fun calculation(
        totalAssets: Double = 20_000.0,
        totalLiabilities: Double = 2_000.0,
        isAboveNisab: Boolean = true,
        nisabType: NisabType = NisabType.GOLD,
        nisabValue: Double = 5_686.2,
    ) = ZakatCalculation(
        calculatedAt = 1_700_000_000_000L,
        totalAssets = totalAssets,
        totalLiabilities = totalLiabilities,
        netWorth = totalAssets - totalLiabilities,
        nisabType = nisabType,
        nisabValue = nisabValue,
        isAboveNisab = isAboveNisab,
        zakatDue = if (isAboveNisab) (totalAssets - totalLiabilities) * 0.025 else 0.0,
    )

    /** Every value dispatched by events of type [T], in order. */
    private inline fun <reified T : ZakatEvent> amountsOf(select: (T) -> Double): List<Double> =
        events.filterIsInstance<T>().map(select)

    /**
     * The plinth's spoken form — "Zakat Due, $450.00, Above nisab".
     *
     * `ZakatSummaryHero` puts `clearAndSetSemantics` over the whole plinth so that the eyebrow,
     * the figure and the verdict are announced as one phrase instead of three, which means this
     * description is both the accessibility contract and the only way a test can address it.
     */
    private fun plinth(amount: String, above: Boolean) = str(
        R.string.zakat_a11y_plinth_status_format,
        str(R.string.zakat_due),
        amount,
        str(if (above) R.string.zakat_status_above_nisab else R.string.zakat_status_below_nisab),
    )

    /** Opens the "Deducted" accordion, which starts closed — assets is the one that starts open. */
    private fun openLiabilities() {
        composeRule.onNodeWithText(str(R.string.zakat_section_deducted)).performClick()
        composeRule.waitForIdle()
    }

    // ------------------------------------------------------------------
    // The form reaches the right event
    // ------------------------------------------------------------------

    @Test
    fun `every asset row is on the form, labelled and hinted`() {
        render()

        // Nine rows, and the hint is what distinguishes two that otherwise read alike — "Gold"
        // wants grams and "Investments" wants money, and only the hint says so.
        composeRule.onNodeWithText(str(R.string.cash_on_hand)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.bank_balance)).assertIsDisplayed()
        composeRule.onAllNodesWithText(str(R.string.gold)).onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText(str(R.string.silver)).onFirst().assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.investments)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.business_inventory)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.receivables)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.rental_income)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.other_assets)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.hint_weight_in_grams)).assertExists()
    }

    @Test
    fun `every liability row is under the deducted section`() {
        render()

        // Closed by default, so none of these exists until the header is tapped — which is the
        // arrangement being asserted as much as the rows themselves.
        composeRule.onAllNodesWithText(str(R.string.debts_owed)).assertCountEquals(0)

        openLiabilities()

        composeRule.onNodeWithText(str(R.string.debts_owed)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.loans)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.bills_due)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.other_liabilities)).assertIsDisplayed()
    }

    @Test
    fun `typing into the cash row updates cash and nothing else`() {
        // The row-to-event mapping is thirteen near-identical call sites. Crossing two of them
        // takes input in the right box and files it against the wrong asset — the total moves,
        // the screen looks correct, and only the breakdown disagrees.
        render()

        fieldBeside(str(R.string.cash_on_hand)).performTextInput("1200")
        composeRule.waitForIdle()

        val updates = events.filterIsInstance<ZakatEvent.UpdateCash>()
        assertThat(updates.map { it.amount }).contains(1200.0)
        assertThat(events.none { it is ZakatEvent.UpdateBankBalance }).isTrue()
    }

    @Test
    fun `every asset row files its figure against its own asset`() {
        // Nine `InputCard` call sites that differ only in which event they construct. A
        // copy-paste between any two takes input in the right box and files it against the wrong
        // asset: the row accepts the figure, the screen looks correct, and the total is wrong by
        // however far the two rows differ. Distinct values per row make any crossing visible.
        render()

        fieldBeside(str(R.string.cash_on_hand)).performTextInput("11")
        fieldBeside(str(R.string.bank_balance)).performTextInput("22")
        fieldBeside(str(R.string.gold)).performTextInput("33")
        fieldBeside(str(R.string.silver)).performTextInput("44")
        fieldBeside(str(R.string.investments)).performTextInput("55")
        fieldBeside(str(R.string.business_inventory)).performTextInput("66")
        fieldBeside(str(R.string.receivables)).performTextInput("77")
        fieldBeside(str(R.string.rental_income)).performTextInput("88")
        fieldBeside(str(R.string.other_assets)).performTextInput("99")
        composeRule.waitForIdle()

        assertThat(amountsOf<ZakatEvent.UpdateCash> { it.amount }).contains(11.0)
        assertThat(amountsOf<ZakatEvent.UpdateBankBalance> { it.amount }).contains(22.0)
        assertThat(amountsOf<ZakatEvent.UpdateGold> { it.grams }).contains(33.0)
        assertThat(amountsOf<ZakatEvent.UpdateSilver> { it.grams }).contains(44.0)
        assertThat(amountsOf<ZakatEvent.UpdateInvestments> { it.amount }).contains(55.0)
        assertThat(amountsOf<ZakatEvent.UpdateBusinessInventory> { it.amount }).contains(66.0)
        assertThat(amountsOf<ZakatEvent.UpdateReceivables> { it.amount }).contains(77.0)
        assertThat(amountsOf<ZakatEvent.UpdateRentalIncome> { it.amount }).contains(88.0)
        assertThat(amountsOf<ZakatEvent.UpdateOtherAssets> { it.amount }).contains(99.0)
    }

    @Test
    fun `every liability row files its figure against its own liability`() {
        render()
        openLiabilities()

        fieldBeside(str(R.string.debts_owed)).performTextInput("11")
        fieldBeside(str(R.string.loans)).performTextInput("22")
        fieldBeside(str(R.string.bills_due)).performTextInput("33")
        fieldBeside(str(R.string.other_liabilities)).performTextInput("44")
        composeRule.waitForIdle()

        assertThat(amountsOf<ZakatEvent.UpdateDebts> { it.amount }).contains(11.0)
        assertThat(amountsOf<ZakatEvent.UpdateLoans> { it.amount }).contains(22.0)
        assertThat(amountsOf<ZakatEvent.UpdateBillsDue> { it.amount }).contains(33.0)
        assertThat(amountsOf<ZakatEvent.UpdateOtherLiabilities> { it.amount }).contains(44.0)
        // And none of them landed on the asset side, which would add the debt to the wealth
        // being taxed instead of subtracting it — the sign error that doubles someone's zakat.
        assertThat(events.none { it is ZakatEvent.UpdateCash }).isTrue()
    }

    @Test
    fun `silver is entered as grams too`() {
        // The other weight row. The silver nisab works out roughly an order of magnitude below
        // the gold one, so it is the basis that applies to more people — a silver row that took
        // a currency amount would be wrong for the majority of the users who choose it.
        render()

        fieldBeside(str(R.string.silver)).performTextInput("612.36")
        composeRule.waitForIdle()

        assertThat(amountsOf<ZakatEvent.UpdateSilver> { it.grams }).contains(612.36)
    }

    @Test
    fun `gold is entered as grams, not as money`() {
        // Gold and silver are the two rows the calculator stores as *weight*. If they took a
        // currency amount the valuation would be out by the whole gold price — and the nisab
        // threshold is derived from that same price, so it changes whether zakat is owed at all.
        render()

        fieldBeside(str(R.string.gold)).performTextInput("87.48")
        composeRule.waitForIdle()

        assertThat(events.filterIsInstance<ZakatEvent.UpdateGold>().map { it.grams })
            .contains(87.48)
    }

    @Test
    fun `a liability typed into the deducted section becomes a liability event`() {
        render()
        openLiabilities()

        fieldBeside(str(R.string.debts_owed)).performTextInput("500")
        composeRule.waitForIdle()

        assertThat(events.filterIsInstance<ZakatEvent.UpdateDebts>().map { it.amount })
            .contains(500.0)
        // An asset event from a liability row would add the debt to the wealth being taxed
        // rather than subtracting it — the sign error that doubles someone's zakat.
        assertThat(events.none { it is ZakatEvent.UpdateCash }).isTrue()
    }

    // ------------------------------------------------------------------
    // The currency the user chose
    // ------------------------------------------------------------------

    @Test
    fun `the fields and the total agree on the currency`() {
        // `currencySymbolOf` is tested in `:core:common`. What is untested — and what actually
        // shipped wrong — is the screen using it for *both*: the money fields lead with the
        // symbol and the hero renders the same currency, so a euro form must not carry dollar
        // prefixes over a euro total.
        state.value = ZakatCalculatorUiState(
            currency = "EUR",
            assets = ZakatAssets(cashOnHand = 20_000.0),
            calculation = calculation().copy(currency = "EUR"),
        )
        render()

        composeRule.onAllNodesWithText(currencySymbolOf("EUR")).onFirst().assertExists()
        composeRule.onAllNodesWithText(formatCurrency(450.0, "EUR")).onFirst().assertExists()
        // And no dollar total anywhere: the bug this replaces formatted every zakat figure with
        // `$` whatever the user had chosen.
        composeRule.onAllNodesWithText(formatCurrency(450.0, "USD")).assertCountEquals(0)
    }

    @Test
    fun `a weight row carries its unit instead of a currency symbol`() {
        state.value = ZakatCalculatorUiState(currency = "EUR")
        render()

        // "$ 1,200" and "1,200 g" are how each is read. The field these replaced chose between
        // them by comparing its suffix against `"$"`.
        composeRule.onAllNodesWithText(str(R.string.zakat_unit_grams)).onFirst().assertExists()
    }

    // ------------------------------------------------------------------
    // The nisab threshold
    // ------------------------------------------------------------------

    @Test
    fun `the basis is reported on the form before anything is typed`() {
        // `nisabValue` is derived on the state even with an empty form, precisely so this row can
        // say what the threshold is. Someone who cannot see it cannot tell why their total is 0.
        state.value = ZakatCalculatorUiState(nisabType = NisabType.GOLD, currency = "USD")
        render()

        composeRule.onNodeWithText(str(R.string.zakat_section_nisab)).assertIsDisplayed()
        val expected = str(
            R.string.settings_value_with_qualifier,
            str(R.string.gold),
            formatCurrency(state.value.nisabValue, "USD"),
        )
        composeRule.onNodeWithText(expected).assertIsDisplayed()
    }

    @Test
    fun `switching the basis to silver changes the threshold the form reports`() {
        // The silver nisab works out roughly an order of magnitude lower than the gold one, so it
        // applies to far more people. A basis change that did not reach this row would leave the
        // screen quoting a threshold the calculation is not using.
        state.value = ZakatCalculatorUiState(nisabType = NisabType.GOLD)
        render()
        val goldThreshold = state.value.nisabValue

        state.value = ZakatCalculatorUiState(nisabType = NisabType.SILVER)
        composeRule.waitForIdle()

        val silverThreshold = state.value.nisabValue
        assertThat(silverThreshold).isNotEqualTo(goldThreshold)
        composeRule.onNodeWithText(
            str(
                R.string.settings_value_with_qualifier,
                str(R.string.silver),
                formatCurrency(silverThreshold, "USD"),
            )
        ).assertIsDisplayed()
    }

    @Test
    fun `the basis row opens the settings screen rather than editing in place`() {
        // Deliberate: the basis and the metal prices are persisted preferences, not figures typed
        // per calculation, and they used to be an accordion in the middle of thirteen inputs.
        render()

        composeRule.onNodeWithText(str(R.string.zakat_section_nisab)).performClick()
        composeRule.waitForIdle()

        assertThat(settingsOpens).isEqualTo(1)
    }

    // ------------------------------------------------------------------
    // The result the user is reading
    // ------------------------------------------------------------------

    @Test
    fun `above the nisab the hero states the amount owed and says so`() {
        state.value = ZakatCalculatorUiState(calculation = calculation(isAboveNisab = true))
        render()

        // The plinth is read as one phrase — `ZakatSummaryHero` puts `clearAndSetSemantics` over
        // it so TalkBack says "Zakat Due, $450.00, Above nisab" rather than three loose fragments,
        // which is also the only handle a test has on it.
        composeRule.onNodeWithContentDescription(plinth(formatCurrency(450.0, "USD"), above = true))
            .assertIsDisplayed()
    }

    @Test
    fun `the hero's tiles carry the net wealth and the threshold it was measured against`() {
        // The three figures that make the headline checkable: without the net wealth and the
        // threshold beside it, "$450.00" is a number the user has to take on trust.
        state.value = ZakatCalculatorUiState(calculation = calculation())
        render()

        composeRule.onNodeWithContentDescription(
            str(R.string.zakat_a11y_stat_format, str(R.string.zakat_stat_net), formatCurrency(18_000.0, "USD"))
        ).assertExists()
        composeRule.onNodeWithContentDescription(
            str(
                R.string.zakat_a11y_stat_format,
                str(R.string.zakat_stat_nisab_format, str(R.string.gold)),
                formatCurrency(5_686.2, "USD"),
            )
        ).assertExists()
    }

    @Test
    fun `below the nisab the hero says why the figure is zero`() {
        // The one place the screen has to explain itself. "2.5% of eligible wealth" over a $0.00
        // reads as a broken calculation; "no zakat is due below the nisab threshold" is the
        // answer, and it is a different string on a different branch.
        state.value = ZakatCalculatorUiState(
            calculation = calculation(totalAssets = 100.0, isAboveNisab = false)
        )
        render()

        composeRule.onNodeWithContentDescription(plinth(formatCurrency(0.0, "USD"), above = false))
            .assertIsDisplayed()
        // And emphatically not the other verdict: a hero that said "Above nisab" over a zero
        // would read as a calculation that had failed rather than as an answer.
        composeRule.onAllNodesWithContentDescription(
            plinth(formatCurrency(0.0, "USD"), above = true)
        ).assertCountEquals(0)
    }

    @Test
    fun `the breakdown shows the working the total came from`() {
        state.value = ZakatCalculatorUiState(
            calculation = calculation(),
            showBreakdown = true,
        )
        render()

        composeRule.onNodeWithText(str(R.string.total_assets)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.total_liabilities)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.net_zakatable_wealth)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.nisab_threshold)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.meets_nisab)).assertIsDisplayed()
        // Liabilities are rendered as a subtraction. A bare figure in a column of additions
        // reads as wealth rather than as something taken off it.
        composeRule.onNodeWithText("- ${formatCurrency(2_000.0, "USD")}").assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.yes)).assertIsDisplayed()
    }

    @Test
    fun `a collapsed breakdown keeps its header and drops its rows`() {
        // `showBreakdown` is ViewModel state rather than local `remember`, so that the choice
        // survives rotation. The header has to stay: a section that vanishes entirely is not
        // collapsed, it is missing.
        state.value = ZakatCalculatorUiState(calculation = calculation(), showBreakdown = false)
        render()

        composeRule.onNodeWithText(str(R.string.calculation_breakdown)).assertIsDisplayed()
        composeRule.onAllNodesWithText(str(R.string.total_assets)).assertCountEquals(0)
    }

    @Test
    fun `tapping the breakdown header asks the ViewModel to toggle it`() {
        state.value = ZakatCalculatorUiState(calculation = calculation(), showBreakdown = true)
        render()

        composeRule.onNodeWithText(str(R.string.calculation_breakdown)).performClick()
        composeRule.waitForIdle()

        assertThat(events).contains(ZakatEvent.ToggleBreakdown)
    }

    @Test
    fun `an empty form shows no breakdown at all`() {
        // The defect this pins: clearing the last field used to leave the previous calculation on
        // screen, so the user read a zakat figure over a blank form — and the redesign pins that
        // total to a sticky hero, which would have made it permanent.
        state.value = ZakatCalculatorUiState(calculation = null)
        render()

        composeRule.onAllNodesWithText(str(R.string.calculation_breakdown)).assertCountEquals(0)
    }

    // ------------------------------------------------------------------
    // Failure, reported without losing the form
    // ------------------------------------------------------------------

    @Test
    fun `a failed calculation is reported inline with every figure still on the form`() {
        state.value = ZakatCalculatorUiState(
            assets = ZakatAssets(cashOnHand = 20_000.0),
            liabilities = ZakatLiabilities(debts = 2_000.0),
            error = UiError(message = R.string.zakat_calculate_failed, details = "overflow"),
        )
        render()

        composeRule.onNodeWithText(str(R.string.zakat_calculate_failed)).assertIsDisplayed()
        // The form is still a form. An error state that replaced the screen would take an
        // afternoon of asset entry with it, and no button brings that back.
        composeRule.onNodeWithText(str(R.string.cash_on_hand)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.zakat_section_nisab)).assertIsDisplayed()
    }

    @Test
    fun `try again re-runs the sum over what is already typed`() {
        state.value = ZakatCalculatorUiState(
            assets = ZakatAssets(cashOnHand = 20_000.0),
            error = UiError(message = R.string.zakat_calculate_failed),
        )
        render()

        composeRule.onNodeWithText(str(R.string.try_again)).performClick()
        composeRule.waitForIdle()

        // Recalculate, not a clear: the retry must not ask the user to retype anything.
        assertThat(events).contains(ZakatEvent.Recalculate)
    }

    // ------------------------------------------------------------------
    // The action bar
    // ------------------------------------------------------------------

    @Test
    fun `save and share are disabled until there is something to act on`() {
        // Saving a calculation that does not exist writes an empty row; sharing one shares a card
        // of zeroes. Both are worse than a button that does nothing.
        state.value = ZakatCalculatorUiState(calculation = null)
        render()

        composeRule.onNodeWithText(str(R.string.zakat_save_this_year)).assertIsNotEnabled()
        composeRule.onNodeWithContentDescription(str(R.string.zakat_share)).assertIsNotEnabled()
    }

    @Test
    fun `saving a finished calculation asks the ViewModel to write it`() {
        state.value = ZakatCalculatorUiState(calculation = calculation())
        render()

        composeRule.onNodeWithText(str(R.string.zakat_save_this_year)).assertIsEnabled()
        composeRule.onNodeWithText(str(R.string.zakat_save_this_year)).performClick()
        composeRule.waitForIdle()

        assertThat(events).contains(ZakatEvent.SaveCalculation)
    }

    @Test
    fun `share is offered once there is a calculation and does not go through the ViewModel`() {
        // Sharing is a composition-scoped action, not an event: the card is rendered from state
        // the screen already holds. What matters is that it becomes available in step with save —
        // an enabled share over a null calculation is what produces a card of zeroes.
        state.value = ZakatCalculatorUiState(calculation = calculation())
        render()

        composeRule.onNodeWithContentDescription(str(R.string.zakat_share)).assertIsEnabled()
        composeRule.onNodeWithContentDescription(str(R.string.zakat_share)).performClick()
        composeRule.waitForIdle()

        assertThat(events.none { it is ZakatEvent.SaveCalculation }).isTrue()
    }

    // ------------------------------------------------------------------
    // The top bar
    // ------------------------------------------------------------------

    @Test
    fun `the top bar offers reset, history and settings`() {
        render()

        composeRule.onNodeWithContentDescription(str(R.string.cd_reset)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(str(R.string.cd_history)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(str(R.string.zakat_settings)).assertIsDisplayed()
    }

    @Test
    fun `reset clears the form through the ViewModel`() {
        render()

        composeRule.onNodeWithContentDescription(str(R.string.cd_reset)).performClick()
        composeRule.waitForIdle()

        assertThat(events).contains(ZakatEvent.ClearAll)
    }

    @Test
    fun `history and settings navigate rather than emitting events`() {
        // Both are navigation, and routing either through `onEvent` would put a destination
        // decision inside the ViewModel — the coupling `NavControllerConfinementTest` exists for.
        render()

        composeRule.onNodeWithContentDescription(str(R.string.cd_history)).performClick()
        composeRule.onNodeWithContentDescription(str(R.string.zakat_settings)).performClick()
        composeRule.waitForIdle()

        assertThat(historyOpens).isEqualTo(1)
        assertThat(settingsOpens).isEqualTo(1)
        assertThat(events).isEmpty()
    }

    @Test
    fun `back leaves the calculator`() {
        render()

        composeRule.onAllNodesWithContentDescription(str(R.string.cd_back)).onFirst()
            .performClick()
        composeRule.waitForIdle()

        assertThat(backs).isEqualTo(1)
    }

    // ------------------------------------------------------------------
    // Subtotals
    // ------------------------------------------------------------------

    @Test
    fun `the assets header values gold and silver rather than counting their grams`() {
        // `assets.total` counts only the cash-like rows — gold and silver are stored as *grams*.
        // Without the two multiplications the header reports a figure that disagrees with the
        // breakdown by the whole value of someone's jewellery.
        state.value = ZakatCalculatorUiState(
            assets = ZakatAssets(cashOnHand = 1_000.0, goldGrams = 10.0, silverGrams = 100.0),
            goldPricePerGram = 65.0,
            silverPricePerGram = 0.80,
        )
        render()

        // 1,000 + (10 × 65) + (100 × 0.80) = 1,730 — not the 1,110 a grams-as-money sum gives.
        composeRule.onAllNodesWithText(formatCurrency(1_730.0, "USD")).onFirst().assertExists()
        composeRule.onAllNodesWithText(formatCurrency(1_110.0, "USD")).assertCountEquals(0)
    }

    @Test
    fun `the deducted header carries the liabilities subtotal`() {
        state.value = ZakatCalculatorUiState(
            liabilities = ZakatLiabilities(debts = 300.0, loans = 200.0, billsDue = 100.0),
        )
        render()

        composeRule.onAllNodesWithText(formatCurrency(600.0, "USD")).onFirst().assertExists()
    }

    // ------------------------------------------------------------------
    // The hero above a scrolling form
    // ------------------------------------------------------------------

    @Test
    // A phone-height viewport, unlike the rest of this class: the collapse is driven by the
    // list's scroll offset, and on a 2,200dp screen the whole form fits, so there is nothing to
    // scroll and the trigger never fires.
    @Config(qualifiers = "w411dp-h891dp")
    fun `scrolling the form collapses the hero and returning to the top restores it`() {
        // The asymmetry is the fix, not a nicety. The hero sits above the list and the list takes
        // the height the hero leaves, so the trigger reads a scroll position its own outcome
        // changes. With one symmetric threshold that is a loop — collapse frees height, the list
        // clamps its offset back under the threshold, the hero expands, the offset goes back over
        // it — which presented as a hero that simply never collapsed. Re-expanding only at a true
        // top breaks it, because clamping can lower an offset but never raise it.
        state.value = ZakatCalculatorUiState(calculation = calculation())
        render()

        // Expanded: the three tiles are the part that folds away.
        composeRule.onNodeWithContentDescription(netTile()).assertExists()

        composeRule.onNode(hasScrollAction()).performScrollToIndex(3)
        composeRule.waitForIdle()

        composeRule.onAllNodesWithContentDescription(netTile()).assertCountEquals(0)
        // The amount itself never goes: on this screen it is the one figure the whole task is
        // about, and losing sight of it mid-entry is what a hero that scrolled away got wrong.
        composeRule.onNodeWithContentDescription(plinth(formatCurrency(450.0, "USD"), above = true))
            .assertExists()

        composeRule.onNode(hasScrollAction()).performScrollToIndex(0)
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription(netTile()).assertExists()
    }

    /** The "Net" tile's spoken form — present only while the hero is expanded. */
    private fun netTile() = str(
        R.string.zakat_a11y_stat_format,
        str(R.string.zakat_stat_net),
        formatCurrency(18_000.0, "USD"),
    )

    // ------------------------------------------------------------------
    // The wide layout
    // ------------------------------------------------------------------

    @Test
    @Config(qualifiers = "w1000dp-h1200dp")
    fun `the tablet layout puts assets and liabilities side by side, both open`() {
        // The wide layout has no accordions: both columns are `NimazSectionHeader` + rows, so
        // every liability row exists without anything being tapped. That is the difference the
        // size class makes, and it is the arm no compact test reaches.
        state.value = ZakatCalculatorUiState(calculation = calculation())
        render()

        composeRule.onNodeWithText(str(R.string.assets)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.liabilities)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.cash_on_hand)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.debts_owed)).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w1000dp-h1200dp")
    fun `the tablet layout keeps the same action bar`() {
        // One place to look for save and share whatever the size class. A wide layout that
        // dropped them would leave a finished calculation with no way to keep it.
        state.value = ZakatCalculatorUiState(calculation = calculation())
        render()

        // The wide layout scrolls as one column, so the bar sits below the fold on a form this
        // long — reaching it is part of what is being asserted.
        composeRule.onNodeWithText(str(R.string.zakat_save_this_year)).performScrollTo()
            .assertIsEnabled()
            .performClick()
        composeRule.waitForIdle()

        assertThat(events).contains(ZakatEvent.SaveCalculation)
    }

    @Test
    @Config(qualifiers = "w1000dp-h1200dp")
    fun `the tablet layout reports the nisab basis and opens settings from it`() {
        state.value = ZakatCalculatorUiState(nisabType = NisabType.SILVER)
        render()

        composeRule.onNodeWithText(str(R.string.zakat_section_nisab)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.zakat_section_nisab)).performClick()
        composeRule.waitForIdle()

        assertThat(settingsOpens).isEqualTo(1)
    }

    @Test
    @Config(qualifiers = "w1000dp-h1200dp")
    fun `the tablet layout still draws the breakdown and takes input`() {
        state.value = ZakatCalculatorUiState(calculation = calculation(), showBreakdown = true)
        render()

        composeRule.onNodeWithText(str(R.string.net_zakatable_wealth)).performScrollTo()
            .assertIsDisplayed()

        fieldBeside(str(R.string.cash_on_hand)).performTextInput("750")
        composeRule.waitForIdle()

        assertThat(events.filterIsInstance<ZakatEvent.UpdateCash>().map { it.amount })
            .contains(750.0)
    }

    /**
     * The amount field belonging to the row carrying [label].
     *
     * Rows are `NimazIconWell` + label/hint + `NimazAmountField`, and the fields carry no text of
     * their own until something is typed — so they are addressed positionally, by index among all
     * editable nodes, in the order the rows are declared.
     */
    private fun fieldBeside(label: String) = composeRule.onAllNodes(
        hasSetTextAction(),
        useUnmergedTree = true,
    )[fieldIndex(label)]

    private fun fieldIndex(label: String): Int = when (label) {
        str(R.string.cash_on_hand) -> 0
        str(R.string.bank_balance) -> 1
        str(R.string.gold) -> 2
        str(R.string.silver) -> 3
        str(R.string.investments) -> 4
        str(R.string.business_inventory) -> 5
        str(R.string.receivables) -> 6
        str(R.string.rental_income) -> 7
        str(R.string.other_assets) -> 8
        // The liability rows compose after the asset ones in both layouts.
        str(R.string.debts_owed) -> 9
        str(R.string.loans) -> 10
        str(R.string.bills_due) -> 11
        str(R.string.other_liabilities) -> 12
        else -> error("no input row labelled $label")
    }
}
