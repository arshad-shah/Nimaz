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

    override fun breadcrumb(message: String) = CrashReporter.log(message)

    override fun customKey(key: String, value: String) = CrashReporter.setCustomKey(key, value)
}
