package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazProgressTrackTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `sizes are complete and ordered`() {
        assertThat(NimazProgressSize.entries).hasSize(3)
        assertThat(NimazProgressSize.THIN.height.value)
            .isLessThan(NimazProgressSize.MEDIUM.height.value)
        assertThat(NimazProgressSize.MEDIUM.height.value)
            .isLessThan(NimazProgressSize.THICK.height.value)
    }

    @Test
    fun `progress above one is coerced rather than thrown`() {
        composeRule.setThemedContent {
            NimazProgressTrack(progress = 4.2f, contentDescription = "over")
        }
        composeRule.onNodeWithContentDescription("over").assertIsDisplayed()
    }

    @Test
    fun `negative progress is coerced rather than thrown`() {
        composeRule.setThemedContent {
            NimazProgressTrack(progress = -1f, contentDescription = "under")
        }
        composeRule.onNodeWithContentDescription("under").assertIsDisplayed()
    }

    @Test
    fun `NaN progress renders as empty rather than crashing`() {
        composeRule.setThemedContent {
            NimazProgressTrack(progress = Float.NaN, contentDescription = "nan")
        }
        composeRule.onNodeWithContentDescription("nan").assertIsDisplayed()
    }

    @Test
    fun `an infinite progress is coerced rather than thrown`() {
        composeRule.setThemedContent {
            NimazProgressTrack(
                progress = Float.POSITIVE_INFINITY,
                contentDescription = "infinite"
            )
        }
        composeRule.onNodeWithContentDescription("infinite").assertIsDisplayed()
    }

    @Test
    fun `an empty bar renders`() {
        composeRule.setThemedContent {
            NimazProgressTrack(progress = 0f, contentDescription = "empty")
        }
        composeRule.onNodeWithContentDescription("empty").assertIsDisplayed()
    }

    @Test
    fun `a full bar renders`() {
        composeRule.setThemedContent {
            NimazProgressTrack(progress = 1f, contentDescription = "full")
        }
        composeRule.onNodeWithContentDescription("full").assertIsDisplayed()
    }

    @Test
    fun `the gradient variant renders`() {
        composeRule.setThemedContent {
            NimazProgressTrack(
                progress = 0.4f,
                gradient = true,
                size = NimazProgressSize.THICK,
                contentDescription = "gradient"
            )
        }
        composeRule.onNodeWithContentDescription("gradient").assertIsDisplayed()
    }

    @Test
    fun `every tone renders`() {
        composeRule.setThemedContent {
            androidx.compose.foundation.layout.Column {
                NimazTone.entries.forEach { tone ->
                    NimazProgressTrack(progress = 0.5f, tone = tone)
                }
            }
        }
        composeRule.waitForIdle()
    }
}
