package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The settings slider — a title, a live value readout, and the track itself.
 *
 * The readout is the interesting part: the caller formats it, so the slider shows "28 sp" while
 * holding 28f. A control whose label stopped tracking its value would leave the reader adjusting
 * the Quran's Arabic size against a number that never moves, which is worse than showing no number
 * at all.
 *
 * The other is the disabled treatment. It dims the **whole column**, not just the track, precisely
 * so a disabled slider and a disabled toggle in the same settings group read the same — the
 * comment in the source says the earlier version dimmed only the track and left the label looking
 * live.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class NimazSettingsSliderTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `the slider shows its title and the caller's formatted value`() {
        composeRule.setThemedContent {
            NimazSettingsSlider(
                title = "Arabic size",
                valueLabel = "28 sp",
                value = 28f,
                onValueChange = {},
                valueRange = 18f..42f,
            )
        }

        composeRule.onNodeWithText("Arabic size").assertExists()
        composeRule.onNodeWithText("28 sp").assertExists()
    }

    @Test
    fun `dragging the track reports a value inside the range`() {
        var reported: Float? = null
        composeRule.setThemedContent {
            NimazSettingsSlider(
                title = "Arabic size",
                valueLabel = "28 sp",
                value = 28f,
                onValueChange = { reported = it },
                valueRange = 18f..42f,
                contentDescription = "Arabic font size",
            )
        }

        composeRule.onNodeWithContentDescription("Arabic font size")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(34f) }

        assertThat(reported).isNotNull()
        assertThat(reported!!).isIn(com.google.common.collect.Range.closed(18f, 42f))
    }

    @Test
    fun `a described slider is addressable by what a screen reader is told`() {
        // The track has no text of its own, so the description is the only handle TalkBack gets —
        // and the only way the reader knows which of two sliders in a group they are on.
        composeRule.setThemedContent {
            NimazSettingsSlider(
                title = "Translation size",
                valueLabel = "16 sp",
                value = 16f,
                onValueChange = {},
                valueRange = 12f..28f,
                contentDescription = "Translation font size",
            )
        }

        composeRule.onNodeWithContentDescription("Translation font size").assertExists()
    }

    @Test
    fun `a slider with no description still renders`() {
        // The `contentDescription == null` arm — the modifier is applied conditionally, and the
        // `else` branch is a plain `Modifier`.
        composeRule.setThemedContent {
            NimazSettingsSlider(
                title = "Arabic size",
                valueLabel = "28 sp",
                value = 28f,
                onValueChange = {},
                valueRange = 18f..42f,
            )
        }

        composeRule.onNodeWithText("Arabic size").assertExists()
    }

    @Test
    fun `a disabled slider cannot be moved`() {
        composeRule.setThemedContent {
            NimazSettingsSlider(
                title = "Arabic size",
                valueLabel = "28 sp",
                value = 28f,
                onValueChange = {},
                valueRange = 18f..42f,
                enabled = false,
                contentDescription = "Arabic font size",
            )
        }

        composeRule.onNodeWithContentDescription("Arabic font size").assertIsNotEnabled()
    }

    @Test
    fun `an enabled slider is enabled`() {
        composeRule.setThemedContent {
            NimazSettingsSlider(
                title = "Arabic size",
                valueLabel = "28 sp",
                value = 28f,
                onValueChange = {},
                valueRange = 18f..42f,
                steps = 5,
                contentDescription = "Arabic font size",
            )
        }

        composeRule.onNodeWithContentDescription("Arabic font size").assertIsEnabled()
    }
}
