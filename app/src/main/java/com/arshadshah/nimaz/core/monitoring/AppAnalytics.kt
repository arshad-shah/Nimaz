package com.arshadshah.nimaz.core.monitoring

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Central wrapper around Firebase Analytics.
 *
 * Like [CrashReporter], every call is guarded so it safely no-ops when Firebase
 * is not initialized (builds without `google-services.json`). Firebase Analytics
 * already auto-collects events such as `app_open` and `session_start`; this
 * helper layers on top of that:
 *
 *  - **Screen tracking** for the Compose navigation graph (with the previous
 *    screen attached so we can spot dead-ends and back-and-forth loops where a
 *    user is "stuck").
 *  - **A typed catalog** ([Event] / [Param] / [UserProperty]) so event and
 *    parameter names stay consistent across call sites and within Firebase's
 *    naming limits.
 *  - **Semantic helpers** for the areas where we most need visibility: the
 *    prayer-notification pipeline (scheduled → displayed → opened), the adhan,
 *    permission/diagnostics state, onboarding progression, app start-up health
 *    and non-fatal errors.
 *
 * The application context is captured once via [init] (called from `NimazApp`),
 * so the rest of the app — including ViewModels, services and the
 * `BootReceiver` — can log without threading a [Context] through every layer.
 * The context-taking overloads are kept for callers that already have one.
 */
object AppAnalytics {

    @Volatile
    private var appContext: Context? = null

    /** Captures the application context. Call once from `Application.onCreate`. */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun analytics(context: Context? = appContext): FirebaseAnalytics? =
        context?.let { runCatching { FirebaseAnalytics.getInstance(it) }.getOrNull() }

    // ---------------------------------------------------------------------
    // Core logging
    // ---------------------------------------------------------------------

    /** Logs a screen view. Backwards-compatible overload that takes an explicit context. */
    fun logScreenView(context: Context, screenName: String) {
        logScreenView(screenName, previousScreen = null, context = context)
    }

    /**
     * Logs a screen view, optionally recording the screen the user came from.
     * Tracking the previous screen lets us detect navigation loops and abandoned
     * flows ("getting stuck") in the funnel reports.
     */
    fun logScreenView(
        screenName: String,
        previousScreen: String? = null,
        context: Context? = appContext,
    ) {
        runCatching {
            analytics(context)?.logEvent(
                FirebaseAnalytics.Event.SCREEN_VIEW,
                bundleOf(
                    FirebaseAnalytics.Param.SCREEN_NAME to screenName,
                    Param.PREVIOUS_SCREEN to previousScreen,
                )
            )
        }
    }

    /** Backwards-compatible overload that takes an explicit context and a pre-built bundle. */
    fun logEvent(context: Context, name: String, params: Bundle? = null) {
        runCatching { analytics(context)?.logEvent(name, params) }
    }

    /** Logs a custom event using the captured application context. */
    fun logEvent(name: String, params: Bundle? = null) {
        runCatching { analytics()?.logEvent(name, params) }
    }

    /** Convenience: log a custom event from key/value pairs without building a [Bundle] by hand. */
    fun logEvent(name: String, vararg params: Pair<String, Any?>) {
        logEvent(name, bundleOf(*params))
    }

    /**
     * Sets a user property used to segment analytics (e.g. only users with
     * notifications disabled, or on a specific calculation method). Property
     * names are limited to 24 chars and values to 36 by Firebase.
     */
    fun setUserProperty(name: String, value: String?) {
        runCatching { analytics()?.setUserProperty(name, value?.take(36)) }
    }

    // ---------------------------------------------------------------------
    // Notification pipeline
    // ---------------------------------------------------------------------

    /**
     * Logged when prayer notifications are (re)scheduled. The exact-alarm and
     * post-notification flags are the strongest predictors of whether the
     * notifications the user expects will actually fire.
     */
    fun logNotificationsScheduled(
        scheduledCount: Int,
        preRemindersEnabled: Boolean,
        exactAlarmAllowed: Boolean,
        postNotificationsGranted: Boolean,
    ) {
        logEvent(
            Event.NOTIFICATION_SCHEDULED,
            Param.SCHEDULED_COUNT to scheduledCount,
            Param.PRE_REMINDER_ENABLED to preRemindersEnabled,
            Param.EXACT_ALARM_ALLOWED to exactAlarmAllowed,
            Param.POST_NOTIF_GRANTED to postNotificationsGranted,
        )
    }

