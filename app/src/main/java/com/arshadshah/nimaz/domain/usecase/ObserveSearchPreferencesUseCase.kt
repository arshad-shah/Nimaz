package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.LibrarySource
import com.arshadshah.nimaz.domain.model.MatchStrictness
import com.arshadshah.nimaz.domain.model.SearchPreferences
import com.arshadshah.nimaz.domain.repository.settings.SearchSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * The stored search preferences, as a typed model with its invariants applied.
 *
 * This is where the four persisted primitives become [SearchPreferences], and where a value
 * the app no longer understands stops being a crash. A preferences file outlives the build
 * that wrote it — a downgrade, a restore from a device on a newer version, a hand-edit — so
 * an unknown `LibrarySource` name or a `MatchStrictness` that has been renamed has to degrade
 * to the default rather than throw on the first search anyone runs.
 */
class ObserveSearchPreferencesUseCase @Inject constructor(
    private val settings: SearchSettings,
) {
    operator fun invoke(): Flow<SearchPreferences> = combine(
        settings.searchResultsPerSource,
        settings.searchSources,
        settings.searchStrictness,
        settings.searchDefaultScope,
    ) { perSource, sources, strictness, scope ->
        SearchPreferences(
            resultsPerSource = perSource,
            sources = parseSources(sources),
            strictness = parseStrictness(strictness),
            defaultScope = parseSource(scope),
        ).sanitised
    }

    private fun parseSources(stored: String): Set<LibrarySource> {
        // Empty means "not chosen", which is every source — including any added since the
        // preference was written, which is the reason it is not stored as a full list.
        if (stored.isBlank()) return LibrarySource.entries.toSet()
        return stored.split(SEPARATOR).mapNotNull(::parseSource).toSet()
    }

    private fun parseSource(name: String): LibrarySource? =
        LibrarySource.entries.firstOrNull { it.name == name.trim() }

    private fun parseStrictness(name: String): MatchStrictness =
        MatchStrictness.entries.firstOrNull { it.name == name.trim() } ?: MatchStrictness.BALANCED

    companion object {
        const val SEPARATOR = ","

        /** How a source set is written down. The inverse of [parseSources]. */
        fun encode(sources: Set<LibrarySource>): String =
            if (sources == LibrarySource.entries.toSet()) "" else sources.joinToString(SEPARATOR) { it.name }
    }
}
