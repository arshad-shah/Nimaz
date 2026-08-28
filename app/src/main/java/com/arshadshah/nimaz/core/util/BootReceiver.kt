package com.arshadshah.nimaz.core.util

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.common.LocaleHelper
import com.arshadshah.nimaz.core.common.NimazChannels
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.data.audio.AdhanAudioManager
import com.arshadshah.nimaz.data.audio.AdhanDownloadService
import com.arshadshah.nimaz.data.audio.AdhanPlaybackService
import com.arshadshah.nimaz.data.audio.AdhanSound
import com.arshadshah.nimaz.core.datastore.PreferencesDataStore
import com.arshadshah.nimaz.domain.model.KhatamProgressCalculator
import com.arshadshah.nimaz.domain.model.PrayerAlertStyle
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.repository.KhatamRepository
import com.arshadshah.nimaz.domain.repository.PrayerRepository
import com.arshadshah.nimaz.widget.WidgetUpdateScheduler
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that handles prayer notifications, boot events, and daily summaries.
 * Features enhanced notification layouts with Islamic greetings and motivational messages.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var preferencesDataStore: PreferencesDataStore

    @Inject
    lateinit var prayerNotificationScheduler: PrayerNotificationScheduler

    @Inject
    lateinit var prayerRepository: PrayerRepository

    @Inject
    lateinit var prayerRescheduler: PrayerRescheduler

    @Inject
    lateinit var khatamRepository: KhatamRepository

    @Inject
    lateinit var adhanAudioManager: AdhanAudioManager

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON" -> {
                AppAnalytics.logNotificationReschedule(trigger = "boot")
                reschedulePrayerNotifications()
                // Widget periodic work is WorkManager's and survives the reboot on its own; the
                // per-minute countdown alarm is ours and does not. Nothing re-armed it, so the
                // countdown widgets stopped ticking at the first restart after they were placed
                // and never started again.
                WidgetUpdateScheduler.ensureScheduled(context)
            }

            PrayerNotificationScheduler.ACTION_MIDNIGHT_RESCHEDULE -> {
                AppAnalytics.logNotificationReschedule(trigger = "midnight")
                reschedulePrayerNotifications()
            }

            PrayerNotificationScheduler.ACTION_PRAYER_NOTIFICATION -> {
                handlePrayerNotification(context, intent)
            }

            PrayerNotificationScheduler.ACTION_DAILY_SUMMARY -> {
                handleDailySummary(context)
            }

            PrayerNotificationScheduler.ACTION_FRIDAY_REMINDER -> {
                handleFridayReminder(context)
            }

            PrayerNotificationScheduler.ACTION_KHATAM_REMINDER -> {
                handleKhatamReminder(context)
            }

            PrayerNotificationScheduler.ACTION_WORSHIP_REMINDER -> {
                handleWorshipReminder(context, intent)
            }
        }
    }

    /**
     * Re-arm today's notifications, on both a reboot and the midnight chain.
     *
     * These used to be two separate methods — twenty-odd lines written out twice, differing only
     * in whether past prayers were marked missed first. Marking is no longer something the
     * midnight chain does: a prayer nobody logged is not a prayer the user missed, and confirming
     * one missed is now an explicit action in the prayer tracker. With that difference gone, both
     * call sites are this one method. See [PrayerRescheduler].
     */
    private fun reschedulePrayerNotifications() {
        scope.launch { prayerRescheduler.rescheduleToday() }
    }

    private fun handlePrayerNotification(context: Context, intent: Intent) {
        val prayerName =
            intent.getStringExtra(PrayerNotificationScheduler.EXTRA_PRAYER_NAME) ?: "Prayer"
        val prayerTime = intent.getStringExtra(PrayerNotificationScheduler.EXTRA_PRAYER_TIME) ?: ""
        val prayerType = intent.getStringExtra(PrayerNotificationScheduler.EXTRA_PRAYER_TYPE) ?: ""
        val isPreReminder =
            intent.getBooleanExtra(PrayerNotificationScheduler.EXTRA_IS_PRE_REMINDER, false)

        val isFajr = prayerType.equals("FAJR", ignoreCase = true)
        val isSunrise = prayerType.equals("SUNRISE", ignoreCase = true)

        // Gap between when this notification was scheduled to fire and now. A large
        // value points to Doze / battery-optimization delaying delivery — the main
        // "notifications don't fire on time" failure mode.
        val deliveryLatencySeconds = deliveryLatencySeconds(prayerTime)

        scope.launch {
            try {
                val prayerNotificationEnabled = isPrayerNotificationEnabled(prayerType)
                if (!prayerNotificationEnabled) {
                    AppAnalytics.logNotificationSuppressed(prayerType, reason = "prayer_disabled")
                    return@launch
                }

                // Honor Do Not Disturb: when the pref is on and the system is in a DND
                // mode, silence the adhan audio — the (silent) visual notification is
                // still posted; the OS suppresses its channel sound under DND.
                val respectDnd = preferencesDataStore.adhanRespectDnd.first()
                val isDndActive =
                    (context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager)
                        .currentInterruptionFilter != android.app.NotificationManager.INTERRUPTION_FILTER_ALL
                val dndBlocksAdhan = respectDnd && isDndActive

                val vibrationEnabled = preferencesDataStore.notificationVibration.first()

                if (isPreReminder) {
                    // The lead time rides on the alarm, because it is now per prayer — a
                    // global read here would put the wrong number in the text.
                    val reminderMinutes = intent.getIntExtra(
                        PrayerNotificationScheduler.EXTRA_REMINDER_MINUTES,
                        preferencesDataStore.notificationReminderMinutes.first()
                    )
                    showEnhancedPreReminderNotification(
                        context,
                        prayerName,
                        prayerType,
                        reminderMinutes,
                        vibrationEnabled
                    )
                    AppAnalytics.logNotificationDisplayed(
                        prayerType = prayerType,
                        isPreReminder = true,
                        adhanPlayed = false,
                        dndBlocked = false,
                        deliveryLatencySeconds = deliveryLatencySeconds,
                    )
                    return@launch
                }

                val globalAdhanEnabled = preferencesDataStore.adhanEnabled.first()
                val alertStyle = preferencesDataStore.prayerAlertStyle(prayerType).first()
                val selectedAdhan = preferencesDataStore.selectedAdhanSound.first()

                // The per-prayer alert style decides what this prayer does. Both rules live
                // on PrayerAlertStyle so the scheduler, this receiver and the tests read the
                // same one rather than three copies that can drift apart.
                val wantsAdhan = alertStyle.playsAdhan(globalAdhanEnabled, isSunrise)
                val muted = alertStyle.isMuted(isSunrise)

                // DND gates only the audio — the visual notification still shows.
                val shouldPlayAdhan = wantsAdhan && !dndBlocksAdhan
                val shouldPlayBeep = globalAdhanEnabled && isSunrise && !dndBlocksAdhan

                // Get notification content for merging into adhan service notification
                val notifTitle = NotificationContentHelper.getPrayerTitle(prayerType, prayerTime)
                val notifMessage = NotificationContentHelper.getShortMessage(context, prayerType)
                val notifColor = getPrayerColor(prayerType)

                var adhanPlayed = false

                if (shouldPlayAdhan) {
                    val adhanSound = AdhanSound.fromName(selectedAdhan)
                    // Check for the specific variant needed, or accept beep as fallback
                    val hasCorrectVariant = adhanAudioManager.isDownloaded(adhanSound, isFajr)
                    val hasBeepFallback =
                        adhanAudioManager.isDownloaded(AdhanSound.SIMPLE_BEEP, false)
                    val hasAdhan = hasCorrectVariant || hasBeepFallback

                    // If the correct variant is missing, schedule a re-download for next time
                    if (!hasCorrectVariant) {
                        android.util.Log.w(
                            "BootReceiver",
                            "Missing ${adhanSound.name} variant (isFajr=$isFajr), triggering re-download"
                        )
                        AdhanDownloadService.downloadSelected(context, adhanSound)
                        AppAnalytics.logAdhanFileMissing(
                            adhanSound = adhanSound.name,
                            isFajr = isFajr
                        )
                    }

                    if (hasAdhan) {
                        // Adhan service notification serves as both prayer + adhan notification
                        AdhanPlaybackService.playAdhan(
                            context = context,
                            adhanSound = adhanSound,
                            isFajr = isFajr,
                            prayerName = prayerName,
                            prayerType = prayerType,
                            prayerTime = prayerTime,
                            notificationTitle = notifTitle,
                            notificationMessage = notifMessage,
                            notificationColor = notifColor
                        )
                        adhanPlayed = true
                    } else {
                        // Adhan file not available, show standalone notification
                        showEnhancedPrayerNotification(
                            context = context,
                            prayerName = prayerName,
                            prayerType = prayerType,
                            prayerTime = prayerTime,
                            adhanEnabled = false,
                            vibrationEnabled = vibrationEnabled,
                            muted = muted
                        )
                    }
                } else if (shouldPlayBeep) {
                    val beepSound = AdhanSound.SIMPLE_BEEP
                    if (adhanAudioManager.isDownloaded(beepSound, false)) {
                        AdhanPlaybackService.playAdhan(
                            context = context,
                            adhanSound = beepSound,
                            isFajr = false,
                            prayerName = prayerName,
                            prayerType = prayerType,
                            prayerTime = prayerTime,
                            notificationTitle = notifTitle,
                            notificationMessage = notifMessage,
                            notificationColor = notifColor
                        )
                        adhanPlayed = true
                    } else {
                        showEnhancedPrayerNotification(
                            context = context,
                            prayerName = prayerName,
                            prayerType = prayerType,
                            prayerTime = prayerTime,
                            adhanEnabled = false,
                            vibrationEnabled = vibrationEnabled
                        )
                    }
                } else {
                    // No adhan — a plain prayer notification, silent if the user asked for it.
                    showEnhancedPrayerNotification(
                        context = context,
                        prayerName = prayerName,
                        prayerType = prayerType,
                        prayerTime = prayerTime,
                        adhanEnabled = false,
                        vibrationEnabled = vibrationEnabled,
                        muted = muted
                    )
                }

                AppAnalytics.logNotificationDisplayed(
                    prayerType = prayerType,
                    isPreReminder = false,
                    adhanPlayed = adhanPlayed,
                    dndBlocked = dndBlocksAdhan,
                    deliveryLatencySeconds = deliveryLatencySeconds,
                )
            } catch (e: Exception) {
                e.printStackTrace()
                CrashReporter.recordException(e)
                showEnhancedPrayerNotification(
                    context,
                    prayerName,
                    prayerType,
                    prayerTime,
                    false,
                    true
                )
                AppAnalytics.logError("notification_display", e.javaClass.simpleName, e.message)
                AppAnalytics.logNotificationDisplayed(
                    prayerType = prayerType,
                    isPreReminder = isPreReminder,
                    adhanPlayed = false,
                    dndBlocked = false,
                    deliveryLatencySeconds = deliveryLatencySeconds,
                )
            }
        }
    }

    /**
     * Seconds between the time a notification was scheduled to fire and now.
     * Returns null when the scheduled time can't be parsed (e.g. the human-readable
     * time strings used by the manual test notifications).
     */
    private fun deliveryLatencySeconds(scheduledTimeIso: String): Long? = runCatching {
        val scheduled = java.time.LocalDateTime.parse(scheduledTimeIso)
        java.time.Duration.between(scheduled, java.time.LocalDateTime.now()).seconds
    }.getOrNull()

    private suspend fun isPrayerNotificationEnabled(prayerType: String): Boolean {
        return when (prayerType.uppercase()) {
            "FAJR" -> preferencesDataStore.fajrNotificationEnabled.first()
            "SUNRISE" -> preferencesDataStore.sunriseNotificationEnabled.first()
            "DHUHR" -> preferencesDataStore.dhuhrNotificationEnabled.first()
            "ASR" -> preferencesDataStore.asrNotificationEnabled.first()
            "MAGHRIB" -> preferencesDataStore.maghribNotificationEnabled.first()
            "ISHA" -> preferencesDataStore.ishaNotificationEnabled.first()
            else -> true
        }
    }

    /**
     * Show an enhanced pre-reminder notification with motivational content.
     */
    private fun showEnhancedPreReminderNotification(
        context: Context,
        prayerName: String,
        prayerType: String,
        minutesBefore: Int,
        vibrationEnabled: Boolean
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        val mainIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val openPendingIntent = mainIntent?.let {
            PendingIntent.getActivity(
                context,
                (prayerName + "_reminder").hashCode(),
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val title =
            NotificationContentHelper.getPreReminderTitle(context, prayerName, minutesBefore)
        val message = NotificationContentHelper.getPreReminderMessage(context, prayerName)
        val bigText = "$message\n\n${NotificationContentHelper.getTimeBasedGreeting(context)}"

        val notification =
            NotificationCompat.Builder(
                context,
                NimazChannels.forPrayer(vibrationEnabled)
            )
                .setSmallIcon(R.drawable.ic_stat_nimaz)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
                .setAutoCancel(true)
                .setContentIntent(openPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .apply {
                    // Pre-O: channel has no effect, so also clear the vibration pattern.
                    if (!vibrationEnabled) {
                        setVibrate(longArrayOf(0L))
                    }
                }
                .build()

        notificationManager.notify((prayerName + "_reminder").hashCode(), notification)
    }

    /**
     * Show an enhanced prayer notification with Islamic greetings and motivational messages.
     */
    private fun showEnhancedPrayerNotification(
        context: Context,
        prayerName: String,
        prayerType: String,
        prayerTime: String,
        adhanEnabled: Boolean = false,
        vibrationEnabled: Boolean = true,
        muted: Boolean = false
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        // Create intent to open app and stop adhan
        val mainIntent =
            context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                putExtra(EXTRA_STOP_ADHAN, true)
            }
        val openPendingIntent = mainIntent?.let {
            PendingIntent.getActivity(
                context,
                prayerName.hashCode(),
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        // Create intent to stop adhan when notification is dismissed
        val dismissIntent = Intent(context, AdhanPlaybackService::class.java).apply {
            action = AdhanPlaybackService.ACTION_STOP
        }
        val dismissPendingIntent = PendingIntent.getService(
            context,
            prayerName.hashCode() + 1000,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Get enhanced content. The time now lives in the title, so the body is
        // just the calm reminder plus a short reflection when expanded.
        val title = NotificationContentHelper.getPrayerTitle(prayerType, prayerTime)
        val shortMessage = NotificationContentHelper.getShortMessage(context, prayerType)
        val reflection = NotificationContentHelper.getPrayerMessage(prayerType)
        val bigText = "$shortMessage\n\n$reflection"

        val channelId = if (adhanEnabled) {
            NimazChannels.forAdhan(vibrationEnabled)
        } else {
            NimazChannels.forPrayer(vibrationEnabled, muted = muted)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_nimaz)
            .setContentTitle(title)
            .setContentText(shortMessage)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(bigText)
                    .setBigContentTitle(title)
            )
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .setDeleteIntent(dismissPendingIntent)
            .setPriority(
                // A silenced prayer must not push a heads-up banner over what the user is
                // doing. The channel already decides this on Android 8+, but the priority
                // is what older builds and some launchers read.
                if (muted) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_HIGH
            )
            .setCategory(
                if (muted) NotificationCompat.CATEGORY_REMINDER
                else NotificationCompat.CATEGORY_ALARM
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColorized(true)
            .setColor(getPrayerColor(prayerType))
            .apply {
                if (muted) {
                    setSilent(true)
                } else if (!vibrationEnabled) {
                    setVibrate(longArrayOf(0L))
                }
                // Add action to mark prayer as done (optional future feature)
                // addAction(R.drawable.ic_check, "Prayed", markPrayedIntent)
            }
            .build()

        notificationManager.notify(prayerName.hashCode(), notification)
    }

    /**
     * Handle the daily summary notification at 11 PM.
     */
    private fun handleDailySummary(context: Context) {
        scope.launch {
            try {
                // Check if notifications are enabled
                val prefs = preferencesDataStore.userPreferences.first()
                if (!prefs.prayerNotificationsEnabled) return@launch

                // Get today's prayer records
                val todayEpoch = LocalDate.now()
                    .atStartOfDay()
                    .toEpochSecond(ZoneOffset.UTC) * 1000

                val prayerRecords = prayerRepository.getPrayerRecordsForDate(todayEpoch).first()

                // Count prayed and missed (excluding Sunrise)
                val mainPrayers = prayerRecords.filter { it.prayerName != PrayerName.SUNRISE }
                val prayedCount =
                    mainPrayers.count { it.status == PrayerStatus.PRAYED || it.status == PrayerStatus.LATE }
                val missedCount =
                    mainPrayers.count { it.status == PrayerStatus.MISSED || it.status == PrayerStatus.NOT_PRAYED }
                val missedPrayers = mainPrayers
                    .filter { it.status == PrayerStatus.MISSED || it.status == PrayerStatus.NOT_PRAYED }
                    .map { it.prayerName.displayName() }

                // Get notification content
                val summaryContent = NotificationContentHelper.getDailySummaryContent(
                    context = context,
                    prayedCount = prayedCount,
                    missedCount = missedCount,
                    missedPrayers = missedPrayers
                )

                showDailySummaryNotification(context, summaryContent)
                AppAnalytics.logDailySummaryShown(
                    prayedCount = prayedCount,
                    missedCount = missedCount
                )

            } catch (e: Exception) {
                e.printStackTrace()
                CrashReporter.recordException(e)
                AppAnalytics.logError("daily_summary", e.javaClass.simpleName, e.message)
            }
        }
    }

    /**
     * Show the daily summary notification.
     */
    private fun showDailySummaryNotification(
        context: Context,
        content: NotificationContentHelper.DailySummaryContent
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        val mainIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val openPendingIntent = mainIntent?.let {
            PendingIntent.getActivity(
                context,
                "daily_summary".hashCode(),
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        // Choose color based on positive/negative outcome
        val notificationColor = if (content.isPositive) {
            0xFF4CAF50.toInt() // Green for positive
        } else {
            0xFFFF9800.toInt() // Orange for needs improvement
        }

        val notification = NotificationCompat.Builder(
            context,
            NimazChannels.DAILY_SUMMARY
        )
            .setSmallIcon(R.drawable.ic_stat_nimaz)
            .setContentTitle(content.title)
            .setContentText(content.message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(content.bigText)
                    .setBigContentTitle(content.title)
            )
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setColorized(true)
            .setColor(notificationColor)
            .build()

        notificationManager.notify("daily_summary".hashCode(), notification)
    }

    /**
     * Post the weekly Friday (Jummah) reminder. Re-checks that notifications and the
     * Friday reminder are still enabled, and honours Do Not Disturb.
     */
    private fun handleFridayReminder(context: Context) {
        scope.launch {
            try {
                val prefs = preferencesDataStore.userPreferences.first()
                if (!prefs.prayerNotificationsEnabled) return@launch
                if (!preferencesDataStore.fridayReminderEnabled.first()) return@launch

                // The Friday reminder has no adhan audio, so it always posts; the OS
                // silences its channel sound under Do Not Disturb.
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                val vibrationEnabled = preferencesDataStore.notificationVibration.first()

                val mainIntent =
                    context.packageManager.getLaunchIntentForPackage(context.packageName)
                val openPendingIntent = mainIntent?.let {
                    PendingIntent.getActivity(
                        context,
                        "friday_reminder".hashCode(),
                        it,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }

                val title = NotificationContentHelper.getFridayReminderTitle(context)
                val message = NotificationContentHelper.getFridayReminderMessage(context)
                val bigText = NotificationContentHelper.getFridayReminderBigText(context)

                val notification = NotificationCompat.Builder(
                    context,
                    NimazChannels.forPrayer(vibrationEnabled)
                )
                    .setSmallIcon(R.drawable.ic_stat_nimaz)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(
                        NotificationCompat.BigTextStyle().bigText(bigText).setBigContentTitle(title)
                    )
                    .setAutoCancel(true)
                    .setContentIntent(openPendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setColorized(true)
                    .setColor(0xFF0D9488.toInt())
                    .apply { if (!vibrationEnabled) setVibrate(longArrayOf(0L)) }
                    .build()

                notificationManager.notify("friday_reminder".hashCode(), notification)
            } catch (e: Exception) {
                e.printStackTrace()
                CrashReporter.recordException(e)
                AppAnalytics.logError("friday_reminder", e.javaClass.simpleName, e.message)
            }
        }
    }

    /**
     * Post the daily khatam reading reminder. Silently does nothing when the reminder is
     * off or there is no active khatam — there is nothing meaningful to nudge about.
     */
    private fun handleKhatamReminder(context: Context) {
        scope.launch {
            try {
                // Re-apply the saved locale first: below API 33 the per-app locale is
                // process-local and set asynchronously by AppInitializer, so an alarm
                // that cold-starts the process would otherwise format this notification
                // in the system language rather than the user's chosen one.
                val langCode = preferencesDataStore.appLanguage.first()
                if (langCode.isNotEmpty()) {
                    LocaleHelper.setLocale(context, langCode)
                }

                if (!preferencesDataStore.khatamReminderEnabled.first()) return@launch

                val khatam = khatamRepository.observeActiveKhatam().first() ?: return@launch

                val daysActive = KhatamProgressCalculator.daysActive(khatam.startedAt)
                val averagePace =
                    KhatamProgressCalculator.averagePace(khatam.totalAyahsRead, daysActive)
                val pace = KhatamProgressCalculator.paceStatus(
                    averagePace = averagePace,
                    dailyTarget = khatam.dailyTarget,
                    daysActive = daysActive
                )

                // What to read today: the daily target, plus the accumulated shortfall
                // when behind so the number actually gets them back on pace. Never more
                // than what is left of the Quran.
                val shortfall =
                    (khatam.dailyTarget * daysActive - khatam.totalAyahsRead).coerceAtLeast(0)
                val ayahsToday = (khatam.dailyTarget + shortfall)
                    .coerceAtMost(khatam.remainingAyahs)
                    .coerceAtLeast(0)

                val title = NotificationContentHelper.getKhatamReminderTitle(context)
                val body = NotificationContentHelper.getKhatamReminderBody(
                    context = context,
                    khatamName = khatam.name,
                    ayahsToday = ayahsToday,
                    pace = pace
                )

                val vibrationEnabled = preferencesDataStore.notificationVibration.first()

                val mainIntent =
                    context.packageManager.getLaunchIntentForPackage(context.packageName)
                val openPendingIntent = mainIntent?.let {
                    PendingIntent.getActivity(
                        context,
                        "khatam_reminder".hashCode(),
                        it,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }

                val notification = NotificationCompat.Builder(
                    context,
                    NimazChannels.KHATAM
                )
                    .setSmallIcon(R.drawable.ic_stat_nimaz)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setStyle(
                        NotificationCompat.BigTextStyle().bigText(body).setBigContentTitle(title)
                    )
                    .setAutoCancel(true)
                    .setContentIntent(openPendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .apply { if (!vibrationEnabled) setVibrate(longArrayOf(0L)) }
                    .build()

                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                notificationManager.notify("khatam_reminder".hashCode(), notification)
            } catch (e: Exception) {
                e.printStackTrace()
                CrashReporter.recordException(e)
                AppAnalytics.logError("khatam_reminder", e.javaClass.simpleName, e.message)
            }
        }
    }

    /**
     * Post an extended worship reminder (Tahajjud, Suhoor, Iftar, adhkar, …). Re-checks the
     * per-type preference at fire time, re-applies the saved locale (so a cold-process alarm
     * formats in the user's language), and posts a gentle nudge on the worship channel.
     */
    private fun handleWorshipReminder(context: Context, intent: Intent) {
        val typeName = intent.getStringExtra(PrayerNotificationScheduler.EXTRA_WORSHIP_TYPE) ?: return
        val subKey = intent.getStringExtra(PrayerNotificationScheduler.EXTRA_WORSHIP_SUBKEY)
        val type = runCatching {
            com.arshadshah.nimaz.domain.model.WorshipReminderType.valueOf(typeName)
        }.getOrNull() ?: return

        scope.launch {
            try {
                val langCode = preferencesDataStore.appLanguage.first()
                if (langCode.isNotEmpty()) LocaleHelper.setLocale(context, langCode)

                val prefs = preferencesDataStore.userPreferences.first()
                if (!prefs.prayerNotificationsEnabled) return@launch
                if (!preferencesDataStore.worshipReminderEnabled(type.key).first()) return@launch

                val title = WorshipReminderContent.title(context, type, subKey)
                val body = WorshipReminderContent.body(context, type, subKey)
                val vibrationEnabled = preferencesDataStore.notificationVibration.first()

                val mainIntent =
                    context.packageManager.getLaunchIntentForPackage(context.packageName)
                val openPendingIntent = mainIntent?.let {
                    PendingIntent.getActivity(
                        context,
                        ("worship_" + type.key).hashCode(),
                        it,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }

                val notification = NotificationCompat.Builder(
                    context,
                    NimazChannels.WORSHIP
                )
                    .setSmallIcon(R.drawable.ic_stat_nimaz)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setStyle(
                        NotificationCompat.BigTextStyle().bigText(body).setBigContentTitle(title)
                    )
                    .setAutoCancel(true)
                    .setContentIntent(openPendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setColor(0xFF0D9488.toInt())
                    .apply { if (!vibrationEnabled) setVibrate(longArrayOf(0L)) }
                    .build()

                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                notificationManager.notify(("worship_" + type.key).hashCode(), notification)
                AppAnalytics.logFeatureUsed("worship_reminder", type.key)
            } catch (e: Exception) {
                e.printStackTrace()
                CrashReporter.recordException(e)
                AppAnalytics.logError("worship_reminder", e.javaClass.simpleName, e.message)
            }
        }
    }

    /**
     * Get a color for the prayer notification based on prayer type.
     */
    private fun getPrayerColor(prayerType: String): Int {
        return when (prayerType.uppercase()) {
            "FAJR" -> 0xFF3F51B5.toInt()     // Indigo - dawn
            "SUNRISE" -> 0xFFFF9800.toInt()  // Orange - sun
            "DHUHR" -> 0xFF2196F3.toInt()    // Blue - midday sky
            "ASR" -> 0xFF009688.toInt()      // Teal - afternoon
            "MAGHRIB" -> 0xFFE91E63.toInt()  // Pink - sunset
            "ISHA" -> 0xFF673AB7.toInt()     // Deep Purple - night
            else -> 0xFF4CAF50.toInt()       // Green - default
        }
    }

    companion object {
        const val EXTRA_STOP_ADHAN = "stop_adhan"
    }
}
