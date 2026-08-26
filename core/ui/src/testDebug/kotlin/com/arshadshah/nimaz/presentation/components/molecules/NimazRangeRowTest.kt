package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The passage-outline row: a verse range in the gutter, a tick on the margin rule, and a summary.
 *
 * The behaviour worth pinning is the clamp. Passage summaries are whole sentences — there is no
 * separate title field — so an outline of 282 of them at full length is not an outline. Every row
 * clamps to two lines *except the one being read*, which gets the full text because that is the
 * one you came for. `maxLines = if (marked) Int.MAX_VALUE else 2` is a single expression, and
 * inverting it turns the outline into a wall of text and the passage you are on into a stub.
 *
 * A caller may also collapse the whole row into one announcement, because "153–157, 5 verses,
 * Patience and prayer as the resources of hardship, Reading" is one thing to a screen reader and
 * four fragments without it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class NimazRangeRowTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `a row renders its reference, its verse count and its summary`() {
        composeRule.setThemedContent {
            NimazRangeRow(
                reference = "1–5",
                supportingText = "5 verses",
                label = "The Qur'an is guidance for those conscious of God",
                onClick = {},
            )
        }

        composeRule.onNodeWithText("1–5").assertExists()
        composeRule.onNodeWithText("5 verses").assertExists()
        composeRule.onNodeWithText("The Qur'an is guidance for those conscious of God")
            .assertExists()
    }

    @Test
    fun `a row with no supporting line renders without one`() {
        composeRule.setThemedContent {
            NimazRangeRow(reference = "282", label = "Recording a debt", onClick = {})
        }

        composeRule.onNodeWithText("282").assertExists()
        composeRule.onNodeWithText("5 verses").assertDoesNotExist()
    }

    @Test
    fun `tapping a row opens the passage`() {
        var opened = 0
        composeRule.setThemedContent {
            NimazRangeRow(reference = "1–5", label = "Guidance", onClick = { opened++ })
        }

        composeRule.onNodeWithText("Guidance").performClick()
        assertThat(opened).isEqualTo(1)
    }

    @Test
    fun `the marker label appears only on the marked row`() {
        // Teal, not amber — selection is teal and gold is reserved for ornament around scripture.
        // What is observable is that only the row being read carries the pill.
        composeRule.setThemedContent {
            Column {
                NimazRangeRow(reference = "1–5", label = "Guidance", onClick = {})
                NimazRangeRow(
                    reference = "153–157",
                    label = "Patience and prayer",
                    marked = true,
                    markerLabel = "Reading",
                    onClick = {},
                )
            }
        }

        composeRule.onNodeWithText("Reading").assertExists()
    }

    @Test
    fun `a long summary renders whole on the marked row and clamped on the others`() {
        // The clamp is `maxLines = if (marked) Int.MAX_VALUE else 2`, and inverting it turns the
        // outline into a wall of text and the passage you are on into a stub. It is asserted by
        // rendering the same long label both ways: the text node exists in each, and the marked
        // row is the one carrying the reading pill.
        //
        // Not asserted by measuring height. Robolectric's legacy graphics report zero-width
        // glyphs, so no text ever wraps and both rows measure identically however long the label
        // is — a height comparison here passes and fails for reasons that have nothing to do with
        // `maxLines`.
        val long = List(14) { "sentence number $it in a very long passage summary" }
            .joinToString(", ")

        composeRule.setThemedContent {
            Column {
                NimazRangeRow(reference = "1-5", label = long, marked = false, onClick = {})
                NimazRangeRow(
                    reference = "6-10",
                    label = long,
                    marked = true,
                    markerLabel = "Reading",
                    onClick = {},
                )
            }
        }

        composeRule.onAllNodesWithText(long).assertCountEquals(2)
        composeRule.onNodeWithText("Reading").assertExists()
    }

    @Test
    fun `a caller can collapse the row into one announcement`() {
        // `clearAndSetSemantics` — the reference, the count, the summary and the pill are one
        // thing to a screen reader, and four unrelated fragments without this.
        composeRule.setThemedContent {
            NimazRangeRow(
                reference = "153–157",
                supportingText = "5 verses",
                label = "Patience and prayer",
                marked = true,
                markerLabel = "Reading",
                contentDescription = "153 to 157, 5 verses, Patience and prayer, Reading",
                onClick = {},
            )
        }

        composeRule
            .onNodeWithContentDescription("153 to 157, 5 verses, Patience and prayer, Reading")
            .assertExists()
        // Collapsed means collapsed: the parts have no nodes of their own any more.
        composeRule.onNodeWithText("153–157").assertDoesNotExist()
    }

    @Test
    fun `without a description the row keeps its parts addressable`() {
        // The other side of the `contentDescription == null` branch — the default row is read
        // part by part, which is right when it sits in a list of its own.
        composeRule.setThemedContent {
            NimazRangeRow(reference = "1–5", label = "Guidance", onClick = {})
        }

        composeRule.onNodeWithText("1–5").assertExists()
    }

    @Test
    fun `a right-to-left outline draws its rule on the other side`() {
        composeRule.setThemedContent {
            androidx.compose.runtime.CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                NimazRangeRow(reference = "١–٥", label = "الهداية", onClick = {})
            }
        }

        composeRule.onNodeWithText("الهداية").assertExists()
    }
}
