package com.arshadshah.nimaz.domain.model

/**
 * The page↔ayah mapping of one Mushaf edition — the single source of truth for every
 * "which pages does the Quran have / what is on page N / where does juz J start" question
 * (#325).
 *
 * Before this existed, [MushafScript] was consulted only for a raw page *count* (the reader's
 * pager bounds and the jump-to-page validation). Everything that mapped a page to *content*
 * read Madani-only sources — the `ayahs.page` column, `surahs.start_page`, and a hardcoded
 * juz page table ending in the literal `604` — so selecting the 16-line IndoPak edition left
 * the Page tab listing 604 tiles and pointed khatam page progress at the wrong ayahs.
 *
 * Everything here is derived from one ordered list of [PageAyahRange]s, which the data layer
 * produces per script (`ayahs.page` for Madani, the `mushaf_layout_indopak16` table for the
 * 16-line edition). Pure Kotlin, so it is unit-tested directly.
 *
 * ## Boundary convention
 * A juz starts on the page holding its first ayah and ends on the page *before* the next
 * juz starts. When two juz share a page the page belongs to the earlier one, so the 30 juz
 * spans tile `1..totalPages` exactly once — the invariant the Page tab's grouped list keys
 * depend on.
 */
class MushafPagination private constructor(
    val script: MushafScript,
    val totalPages: Int,
    /** Whether the mapping was derived from real page data (vs. a static fallback). */
    val isDerived: Boolean,
    private val rangesByPage: Map<Int, PageAyahRange>,
    private val orderedRanges: List<PageAyahRange>,
    private val juzStarts: List<Int>
) {

    /**
     * Whether callers can render page-level UI. False only for a non-Madani edition whose
     * ranges have not loaded yet — guessing there would print wrong page numbers, so the
     * caller should show a loading state instead.
     */
    val isReady: Boolean get() = juzStarts.isNotEmpty()

    /** Every page of this edition, 1-based. */
    val pages: IntRange get() = 1..totalPages

    /** Whether [page] is a page of this edition. False for every page while not [isReady]. */
    fun contains(page: Int): Boolean = isReady && page in pages

    /**
     * The page a "go to page" input names, or null when it is not a page of this edition.
     *
     * Parsing lives here so the bound cannot drift from the edition again: the jump-to-page
     * field used to validate against [MushafScript.totalPages], the count *declared* on the
     * enum, while the Page tab it sits above listed tiles from [totalPages], the count derived
     * from this edition's real page ranges. Blank, non-numeric and overflowing input resolve to
     * null alongside out-of-range pages, so the caller has one thing to check.
     */
    fun pageFromInput(input: String): Int? =
        input.trim().toIntOrNull()?.takeIf { contains(it) }

    /** The ayah span printed on [page], or null when unknown. */
    fun rangeFor(page: Int): PageAyahRange? = rangesByPage[page]

    /** The page [ayahId] is printed on, or null when unknown / out of range. */
    fun pageForAyah(ayahId: Int): Int? {
        if (orderedRanges.isEmpty()) return null
        // Ranges are contiguous and ordered, so a binary search on minAyahId lands on the
        // page whose span starts at or before the ayah.
        var low = 0
        var high = orderedRanges.lastIndex
        var candidate = -1
        while (low <= high) {
            val mid = (low + high) / 2
            if (orderedRanges[mid].minAyahId <= ayahId) {
                candidate = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        if (candidate < 0) return null
        val range = orderedRanges[candidate]
        return if (ayahId <= range.maxAyahId) range.page else null
    }

    /** First page of [juz] (1-30); clamped for out-of-range input. */
    fun juzStartPage(juz: Int): Int {
        if (juzStarts.isEmpty()) return 1
        return juzStarts[juz.coerceIn(1, Khatam.TOTAL_JUZ) - 1]
    }

    /** Last page of [juz] (1-30) — the page before the next juz opens. */
    fun juzEndPage(juz: Int): Int {
        val clamped = juz.coerceIn(1, Khatam.TOTAL_JUZ)
        if (clamped == Khatam.TOTAL_JUZ) return totalPages
        return (juzStartPage(clamped + 1) - 1).coerceAtLeast(juzStartPage(clamped))
    }

    /** The pages [juz] occupies. */
    fun juzPages(juz: Int): IntRange = juzStartPage(juz)..juzEndPage(juz)

    /** The juz (1-30) whose span contains [page]; 1 when the mapping is unavailable. */
    fun juzForPage(page: Int): Int {
        if (juzStarts.isEmpty()) return 1
        for (index in juzStarts.indices.reversed()) {
            if (page >= juzStarts[index]) return index + 1
        }
        return 1
    }

    companion object {
        /**
         * Juz start pages of the printed Madani (Uthmani) mushaf — real reference data, used
         * as the fallback for [MushafScript.MADANI] in the window before the page ranges
         * have loaded, so the default edition never renders an empty Page tab.
         */
        val MADANI_JUZ_START_PAGES: List<Int> = listOf(
            1, 22, 42, 62, 82, 102, 121, 142, 162, 182,
            201, 222, 242, 262, 282, 302, 322, 342, 362, 382,
            402, 422, 442, 462, 482, 502, 522, 542, 562, 582
        )

        /**
         * Builds the mapping for [script] from its [pageRanges]. Pass an empty list to get
         * the fallback described on [isReady].
         */
        fun from(script: MushafScript, pageRanges: List<PageAyahRange>): MushafPagination {
            val ordered = pageRanges.sortedBy { it.page }
            if (ordered.isEmpty()) return fallback(script)

            val total = ordered.last().page
            val byPage = ordered.associateBy { it.page }
            // A juz opens on the page carrying its first ayah. Resolved once here so the
            // Page tab does not binary-search per juz on every recomposition.
            var previous = 1
            val starts = KhatamConstants.JUZ_AYAH_RANGES.map { (startAyah, _) ->
                val page = pageOf(ordered, startAyah) ?: previous
                // Guard against degenerate data: a juz can never open before its predecessor.
                page.coerceAtLeast(previous).also { previous = it }
            }

            return MushafPagination(
                script = script,
                totalPages = total,
                isDerived = true,
                rangesByPage = byPage,
                orderedRanges = ordered,
                juzStarts = starts
            )
        }

        /**
         * The mapping to use before page data is available: the printed juz table for
         * Madani, and a not-[isReady] placeholder for any other edition.
         */
        fun fallback(script: MushafScript): MushafPagination = MushafPagination(
            script = script,
            totalPages = script.totalPages,
            isDerived = false,
            rangesByPage = emptyMap(),
            orderedRanges = emptyList(),
            juzStarts = if (script == MushafScript.MADANI) MADANI_JUZ_START_PAGES else emptyList()
        )

        private fun pageOf(ordered: List<PageAyahRange>, ayahId: Int): Int? =
            ordered.lastOrNull { it.minAyahId <= ayahId }
                ?.takeIf { ayahId <= it.maxAyahId }
                ?.page
    }
}
