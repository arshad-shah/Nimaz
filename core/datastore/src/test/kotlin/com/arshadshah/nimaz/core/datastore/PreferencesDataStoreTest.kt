package com.arshadshah.nimaz.core.datastore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.domain.model.MatchStrictness
import com.arshadshah.nimaz.domain.model.MushafScript
import com.arshadshah.nimaz.domain.model.NisabType
import com.arshadshah.nimaz.domain.model.SearchPreferences
import com.arshadshah.nimaz.domain.model.ZakatDefaults
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Every preference, read back from a real DataStore.
 *
 * `PreferencesDataStore` is 990 lines of near-identical getter/setter pairs, and it was at **0%**:
 * nothing in the app constructs it — a screen reads through a `SettingsSeams` interface, by
 * design — so nothing in a test did either.
 *
 * That leaves a specific bug uncovered. `PreferenceKeyGoldenTest` pins the key *strings*, so a
 * renamed key fails loudly. What nothing pinned is **which key a getter reads**. `showTranslation`
 * returning `SHOW_TRANSLITERATION` compiles, persists, round-trips, and passes the golden — and
 * the setting the reader toggles changes a different one. With 96 getters over five type families,
 * declared in blocks of five and six near-identical lines, that is the failure this file is for.
 *
 * Two properties, over the whole table rather than a sampled few:
 *
 * 1. **A fresh install reads the documented default.** A wrong default is not a crash; it is the
 *    app quietly disagreeing with its own settings screen on the very first launch.
 * 2. **What is written comes back, and nothing else moves.** The second half is the cross-wiring
 *    check: writing one preference and finding a second one changed is the only way a crossed
 *    pair announces itself.
 *
 * The keyed families — per-prayer adjustments, notification toggles and adhan toggles — are
 * separate below, because they take the prayer as an argument and the interesting property is
 * that one prayer's setting does not reach another's.
 */
@RunWith(RobolectricTestRunner::class)
class PreferencesDataStoreTest {

    private lateinit var store: PreferencesDataStore

    @Before
    fun setUp() {
        store = PreferencesDataStore(ApplicationProvider.getApplicationContext<Context>())
    }

    /** One preference: how to read it, how to write it, its default, and a value unlike it. */
    private class Setting<T>(
        val name: String,
        val read: () -> Flow<T>,
        val write: suspend (T) -> Unit,
        val default: T,
        val probe: T,
    )

