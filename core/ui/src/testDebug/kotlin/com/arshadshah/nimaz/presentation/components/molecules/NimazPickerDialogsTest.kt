package com.arshadshah.nimaz.presentation.components.molecules

import android.content.Context
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.presentation.foundation.time.NimazTime
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The date and time pickers, as dialogs.
 *
 * Both are *staging* dialogs: they hold an edit locally and only report it when the user confirms,
 * so cancelling leaves the caller's value untouched. That is the property worth pinning, because
 * the failure is silent and destructive — a picker that reported on every scroll would overwrite a
 * khatam deadline the moment the sheet opened, and a cancel that still confirmed would change a
 * reminder the user decided not to change.
 *
 * The date picker also converts through the **system zone** on the way out: a date chosen in the
 * calendar becomes the epoch millis of its local midnight. Converting through UTC instead shifts
 * the stored deadline by a day for anybody east or west of Greenwich, which is a bug that only
 * appears for some users.
 *
 * A tall viewport, because a dialog carrying a whole month grid does not fit a phone screen (#604).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class NimazPickerDialogsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val ok = context.getString(android.R.string.ok)
    private val zone: ZoneId = ZoneId.systemDefault()

    @Test
    fun `the date dialog shows its title and confirms the date it was opened on`() {
        val today = LocalDate.now()
        var confirmed: Long? = null

        composeRule.setThemedContent {
            NimazDatePickerDialog(
                selectedDateMillis = null,
                onConfirm = { confirmed = it },
                onDismiss = {},
                title = "Deadline",
            )
        }

        composeRule.onNodeWithText("Deadline").assertExists()
        composeRule.onNodeWithText(ok).performClick()

        // Local midnight, through the system zone — not UTC.
        assertThat(confirmed).isEqualTo(today.atStartOfDay(zone).toInstant().toEpochMilli())
    }

    @Test
    fun `an existing date is read back through the system zone`() {
        // `Instant.ofEpochMilli(it).atZone(zone).toLocalDate()` — reading the stored value in UTC
        // opens the calendar on the wrong day for half the world.
        val stored = LocalDate.now().minusDays(40)
        val millis = stored.atStartOfDay(zone).toInstant().toEpochMilli()
        var confirmed: Long? = null

        composeRule.setThemedContent {
            NimazDatePickerDialog(
                selectedDateMillis = millis,
                onConfirm = { confirmed = it },
                onDismiss = {},
                title = "Deadline",
                minDate = null,
            )
        }

        composeRule.onNodeWithText(ok).performClick()

        assertThat(confirmed).isEqualTo(millis)
    }

    @Test
    fun `cancelling the date dialog reports nothing`() {
        var confirmed: Long? = null
        var dismissed = 0

        composeRule.setThemedContent {
            NimazDatePickerDialog(
                selectedDateMillis = null,
                onConfirm = { confirmed = it },
                onDismiss = { dismissed++ },
                title = "Deadline",
            )
        }

        composeRule.onNodeWithText(context.getString(android.R.string.cancel)).performClick()

        assertThat(dismissed).isEqualTo(1)
        assertThat(confirmed).isNull()
    }

    @Test
    fun `a date before the floor cannot be chosen`() {
        // `if (minDate == null || !date.isBefore(minDate))` — a deadline in the past is not a
        // deadline, and the guard is what stops the calendar accepting one. The tap is swallowed
        // rather than reported, so confirming afterwards still yields the original date.
        val today = LocalDate.now()
        var confirmed: Long? = null

        composeRule.setThemedContent {
            NimazDatePickerDialog(
                selectedDateMillis = null,
                onConfirm = { confirmed = it },
                onDismiss = {},
                title = "Deadline",
                minDate = today,
            )
        }

        composeRule.onNodeWithText(ok).performClick()

        assertThat(confirmed).isEqualTo(today.atStartOfDay(zone).toInstant().toEpochMilli())
    }

    @Test
    fun `the time dialog shows the time it is editing as its subtitle`() {
        // The subtitle is the staged value, which is how the user sees the wheel's effect before
        // committing it.
        composeRule.setThemedContent {
            NimazTimePickerDialog(
                initialTime = NimazTime(21, 30),
                onConfirm = {},
                onDismiss = {},
                title = "Reminder time",
            )
        }

        composeRule.onNodeWithText("Reminder time").assertExists()
        composeRule.onNodeWithText("21:30").assertExists()
    }

    @Test
    fun `confirming the time dialog reports the staged value`() {
        var confirmed: NimazTime? = null

        composeRule.setThemedContent {
            NimazTimePickerDialog(
                initialTime = NimazTime(6, 0),
                onConfirm = { confirmed = it },
                onDismiss = {},
                title = "Reminder time",
                minuteStep = 15,
            )
        }

        composeRule.onNodeWithText(ok).performClick()

        assertThat(confirmed).isEqualTo(NimazTime(6, 0))
    }

    @Test
    fun `cancelling the time dialog leaves the reminder alone`() {
        var confirmed: NimazTime? = null
        var dismissed = 0

        composeRule.setThemedContent {
            NimazTimePickerDialog(
                initialTime = NimazTime(6, 0),
                onConfirm = { confirmed = it },
                onDismiss = { dismissed++ },
                title = "Reminder time",
            )
        }

        composeRule.onNodeWithText(context.getString(android.R.string.cancel)).performClick()

        assertThat(dismissed).isEqualTo(1)
        assertThat(confirmed).isNull()
    }
}
