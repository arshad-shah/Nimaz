package com.arshadshah.nimaz.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "dua_categories")
data class DuaCategoryEntity(
    @PrimaryKey
    val id: Int,
    @ColumnInfo(name = "name_english")
    val nameEnglish: String,
    @ColumnInfo(name = "name_arabic")
    val nameArabic: String,
    val icon: String,
    @ColumnInfo(name = "display_order")
    val displayOrder: Int,
    @ColumnInfo(name = "dua_count")
    val duaCount: Int
)

@Entity(
    tableName = "duas",
    foreignKeys = [
        ForeignKey(
            entity = DuaCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["category_id"])]
)
data class DuaEntity(
    @PrimaryKey
    val id: Int,
    @ColumnInfo(name = "category_id")
    val categoryId: Int,
    @ColumnInfo(name = "title_english")
    val titleEnglish: String,
    @ColumnInfo(name = "title_arabic")
    val titleArabic: String,
    @ColumnInfo(name = "text_arabic")
    val textArabic: String,
    val transliteration: String,
    val translation: String,
    val source: String,
    val virtue: String?,
    @ColumnInfo(name = "repeat_count")
    val repeatCount: Int,
    @ColumnInfo(name = "audio_file")
    val audioFile: String?,
    @ColumnInfo(name = "display_order")
    val displayOrder: Int
)

@Entity(
    tableName = "dua_bookmarks",
    indices = [Index(value = ["duaId"])]
)
data class DuaBookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val duaId: Int,
    val categoryId: Int,
    val note: String?,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "dua_progress",
    indices = [Index(value = ["duaId"]), Index(value = ["date"])]
)
data class DuaProgressEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val duaId: Int,
    val date: Long, // Date in millis (start of day)
    val completedCount: Int, // Times completed on this date
    val targetCount: Int, // Target repetitions
    val isCompleted: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)
