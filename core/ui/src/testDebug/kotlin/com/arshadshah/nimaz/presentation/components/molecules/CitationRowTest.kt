package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Column
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
 * The citation row — a verse reference in a gutter with an optional preview of what it says.
 *
 * The preview is optional because a citation can arrive from the AI answer path before its text
 * has been resolved locally, and a row that rendered an empty `Text` there would leave a tappable
 * blank line under the reference. What is pinned is that the row is complete without it, and that
 * the whole row is one tap target: a reference you cannot tap is a proof card you cannot open.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class CitationRowTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `a citation renders its reference and its preview`() {
        composeRule.setThemedContent {
            CitationRow(
                reference = "2:153",
                preview = "Seek help through patience and prayer.",
                onClick = {},
            )
        }

        composeRule.onNodeWithText("2:153").assertExists()
        composeRule.onNodeWithText("Seek help through patience and prayer.").assertExists()
    }

    @Test
    fun `a citation with no preview is just its reference`() {
        composeRule.setThemedContent { CitationRow(reference = "3:200", onClick = {}) }

        composeRule.onNodeWithText("3:200").assertExists()
    }

    @Test
    fun `tapping the reference opens the citation`() {
        var opened = 0
        composeRule.setThemedContent {
            CitationRow(reference = "2:153", preview = "…", onClick = { opened++ })
        }

        composeRule.onNodeWithText("2:153").performClick()
        assertThat(opened).isEqualTo(1)
    }

    @Test
    fun `tapping the preview opens the same citation`() {
        // The card is the tap target, not the reference — so the long line of text is tappable
        // too. A row whose only target is a five-character reference is a row nobody can hit.
        var opened = 0
        composeRule.setThemedContent {
            CitationRow(
                reference = "2:153",
                preview = "Seek help through patience and prayer.",
                onClick = { opened++ },
            )
        }

        composeRule.onNodeWithText("Seek help through patience and prayer.").performClick()
        assertThat(opened).isEqualTo(1)
    }

    @Test
    fun `rows stack without their references drifting`() {
        composeRule.setThemedContent {
            Column {
                CitationRow(reference = "2:153", preview = "Patience", onClick = {})
                CitationRow(reference = "3:200", onClick = {})
            }
        }

        composeRule.onNodeWithText("2:153").assertExists()
        composeRule.onNodeWithText("3:200").assertExists()
    }
}
