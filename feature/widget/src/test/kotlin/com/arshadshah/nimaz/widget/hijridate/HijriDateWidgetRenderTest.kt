package com.arshadshah.nimaz.widget.hijridate

import com.arshadshah.nimaz.widget.support.WidgetRenderer
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * What the Hijri date widget actually draws, per state.
 *
 * A widget that throws while composing does not crash anything a user would report — the launcher
 * keeps the last frame on screen and the date silently stops advancing. These render the real
 * `provideGlance` so that failure is a red test instead.
 */
@RunWith(RobolectricTestRunner::class)
class HijriDateWidgetRenderTest {

    private val widget = HijriDateWidget()

    private val loaded = HijriDateData(
        hijriDay = 17,
        hijriMonth = "Ramadan",
        hijriYear = 1447,
        gregorianDayOfWeek = "Monday",
        gregorianDate = "24 Aug 2026",
    )

    @Test
    fun `success draws the day, the month with its year, and both gregorian lines`() = runTest {
        val rendered = WidgetRenderer.render(widget, HijriDateWidgetState.Success(loaded))

        assertThat(rendered.hasText("17")).isTrue()
        assertThat(rendered.hasText("Ramadan 1447")).isTrue()
        assertThat(rendered.hasText("Monday")).isTrue()
        assertThat(rendered.hasText("24 Aug 2026")).isTrue()
    }

    /**
     * The default state every install starts on is `Success` with an empty payload, so this is
     * the frame a user sees before the first worker run — not a hypothetical.
     */
    @Test
    fun `an empty payload falls back to em dashes rather than blank lines`() = runTest {
        val rendered = WidgetRenderer.render(
            widget,
            HijriDateWidgetState.Success(HijriDateData()),
        )

        // Day-of-week, month and gregorian date each fall back independently.
        assertThat(rendered.texts.count { it == "—" }).isEqualTo(2)
        assertThat(rendered.hasText("— 1446")).isTrue()
    }

    @Test
    fun `loading draws the shared loading caption`() = runTest {
        val rendered = WidgetRenderer.render(widget, HijriDateWidgetState.Loading)

        assertThat(rendered.texts).isNotEmpty()
        assertThat(rendered.hasText("Ramadan 1447")).isFalse()
    }

    @Test
    fun `error draws the tap-to-refresh frame and none of the date`() = runTest {
        val rendered = WidgetRenderer.render(widget, HijriDateWidgetState.Error("boom"))

        assertThat(rendered.hasText("17")).isFalse()
        assertThat(rendered.texts).hasSize(1)
    }

    /**
     * `hasData` is what decides whether a failed refresh keeps the last good reading or replaces
     * it with the error frame (`refreshWidget`). Getting it wrong wipes a correct widget on one
     * transient throw.
     */
    @Test
    fun `only a payload carrying a month counts as data worth keeping`() {
        assertThat(HijriDateWidgetState.Success(loaded).hasData).isTrue()
        assertThat(HijriDateWidgetState.Success(HijriDateData()).hasData).isFalse()
        assertThat(HijriDateWidgetState.Loading.hasData).isFalse()
        assertThat(HijriDateWidgetState.Error(null).hasData).isFalse()
    }
}
