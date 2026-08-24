package com.arshadshah.nimaz.presentation.screens.search

import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.ContentTarget
import com.arshadshah.nimaz.domain.model.Dua
import com.arshadshah.nimaz.domain.model.DuaSearchResult
import com.arshadshah.nimaz.domain.model.Hadith
import com.arshadshah.nimaz.domain.model.HadithGrade
import com.arshadshah.nimaz.domain.model.HadithSearchResult
import com.arshadshah.nimaz.domain.model.NameCatalog
import com.arshadshah.nimaz.domain.model.NameSearchResult
import com.arshadshah.nimaz.domain.model.Proof
import com.arshadshah.nimaz.domain.model.QuranSearchResult
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.SearchType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.model.UnifiedSearchResult

/**
 * The five kinds of thing a library search can return, built once for the screen tests.
 *
 * Deliberately given *distinguishable* text: every helper takes the values the row is supposed
 * to render, so an assertion on "Al-Furqan" is an assertion that the Qur'an row rendered its own
 * subtitle rather than some other row's. Shared identifiers would let a mis-wired card pass.
 */
internal fun quranResult(
    surah: Int = 25,
    ayah: Int = 63,
    surahName: String = "Al-Furqan",
    matchedText: String = "The servants of the Most Merciful walk humbly",
) = UnifiedSearchResult.QuranResult(
    QuranSearchResult(
        ayah = Ayah(
            id = surah * 1000 + ayah,
            surahNumber = surah,
            ayahNumber = ayah,
            textArabic = "وَعِبَادُ الرَّحْمَٰنِ",
            textSimple = "wa ibadu ar-rahman",
            juzNumber = 19,
            hizbNumber = 37,
            rubNumber = 147,
            pageNumber = 365,
            sajdaType = null,
            sajdaNumber = null,
            translation = matchedText,
        ),
        surahName = surahName,
        matchedText = matchedText,
        searchType = SearchType.TRANSLATION,
    ),
)

internal fun surahResult(
    number: Int = 18,
    nameEnglish: String = "The Cave",
    nameTransliteration: String = "Al-Kahf",
    ayahCount: Int = 110,
) = UnifiedSearchResult.SurahResult(
    Surah(
        number = number,
        nameArabic = "الكهف",
        nameEnglish = nameEnglish,
        nameTransliteration = nameTransliteration,
        revelationType = RevelationType.MECCAN,
        ayahCount = ayahCount,
        orderInMushaf = number,
    ),
)

internal fun hadithResult(
    id: String = "bukhari-1",
    bookId: String = "bukhari",
    hadithNumber: Int = 1,
    bookName: String = "Sahih al-Bukhari",
    matchedText: String = "Actions are judged by intentions",
) = UnifiedSearchResult.HadithResult(
    HadithSearchResult(
        hadith = Hadith(
            id = id,
            bookId = bookId,
            chapterId = "1",
            hadithNumber = hadithNumber,
            hadithNumberInBook = hadithNumber,
            textArabic = "إنما الأعمال بالنيات",
            textEnglish = matchedText,
            narratorChain = null,
            narratorName = "Umar ibn al-Khattab",
            grade = HadithGrade.SAHIH,
            gradeArabic = null,
            reference = "bukhari:$hadithNumber",
        ),
        bookName = bookName,
        chapterName = "Revelation",
        matchedText = matchedText,
    ),
)

internal fun duaResult(
    id: String = "dua-anxiety",
    title: String = "Dua for anxiety",
    categoryName: String = "Distress",
    matchedText: String = "O Allah, I seek refuge in You from worry and grief",
) = UnifiedSearchResult.DuaResult(
    DuaSearchResult(
        dua = Dua(
            id = id,
            categoryId = "distress",
            titleArabic = "دعاء الهم",
            titleEnglish = title,
            textArabic = "اللهم إني أعوذ بك من الهم والحزن",
            textTransliteration = null,
            textEnglish = matchedText,
            reference = null,
            occasion = null,
            benefits = null,
            repeatCount = null,
            audioUrl = null,
            displayOrder = 1,
        ),
        categoryName = categoryName,
        matchedText = matchedText,
    ),
)

internal fun nameResult(
    catalog: NameCatalog = NameCatalog.ASMA_UL_HUSNA,
    id: Int = 1,
    transliteration: String = "Ar-Rahman",
    english: String = "The Most Merciful",
    meaning: String = "The One who wills goodness and mercy for all",
) = UnifiedSearchResult.NameResult(
    NameSearchResult(
        catalog = catalog,
        id = id,
        arabic = "الرحمن",
        transliteration = transliteration,
        english = english,
        meaning = meaning,
    ),
)

internal fun quranProof(
    surah: Int = 2,
    ayah: Int = 153,
    surahName: String = "Al-Baqarah",
    displayText: String = "Seek help through patience and prayer",
) = Proof.Quran(
    citationId = "quran:$surah:$ayah",
    surahNumber = surah,
    ayahNumber = ayah,
    surahName = surahName,
    displayText = displayText,
    target = ContentTarget.Ayah(surah, ayah),
)

internal fun hadithProof(
    id: String = "muslim-2999",
    hadithNumber: Int = 2999,
    bookName: String = "Sahih Muslim",
    displayText: String = "Wondrous is the affair of the believer",
) = Proof.Hadith(
    citationId = "hadith:$id",
    hadithNumber = hadithNumber,
    bookName = bookName,
    displayText = displayText,
    target = ContentTarget.Hadith(id),
)
