package com.arshadshah.nimaz.presentation.screens.fasting

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.domain.model.ExemptionReason
import com.arshadshah.nimaz.domain.model.FastRecord
import com.arshadshah.nimaz.domain.model.FastStatus
import com.arshadshah.nimaz.domain.model.FastType
import com.arshadshah.nimaz.presentation.screens.createComponentComposeRule
import com.arshadshah.nimaz.presentation.screens.setThemedContent
import com.arshadshah.nimaz.presentation.viewmodel.tracker.FastingTrackerUiState
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Sized to a phone on purpose. The card is a tall one — date, window band, three-cell control and
 * a chip footer — and on Robolectric's default surface the footer falls off the bottom, so
 * `assertIsDisplayed` fails on content that is perfectly visible in the app. Asserting mere
 * existence instead would have hidden a real "this does not fit" regression, which is exactly the
 * kind of thing a card this tall should be watched for.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class FastingDayCardTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val today = LocalDate.of(2026, 8, 13)

    private fun record(
        status: FastStatus,
        reason: ExemptionReason? = null,
    ) = FastRecord(
        id = 1,
        date = 0,
        hijriDate = null,
        hijriMonth = null,
        hijriYear = null,
        fastType = FastType.VOLUNTARY,
        status = status,
        exemptionReason = reason,
        suhoorTime = null,
        iftarTime = null,
        note = null,
        createdAt = 0,
        updatedAt = 0,
    )

    private fun state(
        date: LocalDate = today,
        isToday: Boolean = true,
        selectedRecord: FastRecord? = null,
    ) = FastingTrackerUiState(
        selectedDate = date,
        selectedRecord = selectedRecord,
        isSelectedToday = isToday,
        isLoading = false,
    )

    private fun render(
        state: FastingTrackerUiState,
        onSetStatus: (FastStatus) -> Unit = {},
        onOpenExemption: () -> Unit = {},
        onOpenNote: () -> Unit = {},
        onBackToToday: () -> Unit = {},
    ) {
        composeRule.setThemedContent {
            FastingDayCard(
                state = state,
                ramadanDay = null,
                onSetStatus = onSetStatus,
                onOpenExemption = onOpenExemption,
                onOpenNote = onOpenNote,
                onBackToToday = onBackToToday,
            )
        }
    }

    @Test
    fun `an unlogged day says so and selects no status`() {
        render(state())
        composeRule.onNodeWithText("Not logged yet").assertIsDisplayed()
        composeRule.onNodeWithText("Fasted").assertIsNotSelected()
        composeRule.onNodeWithText("Not fasting").assertIsNotSelected()
        composeRule.onNodeWithText("Exempt").assertIsNotSelected()
    }

    @Test
    fun `a fasted day selects the fasted cell`() {
        render(state(selectedRecord = record(FastStatus.FASTED)))
        composeRule.onNodeWithText("Fasted").assertIsSelected()
    }

    @Test
    fun `a makeup-due day selects the exempt cell and says it is owed`() {
        render(state(selectedRecord = record(FastStatus.MAKEUP_DUE)))
        composeRule.onNodeWithText("Exempt").assertIsSelected()
        composeRule.onNodeWithText("Owed — make up later").assertIsDisplayed()
    }

    @Test
    fun `an exempted day shows its reason as a chip`() {
        render(state(selectedRecord = record(FastStatus.EXEMPTED, ExemptionReason.TRAVEL)))
        composeRule.onNodeWithText("Travel").assertIsDisplayed()
    }

    @Test
    fun `tapping Fasted reports the fasted status`() {
        var observed: FastStatus? = null
        render(state(), onSetStatus = { observed = it })
        composeRule.onNodeWithText("Fasted").performClick()
        assertThat(observed).isEqualTo(FastStatus.FASTED)
    }

    @Test
    fun `tapping Not fasting reports the not-fasted status`() {
        var observed: FastStatus? = null
        render(state(), onSetStatus = { observed = it })
        composeRule.onNodeWithText("Not fasting").performClick()
        assertThat(observed).isEqualTo(FastStatus.NOT_FASTED)
    }

    @Test
    fun `tapping Exempt opens the reason sheet instead of writing a status`() {
        var status: FastStatus? = null
        var opened = false
        render(state(), onSetStatus = { status = it }, onOpenExemption = { opened = true })
        composeRule.onNodeWithText("Exempt").performClick()
        assertThat(opened).isTrue()
        assertThat(status).isNull()
    }

    @Test
    fun `Back to today is absent when the selection is today`() {
        render(state())
        composeRule.onNodeWithText("Back to today").assertDoesNotExist()
    }

    @Test
    fun `Back to today appears and reports when another day is selected`() {
        var went = false
        render(
            state(date = LocalDate.of(2026, 8, 11), isToday = false),
            onBackToToday = { went = true },
        )
        composeRule.onNodeWithText("Back to today").assertIsDisplayed()
        composeRule.onNodeWithText("Back to today").performClick()
        assertThat(went).isTrue()
    }

    @Test
    fun `Add a note reports`() {
        var opened = false
        render(state(), onOpenNote = { opened = true })
        composeRule.onNodeWithText("Add a note").performClick()
        assertThat(opened).isTrue()
    }

    @Test
    fun `a day with no prayer times still renders its window as placeholders`() {
        // selectedSuhoorAt / selectedIftarAt are null before the schedule loads — the first frame
        // of every open. The card must show placeholders rather than crash or omit the band.
        //
        // Asserted through the content description rather than the visible labels because
        // NimazWindowTrack clears its children from the semantics tree and speaks as one
        // sentence; four unlabelled text nodes read as noise. Querying the labels here would be
        // asserting against the atom's own accessibility contract.
        render(state())
        composeRule
            .onNodeWithContentDescription("Fasting window, --:-- until --:--")
            .assertIsDisplayed()
    }
}
