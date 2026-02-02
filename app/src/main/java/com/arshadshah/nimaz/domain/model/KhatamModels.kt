package com.arshadshah.nimaz.domain.model

data class Khatam(
    val id: Long = 0,
    val name: String,
    val notes: String? = null,
    val status: KhatamStatus = KhatamStatus.ACTIVE,
    val isActive: Boolean = false,
    val dailyTarget: Int = 20,
    val deadline: Long? = null,
    val reminderEnabled: Boolean = false,
    val reminderTime: String? = null,
    val totalAyahsRead: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val progressPercent: Float
        get() = if (TOTAL_QURAN_AYAHS > 0) totalAyahsRead.toFloat() / TOTAL_QURAN_AYAHS else 0f

    companion object {
        const val TOTAL_QURAN_AYAHS = 6236
    }
}

enum class KhatamStatus {
    ACTIVE, COMPLETED, ABANDONED;

    companion object {
        fun fromString(value: String): KhatamStatus = when (value.lowercase()) {
            "completed" -> COMPLETED
            "abandoned" -> ABANDONED
            else -> ACTIVE
        }
    }

    fun toDbString(): String = name.lowercase()
}

data class KhatamProgress(
    val khatam: Khatam,
    val totalAyahsRead: Int,
    val totalAyahs: Int = Khatam.TOTAL_QURAN_AYAHS,
    val daysActive: Int,
    val averagePace: Float, // ayahs per day
    val projectedCompletionDate: Long?, // epoch millis
    val juzProgress: List<JuzProgressInfo>,
    val dailyLogs: List<DailyLogEntry>
)

data class JuzProgressInfo(
    val juzNumber: Int,
    val totalAyahs: Int,
    val readAyahs: Int
) {
    val progressPercent: Float
        get() = if (totalAyahs > 0) readAyahs.toFloat() / totalAyahs else 0f
}

data class SurahProgressInfo(
    val surahNumber: Int,
    val surahName: String,
    val totalAyahs: Int,
    val readAyahs: Int
) {
    val progressPercent: Float
        get() = if (totalAyahs > 0) readAyahs.toFloat() / totalAyahs else 0f
}

data class DailyLogEntry(
    val date: Long,
    val ayahsRead: Int
)

data class KhatamStats(
    val totalKhatamsCompleted: Int,
    val totalKhatamsActive: Int,
    val totalAyahsReadAllTime: Int,
    val longestStreak: Int,
    val currentStreak: Int
)

/**
 * Shared juz boundary constants. Ayah IDs are sequential 1-6236 across the entire Quran.
 * Each pair is (startAyahId, endAyahId) inclusive.
 */
object KhatamConstants {
    val JUZ_AYAH_RANGES: List<Pair<Int, Int>> = listOf(
        1 to 148,      // Juz 1
        149 to 259,     // Juz 2
        260 to 385,     // Juz 3
        386 to 516,     // Juz 4
        517 to 640,     // Juz 5
        641 to 751,     // Juz 6
        752 to 899,     // Juz 7
        900 to 1041,    // Juz 8
        1042 to 1200,   // Juz 9
        1201 to 1327,   // Juz 10
        1328 to 1478,   // Juz 11
        1479 to 1648,   // Juz 12
        1649 to 1802,   // Juz 13
        1803 to 1901,   // Juz 14
        1902 to 2029,   // Juz 15
        2030 to 2140,   // Juz 16
        2141 to 2250,   // Juz 17
        2251 to 2348,   // Juz 18
        2349 to 2483,   // Juz 19
        2484 to 2593,   // Juz 20
        2594 to 2732,   // Juz 21
        2733 to 2872,   // Juz 22
        2873 to 3005,   // Juz 23
        3006 to 3121,   // Juz 24
        3122 to 3226,   // Juz 25
        3227 to 3340,   // Juz 26
        3341 to 3510,   // Juz 27
        3511 to 3733,   // Juz 28
        3734 to 4089,   // Juz 29
        4090 to 6236    // Juz 30
    )
}
