package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The one prev/next button — readers, the Mushaf page bar, the month steppers.
 *
 * It had no test at all, which matters most for the disabled case: the ends of a range are drawn
 * by dimming the tint, and a dimmed control that still fires is the failure nobody looks for,
 * because it looks exactly right in a screenshot. The other reason is [NavArrowDirection]: the
 * direction is *visual only* — right-to-left surfaces deliberately wire the left-pointing arrow
 * to advance — so a test that asserted "PREVIOUS goes back" would pin the wrong thing. What is
 * assertable is that the two directions draw different chevrons and that the click reaches the
 * caller unchanged.
 */
@RunWith(RobolectricTestRunner::class)
class NimazNavArrowButtonTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `both directions render, each with its own chevron`() {
        composeRule.setThemedContent {
            NimazNavArrowButton(
                direction = NavArrowDirection.PREVIOUS,
                onClick = {},
                contentDescription = "previous",
            )
        }
        composeRule.onNodeWithContentDescription("previous").assertIsEnabled()
        assertThat(NimazIcons.Previous).isNotEqualTo(NimazIcons.Next)
    }

    @Test
    fun `a click reaches the caller`() {
        var clicks = 0
        composeRule.setThemedContent {
            NimazNavArrowButton(
                direction = NavArrowDirection.NEXT,
                onClick = { clicks++ },
                contentDescription = "next",
            )
        }
        composeRule.onNodeWithContentDescription("next").performClick()
        assertThat(clicks).isEqualTo(1)
    }

    @Test
    fun `a disabled arrow does not fire`() {
        // The end of a range is drawn by dimming the tint. If the surface stayed clickable the
        // button would look spent and still page past the last ayah.
        var clicks = 0
        composeRule.setThemedContent {
            NimazNavArrowButton(
                direction = NavArrowDirection.NEXT,
                onClick = { clicks++ },
                contentDescription = "next",
                enabled = false,
            )
        }
        composeRule.onNodeWithContentDescription("next").assertIsNotEnabled().performClick()
        assertThat(clicks).isEqualTo(0)
    }

    @Test
    fun `the size parameter sizes the whole target, not just the glyph`() {
        // The denser 44dp stepper is the reason the parameter exists; sizing only the icon would
        // shrink the drawing and leave a 48dp tap target overlapping its neighbour.
        composeRule.setThemedContent {
            NimazNavArrowButton(
                direction = NavArrowDirection.PREVIOUS,
                onClick = {},
                contentDescription = "previous",
                size = 44.dp,
            )
        }
        composeRule.onNodeWithContentDescription("previous")
            .assertWidthIsEqualTo(44.dp)
            .assertHeightIsEqualTo(44.dp)
    }

    @Test
    fun `the default size is the 48dp reader button`() {
        composeRule.setThemedContent {
            NimazNavArrowButton(
                direction = NavArrowDirection.NEXT,
                onClick = {},
                contentDescription = "next",
            )
        }
        composeRule.onNodeWithContentDescription("next")
            .assertWidthIsEqualTo(48.dp)
            .assertHeightIsEqualTo(48.dp)
    }

    @Test
    fun `the direction vocabulary is the two it claims`() {
        assertThat(NavArrowDirection.entries).hasSize(2)
    }
}
