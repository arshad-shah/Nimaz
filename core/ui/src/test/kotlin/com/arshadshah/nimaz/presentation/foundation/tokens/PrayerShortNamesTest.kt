package com.arshadshah.nimaz.presentation.foundation.tokens

import com.arshadshah.nimaz.core.ui.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * `prayerShortNameRes` resolves a prayer name to its translated short label.
 *
 * Split out of `:feature:widget`'s `WidgetPrayerNameTest` in PR 21 of #551, when the lookup moved
 * here: `WidgetsScreen`'s in-app preview needs it and left for `:feature:settings`, so two feature
 * modules read it and it came down to `:core:ui` — where the strings it resolves already lived.
 *
 * **Only the three cases that test this function came.** The four that stayed test
 * `prayerIconRes`, `weekdayInitials` and the no-English-literals sweep over the widget sources,
 * none of which moved. A test follows its subject, and a file that tests three subjects follows
 * three different ways.
 */
class PrayerShortNamesTest {

    @Test
    fun `every prayer resolves to its own translated short name`() {
        assertThat(prayerShortNameRes("Fajr")).isEqualTo(R.string.widget_prayer_short_fajr)
        assertThat(prayerShortNameRes("Sunrise")).isEqualTo(R.string.widget_prayer_short_sunrise)
        assertThat(prayerShortNameRes("Dhuhr")).isEqualTo(R.string.widget_prayer_short_dhuhr)
        assertThat(prayerShortNameRes("Asr")).isEqualTo(R.string.widget_prayer_short_asr)
        assertThat(prayerShortNameRes("Maghrib")).isEqualTo(R.string.widget_prayer_short_maghrib)
        assertThat(prayerShortNameRes("Isha")).isEqualTo(R.string.widget_prayer_short_isha)
    }

    @Test
    fun `the lookup accepts whatever case and spelling the callers pass`() {
        // Callers hand it `PrayerType.name` ("ISHA"), `PrayerType.displayName` ("Isha") and
        // bare literals, and the tracker's toggle round-trips through lowercase.
        listOf("ISHA", "isha", " Isha ").forEach {
            assertThat(prayerShortNameRes(it)).isEqualTo(R.string.widget_prayer_short_isha)
        }
        // Dhuhr is also written Zuhr, which `prayerIconRes` already accepts.
        assertThat(prayerShortNameRes("Zuhr")).isEqualTo(R.string.widget_prayer_short_dhuhr)
    }

    @Test
    fun `an unrecognised name resolves to nothing rather than a wrong prayer`() {
        // Falling back to a real prayer would label a row with someone else's name; the caller
        // shows the raw string instead.
        assertThat(prayerShortNameRes("Tahajjud")).isNull()
        assertThat(prayerShortNameRes("")).isNull()
    }
}
