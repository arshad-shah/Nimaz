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
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.HighLatitudeRule
import com.arshadshah.nimaz.domain.model.PrayerType
import java.time.LocalDateTime
import com.arshadshah.nimaz.core.common.formatClockTime
import com.arshadshah.nimaz.presentation.viewmodel.settings.NotificationSummary
import com.arshadshah.nimaz.presentation.viewmodel.settings.PrayerPreviewUiState
import com.arshadshah.nimaz.presentation.viewmodel.settings.PrayerSettingsUiState
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
import org.robolectric.annotation.Config

/**
 * Prayer calculation settings: a live preview of today's times, the three method pickers, six
 * manual offsets in their own sheet, and one row that reports the reminder state.
 *
 * The pickers matter because a wrong calculation method changes every prayer time in the app
 * without anything looking broken — the times are simply, quietly, someone else's. Each picker
 * therefore has to write the value the user chose rather than a positional index, and the three
 * pickers must not be crossed with each other: they open from three adjacent rows with the same
 * shape, and picking Hanafi from the "wrong" sheet would set a high-latitude rule.
 *
 * The manual offsets are six identical steppers in a row and the classic place for an off-by-one.
 * They are asserted by which prayer key each one writes, which is the only thing distinguishing
 * them in the source. They live in a sheet now, so a scroll of the page cannot land on one.
 *
 * The reminders row reads a `WhileSubscribed` rollup collected from DataStore so it stays true
 * after an edit made on the notifications hub, which runs its own ViewModel instance. Its
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
        preview: PrayerPreviewUiState = PrayerPreviewUiState(),
    ) {
        viewModel.prayerState.value = state
        viewModel.notificationSummary.value = summary
        viewModel.prayerPreview.value = preview
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

    private fun at(h: Int, m: Int) = LocalDateTime.of(2026, 9, 23, h, m)

    /** A time as the screen writes it — the app's own formatter, in its default 12-hour form. */
    private fun clock(h: Int, m: Int) = formatClockTime(h, m, use24Hour = false)

    /** Dublin on 23 September 2026 under the Muslim World League method. */
    private val dublin = PrayerPreviewUiState(
        times = mapOf(
            PrayerType.FAJR to at(5, 13), PrayerType.SUNRISE to at(7, 12), PrayerType.DHUHR to at(13, 17),
            PrayerType.ASR to at(16, 31), PrayerType.MAGHRIB to at(19, 21), PrayerType.ISHA to at(21, 13),
        ),
        methodFajr = mapOf(CalculationMethod.MUSLIM_WORLD_LEAGUE to at(5, 13), CalculationMethod.EGYPTIAN to at(5, 2)),
        asrTimes = mapOf(AsrCalculation.STANDARD to at(16, 31), AsrCalculation.HANAFI to at(17, 18)),
        locationName = "Dublin",
        latitude = 53.35,
    )

    private fun openAdjustments() {
        composeRule.settingsRow(string(R.string.prayer_settings_adjust_title)).performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun `the preview and the three sections render`() {
        setContent(preview = dublin)

        composeRule.onNodeWithText(string(R.string.prayer_preview_today, "Dublin")).assertExists()
        composeRule.onNodeWithText(string(R.string.prayer_preview_note_default)).assertExists()
        composeRule.onNodeWithText(string(R.string.prayer_settings_section_calculation)).assertExists()
        composeRule.onNodeWithText(string(R.string.prayer_settings_section_finetune)).assertExists()
        composeRule.onNodeWithText(string(R.string.prayer_settings_section_reminders)).assertExists()
    }

    @Test
    fun `the preview speaks today's times as one sentence`() {
        // The arc is a drawing, so its content description is the whole accessible timetable.
        setContent(preview = dublin)

        composeRule.onAllNodes(hasContentDescription("Fajr ${clock(5, 13)}", substring = true)).onFirst().assertExists()
        composeRule.onAllNodes(hasContentDescription("Isha ${clock(21, 13)}", substring = true)).onFirst().assertExists()
    }

    @Test
    fun `a change the reader just made is called out`() {
        setContent(preview = dublin.copy(changed = setOf(PrayerType.FAJR, PrayerType.ISHA)))

        composeRule.onNodeWithText(string(R.string.prayer_preview_note_changed)).assertExists()
        composeRule.onNodeWithText(string(R.string.prayer_preview_note_default)).assertDoesNotExist()
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

        composeRule.onAllNodesWithText(CalculationMethod.KARACHI.displayName()).onFirst().assertExists()
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
        composeRule.onNodeWithText(string(R.string.prayer_method_picker_body)).assertExists()
    }

    @Test
    fun `the calculation picker shows today's Fajr under each method`() {
        // Methods differ by a degree or two of twilight; the minutes that makes is the thing a
        // reader can actually compare.
        setContent(preview = dublin)

        composeRule.settingsRow(string(R.string.calculation_method)).performClick()

        composeRule.onNodeWithText(string(R.string.prayer_picker_fajr_at, clock(5, 13))).assertExists()
        composeRule.onNodeWithText(string(R.string.prayer_picker_fajr_at, clock(5, 2))).assertExists()
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
    fun `the asr picker offers both schools, their times, and writes the one chosen`() {
        setContent(PrayerSettingsUiState(asrMethod = AsrCalculation.STANDARD), preview = dublin)

        composeRule.settingsRow(string(R.string.asr_calculation)).performClick()
        composeRule.onNodeWithText(clock(17, 18)).assertExists()
        composeRule.onNodeWithText(string(R.string.asr_hanafi_desc)).performClick()

        assertThat(viewModel.only<SettingsEvent.SetAsrMethod>().method)
            .isEqualTo(AsrCalculation.HANAFI)
    }

    @Test
    fun `cancelling the asr picker puts back the school it opened with`() {
        // The sheet stays open so its picture can change with the choice, which means every tap
        // has already applied. A Cancel that only closed it would keep the choice backed out of.
        setContent(PrayerSettingsUiState(asrMethod = AsrCalculation.STANDARD))

        composeRule.settingsRow(string(R.string.asr_calculation)).performClick()
        composeRule.onNodeWithText(string(R.string.asr_hanafi_desc)).performClick()
        viewModel.prayerState.value = PrayerSettingsUiState(asrMethod = AsrCalculation.HANAFI)
        composeRule.waitForIdle()
        composeRule.onNodeWithText(string(R.string.cancel)).performClick()

        assertThat(viewModel.events.filterIsInstance<SettingsEvent.SetAsrMethod>().map { it.method })
            .containsExactly(AsrCalculation.HANAFI, AsrCalculation.STANDARD).inOrder()
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
    fun `the offsets are not on the page, only in their sheet`() {
        // A scroll that lands on a stepper moves a prayer time without anyone noticing.
        setContent()

        composeRule.onAllNodesWithContentDescription(string(R.string.cd_increase)).assertCountEquals(0)
        composeRule.onNodeWithText(string(R.string.prayer_settings_adjust_hint)).assertExists()
    }

    @Test
    fun `each manual offset stepper writes its own prayer`() {
        // Six identical steppers whose only distinguishing feature is a string literal. An
        // off-by-one here moves the wrong prayer by the right number of minutes, which reads as
        // a calculation bug rather than a settings one.
        setContent()
        openAdjustments()

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
        openAdjustments()

        composeRule.onAllNodesWithContentDescription(string(R.string.cd_increase))[2].performClick()

        val event = viewModel.only<SettingsEvent.SetPrayerAdjustment>()
        assertThat(event.prayer).isEqualTo("dhuhr")
        assertThat(event.minutes).isEqualTo(5)
    }

    @Test
    fun `an offset can be moved backwards as well as forwards`() {
        setContent(PrayerSettingsUiState(maghribAdjustment = 0))
        openAdjustments()

        composeRule.onAllNodesWithContentDescription(string(R.string.cd_decrease))[4].performClick()

        val event = viewModel.only<SettingsEvent.SetPrayerAdjustment>()
        assertThat(event.prayer).isEqualTo("maghrib")
        assertThat(event.minutes).isEqualTo(-1)
    }

    @Test
    fun `the offsets row counts the prayers that are shifted`() {
        setContent(PrayerSettingsUiState(fajrAdjustment = 2, ishaAdjustment = -3))

        composeRule.onNodeWithText(
            context.resources.getQuantityString(R.plurals.prayer_settings_adjusted_count, 2, 2)
        ).assertExists()
    }

    @Test
    fun `each offset shows the time it results in`() {
        setContent(preview = dublin)
        openAdjustments()

        composeRule.onAllNodesWithText(clock(5, 13)).onFirst().assertExists()
        composeRule.onAllNodesWithText(clock(21, 13)).onFirst().assertExists()
    }

    @Test
    fun `the reminders row says all prayers are on when all five are`() {
        setContent(summary = NotificationSummary(enabledPrayerCount = 5))

        composeRule.onNodeWithText(string(R.string.prayer_settings_all_prayers_enabled), substring = true)
            .assertExists()
    }

    @Test
    fun `the reminders row counts the prayers when only some are on`() {
        setContent(summary = NotificationSummary(enabledPrayerCount = 2))

        composeRule.onNodeWithText(string(R.string.prayer_settings_prayers_enabled_count, 2, 5), substring = true)
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

        composeRule.onNodeWithText(string(R.string.prayer_settings_no_prayers_enabled), substring = true)
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
        composeRule.onNodeWithText(string(R.string.prayer_settings_all_prayers_enabled), substring = true)
            .assertDoesNotExist()
    }

    @Test
    fun `the reminders row reports Fajr's lead time, or says it is off`() {
        setContent(summary = NotificationSummary(reminderEnabled = true, reminderMinutes = 25))

        composeRule.onNodeWithText(
            context.resources.getQuantityString(R.plurals.notif_reminder_minutes_before, 25, 25),
            substring = true,
        ).assertExists()
    }

    @Test
    fun `a Fajr reminder that is off says so rather than showing a lead time`() {
        setContent(summary = NotificationSummary(reminderEnabled = false, reminderMinutes = 25))

        composeRule.onNodeWithText(string(R.string.prayer_settings_reminder_off), substring = true).assertExists()
    }

    @Test
    fun `the reminders row opens the notifications hub`() {
        setContent()

        composeRule.settingsRow(string(R.string.prayer_settings_reminders_title)).performClick()

        assertThat(notifications).isEqualTo(1)
    }

    @Test
    fun `the high-latitude notice names the city and its latitude when the rule matters`() {
        setContent(preview = dublin.copy(locationName = "Oslo", latitude = 59.91))

        composeRule.onNodeWithText(
            string(R.string.prayer_settings_high_lat_notice, "Oslo", 60,
                string(R.string.prayer_latitude_north), string(R.string.middle_of_night))
        ).assertExists()
    }

    @Test
    fun `there is no high-latitude notice nearer the equator`() {
        // It used to show for everyone, warning a reader in Dubai about summer nights in Oslo.
        setContent(preview = dublin.copy(locationName = "Dubai", latitude = 25.2))

        composeRule.onNodeWithText(string(R.string.prayer_preview_today, "Dubai")).assertExists()
        composeRule.onNodeWithText(
            string(R.string.prayer_settings_high_lat_notice, "Dubai", 25,
                string(R.string.prayer_latitude_north), string(R.string.middle_of_night))
        ).assertDoesNotExist()
    }

    @Test
    fun `there is no notice for a fallback location the reader is not in`() {
        // Naming the fallback city's latitude would be a claim about a place the reader is not.
        setContent(preview = dublin.copy(locationName = null, latitude = 53.35, isFallbackLocation = true))

        composeRule.onNodeWithText(string(R.string.prayer_preview_today_plain)).assertExists()
        composeRule.onNodeWithText(
            string(R.string.prayer_settings_high_lat_notice, "Dublin", 53,
                string(R.string.prayer_latitude_north), string(R.string.middle_of_night))
        ).assertDoesNotExist()
    }

    @Test
    fun `the back button navigates back`() {
        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.cd_back)).performClick()

        assertThat(backs).isEqualTo(1)
    }
}
