package com.arshadshah.nimaz.domain.model

data class HadithBook(
    val id: String,
    val nameArabic: String,
    val nameEnglish: String,
    val authorName: String,
    val authorArabic: String,
    val totalHadiths: Int,
    val totalChapters: Int,
    val description: String?,
    val displayOrder: Int
)

data class HadithChapter(
    val id: String,
    val bookId: String,
    val chapterNumber: Int,
    val nameArabic: String,
    val nameEnglish: String,
    val hadithCount: Int,
    val hadithStartNumber: Int,
    val hadithEndNumber: Int
)

data class Hadith(
    val id: String,
    val bookId: String,
    val chapterId: String,
    val hadithNumber: Int,
    val hadithNumberInBook: Int,
    val textArabic: String,
    val textEnglish: String,
    val narratorChain: String?,
    val narratorName: String?,
    val grade: HadithGrade?,
    val gradeArabic: String?,
    val reference: String?,
    val isBookmarked: Boolean = false
)

data class HadithBookmark(
    val id: Long,
    val hadithId: String,
    val bookId: String,
    val hadithNumber: Int,
    val note: String?,
    val color: String?,
    val bookName: String? = null,
    val hadithText: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

enum class HadithGrade {
    SAHIH,
    HASAN,
    DAIF,
    MAWDU,
    UNKNOWN;

    companion object {
        fun fromString(value: String?): HadithGrade? {
            return when (value?.lowercase()) {
                "sahih", "authentic" -> SAHIH
                "hasan", "good" -> HASAN
                "daif", "da'if", "weak" -> DAIF
                "mawdu", "mawdu'", "fabricated" -> MAWDU
                else -> if (value != null) UNKNOWN else null
            }
        }
    }

    fun displayName(): String {
        return when (this) {
            SAHIH -> "Sahih (Authentic)"
            HASAN -> "Hasan (Good)"
            DAIF -> "Da'if (Weak)"
            MAWDU -> "Mawdu' (Fabricated)"
            UNKNOWN -> "Unknown"
        }
    }
}

data class HadithSearchResult(
    val hadith: Hadith,
    val bookName: String,
    val chapterName: String,
    val matchedText: String
)

data class HadithBookWithChapters(
    val book: HadithBook,
    val chapters: List<HadithChapter>
)

data class HadithChapterWithHadiths(
    val chapter: HadithChapter,
    val hadiths: List<Hadith>
)
