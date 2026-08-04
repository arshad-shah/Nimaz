package com.arshadshah.nimaz.presentation.components.molecules

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Pins the relationship between the reader's Arabic font size preference and what a
 * line-accurate (IndoPak) page actually renders at.
 *
 * The renderer used to shrink each line independently until it fit the page width, starting
 * from the requested size. Since the densest line of a 16-line page does not fit at *any*
 * value the slider offers, every value collapsed onto the same width-determined size: the
 * preference moved the Madani reader and did nothing at all on the IndoPak editions. These
 * cases are the fix stated as arithmetic — the default reproduces the fit, and everything
 * else scales it.
 */
class MushafLinePageFitTest {

    // A page whose densest line is 50% wider than the viewport at the reference size, so it
    // has to be drawn at 2/3 of the reference (28sp → 18.67sp) to fit.
    private val viewportPx = 1000
    private val widestLinePx = 1500

    @Test
    fun `the default preference renders the fit-to-width size`() {
        assertThat(pageFitFontSize(28f, widestLinePx, viewportPx)).isWithin(0.01f).of(18.667f)
    }

    @Test
    fun `a smaller preference scales the page down proportionally`() {
        // Three quarters of the default preference draws the page at three quarters of the
        // fitted size.
        assertThat(pageFitFontSize(21f, widestLinePx, viewportPx)).isWithin(0.01f).of(14f)
        assertThat(pageFitFontSize(18f, widestLinePx, viewportPx)).isWithin(0.01f).of(12f)
    }

    @Test
    fun `a larger preference actually enlarges the text`() {
        // The regression this guards: 42sp used to render identically to 28sp and to 18sp.
        val default = pageFitFontSize(28f, widestLinePx, viewportPx)
        val largest = pageFitFontSize(42f, widestLinePx, viewportPx)
        assertThat(largest).isGreaterThan(default)
        assertThat(largest).isWithin(0.01f).of(28f)
    }

    @Test
    fun `every step of the settings slider maps to a distinct size`() {
        val sizes = (18..42).map { pageFitFontSize(it.toFloat(), widestLinePx, viewportPx) }
        assertThat(sizes.toSet()).hasSize(sizes.size)
        assertThat(sizes).isInOrder()
    }

    @Test
    fun `a page that already fits is not blown up to fill the width`() {
        // Densest line narrower than the viewport: the page is drawn at the requested size,
        // not stretched to the width it could occupy.
        assertThat(pageFitFontSize(28f, widestLinePx = 500, viewportPx = 1000))
            .isWithin(0.01f).of(28f)
    }

    @Test
    fun `an unmeasured page falls back to the requested size`() {
        // Before the first measure pass, and on a page with no ayah lines at all.
        assertThat(pageFitFontSize(33f, widestLinePx = 0, viewportPx = 1000)).isEqualTo(33f)
        assertThat(pageFitFontSize(33f, widestLinePx = 1500, viewportPx = 0)).isEqualTo(33f)
    }

    @Test
    fun `a very dense page on a narrow screen stays above the legibility floor`() {
        assertThat(pageFitFontSize(18f, widestLinePx = 20_000, viewportPx = 300))
            .isAtLeast(10f)
    }
}
