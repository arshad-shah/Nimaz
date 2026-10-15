package com.arshadshah.nimaz.domain.search

/**
 * What a search pass asks the index, beyond "these words".
 *
 * `SearchLibraryUseCase` runs several passes over the same query and scores each kind of hit
 * differently, so the passes have to ask *different* questions. The query text alone cannot say
 * which question it is — "patience hardship" is both a phrase and a pair of words — so the pass
 * says so explicitly.
 */
enum class SearchMatch {
    /**
     * One word: that word, any ending, plus its synonyms (`prayers` also finds `prayer`, `salah`
     * also finds `salat`). Several words: the exact phrase, adjacent and in order.
     *
     * The default everywhere — every search box in the app that is not the library search asks
     * exactly this.
     */
    AUTO,

    /**
     * Every word, in any order and anywhere in the text, each with its endings and synonyms.
     *
     * The middle tier between an exact phrase and a single word: "patience hardship" finds a
     * hadith that says "be patient in times of hardship", which the phrase pass cannot reach and
     * which the single-word passes would rank no higher than any hadith mentioning "patience".
     */
    ALL_WORDS,
}
