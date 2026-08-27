package com.arshadshah.nimaz.presentation.screens.settings

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.WorshipReminderCategory
import com.arshadshah.nimaz.domain.model.WorshipReminderType
import com.arshadshah.nimaz.domain.worship.WorshipReminderCalculator
import com.arshadshah.nimaz.presentation.viewmodel.settings.NotificationSettingsUiState
import com.arshadshah.nimaz.presentation.viewmodel.settings.SettingsEvent
import com.arshadshah.nimaz.testing.FakeSettingsScreenViewModel
import com.arshadshah.nimaz.testing.accordionRow
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.arshadshah.nimaz.testing.tappableAncestorCount
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The eleven optional Sunnah reminders, in three categories.
 *
 * **The Ramadan group is shown year-round, and that is the point.** Hiding it outside Ramadan made
 * the app look like it had lost a feature and left no way to set the reminders up in advance;
 * nothing depends on their absence, because the scheduler gates them on the Hijri date itself. A
 * regression back to hiding them would be invisible for eleven months of the year, which is
 * exactly why it is asserted here rather than left to a glance at the screen.
 *
 * The rest is the partition. A reminder that can be timed becomes an accordion carrying its own
 * offset; the rest are plain switch rows. Getting that backwards either hides an offset the user
 * needs or puts an empty accordion under a reminder with nothing to set.
 *
 * Witr is the one with a mode as well as an offset — after Isha or before Fajr — and it is a
 * toggle rendered as a row, so the label has to report the *current* mode rather than the one the
 * tap would produce.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h4000dp")
class WorshipRemindersScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val viewModel = FakeSettingsScreenViewModel()
    private var backs = 0

    private fun setContent(state: NotificationSettingsUiState = NotificationSettingsUiState()) {
        viewModel.notificationState.value = state
        composeRule.setThemedContent {
            WorshipRemindersScreen(onNavigateBack = { backs++ }, viewModel = viewModel.mock)
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    @Test
    fun `all three categories are shown`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.worship_settings_section_night)).assertExists()
        composeRule.onNodeWithText(string(R.string.worship_settings_section_ramadan)).assertExists()
        composeRule.onNodeWithText(string(R.string.worship_settings_section_fasting)).assertExists()
    }

    @Test
    fun `the Ramadan reminders are offered outside Ramadan, with a notice rather than a gap`() {
        // The scheduler gates these on the Hijri date, so setting one in Sha'ban arms nothing
        // until Ramadan arrives. Hiding the group made the app look like it had lost a feature
        // for eleven months of the year.
        setContent()

        WorshipReminderType.entries
            .filter { it.category == WorshipReminderCategory.RAMADAN }
            .forEach { type ->
                composeRule.onNodeWithText(string(worshipName(type))).assertExists()
            }
        composeRule.onNodeWithText(string(R.string.worship_settings_ramadan_notice)).assertExists()
    }

    @Test
    fun `every declared reminder gets a row`() {
        // Two `when`s over the enum supply the name and the "when it fires" line. Adding a
        // twelfth type without touching them fails to compile — but a type moved between
        // categories silently disappears from the screen, which this catches.
        setContent()

        WorshipReminderType.entries.forEach { type ->
            composeRule.onNodeWithText(string(worshipName(type))).assertExists()
        }
    }

    @Test
    fun `the screen says up front that none of these carries an adhan`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.worship_settings_intro)).assertIsDisplayed()
    }

    @Test
    fun `a reminder with no timing is a plain row, not an empty accordion`() {
        // Laylatul Qadr has no offset and no mode. An accordion there would open onto nothing.
        setContent()

        composeRule.onNodeWithText(string(R.string.worship_when_laylatul_qadr)).assertExists()
        assertThat(
            composeRule.tappableAncestorCount(string(R.string.worship_laylatul_qadr_name))
        ).isEqualTo(1)
    }

    @Test
    fun `a switched-off timed reminder describes when it fires rather than its offset`() {
        setContent(NotificationSettingsUiState(worshipReminders = mapOf("suhoor" to false)))

        composeRule.onNodeWithText(string(R.string.worship_when_suhoor)).assertExists()
    }

    @Test
    fun `a switched-on timed reminder reports its own offset`() {
        setContent(
            NotificationSettingsUiState(
                worshipReminders = mapOf("suhoor" to true),
                worshipOffsets = mapOf("suhoor" to 45),
            )
        )

        composeRule.onNodeWithText(string(R.string.worship_settings_minutes, 45)).assertExists()
    }

    @Test
    fun `an offset with nothing stored falls back to that type's own default`() {
        // `?: type.defaultOffsetMinutes`, per type. One shared default would put Suhoor's
        // 30-minute lead on Iftar, whose default is 0 — a reminder at the wrong end of the fast.
        setContent(NotificationSettingsUiState(worshipReminders = mapOf("taraweeh" to true)))

        composeRule.onNodeWithText(
            string(R.string.worship_settings_minutes, WorshipReminderType.TARAWEEH.defaultOffsetMinutes)
        ).assertExists()
    }

    @Test
    fun `a switch writes its own reminder's key`() {
        // Eleven rows over one keyed surface. Nothing at the call site names the reminder except
        // the key, which is the one thing a copy-paste leaves unchanged.
        setContent()

        composeRule.onNodeWithText(string(R.string.worship_tahajjud_name)).performClick()

        val event = viewModel.only<SettingsEvent.SetWorshipReminderEnabled>()
        assertThat(event.key).isEqualTo(WorshipReminderType.TAHAJJUD.key)
        assertThat(event.enabled).isTrue()
    }

    @Test
    fun `Witr reports the mode it is in, not the one a tap would produce`() {
        setContent(
            NotificationSettingsUiState(
                worshipReminders = mapOf("witr" to true),
                worshipModes = mapOf("witr" to WorshipReminderCalculator.WITR_MODE_BEFORE_FAJR),
            )
        )

        composeRule.onNodeWithText(string(R.string.worship_witr_name)).performClick()

        // Asserted as "one node carries both strings" rather than "the screen shows this
        // string": "Before Fajr" is also *Suhoor's* when-it-fires line and "After Isha" is
        // Taraweeh's, so a screen-wide match would pass against a mode row showing nothing.
        assertThat(modeRowsShowing(R.string.worship_witr_mode_before_fajr)).isNotEmpty()
        assertThat(modeRowsShowing(R.string.worship_witr_mode_after_isha)).isEmpty()
    }

    @Test
    fun `Witr defaults to after Isha when no mode is stored`() {
        setContent(NotificationSettingsUiState(worshipReminders = mapOf("witr" to true)))

        composeRule.onNodeWithText(string(R.string.worship_witr_name)).performClick()

        assertThat(modeRowsShowing(R.string.worship_witr_mode_after_isha)).isNotEmpty()
        assertThat(modeRowsShowing(R.string.worship_witr_mode_before_fajr)).isEmpty()
    }

    @Test
    fun `tapping the Witr mode row flips it to the other mode`() {
        // It is a toggle wearing a row's clothes, so it must send the *opposite* of what it
        // shows. Sending the current mode makes the row inert and looks identical.
        setContent(
            NotificationSettingsUiState(
                worshipReminders = mapOf("witr" to true),
                worshipModes = mapOf("witr" to WorshipReminderCalculator.WITR_MODE_AFTER_ISHA),
            )
        )

        composeRule.onNodeWithText(string(R.string.worship_witr_name)).performClick()
        composeRule.accordionRow(string(R.string.worship_witr_mode_title)).performClick()

        val event = viewModel.only<SettingsEvent.SetWorshipReminderMode>()
        assertThat(event.key).isEqualTo(WorshipReminderType.WITR.key)
        assertThat(event.mode).isEqualTo(WorshipReminderCalculator.WITR_MODE_BEFORE_FAJR)
    }

    @Test
    fun `the Witr mode row is off-limits while the reminder is off`() {
        // Choosing when a reminder fires that will not fire is a setting with no effect.
        setContent(NotificationSettingsUiState(worshipReminders = mapOf("witr" to false)))

        composeRule.onNodeWithText(string(R.string.worship_witr_name)).performClick()

        assertThat(composeRule.tappableAncestorCount(string(R.string.worship_witr_mode_title)))
            .isEqualTo(1)
    }

    @Test
    fun `the offset stepper writes against the reminder whose accordion it is in`() {
        setContent(
            NotificationSettingsUiState(
                worshipReminders = mapOf("iftar" to true),
                worshipOffsets = mapOf("iftar" to 10),
            )
        )

        composeRule.onNodeWithText(string(R.string.worship_iftar_name)).performClick()
        composeRule
            .onAllNodesWithContentDescription(string(R.string.cd_increase), useUnmergedTree = true)
            .onFirst()
            .performClick()

        val event = viewModel.only<SettingsEvent.SetWorshipReminderOffset>()
        assertThat(event.key).isEqualTo(WorshipReminderType.IFTAR.key)
        assertThat(event.minutes).isEqualTo(15)
    }

    @Test
    fun `the back button navigates back`() {
        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.cd_back)).performClick()

        assertThat(backs).isEqualTo(1)
    }

    /** Nodes carrying the Witr timing label *and* [mode] — i.e. the mode row, reporting [mode]. */
    private fun modeRowsShowing(@StringRes mode: Int) =
        composeRule
            .onAllNodes(
                hasText(string(R.string.worship_witr_mode_title)) and hasText(string(mode))
            )
            .fetchSemanticsNodes()

    private fun worshipName(type: WorshipReminderType): Int = when (type) {
        WorshipReminderType.TAHAJJUD -> R.string.worship_tahajjud_name
        WorshipReminderType.WITR -> R.string.worship_witr_name
        WorshipReminderType.SUHOOR -> R.string.worship_suhoor_name
        WorshipReminderType.IFTAR -> R.string.worship_iftar_name
        WorshipReminderType.TARAWEEH -> R.string.worship_taraweeh_name
        WorshipReminderType.LAYLATUL_QADR -> R.string.worship_laylatul_qadr_name
        WorshipReminderType.ADHKAR_MORNING -> R.string.worship_adhkar_morning_name
        WorshipReminderType.ADHKAR_EVENING -> R.string.worship_adhkar_evening_name
        WorshipReminderType.MONDAY_THURSDAY_FAST -> R.string.worship_mon_thu_name
        WorshipReminderType.WHITE_DAYS_FAST -> R.string.worship_white_days_name
        WorshipReminderType.ARAFAH_ASHURA_FAST -> R.string.worship_arafah_ashura_name
    }
}
