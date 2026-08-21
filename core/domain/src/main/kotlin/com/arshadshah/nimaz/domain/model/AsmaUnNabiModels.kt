package com.arshadshah.nimaz.domain.model

data class AsmaUnNabi(
    val id: Int,
    val number: Int,
    val nameArabic: String,
    val nameTransliteration: String,
    val nameEnglish: String,
    val meaning: String,
    val explanation: String,
    val source: String,
    val displayOrder: Int,
    val isFavorite: Boolean = false
)