    /** Logged when the user has prayer notifications turned off and we cancel everything. */
    fun logNotificationsCancelled(reason: String) {
        logEvent(Event.NOTIFICATION_CANCELLED, Param.REASON to reason)
    }

    /**
     * Logged when a prayer notification actually reaches the device and is shown.
     * [deliveryLatencySeconds] is the gap between the scheduled time and when the
     * receiver fired — a large value means Doze/battery optimization delayed it,
     * which is exactly the "notifications don't work right" class of problem.
     */
    fun logNotificationDisplayed(
        prayerType: String,
        isPreReminder: Boolean,
        adhanPlayed: Boolean,
        dndBlocked: Boolean,
        deliveryLatencySeconds: Long?,
    ) {
        logEvent(
            Event.NOTIFICATION_DISPLAYED,
            Param.PRAYER_TYPE to prayerType,
            Param.IS_PRE_REMINDER to isPreReminder,
            Param.ADHAN_PLAYED to adhanPlayed,
            Param.DND_BLOCKED to dndBlocked,
            Param.DELIVERY_LATENCY_SEC to deliveryLatencySeconds,
            Param.DELAYED to (deliveryLatencySeconds != null && deliveryLatencySeconds > DELAYED_THRESHOLD_SEC),
        )
    }

    /** Logged when a scheduled notification fired but was suppressed (e.g. disabled for that prayer). */
    fun logNotificationSuppressed(prayerType: String, reason: String) {
        logEvent(
            Event.NOTIFICATION_SUPPRESSED,
            Param.PRAYER_TYPE to prayerType,
            Param.REASON to reason,
        )
    }

    /** Logged when the user opens the app from a notification. [source] identifies which one. */
    fun logNotificationOpened(source: String) {
        logEvent(Event.NOTIFICATION_OPENED, Param.SOURCE to source)
    }

    /** Logged when notifications are rescheduled (after boot or at midnight). */
    fun logNotificationReschedule(trigger: String) {
        logEvent(Event.NOTIFICATION_RESCHEDULE, Param.TRIGGER to trigger)
    }

    /** Logged when the daily summary notification is shown, with the day's tally. */
    fun logDailySummaryShown(prayedCount: Int, missedCount: Int) {
        logEvent(
            Event.DAILY_SUMMARY_SHOWN,
            Param.PRAYED_COUNT to prayedCount,
            Param.MISSED_COUNT to missedCount,
        )
    }

    /** Logged when a test notification is fired from settings. */
    fun logTestNotification(allPrayers: Boolean) {
        logEvent(Event.TEST_NOTIFICATION, Param.ALL_PRAYERS to allPrayers)
    }

    // ---------------------------------------------------------------------
    // Adhan
    // ---------------------------------------------------------------------

    /**
     * Logged when the adhan audio file expected at notification time is missing
     * and a re-download is triggered. A spike here means users are getting silent
     * notifications when they expect the adhan — a "doing things incorrectly" case.
     */
    fun logAdhanFileMissing(adhanSound: String, isFajr: Boolean) {
        logEvent(
            Event.ADHAN_FILE_MISSING,
            Param.ADHAN_SOUND to adhanSound,
            Param.IS_FAJR to isFajr,
        )
    }

    // ---------------------------------------------------------------------
    // Onboarding funnel
    // ---------------------------------------------------------------------

    /** Logged as the user moves through onboarding pages — reveals where people drop off. */
    fun logOnboardingStep(page: Int) {
        logEvent(Event.ONBOARDING_STEP, Param.STEP to page)
    }

    /** Logged when onboarding is completed, with which permissions the user granted. */
    fun logOnboardingCompleted(
        locationGranted: Boolean,
        notificationGranted: Boolean,
        batteryOptimizationDisabled: Boolean,
    ) {
        logEvent(
            Event.ONBOARDING_COMPLETED,
            Param.LOCATION_GRANTED to locationGranted,
            Param.NOTIFICATION_GRANTED to notificationGranted,
            Param.BATTERY_OPTIMIZED to !batteryOptimizationDisabled,
        )
    }

    // ---------------------------------------------------------------------
    // App start-up & errors
    // ---------------------------------------------------------------------

    /** Logged when [AppInitializer] finishes, including whether it timed out. */
    fun logAppInit(durationMs: Long, timedOut: Boolean) {
        logEvent(
            Event.APP_INIT,
            Param.DURATION_MS to durationMs,
            Param.TIMED_OUT to timedOut,
        )
    }

