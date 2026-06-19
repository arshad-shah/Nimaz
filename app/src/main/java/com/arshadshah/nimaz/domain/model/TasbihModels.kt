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

    // --- After prayer ---
    val tahlilAfterPrayer = TasbihPreset(
        id = 6,
        name = "La ilaha illallahu wahdah",
        arabicText = "لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ",
        transliteration = "La ilaha illallahu wahdahu la sharika lah, lahul-mulku wa lahul-hamd, wa huwa 'ala kulli shay'in qadir",
        translation = "There is no god but Allah alone, with no partner. His is the dominion and His is the praise, and He is over all things competent.",
        targetCount = 10,
        category = TasbihCategory.AFTER_PRAYER,
        reference = "Sahih Muslim",
        isDefault = true,
        displayOrder = 6,
        createdAt = 0,
        updatedAt = 0
    )

    val subhanAllahWaBihamdihi = TasbihPreset(
        id = 7,
        name = "SubhanAllahi wa bihamdih",
        arabicText = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ",
        transliteration = "SubhanAllahi wa bihamdihi",
        translation = "Glory be to Allah and praise be to Him",
        targetCount = 100,
        category = TasbihCategory.AFTER_PRAYER,
        reference = "Sahih al-Bukhari & Muslim",
        isDefault = true,
        displayOrder = 7,
        createdAt = 0,
        updatedAt = 0
    )

    // --- Morning ---
    val asbahna = TasbihPreset(
        id = 8,
        name = "Asbahna wa asbahal-mulku lillah",
        arabicText = "أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ",
        transliteration = "Asbahna wa asbahal-mulku lillah, wal-hamdu lillah",
        translation = "We have entered the morning and the dominion belongs to Allah, and all praise is for Allah.",
        targetCount = 1,
        category = TasbihCategory.MORNING,
        reference = "Sahih Muslim",
        isDefault = true,
        displayOrder = 8,
        createdAt = 0,
        updatedAt = 0
    )

    val bismillahLaYadurr = TasbihPreset(
        id = 9,
        name = "Bismillahilladhi la yadurr",
        arabicText = "بِسْمِ اللَّهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الْأَرْضِ وَلَا فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ",
        transliteration = "Bismillahilladhi la yadurru ma'asmihi shay'un fil-ardi wa la fis-sama', wa huwas-Sami'ul-'Alim",
        translation = "In the name of Allah, with whose name nothing on earth or in heaven can cause harm, and He is the All-Hearing, the All-Knowing.",
        targetCount = 3,
        category = TasbihCategory.MORNING,
        reference = "Abu Dawud & at-Tirmidhi",
        isDefault = true,
        displayOrder = 9,
        createdAt = 0,
        updatedAt = 0
    )

    val radeetuBillah = TasbihPreset(
        id = 10,
        name = "Radeetu billahi Rabba",
        arabicText = "رَضِيتُ بِاللَّهِ رَبًّا، وَبِالْإِسْلَامِ دِينًا، وَبِمُحَمَّدٍ صَلَّى اللَّهُ عَلَيْهِ وَسَلَّمَ نَبِيًّا",
        transliteration = "Radeetu billahi Rabban, wa bil-Islami dinan, wa bi-Muhammadin nabiyya",
        translation = "I am pleased with Allah as my Lord, Islam as my religion, and Muhammad as my Prophet.",
        targetCount = 3,
        category = TasbihCategory.MORNING,
        reference = "Abu Dawud & at-Tirmidhi",
        isDefault = true,
        displayOrder = 10,
        createdAt = 0,
        updatedAt = 0
    )

    // --- Evening ---
    val amsayna = TasbihPreset(
        id = 11,
        name = "Amsayna wa amsal-mulku lillah",
        arabicText = "أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ",
        transliteration = "Amsayna wa amsal-mulku lillah, wal-hamdu lillah",
        translation = "We have entered the evening and the dominion belongs to Allah, and all praise is for Allah.",
        targetCount = 1,
        category = TasbihCategory.EVENING,
        reference = "Sahih Muslim",
        isDefault = true,
        displayOrder = 11,
        createdAt = 0,
        updatedAt = 0
    )

    val audhuBiKalimatillah = TasbihPreset(
        id = 12,
        name = "A'udhu bikalimatillahit-tammat",
        arabicText = "أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ",
        transliteration = "A'udhu bikalimatillahit-tammati min sharri ma khalaq",
        translation = "I seek refuge in the perfect words of Allah from the evil of what He created.",
        targetCount = 3,
        category = TasbihCategory.EVENING,
        reference = "Sahih Muslim",
        isDefault = true,
        displayOrder = 12,
        createdAt = 0,
        updatedAt = 0
    )

    val allahummaBikaAmsayna = TasbihPreset(
        id = 13,
        name = "Allahumma bika amsayna",
        arabicText = "اللَّهُمَّ بِكَ أَمْسَيْنَا وَبِكَ أَصْبَحْنَا وَبِكَ نَحْيَا وَبِكَ نَمُوتُ وَإِلَيْكَ الْمَصِيرُ",
        transliteration = "Allahumma bika amsayna, wa bika asbahna, wa bika nahya, wa bika namut, wa ilaykal-masir",
        translation = "O Allah, by You we enter the evening and the morning, by You we live and die, and to You is the return.",
        targetCount = 1,
        category = TasbihCategory.EVENING,
        reference = "at-Tirmidhi",
        isDefault = true,
        displayOrder = 13,
        createdAt = 0,
        updatedAt = 0
    )

    /** The original five, baked into the prepackaged DB. */
    val baseDefaults = listOf(subhanAllah, alhamdulillah, allahuAkbar, laIlahaIllallah, astaghfirullah)

    /** Adhkar added after the prepackaged DB shipped; seeded at runtime by name. */
    val addedDefaults = listOf(
        tahlilAfterPrayer, subhanAllahWaBihamdihi,
        asbahna, bismillahLaYadurr, radeetuBillah,
        amsayna, audhuBiKalimatillah, allahummaBikaAmsayna
    )

    val allDefaults = baseDefaults + addedDefaults
}
