package com.arshadshah.nimaz.core.util

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class NotificationContentHelperTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    // ── getPrayerTitle ──────────────────────────────────────────────

    @Test
    fun `getPrayerTitle returns the prayer name`() {
        assertThat(NotificationContentHelper.getPrayerTitle("FAJR")).isEqualTo("Fajr")
        assertThat(NotificationContentHelper.getPrayerTitle("ISHA")).isEqualTo("Isha")
    }

    @Test
    fun `getPrayerTitle appends the time when provided`() {
        val title = NotificationContentHelper.getPrayerTitle("FAJR", "5:30 AM")
        assertThat(title).isEqualTo("Fajr · 5:30 AM")
    }

    @Test
    fun `getPrayerTitle is case insensitive`() {
        assertThat(NotificationContentHelper.getPrayerTitle("fajr")).isEqualTo("Fajr")
    }

    @Test
    fun `getPrayerTitle title-cases an unknown prayer`() {
        val title = NotificationContentHelper.getPrayerTitle("UNKNOWN")
        assertThat(title).isEqualTo("Unknown")
    }

    // ── getPrayerMessage ────────────────────────────────────────────

    @Test
    fun `getPrayerMessage returns non-empty string for all prayers`() {
        val prayers = listOf("FAJR", "SUNRISE", "DHUHR", "ASR", "MAGHRIB", "ISHA")
        for (prayer in prayers) {
            val message = NotificationContentHelper.getPrayerMessage(prayer)
            assertThat(message).isNotEmpty()
        }
    }

    @Test
    fun `getPrayerMessage returns fallback for unknown prayer`() {
        val message = NotificationContentHelper.getPrayerMessage("TAHAJJUD")
        assertThat(message).isEqualTo("It's time for Tahajjud prayer.")
    }

    // ── getShortMessage ─────────────────────────────────────────────

    @Test
    fun `getShortMessage returns non-empty string for all prayers`() {
        val prayers = listOf("FAJR", "SUNRISE", "DHUHR", "ASR", "MAGHRIB", "ISHA")
        for (prayer in prayers) {
            val message = NotificationContentHelper.getShortMessage(context, prayer)
            assertThat(message).isNotEmpty()
        }
    }

    @Test
    fun `getShortMessage returns deterministic values`() {
        // Short messages are not randomized - they should be stable
        val first = NotificationContentHelper.getShortMessage(context, "FAJR")
        val second = NotificationContentHelper.getShortMessage(context, "FAJR")
        assertThat(first).isEqualTo(second)
    }

    @Test
    fun `getShortMessage returns fallback for unknown prayer`() {
        val message = NotificationContentHelper.getShortMessage(context, "WITR")
        assertThat(message).isEqualTo("It's time for Witr prayer.")
    }

    // ── getPreReminderTitle / getPreReminderMessage ─────────────────

    @Test
    fun `getPreReminderTitle includes prayer name and minutes`() {
        val title = NotificationContentHelper.getPreReminderTitle(context, "Fajr", 15)
        assertThat(title).isEqualTo("Fajr in 15 minutes")
    }

    @Test
    fun `getPreReminderMessage returns non-empty string`() {
        val message = NotificationContentHelper.getPreReminderMessage(context, "Fajr")
        assertThat(message).isNotEmpty()
    }

    // ── getDailySummaryContent ───────────────────────────────────────

    @Test
    fun `getDailySummaryContent all prayers completed is positive`() {
        val summary = NotificationContentHelper.getDailySummaryContent(
            context = context,
            prayedCount = 5,
            missedCount = 0,
            missedPrayers = emptyList()
        )
        assertThat(summary.isPositive).isTrue()
        assertThat(summary.title).containsMatch("(?i)all.*complete")
    }

    @Test
    fun `getDailySummaryContent all prayers missed is not positive`() {
        val summary = NotificationContentHelper.getDailySummaryContent(
            context = context,
            prayedCount = 0,
            missedCount = 5,
            missedPrayers = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
        )
        assertThat(summary.isPositive).isFalse()
    }

    @Test
    fun `getDailySummaryContent some prayers missed shows count`() {
        val summary = NotificationContentHelper.getDailySummaryContent(
            context = context,
            prayedCount = 3,
            missedCount = 2,
            missedPrayers = listOf("Fajr", "Isha")
        )
        assertThat(summary.isPositive).isTrue() // 3 > 2
        assertThat(summary.message).contains("3 of 5")
    }

    @Test
    fun `getDailySummaryContent more missed than prayed is not positive`() {
        val summary = NotificationContentHelper.getDailySummaryContent(
            context = context,
            prayedCount = 1,
            missedCount = 4,
            missedPrayers = listOf("Fajr", "Dhuhr", "Asr", "Maghrib")
        )
        assertThat(summary.isPositive).isFalse()
    }

    @Test
    fun `getDailySummaryContent bigText includes missed prayer names`() {
        val summary = NotificationContentHelper.getDailySummaryContent(
            context = context,
            prayedCount = 3,
            missedCount = 2,
            missedPrayers = listOf("Fajr", "Isha")
        )
        assertThat(summary.bigText).contains("Fajr")
        assertThat(summary.bigText).contains("Isha")
    }

    // ── getTimeBasedGreeting ────────────────────────────────────────

    /**
     * All five arms, at a fixed hour each.
     *
     * The hour used to be read inside the function, so exactly one arm ran per test run and
     * *which* one depended on when CI happened to start. That made the module's branch coverage
     * drift through the day: #638 measured 80.8% against an 80% floor at midday and 79.2% that
     * evening, on identical code. These pin the boundaries as well as the bands, because the
     * comparisons are all `<` and an off-by-one at 06:00 or 20:00 is invisible otherwise.
     */
    @Test
    fun `getTimeBasedGreeting picks a greeting per band, at every boundary`() {
        val predawn = greeting(0)
        val morning = greeting(6)
        val afternoon = greeting(12)
        val evening = greeting(17)
        val night = greeting(20)

        // Five distinct strings, or the bands are not doing anything.
        assertThat(setOf(predawn, morning, afternoon, evening, night)).hasSize(5)

        // The last hour of each band still belongs to it — `hour < 6`, not `<= 6`.
        assertThat(greeting(5)).isEqualTo(predawn)
        assertThat(greeting(11)).isEqualTo(morning)
        assertThat(greeting(16)).isEqualTo(afternoon)
        assertThat(greeting(19)).isEqualTo(evening)
        assertThat(greeting(23)).isEqualTo(night)
    }

    @Test
    fun `getTimeBasedGreeting defaults to the wall clock`() {
        // The production caller passes no hour. Whatever hour it is, the default has to land in
        // one of the five bands rather than returning something empty.
        val now = java.time.LocalTime.now().hour

        assertThat(NotificationContentHelper.getTimeBasedGreeting(context))
            .isEqualTo(greeting(now))
    }

    private fun greeting(hour: Int) =
        NotificationContentHelper.getTimeBasedGreeting(context, hour)
}
