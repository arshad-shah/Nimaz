package com.arshadshah.nimaz.domain.model

/** A search hit from any corpus — belongs beside `LibrarySearchResults`, not in a ViewModel. */
sealed class UnifiedSearchResult {
    data class QuranResult(val result: QuranSearchResult) : UnifiedSearchResult()
    data class HadithResult(val result: HadithSearchResult) : UnifiedSearchResult()
    data class DuaResult(val result: DuaSearchResult) : UnifiedSearchResult()
    data class SurahResult(val surah: Surah) : UnifiedSearchResult()

    /**
     * Stable identity for a lazy-list `key`. Prefixed per variant because the underlying ids
     * are only unique within their own corpus — ayah 12 and dua "12" must not collide in a
     * mixed result list.
     */
    val key: String
        get() = when (this) {
            is QuranResult -> "quran:${result.ayah.id}"
            is HadithResult -> "hadith:${result.hadith.id}"
            is DuaResult -> "dua:${result.dua.id}"
            is SurahResult -> "surah:${surah.number}"
        }
}
