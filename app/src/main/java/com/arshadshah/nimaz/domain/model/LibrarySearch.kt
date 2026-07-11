package com.arshadshah.nimaz.domain.model

/**
 * Ranked results of one local library search across all content sources.
 * Produced by [com.arshadshah.nimaz.domain.usecase.SearchLibraryUseCase];
 * each list is already ordered by relevance and capped.
 */
data class LibrarySearchResults(
    val quran: List<QuranSearchResult> = emptyList(),
    val surahs: List<Surah> = emptyList(),
    val hadith: List<HadithSearchResult> = emptyList(),
    val duas: List<DuaSearchResult> = emptyList(),
) {
    val isEmpty: Boolean
        get() = quran.isEmpty() && surahs.isEmpty() && hadith.isEmpty() && duas.isEmpty()

    companion object {
        val EMPTY = LibrarySearchResults()
    }
}
