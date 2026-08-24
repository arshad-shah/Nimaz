package com.arshadshah.nimaz.presentation.screens.zakat

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.common.formatCurrency
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.NisabType
import com.arshadshah.nimaz.domain.model.ZakatHistoryEntry
import com.arshadshah.nimaz.presentation.viewmodel.UiError
import com.arshadshah.nimaz.presentation.viewmodel.tools.ZakatEvent
import com.arshadshah.nimaz.presentation.viewmodel.tools.ZakatHistoryUiState
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
 * The zakat history: the record of what was worked out, and what has actually been paid.
 *
 * Three states share one screen and their order is the whole design. A failed read leaves the list
 * empty too, so if the empty branch were checked first the app would tell someone with years of
 * records that they have none — the worst available answer, because it reads as data loss rather
 * than as a load that failed. The error branch is deliberately first, and that ordering is a
 * rendering fact no ViewModel test can see.
 *
 * The rest is the per-entry card: a paid badge that must track `isPaid` rather than the presence
 * of a payment date, and a "mark as paid" action that must not be offered on something already
 * paid — an obligation discharged twice is a real loss to whoever discharged it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class ZakatHistoryScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val state = MutableStateFlow(ZakatHistoryUiState(isLoading = false))
    private val events = mutableListOf<ZakatEvent>()
    private var backs = 0
    private var calculatorOpens = 0

    private val viewModel: ZakatViewModel = mockk(relaxed = true) {
        every { historyState } returns this@ZakatHistoryScreenTest.state
        every { onEvent(any()) } answers { events += firstArg<ZakatEvent>() }
    }

    private fun str(@StringRes id: Int): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id)

    private fun str(@StringRes id: Int, vararg args: Any): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id, *args)

    private fun render() {
        composeRule.setThemedContent {
            ZakatHistoryScreen(
                onNavigateBack = { backs++ },
                onNavigateToCalculator = { calculatorOpens++ },
                viewModel = viewModel,
            )
        }
        composeRule.waitForIdle()
    }

    /** 20 March 2026, in the device's default zone — the date the cards render. */
    private val marchTwentieth = 1_774_000_000_000L

    private fun entry(
        id: Long = 1L,
        zakatDue: Double = 450.0,
        netWorth: Double = 18_000.0,
        nisabType: NisabType = NisabType.GOLD,
        nisabValue: Double = 5_686.2,
        isPaid: Boolean = false,
        paidAt: Long? = null,
    ) = ZakatHistoryEntry(
        id = id,
        calculatedAt = marchTwentieth,
        totalAssets = 20_000.0,
        totalLiabilities = 2_000.0,
        netWorth = netWorth,
        zakatDue = zakatDue,
        nisabType = nisabType,
        nisabValue = nisabValue,
        isPaid = isPaid,
        paidAt = paidAt,
    )

    // ------------------------------------------------------------------
    // The three states, in the order that matters
    // ------------------------------------------------------------------

    @Test
    fun `with nothing recorded the screen invites a first calculation`() {
        state.value = ZakatHistoryUiState(isLoading = false)
        render()

        composeRule.onNodeWithText(str(R.string.no_zakat_history)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.zakat_history_empty_message)).assertIsDisplayed()
        // The empty state carries the way out. Without it the screen is a dead end reached from a
        // top-bar icon, with only Back to leave by.
        composeRule.onNodeWithText(str(R.string.zakat_calculate)).assertIsDisplayed()
    }

    @Test
    fun `the empty state's action opens the calculator`() {
        state.value = ZakatHistoryUiState(isLoading = false)
        render()

        composeRule.onNodeWithText(str(R.string.zakat_calculate)).performClick()
        composeRule.waitForIdle()

        assertThat(calculatorOpens).isEqualTo(1)
    }

    @Test
    fun `while loading it says neither empty nor broken`() {
        // `isLoading` starts true, and the empty branch is guarded on it. Dropping that guard puts
        // "No Zakat History" on screen for the length of every read, so a slow disk looks exactly
        // like a wiped history.
        state.value = ZakatHistoryUiState(isLoading = true)
        render()

        composeRule.onAllNodesWithText(str(R.string.no_zakat_history)).assertCountEquals(0)
    }

    @Test
    fun `a failed read is reported as a failure, not as an empty history`() {
        // The ordering this pins: a read that threw also leaves `history` empty, so an empty
        // check placed first would tell someone with years of records that they have none.
        state.value = ZakatHistoryUiState(
            isLoading = false,
            error = UiError(message = R.string.zakat_history_load_failed, details = "no table"),
        )
        render()

        composeRule.onNodeWithText(str(R.string.zakat_history_load_failed)).assertIsDisplayed()
        composeRule.onAllNodesWithText(str(R.string.no_zakat_history)).assertCountEquals(0)
    }

    @Test
    fun `the error offers a retry that re-reads the history`() {
        state.value = ZakatHistoryUiState(
            isLoading = false,
            error = UiError(message = R.string.zakat_history_load_failed),
        )
        render()

        composeRule.onNodeWithText(str(R.string.try_again)).performClick()
        composeRule.waitForIdle()

        assertThat(events).contains(ZakatEvent.LoadHistory)
    }

    // ------------------------------------------------------------------
    // A saved calculation, rendered
    // ------------------------------------------------------------------

    @Test
    fun `a saved calculation renders its own figures`() {
        // Every figure on this card comes off the entry rather than being recomputed, so a record
        // filed under one set of metal prices still reads back as what it was — recomputing would
        // silently restate last year's zakat at today's gold price.
        state.value = ZakatHistoryUiState(
            history = listOf(entry(zakatDue = 450.0, netWorth = 18_000.0)),
            totalZakatPaid = 0.0,
            isLoading = false,
        )
        render()

        composeRule.onAllNodesWithText(formatCurrency(450.0)).onFirst().assertExists()
        composeRule.onNodeWithText(formatCurrency(18_000.0)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.net_worth)).assertIsDisplayed()
        composeRule.onNodeWithText(
            str(R.string.nisab_label_format, "Gold", formatCurrency(5_686.2))
        ).assertIsDisplayed()
    }

    @Test
    fun `the running total counts what was paid, not what was calculated`() {
        // `totalZakatPaid` is read from the repository per emission — only paid entries count
        // towards it. A client-side sum over the list would report the whole history as paid.
        state.value = ZakatHistoryUiState(
            history = listOf(
                entry(id = 1L, zakatDue = 450.0, isPaid = true, paidAt = marchTwentieth),
                entry(id = 2L, zakatDue = 300.0),
            ),
            totalZakatPaid = 450.0,
            isLoading = false,
        )
        render()

        // The plinth is announced as one phrase — `ZakatSummaryHero` clears its children's
        // semantics so TalkBack reads "Total Zakat Paid, $450.00" rather than two fragments,
        // which is also the only handle a test has on it.
        composeRule.onNodeWithContentDescription(
            str(
                R.string.zakat_a11y_stat_format,
                str(R.string.zakat_history_total_paid),
                formatCurrency(450.0),
            )
        ).assertIsDisplayed()
        // 450 paid, not the 750 the two rows add up to.
        composeRule.onAllNodesWithContentDescription(
            str(
                R.string.zakat_a11y_stat_format,
                str(R.string.zakat_history_total_paid),
                formatCurrency(750.0),
            )
        ).assertCountEquals(0)
    }

    @Test
    fun `every recorded calculation gets its own row under the heading`() {
        // The list is keyed by entry id. A key collision — or a `key` taken from anything the
        // rows share — silently collapses several years of records into one card, and the only
        // symptom is a history shorter than it should be.
        state.value = ZakatHistoryUiState(
            history = listOf(entry(id = 1L), entry(id = 2L), entry(id = 3L)),
            isLoading = false,
        )
        render()

        composeRule.onNodeWithText(str(R.string.zakat_history_calculation_history))
            .assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription(str(R.string.cd_delete)).assertCountEquals(3)
    }

    @Test
    fun `an entry filed against the silver basis says silver`() {
        // The basis is stored per entry, so a history spanning a change of madhhab has to report
        // each year against the basis it was actually calculated on — restating an old record
        // under today's basis would misstate what was owed then.
        state.value = ZakatHistoryUiState(
            history = listOf(entry(nisabType = NisabType.SILVER, nisabValue = 489.9)),
            isLoading = false,
        )
        render()

        composeRule.onNodeWithText(
            str(R.string.nisab_label_format, "Silver", formatCurrency(489.9))
        ).assertIsDisplayed()
    }

    // ------------------------------------------------------------------
    // Paid, unpaid, and the action between them
    // ------------------------------------------------------------------

    @Test
    fun `an unpaid entry is badged unpaid and offers to be marked paid`() {
        state.value = ZakatHistoryUiState(history = listOf(entry(isPaid = false)), isLoading = false)
        render()

        composeRule.onNodeWithText(str(R.string.zakat_unpaid)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.zakat_mark_as_paid)).assertIsDisplayed()
    }

    @Test
    fun `a paid entry is badged paid and cannot be paid again`() {
        // The action is behind `if (!entry.isPaid)`. Leaving it up on a paid row invites someone
        // to discharge the same obligation twice, and the app would record it as though they had.
        state.value = ZakatHistoryUiState(
            history = listOf(entry(isPaid = true, paidAt = marchTwentieth)),
            isLoading = false,
        )
        render()

        composeRule.onNodeWithText(str(R.string.zakat_paid)).assertIsDisplayed()
        composeRule.onAllNodesWithText(str(R.string.zakat_mark_as_paid)).assertCountEquals(0)
    }

    @Test
    fun `a paid entry shows when it was paid`() {
        // Rendered from `paidAt`, which is separate from `calculatedAt`: zakat is owed on a lunar
        // year, so the two can be a year apart and labelling the payment with the calculation
        // date would misreport which year's obligation was discharged.
        state.value = ZakatHistoryUiState(
            history = listOf(entry(isPaid = true, paidAt = marchTwentieth)),
            isLoading = false,
        )
        render()

        composeRule.onAllNodesWithText(str(R.string.zakat_paid_on_format, ""), substring = true)
            .onFirst().assertExists()
    }

    @Test
    fun `a paid entry with no recorded date does not invent one`() {
        // `paidAt` is nullable and the line is inside a `?.let`. An entry migrated from a build
        // that did not record the date must show no date rather than "Paid on: 1 Jan 1970".
        state.value = ZakatHistoryUiState(
            history = listOf(entry(isPaid = true, paidAt = null)),
            isLoading = false,
        )
        render()

        composeRule.onNodeWithText(str(R.string.zakat_paid)).assertIsDisplayed()
        composeRule.onAllNodesWithText("1970", substring = true).assertCountEquals(0)
    }

    @Test
    fun `marking an entry paid names that entry`() {
        // The id travels with the event. A card that emitted the wrong one marks a different
        // year's zakat as paid, and the entry the user tapped stays outstanding.
        state.value = ZakatHistoryUiState(
            history = listOf(entry(id = 11L), entry(id = 22L)),
            isLoading = false,
        )
        render()

        composeRule.onAllNodesWithText(str(R.string.zakat_mark_as_paid))[1].performClick()
        composeRule.waitForIdle()

        assertThat(events).containsExactly(ZakatEvent.MarkAsPaid(22L))
    }

    @Test
    fun `deleting an entry names that entry`() {
        state.value = ZakatHistoryUiState(
            history = listOf(entry(id = 11L), entry(id = 22L)),
            isLoading = false,
        )
        render()

        composeRule.onAllNodesWithContentDescription(str(R.string.cd_delete))[0].performClick()
        composeRule.waitForIdle()

        assertThat(events).containsExactly(ZakatEvent.DeleteCalculation(11L))
    }

    // ------------------------------------------------------------------
    // Getting in and out
    // ------------------------------------------------------------------

    @Test
    fun `the FAB opens the calculator`() {
        state.value = ZakatHistoryUiState(history = listOf(entry()), isLoading = false)
        render()

        composeRule.onNodeWithContentDescription(str(R.string.cd_new_calculation)).performClick()
        composeRule.waitForIdle()

        assertThat(calculatorOpens).isEqualTo(1)
        // Navigation, not an event — the destination decision stays out of the ViewModel.
        assertThat(events).isEmpty()
    }

    @Test
    fun `back leaves the history`() {
        render()

        composeRule.onAllNodesWithContentDescription(str(R.string.cd_back)).onFirst().performClick()
        composeRule.waitForIdle()

        assertThat(backs).isEqualTo(1)
    }
}
