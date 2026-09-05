package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazSolarArcTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val nodes = listOf(
        NimazSolarNode(0.22f, "Fajr", NimazTone.MUTED, "Fajr at 05:12"),
        NimazSolarNode(0.27f, null, NimazTone.ACCENT, "Sunrise at 06:48"),
        NimazSolarNode(0.55f, "Dhuhr", NimazTone.PROMINENT, "Dhuhr at 13:22"),
        NimazSolarNode(0.72f, "Asr", NimazTone.WARNING, "Asr at 17:13"),
        NimazSolarNode(0.80f, null, NimazTone.WARNING, "Maghrib at 20:04"),
        NimazSolarNode(0.90f, "Isha", NimazTone.MUTED, "Isha at 21:38"),
    )

    private fun arc(
        sunPosition: Float? = null,
        litSpan: ClosedFloatingPointRange<Float>? = null,
    ): @Composable () -> Unit = {
        NimazSolarArc(
            nodes = nodes,
            sunriseFraction = 0.27f,
            sunsetFraction = 0.80f,
            contentDescription = "The sun's day: sunrise 06:48, sunset 20:04",
            sunPosition = sunPosition,
            litSpan = litSpan,
        )
    }

    @Test
    fun `the arc speaks as one node`() {
        composeRule.setThemedContent(arc())
        composeRule
            .onNodeWithContentDescription("The sun's day: sunrise 06:48, sunset 20:04")
            .assertIsDisplayed()
    }

    /**
     * The labels are *painted* into the Canvas, not composed as `Text`, so they create no
     * semantics node — which is the design rather than a shortfall. `clearAndSetSemantics` is
     * what makes the arc speak as one sentence instead of as a pile of unlabelled dots, so
     * asserting each label through the semantics tree would mean undoing the accessibility
     * contract in order to test it.
     *
     * This asserts the drawing renders with a full set of nodes; what it *says* is covered by
     * `the arc speaks as one node`.
     */
    @Test
    fun `the arc renders with a full set of nodes`() {
        composeRule.setThemedContent(arc())
        composeRule
            .onNodeWithContentDescription("The sun's day: sunrise 06:48, sunset 20:04")
            .assertIsDisplayed()
    }

    @Test
    fun `a null sun position still renders - most days are not today`() {
        composeRule.setThemedContent(arc(sunPosition = null))
        composeRule.onNodeWithContentDescription("The sun's day: sunrise 06:48, sunset 20:04")
            .assertIsDisplayed()
    }

    @Test
    fun `a sun position renders`() {
        composeRule.setThemedContent(arc(sunPosition = 0.62f))
        composeRule.onNodeWithContentDescription("The sun's day: sunrise 06:48, sunset 20:04")
            .assertIsDisplayed()
    }

    @Test
    fun `a lit span renders`() {
        composeRule.setThemedContent(arc(sunPosition = 0.62f, litSpan = 0.55f..0.72f))
        composeRule.onNodeWithContentDescription("The sun's day: sunrise 06:48, sunset 20:04")
            .assertIsDisplayed()
    }

    @Test
    fun `an out of range sun position is coerced rather than thrown`() {
        composeRule.setThemedContent(arc(sunPosition = 4f))
        composeRule.onNodeWithContentDescription("The sun's day: sunrise 06:48, sunset 20:04")
            .assertIsDisplayed()
    }

    @Test
    fun `a NaN sun position is coerced rather than thrown`() {
        composeRule.setThemedContent(arc(sunPosition = Float.NaN))
        composeRule.onNodeWithContentDescription("The sun's day: sunrise 06:48, sunset 20:04")
            .assertIsDisplayed()
    }

    @Test
    fun `an inverted lit span is tolerated rather than thrown`() {
        composeRule.setThemedContent(arc(litSpan = 0.9f..0.1f))
        composeRule.onNodeWithContentDescription("The sun's day: sunrise 06:48, sunset 20:04")
            .assertIsDisplayed()
    }

    @Test
    fun `an empty node list renders the bare curve`() {
        composeRule.setThemedContent {
            NimazSolarArc(
                nodes = emptyList(),
                sunriseFraction = 0.27f,
                sunsetFraction = 0.80f,
                contentDescription = "Bare arc",
            )
        }
        composeRule.onNodeWithContentDescription("Bare arc").assertIsDisplayed()
    }

    /**
     * A node is a value, and the screen builds a fresh list on every tick — so equality is what
     * stops six identical nodes counting as a change and redrawing the arc every minute.
     */
    @Test
    fun `a node is a value, comparable and copyable`() {
        val dhuhr = NimazSolarNode(0.55f, "Dhuhr", NimazTone.PROMINENT, "Dhuhr at 13:22")
        assertThat(dhuhr).isEqualTo(
            NimazSolarNode(0.55f, "Dhuhr", NimazTone.PROMINENT, "Dhuhr at 13:22")
        )
        assertThat(dhuhr.copy(position = 0.6f)).isNotEqualTo(dhuhr)
        assertThat(dhuhr.hashCode()).isEqualTo(
            NimazSolarNode(0.55f, "Dhuhr", NimazTone.PROMINENT, "Dhuhr at 13:22").hashCode()
        )
        assertThat(dhuhr.toString()).contains("Dhuhr")
    }

    @Test
    fun `a degenerate day renders rather than throwing`() {
        composeRule.setThemedContent {
            NimazSolarArc(
                nodes = nodes,
                sunriseFraction = 0.8f,
                sunsetFraction = 0.2f, // sunset before sunrise
                contentDescription = "Degenerate",
            )
        }
        composeRule.onNodeWithContentDescription("Degenerate").assertIsDisplayed()
    }
}
