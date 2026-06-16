package com.arshadshah.nimaz.domain.model

import android.net.Uri
import com.arshadshah.nimaz.R

/**
 * Categories a user can tag a bug report with. Each maps to a stable
 * [firestoreValue] (persisted, never localized) and a [labelResId] shown in the
 * dropdown. Order here is the order shown in the UI.
 */
enum class BugCategory(val firestoreValue: String, val labelResId: Int) {
    PRAYER_TIMES("prayer_times", R.string.bug_category_prayer_times),
    NOTIFICATIONS_ADHAN("notifications_adhan", R.string.bug_category_notifications_adhan),
    QIBLA("qibla", R.string.bug_category_qibla),
    QURAN("quran", R.string.bug_category_quran),
    TASBIH("tasbih", R.string.bug_category_tasbih),
    PRAYER_TRACKING("prayer_tracking", R.string.bug_category_prayer_tracking),
    CALENDAR_HIJRI("calendar_hijri", R.string.bug_category_calendar_hijri),
    FASTING("fasting", R.string.bug_category_fasting),
    ZAKAT("zakat", R.string.bug_category_zakat),
    OTHER("other", R.string.bug_category_other),
}

/**
 * Auto-collected diagnostics attached to a bug report. Deliberately privacy-safe:
 * it carries the app/device context and the Nimaz configuration behind the app's
 * most common bug classes, but never precise GPS coordinates — only a coarse
 * [locationMode] (whether a location is set, not where).
 */
data class BugDiagnostics(
    val appVersionName: String,
    val appVersionCode: Long,
    val deviceManufacturer: String,
    val deviceModel: String,
    val androidVersion: String,
    val apiLevel: Int,
    val locale: String,
    val timezone: String,
    val calculationMethod: String,
    val asrMethod: String,
    val highLatitudeRule: String,
    val locationMode: String,
    val notificationsPermissionGranted: Boolean,
    val exactAlarmPermissionGranted: Boolean,
    val batteryOptimizationExempt: Boolean,
) {
    /** Flat string map persisted on the Firestore document. */
    fun toMap(): Map<String, Any> = mapOf(
        "appVersionName" to appVersionName,
        "appVersionCode" to appVersionCode,
        "deviceManufacturer" to deviceManufacturer,
        "deviceModel" to deviceModel,
        "androidVersion" to androidVersion,
        "apiLevel" to apiLevel,
        "locale" to locale,
        "timezone" to timezone,
        "calculationMethod" to calculationMethod,
        "asrMethod" to asrMethod,
        "highLatitudeRule" to highLatitudeRule,
        "locationMode" to locationMode,
        "notificationsPermissionGranted" to notificationsPermissionGranted,
        "exactAlarmPermissionGranted" to exactAlarmPermissionGranted,
        "batteryOptimizationExempt" to batteryOptimizationExempt,
    )
}

/**
 * Everything the user has chosen to submit. [diagnostics] is null when the user
 * toggles the "include diagnostics" switch off, and [screenshotUri] is null when
 * no screenshot is attached.
 */
data class BugReportSubmission(
    val category: BugCategory,
    val description: String,
    val stepsToReproduce: String,
    val contactEmail: String,
    val diagnostics: BugDiagnostics?,
    val screenshotUri: Uri?,
)

/**
 * Outcome of a successful submit. [queuedOffline] is true when the device was
 * offline and Firestore persisted the write locally to sync later, so the UI can
 * tell the user their report is saved and will send when back online.
 */
data class BugSubmitResult(
    val reportId: String,
    val queuedOffline: Boolean,
)
