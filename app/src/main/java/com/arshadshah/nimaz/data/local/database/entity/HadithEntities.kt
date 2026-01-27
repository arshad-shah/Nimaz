package com.arshadshah.nimaz.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "hadith_books")
data class HadithBookEntity(
    @PrimaryKey
    val id: Int,
    @ColumnInfo(name = "name_english")
    val nameEnglish: String,
    @ColumnInfo(name = "name_arabic")
    val nameArabic: String,
    val author: String,
    @ColumnInfo(name = "hadith_count")
    val hadithCount: Int,
    val description: String,
    val icon: String
)

@Entity(
    tableName = "hadiths",
    foreignKeys = [
        ForeignKey(
            entity = HadithBookEntity::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["book_id"]),
        Index(value = ["chapter_id"])
    ]
)
data class HadithEntity(
    @PrimaryKey
    val id: Int,
    @ColumnInfo(name = "book_id")
    val bookId: Int,
    @ColumnInfo(name = "chapter_id")
    val chapterId: Int,
    @ColumnInfo(name = "number_in_book")
    val numberInBook: Int,
    @ColumnInfo(name = "number_in_chapter")
    val numberInChapter: Int,
    @ColumnInfo(name = "text_arabic")
    val textArabic: String,
    @ColumnInfo(name = "text_english")
    val textEnglish: String,
    val narrator: String,
    val grade: String,
    val reference: String
)

@Entity(
    tableName = "hadith_bookmarks",
    indices = [Index(value = ["hadithId"])]
)
data class HadithBookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hadithId: Int,
    val bookId: Int,
    val hadithNumber: Int,
    val note: String?,
    val color: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
