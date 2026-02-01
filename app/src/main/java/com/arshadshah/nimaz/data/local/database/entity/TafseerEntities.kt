package com.arshadshah.nimaz.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tafseer_texts",
    foreignKeys = [
        ForeignKey(
            entity = AyahEntity::class,
            parentColumns = ["id"],
            childColumns = ["ayah_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["ayah_id"]),
        Index(value = ["tafseer_id"]),
        Index(value = ["ayah_id", "tafseer_id"], unique = true)
    ]
)
data class TafseerTextEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "ayah_id")
    val ayahId: Int,
    @ColumnInfo(name = "surah_number")
    val surahNumber: Int,
    @ColumnInfo(name = "ayah_number")
    val ayahNumber: Int,
    @ColumnInfo(name = "tafseer_id")
    val tafseerId: String,
    val text: String
)

@Entity(
    tableName = "tafseer_highlights",
    indices = [
        Index(value = ["ayah_id", "tafseer_id"])
    ]
)
data class TafseerHighlightEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "ayah_id")
    val ayahId: Int,
    @ColumnInfo(name = "tafseer_id")
    val tafseerId: String,
    @ColumnInfo(name = "start_offset")
    val startOffset: Int,
    @ColumnInfo(name = "end_offset")
    val endOffset: Int,
    val color: String,
    val note: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "tafseer_notes",
    indices = [
        Index(value = ["ayah_id", "tafseer_id"])
    ]
)
data class TafseerNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "ayah_id")
    val ayahId: Int,
    @ColumnInfo(name = "tafseer_id")
    val tafseerId: String,
    val text: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
