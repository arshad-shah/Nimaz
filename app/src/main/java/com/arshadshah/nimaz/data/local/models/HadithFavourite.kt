package com.arshadshah.nimaz.data.local.models

data class HadithFavourite(
    val book_title_arabic: String,
    val book_title_english: String,
    val chapter_title_arabic: String,
    val chapter_title_english: String,
    val chapterId: Int,
    val bookId: Int,
    val idInBook: Int,
    val favourite: Boolean,
    val hadithId: Int,
)
