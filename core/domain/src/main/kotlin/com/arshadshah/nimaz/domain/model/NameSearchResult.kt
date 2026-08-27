package com.arshadshah.nimaz.domain.model

/**
 * Which of the three name catalogues a name belongs to.
 *
 * They are one destination — a tabbed Names screen, one search box, one favourites area — but
 * a result still has to say which tab it came from, and a tap still has to reach the right
 * detail route.
 */
enum class NameCatalog {
    /** Asmāʾ al-Ḥusnā — the ninety-nine Names of Allah. */
    ASMA_UL_HUSNA,

    /** Asmāʾ an-Nabī — the names of the Prophet ﷺ. */
    ASMA_UN_NABI,

    /** The Prophets. */
    PROPHETS,
}

/**
 * One name matched by a search, flattened out of whichever catalogue holds it.
 *
 * The three catalogue models share four fields and disagree about everything else — a
 * `Prophet` has an era and a lineage, an `AsmaUlHusna` has benefits and a use in duʿāʾ — and a
 * results list needs none of it. Flattening here keeps `LibrarySearchResults` from carrying
 * three separate lists that every consumer would have to merge back together, and keeps the
 * search screen from importing three catalogue models to render one row.
 */
data class NameSearchResult(
    val catalog: NameCatalog,
    val id: Int,
    val arabic: String,
    val transliteration: String,
    val english: String,
    /** What the name means, where the catalogue records it. Blank rather than absent. */
    val meaning: String = "",
)
