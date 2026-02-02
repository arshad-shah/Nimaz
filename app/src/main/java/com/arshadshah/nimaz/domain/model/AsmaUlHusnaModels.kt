package com.arshadshah.nimaz.domain.model

data class AsmaUlHusna(
    val id: Int,
    val number: Int,
    val nameArabic: String,
    val nameTransliteration: String,
    val nameEnglish: String,
    val meaning: String,
    val explanation: String,
    val benefits: String,
    val quranReferences: List<String>,
    val usageInDua: String,
    val displayOrder: Int,
    val isFavorite: Boolean = false
)
