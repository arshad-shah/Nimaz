package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.AsmaUlHusna
import com.arshadshah.nimaz.domain.model.AsmaUnNabi
import com.arshadshah.nimaz.domain.model.NameCatalog
import com.arshadshah.nimaz.domain.model.NameSearchResult
import com.arshadshah.nimaz.domain.model.Prophet
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * The three name catalogues, searched as one.
 *
 * **Why this is a filter and not a query.** The other library sources are tens of thousands of
 * rows and are searched through the database. These three are 99 + 99 + 25, they are already
 * held in memory by whatever screen is showing them, and none of the repositories has a search
 * method to call — so a `LIKE` round trip would be slower than the filter and would need three
 * new DAO queries to exist first.
 *
 * The matched fields are the same ones the Names screen's own filter uses (`CatalogSource.
 * matches`), so a query typed into the Names search box and the same query typed into global
 * search find the same names. Diacritics are not stripped: the transliteration column carries
 * them (`Ar-Raḥmān`), so a search for "rahman" is matched on the English and Arabic columns
 * instead, and a search for "raḥmān" on the transliteration.
 */
class SearchNamesUseCase @Inject constructor(
    private val asmaUlHusna: AsmaUlHusnaUseCases,
    private val asmaUnNabi: AsmaUnNabiUseCases,
    private val prophets: ProphetUseCases,
) {

    suspend operator fun invoke(query: String): List<NameSearchResult> {
        val term = query.trim()
        if (term.isBlank()) return emptyList()

        return buildList {
            asmaUlHusna.getAllNames().first().forEach { name ->
                if (name.matches(term)) add(name.toResult())
            }
            asmaUnNabi.getAllNames().first().forEach { name ->
                if (name.matches(term)) add(name.toResult())
            }
            prophets.getAllProphets().first().forEach { prophet ->
                if (prophet.matches(term)) add(prophet.toResult())
            }
        }
    }
}

private fun AsmaUlHusna.matches(q: String) =
    nameArabic.contains(q, true) || nameTransliteration.contains(q, true) ||
        nameEnglish.contains(q, true) || meaning.contains(q, true)

private fun AsmaUnNabi.matches(q: String) =
    nameArabic.contains(q, true) || nameTransliteration.contains(q, true) ||
        nameEnglish.contains(q, true) || meaning.contains(q, true)

private fun Prophet.matches(q: String) =
    nameArabic.contains(q, true) || nameTransliteration.contains(q, true) ||
        nameEnglish.contains(q, true) || titleEnglish.contains(q, true)

private fun AsmaUlHusna.toResult() = NameSearchResult(
    catalog = NameCatalog.ASMA_UL_HUSNA,
    id = id,
    arabic = nameArabic,
    transliteration = nameTransliteration,
    english = nameEnglish,
    meaning = meaning,
)

private fun AsmaUnNabi.toResult() = NameSearchResult(
    catalog = NameCatalog.ASMA_UN_NABI,
    id = id,
    arabic = nameArabic,
    transliteration = nameTransliteration,
    english = nameEnglish,
    meaning = meaning,
)

private fun Prophet.toResult() = NameSearchResult(
    catalog = NameCatalog.PROPHETS,
    id = id,
    arabic = nameArabic,
    transliteration = nameTransliteration,
    english = nameEnglish,
    // A prophet has no "meaning"; the title is the equivalent line of context.
    meaning = titleEnglish,
)
