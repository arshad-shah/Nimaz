package com.arshadshah.nimaz.presentation.screens.settings

import android.content.Context
import android.os.PowerManager
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.PrayerAlertStyle
import com.arshadshah.nimaz.presentation.viewmodel.settings.NotificationSummary
import com.arshadshah.nimaz.presentation.viewmodel.settings.SettingsEvent
import com.arshadshah.nimaz.testing.FakeSettingsScreenViewModel
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.arshadshah.nimaz.testing.settingsRow
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager
import org.robolectric.annotation.Config

/**
 * The notifications hub: a master switch, five rows into subscreens, and subtitles that report
 * what is actually set.
 *
 * The subtitles are the reason this is more than a menu, and they are also the thing most likely
 * to go quietly wrong. `NotificationHubSubtitlesTest` pins *which string* each state chooses; this
 * pins that the screen hands each helper the settings that belong to it. Feeding the weekly row's
 * helper the worship counts, or the prayer row Fajr's reminder minutes where it wants its style,
 * both compile and both produce a plausible sentence that is false.
 *
 * The master switch is the structural half. Everything below it is inside
 * `if (notificationsMasterEnabled)`, so switching it off must remove the rows rather than dim them
 * — a hub that still offers "Prayers · Adhan · 15 minutes before" while notifications are off is
 * telling the user alerts are configured when none will arrive.
 *
 * **Every value here comes from `notificationSummary`, and that is the fix rather than an
 * incidental detail.** The rows used to read `notificationState`, which is loaded once per
 * ViewModel instance — and `hiltViewModel()` gives the hub a different instance from each of the
 * five subscreens that edit these settings. Switching a worship reminder on and coming back left
 * the count reporting the value from before the edit. The prayer row was right the whole time,
 * because it was the only one already reading the summary; that asymmetry is what made it look
 * like a rendering fault. `NotificationSummaryTest` pins the ViewModel half.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class NotificationSettingsScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val viewModel = FakeSettingsScreenViewModel()
    private var backs = 0
    private var prayers = 0
    private var worship = 0
    private var weekly = 0
    private var sound = 0
    private var diagnostics = 0

    /**
     * Only the summary. The screen stopped reading `notificationState` when the hub's rows were
     * found to be stale: that flow is a one-shot snapshot per ViewModel instance, and the hub
     * holds a different instance from every subscreen that edits these values. A `state`
     * parameter here would let a test arrange something the screen cannot see.
     */
    private fun setContent(
        summary: NotificationSummary = NotificationSummary(),
    ) {
        viewModel.notificationSummary.value = summary
        composeRule.setThemedContent {
            NotificationSettingsScreen(
                onNavigateBack = { backs++ },
                onNavigateToPrayers = { prayers++ },
                onNavigateToWorshipReminders = { worship++ },
                onNavigateToWeekly = { weekly++ },
                onNavigateToSound = { sound++ },
                onNavigateToDiagnostics = { diagnostics++ },
                viewModel = viewModel.mock,
            )
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    @Test
    fun `the hub offers all five rows when notifications are on`() {
        setContent(summary = NotificationSummary(notificationsMasterEnabled = true))

        composeRule.onNodeWithText(string(R.string.notif_hub_prayers_title)).assertExists()
        composeRule.onNodeWithText(string(R.string.notif_hub_sound_title)).assertExists()
        composeRule.onNodeWithText(string(R.string.worship_settings_title)).assertExists()
        composeRule.onNodeWithText(string(R.string.notif_hub_weekly_title)).assertExists()
        composeRule.onNodeWithText(string(R.string.notif_hub_diagnostics_title)).assertExists()
    }

    @Test
    fun `switching notifications off hides every row rather than dimming them`() {
        // A hub that still reports "Prayers · Adhan · 2 of 5" while the master switch is off is
        // telling the user alerts are configured when none can arrive.
        setContent(summary = NotificationSummary(notificationsMasterEnabled = false))

        composeRule.onNodeWithText(string(R.string.notif_hub_prayers_title)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.notif_hub_sound_title)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.worship_settings_title)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.notif_hub_weekly_title)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.notif_hub_diagnostics_title))
            .assertDoesNotExist()
    }

    @Test
    fun `the master switch stays reachable when it is off, so it can be switched back on`() {
        setContent(summary = NotificationSummary(notificationsMasterEnabled = false))

        composeRule.settingsRow(string(R.string.notification_settings_enable)).performClick()

        assertThat(viewModel.only<SettingsEvent.SetNotificationsEnabled>().enabled).isTrue()
    }

    @Test
    fun `the master switch passes the value the switch reports, not the negation of state`() {
        // This is the one row on the screen wired as `onCheckedChange = { onEvent(Set…(it)) }`
        // rather than `!state`. Rewriting it to match its neighbours would invert it.
        setContent(summary = NotificationSummary(notificationsMasterEnabled = true))

        composeRule.settingsRow(string(R.string.notification_settings_enable)).performClick()

        assertThat(viewModel.only<SettingsEvent.SetNotificationsEnabled>().enabled).isFalse()
    }

    @Test
    fun `the prayers row reports how many of the five are on`() {
        setContent(summary = NotificationSummary(enabledPrayerCount = 3))

        composeRule.onNodeWithText(string(R.string.notif_hub_count_of, 3, 5)).assertExists()
    }

    @Test
    fun `the prayers row reports the alert style, and the reminder when there is one`() {
        setContent(
            summary = NotificationSummary(
                fajrAlertStyle = PrayerAlertStyle.ADHAN,
                reminderEnabled = true,
                reminderMinutes = 20,
            )
        )

        val expected = string(
            R.string.notif_prayer_summary,
            string(R.string.notif_alert_style_adhan),
            context.resources.getQuantityString(R.plurals.notif_reminder_minutes_before, 20, 20),
        )
        composeRule.onNodeWithText(expected).assertExists()
    }

    @Test
    fun `the prayers row reports only the style when no reminder is set`() {
        // The early return exists so the row does not claim a reminder that will not fire.
        setContent(
            summary = NotificationSummary(
                fajrAlertStyle = PrayerAlertStyle.SILENT,
                reminderEnabled = false,
            )
        )

        composeRule.onNodeWithText(string(R.string.notif_alert_style_silent)).assertExists()
    }

    @Test
    fun `the sound row names the chosen adhan and the rule most likely to surprise`() {
        // Do Not Disturb outranks vibration in the subtitle because it is the one that stops a
        // sound the user is expecting.
        setContent(
            summary = NotificationSummary(
                respectDnd = true,
                vibrationEnabled = true,
                selectedAdhanSound = "MISHARY",
            )
        )

        composeRule.onNodeWithText(
            string(
                R.string.notif_hub_sound_dnd,
                com.arshadshah.nimaz.data.audio.AdhanSound.fromName("MISHARY").displayName,
            )
        ).assertExists()
    }

    @Test
    fun `the sound row falls through to vibration when DnD is not respected`() {
        setContent(
            summary = NotificationSummary(respectDnd = false, vibrationEnabled = true)
        )

        composeRule.onNodeWithText(
            string(
                R.string.notif_hub_sound_vibration,
                com.arshadshah.nimaz.data.audio.AdhanSound
                    .fromName(NotificationSummary().selectedAdhanSound).displayName,
            )
        ).assertExists()
    }

    @Test
    fun `the worship row counts the reminders that are on, not the ones that exist`() {
        // Eleven reminders exist and default to off, so a row that counted what exists rather
        // than what is on would read "11 on" for a user who has enabled none. The count is
        // computed in the ViewModel now (`NotificationSummaryTest` pins that it iterates the
        // enum); what this pins is that the row renders the count it is handed.
        setContent(summary = NotificationSummary(worshipRemindersOn = 2))

        composeRule.onNodeWithText(string(R.string.notif_hub_count_on, 2)).assertExists()
    }

    @Test
    fun `the weekly row names which of the two weekly reminders are on`() {
        setContent(
            summary = NotificationSummary(
                fridayReminderEnabled = true,
                khatamReminderEnabled = false,
            )
        )

        composeRule.onNodeWithText(string(R.string.notif_hub_weekly_jumuah)).assertExists()
    }

    @Test
    fun `the weekly row says neither when neither is on`() {
        setContent(
            summary = NotificationSummary(
                fridayReminderEnabled = false,
                khatamReminderEnabled = false,
            )
        )

        composeRule.onNodeWithText(string(R.string.notif_hub_weekly_none)).assertExists()
    }

    @Test
    fun `a row re-renders when its setting changes underneath the composed screen`() {
        // The regression, at the level the user saw it. Coming back from a subscreen does not
        // recompose the hub from scratch — the summary emits and the row has to follow. Every
        // other test here arranges a value *before* composing, which passes just as well against
        // a screen that reads a snapshot once.
        setContent(summary = NotificationSummary(worshipRemindersOn = 0, enabledPrayerCount = 5))
        composeRule.onNodeWithText(string(R.string.notif_hub_count_on, 0)).assertExists()

        viewModel.notificationSummary.value =
            NotificationSummary(worshipRemindersOn = 3, enabledPrayerCount = 2)

        composeRule.onNodeWithText(string(R.string.notif_hub_count_on, 3)).assertExists()
        composeRule.onNodeWithText(string(R.string.notif_hub_count_of, 2, 5)).assertExists()
        composeRule.onNodeWithText(string(R.string.notif_hub_count_on, 0)).assertDoesNotExist()
    }

    @Test
    fun `the master switch turning off underneath the screen takes the rows with it`() {
        setContent(summary = NotificationSummary(notificationsMasterEnabled = true))
        composeRule.onNodeWithText(string(R.string.notif_hub_prayers_title)).assertExists()

        viewModel.notificationSummary.value =
            NotificationSummary(notificationsMasterEnabled = false)

        composeRule.onNodeWithText(string(R.string.notif_hub_prayers_title)).assertDoesNotExist()
    }

    @Test
    fun `each row opens its own subscreen`() {
        // Five rows, five callbacks, all wired in one block. A row pointing at its neighbour's
        // destination is invisible in review and obvious to a user exactly once.
        setContent()

        composeRule.settingsRow(string(R.string.notif_hub_prayers_title)).performClick()
        composeRule.settingsRow(string(R.string.notif_hub_sound_title)).performClick()
        composeRule.settingsRow(string(R.string.worship_settings_title)).performClick()
        composeRule.settingsRow(string(R.string.notif_hub_weekly_title)).performClick()
        composeRule.settingsRow(string(R.string.notif_hub_diagnostics_title)).performClick()

        assertThat(listOf(prayers, sound, worship, weekly, diagnostics))
            .containsExactly(1, 1, 1, 1, 1)
    }

    @Test
    fun `a device that would delay alerts is warned about, and the warning links to diagnostics`() {
        // Robolectric reports the app as battery-*restricted* by default, so `hasProblem` is
        // true here without arranging anything — the same state a real device is in until
        // someone grants the exemption. The banner is the only thing that tells a user their
        // prayer alerts will arrive late, and the row badge is what they see if they scroll
        // past it, so both are asserted.
        setContent()

        composeRule.onNodeWithText(string(R.string.notif_hub_delivery_warning)).assertExists()
        composeRule.onNodeWithText(string(R.string.notif_diag_needs_attention)).assertExists()

        composeRule.onNodeWithText(string(R.string.notif_hub_delivery_warning)).performClick()

        assertThat(diagnostics).isEqualTo(1)
    }

    @Test
    fun `a healthy device shows no delivery warning at all`() {
        // The other half, and the one that matters for trust: a banner that always showed would
        // train people to ignore the one time it is real. Both prerequisites Robolectric denies
        // by default have to be granted for that, which is itself the point — the banner keys
        // off `hasProblem`, not off any single one of them.
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        shadowOf(context.getSystemService(PowerManager::class.java))
            .setIgnoringBatteryOptimizations(context.packageName, true)

        setContent()

        composeRule.onNodeWithText(string(R.string.notif_hub_delivery_warning))
            .assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.notif_diag_needs_attention))
            .assertDoesNotExist()
    }

    @Test
    fun `the warning stays hidden while notifications are switched off entirely`() {
        // The banner lives inside the `if (notificationsEnabled)` block. Warning that alerts
        // may be delayed, when the user has switched alerts off, is noise about a setting they
        // have already decided.
        setContent(summary = NotificationSummary(notificationsMasterEnabled = false))

        composeRule.onNodeWithText(string(R.string.notif_hub_delivery_warning))
            .assertDoesNotExist()
    }

    @Test
    fun `the title is the notifications hub`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.notifications)).assertIsDisplayed()
    }

    @Test
    fun `the back button navigates back`() {
        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.cd_back)).performClick()

        assertThat(backs).isEqualTo(1)
    }
}
