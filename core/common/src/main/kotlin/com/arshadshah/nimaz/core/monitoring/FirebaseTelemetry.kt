package com.arshadshah.nimaz.core.monitoring

import javax.inject.Inject
import javax.inject.Singleton

/**
 * The production [Telemetry], delegating to the existing [AppAnalytics] and
 * [CrashReporter] objects.
 *
 * Deliberately a thin pass-through with no logic of its own: every guard already
 * lives in those two objects (both no-op safely when Firebase is not initialised,
 * e.g. builds without `google-services.json`). Keeping it thin means the behaviour
 * worth testing lives on [Telemetry] itself, where `RecordingTelemetry` can reach it.
 */
@Singleton
class FirebaseTelemetry @Inject constructor() : Telemetry {

    override fun featureUsed(feature: String, action: String?) =
        AppAnalytics.logFeatureUsed(feature, action)

    override fun settingChanged(setting: String, value: String) =
        AppAnalytics.logSettingChanged(setting, value)

    override fun search(filter: String, queryLength: Int) =
        AppAnalytics.logSearch(filter, queryLength)

    override fun aiAnswered(proofCount: Int, durationMs: Long, confidence: String) =
        AppAnalytics.logAiAnswered(proofCount, durationMs, confidence)

    override fun prayerTracked(prayer: String, status: String, isJamaah: Boolean) =
        AppAnalytics.logPrayerTracked(prayer, status, isJamaah)

    override fun fastTracked(action: String, fastType: String?) =
        AppAnalytics.logFastTracked(action, fastType)

    override fun error(domain: String, type: String, message: String?) =
        AppAnalytics.logError(domain, type, message)

    override fun recordException(throwable: Throwable) =
        CrashReporter.recordException(throwable)

    override fun announcementShown(id: String, type: String) =
        AppAnalytics.logAnnouncementShown(id, type)

    override fun announcementCtaClicked(id: String, route: String?) =
        AppAnalytics.logAnnouncementCtaClicked(id, route)

    override fun announcementDismissed(id: String) =
        AppAnalytics.logAnnouncementDismissed(id)

    override fun announcementRouteRejected(id: String, route: String?) =
        AppAnalytics.logAnnouncementRouteRejected(id, route)

    override fun userProperty(name: String, value: String?) =
        AppAnalytics.setUserProperty(name, value)

    override fun testNotification(allPrayers: Boolean) =
        AppAnalytics.logTestNotification(allPrayers)

    override fun onboardingStep(page: Int) =
        AppAnalytics.logOnboardingStep(page)

    override fun onboardingCompleted(
        locationGranted: Boolean,
        notificationGranted: Boolean,
        batteryOptimizationDisabled: Boolean,
    ) = AppAnalytics.logOnboardingCompleted(
        locationGranted, notificationGranted, batteryOptimizationDisabled,
    )

    override suspend fun <T> trace(name: String, block: suspend () -> T): T =
        PerfMonitor.traceSuspend(name, block)

    override fun traceValue(name: String, metric: String, value: Long) {
        val t = PerfMonitor.newTrace(name)
        PerfMonitor.stop(t, metrics = mapOf(metric to value))
    }

    override fun breadcrumb(message: String) = CrashReporter.log(message)

    override fun customKey(key: String, value: String) = CrashReporter.setCustomKey(key, value)
}
