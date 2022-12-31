package com.arshadshah.nimaz.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.arshadshah.nimaz.data.remote.models.Prayertime
import java.time.LocalDateTime

@Entity(tableName = "prayer_times")
data class LocalPrayerTimes(
    @PrimaryKey
    val timeStamp: LocalDateTime = LocalDateTime.now() ,
    val fajr: LocalDateTime? = null ,
    val sunrise: LocalDateTime? = null ,
    val dhuhr: LocalDateTime? = null ,
    val asr: LocalDateTime? = null ,
    val maghrib: LocalDateTime? = null ,
    val isha: LocalDateTime? = null ,
    val nextPrayer: Prayertime? = null ,
    val currentPrayer: Prayertime? = null ,
)