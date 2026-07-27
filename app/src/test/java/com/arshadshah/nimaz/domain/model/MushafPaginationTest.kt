package com.arshadshah.nimaz.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The script-aware page↔ayah mapping behind #325.
 *
 * Before this existed, every "which pages does the Quran have" question was answered from
 * Madani-only sources (the `ayahs.page` column, `surahs.start_page`, and a hardcoded juz
 * page table ending in the literal 604), so the Page tab listed 604 tiles and khatam page
 * progress pointed at the wrong ayahs whenever the 16-line IndoPak edition was selected.
 */
class MushafPaginationTest {

    /**
     * A synthetic but structurally faithful pagination: [pageCount] pages that partition
     * ayahs 1..6236 contiguously, mirroring the real tables (every ayah on exactly one
     * page, ayah ids non-decreasing across pages).
     */
    private fun evenRanges(pageCount: Int): List<PageAyahRange> {
        val total = Khatam.TOTAL_QURAN_AYAHS
        return (1..pageCount).map { page ->
            val min = (page - 1).toLong() * total / pageCount + 1
            val max = page.toLong() * total / pageCount
            PageAyahRange(
                page = page,
                minAyahId = min.toInt(),
                maxAyahId = max.toInt(),
                ayahCount = (max - min + 1).toInt()
            )
        }
    }

    private fun indopak(pageCount: Int = 548) =
        MushafPagination.from(MushafScript.INDOPAK_16, evenRanges(pageCount))

    @Test
    fun `total pages comes from the supplied ranges, not the script literal`() {
        val pagination = MushafPagination.from(MushafScript.INDOPAK_16, evenRanges(548))

        assertThat(pagination.totalPages).isEqualTo(548)
        assertThat(pagination.isDerived).isTrue()
        assertThat(pagination.isReady).isTrue()
        assertThat(pagination.pages).isEqualTo(1..548)
    }

    @Test
    fun `page ranges are looked up by page number`() {
        val pagination = indopak()

        assertThat(pagination.rangeFor(1)!!.minAyahId).isEqualTo(1)
        assertThat(pagination.rangeFor(548)!!.maxAyahId).isEqualTo(Khatam.TOTAL_QURAN_AYAHS)
        assertThat(pagination.rangeFor(0)).isNull()
        assertThat(pagination.rangeFor(549)).isNull()
    }

    @Test
    fun `every ayah resolves to the page whose range contains it`() {
        val pagination = indopak()

        for (page in 1..548) {
            val range = pagination.rangeFor(page)!!
            assertThat(pagination.pageForAyah(range.minAyahId)).isEqualTo(page)
            assertThat(pagination.pageForAyah(range.maxAyahId)).isEqualTo(page)
        }
        assertThat(pagination.pageForAyah(0)).isNull()
        assertThat(pagination.pageForAyah(Khatam.TOTAL_QURAN_AYAHS + 1)).isNull()
    }

    @Test
    fun `juz page boundaries follow the ranges rather than a Madani table`() {
        val pagination = indopak()

        // Juz 1 always opens the mushaf; juz 30 always closes it.
        assertThat(pagination.juzStartPage(1)).isEqualTo(1)
        assertThat(pagination.juzEndPage(30)).isEqualTo(548)
        // Juz 2 starts on the page holding its first ayah (KhatamConstants juz 2 = 149).
        assertThat(pagination.juzStartPage(2))
            .isEqualTo(pagination.pageForAyah(KhatamConstants.JUZ_AYAH_RANGES[1].first))
    }

    @Test
    fun `juz page spans tile the whole mushaf with no gaps or overlaps`() {
        val pagination = indopak()

        val covered = (1..Khatam.TOTAL_JUZ).flatMap { pagination.juzPages(it).toList() }
        assertThat(covered).isEqualTo((1..548).toList())
    }

    @Test
    fun `juz spans stay ordered and non-empty`() {
        val pagination = indopak()

        (1..Khatam.TOTAL_JUZ).forEach { juz ->
            assertThat(pagination.juzEndPage(juz)).isAtLeast(pagination.juzStartPage(juz))
        }
        (1..<Khatam.TOTAL_JUZ).forEach { juz ->
            assertThat(pagination.juzStartPage(juz + 1))
                .isGreaterThan(pagination.juzStartPage(juz))
        }
    }

    @Test
    fun `juzForPage is the inverse of the juz page spans`() {
        val pagination = indopak()

        (1..Khatam.TOTAL_JUZ).forEach { juz ->
            assertThat(pagination.juzForPage(pagination.juzStartPage(juz))).isEqualTo(juz)
            assertThat(pagination.juzForPage(pagination.juzEndPage(juz))).isEqualTo(juz)
        }
        assertThat(pagination.juzForPage(1)).isEqualTo(1)
        assertThat(pagination.juzForPage(548)).isEqualTo(30)
    }

    @Test
    fun `Madani fallback resolves juz by page from the printed table`() {
        val pagination = MushafPagination.fallback(MushafScript.MADANI)

        assertThat(pagination.juzForPage(1)).isEqualTo(1)
        assertThat(pagination.juzForPage(21)).isEqualTo(1)
        assertThat(pagination.juzForPage(22)).isEqualTo(2)
        assertThat(pagination.juzForPage(604)).isEqualTo(30)
    }

    @Test
    fun `a coarser edition produces correspondingly fewer pages`() {
        val pagination = MushafPagination.from(MushafScript.INDOPAK_16, evenRanges(300))

        assertThat(pagination.totalPages).isEqualTo(300)
        assertThat(pagination.juzEndPage(30)).isEqualTo(300)
    }

    @Test
    fun `Madani falls back to the printed juz page table before ranges load`() {
        val pagination = MushafPagination.fallback(MushafScript.MADANI)

        assertThat(pagination.isDerived).isFalse()
        // Still usable: the printed Madani juz start pages are real reference data.
        assertThat(pagination.isReady).isTrue()
        assertThat(pagination.totalPages).isEqualTo(604)
        assertThat(pagination.juzStartPage(1)).isEqualTo(1)
        assertThat(pagination.juzStartPage(2)).isEqualTo(22)
        assertThat(pagination.juzEndPage(1)).isEqualTo(21)
        assertThat(pagination.juzEndPage(30)).isEqualTo(604)
    }

    @Test
    fun `IndoPak without ranges reports itself as not ready instead of guessing`() {
        val pagination = MushafPagination.fallback(MushafScript.INDOPAK_16)

        assertThat(pagination.isDerived).isFalse()
        // The Madani juz table would be plain wrong here, so callers must wait.
        assertThat(pagination.isReady).isFalse()
        assertThat(pagination.totalPages).isEqualTo(548)
        assertThat(pagination.pageForAyah(1)).isNull()
    }

    @Test
    fun `unsorted or sparse input still yields a sane pagination`() {
        val shuffled = evenRanges(548).shuffled()

        val pagination = MushafPagination.from(MushafScript.INDOPAK_16, shuffled)

        assertThat(pagination.totalPages).isEqualTo(548)
        assertThat(pagination.pageForAyah(1)).isEqualTo(1)
        assertThat(pagination.pageForAyah(Khatam.TOTAL_QURAN_AYAHS)).isEqualTo(548)
    }

    @Test
    fun `out of range juz numbers are clamped rather than crashing`() {
        val pagination = indopak()

        assertThat(pagination.juzStartPage(0)).isEqualTo(1)
        assertThat(pagination.juzStartPage(31)).isEqualTo(pagination.juzStartPage(30))
        assertThat(pagination.juzEndPage(31)).isEqualTo(548)
    }
}
