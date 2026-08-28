package com.arshadshah.nimaz.notifications

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arshadshah.nimaz.core.common.NimazChannels
import com.arshadshah.nimaz.core.util.PrayerNotificationScheduler
import com.arshadshah.nimaz.domain.model.PrayerType
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Exercises the [PrayerNotificationScheduler] — the singleton that owns the app's
 * notification channels and schedules adhan/prayer alarms.
 *
 * Constructing it (via Hilt) runs its `init { createNotificationChannels() }`, so the
 * test asserts the three channels are registered with the system. It also smoke-tests
 * the public scheduling / cancellation entry points to guard against crashes on
 * AlarmManager / PendingIntent / NotificationCompat usage across API levels.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class PrayerNotificationSchedulerTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var scheduler: PrayerNotificationScheduler

    private val notificationManager: NotificationManager
        get() = ApplicationProvider.getApplicationContext<Context>()
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    @Before
    fun setup() = hiltRule.inject()

    @Test
    fun construction_registersAllNotificationChannels() {
        // Touch the injected singleton so its init block has definitely run.
        assertThat(scheduler).isNotNull()

        val channelIds = notificationManager.notificationChannels.map { it.id }
        assertThat(channelIds).containsAtLeast(
            NimazChannels.PRAYER,
            NimazChannels.ADHAN,
            NimazChannels.DAILY_SUMMARY,
            // The silent alert style needs a channel of its own — the *_SILENT channels are
            // no-vibration siblings that still carry a sound.
            NimazChannels.PRAYER_MUTED,
        )
    }

    @Test
    fun scheduleTodaysPrayerNotifications_doesNotThrow() {
        scheduler.scheduleTodaysPrayerNotifications(
            latitude = 21.4225,
            longitude = 39.8262,
            notificationsEnabled = true,
            enabledPrayers = PrayerType.entries.toSet(),
            preReminders = PrayerType.entries.associateWith { 15 },
        )
    }

    @Test
    fun cancellation_entryPoints_areSafe() {
        scheduler.scheduleTodaysPrayerNotifications(
            latitude = 21.4225,
            longitude = 39.8262,
            notificationsEnabled = true,
        )

        // Cancelling individual + all prayers, and the daily summary, must be no-throw
        // even when nothing was scheduled for a given prayer.
        PrayerType.entries.forEach { scheduler.cancelPrayerNotification(it) }
        scheduler.cancelAllPrayerNotifications()
        scheduler.cancelDailySummary()
    }
}