    /**
     * Logs a non-fatal error as an analytics event so error *frequency* and the
     * affected user share are visible in dashboards (Crashlytics shows the stack
     * trace; this shows how often and to how many users it happens).
     */
    fun logError(domain: String, type: String, message: String? = null) {
        logEvent(
            Event.APP_ERROR,
            Param.ERROR_DOMAIN to domain,
            Param.ERROR_TYPE to type,
            Param.ERROR_MESSAGE to message?.take(100),
        )
    }

    /** A general feature-usage event for actions that are not screens (counters, toggles, etc.). */
    fun logFeatureUsed(feature: String, action: String? = null) {
        logEvent(
            Event.FEATURE_USED,
            Param.FEATURE to feature,
            Param.ACTION to action,
        )
    }

    /** Logged when a prayer's tracker status changes — the app's core engagement signal. */
    fun logPrayerTracked(prayer: String, status: String, isJamaah: Boolean = false) {
        logEvent(
            Event.PRAYER_TRACKED,
            Param.PRAYER_TYPE to prayer,
            Param.STATUS to status,
            Param.IS_JAMAAH to isJamaah,
        )
    }

    /** Logged when a fasting record changes. */
    fun logFastTracked(action: String, fastType: String? = null) {
        logEvent(
            Event.FAST_TRACKED,
            Param.ACTION to action,
            Param.FAST_TYPE to fastType,
        )
    }

    /**
     * Logged when a user changes a setting. Seeing which settings people change —
     * and to what — surfaces misconfiguration (e.g. notifications switched off, an
     * unusual calculation method) behind "it's doing the wrong thing" reports.
     */
    fun logSettingChanged(setting: String, value: String) {
        logEvent(
            Event.SETTING_CHANGED,
            Param.SETTING to setting,
            Param.VALUE to value,
        )
    }

    /**
     * Logged when a search runs. The raw query is deliberately not recorded — only
     * the active filter and the query length — to keep user input out of analytics.
     */
    fun logSearch(filter: String, queryLength: Int) {
        logEvent(
            Event.SEARCH,
            Param.FILTER to filter,
            Param.QUERY_LENGTH to queryLength,
        )
    }

    // ---------------------------------------------------------------------
    // Diagnostics snapshot
    // ---------------------------------------------------------------------

    /**
     * Reads the current notification-delivery prerequisites and records them as
     * both a one-off event and durable user properties. Segmenting by these in
     * Firebase answers "do notifications work?" at the population level: a user
     * with notifications enabled in-app but `post_notif_granted=false` or
     * `battery_optimized=true` is the typical broken case.
     */
    fun logDiagnostics(context: Context? = appContext) {
        val ctx = context ?: return
        runCatching {
            val notificationsEnabled = NotificationManagerCompat.from(ctx).areNotificationsEnabled()

            val exactAlarmAllowed = exactAlarmAllowed(ctx)

            val powerManager = ctx.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val batteryOptimized = powerManager
                ?.isIgnoringBatteryOptimizations(ctx.packageName)?.not() ?: false

            setUserProperty(UserProperty.POST_NOTIF_GRANTED, notificationsEnabled.toString())
            setUserProperty(UserProperty.EXACT_ALARM_ALLOWED, exactAlarmAllowed.toString())
            setUserProperty(UserProperty.BATTERY_OPTIMIZED, batteryOptimized.toString())

            logEvent(
                Event.DIAGNOSTICS,
                Param.POST_NOTIF_GRANTED to notificationsEnabled,
                Param.EXACT_ALARM_ALLOWED to exactAlarmAllowed,
                Param.BATTERY_OPTIMIZED to batteryOptimized,
            )
        }
    }

