package com.arshadshah.nimaz.domain.model

data class FastRecord(
    val id: Long,
    val date: Long,
    val hijriDate: String?,
    val hijriMonth: Int?,
    val hijriYear: Int?,
    val fastType: FastType,
    val status: FastStatus,
    val exemptionReason: ExemptionReason?,
    val suhoorTime: Long?,
    val iftarTime: Long?,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long
)

data class MakeupFast(
    val id: Long,
    val originalDate: Long,
    val originalHijriDate: String?,
    val reason: String,
    val status: MakeupFastStatus,
    val completedDate: Long?,
    val fidyaAmount: Double?,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long
)

enum class FastType {
    RAMADAN,
    VOLUNTARY,
    MAKEUP,
    EXPIATION,
    VOW;

    companion object {
        fun fromString(value: String): FastType {
            return when (value.lowercase()) {
                "ramadan" -> RAMADAN
                "voluntary", "nafl" -> VOLUNTARY
                "makeup", "qada" -> MAKEUP
                "expiation", "kaffarah" -> EXPIATION
                "vow", "nadhr" -> VOW
                else -> VOLUNTARY
            }
        }
    }

    fun displayName(): String {
        return when (this) {
            RAMADAN -> "Ramadan"
            VOLUNTARY -> "Voluntary"
            MAKEUP -> "Makeup (Qada)"
            EXPIATION -> "Expiation (Kaffarah)"
            VOW -> "Vow (Nadhr)"
        }
    }
}

enum class FastStatus {
    FASTED,
    NOT_FASTED,
    EXEMPTED,
    MAKEUP_DUE;

    companion object {
        fun fromString(value: String): FastStatus {
            return when (value.lowercase()) {
                "fasted" -> FASTED
                "not_fasted", "notfasted" -> NOT_FASTED
                "exempted" -> EXEMPTED
                "makeup_due", "makeupdue" -> MAKEUP_DUE
                else -> NOT_FASTED
            }
        }
    }
}

enum class ExemptionReason {
    TRAVEL,
    ILLNESS,
    MENSTRUATION,
    PREGNANCY,
    BREASTFEEDING,
    ELDERLY,
    OTHER;

    companion object {
        fun fromString(value: String?): ExemptionReason? {
            return when (value?.lowercase()) {
                "travel" -> TRAVEL
                "illness", "sick" -> ILLNESS
                "menstruation", "period" -> MENSTRUATION
                "pregnancy" -> PREGNANCY
                "breastfeeding" -> BREASTFEEDING
                "elderly", "old_age" -> ELDERLY
                "other" -> OTHER
                else -> null
            }
        }
    }

    fun displayName(): String {
        return when (this) {
            TRAVEL -> "Travel"
            ILLNESS -> "Illness"
            MENSTRUATION -> "Menstruation"
            PREGNANCY -> "Pregnancy"
            BREASTFEEDING -> "Breastfeeding"
            ELDERLY -> "Elderly"
            OTHER -> "Other"
        }
    }
}

enum class MakeupFastStatus {
    PENDING,
    COMPLETED,
    FIDYA_PAID;

    companion object {
        fun fromString(value: String): MakeupFastStatus {
            return when (value.lowercase()) {
                "pending" -> PENDING
                "completed" -> COMPLETED
                "fidya_paid", "fidyapaid" -> FIDYA_PAID
                else -> PENDING
            }
        }
    }
}

data class FastingStats(
    val totalFasted: Int,
    val ramadanFasted: Int,
    val voluntaryFasted: Int,
    val pendingMakeupCount: Int,
    val totalFidyaPaid: Double,
    val currentStreak: Int,
    val startDate: Long,
    val endDate: Long
)

data class RamadanProgress(
    val year: Int,
    val totalDays: Int,
    val fastedDays: Int,
    val missedDays: Int,
    val exemptedDays: Int,
    val remainingDays: Int,
    val records: List<FastRecord>
)
