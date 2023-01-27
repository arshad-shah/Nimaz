package com.arshadshah.nimaz.data.remote.models

data class Dua(
    val _id: Int,
    val chapter_id: Int,
    val favourite: Int,
    val arabic_dua: String,
    val arabic_reference: String,
    val english_translation: String,
    val english_reference: String
)
