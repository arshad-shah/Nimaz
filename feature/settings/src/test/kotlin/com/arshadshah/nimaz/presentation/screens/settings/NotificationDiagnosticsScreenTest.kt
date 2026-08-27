package com.arshadshah.nimaz.presentation.screens.settings

import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.viewmodel.settings.SettingsEvent
import com.arshadshah.nimaz.testing.FakeSettingsScreenViewModel
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import org.robolectric.shadows.ShadowToast

/**
 * Diagnostics: what the OS is currently allowing, and the three actions that fix it.
 *
 * The badges are the whole screen. Someone opens this because their prayer alerts are not
 * arriving, so a row reporting "Granted" while the permission is denied does not just fail to
 * help — it sends them looking for the fault somewhere else entirely. Each badge is therefore
 * asserted against an arranged device state rather than against the default.
 *
 * Every row is tappable regardless of whether it passes, deliberately: making only the failing
 * ones tappable leaves a row that looks like its neighbours and does nothing, and someone who
 * wants to see *why* a check passes has nowhere to go. The `Intent` each one starts is what makes
 * that useful, and it is readable from the shadow.
 *
 * Reset is destructive — it cancels every armed alarm before rebuilding — so it asks first, and
 * the cancel path must not dispatch.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class NotificationDiagnosticsScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val viewModel = FakeSettingsScreenViewModel()
    private var backs = 0

    private fun healthyDevice() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        shadowOf(context.getSystemService(PowerManager::class.java))
            .setIgnoringBatteryOptimizations(context.packageName, true)
    }

    private fun setContent() {
        composeRule.setThemedContent {
            NotificationDiagnosticsScreen(
                onNavigateBack = { backs++ },
                viewModel = viewModel.mock,
            )
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    private fun lastStartedAction(): String? =
        shadowOf(ApplicationProvider.getApplicationContext<android.app.Application>())
            .nextStartedActivity?.action

    @Test
    fun `all three checks are listed`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.notif_diag_permission)).assertExists()
        composeRule.onNodeWithText(string(R.string.notif_diag_exact_alarms)).assertExists()
        composeRule.onNodeWithText(string(R.string.notif_diag_battery)).assertExists()
        composeRule.onNodeWithText(string(R.string.notif_diag_status_section)).assertIsDisplayed()
    }

    @Test
    fun `a healthy device reports all three as passing`() {
        healthyDevice()

        setContent()

        composeRule.onNodeWithText(string(R.string.notif_diag_granted)).assertExists()
        composeRule.onNodeWithText(string(R.string.notif_diag_allowed)).assertExists()
        composeRule.onNodeWithText(string(R.string.notif_diag_unrestricted)).assertExists()
    }

    @Test
    fun `a denied exact-alarm permission is reported as not allowed, not as allowed`() {
        // The badge is the only evidence on the device that this is why the adhan is late.
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        shadowOf(context.getSystemService(PowerManager::class.java))
            .setIgnoringBatteryOptimizations(context.packageName, true)

        setContent()

        composeRule.onNodeWithText(string(R.string.notif_diag_not_allowed)).assertExists()
        composeRule.onNodeWithText(string(R.string.notif_diag_allowed)).assertDoesNotExist()
    }

    @Test
    fun `battery restriction is reported as restricted`() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        shadowOf(context.getSystemService(PowerManager::class.java))
            .setIgnoringBatteryOptimizations(context.packageName, false)

        setContent()

        composeRule.onNodeWithText(string(R.string.notif_diag_restricted)).assertExists()
        composeRule.onNodeWithText(string(R.string.notif_diag_unrestricted)).assertDoesNotExist()
    }

    @Test
    fun `each row opens the system screen it reports on, not a neighbour's`() {
        // Three near-identical rows, each carrying a different `Intent` action. Crossing two of
        // them sends someone to the battery screen to fix a notification permission.
        healthyDevice()
        setContent()

        composeRule.onNodeWithText(string(R.string.notif_diag_permission)).performClick()
        assertThat(lastStartedAction()).isEqualTo(Settings.ACTION_APP_NOTIFICATION_SETTINGS)

        composeRule.onNodeWithText(string(R.string.notif_diag_exact_alarms)).performClick()
        assertThat(lastStartedAction())
            .isEqualTo(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)

        composeRule.onNodeWithText(string(R.string.notif_diag_battery)).performClick()
        assertThat(lastStartedAction())
            .isEqualTo(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    }

    @Test
    fun `a passing row is still tappable`() {
        // Deliberate: a row that looked like its neighbours and did nothing would be worse than
        // one that explains a check that already passes.
        healthyDevice()
        setContent()

        composeRule.onNodeWithText(string(R.string.notif_diag_permission)).performClick()

        assertThat(lastStartedAction()).isEqualTo(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
    }

    @Test
    fun `the single test notification is sent and confirmed`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.notification_settings_test)).performClick()

        assertThat(viewModel.events).contains(SettingsEvent.TestNotification)
        assertThat(viewModel.events).doesNotContain(SettingsEvent.TestAllNotifications)
        assertThat(ShadowToast.getTextOfLatestToast())
            .isEqualTo(string(R.string.notification_settings_test_sent))
    }

    @Test
    fun `the all-prayers test is a different action with a different confirmation`() {
        // Two adjacent buttons, one word apart. Crossing them makes "test all five" send one.
        setContent()

        composeRule.onNodeWithText(string(R.string.notification_settings_test_all)).performClick()

        assertThat(viewModel.events).contains(SettingsEvent.TestAllNotifications)
        assertThat(viewModel.events).doesNotContain(SettingsEvent.TestNotification)
        assertThat(ShadowToast.getTextOfLatestToast())
            .isEqualTo(string(R.string.notification_settings_test_all_sent))
    }

    @Test
    fun `reset asks before cancelling every armed alarm`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.notification_settings_reset)).performClick()

        composeRule.onNodeWithText(string(R.string.notif_diag_reset_confirm_title)).assertExists()
        assertThat(viewModel.events).isEmpty()
    }

    @Test
    fun `cancelling the reset dialog resets nothing`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.notification_settings_reset)).performClick()
        composeRule.onNodeWithText(string(R.string.cancel)).performClick()

        assertThat(viewModel.events).doesNotContain(SettingsEvent.ResetNotifications)
    }

    @Test
    fun `confirming the reset dispatches it and confirms`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.notification_settings_reset)).performClick()
        composeRule.onNodeWithText(string(R.string.notif_diag_reset_confirm_action)).performClick()

        assertThat(viewModel.events).contains(SettingsEvent.ResetNotifications)
        assertThat(ShadowToast.getTextOfLatestToast())
            .isEqualTo(string(R.string.notification_settings_reset_success))
    }

    @Test
    fun `the screen explains why battery optimisation matters`() {
        setContent()

        composeRule
            .onNodeWithText(string(R.string.notification_settings_battery_explanation))
            .assertExists()
    }

    @Test
    fun `the back button navigates back`() {
        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.cd_back)).performClick()

        assertThat(backs).isEqualTo(1)
    }
}
