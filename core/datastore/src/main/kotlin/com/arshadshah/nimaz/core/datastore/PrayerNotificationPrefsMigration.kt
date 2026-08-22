package com.arshadshah.nimaz.core.datastore

import com.arshadshah.nimaz.domain.model.PrayerAlertStyle

/** The five prayers that carry an alert style and a reminder. Sunrise is not one of them. */
internal val ALERT_STYLE_PRAYERS = PrayerAlertStyle.PRAYER_KEYS

/** The lead time a reminder falls back to when the user has never chosen one. */
internal const val DEFAULT_REMINDER_MINUTES = PrayerAlertStyle.DEFAULT_REMINDER_MINUTES

/** What the old model stored, read once so the split can be planned from it. */
data class LegacyPrayerNotificationPrefs(
    val adhanEnabled: Boolean,
    val perPrayerAdhanEnabled: Map<String, Boolean>,
    val showReminderBefore: Boolean,
    val reminderMinutes: Int,
)

/** What the new model should hold, keyed by prayer. */
data class MigratedPrayerNotificationPrefs(
    val alertStyle: Map<String, PrayerAlertStyle>,
    val reminderEnabled: Map<String, Boolean>,
    val reminderMinutes: Map<String, Int>,
)

/**
 * Carries an existing install across the notifications rework's two preference splits:
 * the global adhan switch plus its per-prayer companions become a per-prayer
 * [PrayerAlertStyle], and the one global pre-adhan reminder becomes a reminder per prayer.
 *
 * The rule is that nobody loses a setting. A 30-minute pre-adhan reminder becomes a
 * 30-minute reminder on all five prayers; a prayer that was calling the adhan keeps calling
 * it. Nothing migrates to [PrayerAlertStyle.SILENT], because the old model had no way to
 * ask for silence — a user who has never asked for it must not find a prayer gone quiet.
 *
 * The plan is pure so it can be tested without a DataStore; [PreferencesDataStore] runs it
 * once, guarded by [VERSION].
 */
object PrayerNotificationPrefsMigration {

    /**
     * Bumped when this migration's output changes. Stored alongside the preferences so the
     * migration runs exactly once per install, and so a later split can be added without
     * re-deriving this one from keys that have since moved on.
     */
    const val VERSION = 1

    /** The range the reminder stepper offers. Anything outside it is unreachable in the UI. */
    private val LEAD_TIME_RANGE = 5..60

    fun plan(legacy: LegacyPrayerNotificationPrefs): MigratedPrayerNotificationPrefs {
        val minutes = legacy.reminderMinutes.coerceIn(LEAD_TIME_RANGE)

        return MigratedPrayerNotificationPrefs(
            alertStyle = ALERT_STYLE_PRAYERS.associateWith { prayer ->
                // The old per-prayer adhan flags defaulted to true, so a missing entry means
                // "on" — only the global switch being off, or an explicit per-prayer off,
                // demotes a prayer to a plain notification.
                val adhanForPrayer = legacy.perPrayerAdhanEnabled[prayer] ?: true
                if (legacy.adhanEnabled && adhanForPrayer) {
                    PrayerAlertStyle.ADHAN
                } else {
                    PrayerAlertStyle.NOTIFICATION
                }
            },
            reminderEnabled = ALERT_STYLE_PRAYERS.associateWith { legacy.showReminderBefore },
            // The lead time is carried even when the reminder is off, so switching a prayer's
            // reminder back on restores what the user last chose rather than the default.
            reminderMinutes = ALERT_STYLE_PRAYERS.associateWith { minutes },
        )
    }
}
