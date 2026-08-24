package com.arshadshah.nimaz.widget.nextprayer

import com.arshadshah.nimaz.widget.support.WidgetRenderer
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The next-prayer widget picks which prayer to name from the wall clock on every redraw, not from
 * whatever the worker decided up to fifteen minutes ago. These pin that choice, because getting it
 * wrong shows a prayer that has already started with no visible symptom other than being wrong.
 */
@RunWith(RobolectricTestRunner::class)
class NextPrayerWidgetRenderTest {

    private val widget = NextPrayerWidget()

    private fun scheduleAround(now: Long) = listOf(
        NextPrayerEntry("Fajr", "05:12", false, now - 60_000),
        NextPrayerEntry("Dhuhr", "13:04", false, now + 90 * 60_000),
        NextPrayerEntry("Asr", "16:40", false, now + 300 * 60_000),
    )

    @Test
    fun `the named prayer is the first one still ahead of the clock, not the worker's answer`() =
        runTest {
            val now = System.currentTimeMillis()
            val rendered = WidgetRenderer.render(
                widget,
                NextPrayerWidgetState.Success(
                    NextPrayerData(
                        // What the worker stored, already stale.
                        prayerName = "Fajr",
                        prayerTime = "05:12",
                        nextPrayerEpochMillis = now - 60_000,
                        schedule = scheduleAround(now),
                    ),
                ),
            )

            assertThat(rendered.hasText("13:04")).isTrue()
            assertThat(rendered.hasText("05:12")).isFalse()
            assertThat(rendered.contentDescriptions).contains("Dhuhr")
        }

    /**
     * State written by a release before `schedule` existed still has to render. The flat fields
     * are all such a payload has.
     */
    @Test
    fun `an empty schedule falls back to the flat fields the worker stored`() = runTest {
        val rendered = WidgetRenderer.render(
            widget,
            NextPrayerWidgetState.Success(
                NextPrayerData(
                    prayerName = "Maghrib",
                    prayerTime = "19:55",
                    countdown = "1h 20m",
                    nextPrayerEpochMillis = 0L,
                ),
            ),
        )

        assertThat(rendered.hasText("19:55")).isTrue()
        // With no instant to count down to, the stored countdown string is what shows.
        assertThat(rendered.hasText("1h 20m")).isTrue()
    }

    @Test
    fun `tomorrow's first prayer is captioned rather than given a clock time`() = runTest {
        val now = System.currentTimeMillis()
        val rendered = WidgetRenderer.render(
            widget,
            NextPrayerWidgetState.Success(
                NextPrayerData(
                    schedule = listOf(
                        NextPrayerEntry("Fajr", "05:12", true, now + 6 * 3_600_000),
                    ),
                ),
            ),
        )

        assertThat(rendered.hasText("05:12")).isFalse()
        assertThat(rendered.containsText("omorrow")).isTrue()
    }

    /**
     * "in" is prefixed only when there is a real countdown to prefix. An invalid payload used to
     * read "in —".
     */
    @Test
    fun `an invalid payload drops the in prefix instead of writing in em dash`() = runTest {
        val rendered = WidgetRenderer.render(
            widget,
            NextPrayerWidgetState.Success(NextPrayerData(isValid = false)),
        )

        assertThat(rendered.hasText("in ")).isFalse()
        assertThat(rendered.hasText("—")).isTrue()
    }

    @Test
    fun `loading and error draw frames that carry none of the prayer data`() = runTest {
        val loading = WidgetRenderer.render(widget, NextPrayerWidgetState.Loading)
        val error = WidgetRenderer.render(widget, NextPrayerWidgetState.Error("no location"))

        assertThat(loading.texts).hasSize(1)
        assertThat(error.texts).hasSize(1)
        // The error frame says "tap to set up", which is a different string from loading.
        assertThat(error.texts).isNotEqualTo(loading.texts)
    }

    @Test
    fun `nextEntry picks by clock and falls back when nothing is ahead`() {
        val now = 1_000_000L
        val data = NextPrayerData(
            prayerName = "Isha",
            nextPrayerEpochMillis = 42L,
            schedule = listOf(
                NextPrayerEntry("Fajr", "05:12", false, now - 1),
                NextPrayerEntry("Dhuhr", "13:04", false, now + 1),
            ),
        )

        assertThat(data.nextEntry(now).prayerName).isEqualTo("Dhuhr")
        // Every scheduled prayer already passed — fall back to the flat fields.
        assertThat(data.nextEntry(now + 10).prayerName).isEqualTo("Isha")
    }

    @Test
    fun `a payload counts as data worth keeping once it has a schedule or an instant`() {
        assertThat(NextPrayerWidgetState.Success(NextPrayerData()).hasData).isFalse()
        assertThat(
            NextPrayerWidgetState.Success(NextPrayerData(nextPrayerEpochMillis = 1L)).hasData,
        ).isTrue()
        assertThat(
            NextPrayerWidgetState.Success(
                NextPrayerData(schedule = listOf(NextPrayerEntry())),
            ).hasData,
        ).isTrue()
        assertThat(NextPrayerWidgetState.Loading.hasData).isFalse()
        assertThat(NextPrayerWidgetState.Error("x").hasData).isFalse()
    }
}
