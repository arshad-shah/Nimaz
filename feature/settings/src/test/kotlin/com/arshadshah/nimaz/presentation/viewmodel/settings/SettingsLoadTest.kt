package com.arshadshah.nimaz.presentation.viewmodel.settings

import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.HighLatitudeRule
import com.arshadshah.nimaz.domain.model.MushafScript
import com.arshadshah.nimaz.domain.model.PrayerAlertStyle
import com.arshadshah.nimaz.domain.model.WorshipReminderType
import com.arshadshah.nimaz.presentation.theme.NimazPatternStyle
import com.arshadshah.nimaz.testing.SettingsViewModelHarness
import com.arshadshah.nimaz.testing.testLocation
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * What the settings screens show when they open — the loader, not the writes.
 *
 * `loadSettings()` reads fifty-odd preferences with `.first()` and folds them into six state
 * holders. Nothing in the module has ever run it: the existing `SettingsViewModelTest` builds the
 * ViewModel on a relaxed `SettingsRepository`, whose flows never emit, so `first()` throws on the
 * loader's *first* line and `launchSafely` swallows it. All 152 lines were dead, and the failure
 * mode that hides behind them is the worst kind for a settings screen — every control renders its
 * **compile-time default** rather than what the user chose, and the first toggle they touch writes
 * that default back over their real preference.
 *
 * The parsing arms are the other half. Three of these values are persisted as strings and parsed
 * back, and each parser has a fallback that is indistinguishable from a correct read on screen: a
 * calculation method that will not parse silently becomes Muslim World League, which changes every
 * prayer time in the app. The two telemetry errors are the only evidence that happened, so they
 * are asserted rather than the fallback alone.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsLoadTest {

    private val dispatcher = StandardTestDispatcher()
    private val harness = SettingsViewModelHarness()
    private val repo get() = harness.repo

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun buildViewModel(): SettingsViewModel = harness.build()

    @Test
    fun `every stored general preference reaches the general state`() = runTest {
        repo.themeMode.value = "dark"
        repo.appLanguage.value = "tr"
        repo.animationsEnabled.value = false
        repo.showCountdown.value = false
        repo.showQuickActions.value = false
        repo.hapticFeedback.value = false
        repo.use24HourFormat.value = true
        repo.useHijriPrimary.value = true
        repo.hijriDayOffset.value = -1

        val viewModel = buildViewModel()
        advanceUntilIdle()

        val state = viewModel.generalState.value
        assertThat(state.theme).isEqualTo(AppTheme.DARK)
        assertThat(state.language).isEqualTo(AppLanguage.TURKISH)
        assertThat(state.animationsEnabled).isFalse()
        assertThat(state.showCountdown).isFalse()
        assertThat(state.showQuickActions).isFalse()
        assertThat(state.hapticFeedback).isFalse()
        assertThat(state.use24HourFormat).isTrue()
        assertThat(state.useHijriPrimary).isTrue()
        assertThat(state.hijriDayOffset).isEqualTo(-1)
    }

    @Test
    fun `a stored light theme is not read as system`() = runTest {
        // Three arms over one string, and the `else` catches everything — so a typo'd or renamed
        // "light" reads as SYSTEM and the app opens in the wrong theme with no error anywhere.
        repo.themeMode.value = "light"

        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertThat(viewModel.generalState.value.theme).isEqualTo(AppTheme.LIGHT)
    }

    @Test
    fun `an unrecognised theme falls back to following the system`() = runTest {
        repo.themeMode.value = "midnight"

        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertThat(viewModel.generalState.value.theme).isEqualTo(AppTheme.SYSTEM)
    }

    @Test
    fun `an unrecognised language falls back to English rather than to no language`() = runTest {
        // `AppLanguage.entries.find { it.code == … }` returns null for a code the app has since
        // dropped, and the elvis is the only thing between that and a crash on a settings screen.
        repo.appLanguage.value = "xx"

        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertThat(viewModel.generalState.value.language).isEqualTo(AppLanguage.ENGLISH)
    }

    @Test
    fun `patterns switched off force the ornament style to NONE`() = runTest {
        // Two preferences describe one thing, and the boolean wins. A stored style plus
        // `showIslamicPatterns = false` must not paint the ornament — which is what would happen
        // if the style were read on its own.
        repo.showIslamicPatterns.value = false
        repo.patternStyle.value = NimazPatternStyle.CORNER_MEDALLION.name

        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertThat(viewModel.generalState.value.patternStyle).isEqualTo(NimazPatternStyle.NONE)
        assertThat(viewModel.generalState.value.showIslamicPatterns).isFalse()
    }

    @Test
    fun `patterns switched on keep the stored ornament style`() = runTest {
        repo.showIslamicPatterns.value = true
        repo.patternStyle.value = NimazPatternStyle.entries.last().name

        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertThat(viewModel.generalState.value.patternStyle)
            .isEqualTo(NimazPatternStyle.entries.last())
    }

    @Test
    fun `the six per-prayer adjustments land on their own fields`() = runTest {
        // Six near-identical reads assigned to six near-identical fields is exactly where a
        // copy-paste puts maghrib's offset on asr — and the only visible symptom is one prayer
        // time being minutes out, which reads as a calculation bug rather than a settings bug.
        repo.fajrAdjustment.value = 1
        repo.sunriseAdjustment.value = 2
        repo.dhuhrAdjustment.value = 3
        repo.asrAdjustment.value = 4
        repo.maghribAdjustment.value = 5
        repo.ishaAdjustment.value = 6

        val viewModel = buildViewModel()
        advanceUntilIdle()

        val state = viewModel.prayerState.value
        assertThat(state.fajrAdjustment).isEqualTo(1)
        assertThat(state.sunriseAdjustment).isEqualTo(2)
        assertThat(state.dhuhrAdjustment).isEqualTo(3)
        assertThat(state.asrAdjustment).isEqualTo(4)
        assertThat(state.maghribAdjustment).isEqualTo(5)
        assertThat(state.ishaAdjustment).isEqualTo(6)
    }

    @Test
    fun `a stored calculation method is parsed rather than reset`() = runTest {
        repo.calculationMethod.value = CalculationMethod.EGYPTIAN.name
        repo.asrCalculation.value = "hanafi"
        repo.highLatitudeRule.value = HighLatitudeRule.TWILIGHT_ANGLE.name

        val viewModel = buildViewModel()
        advanceUntilIdle()

        val state = viewModel.prayerState.value
        assertThat(state.calculationMethod).isEqualTo(CalculationMethod.EGYPTIAN)
        assertThat(state.asrMethod).isEqualTo(AsrCalculation.HANAFI)
        assertThat(state.highLatitudeRule).isEqualTo(HighLatitudeRule.TWILIGHT_ANGLE)
    }

    @Test
    fun `an unreadable calculation method falls back to MWL and reports it`() = runTest {
        // The fallback alone is invisible: the user sees "Muslim World League" selected and has
        // no way to tell it is not what they chose. The telemetry error is the only evidence,
        // which is why it is what this asserts.
        repo.calculationMethod.value = "NOT_A_METHOD"

        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertThat(viewModel.prayerState.value.calculationMethod)
            .isEqualTo(CalculationMethod.MUSLIM_WORLD_LEAGUE)
        assertThat(harness.telemetry.errors.map { it.type })
            .contains("unreadable_calculation_method")
    }

    @Test
    fun `an unreadable high latitude rule falls back and reports it`() = runTest {
        repo.highLatitudeRule.value = "NOT_A_RULE"

        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertThat(viewModel.prayerState.value.highLatitudeRule)
            .isEqualTo(HighLatitudeRule.MIDDLE_OF_THE_NIGHT)
        assertThat(harness.telemetry.errors.map { it.type })
            .contains("unreadable_high_latitude_rule")
    }

    @Test
    fun `each prayer's notification flag lands on its own field`() = runTest {
        // Sunrise is the one that matters: `:core:datastore` pins that it defaults off, and a
        // loader that read dhuhr's flag into sunrise's field would show it on.
        repo.fajrNotificationEnabled.value = true
        repo.sunriseNotificationEnabled.value = false
        repo.dhuhrNotificationEnabled.value = false
        repo.asrNotificationEnabled.value = true
        repo.maghribNotificationEnabled.value = false
        repo.ishaNotificationEnabled.value = true

        val viewModel = buildViewModel()
        advanceUntilIdle()

        val state = viewModel.notificationState.value
        assertThat(state.fajrNotification).isTrue()
        assertThat(state.sunriseNotification).isFalse()
        assertThat(state.dhuhrNotification).isFalse()
        assertThat(state.asrNotification).isTrue()
        assertThat(state.maghribNotification).isFalse()
        assertThat(state.ishaNotification).isTrue()
    }

    @Test
    fun `the notification preferences load onto their own fields`() = runTest {
        repo.prayerNotificationsEnabled.value = false
        repo.adhanEnabled.value = true
        repo.notificationVibration.value = false
        repo.notificationReminderMinutes.value = 25
        repo.showReminderBefore.value = false
        repo.persistentNotification.value = true
        repo.fridayReminderEnabled.value = true
        repo.fridayReminderMinutes.value = 90
        repo.khatamReminderEnabled.value = true
        repo.khatamReminderTime.value = "21:45"
        repo.adhanRespectDnd.value = false
        repo.selectedAdhanSound.value = "MAKKAH"

        val viewModel = buildViewModel()
        advanceUntilIdle()

        val state = viewModel.notificationState.value
        assertThat(state.notificationsEnabled).isFalse()
        assertThat(state.adhanEnabled).isTrue()
        assertThat(state.vibrationEnabled).isFalse()
        assertThat(state.reminderMinutes).isEqualTo(25)
        assertThat(state.showReminderBefore).isFalse()
        assertThat(state.persistentNotification).isTrue()
        assertThat(state.fridayReminderEnabled).isTrue()
        assertThat(state.fridayReminderMinutes).isEqualTo(90)
        assertThat(state.khatamReminderEnabled).isTrue()
        assertThat(state.khatamReminderTime).isEqualTo("21:45")
        assertThat(state.respectDnd).isFalse()
        assertThat(state.selectedAdhanSound).isEqualTo("MAKKAH")
    }

    @Test
    fun `the per-prayer alert style map is keyed by the prayer it was read for`() = runTest {
        // Five reads through one keyed accessor. A loader that passed the same key five times
        // would look right — every prayer would simply show fajr's setting.
        repo.alertStyles.getValue("fajr").value = PrayerAlertStyle.ADHAN
        repo.alertStyles.getValue("isha").value = PrayerAlertStyle.SILENT
        repo.reminderEnabled.getValue("maghrib").value = true
        repo.reminderMinutes.getValue("maghrib").value = 40

        val viewModel = buildViewModel()
        advanceUntilIdle()

        val state = viewModel.notificationState.value
        assertThat(state.alertStyles).containsEntry("fajr", PrayerAlertStyle.ADHAN)
        assertThat(state.alertStyles).containsEntry("isha", PrayerAlertStyle.SILENT)
        assertThat(state.alertStyles).containsEntry("dhuhr", PrayerAlertStyle.NOTIFICATION)
        assertThat(state.alertStyles.keys)
            .containsExactlyElementsIn(PrayerAlertStyle.PRAYER_KEYS)
        assertThat(state.reminderEnabled).containsEntry("maghrib", true)
        assertThat(state.reminderEnabled).containsEntry("fajr", false)
        assertThat(state.reminderOffsets).containsEntry("maghrib", 40)
    }

    @Test
    fun `every worship reminder type is loaded, not only the ones a screen lists`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        val state = viewModel.notificationState.value
        assertThat(state.worshipReminders.keys)
            .containsExactlyElementsIn(WorshipReminderType.entries.map { it.key })
        assertThat(state.worshipOffsets.keys)
            .containsExactlyElementsIn(WorshipReminderType.entries.map { it.key })
        // Only Witr has a mode, and reading a mode for the others would be a preference that
        // never gets written.
        assertThat(state.worshipModes.keys).containsExactly(WorshipReminderType.WITR.key)
    }

    @Test
    fun `a worship reminder's offset defaults to its own type's default`() = runTest {
        // `worshipReminderOffset(key, default)` takes the default per type. Passing one type's
        // default for another puts suhoor's lead time on iftar, and the reminder fires at the
        // wrong end of the fast.
        val viewModel = buildViewModel()
        advanceUntilIdle()

        val state = viewModel.notificationState.value
        WorshipReminderType.entries.forEach { type ->
            assertThat(state.worshipOffsets[type.key]).isEqualTo(type.defaultOffsetMinutes)
        }
    }

    @Test
    fun `the dua and hadith display preferences load onto their own states`() = runTest {
        // These two states are the ones both reset paths used to forget, and they read from
        // near-identical preference names — `duaArabicFontSize` against `hadithArabicFontSize`.
        repo.duaArabicFont.value = "scheherazade"
        repo.duaArabicFontSize.value = 33f
        repo.duaTranslationFontSize.value = 19f
        repo.duaShowArabic.value = false
        repo.duaShowTransliteration.value = false
        repo.duaShowTranslation.value = false
        repo.hadithArabicFont.value = "noto"
        repo.hadithArabicFontSize.value = 21f
        repo.hadithTranslationFontSize.value = 14f
        repo.hadithShowArabic.value = false
        repo.hadithShowTranslation.value = false
        repo.hadithShowGrade.value = false
        repo.hadithShowChain.value = false

        val viewModel = buildViewModel()
        advanceUntilIdle()

        val dua = viewModel.duaState.value
        assertThat(dua.selectedArabicFontId).isEqualTo("scheherazade")
        assertThat(dua.arabicFontSize).isEqualTo(33f)
        assertThat(dua.translationFontSize).isEqualTo(19f)
        assertThat(dua.showArabic).isFalse()
        assertThat(dua.showTransliteration).isFalse()
        assertThat(dua.showTranslation).isFalse()

        val hadith = viewModel.hadithState.value
        assertThat(hadith.selectedArabicFontId).isEqualTo("noto")
        assertThat(hadith.arabicFontSize).isEqualTo(21f)
        assertThat(hadith.translationFontSize).isEqualTo(14f)
        assertThat(hadith.showArabic).isFalse()
        assertThat(hadith.showTranslation).isFalse()
        assertThat(hadith.showGrade).isFalse()
        assertThat(hadith.showChain).isFalse()
    }

    @Test
    fun `the Quran state follows DataStore rather than a construction-time snapshot`() = runTest {
        // The bug this exists for: a picker screen runs its own `SettingsViewModel`, writes the
        // new reciter, and the settings screen behind it kept the value it read at construction.
        val viewModel = buildViewModel()
        advanceUntilIdle()
        assertThat(viewModel.quranState.value.selectedReciterId).isNull()

        repo.selectedReciterId.value = "alafasy"
        repo.quranTranslatorId.value = "pickthall"
        repo.quranMushafScript.value = MushafScript.entries.last().name
        repo.showTajweed.value = true
        advanceUntilIdle()

        val state = viewModel.quranState.value
        assertThat(state.selectedReciterId).isEqualTo("alafasy")
        assertThat(state.selectedTranslatorId).isEqualTo("pickthall")
        assertThat(state.mushafScript).isEqualTo(MushafScript.entries.last())
        assertThat(state.showTajweed).isTrue()
    }

    @Test
    fun `the preview card shows the text of the translation that is selected`() = runTest {
        harness.previewTranslationIs("In the name of Allah")

        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertThat(viewModel.quranState.value.previewTranslation).isEqualTo("In the name of Allah")
    }

    @Test
    fun `a failed preview read keeps the previous text and does not end the chain`() = runTest {
        // The `catchAndReport` is *inside* the flatMapLatest deliberately: outside it, one failed
        // read would complete the upstream and freeze the preview on the old translation for the
        // ViewModel's whole life, with every later change silently doing nothing.
        harness.previewTranslationIs("first translation")
        val viewModel = buildViewModel()
        advanceUntilIdle()

        harness.previewTranslationThrows()
        repo.quranTranslatorId.value = "broken"
        advanceUntilIdle()
        assertThat(viewModel.quranState.value.previewTranslation).isEqualTo("first translation")

        harness.previewTranslationIs("third translation")
        repo.quranTranslatorId.value = "recovered"
        advanceUntilIdle()
        assertThat(viewModel.quranState.value.previewTranslation).isEqualTo("third translation")
    }

    @Test
    fun `a null preview read leaves the card showing the previous text`() = runTest {
        harness.previewTranslationIs("kept")
        val viewModel = buildViewModel()
        advanceUntilIdle()

        harness.previewTranslationIs(null)
        repo.quranTranslatorId.value = "missing"
        advanceUntilIdle()

        assertThat(viewModel.quranState.value.previewTranslation).isEqualTo("kept")
    }

    @Test
    fun `the three location flows each land on their own list`() = runTest {
        val london = testLocation(id = 1L, name = "London")
        val cairo = testLocation(id = 2L, name = "Cairo", latitude = 30.0, longitude = 31.2)

        val viewModel = buildViewModel()
        advanceUntilIdle()
        // `isLoading` starts true and is cleared only by the saved-locations collector — a screen
        // that never sees it cleared shows a spinner forever.
        assertThat(viewModel.locationState.value.isLoading).isFalse()

        harness.currentLocation.value = london
        harness.allLocations.value = listOf(london, cairo)
        harness.favoriteLocations.value = listOf(cairo)
        advanceUntilIdle()

        val state = viewModel.locationState.value
        assertThat(state.currentLocation).isEqualTo(london)
        assertThat(state.savedLocations).containsExactly(london, cairo).inOrder()
        assertThat(state.favoriteLocations).containsExactly(cairo)
    }

    @Test
    fun `the notification rollup counts only the prayers that are switched on`() = runTest {
        repo.dhuhrNotificationEnabled.value = false
        repo.ishaNotificationEnabled.value = false
        repo.alertStyles.getValue("fajr").value = PrayerAlertStyle.ADHAN
        repo.reminderEnabled.getValue("fajr").value = true
        repo.reminderMinutes.getValue("fajr").value = 20

        val viewModel = buildViewModel()
        // `WhileSubscribed`, so the combine does not run at all until something collects — and a
        // test reading `.value` without a subscriber sees the initial value and passes whatever
        // the rollup would have computed.
        backgroundScope.launch { viewModel.notificationSummary.collect {} }
        advanceUntilIdle()

        val summary = viewModel.notificationSummary.value
        assertThat(summary.enabledPrayerCount).isEqualTo(3)
        assertThat(summary.fajrAlertStyle).isEqualTo(PrayerAlertStyle.ADHAN)
        assertThat(summary.reminderEnabled).isTrue()
        assertThat(summary.reminderMinutes).isEqualTo(20)
    }

    @Test
    fun `the rollup follows a change made on another screen's ViewModel instance`() = runTest {
        // Each settings destination has its own `SettingsViewModel`; DataStore is the singleton
        // they share. That is the entire reason this rollup collects rather than snapshots.
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.notificationSummary.collect {} }
        advanceUntilIdle()
        assertThat(viewModel.notificationSummary.value.enabledPrayerCount).isEqualTo(5)

        repo.maghribNotificationEnabled.value = false
        advanceUntilIdle()

        assertThat(viewModel.notificationSummary.value.enabledPrayerCount).isEqualTo(4)
    }

    @Test
    fun `the widget previews read the stored preferences, not a second DataStore`() = runTest {
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.widgetPreviewPreferences.collect {} }
        advanceUntilIdle()

        assertThat(viewModel.widgetPreviewPreferences.value?.locationName).isEqualTo("London")
    }
}
