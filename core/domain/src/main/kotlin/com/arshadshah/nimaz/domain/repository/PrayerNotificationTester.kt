package com.arshadshah.nimaz.domain.repository

/**
 * Fires a prayer notification **now**, so the user can see what one looks like.
 *
 * Separate from [PrayerAlarmScheduler] on purpose. That port arms tomorrow's alarms and is
 * documented as such; these two post a notification immediately and schedule nothing. Both are
 * implemented by the same `:app` class — a port names a capability, not a class — but folding a
 * diagnostic into an interface called "scheduler" is how an interface stops describing itself.
 *
 * It exists because `PrayerNotificationScheduler` cannot leave `:app`, and for exactly one line:
 * `AppR.drawable.ic_stat_nimaz` in `sendTestNotification()`. Nine hundred lines pinned by a small
 * icon, which `nonTransitiveRClass` keeps off a library's classpath. `SettingsScreen` needs the
 * two calls below; PR 21 of #551 inverted them rather than move a PNG into `:core:ui` and drag the
 * app's whole notification surface after it.
 */
interface PrayerNotificationTester {

    /** Posts one sample notification, on the same channel a real prayer alert would use. */
    fun sendTestNotification()

    /** Posts one per prayer, so the user can check each channel's sound and importance. */
    fun sendAllPrayerTestNotifications()
}
