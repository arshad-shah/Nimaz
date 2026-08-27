package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazCarouselTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `renders first page content`() {
        composeRule.setThemedContent {
            NimazCarousel(count = 3, pageHeight = 100.dp) { page ->
                Box(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                    Text(text = "Page ${page + 1}")
                }
            }
        }

        composeRule.onNodeWithText("Page 1").assertExists()
    }

    @Test
    fun `single page renders without indicators crashing`() {
        composeRule.setThemedContent {
            NimazCarousel(count = 1, pageHeight = 80.dp) {
                Box(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                    Text(text = "Solo")
                }
            }
        }

        composeRule.onNodeWithText("Solo").assertExists()
        composeRule.onRoot().assertExists()
    }

    @Test
    fun `zero count renders nothing and does not crash`() {
        composeRule.setThemedContent {
            NimazCarousel(count = 0, pageHeight = 80.dp) {
                Text(text = "ShouldNotShow")
            }
        }

        composeRule.onRoot().assertExists()
        composeRule.onNodeWithText("ShouldNotShow").assertDoesNotExist()
    }

    @Test
    fun `multi-page indicators render without crashing`() {
        composeRule.setThemedContent {
            NimazCarousel(count = 4, pageHeight = 60.dp) { page ->
                Box(modifier = Modifier.fillMaxWidth().height(60.dp)) {
                    Text(text = "Item $page")
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Item 0").assertExists()
        composeRule.onRoot().assertExists()
    }
}
