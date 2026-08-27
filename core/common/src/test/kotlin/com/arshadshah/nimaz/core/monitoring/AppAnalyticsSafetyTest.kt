package com.arshadshah.nimaz.core.monitoring

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The promise `AppAnalytics` makes in its first paragraph: every call no-ops safely when Firebase
 * is not initialised.
 *
 * That is not a hypothetical state. It is every build without `google-services.json` — which
 * includes a fresh clone, a fork, and CI — and it is the state of this test. The guards are the
 * whole reason the app can log from `BootReceiver`, from a Worker, and from `NimazApp.onCreate`
 * before anything is set up, without any of those call sites checking first.
 *
 * A missing `runCatching` therefore does not produce a wrong number. It produces a crash in a
 * broadcast receiver on a device that has never opened the app, which is about the worst place in
 * the codebase to put an exception. Nothing else catches it: the calls are fire-and-forget, so
 * there is no return value for a test to assert on and no caller to notice.
 *
 * So the assertion is simply *"this returned"* — thirty-odd times, once per entry point. It reads
 * thin and it is exactly the property that matters.
 *
 * The two permission helpers are different: they answer a question, and what they answer when they
 * cannot tell is a decision. Both default to **allowed**, because reporting every install as
 * broken is worse than missing a genuinely broken one.
 */
