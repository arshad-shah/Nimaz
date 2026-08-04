package com.arshadshah.nimaz.domain.model

/**
 * How a prayer announces itself when its time arrives.
 *
 * This replaces the old sound on/off binary, which could only say "adhan" or "the default
 * notification tone" — there was no way to keep a prayer visible but quiet. The style is
 * per prayer: Fajr can call the adhan while Dhuhr, at work, stays silent.
 *
 * The choice is honoured at fire time (see `BootReceiver.handlePrayerNotification`), not at
 * schedule time, so changing it takes effect on the next prayer without rescheduling.
 */
enum class PrayerAlertStyle {
    /** The full adhan plays. Still subject to the global adhan switch and to Do Not Disturb. */
    ADHAN,

    /** A notification with the standard tone — no adhan. */
    NOTIFICATION,

    /** A notification with no sound and no vibration; it appears and waits. */
    SILENT;

    /**
     * Whether this style should play the adhan for a given prayer.
     *
     * [globalAdhanEnabled] is the master switch on the Adhan & sound screen: it stays a gate
     * over the per-prayer style, so turning the adhan off silences the call everywhere
     * without rewriting five styles. Sunrise never gets the adhan — it has a beep instead.
     */
    fun playsAdhan(globalAdhanEnabled: Boolean, isSunrise: Boolean = false): Boolean =
        this == ADHAN && globalAdhanEnabled && !isSunrise

    /**
     * Whether the notification should be posted silently. Unlike Do Not Disturb — which
     * gates only the audio — this is the user's own choice, so it applies to the visual
     * notification too. Sunrise is exempt: it has no style of its own.
     */
    fun isMuted(isSunrise: Boolean = false): Boolean = this == SILENT && !isSunrise

    companion object {
        /**
         * The prayers that carry an alert style and a reminder, lowercase, in the order they
         * fall. Sunrise is not one of them: it is a plain alert with a beep and no reminder.
         */
        val PRAYER_KEYS = listOf("fajr", "dhuhr", "asr", "maghrib", "isha")

        /** The lead time a reminder falls back to when the user has never chosen one. */
        const val DEFAULT_REMINDER_MINUTES = 15

        /**
         * Reads a stored style, falling back to [NOTIFICATION] for anything unrecognised —
         * an empty preference, or a value written by a newer build.
         */
        fun fromStorage(raw: String?): PrayerAlertStyle =
            entries.firstOrNull { it.name == raw } ?: NOTIFICATION
    }
}
