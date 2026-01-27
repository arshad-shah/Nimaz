package com.arshadshah.nimaz.domain.model

import java.time.LocalDate

data class HijriDate(
    val day: Int,
    val month: Int,
    val year: Int
) {
    val monthName: String
        get() = HijriMonth.fromNumber(month)?.displayName() ?: "Unknown"

    val monthNameArabic: String
        get() = HijriMonth.fromNumber(month)?.arabicName() ?: ""

    fun toFormattedString(): String = "$day $monthName $year AH"
}

enum class HijriMonth(val number: Int) {
    MUHARRAM(1),
    SAFAR(2),
    RABI_AL_AWWAL(3),
    RABI_AL_THANI(4),
    JUMADA_AL_AWWAL(5),
    JUMADA_AL_THANI(6),
    RAJAB(7),
    SHABAN(8),
    RAMADAN(9),
    SHAWWAL(10),
    DHU_AL_QADAH(11),
    DHU_AL_HIJJAH(12);

    companion object {
        fun fromNumber(number: Int): HijriMonth? = entries.find { it.number == number }
    }

    fun displayName(): String {
        return when (this) {
            MUHARRAM -> "Muharram"
            SAFAR -> "Safar"
            RABI_AL_AWWAL -> "Rabi' al-Awwal"
            RABI_AL_THANI -> "Rabi' al-Thani"
            JUMADA_AL_AWWAL -> "Jumada al-Awwal"
            JUMADA_AL_THANI -> "Jumada al-Thani"
            RAJAB -> "Rajab"
            SHABAN -> "Sha'ban"
            RAMADAN -> "Ramadan"
            SHAWWAL -> "Shawwal"
            DHU_AL_QADAH -> "Dhu al-Qa'dah"
            DHU_AL_HIJJAH -> "Dhu al-Hijjah"
        }
    }

    fun arabicName(): String {
        return when (this) {
            MUHARRAM -> "\u0645\u062d\u0631\u0645"
            SAFAR -> "\u0635\u0641\u0631"
            RABI_AL_AWWAL -> "\u0631\u0628\u064a\u0639 \u0627\u0644\u0623\u0648\u0644"
            RABI_AL_THANI -> "\u0631\u0628\u064a\u0639 \u0627\u0644\u062b\u0627\u0646\u064a"
            JUMADA_AL_AWWAL -> "\u062c\u0645\u0627\u062f\u0649 \u0627\u0644\u0623\u0648\u0644\u0649"
            JUMADA_AL_THANI -> "\u062c\u0645\u0627\u062f\u0649 \u0627\u0644\u062b\u0627\u0646\u064a\u0629"
            RAJAB -> "\u0631\u062c\u0628"
            SHABAN -> "\u0634\u0639\u0628\u0627\u0646"
            RAMADAN -> "\u0631\u0645\u0636\u0627\u0646"
            SHAWWAL -> "\u0634\u0648\u0627\u0644"
            DHU_AL_QADAH -> "\u0630\u0648 \u0627\u0644\u0642\u0639\u062f\u0629"
            DHU_AL_HIJJAH -> "\u0630\u0648 \u0627\u0644\u062d\u062c\u0629"
        }
    }
}

data class IslamicEvent(
    val id: String,
    val nameArabic: String,
    val nameEnglish: String,
    val description: String?,
    val hijriMonth: Int,
    val hijriDay: Int,
    val eventType: IslamicEventType,
    val isHoliday: Boolean,
    val isFastingDay: Boolean,
    val isNightOfPower: Boolean,
    val gregorianDate: LocalDate?,
    val year: Int?,
    val notes: String?,
    val priority: Int
)

enum class IslamicEventType {
    HOLIDAY,
    FAST,
    NIGHT,
    HISTORICAL;

    companion object {
        fun fromString(value: String): IslamicEventType {
            return when (value.lowercase()) {
                "holiday" -> HOLIDAY
                "fast" -> FAST
                "night" -> NIGHT
                "historical" -> HISTORICAL
                else -> HISTORICAL
            }
        }
    }
}

data class CalendarDay(
    val gregorianDate: LocalDate,
    val hijriDate: HijriDate,
    val events: List<IslamicEvent>,
    val isToday: Boolean,
    val isCurrentMonth: Boolean
)

data class CalendarMonth(
    val hijriMonth: Int,
    val hijriYear: Int,
    val days: List<CalendarDay>,
    val events: List<IslamicEvent>
)

