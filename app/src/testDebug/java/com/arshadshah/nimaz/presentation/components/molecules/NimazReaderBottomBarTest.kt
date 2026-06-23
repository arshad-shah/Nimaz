package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazReaderBottomBarTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `renders pill action content`() {
        composeRule.setThemedContent {
            NimazReaderBottomBar(
                currentPage = 1,
                pageCount = 4,
                onPrev = {},
                onNext = {},
                prevContentDescription = "Previous page",
                nextContentDescription = "Next page"
            ) {
                Text("Highlight")
            }
        }
        composeRule.onNodeWithText("Highlight").assertExists()
    }

    @Test
    fun `chevrons render when pager has multiple pages`() {
        composeRule.setThemedContent {
            NimazReaderBottomBar(
                currentPage = 1,
                pageCount = 4,
                onPrev = {},
                onNext = {},
                prevContentDescription = "Previous page",
                nextContentDescription = "Next page"
            ) { Text("Pill") }
        }
        composeRule.onNodeWithContentDescription("Previous page").assertExists()
        composeRule.onNodeWithContentDescription("Next page").assertExists()
    }

    @Test
    fun `prev chevron fires when enabled`() {
        var fired = false
        composeRule.setThemedContent {
            NimazReaderBottomBar(
                currentPage = 2,
                pageCount = 4,
                onPrev = { fired = true },
                onNext = {},
                prevContentDescription = "Previous page",
                nextContentDescription = "Next page"
            ) { Text("Pill") }
        }
        composeRule.onNodeWithContentDescription("Previous page").performClick()
        assertThat(fired).isTrue()
    }

    @Test
    fun `next chevron fires when enabled`() {
        var fired = false
        composeRule.setThemedContent {
            NimazReaderBottomBar(
                currentPage = 0,
                pageCount = 4,
                onPrev = {},
                onNext = { fired = true },
                prevContentDescription = "Previous page",
                nextContentDescription = "Next page"
            ) { Text("Pill") }
        }
        composeRule.onNodeWithContentDescription("Next page").performClick()
        assertThat(fired).isTrue()
    }

    @Test
    fun `prev chevron disabled on first page`() {
        composeRule.setThemedContent {
            NimazReaderBottomBar(
                currentPage = 0,
                pageCount = 4,
                onPrev = {},
                onNext = {},
                prevContentDescription = "Previous page",
                nextContentDescription = "Next page"
            ) { Text("Pill") }
        }
        composeRule.onNodeWithContentDescription("Previous page").assertIsNotEnabled()
    }

    @Test
    fun `next chevron disabled on last page`() {
        composeRule.setThemedContent {
            NimazReaderBottomBar(
                currentPage = 3,
                pageCount = 4,
                onPrev = {},
                onNext = {},
                prevContentDescription = "Previous page",
                nextContentDescription = "Next page"
            ) { Text("Pill") }
        }
        composeRule.onNodeWithContentDescription("Next page").assertIsNotEnabled()
    }

    @Test
    fun `single page hides chevrons`() {
        composeRule.setThemedContent {
            NimazReaderBottomBar(
                currentPage = 0,
                pageCount = 1,
                onPrev = {},
                onNext = {},
                prevContentDescription = "Previous page",
                nextContentDescription = "Next page"
            ) { Text("Pill") }
        }
        composeRule.onNodeWithText("Pill").assertExists()
        composeRule.onNodeWithContentDescription("Previous page").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Next page").assertDoesNotExist()
    }

    @Test
    fun `long pager shows counter indicator`() {
        composeRule.setThemedContent {
            NimazReaderBottomBar(
                currentPage = 7,
                pageCount = 30,
                onPrev = {},
                onNext = {},
                prevContentDescription = "Previous page",
                nextContentDescription = "Next page"
            ) { Text("Pill") }
        }
        composeRule.onNodeWithText("8 / 30").assertExists()
    }
}
