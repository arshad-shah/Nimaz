package com.arshadshah.nimaz.presentation.screens.hadith

import com.arshadshah.nimaz.domain.model.Hadith
import com.arshadshah.nimaz.domain.model.HadithBook
import com.arshadshah.nimaz.domain.model.HadithChapter
import com.arshadshah.nimaz.domain.model.HadithGrade

/**
 * Content the hadith screens render, built here rather than in each test class.
 *
 * Every field the screens branch on is a parameter with a realistic default, so a test names
 * only the field it is about — `hadith(grade = null)` reads as "a hadith with no grade", which
 * is the case the assertion is making.
 */
internal fun book(
    id: String = "bukhari",
    nameEnglish: String = "Sahih al-Bukhari",
    nameArabic: String = "صحيح البخاري",
    authorName: String = "Imam al-Bukhari",
    totalHadiths: Int = 7563,
    totalChapters: Int = 97,
) = HadithBook(
    id = id,
    nameArabic = nameArabic,
    nameEnglish = nameEnglish,
    authorName = authorName,
    authorArabic = "البخاري",
    totalHadiths = totalHadiths,
    totalChapters = totalChapters,
    description = null,
    displayOrder = 1,
)

internal fun chapter(
    id: String = "bukhari_1",
    bookId: String = "bukhari",
    chapterNumber: Int = 1,
    nameEnglish: String = "Revelation",
    nameArabic: String = "بدء الوحي",
    hadithCount: Int = 7,
) = HadithChapter(
    id = id,
    bookId = bookId,
    chapterNumber = chapterNumber,
    nameArabic = nameArabic,
    nameEnglish = nameEnglish,
    hadithCount = hadithCount,
    hadithStartNumber = 1,
    hadithEndNumber = hadithCount,
)

internal fun hadith(
    id: String = "bukhari_1_1",
    bookId: String = "bukhari",
    chapterId: String = "1",
    hadithNumber: Int = 1,
    hadithNumberInBook: Int = 1,
    textArabic: String = "إنما الأعمال بالنيات",
    textEnglish: String = "Actions are but by intention",
    narratorChain: String? = null,
    narratorName: String? = "Umar ibn al-Khattab",
    grade: HadithGrade? = HadithGrade.SAHIH,
    reference: String? = "Sahih al-Bukhari 1",
    isBookmarked: Boolean = false,
) = Hadith(
    id = id,
    bookId = bookId,
    chapterId = chapterId,
    hadithNumber = hadithNumber,
    hadithNumberInBook = hadithNumberInBook,
    textArabic = textArabic,
    textEnglish = textEnglish,
    narratorChain = narratorChain,
    narratorName = narratorName,
    grade = grade,
    gradeArabic = null,
    reference = reference,
    isBookmarked = isBookmarked,
)
