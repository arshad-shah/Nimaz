package com.arshadshah.nimaz.domain.model

data class TasbihPreset(
    val id: Long,
    val name: String,
    val arabicText: String?,
    val transliteration: String?,
    val translation: String?,
    val targetCount: Int,
    val category: TasbihCategory?,
    val reference: String?,
    val isDefault: Boolean,
    val displayOrder: Int,
    val createdAt: Long,
    val updatedAt: Long
)

data class TasbihSession(
    val id: Long,
    val presetId: Long?,
    val presetName: String?,
    val date: Long,
    val currentCount: Int,
    val targetCount: Int,
    val totalLaps: Int,
    val isCompleted: Boolean,
    val duration: Long?,
    val startedAt: Long,
    val completedAt: Long?,
    val note: String?
)

enum class TasbihCategory {
    DAILY,
    AFTER_PRAYER,
    MORNING,
    EVENING,
    CUSTOM;

    companion object {
        fun fromString(value: String?): TasbihCategory? {
            return when (value?.lowercase()) {
                "daily" -> DAILY
                "after_prayer", "afterprayer" -> AFTER_PRAYER
                "morning" -> MORNING
                "evening" -> EVENING
                "custom" -> CUSTOM
                else -> null
            }
        }
    }

    fun displayName(): String {
        return when (this) {
            DAILY -> "Daily"
            AFTER_PRAYER -> "After Prayer"
            MORNING -> "Morning"
            EVENING -> "Evening"
            CUSTOM -> "Custom"
        }
    }
}

data class TasbihStats(
    val totalCount: Int,
    val completedSessions: Int,
    val totalDuration: Long,
    val mostUsedPresets: List<PresetUsage>,
    val startDate: Long,
    val endDate: Long
)

data class PresetUsage(
    val presetId: Long,
    val presetName: String,
    val totalCount: Int,
    val sessionsCount: Int
)

// Default Tasbih Presets
object DefaultTasbihPresets {
    val subhanAllah = TasbihPreset(
        id = 1,
        name = "SubhanAllah",
        arabicText = "\u0633\u064f\u0628\u0652\u062d\u064e\u0627\u0646\u064e \u0627\u0644\u0644\u0651\u064e\u0647\u0650",
        transliteration = "SubhanAllah",
        translation = "Glory be to Allah",
        targetCount = 33,
        category = TasbihCategory.AFTER_PRAYER,
        reference = "Sahih Muslim",
        isDefault = true,
        displayOrder = 1,
        createdAt = 0,
        updatedAt = 0
    )

    val alhamdulillah = TasbihPreset(
        id = 2,
        name = "Alhamdulillah",
        arabicText = "\u0627\u0644\u0652\u062d\u064e\u0645\u0652\u062f\u064f \u0644\u0650\u0644\u0651\u064e\u0647\u0650",
        transliteration = "Alhamdulillah",
        translation = "All praise is due to Allah",
        targetCount = 33,
        category = TasbihCategory.AFTER_PRAYER,
        reference = "Sahih Muslim",
        isDefault = true,
        displayOrder = 2,
        createdAt = 0,
        updatedAt = 0
    )

    val allahuAkbar = TasbihPreset(
        id = 3,
        name = "Allahu Akbar",
        arabicText = "\u0627\u0644\u0644\u0651\u064e\u0647\u064f \u0623\u064e\u0643\u0652\u0628\u064e\u0631\u064f",
        transliteration = "Allahu Akbar",
        translation = "Allah is the Greatest",
        targetCount = 34,
        category = TasbihCategory.AFTER_PRAYER,
        reference = "Sahih Muslim",
        isDefault = true,
        displayOrder = 3,
        createdAt = 0,
        updatedAt = 0
    )

    val laIlahaIllallah = TasbihPreset(
        id = 4,
        name = "La ilaha illallah",
        arabicText = "\u0644\u064e\u0627 \u0625\u0650\u0644\u064e\u0647\u064e \u0625\u0650\u0644\u0651\u064e\u0627 \u0627\u0644\u0644\u0651\u064e\u0647\u064f",
        transliteration = "La ilaha illallah",
        translation = "There is no god but Allah",
        targetCount = 100,
        category = TasbihCategory.DAILY,
        reference = "Sahih Bukhari",
        isDefault = true,
        displayOrder = 4,
        createdAt = 0,
        updatedAt = 0
    )

    val astaghfirullah = TasbihPreset(
        id = 5,
        name = "Astaghfirullah",
        arabicText = "\u0623\u064e\u0633\u0652\u062a\u064e\u063a\u0652\u0641\u0650\u0631\u064f \u0627\u0644\u0644\u0651\u064e\u0647\u064e",
        transliteration = "Astaghfirullah",
        translation = "I seek forgiveness from Allah",
        targetCount = 100,
        category = TasbihCategory.DAILY,
        reference = "Sahih Muslim",
        isDefault = true,
        displayOrder = 5,
        createdAt = 0,
        updatedAt = 0
    )

    val allDefaults = listOf(subhanAllah, alhamdulillah, allahuAkbar, laIlahaIllallah, astaghfirullah)
}
