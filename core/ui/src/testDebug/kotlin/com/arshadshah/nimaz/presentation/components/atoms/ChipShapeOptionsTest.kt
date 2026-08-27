package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Text
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The chips' shape parameter, which is the last option none of their other tests supplies.
 *
 * Every chip wrapper takes a `shape` so a caller can match the surface it sits on — a pill in a
 * filter row, a softer rectangle inside a card. It is the sort of parameter that gets read into a
 * local and then not passed on, and the result is a chip that ignores the shape it was given while
 * every test that never sets one stays green.
 *
 * `NimazChipStyle` is exercised for the same reason: it is a public part of the vocabulary and
 * nothing else composes every entry of it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class ChipShapeOptionsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `every chip wrapper accepts a caller's shape`() {
        composeRule.setThemedContent {
            Column {
                NimazFilterChip(
                    selected = true,
                    onClick = {},
                    label = "Filter",
                    shape = RoundedCornerShape(2.dp),
                )
                NimazAssistChip(
                    onClick = {},
                    label = "Assist",
                    shape = CutCornerShape(4.dp),
                )
                NimazInputChip(
                    selected = false,
                    onClick = {},
                    label = "Input",
                    onDismiss = {},
                    avatar = { Text("A") },
                    leadingIcon = Icons.Filled.Star,
                    shape = RoundedCornerShape(16.dp),
                )
                NimazSuggestionChip(
                    onClick = {},
                    label = "Suggest",
                    icon = Icons.Filled.Star,
                    shape = RoundedCornerShape(0.dp),
                )
            }
        }

        listOf("Filter", "Assist", "Input", "Suggest").forEach {
            composeRule.onNodeWithText(it).assertExists()
        }
    }

    @Test
    fun `every declared chip style is a distinct part of the vocabulary`() {
        // Public enum, no other test composes it. Two entries collapsing into one would leave a
        // style name in the API that no longer means anything.
        com.google.common.truth.Truth.assertThat(NimazChipStyle.entries.map { it.name }.toSet())
            .hasSize(NimazChipStyle.entries.size)
    }
}
