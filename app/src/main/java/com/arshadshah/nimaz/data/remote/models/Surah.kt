package com.arshadshah.nimaz.data.remote.models

data class Surah(
    val number: Int,
    val numberOfAyahs: Int,
    val startAya: Int,
    val name: String,
    val englishName: String,
    val englishNameTranslation: String,
    val revelationType: String,
    val revelationOrder: Int,
    val rukus: Int,
)