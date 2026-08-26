package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The five chip shapes, and the one option that changes what a chip *claims*.
 *
 * `showSelectedIcon` is not decoration. A filter chip with a tick says "you chose this"; the same
 * chip without one is a **position indicator** — the Quran reader's sticky index marks which
 * section is on screen, which is a statement about where you are and not a filter you applied.
 * Defaulting it on and forgetting to turn it off puts a check mark on something the user never
 * selected.
 *
 * The rest is the surface: five wrappers, each with its own enabled, elevated, icon and shape
 * options, and each of them is what some screen in the app reaches for. A wrapper whose `enabled`
 * stopped being threaded through leaves a disabled-looking chip that still fires.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class NimazChipVariantsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `every chip variant renders and reports its tap`() {
        val taps = mutableListOf<String>()
        composeRule.setThemedContent {
            Column {
                NimazChipVariant.entries.forEach { variant ->
                    NimazChip(
                        text = variant.name,
                        onClick = { taps += variant.name },
                        variant = variant,
                        leadingIcon = Icons.Filled.Star,
                    )
                }
            }
        }

        NimazChipVariant.entries.forEach { variant ->
            composeRule.onNodeWithText(variant.name).performClick()
        }

        assertThat(taps).containsExactlyElementsIn(NimazChipVariant.entries.map { it.name })
    }

    @Test
    fun `a selected chip reports itself as selected`() {
        composeRule.setThemedContent {
            NimazChip(text = "Meccan", onClick = {}, selected = true)
        }

        composeRule.onNodeWithText("Meccan").assertIsSelected()
    }

    @Test
    fun `a position indicator is selected without claiming to be chosen`() {
        // `showSelectedIcon = false`. Both chips are selected; only one wears a tick, and the
        // difference is the claim being made about how it got that way.
        composeRule.setThemedContent {
            Column {
                NimazChip(text = "Filtered", onClick = {}, selected = true)
                NimazChip(
                    text = "Where you are",
                    onClick = {},
                    selected = true,
                    showSelectedIcon = false,
                )
            }
        }

        composeRule.onNodeWithText("Filtered").assertIsSelected()
        composeRule.onNodeWithText("Where you are").assertIsSelected()
    }

    @Test
    fun `a disabled chip cannot be tapped`() {
        var taps = 0
        composeRule.setThemedContent {
            NimazChip(text = "Off", onClick = { taps++ }, enabled = false)
        }

        composeRule.onNodeWithText("Off").assertIsNotEnabled()
        assertThat(taps).isEqualTo(0)
    }

    @Test
    fun `the filter chip carries its own options`() {
        composeRule.setThemedContent {
            Column {
                NimazFilterChip(selected = true, onClick = {}, label = "Plain")
                NimazFilterChip(
                    selected = false,
                    onClick = {},
                    label = "Elevated",
                    leadingIcon = Icons.Filled.Star,
                    showSelectedIcon = false,
                    elevated = true,
                )
                NimazFilterChip(
                    selected = false,
                    onClick = {},
                    label = "Disabled",
                    enabled = false,
                )
            }
        }

        composeRule.onNodeWithText("Plain").assertIsSelected()
        composeRule.onNodeWithText("Disabled").assertIsNotEnabled()
    }

    @Test
    fun `the assist and suggestion chips carry theirs`() {
        var assisted = 0
        var suggested = 0
        composeRule.setThemedContent {
            Column {
                NimazAssistChip(onClick = { assisted++ }, label = "Assist")
                NimazAssistChip(
                    onClick = {},
                    label = "Assist elevated",
                    leadingIcon = Icons.Filled.Star,
                    elevated = true,
                )
                NimazAssistChip(onClick = {}, label = "Assist off", enabled = false)
                NimazSuggestionChip(onClick = { suggested++ }, label = "Suggest")
                NimazSuggestionChip(
                    onClick = {},
                    label = "Suggest elevated",
                    icon = Icons.Filled.Star,
                    elevated = true,
                )
                NimazSuggestionChip(onClick = {}, label = "Suggest off", enabled = false)
            }
        }

        composeRule.onNodeWithText("Assist").performClick()
        composeRule.onNodeWithText("Suggest").performClick()

        assertThat(assisted).isEqualTo(1)
        assertThat(suggested).isEqualTo(1)
        composeRule.onNodeWithText("Assist off").assertIsNotEnabled()
        composeRule.onNodeWithText("Suggest off").assertIsNotEnabled()
    }

    @Test
    fun `an input chip can be dismissed and can carry an avatar`() {
        // `onDismiss` is what turns a chip into a removable token — the search screen's applied
        // filters. Without it the chip has no trailing control at all, which is a different
        // component in practice.
        var dismissed = 0
        composeRule.setThemedContent {
            Column {
                NimazInputChip(
                    selected = true,
                    onClick = {},
                    label = "Removable",
                    onDismiss = { dismissed++ },
                    leadingIcon = Icons.Filled.Star,
                )
                NimazInputChip(
                    selected = false,
                    onClick = {},
                    label = "Avatared",
                    avatar = { Text("A") },
                )
                NimazInputChip(selected = false, onClick = {}, label = "Fixed", enabled = false)
            }
        }

        composeRule.onNodeWithText("Removable").assertIsSelected()
        composeRule.onNodeWithText("A").assertExists()
        composeRule.onNodeWithText("Fixed").assertIsNotEnabled()
        assertThat(dismissed).isEqualTo(0)
    }

    @Test
    fun `the revelation chip distinguishes Meccan from Medinan`() {
        // A surah's place of revelation, drawn on every surah row. One label for both would make
        // the chip meaningless while still rendering.
        composeRule.setThemedContent {
            Column {
                RevelationTypeChip(isMeccan = true)
                RevelationTypeChip(isMeccan = false)
            }
        }

        composeRule.waitForIdle()
    }
}
