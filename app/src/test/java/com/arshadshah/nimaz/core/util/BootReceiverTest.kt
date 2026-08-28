package com.arshadshah.nimaz.core.util

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.testing.TestEntryPointApplication
import com.google.common.truth.Truth.assertThat
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The recovery half of the old 812-line `BootReceiver`: what puts the alarms back.
 *
 * Everything this does is invisible when it stops working. Alarms do not survive a reboot, and
 * nothing else re-arms them — a regression here is prayer notifications that simply never fire
 * again, with no crash, no error state and no screen that shows an alarm was expected.
 *
 * The manifest half of each assertion below is checked by [BootReceiverManifestTest], because a
 * handled action with no `<intent-filter>` behind it is exactly the state
 * `ACTION_LOCKED_BOOT_COMPLETED` and the two `QUICKBOOT_POWERON` actions were in: live-looking
 * code in a branch nothing could ever reach.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestEntryPointApplication::class, sdk = [34])
class BootReceiverTest {

    private lateinit var context: Context
    private lateinit var rescheduler: PrayerRescheduler

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        rescheduler = mockk(relaxed = true)

        TestEntryPointApplication.Injector.reset()
        TestEntryPointApplication.Injector.bootReceiver = { it.prayerRescheduler = rescheduler }
    }

    private fun receive(action: String) = BootReceiver().onReceive(context, Intent(action))

    @Test
    fun `a reboot re-arms todays alarms, because nothing else does`() {
        receive(Intent.ACTION_BOOT_COMPLETED)

        coVerify(timeout = 2_000) { rescheduler.rescheduleToday() }
    }

    @Test
    fun `every boot spelling a manufacturer uses is handled, not just the standard one`() {
        // The "quickboot" OEMs broadcast their own action instead of ACTION_BOOT_COMPLETED;
        // missing one means that vendor's users lose notifications until they open the app.
        listOf(
            BootReceiver.ACTION_QUICKBOOT_POWERON,
            BootReceiver.ACTION_HTC_QUICKBOOT_POWERON,
        ).forEach(::receive)

        coVerify(timeout = 2_000, exactly = 2) { rescheduler.rescheduleToday() }
    }

    @Test
    fun `replacing the app re-arms every alarm, so an install that is never opened keeps working`() {
        // This is what carries an existing install across the BootReceiver -> PrayerAlarmReceiver
        // split. Every alarm armed by an older build names BootReceiver in its PendingIntent and
        // would fire into a receiver that no longer handles its action — silently. The OS takes
        // the app out of the stopped state to deliver MY_PACKAGE_REPLACED, so the re-arm happens
        // seconds after the update whether or not anyone opens the app.
        receive(Intent.ACTION_MY_PACKAGE_REPLACED)

        coVerify(timeout = 2_000) { rescheduler.rescheduleToday() }
    }

    @Test
    fun `locked boot is not answered, because nothing this does can run before first unlock`() {
        // It was in the `when` and had no manifest filter, so it never arrived. Making it arrive
        // would need directBootAware=true, and the reschedule reaches DataStore and Room — both
        // in credential-protected storage, both unreadable at that point.
        receive(Intent.ACTION_LOCKED_BOOT_COMPLETED)

        Thread.sleep(300)
        coVerify(exactly = 0) { rescheduler.rescheduleToday() }
    }

    @Test
    fun `an action the receiver does not own is ignored rather than crashing`() {
        receive("com.example.SOMETHING_ELSE")

        Thread.sleep(300)
        coVerify(exactly = 0) { rescheduler.rescheduleToday() }
    }

    @Test
    fun `an alarm action belongs to the other receiver and is not answered here`() {
        // Answering both would post the notification twice, and re-arm on every alarm.
        receive(PrayerNotificationScheduler.ACTION_PRAYER_NOTIFICATION)

        Thread.sleep(300)
        coVerify(exactly = 0) { rescheduler.rescheduleToday() }
    }

    @Test
    fun `a rescheduler that throws is reported rather than crashing the boot broadcast`() {
        // Crashing here is worse than one missed re-arm: the process dies during the boot
        // broadcast, and on some OEM builds that is remembered against the app.
        io.mockk.coEvery { rescheduler.rescheduleToday() } throws IllegalStateException("db closed")

        receive(Intent.ACTION_BOOT_COMPLETED)

        Thread.sleep(300)
        coVerify(timeout = 2_000) { rescheduler.rescheduleToday() }
    }

    @Test
    fun `every action the receiver handles reports a trigger, and boot and update are told apart`() {
        // The trigger is read as a funnel. Folding "package_replaced" into "boot" would make the
        // recovery above invisible in exactly the data that would show it working.
        assertThat(BootReceiver.TRIGGERS).containsExactly(
            Intent.ACTION_BOOT_COMPLETED, "boot",
            BootReceiver.ACTION_QUICKBOOT_POWERON, "boot",
            BootReceiver.ACTION_HTC_QUICKBOOT_POWERON, "boot",
            Intent.ACTION_MY_PACKAGE_REPLACED, "package_replaced",
        )
    }
}
