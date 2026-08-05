package com.arshadshah.nimaz.presentation.viewmodel.tracker

import com.arshadshah.nimaz.domain.model.ExemptionReason
import com.arshadshah.nimaz.domain.model.FastStatus
import com.arshadshah.nimaz.domain.model.FastType
import com.arshadshah.nimaz.domain.model.MakeupFast
import java.time.LocalDate

sealed interface FastingEvent {
    data class SelectDate(val date: LocalDate) : FastingEvent
    data class StartFast(val date: LocalDate, val fastType: FastType) : FastingEvent
    data class CompleteFast(val date: LocalDate) : FastingEvent
    data class BreakFast(val date: LocalDate) : FastingEvent
    data class MissFast(val date: LocalDate, val reason: String?) : FastingEvent
    data class SetFastType(val fastType: FastType) : FastingEvent
    data class SelectMonth(val month: Int, val year: Int) : FastingEvent
    data class AddMakeupFast(val makeupFast: MakeupFast) : FastingEvent
    data class CompleteMakeupFast(val makeupFastId: Long) : FastingEvent
    data class PayFidya(val makeupFastId: Long, val amount: Double) : FastingEvent
    data class SetStatsPeriod(val period: FastingStatsPeriod) : FastingEvent
    data object LoadToday : FastingEvent
    data object LoadRamadan : FastingEvent
    data object LoadMakeupFasts : FastingEvent
    data object LoadStats : FastingEvent
    data object ToggleTodayFast : FastingEvent
    data class OpenFastSheet(val date: LocalDate) : FastingEvent
    data object DismissFastSheet : FastingEvent
    data class SaveFastForDate(
        val date: LocalDate,
        val status: FastStatus,
        val fastType: FastType,
        val exemptionReason: ExemptionReason?,
        val note: String
    ) : FastingEvent

    data class DeleteFastRecord(val date: LocalDate) : FastingEvent
    data class UpdateMakeupFast(val makeupFast: MakeupFast) : FastingEvent
    data class LogRecommendedFast(val date: LocalDate, val fastType: FastType) : FastingEvent
}
