package com.arshadshah.nimaz.domain.model

data class Surah(
    val number: Int,
    val nameArabic: String,
    val nameEnglish: String,
    val nameTransliteration: String,
    val revelationType: RevelationType,
    val ayahCount: Int,
    val juzStart: Int,
    val orderInMushaf: Int
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
    val isBookmarked: Boolean = false
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
