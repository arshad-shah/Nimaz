package com.arshadshah.nimaz.core.monitoring

/**
 * A [Telemetry] test double that records every call in order.
 *
 * `AppAnalytics` and `CrashReporter` are Kotlin `object`s with a static context, so
 * before [Telemetry] existed no test could assert that an action had been logged.
 * That is precisely why two live defects went unnoticed: `logOnboardingStep` has
 * never fired in production, and `logFeatureUsed("zakat", "calculate")` sits on a
 * branch no screen can reach.
 *
 * Inject this in ViewModel tests and assert against [calls].
 */
class RecordingTelemetry : Telemetry {

    /** Every call made, in order. */
    val calls = mutableListOf<TelemetryCall>()

    val featureUsages: List<TelemetryCall.FeatureUsed>
        get() = calls.filterIsInstance<TelemetryCall.FeatureUsed>()

    val errors: List<TelemetryCall.Error>
        get() = calls.filterIsInstance<TelemetryCall.Error>()

    val exceptions: List<Throwable>
        get() = calls.filterIsInstance<TelemetryCall.Exception>().map { it.throwable }

    val settingChanges: List<TelemetryCall.SettingChanged>
        get() = calls.filterIsInstance<TelemetryCall.SettingChanged>()

    val searches: List<TelemetryCall.Search>
        get() = calls.filterIsInstance<TelemetryCall.Search>()

    val prayersTracked: List<TelemetryCall.PrayerTracked>
        get() = calls.filterIsInstance<TelemetryCall.PrayerTracked>()

    val customKeys: List<TelemetryCall.CustomKey>
        get() = calls.filterIsInstance<TelemetryCall.CustomKey>()

    val aiAnswers: List<TelemetryCall.AiAnswered>
        get() = calls.filterIsInstance<TelemetryCall.AiAnswered>()

    val userProperties: List<TelemetryCall.UserProperty>
        get() = calls.filterIsInstance<TelemetryCall.UserProperty>()

    val announcementsShown: List<TelemetryCall.AnnouncementShown>
        get() = calls.filterIsInstance<TelemetryCall.AnnouncementShown>()

    val onboardingSteps: List<Int>
        get() = calls.filterIsInstance<TelemetryCall.OnboardingStep>().map { it.page }

    val traces: List<TelemetryCall.Trace>
        get() = calls.filterIsInstance<TelemetryCall.Trace>()

    val traceValues: List<TelemetryCall.TraceValue>
        get() = calls.filterIsInstance<TelemetryCall.TraceValue>()

    fun clear() = calls.clear()

    override fun featureUsed(feature: String, action: String?) {
        calls += TelemetryCall.FeatureUsed(feature, action)
    }

    override fun error(domain: String, type: String, message: String?) {
        calls += TelemetryCall.Error(domain, type, message)
    }

    override fun settingChanged(setting: String, value: String) {
        calls += TelemetryCall.SettingChanged(setting, value)
    }

    override fun search(filter: String, queryLength: Int) {
        calls += TelemetryCall.Search(filter, queryLength)
    }

    override fun aiAnswered(proofCount: Int, durationMs: Long, confidence: String) {
        calls += TelemetryCall.AiAnswered(proofCount, durationMs, confidence)
    }

    override fun prayerTracked(prayer: String, status: String, isJamaah: Boolean) {
        calls += TelemetryCall.PrayerTracked(prayer, status, isJamaah)
    }

    override fun fastTracked(action: String, fastType: String?) {
        calls += TelemetryCall.FastTracked(action, fastType)
    }

    override fun recordException(throwable: Throwable) {
        calls += TelemetryCall.Exception(throwable)
    }

    override fun breadcrumb(message: String) {
        calls += TelemetryCall.Breadcrumb(message)
    }

    override fun customKey(key: String, value: String) {
        calls += TelemetryCall.CustomKey(key, value)
    }

    /**
     * Records the trace **and runs the block**. Recording without running would make
     * every test that traces a load see no data, which is the failure mode a fake is
     * supposed to prevent rather than introduce.
     *
     * The call is appended before the block runs, so `calls` reads in the order the
     * work happened even when a trace wraps other telemetry.
     */
    override fun announcementShown(id: String, type: String) {
        calls += TelemetryCall.AnnouncementShown(id, type)
    }

    override fun announcementCtaClicked(id: String, route: String?) {
        calls += TelemetryCall.AnnouncementCtaClicked(id, route)
    }

    override fun announcementDismissed(id: String) {
        calls += TelemetryCall.AnnouncementDismissed(id)
    }

    override fun announcementRouteRejected(id: String, route: String?) {
        calls += TelemetryCall.AnnouncementRouteRejected(id, route)
    }

    override fun userProperty(name: String, value: String?) {
        calls += TelemetryCall.UserProperty(name, value)
    }

    override fun testNotification(allPrayers: Boolean) {
        calls += TelemetryCall.TestNotification(allPrayers)
    }

    override fun onboardingStep(page: Int) {
        calls += TelemetryCall.OnboardingStep(page)
    }

    override fun onboardingCompleted(
        locationGranted: Boolean,
        notificationGranted: Boolean,
        batteryOptimizationDisabled: Boolean,
    ) {
        calls += TelemetryCall.OnboardingCompleted(
            locationGranted, notificationGranted, batteryOptimizationDisabled,
        )
    }

    override suspend fun <T> trace(name: String, block: suspend () -> T): T {
        calls += TelemetryCall.Trace(name)
        return block()
    }

    override fun traceValue(name: String, metric: String, value: Long) {
        calls += TelemetryCall.TraceValue(name, metric, value)
    }
}

/** One recorded [Telemetry] call. */
sealed interface TelemetryCall {
    data class FeatureUsed(val feature: String, val action: String?) : TelemetryCall
    data class Error(val domain: String, val type: String, val message: String?) : TelemetryCall
    data class SettingChanged(val setting: String, val value: String) : TelemetryCall
    data class Search(val filter: String, val queryLength: Int) : TelemetryCall
    data class PrayerTracked(
        val prayer: String,
        val status: String,
        val isJamaah: Boolean,
    ) : TelemetryCall

    data class FastTracked(val action: String, val fastType: String?) : TelemetryCall
    data class AiAnswered(
        val proofCount: Int,
        val durationMs: Long,
        val confidence: String,
    ) : TelemetryCall

    data class Exception(val throwable: Throwable) : TelemetryCall
    data class AnnouncementShown(val id: String, val type: String) : TelemetryCall
    data class AnnouncementCtaClicked(val id: String, val route: String?) : TelemetryCall
    data class AnnouncementDismissed(val id: String) : TelemetryCall
    data class AnnouncementRouteRejected(val id: String, val route: String?) : TelemetryCall
    data class UserProperty(val name: String, val value: String?) : TelemetryCall
    data class TestNotification(val allPrayers: Boolean) : TelemetryCall
    data class OnboardingStep(val page: Int) : TelemetryCall
    data class OnboardingCompleted(
        val locationGranted: Boolean,
        val notificationGranted: Boolean,
        val batteryOptimizationDisabled: Boolean,
    ) : TelemetryCall

    data class Trace(val name: String) : TelemetryCall
    data class TraceValue(val name: String, val metric: String, val value: Long) : TelemetryCall
    data class Breadcrumb(val message: String) : TelemetryCall
    data class CustomKey(val key: String, val value: String) : TelemetryCall
}
