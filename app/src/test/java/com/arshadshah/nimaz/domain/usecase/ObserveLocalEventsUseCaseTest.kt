package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.CelebrationEvent
import com.arshadshah.nimaz.domain.model.UserPreferences
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ObserveLocalEventsUseCaseTest {

    // Minimal fake exposing only hijriDayOffset; other members are unused here.
    private fun repo(offset: Int): SettingsRepository = FakeSettings(offset)

    @Test
    fun `emits eid card on 1 Shawwal`() = runBlocking {
        // 1 Shawwal — use HijriDateCalculator to find a Gregorian date for it.
        val eidGregorian = com.arshadshah.nimaz.core.util.HijriDateCalculator
            .toGregorian(1, 10, com.arshadshah.nimaz.core.util.HijriDateCalculator.today().year + 1)
        val useCase = ObserveLocalEventsUseCase(repo(0), nowDate = { eidGregorian })
        val cards = useCase().first()
        assertThat(cards.map { it.event }).contains(CelebrationEvent.EID_AL_FITR)
    }

    @Test
    fun `emits empty list on an ordinary day`() = runBlocking {
        // 5th of month 2 (Safar) — no event in IslamicEvents.events
        val plainDay = com.arshadshah.nimaz.core.util.HijriDateCalculator.toGregorian(5, 2, 1448)
        val useCase = ObserveLocalEventsUseCase(repo(0), nowDate = { plainDay })
        assertThat(useCase().first()).isEmpty()
    }
}

/**
 * Full [SettingsRepository] stub — only [hijriDayOffset] is meaningful to
 * [ObserveLocalEventsUseCase]; every other member is a harmless default that
 * this use case never touches.
 */
