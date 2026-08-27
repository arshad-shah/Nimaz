package com.arshadshah.nimaz.widget.prayertimes

import com.arshadshah.nimaz.widget.support.WidgetRenderer
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The five-prayer strip: which cell is highlighted, and what the header line says.
 *
 * The highlight is derived from the wall clock at render time rather than from the refresh
 * worker, so these fix a time relative to `now` and assert the strip agrees.
 */
@RunWith(RobolectricTestRunner::class)
class PrayerTimesWidgetRenderTest {

    private val widget = PrayerTimesWidget()

    /** Fajr and Dhuhr behind us, Asr next, Maghrib and Isha ahead. */
    private fun midAfternoon(now: Long = System.currentTimeMillis()) = PrayerTimesData(
        locationName = "Dublin",
        hijriDate = "17 Ramadan",
        fajrTime = "05:12",
        dhuhrTime = "13:04",
        asrTime = "16:40",
        maghribTime = "19:55",
        ishaTime = "21:30",
        fajrEpochMillis = now - 8 * 3_600_000,
        dhuhrEpochMillis = now - 3_600_000,
        asrEpochMillis = now + 90 * 60_000,
        maghribEpochMillis = now + 5 * 3_600_000,
        ishaEpochMillis = now + 7 * 3_600_000,
    )

    @Test
    fun `all five prayers and the location are on screen`() = runTest {
        val rendered = WidgetRenderer.render(
            widget,
            PrayerTimesWidgetState.Success(midAfternoon()),
        )

        assertThat(rendered.hasText("Dublin")).isTrue()
        listOf("05:12", "13:04", "16:40", "19:55", "21:30").forEach {
            assertThat(rendered.hasText(it)).isTrue()
        }
        // The strip uses the *short* names, which are not simply the display names truncated —
        // Maghrib is "Mgrb". A test asserting on "Maghrib" here would be asserting on the wrong
        // contract.
        listOf("Fajr", "Dhuhr", "Asr", "Mgrb", "Isha").forEach {
            assertThat(rendered.hasText(it)).isTrue()
        }
    }

    /**
     * The header's right-hand line names the *next* prayer and its countdown. Naming the wrong
     * one is the exact staleness this widget derives live to avoid.
     */
    @Test
    fun `the header names the next prayer beside the hijri date`() = runTest {
        val rendered = WidgetRenderer.render(
            widget,
            PrayerTimesWidgetState.Success(midAfternoon()),
        )

        val header = rendered.texts.single { it.startsWith("17 Ramadan") }
        assertThat(header).contains("Asr in ")
        assertThat(header).doesNotContain("Fajr in")
    }

    /**
     * After Isha nothing is ahead, so there is no next prayer to name and no countdown to add —
     * the header falls back to the Hijri date alone rather than writing "in —".
     */
    @Test
    fun `once every prayer has passed the header drops the countdown clause`() = runTest {
        val now = System.currentTimeMillis()
        val rendered = WidgetRenderer.render(
            widget,
            PrayerTimesWidgetState.Success(
                midAfternoon(now).copy(
                    asrEpochMillis = now - 3 * 3_600_000,
                    maghribEpochMillis = now - 2 * 3_600_000,
                    ishaEpochMillis = now - 3_600_000,
                ),
            ),
        )

        assertThat(rendered.hasText("17 Ramadan")).isTrue()
        assertThat(rendered.containsText(" in ")).isFalse()
    }

    /**
     * With no Hijri date the countdown clause is all there is, and it must not be prefixed by a
     * dangling separator.
     */
    @Test
    fun `without a hijri date the header is the countdown alone, with no separator`() = runTest {
        val rendered = WidgetRenderer.render(
            widget,
            PrayerTimesWidgetState.Success(midAfternoon().copy(hijriDate = "")),
        )

        val header = rendered.texts.single { it.contains(" in ") }
        assertThat(header).startsWith("Asr in ")
        assertThat(header).doesNotContain("·")
    }

    /** Nothing loaded yet: no location, no hijri date, no instants. */
    @Test
    fun `an empty payload draws the location placeholder and em-dash times`() = runTest {
        val rendered = WidgetRenderer.render(
            widget,
            PrayerTimesWidgetState.Success(PrayerTimesData()),
        )

        assertThat(rendered.texts.count { it == "—" }).isAtLeast(5)
        assertThat(rendered.hasText("Dublin")).isFalse()
    }

    @Test
    fun `loading and error draw single-line frames`() = runTest {
        assertThat(
            WidgetRenderer.render(widget, PrayerTimesWidgetState.Loading).texts,
        ).hasSize(1)
        assertThat(
            WidgetRenderer.render(widget, PrayerTimesWidgetState.Error("no times")).texts,
        ).hasSize(1)
    }

    @Test
    fun `only a payload with a real Fajr instant is worth keeping through a failed refresh`() {
        assertThat(PrayerTimesWidgetState.Success(midAfternoon()).hasData).isTrue()
        assertThat(PrayerTimesWidgetState.Success(PrayerTimesData()).hasData).isFalse()
        assertThat(PrayerTimesWidgetState.Loading.hasData).isFalse()
        assertThat(PrayerTimesWidgetState.Error(null).hasData).isFalse()
    }
}
