package com.arshadshah.nimaz.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * "Go to page" must be bounded by the edition the reader is actually looking at.
 *
 * [MushafPagination] exists to be the single source of truth for "which pages does this Quran
 * have" — its own KDoc names *the jump-to-page validation* as one of the two places
 * [MushafScript] used to be consulted for a raw count. Those two were the ones #325 missed:
 * `QuranHomeScreen` validated the input against `state.mushafScript.totalPages` and
 * `QuranReaderUiState.totalPages` bounded the pager the same way, while the Page tab's grid,
 * its juz sections and the surah badges all read `state.pagination.totalPages`.
 *
 * The two are not the same number. `MushafScript.totalPages` is a constant declared on the
 * enum; `MushafPagination.totalPages` is derived from the edition's real page ranges, which is
 * why `MushafPaginationTest` has a case for an edition whose data yields fewer pages than it
 * declares. And for any non-Madani edition the mapping is **not ready** until its ranges load,
 * a window in which the Page tab shows a spinner while the jump field was happily accepting
 * page numbers on the declared count's authority.
 *
 * The old check also failed silently: `if (page in 1..total) onNavigateToPage(page)` did
 * nothing at all for anything outside the range — no error, no message — so a wrong bound was
 * invisible.
 */
class MushafPageInputTest {

    private fun evenRanges(pages: Int): List<PageAyahRange> {
        val perPage = Khatam.TOTAL_QURAN_AYAHS / pages
        return (1..pages).map { page ->
            val min = (page - 1) * perPage + 1
            val max = if (page == pages) Khatam.TOTAL_QURAN_AYAHS else page * perPage
            PageAyahRange(page = page, minAyahId = min, maxAyahId = max, ayahCount = max - min + 1)
        }
    }

    @Test
    fun `a page inside the edition resolves to itself`() {
        val pagination = MushafPagination.from(MushafScript.MADANI, evenRanges(604))

        assertThat(pagination.pageFromInput("1")).isEqualTo(1)
        assertThat(pagination.pageFromInput("300")).isEqualTo(300)
        assertThat(pagination.pageFromInput("604")).isEqualTo(604)
    }

    @Test
    fun `the bound follows the derived count, not the number declared on the enum`() {
        // The declared constant for this edition is 548; this reader's data paginates to 300.
        val pagination = MushafPagination.from(MushafScript.INDOPAK_16, evenRanges(300))

        assertThat(MushafScript.INDOPAK_16.totalPages).isEqualTo(548)
        assertThat(pagination.totalPages).isEqualTo(300)

        assertThat(pagination.pageFromInput("300")).isEqualTo(300)
        // 400 is a page the old check accepted and this edition does not have.
        assertThat(pagination.pageFromInput("400")).isNull()
        assertThat(pagination.pageFromInput("548")).isNull()
    }

    @Test
    fun `a longer edition accepts pages the Madani count would have rejected`() {
        // The 13-line IndoPak runs to 847 pages; anything past 604 was refused while the
        // bound came from a constant that had not been re-read.
        val pagination = MushafPagination.from(MushafScript.INDOPAK_13, evenRanges(847))

        assertThat(pagination.pageFromInput("700")).isEqualTo(700)
        assertThat(pagination.pageFromInput("847")).isEqualTo(847)
        assertThat(pagination.pageFromInput("848")).isNull()
    }

    @Test
    fun `zero, negatives and out-of-range pages resolve to nothing`() {
        val pagination = MushafPagination.from(MushafScript.MADANI, evenRanges(604))

        assertThat(pagination.pageFromInput("0")).isNull()
        assertThat(pagination.pageFromInput("-5")).isNull()
        assertThat(pagination.pageFromInput("605")).isNull()
    }

    @Test
    fun `junk input resolves to nothing rather than throwing`() {
        val pagination = MushafPagination.from(MushafScript.MADANI, evenRanges(604))

        assertThat(pagination.pageFromInput("")).isNull()
        assertThat(pagination.pageFromInput("   ")).isNull()
        assertThat(pagination.pageFromInput("abc")).isNull()
        assertThat(pagination.pageFromInput("12x")).isNull()
        // Wider than Int — `toIntOrNull` must be what rejects this, not an overflow.
        assertThat(pagination.pageFromInput("99999999999999")).isNull()
    }

    @Test
    fun `surrounding whitespace and leading zeros are accepted`() {
        val pagination = MushafPagination.from(MushafScript.MADANI, evenRanges(604))

        assertThat(pagination.pageFromInput(" 42 ")).isEqualTo(42)
        assertThat(pagination.pageFromInput("007")).isEqualTo(7)
    }

    @Test
    fun `an edition whose ranges have not loaded accepts nothing`() {
        // `isReady` is false here and the Page tab shows a spinner. Letting the jump field
        // navigate on the declared count during that window is what put the two out of step.
        val pagination = MushafPagination.fallback(MushafScript.INDOPAK_16)

        assertThat(pagination.isReady).isFalse()
        assertThat(pagination.pageFromInput("1")).isNull()
        assertThat(pagination.pageFromInput("500")).isNull()
    }

    @Test
    fun `Madani accepts input before its ranges load, because its fallback is real data`() {
        // The printed Madani juz table is reference data, so the default edition stays usable
        // immediately — that asymmetry is deliberate and `isReady` already encodes it.
        val pagination = MushafPagination.fallback(MushafScript.MADANI)

        assertThat(pagination.isReady).isTrue()
        assertThat(pagination.pageFromInput("604")).isEqualTo(604)
        assertThat(pagination.pageFromInput("605")).isNull()
    }

    @Test
    fun `contains agrees with pageFromInput on every boundary`() {
        val pagination = MushafPagination.from(MushafScript.INDOPAK_16, evenRanges(548))

        listOf(0, 1, 2, 547, 548, 549).forEach { page ->
            assertThat(pagination.contains(page))
                .isEqualTo(pagination.pageFromInput(page.toString()) != null)
        }
    }

    @Test
    fun `no page bound is taken from the enum constant outside the domain layer`() {
        // Reads the sources directly, in the shape of WidgetGlyphGuardTest; runs from the
        // module dir. `MushafScript.totalPages` is the *declared* count and belongs to the
        // fallback that builds a MushafPagination — every consumer asks the pagination.
        val offenders = java.io.File("src/main/java/com/arshadshah/nimaz").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.name == "MushafLayout.kt" || it.name == "MushafPagination.kt" }
            // ReadingProgressCalculator is handed a count by its caller, and QuranModels'
            // TOTAL_QURAN_PAGES is the Madani constant used for Madani-only reference data.
            .filterNot { it.name == "QuranModels.kt" }
            .flatMap { file ->
                file.readLines().asSequence().mapIndexedNotNull { index, line ->
                    val trimmed = line.trim()
                    val isComment = trimmed.startsWith("//") || trimmed.startsWith("*") ||
                            trimmed.startsWith("/*")
                    if (!isComment && "cript.totalPages" in line.substringBefore("//")) {
                        "${file.name}:${index + 1}: $trimmed"
                    } else {
                        null
                    }
                }
            }
            .toList()

        assertThat(offenders).isEmpty()
    }
}
