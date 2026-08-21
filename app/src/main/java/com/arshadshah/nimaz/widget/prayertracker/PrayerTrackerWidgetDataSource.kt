package com.arshadshah.nimaz.widget.prayertracker

import com.arshadshah.nimaz.domain.time.TodayProvider
import com.arshadshah.nimaz.core.util.toUtcMidnightMillis
import com.arshadshah.nimaz.data.local.database.dao.PrayerDao
import javax.inject.Inject

/**
 * Computes what the prayer-tracker widget shows.
 *
 * Split out of [PrayerTrackerWorker] so it can be tested — `doWork()` returns early when no
 * widget is placed, which is always true on a test device, so none of this ran under
 * `WidgetWorkersTest` (#474).
 *
 * Two things changed on the way out. It takes [TodayProvider] rather than calling
 * `LocalDate.now()` directly — the same seam the registry introduced when it removed that call
 * from ten ViewModels, and without it a test cannot pin the date label. And the five prayers are
 * a list rather than five copy-pasted comparisons: the original wrote
 * `recordMap["fajr"] == "prayed"` ten times, once for the boolean and again inside the count, so
 * the two could disagree.
 */
class PrayerTrackerWidgetDataSource @Inject constructor(
    private val prayerDao: PrayerDao,
    private val todayProvider: TodayProvider,
) {

    suspend fun load(): PrayerTrackerData {
        val today = todayProvider.today()
        val records = prayerDao.getPrayerRecordsForDateSync(today.toUtcMidnightMillis())
        val statuses = records.associate { it.prayerName to it.status }

        // One source for both the per-prayer flags and the count. Deriving the count from the
        // same list is what stops "3 of 5" disagreeing with the five ticks beside it.
        val prayed = PRAYER_KEYS.associateWith { statuses[it] == STATUS_PRAYED }

        return PrayerTrackerData(
            dateLabel = today.dayOfWeek.name.take(3).lowercase()
                .replaceFirstChar { it.uppercase() },
            fajr = prayed.getValue(FAJR),
            dhuhr = prayed.getValue(DHUHR),
            asr = prayed.getValue(ASR),
            maghrib = prayed.getValue(MAGHRIB),
            isha = prayed.getValue(ISHA),
            prayedCount = prayed.count { it.value },
            totalCount = PRAYER_KEYS.size,
        )
    }

    private companion object {
        const val FAJR = "fajr"
        const val DHUHR = "dhuhr"
        const val ASR = "asr"
        const val MAGHRIB = "maghrib"
        const val ISHA = "isha"

        /**
         * The stored status meaning "prayed". Anything else — missed, excused, or absent —
         * counts as not prayed, which is why this compares equal rather than not-equal.
         */
        const val STATUS_PRAYED = "prayed"

        val PRAYER_KEYS = listOf(FAJR, DHUHR, ASR, MAGHRIB, ISHA)
    }
}
