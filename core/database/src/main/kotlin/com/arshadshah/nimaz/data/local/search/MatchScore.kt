package com.arshadshah.nimaz.data.local.search

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.ln

/**
 * Relevance of one FTS4 match, from its `matchinfo(search_index, 'pcx')` blob.
 *
 * The blob is unsigned 32-bit integers in the host's byte order:
 *
 *  - `p` — the number of phrases in the MATCH expression (`a* OR b* c*` has three);
 *  - `c` — the number of columns (one: `body`);
 *  - then, per phrase and column, three integers: hits in **this** row, hits across all
 *    rows, and the number of rows with at least one hit.
 *
 * Those are exactly the inputs to TF-IDF, and nothing more: the artifact's index is built
 * with `matchinfo=fts3`, which stores no document lengths, so BM25 is not available here.
 *
 * Score = Σ over phrases with a hit of `tf × ln(1 + documents / rowsWithHit)`, where
 * `tf = hitsHere × (K + 1) / (hitsHere + K)` — BM25's saturating term frequency, without the
 * length part it has no data for. One mention scores 1, two 1.4, five 1.8, twenty 2.1: a long
 * hadith that repeats a word twenty times does not bury five short ones that each say it once.
 * A plain `1 + ln(hits)` was tried first, on the data-v10 artifact, and kept letting the
 * longest hadith in the corpus top every common-word search. The second factor makes a hit on
 * a rare word worth more than a hit on a common one.
 */
internal object MatchScore {

    /** BM25's usual k1: how quickly repeated hits stop adding to the score. */
    private const val K = 1.2

    fun of(info: ByteArray, documents: Int): Double {
        if (info.size < 8) return 0.0
        val ints = ByteBuffer.wrap(info).order(ByteOrder.nativeOrder()).asIntBuffer()
        val phrases = ints.get(0)
        val columns = ints.get(1)
        if (phrases <= 0 || columns <= 0) return 0.0
        if (ints.limit() < 2 + phrases * columns * 3) return 0.0

        var score = 0.0
        for (phrase in 0 until phrases) {
            for (column in 0 until columns) {
                val base = 2 + (phrase * columns + column) * 3
                val here = ints.get(base).toUInt().toDouble()
                val rowsWithHit = ints.get(base + 2).toUInt().toDouble()
                if (here <= 0.0 || rowsWithHit <= 0.0) continue
                val tf = here * (K + 1) / (here + K)
                score += tf * ln(1 + documents / rowsWithHit)
            }
        }
        return score
    }
}
