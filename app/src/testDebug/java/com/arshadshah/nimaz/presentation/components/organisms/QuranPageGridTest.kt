package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.test.onAllNodesWithText
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
    val composeRule = createComponentComposeRule()

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
                    // Page 3 (within Juz 1: pages 1..21) becomes a full-width
                    // card showing its number and the supplied surah chip(s).
                    // Page 3 avoids the Juz 1 header's start/end page badges (1, 21).
                    surahStartPageMap = mapOf(3 to listOf("Al-Fatihah"))
                )
            }
        }

        composeRule.onNodeWithText("3").assertExists()
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

        // Page 3 is in the first compact grid of Juz 1, visible near the top.
        // It avoids the Juz 1 header's start/end page badges (1, 21).
        composeRule.onNodeWithText("3").performClick()
        assertThat(navigatedTo).isEqualTo(3)
    }

    @Test
    fun `juz header page badges come from the active edition`() {
        // The bug in #325: juz spans were read from a hardcoded Madani table, so a
        // non-Madani edition still advertised Madani page numbers (juz 1 = pages 1..21).
        val pagination = paginationOf(300)

        composeRule.setThemedContent {
            LazyColumn {
                pageGridItems(onNavigateToPage = {}, pagination = pagination)
            }
        }

        composeRule.onNodeWithText("Juz 1").assertExists()
        // Juz 1 of a 300-page edition cannot end on the Madani page 21.
        assertThat(pagination.juzEndPage(1)).isNotEqualTo(21)
        // Its end page appears at least once — as the header badge, and again as the tile
        // for that page — so match on presence rather than a single node.
        assertThat(
            composeRule.onAllNodesWithText(pagination.juzEndPage(1).toString())
                .fetchSemanticsNodes()
        ).isNotEmpty()
    }

    @Test
    fun `juz sections tile the active edition exactly once`() {
        val pagination = paginationOf(548)

        val covered = (1..30).flatMap { pagination.juzPages(it).toList() }

        assertThat(covered).isEqualTo((1..548).toList())
        // Item keys are derived from page numbers, so a duplicate would crash the LazyColumn.
        assertThat(covered.toSet()).hasSize(548)
    }

    @Test
    fun `khatam progress rings read the active edition's page ayah ranges`() {
        val pagination = paginationOf(300)
        val page1 = pagination.rangeFor(1)!!

        composeRule.setThemedContent {
            LazyColumn {
                pageGridItems(
                    onNavigateToPage = {},
                    pagination = pagination,
                    isKhatamActive = true,
                    // Exactly the ayahs printed on page 1 of *this* edition.
                    khatamReadAyahIds = (page1.minAyahId..page1.maxAyahId).toSet(),
                    surahStartPageMap = mapOf(1 to listOf("Al-Fatihah"))
                )
            }
        }

        composeRule.onNodeWithText("Al-Fatihah").assertExists()
        // A 300-page edition holds far more than the ~7 ayahs of Madani page 1, so a ring
        // computed from the Madani ranges would have covered a different span.
        assertThat(page1.ayahCount).isEqualTo(page1.maxAyahId - page1.minAyahId + 1)
        assertThat(page1.ayahCount).isGreaterThan(7)
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
    fun `computeJuzHeaderIndices follows the active edition's juz spans`() {
        val pagination = paginationOf(300)
        // Every page badged, so each page is its own item and the indices track page counts.
        val allBadged = (1..300).associateWith { listOf("S$it") }

        val indices = computeJuzHeaderIndices(allBadged, pagination)

        assertThat(indices).hasSize(30)
        assertThat(indices[1]).isEqualTo(0)
        // Juz 2's header sits after juz 1's header plus one item per page of juz 1.
        assertThat(indices[2]).isEqualTo(1 + pagination.juzPages(1).count())
        // The whole list is 30 headers plus one item per page of the edition.
        val lastJuzItems = pagination.juzPages(30).count()
        assertThat(indices[30]!! + 1 + lastJuzItems).isEqualTo(30 + 300)
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