private class FakeSettings(private val offset: Int) : SettingsRepository {
    override suspend fun clearAllData() {}
    override val onboardingCompleted: Flow<Boolean> = flowOf(true)
    override suspend fun setOnboardingCompleted(completed: Boolean) {}
    override val themeMode: Flow<String> = flowOf("SYSTEM")
    override suspend fun setThemeMode(mode: String) {}
    override val dynamicColor: Flow<Boolean> = flowOf(false)
    override suspend fun setDynamicColor(enabled: Boolean) {}
    override val showIslamicPatterns: Flow<Boolean> = flowOf(true)
    override suspend fun setShowIslamicPatterns(enabled: Boolean) {}
    override val patternStyle: Flow<String> = flowOf("NONE")
    override suspend fun setPatternStyle(style: String) {}
    override val animationsEnabled: Flow<Boolean> = flowOf(true)
    override suspend fun setAnimationsEnabled(enabled: Boolean) {}
    override val showCountdown: Flow<Boolean> = flowOf(true)
    override suspend fun setShowCountdown(enabled: Boolean) {}
    override val showQuickActions: Flow<Boolean> = flowOf(true)
    override suspend fun setShowQuickActions(enabled: Boolean) {}
    override val hapticFeedback: Flow<Boolean> = flowOf(true)
    override suspend fun setHapticFeedback(enabled: Boolean) {}
    override val use24HourFormat: Flow<Boolean> = flowOf(false)
    override suspend fun setUse24HourFormat(enabled: Boolean) {}
    override val useHijriPrimary: Flow<Boolean> = flowOf(false)
    override suspend fun setUseHijriPrimary(enabled: Boolean) {}
    override val hijriDayOffset: Flow<Int> = flowOf(offset)
    override suspend fun setHijriDayOffset(days: Int) {}
    override val appLanguage: Flow<String> = flowOf("en")
    override suspend fun setAppLanguage(language: String) {}
    override val helpContentVersion: Flow<Int> = flowOf(0)
    override suspend fun setHelpContentVersion(version: Int) {}
    override val hadithBackfillVersion: Flow<Int> = flowOf(0)
    override suspend fun setHadithBackfillVersion(version: Int) {}
    override val tasbihBeadMode: Flow<Boolean> = flowOf(false)
    override suspend fun setTasbihBeadMode(enabled: Boolean) {}
    override val tasbihBeadDesign: Flow<String> = flowOf("default")
    override suspend fun setTasbihBeadDesign(key: String) {}
    override val tasbihSelectedPresetId: Flow<Long> = flowOf(0L)
    override suspend fun setTasbihSelectedPresetId(id: Long) {}
    override val tasbihPresetSeedVersion: Flow<Int> = flowOf(0)
    override suspend fun setTasbihPresetSeedVersion(version: Int) {}
    override val tasbihFavorites: Flow<Set<String>> = flowOf(emptySet())
    override suspend fun setTasbihFavorites(ids: Set<String>) {}
    override val tasbihLeftHanded: Flow<Boolean> = flowOf(false)
    override suspend fun setTasbihLeftHanded(enabled: Boolean) {}
    override val duaContentVersion: Flow<Int> = flowOf(0)
    override suspend fun setDuaContentVersion(version: Int) {}
    override val qaidaContentVersion: Flow<Int> = flowOf(0)
    override suspend fun setQaidaContentVersion(version: Int) {}
    override val indopakContentVersion: Flow<Int> = flowOf(0)
    override suspend fun setIndopakContentVersion(version: Int) {}
    override val arabicFontSize: Flow<String> = flowOf("MEDIUM")
    override suspend fun setArabicFontSize(size: String) {}
    override val calculationMethod: Flow<String> = flowOf("MWL")
    override suspend fun setCalculationMethod(method: String) {}
    override val asrCalculation: Flow<String> = flowOf("STANDARD")
    override suspend fun setAsrCalculation(calculation: String) {}
    override val highLatitudeRule: Flow<String> = flowOf("MIDDLE_OF_THE_NIGHT")
    override suspend fun setHighLatitudeRule(rule: String) {}
    override val currentLocationId: Flow<Long?> = flowOf(null)
    override suspend fun setCurrentLocationId(id: Long) {}
    override val fajrAdjustment: Flow<Int> = flowOf(0)
    override val sunriseAdjustment: Flow<Int> = flowOf(0)
    override val dhuhrAdjustment: Flow<Int> = flowOf(0)
    override val asrAdjustment: Flow<Int> = flowOf(0)
    override val maghribAdjustment: Flow<Int> = flowOf(0)
    override val ishaAdjustment: Flow<Int> = flowOf(0)
    override suspend fun setPrayerAdjustment(prayer: String, minutes: Int) {}
    override val prayerNotificationsEnabled: Flow<Boolean> = flowOf(true)
    override suspend fun setPrayerNotificationsEnabled(enabled: Boolean) {}
    override val adhanEnabled: Flow<Boolean> = flowOf(true)
    override suspend fun setAdhanEnabled(enabled: Boolean) {}
    override val selectedAdhanSound: Flow<String> = flowOf("default")
    override suspend fun setSelectedAdhanSound(sound: String) {}
    override val fajrNotificationEnabled: Flow<Boolean> = flowOf(true)
    override val sunriseNotificationEnabled: Flow<Boolean> = flowOf(true)
    override val dhuhrNotificationEnabled: Flow<Boolean> = flowOf(true)
    override val asrNotificationEnabled: Flow<Boolean> = flowOf(true)
    override val maghribNotificationEnabled: Flow<Boolean> = flowOf(true)
    override val ishaNotificationEnabled: Flow<Boolean> = flowOf(true)
    override suspend fun setPrayerNotificationEnabled(prayer: String, enabled: Boolean) {}
    override val fajrAdhanEnabled: Flow<Boolean> = flowOf(true)
    override val dhuhrAdhanEnabled: Flow<Boolean> = flowOf(true)
    override val asrAdhanEnabled: Flow<Boolean> = flowOf(true)
    override val maghribAdhanEnabled: Flow<Boolean> = flowOf(true)
    override val ishaAdhanEnabled: Flow<Boolean> = flowOf(true)
    override suspend fun setPrayerAdhanEnabled(prayer: String, enabled: Boolean) {}
    override fun isAdhanEnabledForPrayer(prayer: String): Flow<Boolean> = flowOf(true)
    override val adhanRespectDnd: Flow<Boolean> = flowOf(true)
    override suspend fun setAdhanRespectDnd(enabled: Boolean) {}
    override val notificationVibration: Flow<Boolean> = flowOf(true)
    override suspend fun setNotificationVibration(enabled: Boolean) {}
    override val notificationReminderMinutes: Flow<Int> = flowOf(10)
    override suspend fun setNotificationReminderMinutes(minutes: Int) {}
    override val showReminderBefore: Flow<Boolean> = flowOf(true)
    override suspend fun setShowReminderBefore(enabled: Boolean) {}
    override val persistentNotification: Flow<Boolean> = flowOf(false)
    override suspend fun setPersistentNotification(enabled: Boolean) {}
    override val fridayReminderEnabled: Flow<Boolean> = flowOf(false)
    override suspend fun setFridayReminderEnabled(enabled: Boolean) {}
    override val fridayReminderMinutes: Flow<Int> = flowOf(0)
    override suspend fun setFridayReminderMinutes(minutes: Int) {}
    override val khatamReminderEnabled: Flow<Boolean> = flowOf(false)
    override suspend fun setKhatamReminderEnabled(enabled: Boolean) {}
    override val khatamReminderTime: Flow<String> = flowOf("00:00")
    override suspend fun setKhatamReminderTime(time: String) {}
    override val quranTranslatorId: Flow<String> = flowOf("en.sahih")
    override suspend fun setQuranTranslatorId(translatorId: String) {}
    override val showTranslation: Flow<Boolean> = flowOf(true)
    override suspend fun setShowTranslation(show: Boolean) {}
    override val showTransliteration: Flow<Boolean> = flowOf(true)
    override suspend fun setShowTransliteration(show: Boolean) {}
    override val selectedReciterId: Flow<String?> = flowOf(null)
    override suspend fun setSelectedReciterId(reciterId: String?) {}
    override val quranArabicFont: Flow<String> = flowOf("default")
    override suspend fun setQuranArabicFont(fontId: String) {}
    override val quranMushafScript: Flow<String> = flowOf("MADANI")
    override suspend fun setQuranMushafScript(script: String) {}
    override val quranArabicFontSize: Flow<Float> = flowOf(24f)
    override suspend fun setQuranArabicFontSize(size: Float) {}
    override val quranTranslationFontSize: Flow<Float> = flowOf(16f)
    override suspend fun setQuranTranslationFontSize(size: Float) {}
    override val continuousReading: Flow<Boolean> = flowOf(false)
    override suspend fun setContinuousReading(enabled: Boolean) {}
    override val keepScreenOn: Flow<Boolean> = flowOf(false)
    override suspend fun setKeepScreenOn(enabled: Boolean) {}
    override val showTajweed: Flow<Boolean> = flowOf(false)
    override suspend fun setShowTajweed(enabled: Boolean) {}
    override val tajweedUnderline: Flow<Boolean> = flowOf(false)
    override suspend fun setTajweedUnderline(enabled: Boolean) {}
    override val duaArabicFont: Flow<String> = flowOf("default")
    override suspend fun setDuaArabicFont(fontId: String) {}
    override val duaArabicFontSize: Flow<Float> = flowOf(24f)
    override suspend fun setDuaArabicFontSize(size: Float) {}
    override val duaTranslationFontSize: Flow<Float> = flowOf(16f)
    override suspend fun setDuaTranslationFontSize(size: Float) {}
    override val duaShowArabic: Flow<Boolean> = flowOf(true)
    override suspend fun setDuaShowArabic(show: Boolean) {}
    override val duaShowTransliteration: Flow<Boolean> = flowOf(true)
    override suspend fun setDuaShowTransliteration(show: Boolean) {}
    override val duaShowTranslation: Flow<Boolean> = flowOf(true)
    override suspend fun setDuaShowTranslation(show: Boolean) {}
    override val duaCategoriesSortAlphabetical: Flow<Boolean> = flowOf(false)
    override suspend fun setDuaCategoriesSortAlphabetical(enabled: Boolean) {}
    override val hadithArabicFont: Flow<String> = flowOf("default")
    override suspend fun setHadithArabicFont(fontId: String) {}
    override val hadithArabicFontSize: Flow<Float> = flowOf(24f)
    override suspend fun setHadithArabicFontSize(size: Float) {}
    override val hadithTranslationFontSize: Flow<Float> = flowOf(16f)
    override suspend fun setHadithTranslationFontSize(size: Float) {}
    override val hadithShowArabic: Flow<Boolean> = flowOf(true)
    override suspend fun setHadithShowArabic(show: Boolean) {}
    override val hadithShowTranslation: Flow<Boolean> = flowOf(true)
    override suspend fun setHadithShowTranslation(show: Boolean) {}
    override val hadithShowGrade: Flow<Boolean> = flowOf(true)
    override suspend fun setHadithShowGrade(show: Boolean) {}
    override val hadithShowChain: Flow<Boolean> = flowOf(true)
    override suspend fun setHadithShowChain(show: Boolean) {}
    override val tasbihVibrationEnabled: Flow<Boolean> = flowOf(true)
    override suspend fun setTasbihVibrationEnabled(enabled: Boolean) {}
    override val tasbihSoundEnabled: Flow<Boolean> = flowOf(false)
    override suspend fun setTasbihSoundEnabled(enabled: Boolean) {}
    override val latitude: Flow<Double> = flowOf(0.0)
    override val longitude: Flow<Double> = flowOf(0.0)
    override val locationName: Flow<String> = flowOf("")
    override suspend fun updateLocation(latitude: Double, longitude: Double, name: String) {}
    override val aiAskEnabled: Flow<Boolean> = flowOf(false)
    override suspend fun setAiAskEnabled(enabled: Boolean) {}
    override val aiConsentTimestamp: Flow<Long> = flowOf(0L)
    override suspend fun setAiConsentTimestamp(timestamp: Long) {}
    override val aiHistoryEnabled: Flow<Boolean> = flowOf(false)
    override suspend fun setAiHistoryEnabled(enabled: Boolean) {}
    override val aiAskHintDismissed: Flow<Boolean> = flowOf(false)
    override suspend fun setAiAskHintDismissed(dismissed: Boolean) {}
    override val aiQuestionHistory: Flow<String> = flowOf("[]")
    override suspend fun setAiQuestionHistory(json: String) {}
    override suspend fun exportAllPreferences(): Map<String, String> = emptyMap()
    override suspend fun importPreferences(prefsMap: Map<String, String>) {}
    override val userPreferences: Flow<UserPreferences> = flowOf(
        UserPreferences(
            onboardingCompleted = true,
            themeMode = "SYSTEM",
            dynamicColor = false,
            appLanguage = "en",
            calculationMethod = "MWL",
            asrCalculation = "STANDARD",
            latitude = 0.0,
            longitude = 0.0,
            locationName = "",
            prayerNotificationsEnabled = true,
            quranTranslatorId = "en.sahih",
            showTranslation = true,
        )
    )
}
