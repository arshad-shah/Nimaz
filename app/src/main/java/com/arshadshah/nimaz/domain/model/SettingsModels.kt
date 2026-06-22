package com.arshadshah.nimaz.domain.model

/** Combined snapshot of the most commonly-read user preferences. */
data class UserPreferences(
    val onboardingCompleted: Boolean,
    val themeMode: String,
    val dynamicColor: Boolean,
    val appLanguage: String,
    val calculationMethod: String,
    val asrCalculation: String,
    val latitude: Double,
    val longitude: Double,
    val locationName: String,
    val prayerNotificationsEnabled: Boolean,
    val quranTranslatorId: String,
    val showTranslation: Boolean
)
