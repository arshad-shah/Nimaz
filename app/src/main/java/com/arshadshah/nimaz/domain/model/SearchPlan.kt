package com.arshadshah.nimaz.domain.model

/**
 * A retrieval plan produced by the AI for a user's question (the `search-plan`
 * capability). The app uses it to fetch matching records from its LOCAL
 * database — no answer text is generated here.
 *
 *  - [terms]     keyword/phrase terms to run through the local Quran/Hadith/Dua
 *                search (substring match). Drives both the results list and the
 *                evidence gathered for a grounded answer.
 *  - [quranRefs] specific Quran ayat the model judged directly relevant. Hadith
 *                and Dua are addressed by opaque local IDs the model can't know,
 *                so they are only ever reached through [terms].
 */
data class SearchPlan(
    val terms: List<String>,
    val quranRefs: List<CitationId.Quran>,
) {
    val isEmpty: Boolean get() = terms.isEmpty() && quranRefs.isEmpty()
}
