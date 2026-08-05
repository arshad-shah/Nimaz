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
    data class Exception(val throwable: Throwable) : TelemetryCall
    data class Breadcrumb(val message: String) : TelemetryCall
    data class CustomKey(val key: String, val value: String) : TelemetryCall
}
