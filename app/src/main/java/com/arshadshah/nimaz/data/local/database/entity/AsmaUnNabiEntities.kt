package com.arshadshah.nimaz.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "asma_un_nabi")
data class AsmaUnNabiEntity(
    @PrimaryKey val id: Int,
    val number: Int,
    @ColumnInfo(name = "name_arabic") val nameArabic: String,
    @ColumnInfo(name = "name_transliteration") val nameTransliteration: String,
    @ColumnInfo(name = "name_english") val nameEnglish: String,
    val meaning: String,
    val explanation: String,
    val source: String,
    @ColumnInfo(name = "display_order") val displayOrder: Int
)

@Entity(
    tableName = "asma_un_nabi_bookmarks",
    indices = [Index(value = ["name_id"], unique = true)]
)
data class AsmaUnNabiBookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name_id") val nameId: Int,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