@RunWith(RobolectricTestRunner::class)
class AppAnalyticsSafetyTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        AppAnalytics.init(context)
    }

    // ---- Core logging ----

    @Test
    fun `logging a screen view without Firebase is a no-op`() {
        AppAnalytics.logScreenView(context, "Home")
        AppAnalytics.logScreenView("Home", previousScreen = "Onboarding")
        AppAnalytics.logScreenView("Home", previousScreen = null, context = context)
    }

    @Test
    fun `logging a custom event is a no-op in all three shapes`() {
        AppAnalytics.logEvent(context, "custom_event")
        AppAnalytics.logEvent("custom_event")
        AppAnalytics.logEvent("custom_event", "key" to "value")
    }

    @Test
    fun `an event's parameters may be any of the types the bundle builder coerces`() {
        // The private bundle builder has an arm per type; a value it cannot coerce falls through
        // to `toString`. Passing one of each is the only way to reach them from outside.
        AppAnalytics.logEvent(
            "coercion",
            "string" to "text",
            "boolean" to true,
            "int" to 1,
            "long" to 2L,
            "float" to 3.0f,
            "double" to 4.0,
            "other" to listOf("something with no arm of its own"),
            "null" to null,
        )
    }

    @Test
    fun `a very long parameter value is truncated rather than rejected`() {
        // Firebase caps values at 100 characters; the builder takes the first 100 rather than
        // handing over a value the SDK will drop.
        AppAnalytics.logEvent("long_value", "text" to "x".repeat(500))
    }

    @Test
    fun `setting a user property is a no-op, and a long value does not throw`() {
        AppAnalytics.setUserProperty(AppAnalytics.UserProperty.EXACT_ALARM_ALLOWED, "true")
        AppAnalytics.setUserProperty(AppAnalytics.UserProperty.EXACT_ALARM_ALLOWED, "y".repeat(200))
        AppAnalytics.setUserProperty(AppAnalytics.UserProperty.EXACT_ALARM_ALLOWED, null)
    }

    // ---- The notification pipeline ----

    @Test
    fun `every notification-pipeline call is a no-op`() {
        AppAnalytics.logNotificationsScheduled(
            scheduledCount = 5,
            preRemindersEnabled = true,
            exactAlarmAllowed = false,
            postNotificationsGranted = true,
        )
        AppAnalytics.logNotificationsCancelled(reason = "user_disabled")
        AppAnalytics.logNotificationSuppressed(prayerType = "FAJR", reason = "silent")
        AppAnalytics.logNotificationOpened(source = "status_bar")
        AppAnalytics.logNotificationReschedule(trigger = "boot")
        AppAnalytics.logDailySummaryShown(prayedCount = 4, missedCount = 1)
        AppAnalytics.logTestNotification(allPrayers = true)
    }

    @Test
    fun `a notification displayed on time and one delayed by Doze are both no-ops`() {
        // The latency arm has a threshold in it, so both sides of it are worth reaching.
        AppAnalytics.logNotificationDisplayed(
            prayerType = "FAJR",
            isPreReminder = false,
            adhanPlayed = true,
            dndBlocked = false,
            deliveryLatencySeconds = 2,
        )
        AppAnalytics.logNotificationDisplayed(
            prayerType = "FAJR",
            isPreReminder = true,
            adhanPlayed = false,
            dndBlocked = true,
            deliveryLatencySeconds = 3_600,
        )
        // No latency at all — the `delayed` flag has to survive a null rather than assume late.
        AppAnalytics.logNotificationDisplayed(
            prayerType = "ISHA",
            isPreReminder = false,
            adhanPlayed = false,
            dndBlocked = false,
            deliveryLatencySeconds = null,
        )
    }

    // ---- Announcements ----

    @Test
    fun `every announcement call is a no-op, with or without a route`() {
        AppAnalytics.logAnnouncementShown(id = "a1", type = "update")
        AppAnalytics.logAnnouncementCtaClicked(id = "a1", route = "Route.Home")
        AppAnalytics.logAnnouncementCtaClicked(id = "a1", route = null)
        AppAnalytics.logAnnouncementDismissed(id = "a1")
        AppAnalytics.logAnnouncementRouteRejected(id = "a1", route = "nonsense")
        AppAnalytics.logAnnouncementRouteRejected(id = "a1", route = null)
    }

    // ---- Everything else ----

    @Test
    fun `the adhan, onboarding and start-up calls are no-ops`() {
        AppAnalytics.logAdhanFileMissing(adhanSound = "makkah", isFajr = true)
        AppAnalytics.logAdhanFileMissing(adhanSound = "makkah", isFajr = false)
        AppAnalytics.logOnboardingStep(page = 2)
        AppAnalytics.logOnboardingCompleted(
            locationGranted = true,
            notificationGranted = false,
            batteryOptimizationDisabled = true,
        )
        AppAnalytics.logAppInit(durationMs = 120, timedOut = false)
        AppAnalytics.logAppInit(durationMs = 10_000, timedOut = true)
    }

    @Test
    fun `error, feature and search logging are no-ops`() {
        AppAnalytics.logError(domain = "quran", type = "read_failed")
        AppAnalytics.logError(domain = "quran", type = "read_failed", message = "boom")
        AppAnalytics.logFeatureUsed(feature = "quran")
        AppAnalytics.logFeatureUsed(feature = "quran", action = "open_surah")
        AppAnalytics.logSearch(filter = "quran", queryLength = 6)
    }

    @Test
    fun `the tracker and settings calls are no-ops`() {
        AppAnalytics.logPrayerTracked(prayer = "FAJR", status = "PRAYED")
        AppAnalytics.logPrayerTracked(prayer = "FAJR", status = "PRAYED", isJamaah = true)
        AppAnalytics.logFastTracked(action = "completed")
        AppAnalytics.logFastTracked(action = "completed", fastType = "ramadan")
        AppAnalytics.logSettingChanged(setting = "calculation_method", value = "MWL")
    }

    @Test
    fun `the AI answer call is a no-op`() {
        AppAnalytics.logAiAnswered(proofCount = 3, durationMs = 900, confidence = "high")
    }

    @Test
    fun `collecting diagnostics is a no-op, with a context and without one`() {
        AppAnalytics.logDiagnostics(context)
        AppAnalytics.logDiagnostics(null)
    }

    // ---- The two calls that answer rather than record ----

    @Test
    fun `exact alarms are assumed allowed when there is nobody to ask`() {
        // Reporting every install as broken is worse than missing a genuinely broken one.
        assertThat(AppAnalytics.exactAlarmAllowed(null)).isTrue()
    }

    @Test
    fun `exact alarms are answered from the OS when there is a context`() {
        // Whatever Robolectric's AlarmManager says, the call must answer rather than throw.
        val answer = AppAnalytics.exactAlarmAllowed(context)

        assertThat(answer).isAnyOf(true, false)
    }

    @Test
    fun `posting notifications is assumed allowed when there is nobody to ask`() {
        assertThat(AppAnalytics.postNotificationsGranted(null)).isTrue()
    }

    @Test
    fun `posting notifications is answered from the OS when there is a context`() {
        assertThat(AppAnalytics.postNotificationsGranted(context)).isAnyOf(true, false)
    }
}
