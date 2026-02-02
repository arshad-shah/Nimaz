package com.arshadshah.nimaz.domain.model

data class Prophet(
    val id: Int,
    val number: Int,
    val nameArabic: String,
    val nameEnglish: String,
    val nameTransliteration: String,
    val titleArabic: String,
    val titleEnglish: String,
    val storySummary: String,
    val keyLessons: List<String>,
    val quranMentions: List<String>,
    val era: String,
    val lineage: String,
    val yearsLived: String,
    val placeOfPreaching: String,
    val miracles: List<String>,
    val displayOrder: Int,
    val isFavorite: Boolean = false
)