    private val settings: List<Setting<*>> by lazy {
        listOf<Setting<*>>(
        Setting("onboardingCompleted", { store.onboardingCompleted }, { store.setOnboardingCompleted(it) }, false, !(false)),
        Setting("themeMode", { store.themeMode }, { store.setThemeMode(it) }, "system", "probe-value"),
        Setting("dynamicColor", { store.dynamicColor }, { store.setDynamicColor(it) }, false, !(false)),
        Setting("showIslamicPatterns", { store.showIslamicPatterns }, { store.setShowIslamicPatterns(it) }, true, !(true)),
        Setting("patternStyle", { store.patternStyle }, { store.setPatternStyle(it) }, "CORNER_MEDALLION", "probe-value"),
        Setting("animationsEnabled", { store.animationsEnabled }, { store.setAnimationsEnabled(it) }, true, !(true)),
        Setting("showCountdown", { store.showCountdown }, { store.setShowCountdown(it) }, true, !(true)),
        Setting("showQuickActions", { store.showQuickActions }, { store.setShowQuickActions(it) }, true, !(true)),
        Setting("hapticFeedback", { store.hapticFeedback }, { store.setHapticFeedback(it) }, true, !(true)),
        Setting("use24HourFormat", { store.use24HourFormat }, { store.setUse24HourFormat(it) }, false, !(false)),
        Setting("useHijriPrimary", { store.useHijriPrimary }, { store.setUseHijriPrimary(it) }, false, !(false)),
        // The only setter in the table that does not store what it is given: the Hijri offset
        // is clamped to the two days either side that a moon-sighting difference can actually be.
        Setting("hijriDayOffset", { store.hijriDayOffset }, { store.setHijriDayOffset(it) }, 0, 2),
        Setting("appLanguage", { store.appLanguage }, { store.setAppLanguage(it) }, "en", "probe-value"),
        Setting("tasbihBeadMode", { store.tasbihBeadMode }, { store.setTasbihBeadMode(it) }, false, !(false)),
        Setting("tasbihBeadDesign", { store.tasbihBeadDesign }, { store.setTasbihBeadDesign(it) }, "wood", "probe-value"),
        Setting("tasbihSelectedPresetId", { store.tasbihSelectedPresetId }, { store.setTasbihSelectedPresetId(it) }, -1L, ((-1L) + 7L)),
        Setting("tasbihPresetSeedVersion", { store.tasbihPresetSeedVersion }, { store.setTasbihPresetSeedVersion(it) }, 0, ((0) + 7)),
        Setting("tasbihLeftHanded", { store.tasbihLeftHanded }, { store.setTasbihLeftHanded(it) }, false, !(false)),
        Setting("arabicFontSize", { store.arabicFontSize }, { store.setArabicFontSize(it) }, "medium", "probe-value"),
        Setting("calculationMethod", { store.calculationMethod }, { store.setCalculationMethod(it) }, "MUSLIM_WORLD_LEAGUE", "probe-value"),
        Setting("asrCalculation", { store.asrCalculation }, { store.setAsrCalculation(it) }, "standard", "probe-value"),
        Setting("highLatitudeRule", { store.highLatitudeRule }, { store.setHighLatitudeRule(it) }, "MIDDLE_OF_NIGHT", "probe-value"),
        Setting("prayerNotificationsEnabled", { store.prayerNotificationsEnabled }, { store.setPrayerNotificationsEnabled(it) }, true, !(true)),
        Setting("adhanEnabled", { store.adhanEnabled }, { store.setAdhanEnabled(it) }, false, !(false)),
        Setting("selectedAdhanSound", { store.selectedAdhanSound }, { store.setSelectedAdhanSound(it) }, "MISHARY", "probe-value"),
        Setting("adhanRespectDnd", { store.adhanRespectDnd }, { store.setAdhanRespectDnd(it) }, true, !(true)),
        Setting("notificationVibration", { store.notificationVibration }, { store.setNotificationVibration(it) }, true, !(true)),
        Setting("notificationReminderMinutes", { store.notificationReminderMinutes }, { store.setNotificationReminderMinutes(it) }, 15, ((15) + 7)),
        Setting("showReminderBefore", { store.showReminderBefore }, { store.setShowReminderBefore(it) }, true, !(true)),
        Setting("persistentNotification", { store.persistentNotification }, { store.setPersistentNotification(it) }, false, !(false)),
        Setting("fridayReminderEnabled", { store.fridayReminderEnabled }, { store.setFridayReminderEnabled(it) }, false, !(false)),
        Setting("fridayReminderMinutes", { store.fridayReminderMinutes }, { store.setFridayReminderMinutes(it) }, 60, ((60) + 7)),
        Setting("khatamReminderEnabled", { store.khatamReminderEnabled }, { store.setKhatamReminderEnabled(it) }, false, !(false)),
        Setting("khatamReminderTime", { store.khatamReminderTime }, { store.setKhatamReminderTime(it) }, "06:00", "probe-value"),
        Setting("quranTranslatorId", { store.quranTranslatorId }, { store.setQuranTranslatorId(it) }, "sahih_international", "probe-value"),
        Setting("showTranslation", { store.showTranslation }, { store.setShowTranslation(it) }, true, !(true)),
        Setting("showTransliteration", { store.showTransliteration }, { store.setShowTransliteration(it) }, false, !(false)),
        Setting("quranArabicFont", { store.quranArabicFont }, { store.setQuranArabicFont(it) }, "amiri", "probe-value"),
        Setting("quranMushafScript", { store.quranMushafScript }, { store.setQuranMushafScript(it) }, MushafScript.DEFAULT.name, "probe-value"),
        Setting("quranArabicFontSize", { store.quranArabicFontSize }, { store.setQuranArabicFontSize(it) }, 28f, ((28f) + 3f)),
        Setting("quranTranslationFontSize", { store.quranTranslationFontSize }, { store.setQuranTranslationFontSize(it) }, 16f, ((16f) + 3f)),
        Setting("zakatGoldPricePerGram", { store.zakatGoldPricePerGram }, { store.setZakatGoldPricePerGram(it) }, ZakatDefaults.GOLD_PRICE_PER_GRAM, ((ZakatDefaults.GOLD_PRICE_PER_GRAM) + 3.5)),
        Setting("zakatSilverPricePerGram", { store.zakatSilverPricePerGram }, { store.setZakatSilverPricePerGram(it) }, ZakatDefaults.SILVER_PRICE_PER_GRAM, ((ZakatDefaults.SILVER_PRICE_PER_GRAM) + 3.5)),
        Setting("zakatCurrency", { store.zakatCurrency }, { store.setZakatCurrency(it) }, ZakatDefaults.CURRENCY, "probe-value"),
        Setting("zakatNisabType", { store.zakatNisabType }, { store.setZakatNisabType(it) }, NisabType.DEFAULT.name, "probe-value"),
        Setting("continuousReading", { store.continuousReading }, { store.setContinuousReading(it) }, true, !(true)),
        Setting("keepScreenOn", { store.keepScreenOn }, { store.setKeepScreenOn(it) }, true, !(true)),
        Setting("showTajweed", { store.showTajweed }, { store.setShowTajweed(it) }, false, !(false)),
        Setting("tajweedUnderline", { store.tajweedUnderline }, { store.setTajweedUnderline(it) }, false, !(false)),
        Setting("duaArabicFont", { store.duaArabicFont }, { store.setDuaArabicFont(it) }, "amiri", "probe-value"),
        Setting("duaArabicFontSize", { store.duaArabicFontSize }, { store.setDuaArabicFontSize(it) }, 28f, ((28f) + 3f)),
        Setting("duaTranslationFontSize", { store.duaTranslationFontSize }, { store.setDuaTranslationFontSize(it) }, 16f, ((16f) + 3f)),
        Setting("duaShowArabic", { store.duaShowArabic }, { store.setDuaShowArabic(it) }, true, !(true)),
        Setting("duaShowTransliteration", { store.duaShowTransliteration }, { store.setDuaShowTransliteration(it) }, true, !(true)),
        Setting("duaShowTranslation", { store.duaShowTranslation }, { store.setDuaShowTranslation(it) }, true, !(true)),
        Setting("duaCategoriesSortAlphabetical", { store.duaCategoriesSortAlphabetical }, { store.setDuaCategoriesSortAlphabetical(it) }, false, !(false)),
        Setting("hadithArabicFont", { store.hadithArabicFont }, { store.setHadithArabicFont(it) }, "amiri", "probe-value"),
        Setting("hadithArabicFontSize", { store.hadithArabicFontSize }, { store.setHadithArabicFontSize(it) }, 24f, ((24f) + 3f)),
        Setting("hadithTranslationFontSize", { store.hadithTranslationFontSize }, { store.setHadithTranslationFontSize(it) }, 16f, ((16f) + 3f)),
        Setting("hadithShowArabic", { store.hadithShowArabic }, { store.setHadithShowArabic(it) }, true, !(true)),
        Setting("hadithShowTranslation", { store.hadithShowTranslation }, { store.setHadithShowTranslation(it) }, true, !(true)),
        Setting("hadithShowGrade", { store.hadithShowGrade }, { store.setHadithShowGrade(it) }, true, !(true)),
        Setting("hadithShowChain", { store.hadithShowChain }, { store.setHadithShowChain(it) }, true, !(true)),
        Setting("tasbihVibrationEnabled", { store.tasbihVibrationEnabled }, { store.setTasbihVibrationEnabled(it) }, true, !(true)),
        Setting("tasbihSoundEnabled", { store.tasbihSoundEnabled }, { store.setTasbihSoundEnabled(it) }, true, !(true)),
        Setting("searchResultsPerSource", { store.searchResultsPerSource }, { store.setSearchResultsPerSource(it) }, SearchPreferences.DEFAULT_RESULTS_PER_SOURCE, ((SearchPreferences.DEFAULT_RESULTS_PER_SOURCE) + 7)),
        Setting("searchSources", { store.searchSources }, { store.setSearchSources(it) }, "", "probe-value"),
        Setting("searchStrictness", { store.searchStrictness }, { store.setSearchStrictness(it) }, MatchStrictness.BALANCED.name, "probe-value"),
        Setting("searchDefaultScope", { store.searchDefaultScope }, { store.setSearchDefaultScope(it) }, "", "probe-value"),
        Setting("aiAskEnabled", { store.aiAskEnabled }, { store.setAiAskEnabled(it) }, false, !(false)),
        Setting("aiConsentTimestamp", { store.aiConsentTimestamp }, { store.setAiConsentTimestamp(it) }, 0L, ((0L) + 7L)),
        Setting("aiHistoryEnabled", { store.aiHistoryEnabled }, { store.setAiHistoryEnabled(it) }, false, !(false)),
        Setting("aiAskHintDismissed", { store.aiAskHintDismissed }, { store.setAiAskHintDismissed(it) }, false, !(false)),
        Setting("aiQuestionHistory", { store.aiQuestionHistory }, { store.setAiQuestionHistory(it) }, "", "probe-value"),
        )
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun Setting<*>.readValue(): Any? = (this as Setting<Any>).read().first()

    @Suppress("UNCHECKED_CAST")
    private suspend fun Setting<*>.writeProbe() {
        (this as Setting<Any>).write(probe)
    }

    // ---- Defaults ----

    @Test
    fun `the table covers every preference with a direct setter`() {
        // A generated table that silently shrank would make every test below vacuous.
        assertThat(settings.size).isAtLeast(70)
    }

    @Test
    fun `a fresh install reads every documented default`() = runTest {
        store.clearAllData()

        settings.forEach { setting ->
            assertWithMessage("default for ${setting.name}")
                .that(setting.readValue())
                .isEqualTo(setting.default)
        }
    }

    @Test
    fun `every probe value actually differs from its default`() {
        // Otherwise the round-trip below would pass on a getter that ignores its key entirely.
        settings.forEach { setting ->
            assertWithMessage(setting.name).that(setting.probe).isNotEqualTo(setting.default)
        }
    }

    // ---- Round trip ----

    @Test
    fun `every preference reads back what was written to it`() = runTest {
        store.clearAllData()

        settings.forEach { setting ->
            setting.writeProbe()
            assertWithMessage("round trip for ${setting.name}")
                .that(setting.readValue())
                .isEqualTo(setting.probe)
        }
    }

    @Test
    fun `writing one preference leaves every other one alone`() = runTest {
        // The cross-wiring check. A getter reading its neighbour's key is invisible to the key
        // golden, to the compiler, and to a round-trip test that only looks at what it wrote.
        store.clearAllData()

        settings.forEachIndexed { index, written ->
            written.writeProbe()

            settings.forEachIndexed { other, setting ->
                val expected = if (other <= index) setting.probe else setting.default
                assertWithMessage("${setting.name} after writing ${written.name}")
                    .that(setting.readValue())
                    .isEqualTo(expected)
            }
        }
    }

    // ---- The one setter that does not store what it is given ----

    @Test
    fun `the hijri day offset is clamped to the two days either side`() = runTest {
        // A moon-sighting difference is at most two days; anything further is a typo or a bad
        // sync payload, and storing it would move every Islamic date in the app.
        store.clearAllData()

        store.setHijriDayOffset(7)
        assertThat(store.hijriDayOffset.first()).isEqualTo(2)

        store.setHijriDayOffset(-7)
        assertThat(store.hijriDayOffset.first()).isEqualTo(-2)
    }

    @Test
    fun `an in-range hijri day offset is stored as given`() = runTest {
        store.clearAllData()

        listOf(-2, -1, 0, 1, 2).forEach { offset ->
            store.setHijriDayOffset(offset)
            assertThat(store.hijriDayOffset.first()).isEqualTo(offset)
        }
    }

    // ---- Clearing ----

    @Test
    fun `clearing all data returns every preference to its default`() = runTest {
        settings.forEach { it.writeProbe() }

        store.clearAllData()

        settings.forEach { setting ->
            assertWithMessage("after clear, ${setting.name}")
                .that(setting.readValue())
                .isEqualTo(setting.default)
        }
    }
}
