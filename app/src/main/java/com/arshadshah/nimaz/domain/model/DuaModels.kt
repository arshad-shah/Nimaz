package com.arshadshah.nimaz.domain.model

data class DuaCategory(
    val id: String,
    val nameArabic: String,
    val nameEnglish: String,
    val description: String?,
    val iconName: String?,
    val displayOrder: Int,
    val duaCount: Int
)

data class Dua(
    val id: String,
    val categoryId: String,
    val titleArabic: String,
    val titleEnglish: String,
    val textArabic: String,
    val textTransliteration: String?,
    val textEnglish: String,
    val reference: String?,
    val occasion: DuaOccasion?,
    val benefits: String?,
    val repeatCount: Int?,
    val audioUrl: String?,
    val displayOrder: Int,
    val isFavorite: Boolean = false,
    val isBookmarked: Boolean = false
)

data class DuaBookmark(
    val id: Long,
    val duaId: String,
    val categoryId: String,
    val note: String?,
    val isFavorite: Boolean,
    val duaTitle: String? = null,
    val categoryName: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

data class DuaProgress(
    val id: Long,
    val duaId: String,
    val date: Long,
    val completedCount: Int,
    val targetCount: Int,
    val isCompleted: Boolean,
    val createdAt: Long
)

enum class DuaOccasion {
    MORNING,
    EVENING,
    AFTER_PRAYER,
    BEFORE_SLEEP,
    WAKING_UP,
    EATING,
    TRAVELING,
    ENTERING_MOSQUE,
    LEAVING_MOSQUE,
    ENTERING_HOME,
    LEAVING_HOME,
    RAIN,
    DISTRESS,
    FORGIVENESS,
    PARENTS,
    GRATITUDE,
    GENERAL;

    companion object {
        fun fromString(value: String?): DuaOccasion? {
            return when (value?.lowercase()) {
                "morning" -> MORNING
                "evening" -> EVENING
                "after_prayer", "afterprayer" -> AFTER_PRAYER
                "before_sleep", "beforesleep" -> BEFORE_SLEEP
                "waking_up", "wakingup" -> WAKING_UP
                "eating" -> EATING
                "traveling" -> TRAVELING
                "entering_mosque" -> ENTERING_MOSQUE
                "leaving_mosque" -> LEAVING_MOSQUE
                "entering_home" -> ENTERING_HOME
                "leaving_home" -> LEAVING_HOME
                "rain" -> RAIN
                "distress" -> DISTRESS
                "forgiveness" -> FORGIVENESS
                "parents" -> PARENTS
                "gratitude" -> GRATITUDE
                "general" -> GENERAL
                else -> null
            }
        }
    }

    fun displayName(): String {
        return when (this) {
            MORNING -> "Morning"
            EVENING -> "Evening"
            AFTER_PRAYER -> "After Prayer"
            BEFORE_SLEEP -> "Before Sleep"
            WAKING_UP -> "Waking Up"
            EATING -> "Eating"
            TRAVELING -> "Traveling"
            ENTERING_MOSQUE -> "Entering Mosque"
            LEAVING_MOSQUE -> "Leaving Mosque"
            ENTERING_HOME -> "Entering Home"
            LEAVING_HOME -> "Leaving Home"
            RAIN -> "Rain"
            DISTRESS -> "Distress"
            FORGIVENESS -> "Forgiveness"
            PARENTS -> "Parents"
            GRATITUDE -> "Gratitude"
            GENERAL -> "General"
        }
    }
}

data class DuaSearchResult(
    val dua: Dua,
    val categoryName: String,
    val matchedText: String
)

data class DuaCategoryWithDuas(
    val category: DuaCategory,
    val duas: List<Dua>
)

data class DailyDuaProgress(
    val date: Long,
    val duasCompleted: Int,
    val totalDuas: Int,
    val progressItems: List<DuaProgress>
)
