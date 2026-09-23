package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onRoot
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazPageIndicatorTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private fun dots() = composeRule.onAllNodesWithTag(NimazPageIndicatorDotTag)

    private fun selectedDots(): SemanticsMatcher = isSelected()

    @Test
    fun `renders one dot per page`() {
        composeRule.setThemedContent {
            NimazPageIndicator(pageCount = 5, currentPage = 0)
        }

        dots().assertCountEquals(5)
    }

    @Test
    fun `marks exactly one dot selected`() {
        composeRule.setThemedContent {
            NimazPageIndicator(pageCount = 5, currentPage = 2)
        }

        composeRule.onAllNodesWithTag(NimazPageIndicatorDotTag)
            .filter(selectedDots())
            .assertCountEquals(1)
    }

    @Test
    fun `selects the dot matching the current page`() {
        // Dot at index 3 (the 4th node) should be the selected one.
        composeRule.setThemedContent {
            NimazPageIndicator(pageCount = 4, currentPage = 3)
        }

        composeRule.onAllNodesWithTag(NimazPageIndicatorDotTag)[3]
            .assert(selectedDots())
    }

    @Test
    fun `renders nothing for a single page`() {
        composeRule.setThemedContent {
            NimazPageIndicator(pageCount = 1, currentPage = 0)
        }

        dots().assertCountEquals(0)
    }

    @Test
    fun `renders nothing for zero pages`() {
        composeRule.setThemedContent {
            NimazPageIndicator(pageCount = 0, currentPage = 0)
        }

        dots().assertCountEquals(0)
        composeRule.onRoot().assertExists()
    }

    @Test
    fun `pager-state overload tracks the state's current page`() {
        composeRule.setThemedContent {
            val state = rememberNimazPagerState(initialPage = 1) { 3 }
            NimazPageIndicator(state = state)
        }

        dots().assertCountEquals(3)
        composeRule.onAllNodesWithTag(NimazPageIndicatorDotTag)[1].assert(selectedDots())
    }

    @Test
    fun `follow-drag keeps one dot per page and selects the current one`() {
        // The continuous rendering shares the pill between two dots mid-swipe, but a screen
        // reader should still hear exactly one position.
        composeRule.setThemedContent {
            val state = rememberNimazPagerState(initialPage = 2) { 5 }
            NimazPageIndicator(state = state, followDrag = true, glowColor = androidx.compose.ui.graphics.Color.Yellow)
        }

        dots().assertCountEquals(5)
        composeRule.onAllNodesWithTag(NimazPageIndicatorDotTag).filter(selectedDots()).assertCountEquals(1)
        composeRule.onAllNodesWithTag(NimazPageIndicatorDotTag)[2].assert(selectedDots())
    }

    @Test
    fun `follow-drag draws nothing for a single page`() {
        composeRule.setThemedContent {
            val state = rememberNimazPagerState { 1 }
            NimazPageIndicator(state = state, followDrag = true)
        }

        dots().assertCountEquals(0)
    }
}
