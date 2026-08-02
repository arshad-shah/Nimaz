package com.arshadshah.nimaz.domain.model

import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.time.Instant

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

/**
 * Where prayer times are computed for when the reader has not set a location.
 *
 * Onboarding can be skipped and the location permission can be denied, so this is reached in
 * normal use. It is a *stand-in*, not a claim about where the reader is — [ResolvedLocation]
 * carries `isFallback` so a surface can say so instead of asserting a city.
 */
object FallbackLocation {
    const val LATITUDE = 53.3498
    const val LONGITUDE = -6.2603
    const val NAME = "Dublin, Ireland"
}

/**
 * A position to compute with, and whether it is the reader's own or [FallbackLocation].
 *
 * [name] is empty when the position is real but unnamed — reverse geocoding can fail while the
 * fix itself is good.
 */
data class ResolvedLocation(
    val latitude: Double,
    val longitude: Double,
    val name: String,
    val isFallback: Boolean
)

/**
 * Whether these coordinates are a position the reader actually has.
 *
 * `(0, 0)` is Null Island, open water in the Gulf of Guinea, and is the sentinel the
 * preferences store writes before anything is set — so it means "unset", but **only when both
 * axes are zero**. Testing them separately, as five call sites used to, relocates anyone on the
 * equator or the prime meridian onto one of Dublin's axes.
 *
 * Out-of-range and non-finite values count as unset too: a corrupt or wrongly-typed preference
 * should not reach the prayer-time calculator as a real position.
 */
fun isLocationSet(latitude: Double, longitude: Double): Boolean =
    latitude.isFinite() && longitude.isFinite() &&
            latitude in -90.0..90.0 && longitude in -180.0..180.0 &&
            !(latitude == 0.0 && longitude == 0.0)

/** The coordinates to compute with: the reader's own, or [FallbackLocation] whole. */
fun resolveLocation(
    latitude: Double,
    longitude: Double,
    name: String = ""
): ResolvedLocation = if (isLocationSet(latitude, longitude)) {
    ResolvedLocation(latitude, longitude, name.trim(), isFallback = false)
} else {
    ResolvedLocation(
        FallbackLocation.LATITUDE,
        FallbackLocation.LONGITUDE,
        FallbackLocation.NAME,
        isFallback = true
    )
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

    /**
     * Compact label for dense UI (e.g. the prayer-times header). Centralised
     * here so screens/view-models don't each keep their own copy.
     */
    fun shortName(): String {
        return when (this) {
            MUSLIM_WORLD_LEAGUE -> "MWL"
            EGYPTIAN -> "Egyptian"
            KARACHI -> "Karachi"
            UMM_AL_QURA -> "Umm al-Qura"
            DUBAI -> "Dubai"
            MOON_SIGHTING_COMMITTEE -> "Moonsighting"
            NORTH_AMERICA -> "ISNA"
            KUWAIT -> "Kuwait"
            QATAR -> "Qatar"
            SINGAPORE -> "Singapore"
            TURKEY -> "Turkey"
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

    /** Compact label for dense UI. Centralised so view-models don't duplicate it. */
    fun shortName(): String {
        return when (this) {
            STANDARD -> "Standard"
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
    val perfectDays: Int,
    val startDate: Long,
    val endDate: Long
)

data class CurrentPrayerInfo(
    val currentPrayer: PrayerName?,
    val nextPrayer: PrayerName,
    val nextPrayerTime: LocalDateTime,
    val timeUntilNext: Long // in millis
)
