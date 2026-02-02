package com.arshadshah.nimaz.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "asma_ul_husna")
data class AsmaUlHusnaEntity(
    @PrimaryKey val id: Int,
    val number: Int,
    @ColumnInfo(name = "name_arabic") val nameArabic: String,
    @ColumnInfo(name = "name_transliteration") val nameTransliteration: String,
    @ColumnInfo(name = "name_english") val nameEnglish: String,
    val meaning: String,
    val explanation: String,
    val benefits: String,
    @ColumnInfo(name = "quran_references") val quranReferences: String, // JSON array stored as string
    @ColumnInfo(name = "usage_in_dua") val usageInDua: String,
    @ColumnInfo(name = "display_order") val displayOrder: Int
)

@Entity(
    tableName = "asma_ul_husna_bookmarks",
    indices = [Index(value = ["name_id"], unique = true)]
)
data class AsmaUlHusnaBookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name_id") val nameId: Int,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
