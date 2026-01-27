package com.arshadshah.nimaz.domain.model

import kotlinx.datetime.Instant
import java.time.LocalDate
import java.time.LocalDateTime

enum class PrayerType(val displayName: String) {
    FAJR("Fajr"),
    SUNRISE("Sunrise"),
    DHUHR("Dhuhr"),
    ASR("Asr"),
    MAGHRIB("Maghrib"),
    ISHA("Isha")
}

data class PrayerTime(
    val type: PrayerType,
    val time: Instant
)

data class PrayerTimes(
    val fajr: LocalDateTime,
    val sunrise: LocalDateTime,
    val dhuhr: LocalDateTime,
    val asr: LocalDateTime,
    val maghrib: LocalDateTime,
    val isha: LocalDateTime,
    val date: LocalDate,
    val location: Location
)

data class PrayerRecord(
    val id: Long,
    val date: Long,
    val prayerName: PrayerName,
    val status: PrayerStatus,
    val prayedAt: Long?,
    val scheduledTime: Long,
    val isJamaah: Boolean,
    val isQadaFor: Long?,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long
)

enum class PrayerName {
    FAJR,
    SUNRISE,
    DHUHR,
    ASR,
    MAGHRIB,
    ISHA;

    companion object {
        fun fromString(value: String): PrayerName {
            return when (value.lowercase()) {
                "fajr" -> FAJR
                "sunrise" -> SUNRISE
                "dhuhr", "zuhr" -> DHUHR
                "asr" -> ASR
                "maghrib" -> MAGHRIB
                "isha" -> ISHA
                else -> FAJR
            }
        }
    }

    fun displayName(): String {
        return when (this) {
            FAJR -> "Fajr"
            SUNRISE -> "Sunrise"
            DHUHR -> "Dhuhr"
            ASR -> "Asr"
            MAGHRIB -> "Maghrib"
            ISHA -> "Isha"
        }
    }
}

enum class PrayerStatus {
    PRAYED,
    MISSED,
    QADA,
    PENDING,
    LATE,
    NOT_PRAYED;

    companion object {
        fun fromString(value: String): PrayerStatus {
            return when (value.lowercase()) {
                "prayed" -> PRAYED
                "missed" -> MISSED
                "qada" -> QADA
                "pending" -> PENDING
                "late" -> LATE
                "not_prayed", "notprayed" -> NOT_PRAYED
                else -> PENDING
            }
        }
    }
}

data class Location(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val country: String?,
    val city: String?,
    val isCurrentLocation: Boolean,
    val isFavorite: Boolean,
    val calculationMethod: CalculationMethod,
    val asrCalculation: AsrCalculation,
    val highLatitudeRule: HighLatitudeRule?,
    val fajrAngle: Double?,
    val ishaAngle: Double?
)

enum class CalculationMethod {
    MUSLIM_WORLD_LEAGUE,
    EGYPTIAN,
    KARACHI,
    UMM_AL_QURA,
    DUBAI,
    MOON_SIGHTING_COMMITTEE,
    NORTH_AMERICA,
    KUWAIT,
    QATAR,
    SINGAPORE,
    TURKEY;

    companion object {
        fun fromString(value: String?): CalculationMethod {
            return when (value?.uppercase()) {
                "MWL", "MUSLIM_WORLD_LEAGUE" -> MUSLIM_WORLD_LEAGUE
                "EGYPTIAN", "EGYPT" -> EGYPTIAN
                "KARACHI" -> KARACHI
                "UMM_AL_QURA", "MAKKAH" -> UMM_AL_QURA
                "DUBAI" -> DUBAI
                "MOON_SIGHTING_COMMITTEE", "MOONSIGHTING" -> MOON_SIGHTING_COMMITTEE
                "NORTH_AMERICA", "ISNA" -> NORTH_AMERICA
                "KUWAIT" -> KUWAIT
                "QATAR" -> QATAR
                "SINGAPORE" -> SINGAPORE
                "TURKEY" -> TURKEY
                else -> MUSLIM_WORLD_LEAGUE
            }
        }
    }

    fun displayName(): String {
        return when (this) {
            MUSLIM_WORLD_LEAGUE -> "Muslim World League"
            EGYPTIAN -> "Egyptian General Authority"
            KARACHI -> "University of Islamic Sciences, Karachi"
            UMM_AL_QURA -> "Umm Al-Qura University, Makkah"
            DUBAI -> "Dubai"
            MOON_SIGHTING_COMMITTEE -> "Moon Sighting Committee"
            NORTH_AMERICA -> "Islamic Society of North America"
            KUWAIT -> "Kuwait"
            QATAR -> "Qatar"
            SINGAPORE -> "Singapore"
            TURKEY -> "Diyanet, Turkey"
        }
    }
}

enum class AsrCalculation {
    STANDARD,  // Shafi'i, Maliki, Hanbali
    HANAFI;    // Hanafi

    companion object {
        fun fromString(value: String?): AsrCalculation {
            return when (value?.lowercase()) {
                "hanafi" -> HANAFI
                else -> STANDARD
            }
        }
    }

    fun displayName(): String {
        return when (this) {
            STANDARD -> "Standard (Shafi'i)"
            HANAFI -> "Hanafi"
        }
    }
}

enum class HighLatitudeRule {
    MIDDLE_OF_THE_NIGHT,
    SEVENTH_OF_THE_NIGHT,
    TWILIGHT_ANGLE;

    companion object {
        fun fromString(value: String?): HighLatitudeRule? {
            return when (value?.lowercase()) {
                "middle_of_night", "middle_of_the_night" -> MIDDLE_OF_THE_NIGHT
                "seventh_of_night", "seventh_of_the_night" -> SEVENTH_OF_THE_NIGHT
                "twilight_angle" -> TWILIGHT_ANGLE
                else -> null
            }
        }
    }
}

data class PrayerStats(
    val totalPrayed: Int,
    val totalMissed: Int,
    val totalJamaah: Int,
    val prayedByPrayer: Map<PrayerName, Int>,
    val missedByPrayer: Map<PrayerName, Int>,
    val currentStreak: Int,
    val longestStreak: Int,
    val startDate: Long,
    val endDate: Long
)

data class CurrentPrayerInfo(
    val currentPrayer: PrayerName?,
    val nextPrayer: PrayerName,
    val nextPrayerTime: LocalDateTime,
    val timeUntilNext: Long // in millis
)
