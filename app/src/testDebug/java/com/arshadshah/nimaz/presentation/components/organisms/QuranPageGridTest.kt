package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuranPageGridTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `renders the first juz header`() {
        composeRule.setThemedContent {
            LazyColumn {
                pageGridItems(onNavigateToPage = {})
            }
        }

        // Juz header text is the literal "Juz $juz".
        composeRule.onNodeWithText("Juz 1").assertExists()
    }

    @Test
    fun `page with a surah badge renders its number and chip`() {
        composeRule.setThemedContent {
            LazyColumn {
                pageGridItems(
                    onNavigateToPage = {},
                    // Page 1 (within Juz 1: pages 1..21) becomes a full-width
                    // card showing its number and the supplied surah chip(s).
                    surahStartPageMap = mapOf(1 to listOf("Al-Fatihah"))
                )
            }
        }

        composeRule.onNodeWithText("1").assertExists()
        composeRule.onNodeWithText("Al-Fatihah").assertExists()
    }

    @Test
    fun `clicking a badged page invokes onNavigateToPage with its number`() {
        var navigatedTo = -1
        composeRule.setThemedContent {
            LazyColumn {
                pageGridItems(
                    onNavigateToPage = { navigatedTo = it },
                    surahStartPageMap = mapOf(1 to listOf("Al-Fatihah"))
                )
            }
        }

        composeRule.onNodeWithText("Al-Fatihah").performClick()
        assertThat(navigatedTo).isEqualTo(1)
    }

    @Test
    fun `clicking a compact grid page invokes onNavigateToPage`() {
        var navigatedTo = -1
        composeRule.setThemedContent {
            LazyColumn {
                // No surah map -> pages collapse into a compact FlowRow grid.
                pageGridItems(onNavigateToPage = { navigatedTo = it })
            }
        }

        // Page 1 is in the first compact grid of Juz 1 and visible at the top.
        composeRule.onNodeWithText("1").performClick()
        assertThat(navigatedTo).isEqualTo(1)
    }

    @Test
    fun `computeJuzHeaderIndices places juz 1 at index 0 with no surah map`() {
        val indices = computeJuzHeaderIndices(emptyMap())

        assertThat(indices[1]).isEqualTo(0)
        // 30 juz headers are computed.
        assertThat(indices).hasSize(30)
        // Indices are strictly increasing across juz.
        assertThat(indices[2]!!).isGreaterThan(indices[1]!!)
        assertThat(indices[30]!!).isGreaterThan(indices[2]!!)
    }

    @Test
    fun `computeJuzHeaderIndices shifts later headers when pages carry badges`() {
        val withBadges = computeJuzHeaderIndices(mapOf(1 to listOf("Al-Fatihah")))
        val withoutBadges = computeJuzHeaderIndices(emptyMap())

        assertThat(withBadges[1]).isEqualTo(0)
        // A badged page in Juz 1 splits a compact grid into extra items,
        // pushing Juz 2's header to a higher index than the no-badge case.
        assertThat(withBadges[2]!!).isAtLeast(withoutBadges[2]!!)
    }
}
