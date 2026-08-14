package com.arshadshah.nimaz.domain.model

/**
 * What the Browse search field was asking for.
 *
 * One field serves four questions — a surah name, a surah number, a juz and a
 * mushaf page — because they are all ways of naming a place, and making the
 * reader pick the right tab first is the thing this redesign removes.
 *
 * Out-of-range numbers fall back to [Name] rather than erroring: "juz 31" is
 * far more likely to be someone typing than someone expecting juz 31 to exist,
 * and a name search shows them that nothing matches.
 */
sealed interface QuranSearchQuery {

    data object Empty : QuranSearchQuery
    data class Juz(val number: Int) : QuranSearchQuery
    data class Page(val number: Int) : QuranSearchQuery
    data class SurahNumber(val number: Int) : QuranSearchQuery
    data class Name(val text: String) : QuranSearchQuery

    companion object {
        private const val SURAH_COUNT = 114
        private const val JUZ_COUNT = 30

        /**
         * The largest page count of any shipped mushaf edition. Validated
         * against the largest so a query resolves whichever script is active;
         * the reader then clamps to that script's real count.
         */
        private const val MAX_PAGE = 847

        private val JUZ = Regex("""^(?:juz|para|j)\s*(\d{1,2})$""")
        private val PAGE = Regex("""^(?:page|pg|p)\s*(\d{1,3})$""")
        private val NUMBER = Regex("""^\d{1,3}$""")

        fun parse(raw: String): QuranSearchQuery {
            val text = raw.trim().lowercase()
            if (text.isEmpty()) return Empty

            JUZ.find(text)?.let { match ->
                val n = match.groupValues[1].toInt()
                if (n in 1..JUZ_COUNT) return Juz(n)
                return Name(text)
            }
            PAGE.find(text)?.let { match ->
                val n = match.groupValues[1].toInt()
                if (n in 1..MAX_PAGE) return Page(n)
                return Name(text)
            }
            if (NUMBER.matches(text)) {
                val n = text.toInt()
                return if (n in 1..SURAH_COUNT) SurahNumber(n) else Name(text)
            }
            return Name(text)
        }
    }
}
