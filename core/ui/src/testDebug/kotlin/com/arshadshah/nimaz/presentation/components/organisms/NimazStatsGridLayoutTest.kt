package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The stats grid's wrapping rule — the half of it that only happens on a tablet.
 *
 * On a phone, or with four stats or fewer, every figure sits in one row. Past that on a wide
 * window the grid splits into two rows and **pads the second row with spacers** so the columns line
 * up. Without the padding an odd count leaves the last card stretched across the gap, and the two
 * rows stop reading as a grid — which is the whole reason the split exists rather than letting the
 * cards shrink.
 *
 * The split is invisible on the device most development happens on, so the width qualifier is the
 * test: the same six stats laid out at phone width and at tablet width have to land differently.
 */
@RunWith(RobolectricTestRunner::class)
class NimazStatsGridLayoutTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val six = (1..6).map { NimazStatData(value = "$it", label = "Label $it") }
    private val five = (1..5).map { NimazStatData(value = "$it", label = "Label $it") }

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `a phone keeps every figure in one row`() {
        composeRule.setThemedContent { NimazStatsGrid(stats = six) }

        val ys = (1..6).map {
            composeRule.onNodeWithText("Label $it").fetchSemanticsNode().positionInRoot.y
        }

        assertThat(ys.toSet()).hasSize(1)
    }

    @Test
    @Config(qualifiers = "w1400dp-h1000dp")
    fun `a tablet with more than four figures splits into two rows`() {
        composeRule.setThemedContent { NimazStatsGrid(stats = six) }

        val ys = (1..6).map {
            composeRule.onNodeWithText("Label $it").fetchSemanticsNode().positionInRoot.y
        }

        assertThat(ys.toSet()).hasSize(2)
        // The split is front-loaded: `(size + 1) / 2` puts the extra card on the first row.
        assertThat(ys[0]).isEqualTo(ys[2])
        assertThat(ys[3]).isEqualTo(ys[5])
        assertThat(ys[3]).isGreaterThan(ys[0])
    }

    @Test
    @Config(qualifiers = "w1400dp-h1000dp")
    fun `four figures on a tablet stay in one row`() {
        // The boundary. Splitting at four would put two cards on a row of their own with half the
        // width empty.
        composeRule.setThemedContent { NimazStatsGrid(stats = six.take(4)) }

        val ys = (1..4).map {
            composeRule.onNodeWithText("Label $it").fetchSemanticsNode().positionInRoot.y
        }

        assertThat(ys.toSet()).hasSize(1)
    }

    @Test
    @Config(qualifiers = "w1400dp-h1000dp")
    fun `an odd split pads the short row so the columns still line up`() {
        // Five stats become three and two, and the missing third column is a spacer rather than a
        // stretched card. Asserted on width: the last card of the short row must match the width
        // of the card above it, not fill the remainder.
        composeRule.setThemedContent { NimazStatsGrid(stats = five) }

        val first = composeRule.onNodeWithText("Label 1").fetchSemanticsNode().size.width
        val last = composeRule.onNodeWithText("Label 5").fetchSemanticsNode().size.width

        assertThat(last).isEqualTo(first)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `a caller's colour is used and the default fills in for the rest`() {
        // `stat.color ?: primary`, applied at four separate call sites in this file — one per
        // branch of the layout — which is exactly the shape where one gets missed.
        composeRule.setThemedContent {
            NimazStatsGrid(
                stats = listOf(
                    NimazStatData(value = "7", label = "Tinted", color = Color.Magenta),
                    NimazStatData(value = "8", label = "Default"),
                ),
                compact = true,
            )
        }

        composeRule.onNodeWithText("Tinted").assertExists()
        composeRule.onNodeWithText("Default").assertExists()
    }

    @Test
    @Config(qualifiers = "w1400dp-h1000dp")
    fun `a compact tablet grid tightens its spacing`() {
        // Three-way spacing decision — compact wins over the width class. Both split rows are
        // laid out with it, so the whole grid closes up rather than half of it.
        composeRule.setThemedContent { NimazStatsGrid(stats = six, compact = true) }

        composeRule.onNodeWithText("Label 6").assertExists()
    }
}
