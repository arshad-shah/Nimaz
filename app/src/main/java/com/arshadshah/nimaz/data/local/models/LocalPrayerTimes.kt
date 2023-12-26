package com.arshadshah.nimaz.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.arshadshah.nimaz.data.remote.models.LocalDateSerializer
import com.arshadshah.nimaz.data.remote.models.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "prayer_times")
@Serializable
data class LocalPrayerTimes(
    @PrimaryKey
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate = LocalDate.now(),
    @Serializable(with = LocalDateTimeSerializer::class)
    var fajr: LocalDateTime? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    var sunrise: LocalDateTime? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    var dhuhr: LocalDateTime? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    var asr: LocalDateTime? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    var maghrib: LocalDateTime? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    var isha: LocalDateTime? = null,
)