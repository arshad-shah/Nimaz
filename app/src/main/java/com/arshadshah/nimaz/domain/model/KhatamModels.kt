package com.arshadshah.nimaz.domain.model

import com.arshadshah.nimaz.domain.model.KhatamProgressCalculator.averagePace
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
    /**
     * The juz the reader is actually on: the first one not yet finished.
     *
     * Deliberately NOT `juzCompleted + 1` — that is only correct for someone reading
     * strictly in order. Finish only juz 30 and "completed + 1" claims you are on juz 2.
     */
    val currentJuz: Int = 1,
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

    /**
     * A day-by-day reading log, derived from **when each ayah was marked read**.
     *
     * The `khatam_daily_log` table this used to come from is written by exactly one function,
     * `logDailyProgress`, and nothing in the app ever calls it — its only reference is its own
     * DI wiring. So the table was always empty, [currentStreak] and [longestStreak] always ran
     * over an empty list, and the streak on the khatam detail screen read 0 for everyone.
     *
     * `khatam_ayahs.read_at` was being stamped on every mark the whole time, and travels over
     * sync, so deriving from it makes the streak correct for history **already on disk**
     * instead of only for reading done after a fix ships.
     *
     * @param readAt one timestamp per ayah marked read, in any order.
     * @param syncedLogs rows that arrived in `khatam_daily_log` from a peer. A day present in
     *   both takes the larger count rather than the sum: the two describe the same reading, and
     *   a peer on an older build may have sent a day this device has no ayah rows for.
     */
    fun dailyLogsFrom(
        readAt: List<Long>,
        syncedLogs: List<DailyLogEntry> = emptyList()
    ): List<DailyLogEntry> {
        val derived = readAt.groupingBy { startOfDay(it) }.eachCount()
        val synced = syncedLogs.groupBy { startOfDay(it.date) }
            .mapValues { (_, entries) -> entries.sumOf { it.ayahsRead } }

        return (derived.keys + synced.keys)
            .sorted()
            .map { day ->
                DailyLogEntry(
                    date = day,
                    ayahsRead = maxOf(derived[day] ?: 0, synced[day] ?: 0)
                )
            }
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
            currentJuz = juzProgress.firstOrNull { !it.isComplete }?.juzNumber
                ?: Khatam.TOTAL_JUZ,
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
 * Each pair is (startAyahId, endAyahId) inclusive, and the comment on each line names the
 * classical surah:ayah the juz opens on — the reference the ids are derived from.
 *
 * These had drifted badly (#325): juz 7 was off by one and juz 15-30 were wrong by hundreds
 * of ayahs — juz 30 claimed to start at 4090 (mid-Ya-Sin) instead of 5673 (An-Naba 78:1),
 * i.e. a third of the Quran. The Juz tab's khatam rings read this table while the Khatam
 * detail screen groups by the database's own `ayahs.juz` column, so the two disagreed.
 * [com.arshadshah.nimaz.domain.model.KhatamJuzBoundariesTest] re-derives every boundary
 * from the surah ayah counts so it cannot silently drift again.
 */
object KhatamConstants {
    val JUZ_AYAH_RANGES: List<Pair<Int, Int>> = listOf(
        1 to 148,       // Juz 1  — 1:1
        149 to 259,     // Juz 2  — 2:142
        260 to 385,     // Juz 3  — 2:253
        386 to 516,     // Juz 4  — 3:93
        517 to 640,     // Juz 5  — 4:24
        641 to 750,     // Juz 6  — 4:148
        751 to 899,     // Juz 7  — 5:82
        900 to 1041,    // Juz 8  — 6:111
        1042 to 1200,   // Juz 9  — 7:88
        1201 to 1327,   // Juz 10 — 8:41
        1328 to 1478,   // Juz 11 — 9:93
        1479 to 1648,   // Juz 12 — 11:6
        1649 to 1802,   // Juz 13 — 12:53
        1803 to 2029,   // Juz 14 — 15:1
        2030 to 2214,   // Juz 15 — 17:1
        2215 to 2483,   // Juz 16 — 18:75
        2484 to 2673,   // Juz 17 — 21:1
        2674 to 2875,   // Juz 18 — 23:1
        2876 to 3214,   // Juz 19 — 25:21
        3215 to 3385,   // Juz 20 — 27:56
        3386 to 3563,   // Juz 21 — 29:46
        3564 to 3732,   // Juz 22 — 33:31
        3733 to 4089,   // Juz 23 — 36:28
        4090 to 4264,   // Juz 24 — 39:32
        4265 to 4510,   // Juz 25 — 41:47
        4511 to 4705,   // Juz 26 — 46:1
        4706 to 5104,   // Juz 27 — 51:31
        5105 to 5241,   // Juz 28 — 58:1
        5242 to 5672,   // Juz 29 — 67:1
        5673 to 6236    // Juz 30 — 78:1
    )

    /**
     * Juz number (1-30) containing the given sequential ayah id, or null if out of range.
     */
    fun juzForAyahId(ayahId: Int): Int? {
        val index = JUZ_AYAH_RANGES.indexOfFirst { (start, end) -> ayahId in start..end }
        return if (index >= 0) index + 1 else null
    }
}
