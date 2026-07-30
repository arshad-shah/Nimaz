package com.arshadshah.nimaz.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single commentary passage, covering a contiguous ayah range within one surah
 * (e.g. Ibn Kathir discussing 43:81-89 as one block) rather than being repeated
 * per ayah. See issue #329 / schemaVersion 21.
 */
@Entity(
    tableName = "tafseer_blocks",
    indices = [
        Index(value = ["tafseer_id", "surah_number", "ayah_start", "ayah_end"])
    ]
)
data class TafseerBlockEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "tafseer_id")
    val tafseerId: String,
    @ColumnInfo(name = "surah_number")
    val surahNumber: Int,
    @ColumnInfo(name = "ayah_start")
    val ayahStart: Int,
    @ColumnInfo(name = "ayah_end")
    val ayahEnd: Int,
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
