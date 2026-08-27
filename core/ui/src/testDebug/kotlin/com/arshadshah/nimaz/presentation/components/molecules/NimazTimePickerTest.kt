package com.arshadshah.nimaz.presentation.components.molecules

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.foundation.time.NimazTime
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The wheel time picker, which every reminder in the app is set with.
 *
 * Three things here are worth pinning, and all three are silent when wrong.
 *
 * **The 12/24-hour split changes what the hour wheel contains** — `0..23` against `1..12` plus an
 * AM/PM wheel. A picker built with the wrong list offers "0" as an hour to somebody on a 12-hour
 * locale, or hides 13:00 from somebody on a 24-hour one.
 *
 * **The minute step decides which minutes exist at all.** At the default of five there is no 06:07,
 * and a reminder the user cannot express is not a bug they can report — it just looks like the
 * wheel skipped.
 *
 * **Every label is zero-padded.** "6:5" is not a time, and `"%02d".format` is one call site away
 * from being dropped.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class NimazTimePickerTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `a 24-hour picker offers the hour it is set to and no meridiem wheel`() {
        composeRule.setThemedContent {
            NimazTimePicker(
                value = NimazTime(14, 30),
                onValueChange = {},
                is24Hour = true,
            )
        }

        composeRule.onAllNodesWithText("14").assertCountEquals(1)
        composeRule.onNodeWithText(context.getString(R.string.time_period_pm)).assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.time_period_am)).assertDoesNotExist()
    }

    @Test
    fun `a 12-hour picker shows the meridiem and the converted hour`() {
        // 14:30 is 02:30 PM. A picker that fed the raw hour into a 1..12 wheel would show nothing
        // selected at all, because 14 is not in the list.
        composeRule.setThemedContent {
            NimazTimePicker(
                value = NimazTime(14, 30),
                onValueChange = {},
                is24Hour = false,
            )
        }

        composeRule.onNodeWithText(context.getString(R.string.time_period_pm)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.time_period_am)).assertExists()
        composeRule.onAllNodesWithText("02").assertCountEquals(1)
    }

    @Test
    fun `hours and minutes are zero-padded`() {
        // "3:7" is not a time. Both wheels format through the same `"%02d"`, and both are asserted
        // because they are two separate call sites.
        //
        // 03 and 07 rather than 06 and 05: each wheel renders a few neighbours either side of its
        // selection, so a value that appears in both wheels' visible windows matches twice and the
        // assertion fails on the arithmetic of the test rather than on the padding.
        composeRule.setThemedContent {
            NimazTimePicker(
                value = NimazTime(3, 7),
                onValueChange = {},
                is24Hour = true,
                minuteStep = 1,
            )
        }

        composeRule.onAllNodesWithText("03").assertCountEquals(1)
        composeRule.onAllNodesWithText("07").assertCountEquals(1)
    }

    @Test
    fun `midnight reads as twelve on the twelve-hour wheel`() {
        // The boundary the 12-hour conversion gets wrong: hour 0 must present as 12 AM, not as 0.
        composeRule.setThemedContent {
            NimazTimePicker(
                value = NimazTime(0, 0),
                onValueChange = {},
                is24Hour = false,
            )
        }

        composeRule.onAllNodesWithText("12").assertCountEquals(1)
        composeRule.onNodeWithText(context.getString(R.string.time_period_am)).assertExists()
    }

    @Test
    fun `the minute step decides which minutes exist`() {
        // At the default of five there is no 06:07 to land on. A picker offering every minute in
        // a five-minute wheel — or the reverse — is a reminder the user cannot set.
        composeRule.setThemedContent {
            NimazTimePicker(
                value = NimazTime(6, 0),
                onValueChange = {},
                is24Hour = true,
                minuteStep = 15,
            )
        }

        composeRule.onAllNodesWithText("15").assertCountEquals(1)
        composeRule.onAllNodesWithText("20").assertCountEquals(0)
    }

    @Test
    fun `a one-minute step offers every minute`() {
        composeRule.setThemedContent {
            NimazTimePicker(
                value = NimazTime(6, 0),
                onValueChange = {},
                is24Hour = true,
                minuteStep = 1,
            )
        }

        composeRule.onAllNodesWithText("01").assertCountEquals(1)
    }

    @Test
    fun `the separator is drawn between the wheels`() {
        composeRule.setThemedContent {
            NimazTimePicker(value = NimazTime(9, 45), onValueChange = {}, is24Hour = true)
        }

        composeRule.onNodeWithText(":").assertExists()
    }

    @Test
    fun `a picker built from the device format still renders`() {
        // `is24Hour` defaults to `DateFormat.is24HourFormat(LocalContext.current)`. The default
        // argument is a real call site, and it is the one every caller in the app uses.
        composeRule.setThemedContent {
            NimazTimePicker(value = NimazTime(7, 30), onValueChange = {})
        }

        composeRule.onAllNodesWithText("30").assertCountEquals(1)
    }

    @Test
    fun `an afternoon hour on the twelve-hour wheel keeps its meridiem`() {
        // Converting for display must not lose which half of the day it is: 23:15 shows 11 and PM,
        // and a lost meridiem is a reminder twelve hours out.
        var reported: NimazTime? = null
        composeRule.setThemedContent {
            NimazTimePicker(
                value = NimazTime(23, 15),
                onValueChange = { reported = it },
                is24Hour = false,
            )
        }

        composeRule.onAllNodesWithText("11").assertCountEquals(1)
        composeRule.onNodeWithText(context.getString(R.string.time_period_pm)).assertExists()
        // Nothing was scrolled, so nothing should have been reported — a wheel that fires on
        // composition would overwrite the stored time the moment the sheet opens.
        assertThat(reported).isNull()
    }
}
