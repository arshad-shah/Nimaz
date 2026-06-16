package com.arshadshah.nimaz.core.util

import android.content.Context
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
import com.arshadshah.nimaz.domain.model.BugDiagnostics

/**
 * Gathers the privacy-safe diagnostics attached to a bug report.
 *
 * The fields here map directly onto Nimaz's most common bug classes: the prayer
 * calculation configuration (method / Asr / high-latitude rule) and the
 * notification-delivery prerequisites (post-notification, exact-alarm and
 * battery-optimization state). No precise location is ever read — only a coarse
 * flag for whether a location has been configured.
 */
@Singleton
class DiagnosticsCollector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesDataStore: PreferencesDataStore,
) {

    suspend fun collect(): BugDiagnostics {
        val packageInfo = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0)
        }.getOrNull()

        val versionName = packageInfo?.versionName ?: "unknown"
        val versionCode = packageInfo?.let { PackageInfoCompat.getLongVersionCode(it) } ?: 0L

        val calculationMethod = preferencesDataStore.calculationMethod.first()
        val asrMethod = preferencesDataStore.asrCalculation.first()
        val highLatitudeRule = preferencesDataStore.highLatitudeRule.first()
        // Coarse only: whether a location is configured, never the coordinates.
        val locationName = preferencesDataStore.locationName.first()
        val locationMode = if (locationName.isNotBlank()) "set" else "not_set"

        return BugDiagnostics(
            appVersionName = versionName,
            appVersionCode = versionCode,
            deviceManufacturer = Build.MANUFACTURER ?: "unknown",
            deviceModel = Build.MODEL ?: "unknown",
            androidVersion = Build.VERSION.RELEASE ?: "unknown",
            apiLevel = Build.VERSION.SDK_INT,
            locale = Locale.getDefault().toLanguageTag(),
            timezone = TimeZone.getDefault().id,
            calculationMethod = calculationMethod,
            asrMethod = asrMethod,
            highLatitudeRule = highLatitudeRule,
            locationMode = locationMode,
            notificationsPermissionGranted = AppAnalytics.postNotificationsGranted(context),
            exactAlarmPermissionGranted = AppAnalytics.exactAlarmAllowed(context),
            batteryOptimizationExempt = isIgnoringBatteryOptimizations(),
        )
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }
}
