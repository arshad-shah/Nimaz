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
}
