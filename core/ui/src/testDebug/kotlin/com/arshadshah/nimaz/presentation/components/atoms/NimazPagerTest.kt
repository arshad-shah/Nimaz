package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazPagerTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val pagerTag = "pager_under_test"

    @Composable
    private fun TestPager(
        pageCount: Int = 3,
        initialPage: Int = 0,
        reverseLayout: Boolean = false,
        onState: (PagerState) -> Unit = {},
    ) {
        val state = rememberNimazPagerState(initialPage = initialPage) { pageCount }
        onState(state)
        NimazPager(
            state = state,
            modifier = Modifier
                .testTag(pagerTag)
                .fillMaxWidth()
                .height(120.dp),
            reverseLayout = reverseLayout,
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                Text(text = "Page ${page + 1}")
            }
        }
    }

    @Test
    fun `renders the first page content`() {
        composeRule.setThemedContent { TestPager() }

        composeRule.onNodeWithText("Page 1").assertIsDisplayed()
    }

    @Test
    fun `honours initialPage from the supplied state`() {
        composeRule.setThemedContent { TestPager(initialPage = 2) }

        composeRule.onNodeWithText("Page 3").assertIsDisplayed()
    }

    @Test
    fun `swiping left advances to the next page`() {
        composeRule.setThemedContent { TestPager() }

        composeRule.onNodeWithTag(pagerTag).performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Page 2").assertIsDisplayed()
    }

    @Test
    fun `swipe is disabled when userScrollEnabled is false`() {
        composeRule.setThemedContent {
            val state = rememberNimazPagerState { 3 }
            NimazPager(
                state = state,
                modifier = Modifier
                    .testTag(pagerTag)
                    .fillMaxWidth()
                    .height(120.dp),
                userScrollEnabled = false,
            ) { page ->
                Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                    Text(text = "Page ${page + 1}")
                }
            }
        }

        composeRule.onNodeWithTag(pagerTag).performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        // Still on the first page — the gesture was ignored.
        composeRule.onNodeWithText("Page 1").assertIsDisplayed()
    }

    @Test
    fun `state currentPage updates after a swipe`() {
        lateinit var pagerState: PagerState
        composeRule.setThemedContent { TestPager(onState = { pagerState = it }) }

        assertEquals(0, pagerState.currentPage)
        composeRule.onNodeWithTag(pagerTag).performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        assertEquals(1, pagerState.currentPage)
    }

    @Test
    fun `rememberNimazPagerState exposes the supplied page count`() {
        lateinit var pagerState: PagerState
        composeRule.setThemedContent { TestPager(pageCount = 5, onState = { pagerState = it }) }

        assertEquals(5, pagerState.pageCount)
    }

    @Test
    fun `reverseLayout pager still renders its pages`() {
        // RTL readers (Mushaf) pass reverseLayout = true; the wrapper must compose.
        composeRule.setThemedContent {
            TestPager(reverseLayout = true)
        }

        composeRule.onNodeWithText("Page 1").assertIsDisplayed()
    }

    @Test
    fun `applies contentPadding without crashing`() {
        composeRule.setThemedContent {
            val state = rememberNimazPagerState { 3 }
            NimazPager(
                state = state,
                modifier = Modifier.testTag(pagerTag).fillMaxWidth().height(120.dp),
                contentPadding = PaddingValues(horizontal = 24.dp),
                pageSpacing = 12.dp,
            ) { page ->
                Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                    Text(text = "Page ${page + 1}")
                }
            }
        }

        composeRule.onNodeWithText("Page 1").assertIsDisplayed()
    }
}
