package com.arshadshah.nimaz.domain.model

import java.util.concurrent.TimeUnit

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

    val remainingAyahs: Int
        get() = (TOTAL_QURAN_AYAHS - totalAyahsRead).coerceAtLeast(0)

    companion object {
        const val TOTAL_QURAN_AYAHS = 6236
        const val TOTAL_JUZ = 30
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

data class JuzProgressInfo(
    val juzNumber: Int,
    val totalAyahs: Int,
    val readAyahs: Int
) {
    val progressPercent: Float
        get() = if (totalAyahs > 0) readAyahs.toFloat() / totalAyahs else 0f

    val isComplete: Boolean get() = readAyahs >= totalAyahs
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
 * How the reader's actual pace compares to the daily target they set.
 *
 * Derived, never persisted — [KhatamProgressCalculator.paceStatus] is the single
 * place this is decided so the list, detail, home card and widget cannot disagree.
 */
enum class KhatamPace {
    /** No [Khatam.startedAt] yet, or fewer than a full day elapsed. */
    NOT_STARTED,

    /** Meeting or beating the daily target. */
    ON_TRACK,

    /** Behind the daily target but by less than [KhatamProgressCalculator.BEHIND_TOLERANCE]. */
    SLIGHTLY_BEHIND,

    /** Meaningfully behind the daily target. */
    BEHIND
}

/**
 * Everything the UI needs about a khatam's progress that isn't stored on the row itself.
 *
 * Computed once per emission in the repository layer so every surface renders identical
 * numbers. All fields are safe to show directly; callers never re-derive.
 */
data class KhatamInsights(
    val daysActive: Int = 0,
    val averagePace: Float = 0f,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val juzCompleted: Int = 0,
    val remainingAyahs: Int = Khatam.TOTAL_QURAN_AYAHS,
    val estimatedDaysRemaining: Int? = null,
    val projectedCompletionAt: Long? = null,
    val paceStatus: KhatamPace = KhatamPace.NOT_STARTED
)

/**
 * One consistent view of a single khatam: the row, its juz breakdown, its daily logs
 * and the numbers derived from all three.
 *
 * Emitted as a unit so no surface can render a khatam's progress against a stale
 * juz grid or an out-of-date streak.
 */
data class KhatamDetailSnapshot(
    val khatam: Khatam,
    val juzProgress: List<JuzProgressInfo>,
    val dailyLogs: List<DailyLogEntry>,
    val insights: KhatamInsights,
    /**
     * The raw read-ayah set, carried here so the Quran reader's per-ayah ticks and the
     * home card's progress come from one subscription rather than two.
     */
    val readAyahIds: Set<Int>
)

/**
 * Pure progress maths for khatam. Kept free of Android and Room types so it can be
 * unit tested directly and reused by the widget worker and the reminder receiver.
 */
object KhatamProgressCalculator {

    /** Ayahs/day shortfall tolerated before a reader is called [KhatamPace.BEHIND]. */
    const val BEHIND_TOLERANCE = 0.75f

    private val DAY_MILLIS = TimeUnit.DAYS.toMillis(1)

    /**
     * Whole days since the khatam started, floored at 1 once started so
     * [averagePace] never divides by zero on the first day.
     */
    fun daysActive(startedAt: Long?, now: Long = System.currentTimeMillis()): Int {
        if (startedAt == null) return 0
        val elapsed = now - startedAt
        if (elapsed < 0) return 0
        return maxOf(1, (elapsed / DAY_MILLIS).toInt())
    }

    fun averagePace(totalAyahsRead: Int, daysActive: Int): Float =
        if (daysActive > 0) totalAyahsRead.toFloat() / daysActive else 0f

    fun paceStatus(averagePace: Float, dailyTarget: Int, daysActive: Int): KhatamPace = when {
        daysActive <= 0 -> KhatamPace.NOT_STARTED
        dailyTarget <= 0 -> KhatamPace.ON_TRACK
        averagePace >= dailyTarget -> KhatamPace.ON_TRACK
        averagePace >= dailyTarget * BEHIND_TOLERANCE -> KhatamPace.SLIGHTLY_BEHIND
        else -> KhatamPace.BEHIND
    }

    /**
     * Days to finish the remainder at the reader's *actual* pace, falling back to their
     * target when there is not yet a meaningful pace. Null once nothing remains.
     */
    fun estimatedDaysRemaining(remainingAyahs: Int, averagePace: Float, dailyTarget: Int): Int? {
        if (remainingAyahs <= 0) return null
        val rate = if (averagePace > 0f) averagePace else dailyTarget.toFloat()
        if (rate <= 0f) return null
        return Math.ceil((remainingAyahs / rate).toDouble()).toInt()
    }

    /**
     * Consecutive days ending today (or yesterday) with reading logged.
     *
     * Counting from yesterday keeps a streak alive during the part of today
     * before the reader has opened the app.
     */
    fun currentStreak(logs: List<DailyLogEntry>, now: Long = System.currentTimeMillis()): Int {
        val days = logs.filter { it.ayahsRead > 0 }
            .map { startOfDay(it.date) }
            .distinct()
            .sortedDescending()
        if (days.isEmpty()) return 0

        val today = startOfDay(now)
        val mostRecent = days.first()
        // A gap of more than one day means the streak is already broken.
        if (today - mostRecent > DAY_MILLIS) return 0

        var streak = 1
        var expected = mostRecent - DAY_MILLIS
        for (day in days.drop(1)) {
            if (day != expected) break
            streak++
            expected -= DAY_MILLIS
        }
        return streak
    }

    /** Longest run of consecutive logged days anywhere in the history. */
    fun longestStreak(logs: List<DailyLogEntry>): Int {
        val days = logs.filter { it.ayahsRead > 0 }
            .map { startOfDay(it.date) }
            .distinct()
            .sorted()
        if (days.isEmpty()) return 0

        var longest = 1
        var run = 1
        for (i in 1 until days.size) {
            if (days[i] - days[i - 1] == DAY_MILLIS) run++ else run = 1
            if (run > longest) longest = run
        }
        return longest
    }

    fun insights(
        khatam: Khatam,
        logs: List<DailyLogEntry>,
        juzProgress: List<JuzProgressInfo>,
        now: Long = System.currentTimeMillis()
    ): KhatamInsights {
        val days = daysActive(khatam.startedAt, now)
        val pace = averagePace(khatam.totalAyahsRead, days)
        val remaining = khatam.remainingAyahs
        val estDays = estimatedDaysRemaining(remaining, pace, khatam.dailyTarget)
        return KhatamInsights(
            daysActive = days,
            averagePace = pace,
            currentStreak = currentStreak(logs, now),
            longestStreak = longestStreak(logs),
            juzCompleted = juzProgress.count { it.isComplete },
            remainingAyahs = remaining,
            estimatedDaysRemaining = estDays,
            projectedCompletionAt = estDays?.let { now + it * DAY_MILLIS },
            paceStatus = paceStatus(pace, khatam.dailyTarget, days)
        )
    }

    /**
     * Normalises an epoch-millis timestamp to local midnight.
     *
     * Daily logs are already written as start-of-day, but sync imports and older rows
     * are not guaranteed to be, so streak maths normalises defensively.
     */
    private fun startOfDay(timestamp: Long): Long {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}

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

    /**
     * Juz number (1-30) containing the given sequential ayah id, or null if out of range.
     */
    fun juzForAyahId(ayahId: Int): Int? {
        val index = JUZ_AYAH_RANGES.indexOfFirst { (start, end) -> ayahId in start..end }
        return if (index >= 0) index + 1 else null
    }
}
