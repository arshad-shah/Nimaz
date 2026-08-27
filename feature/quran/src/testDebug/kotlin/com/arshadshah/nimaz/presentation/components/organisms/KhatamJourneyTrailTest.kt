package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.JuzProgressInfo
import com.arshadshah.nimaz.presentation.screens.str
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Thirty juz drawn as a path, and the three things a node can be.
 *
 * The trail is a picture, so almost nothing about it is readable — which is exactly why the
 * three states are published as content descriptions rather than left to colour. "Complete",
 * "in progress" and "not started" are the whole information content of the drawing, and a
 * screen reader that heard only "1, 2, 3, …" would get none of it.
 *
 * The current juz is **the first unfinished one**, not "completed + 1": finish only juz 30 and
 * the arithmetic version claims the reader is on juz 2.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class KhatamJourneyTrailTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private var clicked: Int? = null

    /** Thirty juz, with [done] of them finished from the start. */
    private fun trail(done: Int) = (1..30).map { n ->
        JuzProgressInfo(juzNumber = n, totalAyahs = 200, readAyahs = if (n <= done) 200 else 0)
    }

    private fun render(
        juzProgress: List<JuzProgressInfo>,
        onJuzClick: ((Int) -> Unit)? = { clicked = it },
    ) {
        composeRule.setThemedContent {
            KhatamJourneyTrail(juzProgress = juzProgress, onJuzClick = onJuzClick)
        }
    }

    private fun complete(juz: Int) = str(R.string.khatam_a11y_juz_complete, juz)
    private fun current(juz: Int) = str(R.string.khatam_a11y_juz_current, juz)
    private fun locked(juz: Int) = str(R.string.khatam_a11y_juz_locked, juz)

    @Test
    fun `a finished juz says it is finished`() {
        render(trail(done = 3))

        composeRule.onNodeWithContentDescription(complete(1)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(complete(3)).assertIsDisplayed()
    }

    @Test
    fun `the first unfinished juz is the one in progress`() {
        render(trail(done = 3))

        composeRule.onNodeWithContentDescription(current(4)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(locked(5)).assertIsDisplayed()
    }

    @Test
    fun `a reader who has read nothing is on the first juz`() {
        render(trail(done = 0))

        composeRule.onNodeWithContentDescription(current(1)).assertIsDisplayed()
    }

    @Test
    fun `finishing out of order does not claim the reader is at the front`() {
        // Finish only juz 30. "completed + 1" would say juz 2; the first *unfinished* one is 1.
        val outOfOrder = (1..30).map { n ->
            JuzProgressInfo(juzNumber = n, totalAyahs = 200, readAyahs = if (n == 30) 200 else 0)
        }

        render(outOfOrder)

        composeRule.onNodeWithContentDescription(current(1)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(complete(30)).assertIsDisplayed()
    }

    @Test
    fun `a juz that is part-read is not a finished one`() {
        val partial = (1..30).map { n ->
            JuzProgressInfo(juzNumber = n, totalAyahs = 200, readAyahs = if (n == 1) 199 else 0)
        }

        render(partial)

        composeRule.onNodeWithContentDescription(current(1)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(complete(1)).assertDoesNotExist()
    }

    @Test
    fun `a finished khatam has no juz in progress`() {
        render(trail(done = 30))

        composeRule.onNodeWithContentDescription(complete(30)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(current(30)).assertDoesNotExist()
    }

    @Test
    fun `tapping a juz is the caller's business`() {
        render(trail(done = 3))

        composeRule.onNodeWithContentDescription(current(4)).performClick()

        assertThat(clicked).isEqualTo(4)
    }

    @Test
    fun `a trail with no handler is a picture, not a control`() {
        render(trail(done = 3), onJuzClick = null)

        composeRule.onNodeWithContentDescription(current(4)).performClick()

        assertThat(clicked).isNull()
    }

    @Test
    fun `a trail with nothing in it draws nothing rather than failing`() {
        render(emptyList())

        composeRule.onNodeWithContentDescription(current(1)).assertDoesNotExist()
    }
}
