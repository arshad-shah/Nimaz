package com.arshadshah.nimaz.presentation.screens.fasting

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.common.formatMediumDate
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.MakeupFast
import com.arshadshah.nimaz.domain.model.MakeupFastStatus
import com.arshadshah.nimaz.presentation.viewmodel.tracker.FastingEvent
import com.arshadshah.nimaz.presentation.viewmodel.tracker.FastingViewModel
import com.arshadshah.nimaz.presentation.viewmodel.tracker.MakeupFastsUiState
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
import java.time.Instant
import java.time.ZoneId

/**
 * What is owed, and the two doors out of owing it.
 *
 * `FastingDaoTest` (#597) already pins that a missed fast leaves a `pending` row and that either
 * completing it or paying fidya clears it. What it cannot pin is the *surface*: 247 lines that
 * decide which section a make-up fast is filed under, which of the two doors a tap opens, and
 * what the sheet sends when it closes. The failure mode is a debt that looks settled and is not,
 * or a fidya payment recorded as a completed fast — both permanent, and neither visible.
 *
 * The split is the load-bearing rule here: `COMPLETED` and `FIDYA_PAID` are different acts, but
 * from the reader's side they answer the same question, so both belong under "Settled" and
 * neither may appear under "Owed".
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class MakeupFastsScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun makeupFast(
        id: Long,
        status: MakeupFastStatus,
        reason: String = "Travel",
        hijri: String? = null,
        completedDate: Long? = null,
        fidya: Double? = null,
    ) = MakeupFast(
        id = id,
        originalDate = 1_700_000_000_000L,
        originalHijriDate = hijri,
        reason = reason,
        status = status,
        completedDate = completedDate,
        fidyaAmount = fidya,
        note = null,
        createdAt = 0L,
        updatedAt = 0L,
    )

    private val owed = makeupFast(1, MakeupFastStatus.PENDING, hijri = "3 Ramadan 1445")
    private val alsoOwed = makeupFast(2, MakeupFastStatus.PENDING, reason = "Illness")
    private val madeUp = makeupFast(3, MakeupFastStatus.COMPLETED, completedDate = 1_710_000_000_000L)
    private val paid =
        makeupFast(4, MakeupFastStatus.FIDYA_PAID, completedDate = 1_715_000_000_000L, fidya = 5.0)

    private val makeupState = MutableStateFlow(
        MakeupFastsUiState(
            pendingMakeupFasts = listOf(owed, alsoOwed),
            allMakeupFasts = listOf(owed, alsoOwed, madeUp, paid),
            pendingCount = 2,
            isLoading = false,
        )
    )
    private val events = mutableListOf<FastingEvent>()
    private var backs = 0

    private val viewModel: FastingViewModel = mockk(relaxed = true) {
        every { this@mockk.makeupState } returns this@MakeupFastsScreenTest.makeupState
        every { onEvent(any()) } answers { events += firstArg<FastingEvent>() }
    }

    private fun setContent() {
        composeRule.setThemedContent {
            MakeupFastsScreen(onNavigateBack = { backs++ }, viewModel = viewModel)
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    /** The same rendering the card does, so the assertion is about the label, not the format. */
    private fun mediumDate(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
            .toLocalDate().formatMediumDate()

    @Test
    fun `with nothing owed and nothing settled the screen says so`() {
        makeupState.value = MakeupFastsUiState(isLoading = false)
        setContent()

        composeRule.onNodeWithText(string(R.string.fasting_no_makeup)).assertExists()
        composeRule.onNodeWithText(string(R.string.fasting_all_up_to_date)).assertExists()
        // The summary card belongs to the populated arm; an empty state that also showed
        // "0 fasts to make up" would be saying the same thing twice.
        composeRule.onNodeWithText(string(R.string.fasting_fasts_to_makeup)).assertDoesNotExist()
    }

    @Test
    fun `the summary and the stats agree about how many are outstanding`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.fasting_fasts_to_makeup)).assertExists()
        composeRule.onNodeWithText(string(R.string.fasting_pending_count, 2)).assertExists()
        // Four in total. The grid's "completed" cell counts both settled statuses, which is the
        // arithmetic worth pinning — counting only `COMPLETED` would under-report every fidya
        // payment the user has made.
        composeRule.onNodeWithText("4").assertExists()
    }

    @Test
    fun `an owed fast is listed under Owed with its date and reason`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.fasting_makeup_owed)).assertExists()
        // The Hijri date wins when there is one: a make-up fast is owed for a day of Ramadan,
        // and that is the date the user remembers it by.
        composeRule.onNodeWithText(owed.originalHijriDate!!).assertExists()
        composeRule.onNodeWithText(alsoOwed.reason).assertExists()
    }

    @Test
    fun `both ways of settling a fast are filed under Settled, and neither under Owed`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.fasting_makeup_settled)).assertExists()
        composeRule.onNodeWithText(
            context.resources.getQuantityString(R.plurals.fasting_fasts_count, 2, 2)
        ).assertExists()

        // Only the two pending rows offer a way to act — a settled fast with a live "Mark
        // Complete" button is an invitation to record the same debt twice.
        composeRule.onAllNodesWithText(string(R.string.fasting_mark_complete)).assertCountEquals(2)
        composeRule.onAllNodesWithText(string(R.string.fasting_edit)).assertCountEquals(2)
    }

    @Test
    fun `a settled fast reports which door it went out by`() {
        setContent()

        composeRule.onNodeWithText(
            string(R.string.fasting_fidya_paid_on, mediumDate(paid.completedDate!!))
        ).assertExists()
        composeRule.onNodeWithText(
            string(R.string.fasting_made_up_on, mediumDate(madeUp.completedDate!!))
        ).assertExists()
    }

    @Test
    fun `a settled fast with no completion date still says how it was settled`() {
        makeupState.value = makeupState.value.copy(
            pendingMakeupFasts = emptyList(),
            allMakeupFasts = listOf(
                madeUp.copy(completedDate = null),
                paid.copy(completedDate = null),
            ),
            pendingCount = 0,
        )
        setContent()

        // "Completed" twice: `fasting_completed_label` names the stats cell and
        // `fasting_completed` is the row's own line, and the two strings render the same word.
        // A count is the honest assertion — the alternative is matching one of them by position.
        composeRule.onAllNodesWithText(string(R.string.fasting_completed)).assertCountEquals(2)
        composeRule.onNodeWithText(string(R.string.fasting_fidya_paid)).assertExists()
    }

    @Test
    fun `marking complete reports the fast that was marked`() {
        setContent()

        composeRule.onAllNodesWithText(string(R.string.fasting_mark_complete))[0].performClick()

        assertThat(events).containsExactly(FastingEvent.CompleteMakeupFast(owed.id))
    }

    @Test
    fun `editing a fast opens the sheet on that fast`() {
        setContent()

        composeRule.onAllNodesWithText(string(R.string.fasting_edit))[1].performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(string(R.string.fasting_sheet_edit_makeup)).assertExists()
        // The sheet's own reason field, seeded from the *second* row — so the sheet is
        // demonstrably on the fast that was tapped rather than on whichever one the list
        // happened to hold first. Asserted on the field rather than on the text, because the
        // row behind the sheet carries the same word.
        composeRule.onAllNodes(hasSetTextAction())[0].assertTextContains(alsoOwed.reason)
    }

    @Test
    fun `the sheet saves an edited reason back onto the same fast`() {
        setContent()

        composeRule.onAllNodesWithText(string(R.string.fasting_edit))[0].performClick()
        composeRule.waitForIdle()
        // Reason, note, then the status chips: the first editable field is the reason.
        composeRule.onAllNodes(hasSetTextAction())[0].performTextReplacement("Hospital stay")
        composeRule.onNodeWithText(string(R.string.fasting_sheet_save)).performClick()
        composeRule.waitForIdle()

        val updated = (events.single() as FastingEvent.UpdateMakeupFast).makeupFast
        assertThat(updated.id).isEqualTo(owed.id)
        assertThat(updated.reason).isEqualTo("Hospital stay")
        assertThat(updated.status).isEqualTo(MakeupFastStatus.PENDING)
    }

    @Test
    fun `choosing fidya in the sheet pays rather than completes`() {
        setContent()

        composeRule.onAllNodesWithText(string(R.string.fasting_edit))[0].performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(string(R.string.fasting_fidya_paid)).performClick()
        composeRule.waitForIdle()
        // The amount field only exists once fidya is chosen — it is the fourth text field, after
        // reason and note.
        composeRule.onAllNodes(hasSetTextAction())[2].performTextReplacement("12.50")
        composeRule.onNodeWithText(string(R.string.fasting_sheet_save)).performClick()
        composeRule.waitForIdle()

        // Money, not a fast. Sending `UpdateMakeupFast` here would record the debt as fasted and
        // lose the payment entirely.
        assertThat(events).containsExactly(FastingEvent.PayFidya(owed.id, 12.50))
    }

    @Test
    fun `an unreadable fidya amount is treated as nothing paid rather than crashing`() {
        setContent()

        composeRule.onAllNodesWithText(string(R.string.fasting_edit))[0].performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(string(R.string.fasting_fidya_paid)).performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodes(hasSetTextAction())[2].performTextReplacement("not a number")
        composeRule.onNodeWithText(string(R.string.fasting_sheet_save)).performClick()
        composeRule.waitForIdle()

        assertThat(events).containsExactly(FastingEvent.PayFidya(owed.id, 0.0))
    }

    @Test
    fun `marking a fast completed in the sheet stamps a completion date`() {
        setContent()

        composeRule.onAllNodesWithText(string(R.string.fasting_edit))[0].performClick()
        composeRule.waitForIdle()
        // The stats grid behind the sheet is labelled "Completed" too; the chip is the one of
        // the two that can be tapped.
        composeRule.onAllNodesWithText(string(R.string.fasting_completed_label))
            .filterToOne(hasClickAction())
            .performClick()
        composeRule.onNodeWithText(string(R.string.fasting_sheet_save)).performClick()
        composeRule.waitForIdle()

        val updated = (events.single() as FastingEvent.UpdateMakeupFast).makeupFast
        assertThat(updated.status).isEqualTo(MakeupFastStatus.COMPLETED)
        // Without a date the settled row can only say "Completed" and never when — the whole
        // point of the settled section is being able to show your working.
        assertThat(updated.completedDate).isNotNull()
    }

    @Test
    fun `the sheet opens on the note and the amount already stored`() {
        makeupState.value = makeupState.value.copy(
            pendingMakeupFasts = listOf(
                owed.copy(note = "Second day of the journey", fidyaAmount = 7.5)
            ),
            allMakeupFasts = listOf(
                owed.copy(note = "Second day of the journey", fidyaAmount = 7.5)
            ),
            pendingCount = 1,
        )
        setContent()

        composeRule.onNodeWithText(string(R.string.fasting_edit)).performClick()
        composeRule.waitForIdle()

        // Both fields are optional and both are seeded from the row. A sheet that opened blank
        // would wipe whichever the user did not retype on the next save.
        composeRule.onAllNodes(hasSetTextAction())[1]
            .assertTextContains("Second day of the journey")
        composeRule.onNodeWithText(string(R.string.fasting_fidya_paid)).performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodes(hasSetTextAction())[2].assertTextContains("7.5")
    }

    @Test
    fun `clearing the note stores it as absent rather than as a blank`() {
        makeupState.value = makeupState.value.copy(
            pendingMakeupFasts = listOf(owed.copy(note = "Something")),
            allMakeupFasts = listOf(owed.copy(note = "Something")),
            pendingCount = 1,
        )
        setContent()

        composeRule.onNodeWithText(string(R.string.fasting_edit)).performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodes(hasSetTextAction())[1].performTextClearance()
        composeRule.onNodeWithText(string(R.string.fasting_sheet_save)).performClick()
        composeRule.waitForIdle()

        // An empty string and "no note" render differently in the row, and only one of them is
        // what the user meant by deleting the text.
        assertThat((events.single() as FastingEvent.UpdateMakeupFast).makeupFast.note).isNull()
    }

    @Test
    fun `the sheet names every status it can be set to`() {
        setContent()

        composeRule.onAllNodesWithText(string(R.string.fasting_edit))[0].performClick()
        composeRule.waitForIdle()

        // Three chips, one per `MakeupFastStatus`. A `when` arm that fell through would leave a
        // status the user can select and cannot read.
        composeRule.onNodeWithText(string(R.string.fasting_sheet_status)).assertExists()
        MakeupFastStatus.entries.forEach { status ->
            val label = when (status) {
                MakeupFastStatus.PENDING -> string(R.string.fasting_pending)
                MakeupFastStatus.COMPLETED -> string(R.string.fasting_completed_label)
                MakeupFastStatus.FIDYA_PAID -> string(R.string.fasting_fidya_paid)
            }
            composeRule.onAllNodesWithText(label).filterToOne(hasClickAction()).assertExists()
        }
    }

    @Test
    fun `the back arrow goes back`() {
        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.cd_back)).performClick()

        assertThat(backs).isEqualTo(1)
    }
}
