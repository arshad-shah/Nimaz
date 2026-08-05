package com.arshadshah.nimaz.domain.repository.settings

import com.arshadshah.nimaz.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

/**
 * Feature-scoped slices of the app's settings surface.
 *
 * `SettingsRepository` is a flat preference store — 179 members, one `Flow` + one setter
 * per preference. Injecting the whole of it into a ViewModel that reads three fields hands
 * that ViewModel the entire app's configuration, and the compiler stops being able to tell
 * anyone which settings a feature actually depends on.
 *
 * Each interface below is the slice one feature consumes. `SettingsRepository` extends all
 * of them, so the DataStore-backed implementation is unchanged and DI binds every seam to
 * the same singleton; what changes is what a given ViewModel can reach.
 *
 * `SettingsViewModel` is the deliberate exception — it is the settings feature, it edits
 * nearly every preference, and it keeps `SettingsRepository` itself.
 */

/** Reader display preferences for the Qur'an — script, fonts, translation and tajweed. */
interface QuranPreferences {
    val quranTranslatorId: Flow<String>
    suspend fun setQuranTranslatorId(translatorId: String)
    val showTranslation: Flow<Boolean>
    suspend fun setShowTranslation(show: Boolean)
    val showTransliteration: Flow<Boolean>
    suspend fun setShowTransliteration(show: Boolean)
    val selectedReciterId: Flow<String?>
    suspend fun setSelectedReciterId(reciterId: String?)
    val quranArabicFont: Flow<String>
    suspend fun setQuranArabicFont(fontId: String)
    val quranMushafScript: Flow<String>
    suspend fun setQuranMushafScript(script: String)
    val quranArabicFontSize: Flow<Float>
    suspend fun setQuranArabicFontSize(size: Float)
    val quranTranslationFontSize: Flow<Float>
    suspend fun setQuranTranslationFontSize(size: Float)
    val continuousReading: Flow<Boolean>
    suspend fun setContinuousReading(enabled: Boolean)
    val keepScreenOn: Flow<Boolean>
    suspend fun setKeepScreenOn(enabled: Boolean)
    val showTajweed: Flow<Boolean>
    suspend fun setShowTajweed(enabled: Boolean)
    val tajweedUnderline: Flow<Boolean>
    suspend fun setTajweedUnderline(enabled: Boolean)
}

/** Reader display preferences for Hadith. */
interface HadithDisplaySettings {
    val hadithArabicFont: Flow<String>
    suspend fun setHadithArabicFont(fontId: String)
    val hadithArabicFontSize: Flow<Float>
    suspend fun setHadithArabicFontSize(size: Float)
    val hadithTranslationFontSize: Flow<Float>
    suspend fun setHadithTranslationFontSize(size: Float)
    val hadithShowArabic: Flow<Boolean>
    suspend fun setHadithShowArabic(show: Boolean)
    val hadithShowTranslation: Flow<Boolean>
    suspend fun setHadithShowTranslation(show: Boolean)
    val hadithShowGrade: Flow<Boolean>
    suspend fun setHadithShowGrade(show: Boolean)
    val hadithShowChain: Flow<Boolean>
    suspend fun setHadithShowChain(show: Boolean)
}

/** Reader display preferences for Duas, plus the category sort order. */
interface DuaDisplaySettings {
    val duaArabicFont: Flow<String>
    suspend fun setDuaArabicFont(fontId: String)
    val duaArabicFontSize: Flow<Float>
    suspend fun setDuaArabicFontSize(size: Float)
    val duaTranslationFontSize: Flow<Float>
    suspend fun setDuaTranslationFontSize(size: Float)
    val duaShowArabic: Flow<Boolean>
    suspend fun setDuaShowArabic(show: Boolean)
    val duaShowTransliteration: Flow<Boolean>
    suspend fun setDuaShowTransliteration(show: Boolean)
    val duaShowTranslation: Flow<Boolean>
    suspend fun setDuaShowTranslation(show: Boolean)
    val duaCategoriesSortAlphabetical: Flow<Boolean>
    suspend fun setDuaCategoriesSortAlphabetical(enabled: Boolean)
}

/** Tasbih counter preferences — bead presentation, presets and favourites. */
interface TasbihSettings {
    val tasbihBeadMode: Flow<Boolean>
    suspend fun setTasbihBeadMode(enabled: Boolean)
    val tasbihBeadDesign: Flow<String>
    suspend fun setTasbihBeadDesign(key: String)
    val tasbihSelectedPresetId: Flow<Long>
    suspend fun setTasbihSelectedPresetId(id: Long)
    val tasbihPresetSeedVersion: Flow<Int>
    suspend fun setTasbihPresetSeedVersion(version: Int)
    val tasbihFavorites: Flow<Set<String>>
    suspend fun setTasbihFavorites(ids: Set<String>)
    val tasbihLeftHanded: Flow<Boolean>
    suspend fun setTasbihLeftHanded(enabled: Boolean)
    val tasbihVibrationEnabled: Flow<Boolean>
    suspend fun setTasbihVibrationEnabled(enabled: Boolean)
    val tasbihSoundEnabled: Flow<Boolean>
    suspend fun setTasbihSoundEnabled(enabled: Boolean)
}

/** Metal prices and the display currency the zakat calculator values assets in. */
interface ZakatSettings {
    val zakatGoldPricePerGram: Flow<Double>
    suspend fun setZakatGoldPricePerGram(pricePerGram: Double)
    val zakatSilverPricePerGram: Flow<Double>
    suspend fun setZakatSilverPricePerGram(pricePerGram: Double)
    val zakatCurrency: Flow<String>
    suspend fun setZakatCurrency(currency: String)
}

/**
 * Consent and history for the opt-in "Ask with Proof" feature. Kept as its own seam
 * because consent state is the gate on anything leaving the device — see
 * `docs/ai-ask-with-proof.md`.
 */
interface AiSettings {
    val aiAskEnabled: Flow<Boolean>
    suspend fun setAiAskEnabled(enabled: Boolean)
    val aiConsentTimestamp: Flow<Long>
    suspend fun setAiConsentTimestamp(timestamp: Long)
    val aiHistoryEnabled: Flow<Boolean>
    suspend fun setAiHistoryEnabled(enabled: Boolean)
    val aiAskHintDismissed: Flow<Boolean>
    suspend fun setAiAskHintDismissed(dismissed: Boolean)
    val aiQuestionHistory: Flow<String>
    suspend fun setAiQuestionHistory(json: String)
}

/**
 * The user's current place, and the resolved preference bundle that prayer-time and
 * qibla calculation read. Written by onboarding and the location picker; read by
 * anything that needs coordinates.
 */
interface LocationSettings {
    val latitude: Flow<Double>
    val longitude: Flow<Double>
    val locationName: Flow<String>
    suspend fun updateLocation(latitude: Double, longitude: Double, name: String)
    val userPreferences: Flow<UserPreferences>
}

/** First-run state and the app's language, read outside the settings screen. */
interface AppSettings {
    val onboardingCompleted: Flow<Boolean>
    suspend fun setOnboardingCompleted(completed: Boolean)
    val appLanguage: Flow<String>
    suspend fun setAppLanguage(language: String)
}
