package com.arshadshah.nimaz.core.util

import android.content.Context
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager
import org.robolectric.annotation.Config

/**
 * The three device-level prerequisites for a prayer alert arriving on time, read from the OS.
 *
 * `hasProblem` is what the notifications hub's warning banner and its "needs attention" badge key
 * off, so getting it wrong goes one of two ways and both are bad: a banner that never appears
 * leaves someone wondering why their Fajr alert is twenty minutes late with nothing in the app to
 * explain it, and a banner that always appears trains them to ignore it before the day it is real.
 * `&&` where `||` belongs produces exactly the first.
 *
 * The API-31 boundary is the other half. `canScheduleExactAlarms` does not exist before Android 12
 * — exact alarms are simply always allowed there — so a version check that reported anything but
 * "fine" on an older device would invent a fault the user cannot fix.
 */
@RunWith(RobolectricTestRunner::class)
class NotificationDiagnosticsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun batteryExempt(exempt: Boolean) {
        shadowOf(context.getSystemService(PowerManager::class.java))
            .setIgnoringBatteryOptimizations(context.packageName, exempt)
    }

    /**
     * Robolectric denies exact alarms by default, which is the *realistic* default — Android 12
     * grants `SCHEDULE_EXACT_ALARM` to nobody without a user action — so the healthy case has to
     * be arranged rather than assumed.
     */
    private fun exactAlarmsAllowed(allowed: Boolean) {
        ShadowAlarmManager.setCanScheduleExactAlarms(allowed)
    }

    @Test
    fun `a device with all three prerequisites reports no problem`() {
        batteryExempt(true)
        exactAlarmsAllowed(true)

        val diagnostics = NotificationDiagnostics.read(context)

        assertThat(diagnostics.notificationsPermitted).isTrue()
        assertThat(diagnostics.exactAlarmsAllowed).isTrue()
        assertThat(diagnostics.batteryUnrestricted).isTrue()
        assertThat(diagnostics.hasProblem).isFalse()
    }

    @Test
    fun `battery optimisation on its own is a problem`() {
        // Doze holds alarms back, so this alone makes alerts late — which is the single most
        // common reason a prayer notification does not arrive when it should.
        exactAlarmsAllowed(true)
        batteryExempt(false)

        val diagnostics = NotificationDiagnostics.read(context)

        assertThat(diagnostics.batteryUnrestricted).isFalse()
        assertThat(diagnostics.hasProblem).isTrue()
    }

    @Test
    @Config(sdk = [30])
    fun `before Android 12 exact alarms are reported as allowed, not as a fault`() {
        // The permission does not exist on API 30, and nothing is arranged here on purpose:
        // reporting it as missing would put a warning on the hub that nothing on the device can
        // clear.
        batteryExempt(true)

        val diagnostics = NotificationDiagnostics.read(context)

        assertThat(diagnostics.exactAlarmsAllowed).isTrue()
        assertThat(diagnostics.hasProblem).isFalse()
    }

    @Test
    fun `a denied exact-alarm permission is a problem on Android 12 and later`() {
        // Without it the OS is free to batch the alarm, and a prayer alert that drifts by
        // minutes is wrong in the one way this app cannot afford.
        batteryExempt(true)
        exactAlarmsAllowed(false)

        val diagnostics = NotificationDiagnostics.read(context)

        assertThat(diagnostics.exactAlarmsAllowed).isFalse()
        assertThat(diagnostics.hasProblem).isTrue()
    }

    @Test
    fun `hasProblem is an OR over the three, not an AND`() {
        // Asserted on the data class directly rather than through the OS reads, because the
        // mixed cases are the ones an `&&` would get wrong and only one of them is arrangeable
        // from a shadow.
        assertThat(
            NotificationDiagnostics(
                notificationsPermitted = false,
                exactAlarmsAllowed = true,
                batteryUnrestricted = true,
            ).hasProblem
        ).isTrue()
        assertThat(
            NotificationDiagnostics(
                notificationsPermitted = true,
                exactAlarmsAllowed = false,
                batteryUnrestricted = true,
            ).hasProblem
        ).isTrue()
        assertThat(
            NotificationDiagnostics(
                notificationsPermitted = true,
                exactAlarmsAllowed = true,
                batteryUnrestricted = true,
            ).hasProblem
        ).isFalse()
    }
}
