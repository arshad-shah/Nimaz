package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuranJuzGridTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `renders juz medallions and names`() {
        composeRule.setThemedContent {
            JuzGrid(onNavigateToJuz = {})
        }

        // Medallion numbers (Column is not lazy — all 30 compose).
        composeRule.onNodeWithText("1").assertExists()
        composeRule.onNodeWithText("2").assertExists()
        composeRule.onNodeWithText("5").assertExists()
        // Juz names — Arabic first-word of each juz.
        composeRule.onNodeWithText("الم").assertExists()   // juz 1
        composeRule.onNodeWithText("حم").assertExists()    // juz 26
    }

    @Test
    fun `renders page range badges for a juz`() {
        composeRule.setThemedContent {
            JuzGrid(onNavigateToJuz = {})
        }

        // Page range renders as separate cutout badges. Juz 3 spans pages 42–61
        // (both > 30, so unambiguous vs. the 1..30 medallion numbers).
        composeRule.onNodeWithText("42").assertExists()
        composeRule.onNodeWithText("61").assertExists()
    }

    @Test
    fun `clicking a juz cell invokes onNavigateToJuz with its number`() {
        var navigatedTo = -1
        composeRule.setThemedContent {
            JuzGrid(onNavigateToJuz = { navigatedTo = it })
        }

        composeRule.onNodeWithText("3").performClick()
        assertThat(navigatedTo).isEqualTo(3)
    }

    @Test
    fun `renders with selected juz highlighted`() {
        composeRule.setThemedContent {
            JuzGrid(onNavigateToJuz = {}, selectedJuzNumber = 2)
        }

        // Selection only changes styling; the cell still renders its number.
        composeRule.onNodeWithText("2").assertExists()
    }

    @Test
    fun `renders with khatam active`() {
        composeRule.setThemedContent {
            JuzGrid(
                onNavigateToJuz = {},
                isKhatamActive = true,
                khatamReadAyahIds = setOf(1, 2, 3)
            )
        }

        composeRule.onNodeWithText("1").assertExists()
    }

    @Test
    fun `page range badges follow the active edition`() {
        // A 300-page edition: juz 2 can no longer start on the Madani page 22 (#325).
        val pagination = paginationOf(300)
        assertThat(pagination.juzStartPage(2)).isNotEqualTo(22)

        composeRule.setThemedContent {
            JuzGrid(onNavigateToJuz = {}, pagination = pagination)
        }

        // Page numbers repeat across the 30 cards (one juz's end page is often the next
        // one's start), so match on "at least one node" rather than exactly one.
        fun textCount(text: String) =
            composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().size

        assertThat(textCount(pagination.juzStartPage(2).toString())).isAtLeast(1)
        assertThat(textCount("300")).isAtLeast(1)   // juz 30 ends the mushaf
        assertThat(textCount("604")).isEqualTo(0)   // …never the Madani total
    }
}