    /** True if the OS currently allows this app to schedule exact alarms. */
    fun exactAlarmAllowed(context: Context? = appContext): Boolean {
        val ctx = context ?: return true
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.canScheduleExactAlarms() ?: false
        } else {
            true // No runtime gate before Android 12.
        }
    }

    /** True if the app may post notifications (channel/app level). */
    fun postNotificationsGranted(context: Context? = appContext): Boolean {
        val ctx = context ?: return true
        return runCatching {
            NotificationManagerCompat.from(ctx).areNotificationsEnabled()
        }.getOrDefault(true)
    }

    // ---------------------------------------------------------------------
    // Helpers & catalog
    // ---------------------------------------------------------------------

    /**
     * Builds a [Bundle] from key/value pairs, skipping nulls and coercing common
     * types into what Firebase Analytics accepts. Booleans are stored as
     * "true"/"false" strings so they read cleanly as dimensions.
     */
    private fun bundleOf(vararg params: Pair<String, Any?>): Bundle = Bundle().apply {
        params.forEach { (key, value) ->
            when (value) {
                null -> {}
                is String -> putString(key, value.take(100))
                is Boolean -> putString(key, value.toString())
                is Int -> putLong(key, value.toLong())
                is Long -> putLong(key, value)
                is Float -> putDouble(key, value.toDouble())
                is Double -> putDouble(key, value)
                else -> putString(key, value.toString().take(100))
            }
        }
    }

    private const val DELAYED_THRESHOLD_SEC = 120L

    /** Custom event names (Firebase limit: 40 chars, snake_case). */
    object Event {
        const val NOTIFICATION_SCHEDULED = "notification_scheduled"
        const val NOTIFICATION_CANCELLED = "notification_cancelled"
        const val NOTIFICATION_DISPLAYED = "notification_displayed"
        const val NOTIFICATION_SUPPRESSED = "notification_suppressed"
        const val NOTIFICATION_OPENED = "notification_opened"
        const val NOTIFICATION_RESCHEDULE = "notification_reschedule"
        const val DAILY_SUMMARY_SHOWN = "daily_summary_shown"
        const val TEST_NOTIFICATION = "test_notification"
        const val ADHAN_FILE_MISSING = "adhan_file_missing"
        const val ONBOARDING_STEP = "onboarding_step"
        const val ONBOARDING_COMPLETED = "onboarding_completed"
        const val APP_INIT = "app_init"
        const val APP_ERROR = "app_error"
        const val FEATURE_USED = "feature_used"
        const val DIAGNOSTICS = "diagnostics_snapshot"
        const val PRAYER_TRACKED = "prayer_tracked"
        const val FAST_TRACKED = "fast_tracked"
        const val SETTING_CHANGED = "setting_changed"
        const val SEARCH = "search_performed"
    }

    /** Custom parameter names (Firebase limit: 40 chars). */
    object Param {
        const val PREVIOUS_SCREEN = "previous_screen"
        const val PRAYER_TYPE = "prayer_type"
        const val IS_PRE_REMINDER = "is_pre_reminder"
        const val ADHAN_PLAYED = "adhan_played"
        const val DND_BLOCKED = "dnd_blocked"
        const val DELIVERY_LATENCY_SEC = "delivery_latency_sec"
        const val DELAYED = "delayed"
        const val SCHEDULED_COUNT = "scheduled_count"
        const val PRE_REMINDER_ENABLED = "pre_reminder_enabled"
        const val EXACT_ALARM_ALLOWED = "exact_alarm_allowed"
        const val POST_NOTIF_GRANTED = "post_notif_granted"
        const val BATTERY_OPTIMIZED = "battery_optimized"
        const val REASON = "reason"
        const val SOURCE = "source"
        const val TRIGGER = "trigger"
        const val PRAYED_COUNT = "prayed_count"
        const val MISSED_COUNT = "missed_count"
        const val ALL_PRAYERS = "all_prayers"
        const val ADHAN_SOUND = "adhan_sound"
        const val IS_FAJR = "is_fajr"
        const val STEP = "step"
        const val LOCATION_GRANTED = "location_granted"
        const val NOTIFICATION_GRANTED = "notification_granted"
        const val DURATION_MS = "duration_ms"
        const val TIMED_OUT = "timed_out"
        const val ERROR_DOMAIN = "error_domain"
        const val ERROR_TYPE = "error_type"
        const val ERROR_MESSAGE = "error_message"
        const val FEATURE = "feature"
        const val ACTION = "action"
        const val STATUS = "status"
        const val IS_JAMAAH = "is_jamaah"
        const val FAST_TYPE = "fast_type"
        const val SETTING = "setting"
        const val VALUE = "value"
        const val FILTER = "filter"
        const val QUERY_LENGTH = "query_length"
    }

    /** User-property names (Firebase limit: 24 chars). */
    object UserProperty {
        const val NOTIFICATIONS_ENABLED = "notifications_enabled"
        const val APP_LANGUAGE = "app_language"
        const val CALC_METHOD = "calc_method"
        const val LOCATION_SET = "location_set"
        const val POST_NOTIF_GRANTED = "post_notif_granted"
        const val EXACT_ALARM_ALLOWED = "exact_alarm_allowed"
        const val BATTERY_OPTIMIZED = "battery_optimized"
    }
}
