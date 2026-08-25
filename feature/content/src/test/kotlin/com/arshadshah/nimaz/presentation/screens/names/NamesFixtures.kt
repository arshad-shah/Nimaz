package com.arshadshah.nimaz.presentation.screens.names

import com.arshadshah.nimaz.domain.model.AsmaUlHusna
import com.arshadshah.nimaz.domain.model.AsmaUnNabi
import com.arshadshah.nimaz.domain.model.Prophet

/** The three catalogues' items, with every field a screen branches on named. */
internal fun divineName(
    id: Int = 1,
    nameArabic: String = "الرحمن",
    nameTransliteration: String = "Ar-Rahman",
    nameEnglish: String = "The Most Compassionate",
    meaning: String = "The One whose mercy encompasses all",
    explanation: String = "Mercy that reaches believer and disbeliever alike",
    benefits: String = "Reciting it softens the heart",
    quranReferences: List<String> = listOf("1:1", "55:1"),
    usageInDua: String = "Ya Rahman, have mercy on me",
    isFavorite: Boolean = false,
) = AsmaUlHusna(
    id = id,
    number = id,
    nameArabic = nameArabic,
    nameTransliteration = nameTransliteration,
    nameEnglish = nameEnglish,
    meaning = meaning,
    explanation = explanation,
    benefits = benefits,
    quranReferences = quranReferences,
    usageInDua = usageInDua,
    displayOrder = id,
    isFavorite = isFavorite,
)

internal fun prophetName(
    id: Int = 1,
    nameArabic: String = "محمد",
    nameTransliteration: String = "Muhammad",
    nameEnglish: String = "The Praised One",
    meaning: String = "The one who is much praised",
    explanation: String = "Named before his birth",
    source: String = "Sahih al-Bukhari 3532",
    isFavorite: Boolean = false,
) = AsmaUnNabi(
    id = id,
    number = id,
    nameArabic = nameArabic,
    nameTransliteration = nameTransliteration,
    nameEnglish = nameEnglish,
    meaning = meaning,
    explanation = explanation,
    source = source,
    displayOrder = id,
    isFavorite = isFavorite,
)

internal fun prophet(
    id: Int = 1,
    nameArabic: String = "إبراهيم",
    nameEnglish: String = "Abraham",
    nameTransliteration: String = "Ibrahim",
    titleEnglish: String = "Friend of Allah",
    storySummary: String = "Called his people away from idols",
    keyLessons: List<String> = listOf("Tawhid before all", "Patience under trial"),
    quranMentions: List<String> = listOf("2:124", "6:74"),
    era: String = "circa 2000 BCE",
    lineage: String = "Son of Azar",
    yearsLived: String = "175 years",
    placeOfPreaching: String = "Ur and Canaan",
    miracles: List<String> = listOf("Unburned by the fire"),
    isFavorite: Boolean = false,
) = Prophet(
    id = id,
    number = id,
    nameArabic = nameArabic,
    nameEnglish = nameEnglish,
    nameTransliteration = nameTransliteration,
    titleArabic = "خليل الله",
    titleEnglish = titleEnglish,
    storySummary = storySummary,
    keyLessons = keyLessons,
    quranMentions = quranMentions,
    era = era,
    lineage = lineage,
    yearsLived = yearsLived,
    placeOfPreaching = placeOfPreaching,
    miracles = miracles,
    displayOrder = id,
    isFavorite = isFavorite,
)