// Pre-defined Islamic Events
object IslamicEvents {
    val events = listOf(
        // Muharram
        IslamicEvent("islamic_new_year", "\u0631\u0623\u0633 \u0627\u0644\u0633\u0646\u0629 \u0627\u0644\u0647\u062c\u0631\u064a\u0629", "Islamic New Year", "Beginning of the Islamic calendar year", 1, 1, IslamicEventType.HOLIDAY, true, false, false, null, null, null, 1),
        IslamicEvent("ashura", "\u0639\u0627\u0634\u0648\u0631\u0627\u0621", "Day of Ashura", "Recommended fasting day, commemorates various historical events", 1, 10, IslamicEventType.FAST, false, true, false, null, null, "Recommended to fast on 9th and 10th or 10th and 11th", 1),

        // Rabi' al-Awwal
        IslamicEvent("mawlid", "\u0627\u0644\u0645\u0648\u0644\u062f \u0627\u0644\u0646\u0628\u0648\u064a", "Mawlid an-Nabi", "Birth of Prophet Muhammad (PBUH)", 3, 12, IslamicEventType.HISTORICAL, true, false, false, null, null, null, 1),

        // Rajab
        IslamicEvent("isra_miraj", "\u0627\u0644\u0625\u0633\u0631\u0627\u0621 \u0648\u0627\u0644\u0645\u0639\u0631\u0627\u062c", "Isra and Mi'raj", "Night Journey and Ascension of Prophet Muhammad (PBUH)", 7, 27, IslamicEventType.NIGHT, false, false, false, null, null, null, 1),

        // Sha'ban
        IslamicEvent("shab_e_barat", "\u0644\u064a\u0644\u0629 \u0627\u0644\u0628\u0631\u0627\u0621\u0629", "Shab-e-Barat", "Night of Forgiveness", 8, 15, IslamicEventType.NIGHT, false, false, false, null, null, null, 1),

        // Ramadan
        IslamicEvent("ramadan_start", "\u0628\u062f\u0627\u064a\u0629 \u0631\u0645\u0636\u0627\u0646", "Start of Ramadan", "Beginning of the fasting month", 9, 1, IslamicEventType.FAST, true, true, false, null, null, null, 1),
        IslamicEvent("laylat_al_qadr_21", "\u0644\u064a\u0644\u0629 \u0627\u0644\u0642\u062f\u0631", "Laylat al-Qadr (21)", "Night of Power - possible date", 9, 21, IslamicEventType.NIGHT, false, false, true, null, null, "Search for it in odd nights of last 10 days", 2),
        IslamicEvent("laylat_al_qadr_23", "\u0644\u064a\u0644\u0629 \u0627\u0644\u0642\u062f\u0631", "Laylat al-Qadr (23)", "Night of Power - possible date", 9, 23, IslamicEventType.NIGHT, false, false, true, null, null, "Search for it in odd nights of last 10 days", 2),
        IslamicEvent("laylat_al_qadr_25", "\u0644\u064a\u0644\u0629 \u0627\u0644\u0642\u062f\u0631", "Laylat al-Qadr (25)", "Night of Power - possible date", 9, 25, IslamicEventType.NIGHT, false, false, true, null, null, "Search for it in odd nights of last 10 days", 2),
        IslamicEvent("laylat_al_qadr_27", "\u0644\u064a\u0644\u0629 \u0627\u0644\u0642\u062f\u0631", "Laylat al-Qadr (27)", "Night of Power - most likely date", 9, 27, IslamicEventType.NIGHT, false, false, true, null, null, "Most commonly observed night", 1),
        IslamicEvent("laylat_al_qadr_29", "\u0644\u064a\u0644\u0629 \u0627\u0644\u0642\u062f\u0631", "Laylat al-Qadr (29)", "Night of Power - possible date", 9, 29, IslamicEventType.NIGHT, false, false, true, null, null, "Search for it in odd nights of last 10 days", 2),

        // Shawwal
        IslamicEvent("eid_al_fitr", "\u0639\u064a\u062f \u0627\u0644\u0641\u0637\u0631", "Eid al-Fitr", "Festival of Breaking the Fast", 10, 1, IslamicEventType.HOLIDAY, true, false, false, null, null, "Forbidden to fast on this day", 1),

        // Dhu al-Hijjah
        IslamicEvent("day_of_arafah", "\u064a\u0648\u0645 \u0639\u0631\u0641\u0629", "Day of Arafah", "Standing at Arafah during Hajj - recommended to fast for non-pilgrims", 12, 9, IslamicEventType.FAST, false, true, false, null, null, "Fasting expiates sins of two years", 1),
        IslamicEvent("eid_al_adha", "\u0639\u064a\u062f \u0627\u0644\u0623\u0636\u062d\u0649", "Eid al-Adha", "Festival of Sacrifice", 12, 10, IslamicEventType.HOLIDAY, true, false, false, null, null, "Forbidden to fast on this day and 3 days following", 1)
    )
}
