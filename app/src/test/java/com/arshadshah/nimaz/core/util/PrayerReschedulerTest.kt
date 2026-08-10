package com.arshadshah.nimaz.core.util

import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.domain.repository.PrayerRepository
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coVerify
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
 */
class PrayerReschedulerTest {

    private lateinit var settings: SettingsRepository
    private lateinit var scheduler: PrayerNotificationScheduler
    private lateinit var prayers: PrayerRepository

    @Before
    fun setUp() {
        scheduler = mockk(relaxed = true)
        prayers = mockk(relaxed = true)
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

    private fun rescheduler() = PrayerRescheduler(settings, scheduler, prayers)

    @Test
    fun `a reboot reschedules today from the stored preferences`() = runTest {
        val ok = rescheduler().rescheduleToday(markPastAsMissed = false)

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

    /**
     * A reboot must not touch prayer records. The day has not changed, so marking past prayers
     * missed here would overwrite what the user actually recorded.
     */
    @Test
    fun `a reboot does not mark past prayers as missed`() = runTest {
        rescheduler().rescheduleToday(markPastAsMissed = false)

        coVerify(exactly = 0) { prayers.markPastPrayersAsMissed() }
    }

    /** A date change or timezone shift is the case where marking *is* correct. */
    @Test
    fun `a date change marks past prayers missed before rescheduling`() = runTest {
        rescheduler().rescheduleToday(markPastAsMissed = true)

        coVerify(exactly = 1) { prayers.markPastPrayersAsMissed() }
        // Named rather than positional: the scheduler takes eleven parameters, six of them
        // defaulted, so a positional any()-list silently stops matching the moment one is added.
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
