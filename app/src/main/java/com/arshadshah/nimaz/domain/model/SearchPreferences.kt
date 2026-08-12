package com.arshadshah.nimaz.domain.model

/**
 * What the user has decided about how search behaves.
 *
 * All four of these were compile-time constants in `SearchLibraryUseCase`, which meant the
 * only way to discover them was to hit one: a search for الله returned 180 results and looked
 * like a defect, when it was three sources each returning a full 60. A cap you cannot see and
 * cannot change reads as a bug every time.
 */
data class SearchPreferences(
    /** How many results each source may contribute. Was a hardcoded 60. */
    val resultsPerSource: Int = DEFAULT_RESULTS_PER_SOURCE,
    /** Which sources are searched at all. Empty is not allowed — see [sanitised]. */
    val sources: Set<LibrarySource> = LibrarySource.entries.toSet(),
    /** How hard a multi-word query tries. Was a hardcoded eight word passes. */
    val strictness: MatchStrictness = MatchStrictness.BALANCED,
    /** Which source the search screen opens filtered to; null means "everything". */
    val defaultScope: LibrarySource? = null,
) {

    /**
     * The same preferences with the values that would break search corrected.
     *
     * A user cannot reach these states through the settings screen, but a preference file can:
     * it survives an app downgrade, a restore from a backup written by a newer version, and a
     * hand-edit. Search returning nothing at all because the persisted source set was empty is
     * not a state worth honouring faithfully.
     */
    val sanitised: SearchPreferences
        get() = copy(
            resultsPerSource = resultsPerSource.coerceIn(MIN_RESULTS_PER_SOURCE, MAX_RESULTS_PER_SOURCE),
            sources = sources.ifEmpty { LibrarySource.entries.toSet() },
            // A scope pointing at a source that is switched off would silently show nothing.
            defaultScope = defaultScope?.takeIf { it in sources },
        )

    companion object {
        const val DEFAULT_RESULTS_PER_SOURCE = 60
        const val MIN_RESULTS_PER_SOURCE = 10
        const val MAX_RESULTS_PER_SOURCE = 200
    }
}

/**
 * A body of content search can look in.
 *
 * One entry per thing a user would think of as a separate place to look, which is why surah
 * names are not their own source: "search the Qur'an but not its surah names" is not a
 * distinction anyone wants, and offering it would make the settings screen longer without
 * making it more useful. The order is the reading order of the library, and it is the order
 * results appear in.
 *
 * Adding a source here is enough to make it selectable — the settings screen and the search
 * screen's filter chips are both built from [entries] — but the query itself has to be added
 * to `SearchLibraryUseCase` in the same change, or the new source is a switch that does
 * nothing.
 */
enum class LibrarySource {
    /** Ayat and their translations, plus surah names. */
    QURAN,
    HADITH,
    DUAS,
}

/**
 * How aggressively a multi-word query is broken into single-word passes.
 *
 * The whole phrase is always searched and always outranks a word hit. This governs only the
 * *extra* passes: "patience in hardship" as three more searches whose hits are merged and
 * ranked by how many words they match.
 *
 * More passes means more recall and more noise, and each pass is a query per source — so this
 * is also the setting that decides how much work a long query does.
 */
enum class MatchStrictness(val wordPasses: Int) {
    /** The phrase only. A search means exactly what it says. */
    EXACT(0),

    /** Up to eight word passes — what the app did before this was a setting. */
    BALANCED(8),

    /** Up to twenty, for someone who would rather sift than miss something. */
    BROAD(20),
}
