package com.arshadshah.nimaz.domain.search

/**
 * Turns one folded query word into the word *prefixes* the index should be asked for.
 *
 * The index is FTS4 with the `unicode61` tokenizer: no stemmer, and FTS4 on stock Android
 * cannot take one. What it can do is prefix matching (`word*`), so word forms are handled by
 * asking for a shorter prefix rather than by stemming the corpus:
 *
 *  - **Endings.** `prayers` → `prayer*` (finds prayer, prayers), `fasting` → `fast*` (fast,
 *    fasted, fasting), `forgiveness` → `forgive*`. A plain `prayers*` finds none of the
 *    singular forms, which is most of the corpus.
 *  - **Synonyms.** The corpus spells the same practice several ways — Sahih International
 *    writes "prayer", the hadith translations write "Salat" and "prayer", users type "salah"
 *    or "namaz". A small, hand-kept list of Islamic terms is expanded both ways.
 *
 * Every rule here only ever **adds** prefixes; the word the user typed is always one of them
 * (or covered by a shorter one), so nothing that matched before stops matching.
 *
 * Endings are trimmed for Latin-script words only. Arabic is already folded by
 * [ArabicSearchNormaliser] and its morphology is not something a suffix list can handle.
 */
object SearchTermExpander {

    /**
     * English words too common to search on their own. A multi-word query drops them before
     * its per-word passes (`SearchLibraryUseCase.tokenize`), and highlighting skips them — a
     * highlighted "the" on every line is noise, not a match.
     */
    val STOPWORDS: Set<String> = setOf(
        "the", "and", "for", "are", "but", "not", "you", "all", "any", "can",
        "her", "was", "one", "our", "out", "has", "him", "his", "how", "what",
        "when", "where", "which", "who", "why", "with", "does", "did", "about",
        "that", "this", "there", "their", "them", "then", "they", "have", "from",
        "into", "over", "than", "your", "should", "would", "could", "will",
        "say", "says", "said",
    )

    /** No more than this many prefixes per word — a cap on how wide one term can fan out. */
    private const val MAX_PREFIXES = 8

    /** A trimmed base shorter than this matches too much to be useful ("pra*"). */
    private const val MIN_BASE = 3

    /**
     * The prefixes to ask the index for, shortest-covering first. Never empty for a non-blank
     * [term]; a prefix that another one already covers ("prayers" under "prayer") is dropped.
     */
    fun prefixes(term: String): List<String> {
        if (term.isBlank()) return emptyList()
        val candidates = LinkedHashSet<String>()
        candidates += term
        trimEnding(term)?.let { candidates += it }
        // Look synonyms up under the word and under its trimmed base, so "prayers" reaches
        // the same group as "prayer".
        candidates.toList().forEach { word -> SYNONYMS[word]?.let { candidates += it } }
        return minimalCover(candidates).take(MAX_PREFIXES)
    }

    /**
     * The base a common English ending hides, or null when there is nothing to trim.
     *
     * Deliberately light — a handful of endings, each producing something whose prefix still
     * reaches the original word (`fasting` → `fast`, which `fast*` covers). Not Porter: Porter
     * maps "mercy" to "merci", and `merci*` would no longer find "mercy".
     */
    internal fun trimEnding(term: String): String? {
        if (term.length < 5 || !term.all { it in 'a'..'z' }) return null
        val base = when {
            term.endsWith("ies") -> term.dropLast(3) + "y"          // mercies → mercy
            term.endsWith("ied") -> term.dropLast(3) + "y"          // purified → purify
            term.endsWith("ness") -> term.dropLast(4)               // forgiveness → forgive
            term.endsWith("sses") -> term.dropLast(2)               // witnesses → witness
            term.endsWith("ing") -> undouble(term.dropLast(3))      // fasting → fast
            term.endsWith("ed") -> undouble(term.dropLast(2))       // prayed → pray
            ES_AFTER.any { term.endsWith(it + "es") } -> term.dropLast(2)  // wishes → wish
            term.endsWith("s") && !term.endsWith("ss") &&
                !term.endsWith("us") && !term.endsWith("is") -> term.dropLast(1)  // prayers → prayer
            else -> return null
        }
        return base.takeIf { it.length >= MIN_BASE && it != term }
    }

    /** "sitt" → "sit": the doubled consonant an -ing/-ed ending added. */
    private fun undouble(stem: String): String =
        if (stem.length >= 4 && stem[stem.length - 1] == stem[stem.length - 2] &&
            stem.last() !in "aeiouls"
        ) stem.dropLast(1) else stem

    /** Drops any candidate another candidate is a prefix of: `x*` already matches `xy*`. */
    private fun minimalCover(candidates: Collection<String>): List<String> {
        val sorted = candidates.filter { it.isNotBlank() }.distinct().sortedBy { it.length }
        val kept = mutableListOf<String>()
        for (word in sorted) {
            if (kept.none { word.startsWith(it) }) kept += word
        }
        return kept
    }

    // Not "s": "moses" → "mos" would reach "mosque" and "most". The -s rule below already
    // turns it into "mose", and "sses" covers "witnesses".
    private val ES_AFTER = listOf("ch", "sh", "x", "z")

    /**
     * Groups of words that name the same thing in this corpus, folded (lowercase, no marks).
     * Kept small on purpose: every entry widens every search for it, and a loose synonym is
     * a search that returns the wrong things. Add a group when real queries miss, not
     * speculatively.
     */
    private val GROUPS: List<List<String>> = listOf(
        listOf("salah", "salat", "salaat", "namaz", "prayer"),
        listOf("zakat", "zakah", "alms"),
        listOf("sawm", "siyam", "fasting"),
        listOf("wudu", "wudhu", "ablution"),
        listOf("hajj", "pilgrimage"),
        listOf("umrah", "umra"),
        listOf("sabr", "patience", "patient"),
        listOf("dua", "supplication"),
        listOf("dhikr", "zikr", "remembrance"),
        listOf("tawbah", "taubah", "tauba", "repentance"),
        listOf("jannah", "paradise"),
        listOf("jahannam", "hellfire"),
        listOf("jumuah", "jummah", "jumma"),
        listOf("quran", "koran"),
        listOf("ramadan", "ramadhan", "ramzan"),
        listOf("taqwa", "piety"),
        listOf("tawhid", "tauhid", "tawheed"),
        listOf("iman", "eman"),
        listOf("sadaqah", "sadaqa", "sadqa", "charity"),
        listOf("riba", "usury"),
        listOf("masjid", "mosque"),
        listOf("kaaba", "kabah"),
        listOf("akhirah", "akhira", "hereafter"),
        listOf("adhan", "azan", "athan"),
        listOf("nikah", "marriage"),
        listOf("janazah", "janaza", "funeral"),
    )

    private val SYNONYMS: Map<String, List<String>> = buildMap {
        for (group in GROUPS) for (word in group) put(word, group)
    }
}
