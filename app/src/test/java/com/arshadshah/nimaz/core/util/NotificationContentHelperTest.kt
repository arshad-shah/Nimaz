package com.arshadshah.nimaz.core.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NotificationContentHelperTest {

    // ── getPrayerTitle ──────────────────────────────────────────────

    @Test
    fun `getPrayerTitle returns non-empty string for all prayers`() {
        val prayers = listOf("FAJR", "SUNRISE", "DHUHR", "ASR", "MAGHRIB", "ISHA")
        for (prayer in prayers) {
            val title = NotificationContentHelper.getPrayerTitle(prayer)
            assertThat(title).isNotEmpty()
            // Title is randomly selected and may not always contain the prayer name literally
            // (e.g. "The Morning Prayer Awaits" for Fajr), so just check non-empty
        }
    }

    @Test
    fun `getPrayerTitle is case insensitive`() {
        // Titles are picked at random from a prayer-specific list, so the two
        // calls can't be compared directly and won't necessarily contain "fajr"
        // (e.g. "The Morning Prayer Awaits"). Case-insensitivity means "fajr" is
        // recognised the same as "FAJR" and never falls through to the
        // "<name> Time" fallback that getPrayerTitle returns for unknown prayers.
        val titleUpper = NotificationContentHelper.getPrayerTitle("FAJR")
        val titleLower = NotificationContentHelper.getPrayerTitle("fajr")
        assertThat(titleUpper).isNotEqualTo("FAJR Time")
        assertThat(titleLower).isNotEqualTo("fajr Time")
    }

    @Test
    fun `getPrayerTitle returns fallback for unknown prayer`() {
        val title = NotificationContentHelper.getPrayerTitle("UNKNOWN")
        assertThat(title).isEqualTo("UNKNOWN Time")
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
        assertThat(message).isEqualTo("It's time for TAHAJJUD prayer.")
    }

    // ── getShortMessage ─────────────────────────────────────────────

    @Test
    fun `getShortMessage returns non-empty string for all prayers`() {
        val prayers = listOf("FAJR", "SUNRISE", "DHUHR", "ASR", "MAGHRIB", "ISHA")
        for (prayer in prayers) {
            val message = NotificationContentHelper.getShortMessage(prayer)
            assertThat(message).isNotEmpty()
        }
    }

    @Test
    fun `getShortMessage returns deterministic values`() {
        // Short messages are not randomized - they should be stable
        val first = NotificationContentHelper.getShortMessage("FAJR")
        val second = NotificationContentHelper.getShortMessage("FAJR")
        assertThat(first).isEqualTo(second)
    }

    @Test
    fun `getShortMessage returns fallback for unknown prayer`() {
        val message = NotificationContentHelper.getShortMessage("WITR")
        assertThat(message).isEqualTo("It's time for WITR prayer.")
    }

    // ── getPreReminderTitle / getPreReminderMessage ─────────────────

    @Test
    fun `getPreReminderTitle includes prayer name and minutes`() {
        val title = NotificationContentHelper.getPreReminderTitle("Fajr", 15)
        assertThat(title).isEqualTo("Fajr in 15 minutes")
    }

    @Test
    fun `getPreReminderMessage returns non-empty string`() {
        val message = NotificationContentHelper.getPreReminderMessage("Fajr")
        assertThat(message).isNotEmpty()
    }

    // ── getPrayerEmoji ──────────────────────────────────────────────

    @Test
    fun `getPrayerEmoji returns correct emoji for each prayer`() {
        assertThat(NotificationContentHelper.getPrayerEmoji("FAJR")).isEqualTo("🌅")
        assertThat(NotificationContentHelper.getPrayerEmoji("SUNRISE")).isEqualTo("☀️")
        assertThat(NotificationContentHelper.getPrayerEmoji("DHUHR")).isEqualTo("🕐")
        assertThat(NotificationContentHelper.getPrayerEmoji("ASR")).isEqualTo("🌤️")
        assertThat(NotificationContentHelper.getPrayerEmoji("MAGHRIB")).isEqualTo("🌅")
        assertThat(NotificationContentHelper.getPrayerEmoji("ISHA")).isEqualTo("🌙")
    }

    @Test
    fun `getPrayerEmoji returns mosque emoji for unknown prayer`() {
        assertThat(NotificationContentHelper.getPrayerEmoji("UNKNOWN")).isEqualTo("🕌")
    }

    // ── getDailySummaryContent ───────────────────────────────────────

    @Test
    fun `getDailySummaryContent all prayers completed is positive`() {
        val summary = NotificationContentHelper.getDailySummaryContent(
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
            prayedCount = 0,
            missedCount = 5,
            missedPrayers = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
        )
        assertThat(summary.isPositive).isFalse()
    }

    @Test
    fun `getDailySummaryContent some prayers missed shows count`() {
        val summary = NotificationContentHelper.getDailySummaryContent(
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
            prayedCount = 1,
            missedCount = 4,
            missedPrayers = listOf("Fajr", "Dhuhr", "Asr", "Maghrib")
        )
        assertThat(summary.isPositive).isFalse()
    }

    @Test
    fun `getDailySummaryContent bigText includes missed prayer names`() {
        val summary = NotificationContentHelper.getDailySummaryContent(
            prayedCount = 3,
            missedCount = 2,
            missedPrayers = listOf("Fajr", "Isha")
        )
        assertThat(summary.bigText).contains("Fajr")
        assertThat(summary.bigText).contains("Isha")
    }
}
