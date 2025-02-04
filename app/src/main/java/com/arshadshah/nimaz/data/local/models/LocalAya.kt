package com.arshadshah.nimaz.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Aya")
data class LocalAya(
    @PrimaryKey
    val ayaNumberInQuran: Int,
    var ayaArabic: String,
    val translationEnglish: String,
    val translationUrdu: String,
    val suraNumber: Int,
    val ayaNumberInSurah: Int,
    var bookmark: Boolean,
    var favorite: Boolean,
    var note: String,
    var audioFileLocation: String,
    val sajda: Boolean,
    val sajdaType: String,
    val ruku: Int,
    val juzNumber: Int,
)