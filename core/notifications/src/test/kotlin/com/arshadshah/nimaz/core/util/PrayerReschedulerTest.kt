package com.arshadshah.nimaz.core.util

import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.verify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * After a reboot, **nothing re-arms a prayer alarm except this**.
 *
 * If it regresses, prayer notifications stop — silently. There is no crash, no error state and
 * no screen that shows an alarm was expected, so a user notices weeks later if at all. That is
 * the failure these tests exist for, and why the logic was lifted out of `BootReceiver` (851
 * lines, a `BroadcastReceiver`, untestable) to somewhere it could have them.
 *
 * Verifications against [PrayerNotificationScheduler.scheduleTodaysPrayerNotifications] below are
 * named rather than positional: the scheduler takes eleven parameters, six of them defaulted, so
 * a positional `any()`-list silently stops matching the moment one is added.
 */
class PrayerReschedulerTest {

    private lateinit var settings: SettingsRepository
    private lateinit var scheduler: PrayerNotificationScheduler

    @Before
    fun setUp() {
        scheduler = mockk(relaxed = true)
        settings = mockk(relaxed = true) {
            every { userPreferences } returns flowOf(
                mockk(relaxed = true) {
                    every { latitude } returns 51.5
                    every { longitude } returns -0.12
                    every { prayerNotificationsEnabled } returns true
                }
            )
            // enabledPrayerTypes() reads these six flags one by one.
            every { fajrNotificationEnabled } returns flowOf(true)
            every { sunriseNotificationEnabled } returns flowOf(false)
            every { dhuhrNotificationEnabled } returns flowOf(true)
            every { asrNotificationEnabled } returns flowOf(false)
            every { maghribNotificationEnabled } returns flowOf(true)
            every { ishaNotificationEnabled } returns flowOf(false)
            every { fridayReminderEnabled } returns flowOf(true)
            every { fridayReminderMinutes } returns flowOf(30)
            // preReminderMinutesByPrayer() — nothing enabled, so no reminders.
            every { prayerReminderEnabled(any()) } returns flowOf(false)
            every { prayerReminderMinutes(any()) } returns flowOf(0)
        }
    }

    private fun rescheduler() = PrayerRescheduler(settings, scheduler)

    @Test
    fun `a reboot reschedules today from the stored preferences`() = runTest {
        val ok = rescheduler().rescheduleToday()

        assertThat(ok).isTrue()
        verify(exactly = 1) {
            scheduler.scheduleTodaysPrayerNotifications(
                latitude = 51.5,
                longitude = -0.12,
                notificationsEnabled = true,
                enabledPrayers = setOf(PrayerType.FAJR, PrayerType.DHUHR, PrayerType.MAGHRIB),
                preReminders = emptyMap(),
                fridayReminderEnabled = true,
                fridayReminderMinutes = 30,
            )
        }
    }

    /** Notifications switched off must reach the scheduler as off, not as "skip the call". */
    @Test
    fun `notifications disabled still reschedules, with the flag off`() = runTest {
        every { settings.userPreferences } returns flowOf(
            mockk(relaxed = true) {
                every { latitude } returns 51.5
                every { longitude } returns -0.12
                every { prayerNotificationsEnabled } returns false
            }
        )

        rescheduler().rescheduleToday()

        verify(exactly = 1) {
            scheduler.scheduleTodaysPrayerNotifications(
                latitude = any(),
                longitude = any(),
                notificationsEnabled = false,
                enabledPrayers = any(),
                preReminders = any(),
                fridayReminderEnabled = any(),
                fridayReminderMinutes = any(),
            )
        }
    }

    /**
     * A receiver has nowhere to propagate to, and crashing on boot is worse than one missed
     * re-arm — so a failure is reported and swallowed. It must not escape.
     */
    @Test
    fun `a scheduler failure is reported, not thrown`() = runTest {
        // `every`, not `coEvery` — scheduleTodaysPrayerNotifications is not a suspend function.
        every {
            scheduler.scheduleTodaysPrayerNotifications(
                latitude = any(),
                longitude = any(),
                notificationsEnabled = any(),
                enabledPrayers = any(),
                preReminders = any(),
                fridayReminderEnabled = any(),
                fridayReminderMinutes = any(),
            )
        } throws IllegalStateException("alarm manager said no")

        val ok = rescheduler().rescheduleToday()

        assertThat(ok).isFalse()
    }

    @Test
    fun `a preferences failure is reported, not thrown`() = runTest {
        every { settings.userPreferences } throws IllegalStateException("datastore corrupt")

        val ok = rescheduler().rescheduleToday()

        assertThat(ok).isFalse()
        verify(exactly = 0) {
            scheduler.scheduleTodaysPrayerNotifications(
                latitude = any(),
                longitude = any(),
                notificationsEnabled = any(),
                enabledPrayers = any(),
                preReminders = any(),
                fridayReminderEnabled = any(),
                fridayReminderMinutes = any(),
            )
        }
    }
}
