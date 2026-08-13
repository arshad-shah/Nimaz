package com.arshadshah.nimaz.domain.usecase.khatam

import com.arshadshah.nimaz.domain.model.Khatam
import javax.inject.Inject

/**
 * The stretch of the Qur'an this plan asks for today.
 *
 * A khatam exists to assign a **daily portion**; where you happened to stop is the fallback, not
 * the headline. The detail screen led with "resume where you stopped", which answers a question
 * about the past — the plan's actual instruction, *read this much today*, was nowhere on it.
 *
 * Today's portion always starts at the **next unread verse**, never at the backlog: a reader two
 * days behind is asked for today's twenty verses, not sixty. Falling behind is reported by the
 * pace status, which is a different sentence and does not belong inside the instruction.
 */
data class KhatamPortion(
    val fromAyahId: Int,
    val toAyahId: Int,
) {
    /** How many verses today asks for. */
    val ayahCount: Int get() = (toAyahId - fromAyahId + 1).coerceAtLeast(0)
}

class GetTodaysPortion @Inject constructor() {

    /**
     * @param nextUnreadAyahId the global id of the first verse not yet marked read, or null
     *   when nothing has been read yet — in which case the portion starts at the beginning.
     * @return null once the plan is finished; there is no portion for a completed khatam, and
     *   an empty range shown as one would be a plan still giving orders after it is done.
     */
    operator fun invoke(khatam: Khatam, nextUnreadAyahId: Int?): KhatamPortion? {
        val target = khatam.dailyTarget.takeIf { it > 0 } ?: return null
        val start = (nextUnreadAyahId ?: 1).coerceAtLeast(1)
        if (start > Khatam.TOTAL_QURAN_AYAHS) return null
        val end = (start + target - 1).coerceAtMost(Khatam.TOTAL_QURAN_AYAHS)
        return KhatamPortion(fromAyahId = start, toAyahId = end)
    }
}
