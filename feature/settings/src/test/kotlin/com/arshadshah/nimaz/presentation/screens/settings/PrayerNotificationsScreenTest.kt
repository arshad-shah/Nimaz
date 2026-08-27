package com.arshadshah.nimaz.presentation.screens.settings

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.PrayerAlertStyle
import com.arshadshah.nimaz.domain.model.PrayerTimes
import com.arshadshah.nimaz.presentation.viewmodel.settings.NotificationSettingsUiState
import com.arshadshah.nimaz.presentation.viewmodel.settings.SettingsEvent
import com.arshadshah.nimaz.testing.FakeSettingsScreenViewModel
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.arshadshah.nimaz.testing.accordionRow
import com.arshadshah.nimaz.testing.settingsRow
import com.arshadshah.nimaz.testing.tappableAncestorCount
import com.arshadshah.nimaz.testing.testLocation
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Prayer notifications — the highest-stakes screen in the module, because it decides whether a
 * user's prayer alerts fire at all.
 *
 * Two structural claims are worth pinning above everything else.
 *
 * **Sunrise is not a sixth prayer.** It has no alert style and no reminder — it is the end of
 * Fajr's window — so its toggle lives *inside* Fajr's accordion and writes the "sunrise" key.
 * `:core:datastore` pins that it defaults off and can never carry an adhan; this pins that the
 * screen agrees, and in particular that the five rows are built from `PRAYER_KEYS` rather than
 * from a six-element list that would give sunrise controls it must not have.
 *
 * **"Remind me before every prayer" writes seven things, not one.** The app-wide pair
 * (`SetShowReminderBefore` / `SetReminderMinutes`) is what a delivered reminder falls back to; the
 * five per-prayer events are what actually rearms the alarms. Writing only the app-wide pair —
 * which is what the control did before the per-prayer split — ships a switch that changes no
 * notification at all, and looks correct on every screen.
 *
 * The header summaries are the third: this screen is usually opened to answer "what happens at
 * Asr?", and a header that reported the wrong prayer's setting would answer it wrongly without
 * ever being opened.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class PrayerNotificationsScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val viewModel = FakeSettingsScreenViewModel()
    private var backs = 0

    private val times = PrayerTimes(
        fajr = LocalDateTime.of(2026, 8, 26, 4, 30),
        sunrise = LocalDateTime.of(2026, 8, 26, 6, 5),
        dhuhr = LocalDateTime.of(2026, 8, 26, 13, 5),
        asr = LocalDateTime.of(2026, 8, 26, 16, 45),
        maghrib = LocalDateTime.of(2026, 8, 26, 20, 10),
        isha = LocalDateTime.of(2026, 8, 26, 21, 40),
        date = LocalDate.of(2026, 8, 26),
        location = testLocation(),
    )

    private fun setContent(
        state: NotificationSettingsUiState = NotificationSettingsUiState(),
        prayerTimes: PrayerTimes? = times,
    ) {
        viewModel.notificationState.value = state
        viewModel.todayPrayerTimes.value = prayerTimes
        composeRule.setThemedContent {
            PrayerNotificationsScreen(onNavigateBack = { backs++ }, viewModel = viewModel.mock)
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    private fun minutesBefore(minutes: Int): String =
        context.resources.getQuantityString(
            R.plurals.notif_reminder_minutes_before, minutes, minutes
        )

    @Test
    fun `there are five prayer rows and sunrise is not one of them`() {
        // Sunrise has no alert style and no reminder. A sixth row would offer it both.
        setContent()

        listOf(
            R.string.prayer_fajr,
            R.string.prayer_dhuhr,
            R.string.prayer_asr,
            R.string.prayer_maghrib,
            R.string.prayer_isha,
        ).forEach { composeRule.onNodeWithText(string(it)).assertExists() }

        composeRule.onNode(hasText(string(R.string.notif_sunrise)) and isToggleable())
            .assertDoesNotExist()
    }

    @Test
    fun `each row shows that prayer's own time`() {
        // Five parallel lists indexed together. An off-by-one puts Asr's time on Dhuhr's row,
        // and the row still reads as a plausible prayer time.
        setContent()

        composeRule.onNodeWithText("04:30").assertExists()
        composeRule.onNodeWithText("13:05").assertExists()
        composeRule.onNodeWithText("16:45").assertExists()
        composeRule.onNodeWithText("20:10").assertExists()
        composeRule.onNodeWithText("21:40").assertExists()
    }

    @Test
    fun `a row renders without a time when today's times are not known yet`() {
        // The times arrive from a `WhileSubscribed` flow that starts null, so every row composes
        // at least once with no time. A non-null assumption crashes the screen on open.
        setContent(prayerTimes = null)

        composeRule.onNodeWithText(string(R.string.prayer_fajr)).assertExists()
        composeRule.onNodeWithText("04:30").assertDoesNotExist()
    }

    @Test
    fun `a switched-off prayer says so instead of describing an alert that will not fire`() {
        setContent(NotificationSettingsUiState(asrNotification = false))

        composeRule.onNodeWithText(string(R.string.notif_prayer_off)).assertExists()
    }

    @Test
    fun `a row's summary reports that prayer's own alert style`() {
        setContent(
            NotificationSettingsUiState(
                alertStyles = mapOf(
                    "fajr" to PrayerAlertStyle.ADHAN,
                    "dhuhr" to PrayerAlertStyle.SILENT,
                )
            )
        )

        composeRule.onNodeWithText(string(R.string.notif_alert_style_adhan)).assertExists()
        composeRule.onNodeWithText(string(R.string.notif_alert_style_silent)).assertExists()
    }

    @Test
    fun `a row's summary adds the lead time only when that prayer has a reminder`() {
        setContent(
            NotificationSettingsUiState(
                alertStyles = PrayerAlertStyle.PRAYER_KEYS
                    .associateWith { PrayerAlertStyle.NOTIFICATION },
                reminderEnabled = mapOf("maghrib" to true),
                reminderOffsets = mapOf("maghrib" to 25),
            )
        )

        composeRule.onNodeWithText(
            string(
                R.string.notif_prayer_summary,
                string(R.string.notif_alert_style_notification),
                minutesBefore(25),
            )
        ).assertExists()
    }

    @Test
    fun `a reminder switched on with no stored offset falls back to the documented default`() {
        // `reminderOffsets[prayer] ?: DEFAULT_REMINDER_MINUTES`. Falling back to 0 instead would
        // fire the reminder at the same moment as the adhan, which is not a reminder.
        setContent(
            NotificationSettingsUiState(
                alertStyles = PrayerAlertStyle.PRAYER_KEYS
                    .associateWith { PrayerAlertStyle.NOTIFICATION },
                reminderEnabled = mapOf("isha" to true),
                reminderOffsets = emptyMap(),
            )
        )

        composeRule.onNodeWithText(
            string(
                R.string.notif_prayer_summary,
                string(R.string.notif_alert_style_notification),
                minutesBefore(PrayerAlertStyle.DEFAULT_REMINDER_MINUTES),
            )
        ).assertExists()
    }

    @Test
    fun `a prayer's switch toggles that prayer, keyed by its own name`() {
        setContent()

        // Row order is Fajr, Dhuhr, Asr, Maghrib, Isha. The "all prayers" row is a
        // `NimazSettingsItem`, whose switch defers its click to the enclosing row, so the only
        // toggleable nodes on screen are the five accordion switches — in prayer order.
        composeRule.onAllNodes(isToggleable())[1].performClick()

        val event = viewModel.events.filterIsInstance<SettingsEvent.SetPrayerNotification>().single()
        assertThat(event.prayer).isEqualTo("dhuhr")
    }

    @Test
    fun `sunrise's toggle lives under Fajr and writes the sunrise key`() {
        // It renders only inside Fajr's expanded body, and it must not write "fajr".
        setContent(NotificationSettingsUiState(sunriseNotification = false))

        composeRule.onNodeWithText(string(R.string.prayer_fajr)).performClick()
        composeRule.accordionRow(string(R.string.notif_sunrise)).performClick()

        val event = viewModel.events.filterIsInstance<SettingsEvent.SetPrayerNotification>().single()
        assertThat(event.prayer).isEqualTo("sunrise")
        assertThat(event.enabled).isTrue()
    }

    @Test
    fun `no other prayer's accordion offers the sunrise toggle`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.prayer_isha)).performClick()

        assertThat(composeRule.tappableAncestorCount(string(R.string.notif_sunrise))).isEqualTo(0)
    }

    @Test
    fun `a switched-off prayer's alert style and reminder rows are off-limits`() {
        // Choosing an alert style for a prayer that sends nothing is a setting with no effect,
        // and offering it suggests the notification will arrive.
        setContent(NotificationSettingsUiState(fajrNotification = false))

        composeRule.onNodeWithText(string(R.string.prayer_fajr)).performClick()

        // Only the accordion card encloses them — the rows themselves have dropped their
        // `clickable`, which is how `NimazSettingsItem` expresses "disabled".
        assertThat(composeRule.tappableAncestorCount(string(R.string.notif_alert_style)))
            .isEqualTo(1)
        assertThat(composeRule.tappableAncestorCount(string(R.string.notif_reminder_before)))
            .isEqualTo(1)

        // And tapping there opens nothing.
        composeRule.accordionRow(string(R.string.notif_alert_style)).performClick()
        composeRule.onNodeWithText(string(R.string.notif_alert_style_adhan_subtitle))
            .assertDoesNotExist()
    }

    @Test
    fun `picking an alert style writes it against the prayer whose row was opened`() {
        // One nullable `openSheet` rather than a boolean per prayer per setting. The sheet has
        // to carry *which* prayer, or every pick lands on whichever prayer the code names.
        setContent(NotificationSettingsUiState(asrNotification = true))

        composeRule.onNodeWithText(string(R.string.prayer_asr)).performClick()
        composeRule.accordionRow(string(R.string.notif_alert_style)).performClick()
        composeRule.onNodeWithText(string(R.string.notif_alert_style_silent_subtitle))
            .performClick()

        val event = viewModel.only<SettingsEvent.SetPrayerAlertStyle>()
        assertThat(event.prayer).isEqualTo("asr")
        assertThat(event.style).isEqualTo(PrayerAlertStyle.SILENT)
    }

    @Test
    fun `picking a lead time switches that prayer's reminder on and stores the number`() {
        setContent(NotificationSettingsUiState(maghribNotification = true))

        composeRule.onNodeWithText(string(R.string.prayer_maghrib)).performClick()
        composeRule.accordionRow(string(R.string.notif_reminder_before)).performClick()
        composeRule.onNodeWithText(minutesBefore(30)).performClick()

        val enabled = viewModel.only<SettingsEvent.SetPrayerReminderEnabled>()
        assertThat(enabled.prayer).isEqualTo("maghrib")
        assertThat(enabled.enabled).isTrue()
        val minutes = viewModel.only<SettingsEvent.SetPrayerReminderMinutes>()
        assertThat(minutes.prayer).isEqualTo("maghrib")
        assertThat(minutes.minutes).isEqualTo(30)
    }

    @Test
    fun `picking No reminder switches it off without writing a lead time`() {
        // The `if (minutes != null)` guard. Writing 0 minutes instead would leave the reminder
        // stored as "0 minutes before", which reads back as a reminder that fires with the adhan.
        setContent(
            NotificationSettingsUiState(
                ishaNotification = true,
                reminderEnabled = mapOf("isha" to true),
                reminderOffsets = mapOf("isha" to 20),
            )
        )

        composeRule.onNodeWithText(string(R.string.prayer_isha)).performClick()
        composeRule.accordionRow(string(R.string.notif_reminder_before)).performClick()
        composeRule.onNodeWithText(string(R.string.notif_reminder_none)).performClick()

        assertThat(viewModel.only<SettingsEvent.SetPrayerReminderEnabled>().enabled).isFalse()
        assertThat(viewModel.events.filterIsInstance<SettingsEvent.SetPrayerReminderMinutes>())
            .isEmpty()
    }

    @Test
    fun `remind-me-before-every-prayer writes the app-wide pair and all five prayers`() {
        // Seven events. The app-wide pair alone is what the control used to write, and it
        // changes no notification; the five per-prayer events are what rearms the alarms.
        setContent(
            NotificationSettingsUiState(showReminderBefore = false, reminderMinutes = 15)
        )

        composeRule.settingsRow(string(R.string.notif_all_prayers_reminder_title)).performClick()

        assertThat(viewModel.only<SettingsEvent.SetShowReminderBefore>().enabled).isTrue()
        assertThat(viewModel.only<SettingsEvent.SetReminderMinutes>().minutes).isEqualTo(15)
        assertThat(
            viewModel.events.filterIsInstance<SettingsEvent.SetPrayerReminderEnabled>()
                .map { it.prayer }
        ).containsExactlyElementsIn(PrayerAlertStyle.PRAYER_KEYS)
        assertThat(
            viewModel.events.filterIsInstance<SettingsEvent.SetPrayerReminderMinutes>()
                .map { it.minutes }
        ).containsExactly(15, 15, 15, 15, 15)
    }

    @Test
    fun `switching it off keeps the chosen lead time so switching back on restores it`() {
        // The minutes are written even while turning the reminder off, deliberately: otherwise
        // the next "on" silently reverts to the default rather than to what the user chose.
        setContent(
            NotificationSettingsUiState(showReminderBefore = true, reminderMinutes = 45)
        )

        composeRule.settingsRow(string(R.string.notif_all_prayers_reminder_title)).performClick()

        assertThat(viewModel.only<SettingsEvent.SetShowReminderBefore>().enabled).isFalse()
        assertThat(viewModel.only<SettingsEvent.SetReminderMinutes>().minutes).isEqualTo(45)
        assertThat(viewModel.events.filterIsInstance<SettingsEvent.SetPrayerReminderMinutes>())
            .isEmpty()
    }

    @Test
    fun `the bulk lead-time row reports no reminder while the bulk switch is off`() {
        setContent(
            NotificationSettingsUiState(showReminderBefore = false, reminderMinutes = 30)
        )

        composeRule.onNodeWithText(string(R.string.notif_reminder_none)).assertExists()
    }

    @Test
    fun `the bulk lead-time row reports the stored minutes while it is on`() {
        setContent(
            NotificationSettingsUiState(showReminderBefore = true, reminderMinutes = 30)
        )

        composeRule.onNodeWithText(minutesBefore(30)).assertExists()
    }

    @Test
    fun `picking a bulk lead time applies it to every prayer`() {
        setContent(
            NotificationSettingsUiState(showReminderBefore = true, reminderMinutes = 15)
        )

        composeRule.settingsRow(string(R.string.notif_all_prayers_lead_title)).performClick()
        composeRule.onNodeWithText(minutesBefore(60)).performClick()

        assertThat(viewModel.only<SettingsEvent.SetReminderMinutes>().minutes).isEqualTo(60)
        assertThat(
            viewModel.events.filterIsInstance<SettingsEvent.SetPrayerReminderMinutes>()
                .map { it.minutes }
        ).containsExactly(60, 60, 60, 60, 60)
    }

    @Test
    fun `the sections and the title render`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.notif_all_prayers_section)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.notif_prayers_section)).assertIsDisplayed()
    }

    @Test
    fun `the back button navigates back`() {
        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.cd_back)).performClick()

        assertThat(backs).isEqualTo(1)
    }
}
