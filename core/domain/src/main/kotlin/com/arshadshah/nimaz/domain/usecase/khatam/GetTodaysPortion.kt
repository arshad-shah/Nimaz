package com.arshadshah.nimaz.domain.usecase.khatam

import com.arshadshah.nimaz.domain.model.Khatam
import com.arshadshah.nimaz.domain.model.KhatamStatus
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/** The fixed set assigned for one local day; previously read gaps are not assigned again. */
data class KhatamPortion(
    val fromAyahId: Int,
    val toAyahId: Int,
    val ayahIds: List<Int> = (fromAyahId..toAyahId).toList(),
) {
    val ayahCount: Int get() = ayahIds.size
    fun readCount(readAyahIds: Set<Int>): Int = ayahIds.count { it in readAyahIds }
}

class GetTodaysPortion @Inject constructor() {
    /**
     * Reconstruct the unread set at local midnight from durable read marks. Marking today's
     * verses therefore fills this portion instead of moving its finish line. No extra table,
     * preferences or in-memory cache is needed. An undo of an older mark intentionally makes
     * that verse eligible again. A changed target intentionally resizes today's assignment.
     *
     * [nextUnreadAyahId] is a fallback for callers without timestamped marks. Calendar-day
     * boundaries use the device zone (including DST), never an assumed 24-hour duration.
     */
    operator fun invoke(khatam: Khatam, nextUnreadAyahId: Int?): KhatamPortion? =
        portionFrom(khatam, ((nextUnreadAyahId ?: 1).coerceAtLeast(1)..Khatam.TOTAL_QURAN_AYAHS).asSequence())

    fun forDay(
        khatam: Khatam,
        readAtByAyah: Map<Int, Long>,
        now: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): KhatamPortion? {
        val midnight = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
            .atStartOfDay(zone).toInstant().toEpochMilli()
        return portionFrom(khatam, (1..Khatam.TOTAL_QURAN_AYAHS).asSequence()
            .filter { id -> readAtByAyah[id]?.let { it >= midnight } ?: true })
    }

    private fun portionFrom(khatam: Khatam, eligible: Sequence<Int>): KhatamPortion? {
        if (khatam.status != KhatamStatus.ACTIVE || khatam.totalAyahsRead >= Khatam.TOTAL_QURAN_AYAHS) return null
        val target = khatam.dailyTarget.takeIf { it > 0 } ?: return null
        val ids = eligible.take(target).toList()
        return ids.takeIf { it.isNotEmpty() }?.let { KhatamPortion(it.first(), it.last(), it) }
    }
}
