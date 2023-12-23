package com.arshadshah.nimaz.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Aya")
data class LocalAya(
    @PrimaryKey
    val ayaNumberInQuran: Int,
    val ayaNumber: Int,
    val ayaArabic: String,
    val translationEnglish: String,
    val translationUrdu: String,
    val suraNumber: Int,
    val ayaNumberInSurah: Int,
    val bookmark: Boolean,
    val favorite: Boolean,
    val note: String,
    val audioFileLocation: String,
    val sajda: Boolean,
    val sajdaType: String,
    val ruku: Int,
    val juzNumber: Int,
)