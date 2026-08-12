package com.arshadshah.nimaz.core.util

import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.repository.PrayerRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Re-arms today's prayer notifications from the current preferences.
 *
 * Extracted from [BootReceiver], where it existed twice. `reschedulePrayerNotifications` and
 * `markMissedPrayersAndReschedule` were the same twenty-odd lines — read seven preferences, call
 * `scheduleTodaysPrayerNotifications` with all of them — differing only in the
 * `markPastPrayersAsMissed()` call at the top of the second and the string in its catch block.
 * Two copies of the code that decides whether a user is reminded to pray, free to drift apart.
 *
 * It is a class rather than a private helper because of what depends on it: after a reboot,
 * **nothing re-arms an alarm except this**. If it regresses, prayer notifications simply stop,
 * and they stop *silently* — there is no crash, no error state, and no screen that shows an
 * alarm was expected. A user notices weeks later, if at all. `BootReceiver` is 851 lines with a
 * `BroadcastReceiver` at the top of it and cannot be unit-tested; this can.
 *
 * Typed to [SettingsRepository], not the concrete `PreferencesDataStore` the receiver injects:
 * the extension functions it calls (`enabledPrayerTypes`, `preReminderMinutesByPrayer`) are
 * declared on the interface anyway, and depending on the interface is both the house rule and
 * what lets a test hand it a fake instead of a DataStore.
 */
@Singleton
class PrayerRescheduler @Inject constructor(
    private val preferences: SettingsRepository,
    private val scheduler: PrayerNotificationScheduler,
    private val prayerRepository: PrayerRepository,
) {

    /**
     * Re-arm today's notifications.
     *
     * @param markPastAsMissed first mark prayers whose time has passed as missed. True on a
     *   date change or a timezone shift, where the day being re-armed is not the day that was
     *   scheduled; false after a reboot, where the day has not changed and marking would
     *   overwrite what the user actually did.
     * @return true when the reschedule completed. Failures are reported and swallowed — a
     *   receiver has nowhere to propagate to, and crashing on boot is worse than one missed
     *   re-arm — so the return value exists for tests and callers that want to know.
     */
    suspend fun rescheduleToday(markPastAsMissed: Boolean = false): Boolean = try {
        if (markPastAsMissed) {
            prayerRepository.markPastPrayersAsMissed()
        }

        val prefs = preferences.userPreferences.first()

        scheduler.scheduleTodaysPrayerNotifications(
            latitude = prefs.latitude,
            longitude = prefs.longitude,
            notificationsEnabled = prefs.prayerNotificationsEnabled,
            enabledPrayers = preferences.enabledPrayerTypes(),
            preReminders = preferences.preReminderMinutesByPrayer(),
            fridayReminderEnabled = preferences.fridayReminderEnabled.first(),
            fridayReminderMinutes = preferences.fridayReminderMinutes.first(),
        )
        true
    } catch (e: Exception) {
        CrashReporter.log(
            "prayer reschedule failed (markPastAsMissed=$markPastAsMissed)"
        )
        CrashReporter.recordException(e)
        false
    }
}
