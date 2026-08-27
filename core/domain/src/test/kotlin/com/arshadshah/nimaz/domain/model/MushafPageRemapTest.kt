package com.arshadshah.nimaz.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Switching Mushaf edition must keep the reader on the same *text*, not the same page number.
 *
 * A page number is meaningless without its edition. Page 500 of the 604-page Madani mushaf and
 * page 500 of the 847-page 13-line IndoPak are hundreds of ayahs apart, and Madani page 600
 * does not exist at all in the 548-page 16-line edition.
 *
 * `QuranViewModel.reloadReaderContent` re-issued `loadPage(target.number)` after a script
 * change — the same integer, against the new edition. So changing the layout while reading
 * silently threw the reader to unrelated text, or to a page the edition does not have, which
 * loads nothing and renders blank.
 *
 * The fix is to repaginate the *position*: resolve the page to an ayah in the old edition and
 * ask the new one which page carries it.
 */
class MushafPageRemapTest {

    /** [pages] pages of equal size, covering the whole Quran. */
    private fun pagination(script: MushafScript, pages: Int): MushafPagination {
        val perPage = Khatam.TOTAL_QURAN_AYAHS / pages
        val ranges = (1..pages).map { page ->
            val min = (page - 1) * perPage + 1
            val max = if (page == pages) Khatam.TOTAL_QURAN_AYAHS else page * perPage
            PageAyahRange(page, min, max, max - min + 1)
        }
        return MushafPagination.from(script, ranges)
    }

    private val madani = pagination(MushafScript.MADANI, 604)
    private val indopak16 = pagination(MushafScript.INDOPAK_16, 548)
    private val indopak13 = pagination(MushafScript.INDOPAK_13, 847)

    @Test
    fun `the page the reader lands on carries the text they were on`() {
        // The invariant, rather than an arithmetic guess: whatever page comes back must
        // actually print the ayah the source page opened with. Asserted in both directions
        // so a coarser and a finer edition are both covered.
        listOf(indopak16, indopak13).forEach { target ->
            (1..604 step 53).forEach { page ->
                val openingAyah = madani.rangeFor(page)!!.minAyahId
                val remapped = target.pageMatching(page, madani)

                assertThat(remapped).isNotNull()
                val landed = target.rangeFor(remapped!!)!!
                assertThat(openingAyah).isAtLeast(landed.minAyahId)
                assertThat(openingAyah).isAtMost(landed.maxAyahId)
            }
        }
    }

    @Test
    fun `a finer edition maps a page further into the book than a coarser one`() {
        // 302 of 604 is mid-Quran; the 847-page edition must place it past its own midpoint
        // and the 548-page one before its own, or the switch has lost the position.
        val finer = indopak13.pageMatching(302, madani)!!
        val coarser = indopak16.pageMatching(302, madani)!!

        assertThat(finer).isGreaterThan(coarser)
        assertThat(finer).isGreaterThan(302)
        assertThat(coarser).isLessThan(302)
    }

    @Test
    fun `a page past the end of the target edition still resolves`() {
        // Madani 600 does not exist in the 548-page edition. Re-issuing the number loads
        // nothing; resolving through the ayah lands near the end, where the reader was.
        val remapped = indopak16.pageMatching(600, madani)

        assertThat(remapped).isNotNull()
        assertThat(remapped!!).isAtMost(548)
        assertThat(remapped).isAtLeast(540)
    }

    @Test
    fun `the first and last pages map to the first and last pages`() {
        assertThat(indopak16.pageMatching(1, madani)).isEqualTo(1)
        assertThat(indopak16.pageMatching(604, madani)).isEqualTo(548)
        assertThat(indopak13.pageMatching(1, madani)).isEqualTo(1)
        assertThat(indopak13.pageMatching(604, madani)).isEqualTo(847)
    }

    @Test
    fun `remapping within one edition is the identity`() {
        (1..604 step 97).forEach { page ->
            assertThat(madani.pageMatching(page, madani)).isEqualTo(page)
        }
    }

    @Test
    fun `a round trip through another edition stays close to where it started`() {
        // Coarser pagination loses precision, so this cannot be exact — but it must not
        // drift by more than the page it passed through.
        (50..550 step 100).forEach { page ->
            val there = indopak16.pageMatching(page, madani)!!
            val back = madani.pageMatching(there, indopak16)!!
            assertThat(back).isIn((page - 3)..(page + 3))
        }
    }

    @Test
    fun `a page the source edition does not have resolves to nothing`() {
        assertThat(indopak16.pageMatching(0, madani)).isNull()
        assertThat(indopak16.pageMatching(605, madani)).isNull()
        assertThat(indopak16.pageMatching(-1, madani)).isNull()
    }

    @Test
    fun `an edition whose ranges have not loaded cannot answer`() {
        // Nothing is known about where a page sits, in either direction, so the caller must
        // fall back rather than be handed a guess.
        val notReady = MushafPagination.fallback(MushafScript.INDOPAK_16)

        assertThat(notReady.pageMatching(300, madani)).isNull()
        assertThat(madani.pageMatching(300, notReady)).isNull()
    }

    @Test
    fun `the Madani fallback has no ranges, so it cannot be a source either`() {
        // It reports `isReady` because its printed juz table is real data, but it carries no
        // page-to-ayah ranges — `isReady` is not the same question as "can I remap".
        val fallback = MushafPagination.fallback(MushafScript.MADANI)

        assertThat(fallback.isReady).isTrue()
        assertThat(indopak16.pageMatching(300, fallback)).isNull()
    }

    @Test
    fun `remapping is monotonic across the whole edition`() {
        // Two pages in order must stay in order, or the reader jumps backwards on a switch.
        var previous = 0
        (1..604).forEach { page ->
            val remapped = indopak13.pageMatching(page, madani)!!
            assertThat(remapped).isAtLeast(previous)
            previous = remapped
        }
    }
}
