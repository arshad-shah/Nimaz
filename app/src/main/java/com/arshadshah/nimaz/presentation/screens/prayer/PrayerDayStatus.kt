package com.arshadshah.nimaz.presentation.screens.prayer

import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerRecord
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.PrayerTimes
import com.arshadshah.nimaz.presentation.model.PrayerDisplayStatus
import java.time.LocalDate
import java.time.LocalDateTime

/** The five obligatory prayers, in the order they fall. `SUNRISE` is a time, not a prayer. */
val TRACKED_PRAYERS: List<PrayerName> = listOf(
    PrayerName.FAJR,
    PrayerName.DHUHR,
    PrayerName.ASR,
    PrayerName.MAGHRIB,
    PrayerName.ISHA,
)

// PrayerDisplayStatus, isDone(), tone(), dotStyle() live in
// com.arshadshah.nimaz.presentation.model.PrayerDisplayStatus so that organisms
// (HomePrayerCard) can share the same atom without importing from a screen package.

/**
 * Resolve every tracked prayer's displayed status for [date].
 *
 * A record counts only when it carries an **assertion** — `PRAYED`, `LATE`, `MISSED` or `QADA`.
 * A missing row, a `PENDING` row and a `NOT_PRAYED` row all say the same thing (nobody has said),
 * so all three fall through to the derivation. That equivalence is also what makes the picker's
 * tap-to-clear free: clearing writes `NOT_PRAYED` and the row reads back as [NOT_RECORDED].
 *
 * @param times the day's schedule, or `null` when no location is set yet. Without times there is
 *   no basis to claim a prayer has passed, so on [date] == today everything reads [UPCOMING];
 *   a date wholly in the past still resolves, because the day being over is enough.
 * @param now read from a ticking clock by the caller. A bare `LocalDateTime.now()` is not
 *   observable state, so a screen holding one would not re-resolve as a prayer time arrives.
 */
fun resolvePrayerStatuses(
    records: List<PrayerRecord>,
    times: PrayerTimes?,
    date: LocalDate,
    now: LocalDateTime,
): Map<PrayerName, PrayerDisplayStatus> {
    val today = now.toLocalDate()
    val dayIsOver = date.isBefore(today)
    val dayIsFuture = date.isAfter(today)

    val asserted = records
        .mapNotNull { rec -> rec.status.asAssertion()?.let { rec.prayerName to it } }
        .toMap()

    return TRACKED_PRAYERS.associateWith { prayer ->
        asserted[prayer] ?: when {
            dayIsFuture -> PrayerDisplayStatus.UPCOMING
            dayIsOver -> PrayerDisplayStatus.NOT_RECORDED
            // Today. `isAfter` rather than `!isBefore`: a prayer at exactly its own time has
            // arrived, not passed, and calling it unrecorded on the minute is a false accusation.
            times?.timeFor(prayer)
                ?.let { now.isAfter(it) } == true -> PrayerDisplayStatus.NOT_RECORDED

            else -> PrayerDisplayStatus.UPCOMING
        }
    }
}

/** The stored status as an assertion, or `null` when it asserts nothing. */
private fun PrayerStatus.asAssertion(): PrayerDisplayStatus? = when (this) {
    PrayerStatus.PRAYED -> PrayerDisplayStatus.PRAYED
    PrayerStatus.LATE -> PrayerDisplayStatus.LATE
    PrayerStatus.QADA -> PrayerDisplayStatus.QADA
    PrayerStatus.MISSED -> PrayerDisplayStatus.MISSED
    PrayerStatus.PENDING, PrayerStatus.NOT_PRAYED -> null
}

/** The scheduled time of one tracked prayer, or `null` for `SUNRISE`. */
fun PrayerTimes.timeFor(prayer: PrayerName): LocalDateTime? = when (prayer) {
    PrayerName.FAJR -> fajr
    PrayerName.DHUHR -> dhuhr
    PrayerName.ASR -> asr
    PrayerName.MAGHRIB -> maghrib
    PrayerName.ISHA -> isha
    PrayerName.SUNRISE -> null
}

// isDone(), tone(), dotStyle() are defined in PrayerDisplayStatus.kt (presentation/model)
