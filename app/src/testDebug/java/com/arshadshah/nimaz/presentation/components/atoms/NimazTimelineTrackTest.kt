package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazTimelineTrackTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val nodes = listOf(
        NimazTimelineNode(0f, NimazStatusDotSpec(NimazTone.SUCCESS), "Fajr"),
        NimazTimelineNode(0.5f, NimazStatusDotSpec(NimazTone.WARNING, NimazStatusDotStyle.OUTLINED), "Asr"),
        NimazTimelineNode(1f, NimazStatusDotSpec(NimazTone.MUTED), "Isha"),
    )

    @Test
    fun `node positions are clamped into the track`() {
        assertThat(NimazTimelineNode(-3f, NimazStatusDotSpec(NimazTone.SUCCESS), "x").safePosition)
            .isEqualTo(0f)
        assertThat(NimazTimelineNode(4f, NimazStatusDotSpec(NimazTone.SUCCESS), "x").safePosition)
            .isEqualTo(1f)
        assertThat(NimazTimelineNode(Float.NaN, NimazStatusDotSpec(NimazTone.SUCCESS), "x").safePosition)
            .isEqualTo(0f)
    }

    @Test
    fun `edge labels are rendered`() {
        composeRule.setThemedContent {
            NimazTimelineTrack(nodes = nodes, startLabel = "Fajr 04:31", endLabel = "Isha 22:35")
        }
        composeRule.onNodeWithText("Fajr 04:31").assertIsDisplayed()
        composeRule.onNodeWithText("Isha 22:35").assertIsDisplayed()
    }

    @Test
    fun `a described track speaks one sentence and hides its nodes`() {
        composeRule.setThemedContent {
            NimazTimelineTrack(
                nodes = nodes,
                startLabel = "Fajr 04:31",
                endLabel = "Isha 22:35",
                progress = 0.6f,
                contentDescription = "Three of five prayers recorded",
            )
        }
        composeRule.onNodeWithContentDescription("Three of five prayers recorded").assertIsDisplayed()
        composeRule.onNodeWithText("Fajr 04:31").assertDoesNotExist()
    }

    @Test
    fun `an out of range or NaN progress does not throw`() {
        composeRule.setThemedContent {
            NimazTimelineTrack(nodes = nodes, startLabel = "a", endLabel = "b", progress = Float.NaN)
            NimazTimelineTrack(nodes = nodes, startLabel = "a", endLabel = "b", progress = 9f)
            NimazTimelineTrack(nodes = nodes, startLabel = "a", endLabel = "b", progress = -2f)
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `an empty node list renders the bare track`() {
        composeRule.setThemedContent {
            NimazTimelineTrack(nodes = emptyList(), startLabel = "a", endLabel = "b")
        }
        composeRule.waitForIdle()
    }
}
