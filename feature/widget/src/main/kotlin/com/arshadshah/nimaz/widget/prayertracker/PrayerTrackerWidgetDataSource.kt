package com.arshadshah.nimaz.widget.prayertracker

import com.arshadshah.nimaz.core.common.toUtcMidnightMillis
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.repository.PrayerRepository
import com.arshadshah.nimaz.domain.time.TodayProvider
import javax.inject.Inject
import kotlinx.coroutines.flow.first

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
 *
 * A third changed when `widget/` became `:feature:widget` in PR 13 of #551. This injected
 * `PrayerDao` directly — a `:core:database` type — so a widget was reaching past its repository
 * into the database. Nothing objected while both lived in `:app`; the module boundary turned it
 * into an unresolved reference. It now takes [PrayerRepository], which `:core:domain` already
 * declared with exactly the query needed.
 *
 * That also deleted the stringly-typed comparison. The DAO returned rows whose `prayerName` and
 * `status` were `String`, so this matched on the literals `"fajr"` and `"prayed"`; the repository
 * returns [PrayerName] and [PrayerStatus], and a typo in an enum does not compile.
 */
class PrayerTrackerWidgetDataSource @Inject constructor(
    private val prayerRepository: PrayerRepository,
    private val todayProvider: TodayProvider,
) {

    suspend fun load(): PrayerTrackerData {
        val today = todayProvider.today()
        val records = prayerRepository
            .getPrayerRecordsForDate(today.toUtcMidnightMillis())
            .first()
        val statuses = records.associate { it.prayerName to it.status }

        // One source for both the per-prayer flags and the count. Deriving the count from the
        // same list is what stops "3 of 5" disagreeing with the five ticks beside it.
        val prayed = PRAYER_KEYS.associateWith { statuses[it] == PrayerStatus.PRAYED }

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
        val FAJR = PrayerName.FAJR
        val DHUHR = PrayerName.DHUHR
        val ASR = PrayerName.ASR
        val MAGHRIB = PrayerName.MAGHRIB
        val ISHA = PrayerName.ISHA

        /**
         * The five obligatory prayers, in order. Deliberately not [PrayerName.entries]: that
         * includes `SUNRISE`, which is a time rather than a prayer and would make the widget read
         * "3 of 6".
         *
         * Anything other than [PrayerStatus.PRAYED] — missed, qada, pending, late — counts as not
         * prayed, which is why the comparison is equality rather than its negation.
         */
        val PRAYER_KEYS = listOf(FAJR, DHUHR, ASR, MAGHRIB, ISHA)
    }
}
