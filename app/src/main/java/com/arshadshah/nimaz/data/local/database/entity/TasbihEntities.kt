package com.arshadshah.nimaz.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "tasbih_presets")
data class TasbihPresetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val arabic: String,
    val transliteration: String,
    val translation: String,
    @ColumnInfo(name = "target_count")
    val targetCount: Int, // e.g., 33, 99, 100
    @ColumnInfo(name = "is_custom")
    val isCustom: Int, // 0 = default, 1 = custom
    @ColumnInfo(name = "display_order")
    val displayOrder: Int
)

@Entity(
    tableName = "tasbih_sessions",
    indices = [
        Index(value = ["presetId"]),
        Index(value = ["date"]),
        Index(value = ["isCompleted"])
    ]
)
data class TasbihSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val presetId: Long?, // Null if using free counter
    val presetName: String?, // Store name in case preset is deleted
    val date: Long, // Date in millis (start of day)
    val currentCount: Int,
    val targetCount: Int,
    val totalLaps: Int, // Number of times target was reached
    val isCompleted: Boolean,
    val duration: Long?, // Total time spent in millis
    val startedAt: Long,
    val completedAt: Long?,
    val note: String?
)
