package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuranJuzGridTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `renders juz numbers and labels`() {
        composeRule.setThemedContent {
            JuzGrid(onNavigateToJuz = {})
        }

        // First row of the grid (juz 1..5) is visible without scrolling.
        composeRule.onNodeWithText("1").assertExists()
        composeRule.onNodeWithText("2").assertExists()
        composeRule.onNodeWithText("5").assertExists()
        // R.string.quran_home_juz_label == "Juz" — one per cell.
        composeRule.onAllNodesWithText("Juz")[0].assertExists()
    }

    @Test
    fun `renders page range for first juz`() {
        composeRule.setThemedContent {
            JuzGrid(onNavigateToJuz = {})
        }

        // quran_home_page_range_format == "p. %1$d–%2$d"
        // Juz 1: startPage 1, endPage = juzStartPages[1] - 1 = 21.
        composeRule.onNodeWithText("p. 1–21").assertExists()
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
    fun `getJuzForPage maps pages to the correct juz`() {
        assertThat(getJuzForPage(1)).isEqualTo(1)
        assertThat(getJuzForPage(21)).isEqualTo(1)
        assertThat(getJuzForPage(22)).isEqualTo(2)
        assertThat(getJuzForPage(604)).isEqualTo(30)
    }

    @Test
    fun `getJuzStartPage and getJuzEndPage return expected bounds`() {
        assertThat(getJuzStartPage(1)).isEqualTo(1)
        assertThat(getJuzEndPage(1)).isEqualTo(21)
        assertThat(getJuzStartPage(2)).isEqualTo(22)
        assertThat(getJuzEndPage(30)).isEqualTo(604)
    }
}
