package com.arshadshah.nimaz.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "khatams")
data class KhatamEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val notes: String? = null,
    val status: String = "active", // "active", "completed", "abandoned"
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = false,
    @ColumnInfo(name = "daily_target")
    val dailyTarget: Int = 20,
    val deadline: Long? = null,
    @ColumnInfo(name = "reminder_enabled")
    val reminderEnabled: Boolean = false,
    @ColumnInfo(name = "reminder_time")
    val reminderTime: String? = null, // HH:mm
    @ColumnInfo(name = "total_ayahs_read")
    val totalAyahsRead: Int = 0,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "started_at")
    val startedAt: Long? = null,
    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "khatam_ayahs",
    primaryKeys = ["khatam_id", "ayah_id"],
    foreignKeys = [
        ForeignKey(
            entity = KhatamEntity::class,
            parentColumns = ["id"],
            childColumns = ["khatam_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["khatam_id"]),
        Index(value = ["ayah_id"]),
        Index(value = ["read_at"])
    ]
)
data class KhatamAyahEntity(
    @ColumnInfo(name = "khatam_id")
    val khatamId: Long,
    @ColumnInfo(name = "ayah_id")
    val ayahId: Int,
    @ColumnInfo(name = "read_at")
    val readAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "khatam_daily_log",
    primaryKeys = ["khatam_id", "date"],
    foreignKeys = [
        ForeignKey(
            entity = KhatamEntity::class,
            parentColumns = ["id"],
            childColumns = ["khatam_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["khatam_id"])
    ]
)
data class KhatamDailyLogEntity(
    @ColumnInfo(name = "khatam_id")
    val khatamId: Long,
    val date: Long, // date as epoch millis (start of day)
    @ColumnInfo(name = "ayahs_read")
    val ayahsRead: Int = 0
)
