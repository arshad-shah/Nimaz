package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The pager dots.
 *
 * `pageCount <= 1` returns before drawing anything, and that early return is the point: a
 * one-page carousel with a single dot under it tells the reader there is somewhere else to go
 * when there is not. The Home carousel's page count is data-driven, so one-page is a real state.
 *
 * The current dot is also **wider** rather than merely a different colour, which is what makes the
 * position readable without relying on colour alone — and it reports `selected` so a screen reader
 * can say where in the set the reader is.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class NimazPageIndicatorVariantsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `one page draws no dots at all`() {
        composeRule.setThemedContent {
            Column {
                NimazPageIndicator(pageCount = 1, currentPage = 0)
                NimazPageIndicator(pageCount = 0, currentPage = 0)
            }
        }

        composeRule.onAllNodesWithTag(NimazPageIndicatorDotTag).assertCountEquals(0)
    }

    @Test
    fun `a dot is drawn per page`() {
        composeRule.setThemedContent {
            NimazPageIndicator(pageCount = 5, currentPage = 2)
        }

        composeRule.onAllNodesWithTag(NimazPageIndicatorDotTag).assertCountEquals(5)
    }

    @Test
    fun `the current page's dot is the selected one`() {
        composeRule.setThemedContent {
            NimazPageIndicator(pageCount = 3, currentPage = 0)
        }

        composeRule.onAllNodesWithTag(NimazPageIndicatorDotTag).onFirst().assertIsSelected()
    }

    @Test
    fun `every metric a caller can set is honoured`() {
        composeRule.setThemedContent {
            NimazPageIndicator(
                pageCount = 4,
                currentPage = 1,
                activeColor = Color.Magenta,
                inactiveColor = Color.Gray,
                dotSize = 12.dp,
                activeWidth = 40.dp,
                spacing = 6.dp,
            )
        }

        composeRule.onAllNodesWithTag(NimazPageIndicatorDotTag).assertCountEquals(4)
    }

    @Test
    fun `the pager overload reads its count and position from the pager`() {
        // The overload every carousel actually calls — it forwards eight arguments, and a
        // forwarding mistake shows up as dots that do not track the page.
        composeRule.setThemedContent {
            val state = rememberPagerState(initialPage = 1) { 3 }
            Column {
                HorizontalPager(state = state) { page -> Text("page $page") }
                NimazPageIndicator(
                    state = state,
                    activeColor = Color.Magenta,
                    inactiveColor = Color.Gray,
                    dotSize = 10.dp,
                    activeWidth = 24.dp,
                    spacing = 4.dp,
                )
            }
        }

        composeRule.onAllNodesWithTag(NimazPageIndicatorDotTag).assertCountEquals(3)
    }
}
