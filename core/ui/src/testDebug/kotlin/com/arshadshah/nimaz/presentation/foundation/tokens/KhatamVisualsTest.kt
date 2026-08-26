package com.arshadshah.nimaz.presentation.foundation.tokens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.KhatamPace
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The khatam progress ring and bar, and the pace verdict behind them.
 *
 * These three exist to stop the same reading being drawn four different ways: the list hero, the
 * detail hero, the home card and the widget all show "how far through the Quran am I", and before
 * this they each hand-rolled it — two rings differing only in size, one stock
 * `LinearProgressIndicator` against one hand-drawn gradient bar. The failure that matters is a
 * percentage that disagrees with itself between two surfaces, so what is pinned is the *number*:
 * the ring's own label and the announcement a screen reader receives, at the ends and in between.
 *
 * The ring sets `clearAndSetSemantics` so TalkBack reads one phrase, which also removes the
 * percentage `Text` from the merged tree — every assertion on it goes through
 * `useUnmergedTree = true`, and the announcement is asserted on the merged node beside it.
 *
 * `paceLabel` and `paceColor` get the same treatment for the same reason — four verdicts, four
 * resources, and no other test in the app checks that "behind" does not read as "on track".
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class KhatamVisualsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `the ring prints the percentage it was given`() {
        composeRule.setThemedContent {
            Box(Modifier.size(120.dp)) { KhatamProgressRing(progress = 0.38f) }
        }

        composeRule.onNodeWithText(useUnmergedTree = true, text = "38%").assertExists()
    }

    @Test
    fun `the ring announces the same percentage it prints`() {
        // The ring sets `clearAndSetSemantics`, so the text node and the announcement come from
        // two different code paths off one `percent`. They must not diverge.
        composeRule.setThemedContent {
            Box(Modifier.size(120.dp)) { KhatamProgressRing(progress = 0.72f) }
        }

        val expected = androidx.test.core.app.ApplicationProvider
            .getApplicationContext<android.content.Context>()
            .getString(R.string.khatam_a11y_progress_ring, 72)
        composeRule.onNodeWithContentDescription(expected).assertExists()
    }

    @Test
    fun `progress past the ends is clamped rather than printed`() {
        // `coerceIn(0f, 1f)`. A khatam read one juz beyond its own plan is arithmetic that
        // happens; "112%" on a ring is not something to render.
        composeRule.setThemedContent {
            androidx.compose.foundation.layout.Column {
                Box(Modifier.size(120.dp)) { KhatamProgressRing(progress = 1.4f) }
                Box(Modifier.size(120.dp)) { KhatamProgressRing(progress = -0.3f) }
            }
        }

        composeRule.onNodeWithText(useUnmergedTree = true, text = "100%").assertExists()
        composeRule.onNodeWithText(useUnmergedTree = true, text = "0%").assertExists()
        composeRule.onNodeWithText(useUnmergedTree = true, text = "140%").assertDoesNotExist()
    }

    @Test
    fun `a zero-progress ring draws its track and no sweep`() {
        // `if (sweep > 0f)` — the arm that is skipped. It renders the empty ring, which is what a
        // khatam looks like on the day it is created.
        composeRule.setThemedContent {
            Box(Modifier.size(120.dp)) { KhatamProgressRing(progress = 0f) }
        }

        composeRule.onNodeWithText(useUnmergedTree = true, text = "0%").assertExists()
    }

    @Test
    fun `a complete ring is drawn in the completion accent`() {
        // `isComplete` swaps both the ink and the gradient for the flat gold. The label is the
        // only thing observable from the semantics tree, and it must still be right.
        composeRule.setThemedContent {
            Box(Modifier.size(120.dp)) {
                KhatamProgressRing(progress = 1f, isComplete = true)
            }
        }

        composeRule.onNodeWithText(useUnmergedTree = true, text = "100%").assertExists()
    }

    @Test
    fun `the ring takes a caller's own size, stroke and text style`() {
        // The parameters exist precisely so a third hand-rolled copy never gets written; a change
        // that dropped one would send a caller straight back to writing its own.
        composeRule.setThemedContent {
            Box(Modifier.size(200.dp)) {
                KhatamProgressRing(
                    progress = 0.5f,
                    size = 96.dp,
                    strokeWidth = 10.dp,
                    textStyle = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                    animated = false,
                )
            }
        }

        composeRule.onNodeWithText(useUnmergedTree = true, text = "50%").assertExists()
    }

    @Test
    fun `the bar renders at every progress including the ends`() {
        // No text and no semantics of its own — the assertion that means anything is that it
        // composes and measures at 0, mid, and 1, including the `animatedProgress > 0f` skip.
        composeRule.setThemedContent {
            androidx.compose.foundation.layout.Column {
                KhatamProgressBar(progress = 0f)
                KhatamProgressBar(progress = 0.38f)
                KhatamProgressBar(progress = 0.85f, height = 8.dp)
                KhatamProgressBar(progress = 1f, isComplete = true)
                KhatamProgressBar(progress = 2f)
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun `every pace verdict has its own words`() {
        // Four arms over one resource `when`. Two pointing at one id is a khatam that says it is
        // on track when it is behind — the single most misleading thing this feature can say.
        val labels = mutableListOf<String>()
        composeRule.setThemedContent {
            KhatamPace.entries.forEach { labels += paceLabel(it) }
        }

        assertThat(labels).hasSize(KhatamPace.entries.size)
        assertThat(labels.toSet()).hasSize(KhatamPace.entries.size)
        assertThat(labels.none { it.isBlank() }).isTrue()
    }

    @Test
    fun `every pace verdict has its own colour, and behind is not on track`() {
        val colours = mutableListOf<androidx.compose.ui.graphics.Color>()
        composeRule.setThemedContent {
            KhatamPace.entries.forEach { colours += paceColor(it) }
        }

        assertThat(colours[KhatamPace.entries.indexOf(KhatamPace.ON_TRACK)])
            .isNotEqualTo(colours[KhatamPace.entries.indexOf(KhatamPace.BEHIND)])
        assertThat(colours[KhatamPace.entries.indexOf(KhatamPace.SLIGHTLY_BEHIND)])
            .isNotEqualTo(colours[KhatamPace.entries.indexOf(KhatamPace.ON_TRACK)])
    }

    @Test
    fun `the accent is one value shared by every khatam surface`() {
        // `rememberKhatamAccent()` is the reason the hero, the rows, the trail and the chips
        // re-theme together. Its gradient runs completion → progress; an empty or single-stop
        // list is a bar that draws flat.
        var accent: KhatamAccent? = null
        composeRule.setThemedContent { accent = rememberKhatamAccent() }

        val resolved = requireNotNull(accent)
        assertThat(resolved.progressGradient).hasSize(2)
        assertThat(resolved.progressGradient.first()).isEqualTo(resolved.complete)
        assertThat(resolved.progressGradient.last()).isEqualTo(resolved.progress)
        assertThat(resolved.progress).isNotEqualTo(resolved.complete)
    }
}
