package com.arshadshah.nimaz.presentation.screens.settings

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.HighLatitudeRule
import com.arshadshah.nimaz.presentation.viewmodel.settings.LocationSettingsUiState
import com.arshadshah.nimaz.presentation.viewmodel.settings.NotificationSummary
import com.arshadshah.nimaz.presentation.viewmodel.settings.PrayerSettingsUiState
import com.arshadshah.nimaz.presentation.viewmodel.settings.SettingsEvent
import com.arshadshah.nimaz.testing.FakeSettingsScreenViewModel
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.arshadshah.nimaz.testing.settingsRow
import com.arshadshah.nimaz.testing.testLocation
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Prayer calculation settings: the three method pickers, six manual offsets, and two rows that
 * report the notification state.
 *
 * The pickers matter because a wrong calculation method changes every prayer time in the app
 * without anything looking broken — the times are simply, quietly, someone else's. Each picker
 * therefore has to write the value the user chose rather than a positional index, and the three
 * pickers must not be crossed with each other: they open from three adjacent rows with the same
 * shape, and picking Hanafi from the "wrong" sheet would set a high-latitude rule.
 *
 * The manual offsets are six identical steppers in a row and the classic place for an off-by-one.
 * They are asserted by which prayer key each one writes, which is the only thing distinguishing
 * them in the source.
 *
 * The notification rows read a `WhileSubscribed` rollup collected from DataStore so they stay
 * true after an edit made on the notifications hub, which runs its own ViewModel instance. Their
 * four-arm `when` covers off, all-on, none-on and a count — and the difference between "No
 * prayers enabled" and "Notifications off" is a real one, because only the first is fixed here.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h4000dp")
class PrayerSettingsScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val viewModel = FakeSettingsScreenViewModel()
    private var backs = 0
    private var notifications = 0

    private fun setContent(
        state: PrayerSettingsUiState = PrayerSettingsUiState(),
        summary: NotificationSummary = NotificationSummary(),
        location: LocationSettingsUiState = LocationSettingsUiState(isLoading = false),
    ) {
        viewModel.prayerState.value = state
        viewModel.notificationSummary.value = summary
        viewModel.locationState.value = location
        composeRule.setThemedContent {
            PrayerSettingsScreen(
                onNavigateBack = { backs++ },
                onNavigateToNotifications = { notifications++ },
                viewModel = viewModel.mock,
            )
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    @Test
    fun `the three sections render`() {
        setContent()

        composeRule.onAllNodesWithText(string(R.string.calculation_method)).onFirst()
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.manual_adjustments)).assertExists()
        composeRule.onNodeWithText(string(R.string.notifications)).assertExists()
    }

    @Test
    fun `each row reports the method that is actually stored`() {
        setContent(
            PrayerSettingsUiState(
                calculationMethod = CalculationMethod.KARACHI,
                asrMethod = AsrCalculation.HANAFI,
                highLatitudeRule = HighLatitudeRule.TWILIGHT_ANGLE,
            )
        )

        composeRule.onNodeWithText(CalculationMethod.KARACHI.displayName()).assertExists()
        composeRule.onAllNodesWithText(string(R.string.asr_hanafi)).onFirst().assertExists()
        composeRule.onAllNodesWithText(string(R.string.twilight_angle)).onFirst().assertExists()
    }

    @Test
    fun `the calculation picker offers every method with the region it is used in`() {
        // "Used in Pakistan" beats "Karachi" if you do not already know the acronym, and the
        // `when` supplying those descriptions is the shape that goes stale when a method is
        // added — it would not compile, but a *renamed* one silently keeps the old region.
        setContent()

        composeRule.settingsRow(string(R.string.calculation_method)).performClick()

        // The sheet is a `LazyColumn`, so only the first screenful composes — the assertion is
        // that the list is built from the enum with a region beside each, not that all eleven
        // are laid out at once.
        CalculationMethod.entries.take(3).forEach { method ->
            composeRule.onAllNodesWithText(method.displayName()).onFirst().assertExists()
        }
        composeRule.onNodeWithText(string(R.string.calc_region_karachi)).assertExists()
    }

    @Test
    fun `picking a calculation method sends that method`() {
        setContent(PrayerSettingsUiState(calculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE))

        composeRule.settingsRow(string(R.string.calculation_method)).performClick()
        composeRule.onNodeWithText(string(R.string.calc_region_egyptian)).performClick()

        assertThat(viewModel.only<SettingsEvent.SetCalculationMethod>().method)
            .isEqualTo(CalculationMethod.EGYPTIAN)
    }

    @Test
    fun `the asr picker offers both schools and writes the one chosen`() {
        setContent(PrayerSettingsUiState(asrMethod = AsrCalculation.STANDARD))

        composeRule.settingsRow(string(R.string.asr_calculation)).performClick()
        composeRule.onNodeWithText(string(R.string.asr_hanafi_desc)).performClick()

        assertThat(viewModel.only<SettingsEvent.SetAsrMethod>().method)
            .isEqualTo(AsrCalculation.HANAFI)
    }

    @Test
    fun `the high-latitude picker offers all three rules and writes the one chosen`() {
        // Three adjacent rows opening three same-shaped sheets. A picker wired to its
        // neighbour's event would set an Asr school from the high-latitude sheet.
        setContent(PrayerSettingsUiState(highLatitudeRule = HighLatitudeRule.MIDDLE_OF_THE_NIGHT))

        composeRule.settingsRow(string(R.string.high_latitude_method)).performClick()
        composeRule.onNodeWithText(string(R.string.high_lat_seventh_desc)).performClick()

        assertThat(viewModel.only<SettingsEvent.SetHighLatitudeRule>().rule)
            .isEqualTo(HighLatitudeRule.SEVENTH_OF_THE_NIGHT)
        assertThat(viewModel.events.filterIsInstance<SettingsEvent.SetAsrMethod>()).isEmpty()
    }

    @Test
    fun `each manual offset stepper writes its own prayer`() {
        // Six identical steppers whose only distinguishing feature is a string literal. An
        // off-by-one here moves the wrong prayer by the right number of minutes, which reads as
        // a calculation bug rather than a settings one.
        setContent()

        val increments =
            composeRule.onAllNodesWithContentDescription(string(R.string.cd_increase))
        increments[0].performClick()
        assertThat(viewModel.only<SettingsEvent.SetPrayerAdjustment>().prayer).isEqualTo("fajr")
        viewModel.events.clear()

        increments[3].performClick()
        assertThat(viewModel.only<SettingsEvent.SetPrayerAdjustment>().prayer).isEqualTo("asr")
        viewModel.events.clear()

        increments[5].performClick()
        assertThat(viewModel.only<SettingsEvent.SetPrayerAdjustment>().prayer).isEqualTo("isha")
    }

    @Test
    fun `an offset stepper starts from the value that prayer already holds`() {
        setContent(PrayerSettingsUiState(dhuhrAdjustment = 4))

        composeRule.onAllNodesWithContentDescription(string(R.string.cd_increase))[2].performClick()

        val event = viewModel.only<SettingsEvent.SetPrayerAdjustment>()
        assertThat(event.prayer).isEqualTo("dhuhr")
        assertThat(event.minutes).isEqualTo(5)
    }

    @Test
    fun `an offset can be moved backwards as well as forwards`() {
        setContent(PrayerSettingsUiState(maghribAdjustment = 0))

        composeRule.onAllNodesWithContentDescription(string(R.string.cd_decrease))[4].performClick()

        val event = viewModel.only<SettingsEvent.SetPrayerAdjustment>()
        assertThat(event.prayer).isEqualTo("maghrib")
        assertThat(event.minutes).isEqualTo(-1)
    }

    @Test
    fun `the notification row says all prayers are on when all five are`() {
        setContent(summary = NotificationSummary(enabledPrayerCount = 5))

        composeRule.onNodeWithText(string(R.string.prayer_settings_all_prayers_enabled))
            .assertExists()
    }

    @Test
    fun `the notification row counts the prayers when only some are on`() {
        setContent(summary = NotificationSummary(enabledPrayerCount = 2))

        composeRule.onNodeWithText(string(R.string.prayer_settings_prayers_enabled_count, 2, 5))
            .assertExists()
    }

    @Test
    fun `no prayers enabled is reported differently from notifications being off`() {
        // Only the first is fixable here; the second is the master switch on another screen.
        // One string for both would send someone to the wrong place.
        setContent(
            summary = NotificationSummary(
                notificationsMasterEnabled = true,
                enabledPrayerCount = 0,
            )
        )

        composeRule.onNodeWithText(string(R.string.prayer_settings_no_prayers_enabled))
            .assertExists()
    }

    @Test
    fun `the master switch being off outranks the per-prayer count`() {
        // Five prayers "enabled" under a master switch that is off means no notification will
        // arrive, so reporting "All prayers enabled" there would be a lie about delivery.
        setContent(
            summary = NotificationSummary(
                notificationsMasterEnabled = false,
                enabledPrayerCount = 5,
            )
        )

        composeRule.onNodeWithText(string(R.string.prayer_settings_notifications_off))
            .assertExists()
        composeRule.onNodeWithText(string(R.string.prayer_settings_all_prayers_enabled))
            .assertDoesNotExist()
    }

    @Test
    fun `the Fajr reminder row reports its lead time, or says it is off`() {
        setContent(summary = NotificationSummary(reminderEnabled = true, reminderMinutes = 25))

        composeRule.onNodeWithText(
            context.resources.getQuantityString(R.plurals.notif_reminder_minutes_before, 25, 25)
        ).assertExists()
    }

    @Test
    fun `a Fajr reminder that is off says so rather than showing a lead time`() {
        setContent(summary = NotificationSummary(reminderEnabled = false, reminderMinutes = 25))

        composeRule.onNodeWithText(string(R.string.prayer_settings_reminder_off)).assertExists()
    }

    @Test
    fun `both notification rows open the notifications hub`() {
        setContent()

        composeRule.settingsRow(string(R.string.adhan_notifications)).performClick()
        composeRule.settingsRow(string(R.string.prayer_settings_fajr_reminder)).performClick()

        assertThat(notifications).isEqualTo(2)
    }

    @Test
    fun `the high-latitude notice names the user's own city`() {
        setContent(location = LocationSettingsUiState(currentLocation = testLocation(city = "Oslo")))

        composeRule.onNodeWithText(
            string(R.string.prayer_settings_high_latitude_notice_format, "Oslo")
        ).assertExists()
    }

    @Test
    fun `the notice falls back to a neutral phrase when no city is known`() {
        // The location arrives asynchronously, so this renders on every cold open. A non-null
        // assumption crashes the screen; a raw "null" in the sentence is worse than a fallback.
        setContent(location = LocationSettingsUiState(currentLocation = null))

        composeRule.onNodeWithText(
            string(
                R.string.prayer_settings_high_latitude_notice_format,
                string(R.string.prayer_settings_your_location),
            )
        ).assertExists()
    }

    @Test
    fun `the back button navigates back`() {
        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.cd_back)).performClick()

        assertThat(backs).isEqualTo(1)
    }
}
