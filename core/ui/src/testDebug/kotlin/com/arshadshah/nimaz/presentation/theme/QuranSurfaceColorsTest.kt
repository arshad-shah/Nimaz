package com.arshadshah.nimaz.presentation.theme

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// A ComposeContentTestRule (createComposeRule()) hosts its content on a single
// Activity and only allows one setContent call per test method — several of
// these tests need to compose the tokens twice (once per theme) to compare
// them, so each call gets its own isolated runComposeUiTest environment
// instead of sharing one Activity-backed rule.
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
class QuranSurfaceColorsTest {

    private fun paperRoles(dark: Boolean): Triple<Color, Color, Color> {
        lateinit var roles: Triple<Color, Color, Color>
        runComposeUiTest {
            setContent {
                CompositionLocalProvider(LocalIsDarkTheme provides dark) {
                    roles = Triple(
                        QuranSurfaceColors.paper,
                        QuranSurfaceColors.paperLine,
                        QuranSurfaceColors.paperInk,
                    )
                }
            }
        }
        return roles
    }

    @Test
    fun `paper roles differ between light and dark`() {
        val light = paperRoles(dark = false)
        val dark = paperRoles(dark = true)
        assertThat(light).isNotEqualTo(dark)
    }

    @Test
    fun `light paper is a warm ground, not pure white`() {
        val (paper, _, _) = paperRoles(dark = false)
        assertThat(paper).isNotEqualTo(Color.White)
        // Warm: red channel exceeds blue.
        assertThat(paper.red).isGreaterThan(paper.blue)
    }

    @Test
    fun `paper ink contrasts with its own ground in both themes`() {
        listOf(false, true).forEach { dark ->
            val (paper, _, ink) = paperRoles(dark = dark)
            assertThat(contrastRatio(paper, ink)).isAtLeast(4.5)
        }
    }

    @Test
    fun `paper line is distinguishable from paper in both themes`() {
        listOf(false, true).forEach { dark ->
            val (paper, line, _) = paperRoles(dark = dark)
            // A decorative hairline, not a UI component: it needs to be *visible*
            // against the page, not to meet the 3:1 identification bar. The value
            // is the approved prototype's; the ratio is what that value gives.
            assertThat(contrastRatio(paper, line)).isGreaterThan(1.2)
            assertThat(line).isNotEqualTo(paper)
        }
    }

    /** WCAG relative-luminance contrast ratio. */
    private fun contrastRatio(a: Color, b: Color): Double {
        fun channel(c: Float): Double {
            val v = c.toDouble()
            return if (v <= 0.03928) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4)
        }
        fun luminance(c: Color) =
            0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)
        val la = luminance(a)
        val lb = luminance(b)
        val lighter = maxOf(la, lb)
        val darker = minOf(la, lb)
        return (lighter + 0.05) / (darker + 0.05)
    }
}
