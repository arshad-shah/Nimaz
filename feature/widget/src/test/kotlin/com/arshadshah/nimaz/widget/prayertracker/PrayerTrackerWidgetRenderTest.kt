package com.arshadshah.nimaz.widget.prayertracker

import com.arshadshah.nimaz.widget.support.WidgetRenderer
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The five tick-boxes and the counter above them.
 *
 * Each tile is a tap target that writes to the database, so a tile that renders the wrong state
 * is worse than a cosmetic bug — it tells the reader they have not prayed something they have.
 */
@RunWith(RobolectricTestRunner::class)
class PrayerTrackerWidgetRenderTest {

    private val widget = PrayerTrackerWidget()

    @Test
    fun `every prayer is labelled and the counter reports the tally`() = runTest {
        val rendered = WidgetRenderer.render(
            widget,
            PrayerTrackerWidgetState.Success(
                PrayerTrackerData(
                    dateLabel = "Mon 24 Aug",
                    fajr = true,
                    dhuhr = true,
                    asr = false,
                    maghrib = false,
                    isha = false,
                    prayedCount = 2,
                ),
            ),
        )

        assertThat(rendered.hasText("Mon 24 Aug")).isTrue()
        assertThat(rendered.hasText("2 / 5")).isTrue()
        listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha").forEach {
            assertThat(rendered.hasText(it)).isTrue()
        }
    }

    /**
     * A prayed tile draws a check vector; an unprayed one is a two-disc ring with no icon. The
     * tiles carry no text that distinguishes them, so the count of drawn icons is what says how
     * many read as prayed — and that is the whole point of the widget.
     */
    @Test
    fun `exactly one check mark is drawn per prayed prayer`() = runTest {
        suspend fun iconsFor(vararg prayed: Boolean): Int = WidgetRenderer.render(
            widget,
            PrayerTrackerWidgetState.Success(
                PrayerTrackerData(
                    dateLabel = "Mon",
                    fajr = prayed[0],
                    dhuhr = prayed[1],
                    asr = prayed[2],
                    maghrib = prayed[3],
                    isha = prayed[4],
                    prayedCount = prayed.count { it },
                ),
            ),
        ).imageCount

        // The card's own background art is drawn either way, so the baseline is the offset.
        val baseline = iconsFor(false, false, false, false, false)
        assertThat(iconsFor(true, false, false, false, false)).isEqualTo(baseline + 1)
        assertThat(iconsFor(true, true, false, false, false)).isEqualTo(baseline + 2)
        assertThat(iconsFor(true, true, true, true, true)).isEqualTo(baseline + 5)
    }

    @Test
    fun `the counter tracks the tally independently of the tiles`() = runTest {
        val rendered = WidgetRenderer.render(
            widget,
            PrayerTrackerWidgetState.Success(
                PrayerTrackerData(dateLabel = "Mon", fajr = true, prayedCount = 1),
            ),
        )

        assertThat(rendered.hasText("1 / 5")).isTrue()
    }

    @Test
    fun `the error frame offers a retry line as well as the failure line`() = runTest {
        val rendered = WidgetRenderer.render(
            widget,
            PrayerTrackerWidgetState.Error("db locked"),
        )

        assertThat(rendered.texts).hasSize(2)
        assertThat(rendered.hasText("Fajr")).isFalse()
    }

    @Test
    fun `loading draws neither the tiles nor the counter`() = runTest {
        val rendered = WidgetRenderer.render(widget, PrayerTrackerWidgetState.Loading)

        assertThat(rendered.hasText("0 / 5")).isFalse()
        assertThat(rendered.texts).hasSize(1)
    }

    /**
     * A day with nothing prayed yet is still a real reading — the date label is what says a load
     * completed, not the count.
     */
    @Test
    fun `the date label decides whether a reading survives a failed refresh`() {
        assertThat(
            PrayerTrackerWidgetState.Success(PrayerTrackerData(dateLabel = "Mon")).hasData,
        ).isTrue()
        assertThat(PrayerTrackerWidgetState.Success(PrayerTrackerData()).hasData).isFalse()
        assertThat(PrayerTrackerWidgetState.Loading.hasData).isFalse()
        assertThat(PrayerTrackerWidgetState.Error(null).hasData).isFalse()
    }
}
