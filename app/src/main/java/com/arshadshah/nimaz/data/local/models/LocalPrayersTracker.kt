package com.arshadshah.nimaz.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_ASR
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_DHUHR
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_FAJR
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_ISHA
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_MAGHRIB
import com.arshadshah.nimaz.data.remote.models.LocalDateSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Entity(tableName = "PrayersTracker")
@Serializable
data class LocalPrayersTracker(
    @PrimaryKey
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate = LocalDate.now(),
    val fajr: Boolean = false,
    val dhuhr: Boolean = false,
    val asr: Boolean = false,
    val maghrib: Boolean = false,
    val isha: Boolean = false,
    val progress: Int = 0,
    val isMenstruating: Boolean = false,
) {
    fun isPrayerCompleted(prayerName: String): Boolean {
        return when (prayerName) {
            PRAYER_NAME_FAJR -> fajr
            PRAYER_NAME_DHUHR -> dhuhr
            PRAYER_NAME_ASR -> asr
            PRAYER_NAME_MAGHRIB -> maghrib
            PRAYER_NAME_ISHA -> isha
            else -> false
        }
    }
}
