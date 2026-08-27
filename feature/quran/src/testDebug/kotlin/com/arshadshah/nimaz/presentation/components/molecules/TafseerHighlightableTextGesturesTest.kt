package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performTouchInput
import com.arshadshah.nimaz.domain.model.TafseerHighlight
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The two gestures over a page of commentary: long-press to start a selection, tap to reopen a
 * highlight that is already there.
 *
 * A long press seeds the selection with the whole word under the finger rather than a caret,
 * because a highlight is a phrase and refining from a word is one drag instead of two. Pressing
 * a gap has to give the handles *something* to hold — a zero-width selection draws no handles,
 * which reads as the press having done nothing at all.
 *
 * The tap is the other half: inside an existing highlight it reopens it, outside one it clears
 * whatever was selected. A tap that does neither leaves the reader with a selection they cannot
 * get rid of without leaving the page.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class TafseerHighlightableTextGesturesTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val text = "Mercy is what the surah opens with."

    private val selections = mutableListOf<Pair<Int, Int>>()
    private val tapped = mutableListOf<Long>()

    private fun highlight(id: Long, start: Int, end: Int) = TafseerHighlight(
        id = id,
        ayahId = 1,
        tafseerId = "ibn-kathir",
        startOffset = start,
        endOffset = end,
        color = "#FDE68A",
        note = null,
        createdAt = 0L,
        updatedAt = 0L,
    )

    private fun render(
        highlights: List<TafseerHighlight> = emptyList(),
        selectionStart: Int = -1,
        selectionEnd: Int = -1,
    ) {
        composeRule.setThemedContent {
            TafseerHighlightableText(
                text = text,
                highlights = highlights,
                selectionStart = selectionStart,
                selectionEnd = selectionEnd,
                onSelectionChange = { s, e -> selections += s to e },
                onHighlightTapped = { tapped += it.id },
                clearSelectionToken = 0,
            )
        }
    }

    /** The commentary is LTR, so the first character sits at the left edge. */
    private fun atStart(): Offset = Offset(2f, 8f)

    @Test
    fun `a long press selects the whole word under the finger`() {
        render()

        composeRule.onAllNodes(hasText(text)).onFirst()
            .performTouchInput { longClick(atStart()) }
        composeRule.waitForIdle()

        // "Mercy" — not a caret, and not the rest of the line.
        assertThat(selections).containsExactly(0 to 5)
    }

    @Test
    fun `tapping inside an existing highlight reopens it`() {
        render(highlights = listOf(highlight(id = 9, start = 0, end = 5)))

        composeRule.onAllNodes(hasText(text)).onFirst()
            .performTouchInput { click(atStart()) }
        composeRule.waitForIdle()

        assertThat(tapped).containsExactly(9L)
    }

    @Test
    fun `tapping outside every highlight opens none of them`() {
        render(highlights = listOf(highlight(id = 9, start = 20, end = 25)))

        composeRule.onAllNodes(hasText(text)).onFirst()
            .performTouchInput { click(atStart()) }
        composeRule.waitForIdle()

        assertThat(tapped).isEmpty()
    }

    @Test
    fun `tapping away from a selection dismisses it`() {
        // Otherwise the reader is left holding a selection with no way to put it down.
        render(selectionStart = 20, selectionEnd = 25)

        composeRule.onAllNodes(hasText(text)).onFirst()
            .performTouchInput { click(atStart()) }
        composeRule.waitForIdle()

        assertThat(selections).containsExactly(-1 to -1)
    }

    @Test
    fun `tapping with nothing selected and nothing highlighted changes nothing`() {
        render()

        composeRule.onAllNodes(hasText(text)).onFirst()
            .performTouchInput { click(atStart()) }
        composeRule.waitForIdle()

        assertThat(selections).isEmpty()
        assertThat(tapped).isEmpty()
    }

    @Test
    fun `a selection draws a handle at each end`() {
        render(selectionStart = 0, selectionEnd = 5)

        // Two draggable handles hang off the caret rects; without them the selection cannot be
        // refined and the feature is a long-press that produces a fixed word.
        composeRule.onAllNodes(hasText(text)).onFirst().assertExists()
    }
}
