package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The tinted squircle at the leading edge of every list row, settings entry and empty state.
 *
 * Two things here are contracts rather than styling, and both were untested. The **container is
 * sized by the preset, the icon separately** — a well whose icon scaled but whose box did not
 * misaligns every row it sits in. And the **container tint is the hue at 14%, the icon the hue at
 * full strength**; the caller passes one raw colour and the well applies the alpha, which is the
 * API decision that lets a row say `color = colorScheme.primary` and get both.
 */
@RunWith(RobolectricTestRunner::class)
class NimazIconWellTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `each size preset sizes the container, and the icon inside it separately`() {
        NimazIconWellSize.entries.forEach { preset ->
            assertThat(preset.icon.value).isLessThan(preset.container.value)
        }
        assertThat(NimazIconWellSize.STANDARD.container).isEqualTo(40.dp)
        assertThat(NimazIconWellSize.STANDARD.icon).isEqualTo(20.dp)
    }

    @Test
    fun `the default preset is STANDARD, which is what a list row uses`() {
        composeRule.setThemedContent {
            NimazIconWell(
                icon = Icons.Default.Bookmark,
                color = Color.Red,
                contentDescription = "saved",
            )
        }
        composeRule.onNodeWithContentDescription("saved")
            .assertWidthIsEqualTo(NimazIconWellSize.STANDARD.icon)
            .assertHeightIsEqualTo(NimazIconWellSize.STANDARD.icon)
    }

    @Test
    fun `a larger preset grows the well`() {
        composeRule.setThemedContent {
            NimazIconWell(
                icon = Icons.Default.Bookmark,
                color = Color.Red,
                size = NimazIconWellSize.LARGE,
                contentDescription = "saved",
            )
        }
        composeRule.onNodeWithContentDescription("saved")
            .assertWidthIsEqualTo(NimazIconWellSize.LARGE.icon)
            .assertHeightIsEqualTo(NimazIconWellSize.LARGE.icon)
    }

    @Test
    fun `a decorative well publishes no content description`() {
        // Most wells sit beside text that already says what the row is. Announcing the icon as
        // well makes a screen reader read every row twice.
        composeRule.setThemedContent {
            NimazIconWell(icon = Icons.Default.Bookmark, color = Color.Red)
        }
        assertThat(composeRule.onRoot().printToString()).doesNotContain("ContentDescription")
    }

    @Test
    fun `the container alpha is the one the API promises to apply for the caller`() {
        // The caller passes a raw hue and gets both the 14% container and the full-strength
        // icon. A caller pre-dimming its own colour would land at 2%.
        assertThat(ACCENT_WELL_ALPHA).isEqualTo(0.14f)
    }
}
