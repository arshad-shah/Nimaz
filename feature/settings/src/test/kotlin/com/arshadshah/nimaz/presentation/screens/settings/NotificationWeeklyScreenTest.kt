package com.arshadshah.nimaz.presentation.screens.settings

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.viewmodel.settings.NotificationSettingsUiState
import com.arshadshah.nimaz.presentation.viewmodel.settings.SettingsEvent
import com.arshadshah.nimaz.testing.FakeSettingsScreenViewModel
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The weekly reminders: Jumu'ah with a lead-time stepper, and Khatam with a time picker.
 *
 * Each row is an accordion whose *subtitle reports the setting* when it is on and describes the
 * feature when it is off. That is a real contract, not decoration: it is the only way to read your
 * Friday lead time or your Khatam time without opening the row, and the two branches are one `if`
 * apart. Inverted, the row would advertise "Weekly reminder before Friday prayer" for someone who
 * had set it to 90 minutes, and "90 min" for someone who had it switched off.
 *
 * The lead-time stepper's bounds are the other half. Jumu'ah leads by 15 to 120 minutes in
 * quarter-hour steps, because a reminder for a prayer that happens at one fixed time each week is
 * only useful in that window — a stepper moving by one minute would need 105 taps to cross it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class NotificationWeeklyScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val viewModel = FakeSettingsScreenViewModel()
    private var backs = 0

    private fun setContent(state: NotificationSettingsUiState = NotificationSettingsUiState()) {
        viewModel.notificationState.value = state
        composeRule.setThemedContent {
            NotificationWeeklyScreen(onNavigateBack = { backs++ }, viewModel = viewModel.mock)
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    @Test
    fun `both weekly reminders are offered`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.notification_settings_friday_reminder))
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.notification_settings_khatam_reminder))
            .assertIsDisplayed()
    }

    @Test
    fun `a switched-off Friday reminder describes the feature rather than a time`() {
        setContent(
            NotificationSettingsUiState(fridayReminderEnabled = false, fridayReminderMinutes = 90)
        )

        composeRule.onNodeWithText(string(R.string.notification_settings_friday_subtitle))
            .assertExists()
        composeRule.onNodeWithText(string(R.string.notification_settings_minutes_value, 90))
            .assertDoesNotExist()
    }

    @Test
    fun `a switched-on Friday reminder reports its own lead time`() {
        setContent(
            NotificationSettingsUiState(fridayReminderEnabled = true, fridayReminderMinutes = 90)
        )

        composeRule.onNodeWithText(string(R.string.notification_settings_minutes_value, 90))
            .assertExists()
        composeRule.onNodeWithText(string(R.string.notification_settings_friday_subtitle))
            .assertDoesNotExist()
    }

    @Test
    fun `a switched-off Khatam reminder describes the feature rather than a time`() {
        setContent(
            NotificationSettingsUiState(
                khatamReminderEnabled = false,
                khatamReminderTime = "21:30",
            )
        )

        composeRule.onNodeWithText(string(R.string.notification_settings_khatam_subtitle))
            .assertExists()
    }

    @Test
    fun `an afternoon Khatam time is not read as a morning one`() {
        // The stored string is 24-hour and the wheel is not. Reading "21:30" as 9 AM would move
        // the reminder twelve hours and still look like a valid setting on every screen.
        setContent(
            NotificationSettingsUiState(khatamReminderEnabled = true, khatamReminderTime = "09:30")
        )

        composeRule.onNodeWithText(string(R.string.notification_settings_khatam_reminder))
            .performClick()

        composeRule.onAllNodesWithText(string(R.string.time_period_am), useUnmergedTree = true)
            .onFirst().assertExists()
    }

    @Test
    fun `a switched-on Khatam reminder reports the time it will fire`() {
        setContent(
            NotificationSettingsUiState(
                khatamReminderEnabled = true,
                khatamReminderTime = "21:30",
            )
        )

        composeRule.onNodeWithText("21:30").assertExists()
    }

    @Test
    fun `the first switch is Jumuah's and the second is Khatam's`() {
        // Two accordions built from one block, in an order only the source states. A switch
        // wired to its neighbour's event silently swaps which reminder the user just turned on,
        // and the row it sits in still reads correctly.
        setContent(
            NotificationSettingsUiState(
                fridayReminderEnabled = false,
                khatamReminderEnabled = false,
            )
        )

        val switches = composeRule.onAllNodes(isToggleable())
        switches[0].performClick()
        assertThat(viewModel.only<SettingsEvent.SetFridayReminderEnabled>().enabled).isTrue()
        assertThat(viewModel.events.filterIsInstance<SettingsEvent.SetKhatamReminderEnabled>())
            .isEmpty()

        switches[1].performClick()
        assertThat(viewModel.only<SettingsEvent.SetKhatamReminderEnabled>().enabled).isTrue()
    }

    @Test
    fun `a switch passes what it reports rather than negating state`() {
        setContent(
            NotificationSettingsUiState(
                fridayReminderEnabled = true,
                khatamReminderEnabled = true,
            )
        )

        composeRule.onAllNodes(isToggleable())[0].performClick()

        assertThat(viewModel.only<SettingsEvent.SetFridayReminderEnabled>().enabled).isFalse()
    }

    @Test
    fun `the Jumuah lead time steps in quarter hours, not minutes`() {
        // A reminder for a prayer at one fixed time each week is only useful in a 15-to-120
        // minute window; a one-minute step would need 105 taps to cross it.
        setContent(
            NotificationSettingsUiState(fridayReminderEnabled = true, fridayReminderMinutes = 30)
        )

        composeRule.onNodeWithText(string(R.string.notification_settings_friday_reminder))
            .performClick()
        // The accordion is a clickable card, so it *merges* its children's semantics — the
        // stepper's own +/- nodes exist only in the unmerged tree, and the merged card reports
        // `ContentDescription=[Decrease, Increase]` as its own.
        composeRule
            .onAllNodesWithContentDescription(string(R.string.cd_increase), useUnmergedTree = true)
            .onFirst()
            .performClick()

        assertThat(viewModel.only<SettingsEvent.SetFridayReminderMinutes>().minutes).isEqualTo(45)
    }

    @Test
    fun `the Jumuah lead time will not step below its quarter-hour floor`() {
        // Below 15 minutes the reminder and the adhan land together, which is not a reminder.
        setContent(
            NotificationSettingsUiState(fridayReminderEnabled = true, fridayReminderMinutes = 15)
        )

        composeRule.onNodeWithText(string(R.string.notification_settings_friday_reminder))
            .performClick()
        composeRule
            .onAllNodesWithContentDescription(string(R.string.cd_decrease), useUnmergedTree = true)
            .onFirst()
            .performClick()

        assertThat(viewModel.events.filterIsInstance<SettingsEvent.SetFridayReminderMinutes>())
            .isEmpty()
    }

    @Test
    fun `expanding the Khatam row opens a picker positioned on the stored time`() {
        // The stored preference is an "HH:mm" string and the control is a pair of wheels, so the
        // parse is the seam: `NimazTime.parse` failing quietly would open the picker at midnight
        // and the first nudge would move the reminder to a time nobody chose. The wheels are
        // driven by drag rather than by a click action, so what is pinned here is the position
        // they open at; `SettingsEventTableTest` pins the write that a change produces.
        setContent(
            NotificationSettingsUiState(khatamReminderEnabled = true, khatamReminderTime = "21:30")
        )

        composeRule.onNodeWithText(string(R.string.notification_settings_khatam_reminder))
            .performClick()

        // The test locale is 12-hour, so 21:30 must open the wheel on 09 in the afternoon. A
        // parse that quietly failed would give 00:00 — which reads as 12 AM, the one value that
        // looks deliberate and is not.
        composeRule.onAllNodesWithText("09", useUnmergedTree = true).onFirst().assertExists()
        composeRule.onAllNodesWithText(string(R.string.time_period_pm), useUnmergedTree = true)
            .onFirst().assertExists()
    }

    @Test
    fun `the back button navigates back`() {
        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.cd_back)).performClick()

        assertThat(backs).isEqualTo(1)
    }
}
