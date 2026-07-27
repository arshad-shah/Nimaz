package com.arshadshah.nimaz.domain.model

data class Surah(
    val number: Int,
    val nameArabic: String,
    val nameEnglish: String,
    val nameTransliteration: String,
    val revelationType: RevelationType,
    val ayahCount: Int,
    val juzStart: Int,
    val orderInMushaf: Int,
    val startPage: Int = 1
) {
    // Alias for backwards compatibility
    val numberOfAyahs: Int get() = ayahCount
}

data class Ayah(
    val id: Int,
    val surahNumber: Int,
    val ayahNumber: Int,
    val textArabic: String,
    val textSimple: String,
    val juzNumber: Int,
    val hizbNumber: Int,
    val rubNumber: Int,
    val pageNumber: Int,
    val sajdaType: SajdaType?,
    val sajdaNumber: Int?,
    val translation: String? = null,
    val isBookmarked: Boolean = false,
    val transliteration: String? = null,
    val textTajweed: String? = null
) {
    // Aliases for backwards compatibility
    val numberInSurah: Int get() = ayahNumber
    val page: Int get() = pageNumber
    val juz: Int get() = juzNumber
}

data class SurahWithAyahs(
    val surah: Surah,
    val ayahs: List<Ayah>
)

data class QuranBookmark(
    val id: Long,
    val ayahId: Int,
    val surahNumber: Int,
    val ayahNumber: Int,
    val surahName: String? = null,
    val ayahText: String? = null,
    val note: String?,
    val color: String?,
    val createdAt: Long,
    val updatedAt: Long
)

/** The span of ayah ids that make up a single mushaf page. */
data class PageAyahRange(
    val page: Int,
    val minAyahId: Int,
    val maxAyahId: Int,
    val ayahCount: Int
)

data class ReadingProgress(
    val lastReadSurah: Int,
    val lastReadAyah: Int,
    val lastReadPage: Int,
    val lastReadJuz: Int,
    val totalAyahsRead: Int,
    val currentKhatmaCount: Int,
    val updatedAt: Long
) {
    // Aliases for backwards compatibility
    val lastSurah: Int get() = lastReadSurah
    val lastAyah: Int get() = lastReadAyah
}

/**
 * Pure progress maths for the "Continue Reading" card. Android-free so it can be unit
 * tested directly.
 *
 * The card labels itself with a surah name and "Verse N", so the progress it shows is
 * **progress through the current surah**, not through the whole Quran. The previous
 * implementation divided [ReadingProgress.totalAyahsRead] by 6236 — and nothing in the
 * app ever calls `incrementAyahsRead`, so that counter is permanently 0 and the bar
 * always rendered 0%.
 */
object ReadingProgressCalculator {

    /**
     * Total pages in the standard Madani mushaf, used for the no-surah fallback. Kept in
     * sync with (and single-sourced from) [MushafScript.MADANI] so the 16-line IndoPak
     * edition's 548-page count lives in one place — see [MushafScript.INDOPAK_16].
     */
    val TOTAL_QURAN_PAGES: Int = MushafScript.MADANI.totalPages

    /**
     * Fraction (0f..1f) of the way through a surah, given the last-read ayah number and
     * the surah's ayah count. Returns 0f when the surah size is unknown/invalid.
     */
    fun surahFraction(ayahNumber: Int, surahAyahCount: Int): Float {
        if (surahAyahCount <= 0) return 0f
        return (ayahNumber.toFloat() / surahAyahCount).coerceIn(0f, 1f)
    }

    /**
     * Fallback used when the surah metadata has not loaded yet: position in the mushaf
     * by page number.
     *
     * [totalPages] is the *active* edition's page count — the 16-line IndoPak mushaf ends
     * at 548, so pinning this to the Madani 604 under-reported progress there (#325).
     */
    fun pageFraction(pageNumber: Int, totalPages: Int = TOTAL_QURAN_PAGES): Float {
        if (totalPages <= 0) return 0f
        return (pageNumber.toFloat() / totalPages).coerceIn(0f, 1f)
    }

    /**
     * Whole-percent label for a fraction.
     *
     * Rounds to nearest, but never reports 0% for genuine progress nor 100% before the
     * fraction actually reaches 1 — a reader one ayah in should not see "0%", and a
     * reader one ayah from the end should not see "100%".
     */
    fun percent(fraction: Float): Int {
        val clamped = fraction.coerceIn(0f, 1f)
        if (clamped <= 0f) return 0
        if (clamped >= 1f) return 100
        return Math.round(clamped * 100).coerceIn(1, 99)
    }
}

data class QuranFavorite(
    val ayahId: Int,
    val surahNumber: Int,
    val ayahNumber: Int,
    val createdAt: Long
)

data class Translation(
    val id: Long,
    val ayahId: Int,
    val text: String,
    val languageCode: String,
    val translatorName: String,
    val translatorId: String
)

data class Translator(
    val id: String,
    val name: String,
    val languageCode: String
)

enum class RevelationType {
    MECCAN,
    MEDINAN;

    companion object {
        fun fromString(value: String): RevelationType {
            return when (value.lowercase()) {
                "meccan", "makkah", "makki" -> MECCAN
                "medinan", "madinah", "madani" -> MEDINAN
                else -> MECCAN
            }
        }
    }
}

enum class SajdaType {
    OBLIGATORY,
    RECOMMENDED;

    companion object {
        fun fromString(value: String?): SajdaType? {
            return when (value?.lowercase()) {
                "obligatory", "wajib" -> OBLIGATORY
                "recommended", "mustahab" -> RECOMMENDED
                else -> null
            }
        }
    }
}

data class QuranSearchResult(
    val ayah: Ayah,
    val surahName: String,
    val matchedText: String,
    val searchType: SearchType
)

enum class SearchType {
    ARABIC,
    TRANSLATION
}

data class JuzInfo(
    val number: Int,
    val startSurah: Int,
    val startAyah: Int,
    val endSurah: Int,
    val endAyah: Int
)

data class SurahInfo(
    val description: String,
    val themes: List<String>
)

data class PageInfo(
    val number: Int,
    val surahNumber: Int,
    val ayahNumber: Int,
    val juzNumber: Int
)
