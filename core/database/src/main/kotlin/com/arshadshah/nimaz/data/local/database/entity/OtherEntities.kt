package com.arshadshah.nimaz.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String, // e.g., "America/New_York"
    val country: String?,
    val city: String?,
    val isCurrentLocation: Boolean = false,
    val isFavorite: Boolean = false,
    val calculationMethod: String?, // "MWL", "ISNA", "Egypt", etc.
    val asrCalculation: String?, // "standard", "hanafi"
    val highLatitudeRule: String?, // "middle_of_night", "seventh_of_night", "twilight_angle"
    val fajrAngle: Double?,
    val ishaAngle: Double?,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "islamic_events",
    indices = [
        Index(value = ["hijri_month", "hijri_day"])
    ]
)
data class IslamicEventEntity(
    @PrimaryKey
    val id: Int,
    @ColumnInfo(name = "name_english")
    val nameEnglish: String,
    @ColumnInfo(name = "name_arabic")
    val nameArabic: String,
    @ColumnInfo(name = "hijri_month")
    val hijriMonth: Int, // 1-12
    @ColumnInfo(name = "hijri_day")
    val hijriDay: Int, // 1-30
    @ColumnInfo(name = "event_type")
    val eventType: String, // "holiday", "fast", "night", "historical"
    val description: String,
    @ColumnInfo(name = "is_holiday")
    val isHoliday: Int // 0 = false, 1 = true
)
