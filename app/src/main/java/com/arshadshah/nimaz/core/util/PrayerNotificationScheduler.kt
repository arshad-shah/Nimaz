package com.arshadshah.nimaz.core.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.PerfMonitor
import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.HighLatitudeRule
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.domain.model.WorshipReminderType
import com.arshadshah.nimaz.domain.model.isLocationSet
import com.arshadshah.nimaz.domain.repository.PrayerAlarmScheduler
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Scheduler for prayer time notifications.
 * Handles scheduling, rescheduling, and cancellation of prayer notification alarms.
 */
@Singleton
class PrayerNotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prayerTimeCalculator: PrayerTimeCalculator,
    private val settingsRepository: SettingsRepository
) : PrayerAlarmScheduler {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Pure, stateless — no DI needed. Computes the next occurrence of each worship reminder.
    private val worshipCalculator = WorshipReminderCalculator()

    companion object {
        const val CHANNEL_ID_PRAYER = "prayer_notifications"
        const val CHANNEL_ID_ADHAN = "adhan_notifications"
        const val CHANNEL_ID_DAILY_SUMMARY = "daily_summary_notifications"
        const val CHANNEL_ID_KHATAM = "khatam_notifications"

        // Extended worship reminders (Tahajjud, Suhoor, Iftar, …) — a gentle DEFAULT-importance
        // nudge like the khatam channel, never an alarm. See spec §2 (epic #300).
        const val CHANNEL_ID_WORSHIP = "worship_reminders"

        // Silent (no-vibration) siblings — Android ignores enableVibration() changes
        // after a channel exists, so the vibration preference is honoured by posting
        // on the matching channel instead. See channelForPrayer/channelForAdhan.
        const val CHANNEL_ID_PRAYER_SILENT = "prayer_notifications_silent"
        const val CHANNEL_ID_ADHAN_SILENT = "adhan_notifications_silent"

        // A prayer set to the SILENT alert style posts here: no sound, no vibration, no
        // heads-up. The two channels above are only *no-vibration* siblings — they still
        // carry the channel's sound at IMPORTANCE_HIGH — and Android will not let an
        // existing channel's importance be lowered from code, so silence needs its own.
        const val CHANNEL_ID_PRAYER_MUTED = "prayer_notifications_muted"

        const val ACTION_PRAYER_NOTIFICATION = "com.arshadshah.nimaz.PRAYER_NOTIFICATION"
        const val ACTION_DAILY_SUMMARY = "com.arshadshah.nimaz.DAILY_SUMMARY"
        const val ACTION_FRIDAY_REMINDER = "com.arshadshah.nimaz.FRIDAY_REMINDER"
        const val ACTION_KHATAM_REMINDER = "com.arshadshah.nimaz.KHATAM_REMINDER"
        const val EXTRA_PRAYER_TYPE = "prayer_type"
        const val EXTRA_PRAYER_NAME = "prayer_name"
        const val EXTRA_PRAYER_TIME = "prayer_time"
        const val EXTRA_IS_PRE_REMINDER = "is_pre_reminder"

        /** Lead time carried by a pre-reminder alarm, now that it differs per prayer. */
        const val EXTRA_REMINDER_MINUTES = "reminder_minutes"

        // Request codes for different prayers (use prayer ordinal * 10 for different notification types)
        private const val REQUEST_CODE_BASE = 1000
        private const val PRE_REMINDER_REQUEST_CODE_BASE = 2000
        private const val TEST_NOTIFICATION_ID = 8888
        private const val DAILY_SUMMARY_REQUEST_CODE = 8889
        private const val FRIDAY_REMINDER_REQUEST_CODE = 8890
        private const val KHATAM_REMINDER_REQUEST_CODE = 8891

        const val ACTION_WORSHIP_REMINDER = "com.arshadshah.nimaz.WORSHIP_REMINDER"
        const val EXTRA_WORSHIP_TYPE = "worship_type"
        const val EXTRA_WORSHIP_SUBKEY = "worship_subkey"
        const val EXTRA_WORSHIP_EVENT_TIME = "worship_event_time"

        // One request code per worship reminder type, well clear of the prayer (1000+),
        // pre-reminder (2000+) and single-purpose codes (8889-8891, 9999) above.
        private const val WORSHIP_REQUEST_CODE_BASE = 9000

        const val ACTION_MIDNIGHT_RESCHEDULE = "com.arshadshah.nimaz.MIDNIGHT_RESCHEDULE"
        private const val MIDNIGHT_REQUEST_CODE = 9999

        /**
         * Channel id for a standalone prayer notification honouring the vibration pref.
         *
         * [muted] wins over [vibrate]: a prayer the user has silenced posts on the muted
         * channel whatever the vibration preference says.
         */
        fun channelForPrayer(vibrate: Boolean, muted: Boolean = false): String = when {
            muted -> CHANNEL_ID_PRAYER_MUTED
            vibrate -> CHANNEL_ID_PRAYER
            else -> CHANNEL_ID_PRAYER_SILENT
        }

        /** Channel id for an adhan notification honouring the vibration pref. */
        fun channelForAdhan(vibrate: Boolean): String =
            if (vibrate) CHANNEL_ID_ADHAN else CHANNEL_ID_ADHAN_SILENT
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        // Prayer time notification channel
        val prayerChannel = NotificationChannel(
            CHANNEL_ID_PRAYER,
            "Prayer Time Notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for prayer times with Islamic reminders"
            enableVibration(true)
            enableLights(true)
        }

        // Adhan notification channel (higher priority)
        val adhanChannel = NotificationChannel(
            CHANNEL_ID_ADHAN,
            "Adhan Notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Adhan sound notifications"
            enableVibration(true)
            enableLights(true)
        }

        // Daily summary notification channel
        val dailySummaryChannel = NotificationChannel(
            CHANNEL_ID_DAILY_SUMMARY,
            "Daily Prayer Summary",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Nightly summary of your daily prayer completion"
            enableVibration(true)
            enableLights(true)
        }

        // Silent (no-vibration) siblings for when the vibration preference is off.
        val prayerChannelSilent = NotificationChannel(
            CHANNEL_ID_PRAYER_SILENT,
            "Prayer Time Notifications (No Vibration)",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Prayer time notifications without vibration"
            enableVibration(false)
            enableLights(true)
        }

        val adhanChannelSilent = NotificationChannel(
            CHANNEL_ID_ADHAN_SILENT,
            "Adhan Notifications (No Vibration)",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Adhan notifications without vibration"
            enableVibration(false)
            enableLights(true)
        }

        // Prayers set to the silent alert style: it appears and waits. LOW importance keeps
        // it out of the heads-up path; the null sound keeps it quiet even so.
        val prayerChannelMuted = NotificationChannel(
            CHANNEL_ID_PRAYER_MUTED,
            "Prayer Time Notifications (Silent)",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Prayer time notifications with no sound and no vibration"
            setSound(null, null)
            enableVibration(false)
            enableLights(true)
        }

        // Khatam reminder channel — a gentle nudge to read, not an alarm, so it
        // stays at DEFAULT importance and never interrupts.
        val khatamChannel = NotificationChannel(
            CHANNEL_ID_KHATAM,
            context.getString(R.string.notif_khatam_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notif_khatam_channel_description)
            enableVibration(true)
            enableLights(true)
        }

        // Extended worship reminders — a gentle nudge, DEFAULT importance like khatam.
        val worshipChannel = NotificationChannel(
            CHANNEL_ID_WORSHIP,
            context.getString(R.string.worship_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.worship_channel_description)
            enableVibration(true)
            enableLights(true)
        }

        notificationManager.createNotificationChannels(
            listOf(
                prayerChannel,
                adhanChannel,
                dailySummaryChannel,
                khatamChannel,
                worshipChannel,
                prayerChannelSilent,
                adhanChannelSilent,
                prayerChannelMuted
            )
        )
    }

    /**
     * Schedule notifications for today's prayers.
     * This should be called after boot, when settings change, or at midnight.
     *
     * @param enabledPrayers If provided, only schedule for these prayer types. If null, schedule all non-sunrise prayers.
     * @param preReminders Lead time in minutes per prayer. A prayer absent from the map gets
     *   no pre-reminder — that is how "off" is expressed, rather than a zero lead time.
     */
    override fun scheduleTodaysPrayerNotifications(
        latitude: Double,
        longitude: Double,
        notificationsEnabled: Boolean,
        enabledPrayers: Set<PrayerType>?,
        preReminders: Map<PrayerType, Int>,
        calculationMethod: CalculationMethod,
        asrCalculation: AsrCalculation,
        highLatitudeRule: HighLatitudeRule?,
        adjustments: Map<PrayerType, Int>,
        fridayReminderEnabled: Boolean,
        fridayReminderMinutes: Int
    ) {
        if (!notificationsEnabled) {
            cancelAllPrayerNotifications()
            AppAnalytics.logNotificationsCancelled(reason = "notifications_disabled")
            return
        }

        if (!isLocationSet(latitude, longitude)) {
            AppAnalytics.logNotificationsCancelled(reason = "no_location")
            return // No location set
        }

        // Cancel all first, then reschedule only enabled ones
        PrayerType.entries.forEach {
            cancelPrayerNotification(it)
            cancelPreReminderNotification(it)
        }

        val perfTrace = PerfMonitor.newTrace(PerfMonitor.Traces.NOTIFICATION_SCHEDULE)

        val prayerTimes = prayerTimeCalculator.getPrayerTimes(
            latitude = latitude,
            longitude = longitude,
            date = LocalDate.now(),
            calculationMethod = calculationMethod,
            asrCalculation = asrCalculation,
            highLatitudeRule = highLatitudeRule,
            adjustments = adjustments
        )
        val now = LocalDateTime.now()
        var scheduledCount = 0

        prayerTimes.forEach { prayerTime ->
            // Skip Sunrise by default, or skip if not in enabledPrayers set
            if (enabledPrayers != null) {
                if (prayerTime.type !in enabledPrayers) return@forEach
            } else {
                if (prayerTime.type == PrayerType.SUNRISE) return@forEach
            }

            val prayerLocalDateTime = prayerTime.time.toLocalDateTime()

            // Only schedule if prayer time is in the future
            if (prayerLocalDateTime.isAfter(now)) {
                schedulePrayerNotification(prayerTime.type, prayerLocalDateTime)
                scheduledCount++

                // Schedule this prayer's own pre-reminder, if it has one (never for sunrise).
                val leadMinutes = preReminders[prayerTime.type]
                if (leadMinutes != null && prayerTime.type != PrayerType.SUNRISE) {
                    val preReminderTime =
                        PrayerAlarmTimes.preReminderAt(prayerLocalDateTime, leadMinutes)
                    if (preReminderTime.isAfter(now)) {
                        schedulePreReminderNotification(
                            prayerTime.type,
                            preReminderTime,
                            leadMinutes
                        )
                    }
                }
            }
        }

        // Schedule midnight reschedule for tomorrow
        scheduleMidnightReschedule()

        // Schedule daily summary notification at 11 PM
        scheduleDailySummary()

        // Schedule (or cancel) the weekly Friday (Jummah) reminder
        scheduleFridayReminder(
            latitude = latitude,
            longitude = longitude,
            enabled = fridayReminderEnabled,
            minutesBefore = fridayReminderMinutes,
            calculationMethod = calculationMethod,
            asrCalculation = asrCalculation,
            highLatitudeRule = highLatitudeRule,
            adjustments = adjustments
        )

        // Schedule (or cancel) the daily khatam reading reminder. Must live here so the
        // midnight reschedule chain and the boot path re-arm this one-shot every day.
        scheduleKhatamReminder()

        // Schedule (or cancel) the extended worship reminders. Same rationale — armed here so
        // the midnight chain + boot path re-arm each one-shot daily. See spec §2 (epic #300).
        scheduleWorshipReminders(
            latitude = latitude,
            longitude = longitude,
            calculationMethod = calculationMethod,
            asrCalculation = asrCalculation,
            highLatitudeRule = highLatitudeRule,
            adjustments = adjustments
        )

        PerfMonitor.stop(
            perfTrace,
            metrics = mapOf("scheduled_count" to scheduledCount.toLong()),
        )

        // Record the scheduling outcome along with the OS-level prerequisites that
        // determine whether these alarms will actually fire on time.
        AppAnalytics.logNotificationsScheduled(
            scheduledCount = scheduledCount,
            preRemindersEnabled = preReminders.isNotEmpty(),
            exactAlarmAllowed = AppAnalytics.exactAlarmAllowed(context),
            postNotificationsGranted = AppAnalytics.postNotificationsGranted(context),
        )
    }

    /**
     * Schedule daily summary notification at 11 PM.
     * This shows a summary of prayers completed/missed for the day.
     */
    private fun scheduleDailySummary() {
        val intent = Intent(context, BootReceiver::class.java).apply {
            action = ACTION_DAILY_SUMMARY
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DAILY_SUMMARY_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule for 11:00 PM today (or tomorrow if already past 11 PM)
        val now = LocalDateTime.now()
        var summaryTime = LocalDate.now().atTime(23, 0) // 11:00 PM

        if (now.isAfter(summaryTime)) {
            // If it's already past 11 PM, schedule for tomorrow
            summaryTime = LocalDate.now().plusDays(1).atTime(23, 0)
        }

        val triggerTimeMillis = summaryTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTimeMillis,
            pendingIntent
        )
    }

    /**
     * Schedule the weekly Friday (Jummah) reminder for the upcoming Friday, at that
     * Friday's Dhuhr time minus [minutesBefore]. Re-armed on every reschedule (daily
     * midnight chain / settings change / boot), so the one-shot always targets the
     * next Friday. Cancels when [enabled] is false.
     */
    private fun scheduleFridayReminder(
        latitude: Double,
        longitude: Double,
        enabled: Boolean,
        minutesBefore: Int,
        calculationMethod: CalculationMethod,
        asrCalculation: AsrCalculation,
        highLatitudeRule: HighLatitudeRule?,
        adjustments: Map<PrayerType, Int>
    ) {
        cancelFridayReminder()
        if (!enabled) return
        if (!isLocationSet(latitude, longitude)) return

        val now = LocalDateTime.now()
        val today = LocalDate.now()

        // Find the reminder time for the upcoming Friday; if this week's has already
        // passed, roll to next Friday.
        fun reminderFor(friday: LocalDate): LocalDateTime? {
            val times = prayerTimeCalculator.getPrayerTimes(
                latitude = latitude,
                longitude = longitude,
                date = friday,
                calculationMethod = calculationMethod,
                asrCalculation = asrCalculation,
                highLatitudeRule = highLatitudeRule,
                adjustments = adjustments
            )
            val dhuhr = times.firstOrNull { it.type == PrayerType.DHUHR } ?: return null
            return dhuhr.time.toLocalDateTime().minusMinutes(minutesBefore.toLong())
        }

        val daysUntilFriday = ((DayOfWeek.FRIDAY.value - today.dayOfWeek.value) + 7) % 7
        var reminderTime = reminderFor(today.plusDays(daysUntilFriday.toLong()))
        if (reminderTime == null || reminderTime.isBefore(now)) {
            reminderTime = reminderFor(today.plusDays((daysUntilFriday + 7).toLong()))
        }
        val trigger = reminderTime ?: return

        val intent = Intent(context, BootReceiver::class.java).apply {
            action = ACTION_FRIDAY_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            FRIDAY_REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerMillis = trigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerMillis,
            pendingIntent
        )
    }

    /**
     * Schedule the daily khatam reading reminder at the user's chosen time, or cancel it
     * when the preference is off. Re-armed on every reschedule (midnight chain / settings
     * change / boot), which is what keeps this one-shot alarm recurring.
     *
     * The preference is read here rather than passed in so callers of
     * [scheduleTodaysPrayerNotifications] don't all have to learn about khatam.
     */
    private fun scheduleKhatamReminder() {
        cancelKhatamReminder()

        // Blocking read: scheduling is already an off-main-thread, fire-and-forget step,
        // and the alarm must be armed before this method returns.
        val (enabled, timeString) = runBlocking {
            settingsRepository.khatamReminderEnabled.first() to
                    settingsRepository.khatamReminderTime.first()
        }
        if (!enabled) return

        // Fall back to the default rather than dropping the reminder if the stored
        // value is ever malformed.
        val reminderAt = runCatching { LocalTime.parse(timeString) }
            .getOrDefault(LocalTime.of(6, 0))

        val now = LocalDateTime.now()
        var trigger = LocalDate.now().atTime(reminderAt)
        if (now.isAfter(trigger)) {
            trigger = LocalDate.now().plusDays(1).atTime(reminderAt)
        }

        val intent = Intent(context, BootReceiver::class.java).apply {
            action = ACTION_KHATAM_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            KHATAM_REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerMillis = trigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerMillis,
            pendingIntent
        )
    }

    private fun cancelKhatamReminder() {
        val intent = Intent(context, BootReceiver::class.java).apply {
            action = ACTION_KHATAM_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            KHATAM_REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    /**
     * Schedule (or cancel) every enabled extended worship reminder. Cancels all first, then re-arms
     * the next upcoming occurrence of each enabled type via [WorshipReminderCalculator]. Preferences
     * are read here (blocking, off-main-thread) so callers of [scheduleTodaysPrayerNotifications]
     * don't need to know about worship reminders — mirrors [scheduleKhatamReminder].
     */
    private fun scheduleWorshipReminders(
        latitude: Double,
        longitude: Double,
        calculationMethod: CalculationMethod,
        asrCalculation: AsrCalculation,
        highLatitudeRule: HighLatitudeRule?,
        adjustments: Map<PrayerType, Int>
    ) {
        WorshipReminderType.entries.forEach { cancelWorshipReminder(it) }
        if (!isLocationSet(latitude, longitude)) return

        // One blocking read of all worship prefs + the Hijri offset.
        val hijriOffset: Int
        val enabledMap: Map<WorshipReminderType, Boolean>
        val offsetMap: Map<WorshipReminderType, Int>
        val witrBeforeFajr: Boolean
        runBlocking {
            hijriOffset = settingsRepository.hijriDayOffset.first()
            enabledMap = WorshipReminderType.entries.associateWith {
                settingsRepository.worshipReminderEnabled(it.key).first()
            }
            offsetMap = WorshipReminderType.entries.associateWith {
                settingsRepository.worshipReminderOffset(it.key, it.defaultOffsetMinutes).first()
            }
            witrBeforeFajr = settingsRepository.worshipReminderMode(
                WorshipReminderType.WITR.key, WorshipReminderCalculator.WITR_MODE_AFTER_ISHA
            ).first() == WorshipReminderCalculator.WITR_MODE_BEFORE_FAJR
        }
        if (enabledMap.values.none { it }) return

        val zone = ZoneId.systemDefault()
        fun instantToLocal(instant: kotlin.time.Instant): LocalDateTime =
            java.time.Instant.ofEpochMilli(instant.toEpochMilliseconds()).atZone(zone).toLocalDateTime()

        val timesFor: (LocalDate) -> DayWorshipTimes? = { date ->
            val byType = prayerTimeCalculator.getPrayerTimes(
                latitude, longitude, date, calculationMethod, asrCalculation, highLatitudeRule, adjustments
            ).associate { it.type to instantToLocal(it.time) }
            val sunnah = prayerTimeCalculator.getSunnahTimes(
                latitude, longitude, date, calculationMethod, asrCalculation, highLatitudeRule
            )
            val fajr = byType[PrayerType.FAJR]
            val sunrise = byType[PrayerType.SUNRISE]
            val dhuhr = byType[PrayerType.DHUHR]
            val asr = byType[PrayerType.ASR]
            val maghrib = byType[PrayerType.MAGHRIB]
            val isha = byType[PrayerType.ISHA]
            if (fajr != null && sunrise != null && dhuhr != null && asr != null && maghrib != null && isha != null) {
                DayWorshipTimes(
                    fajr = fajr, sunrise = sunrise, dhuhr = dhuhr, asr = asr, maghrib = maghrib,
                    isha = isha, lastThirdOfNight = instantToLocal(sunnah.lastThirdOfTheNight)
                )
            } else null
        }

        val hijriFor: (LocalDate) -> HijriDayInfo = { date ->
            val h = HijriDateCalculator.toHijri(date.plusDays(hijriOffset.toLong()))
            HijriDayInfo(month = h.month, day = h.day)
        }

        val now = LocalDateTime.now()
        WorshipReminderType.entries.forEach { type ->
            if (enabledMap[type] != true) return@forEach
            val occ = worshipCalculator.nextOccurrence(
                type = type,
                now = now,
                offsetMinutes = offsetMap[type] ?: type.defaultOffsetMinutes,
                timesFor = timesFor,
                hijriFor = hijriFor,
                witrBeforeFajr = witrBeforeFajr,
                // Only arm a strictly-future trigger. An already-active occurrence has
                // already fired; arming it again at a past instant would make Android
                // deliver it immediately, re-posting the notification on every app-open
                // (or reschedule) for the whole window. Roll forward to the next one.
                requireFutureTrigger = true
            ) ?: return@forEach
            armWorshipReminder(type, occ.triggerAt, occ.eventAt, occ.subKey)
        }
    }

    private fun armWorshipReminder(
        type: WorshipReminderType,
        triggerAt: LocalDateTime,
        eventAt: LocalDateTime,
        subKey: String?
    ) {
        val intent = Intent(context, BootReceiver::class.java).apply {
            action = ACTION_WORSHIP_REMINDER
            putExtra(EXTRA_WORSHIP_TYPE, type.name)
            putExtra(EXTRA_WORSHIP_SUBKEY, subKey)
            putExtra(EXTRA_WORSHIP_EVENT_TIME, eventAt.toString())
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            WORSHIP_REQUEST_CODE_BASE + type.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerMillis = triggerAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
    }

    private fun cancelWorshipReminder(type: WorshipReminderType) {
        val intent = Intent(context, BootReceiver::class.java).apply { action = ACTION_WORSHIP_REMINDER }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            WORSHIP_REQUEST_CODE_BASE + type.ordinal,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    private fun cancelFridayReminder() {
        val intent = Intent(context, BootReceiver::class.java).apply {
            action = ACTION_FRIDAY_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            FRIDAY_REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    /**
     * Cancel the daily summary notification alarm.
     */
    fun cancelDailySummary() {
        val intent = Intent(context, BootReceiver::class.java).apply {
            action = ACTION_DAILY_SUMMARY
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DAILY_SUMMARY_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    /**
     * Schedule a pre-reminder notification for a prayer.
     */
    private fun schedulePreReminderNotification(
        prayerType: PrayerType,
        reminderTime: LocalDateTime,
        leadMinutes: Int
    ) {
        // Use explicit intent for BootReceiver (required for Android 8.0+).
        // The lead time travels with the alarm because it is now per prayer: reading a
        // global value back at fire time would put the wrong number in the text.
        val intent = Intent(context, BootReceiver::class.java).apply {
            action = ACTION_PRAYER_NOTIFICATION
            putExtra(EXTRA_PRAYER_TYPE, prayerType.name)
            putExtra(EXTRA_PRAYER_NAME, prayerType.displayName)
            putExtra(EXTRA_PRAYER_TIME, reminderTime.toString())
            putExtra(EXTRA_IS_PRE_REMINDER, true)
            putExtra(EXTRA_REMINDER_MINUTES, leadMinutes)
        }

        val requestCode = PRE_REMINDER_REQUEST_CODE_BASE + prayerType.ordinal
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTimeMillis = PrayerAlarmTimes.triggerMillis(reminderTime)

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTimeMillis,
            pendingIntent
        )
    }

    /**
     * Cancel pre-reminder notification for a specific prayer.
     */
    private fun cancelPreReminderNotification(prayerType: PrayerType) {
        val intent = Intent(context, BootReceiver::class.java).apply {
            action = ACTION_PRAYER_NOTIFICATION
        }

        val requestCode = PRE_REMINDER_REQUEST_CODE_BASE + prayerType.ordinal
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    /**
     * Schedule a single prayer notification.
     */
    private fun schedulePrayerNotification(
        prayerType: PrayerType,
        prayerTime: LocalDateTime
    ) {
        // Use explicit intent for BootReceiver (required for Android 8.0+)
        val intent = Intent(context, BootReceiver::class.java).apply {
            action = ACTION_PRAYER_NOTIFICATION
            putExtra(EXTRA_PRAYER_TYPE, prayerType.name)
            putExtra(EXTRA_PRAYER_NAME, prayerType.displayName)
            putExtra(EXTRA_PRAYER_TIME, prayerTime.toString())
        }

        val requestCode = REQUEST_CODE_BASE + prayerType.ordinal
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTimeMillis = PrayerAlarmTimes.triggerMillis(prayerTime)

        // Use setExactAndAllowWhileIdle for precise timing
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTimeMillis,
            pendingIntent
        )
    }

    /**
     * Cancel notification for a specific prayer.
     */
    fun cancelPrayerNotification(prayerType: PrayerType) {
        val intent = Intent(context, BootReceiver::class.java).apply {
            action = ACTION_PRAYER_NOTIFICATION
        }

        val requestCode = REQUEST_CODE_BASE + prayerType.ordinal
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    /**
     * Cancel all scheduled prayer notifications.
     */
    fun cancelAllPrayerNotifications() {
        PrayerType.entries.forEach { prayerType ->
            cancelPrayerNotification(prayerType)
            cancelPreReminderNotification(prayerType)
        }
        cancelMidnightReschedule()
        cancelFridayReminder()
        cancelKhatamReminder()
        WorshipReminderType.entries.forEach { cancelWorshipReminder(it) }
    }

    /**
     * Send an immediate test notification to verify notifications are working.
     */
    fun sendTestNotification() {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_PRAYER)
            .setSmallIcon(R.drawable.ic_stat_nimaz)
            .setContentTitle(context.getString(R.string.test_notification_title))
            .setContentText(context.getString(R.string.test_notification_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(TEST_NOTIFICATION_ID, notification)
        AppAnalytics.logTestNotification(allPrayers = false)
    }

    /**
     * Send test notifications for all prayers to validate the notification system.
     * Uses explicit broadcasts to ensure BootReceiver receives them on Android 8.0+.
     */
    fun sendAllPrayerTestNotifications() {
        val prayers = listOf(
            PrayerType.FAJR to "05:30 AM",
            PrayerType.SUNRISE to "06:45 AM",
            PrayerType.DHUHR to "12:30 PM",
            PrayerType.ASR to "03:45 PM",
            PrayerType.MAGHRIB to "06:15 PM",
            PrayerType.ISHA to "07:45 PM"
        )

        prayers.forEach { (prayerType, time) ->
            // Create explicit intent for BootReceiver (required for Android 8.0+)
            val intent = Intent(context, BootReceiver::class.java).apply {
                action = ACTION_PRAYER_NOTIFICATION
                putExtra(EXTRA_PRAYER_TYPE, prayerType.name)
                putExtra(EXTRA_PRAYER_NAME, prayerType.displayName)
                putExtra(EXTRA_PRAYER_TIME, time)
            }

            // Send explicit broadcast to trigger the full notification flow
            context.sendBroadcast(intent)
        }
        AppAnalytics.logTestNotification(allPrayers = true)
    }

    /**
     * Schedule a midnight alarm to reschedule tomorrow's prayers.
     */
    private fun scheduleMidnightReschedule() {
        // Use explicit intent for BootReceiver (required for Android 8.0+)
        val intent = Intent(context, BootReceiver::class.java).apply {
            action = ACTION_MIDNIGHT_RESCHEDULE
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MIDNIGHT_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule for 12:01 AM tomorrow
        val tomorrow = LocalDate.now().plusDays(1)
        val midnight = tomorrow.atTime(0, 1)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            midnight,
            pendingIntent
        )
    }

    private fun cancelMidnightReschedule() {
        val intent = Intent(context, BootReceiver::class.java).apply {
            action = ACTION_MIDNIGHT_RESCHEDULE
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MIDNIGHT_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    // Extension to convert an Instant to LocalDateTime
    private fun kotlin.time.Instant.toLocalDateTime(): LocalDateTime {
        return java.time.Instant.ofEpochMilli(this.toEpochMilliseconds())
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
    }
}
