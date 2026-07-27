package com.arshadshah.nimaz.presentation.components.organisms

import com.arshadshah.nimaz.domain.model.Khatam
import com.arshadshah.nimaz.domain.model.MushafPagination
import com.arshadshah.nimaz.domain.model.MushafScript
import com.arshadshah.nimaz.domain.model.PageAyahRange

/**
 * A [MushafPagination] over [pageCount] pages that partition ayahs 1..6236 contiguously —
 * the shape of both real editions' page tables, at whatever size a test needs. Deliberately
 * *not* 604, so a test that still leaks the old hardcoded Madani page table fails loudly.
 */
internal fun paginationOf(
    pageCount: Int,
    script: MushafScript = MushafScript.INDOPAK_16
): MushafPagination {
    val total = Khatam.TOTAL_QURAN_AYAHS
    val ranges = (1..pageCount).map { page ->
        val min = (page - 1).toLong() * total / pageCount + 1
        val max = page.toLong() * total / pageCount
        PageAyahRange(
            page = page,
            minAyahId = min.toInt(),
            maxAyahId = max.toInt(),
            ayahCount = (max - min + 1).toInt()
        )
    }
    return MushafPagination.from(script, ranges)
}
