package com.arshadshah.nimaz.presentation.viewmodel.tracker

import com.arshadshah.nimaz.domain.model.ExemptionReason
import com.arshadshah.nimaz.domain.model.FastStatus
import com.arshadshah.nimaz.domain.model.MakeupFast
import java.time.LocalDate

sealed interface FastingEvent {
    data class SelectDate(val date: LocalDate) : FastingEvent
    data class SelectMonth(val month: Int, val year: Int) : FastingEvent
    data class CompleteMakeupFast(val makeupFastId: Long) : FastingEvent
    data class PayFidya(val makeupFastId: Long, val amount: Double) : FastingEvent
    data class SetStatsPeriod(val period: FastingStatsPeriod) : FastingEvent
    data object LoadToday : FastingEvent
    data object LoadRamadan : FastingEvent
    data object LoadMakeupFasts : FastingEvent
    data object LoadStats : FastingEvent
    data class UpdateMakeupFast(val makeupFast: MakeupFast) : FastingEvent

    /**
     * Writes [status] for [date] straight from the day card's segmented control.
     *
     * Passing the status the day **already** has deletes the record, which is what makes the
     * control tap-to-clear. The fast type is not a parameter: a Ramadan day is a Ramadan fast and
     * anything else is voluntary, inferred rather than asked.
     */
    data class SetFastStatus(val date: LocalDate, val status: FastStatus) : FastingEvent

    /** Records [date] as exempted with [reason]. Raised by the reason sheet, not the control. */
    data class SaveExemption(val date: LocalDate, val reason: ExemptionReason) : FastingEvent

    /** Attaches [note] to [date] without disturbing whatever status it already carries. */
    data class SaveNote(val date: LocalDate, val note: String) : FastingEvent
}
