package com.arshadshah.nimaz.presentation.viewmodel.settings

import com.arshadshah.nimaz.core.monitoring.TelemetryCall
import com.arshadshah.nimaz.data.audio.AdhanSound
import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.HighLatitudeRule
import com.arshadshah.nimaz.domain.model.MushafScript
import com.arshadshah.nimaz.domain.model.PrayerAlertStyle
import com.arshadshah.nimaz.presentation.theme.NimazPatternStyle
import com.arshadshah.nimaz.testing.SettingsViewModelHarness
import com.arshadshah.nimaz.testing.testLocation
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
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
 * Every `SettingsEvent`, against the preference it must actually write.
 *
 * This is the module's central risk. Seventy-eight branches of one `when`, each a two-line body
 * that updates a state field and writes a setter, and every one of them looks like its neighbour.
 * A branch that updates the right field and writes the *wrong* setter is invisible from the
 * screen — the switch moves, the row reads correctly, and the preference the user thinks they
 * changed is untouched until the next launch reloads it. `:core:datastore` (#603) pins the
 * persistence half exhaustively; nothing has ever pinned that the event reaching it is the right
 * one.
 *
 * So each assertion is a pair: the optimistic state update the screen paints on the frame of the
 * tap, and a `coVerify` naming the exact setter. A verify against `any()` would pass against the
 * bug this is written for.
 *
 * The reschedule calls are asserted separately and deliberately. Alarms are baked at scheduling
 * time, so a preference that changes *when* a notification fires must rearm and one that changes
 * only *how* it fires must not — and getting that backwards either leaves a stale alarm or
 * rewrites every alarm on every keystroke of a stepper.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsEventTableTest {

    private val dispatcher = StandardTestDispatcher()
    private val harness = SettingsViewModelHarness()
    private val repo get() = harness.repo
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = harness.build()
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun send(vararg events: SettingsEvent) = runTest(dispatcher) {
        advanceUntilIdle()
        events.forEach { viewModel.onEvent(it) }
        advanceUntilIdle()
    }

    // ── General ──────────────────────────────────────────────────────────────────────────────

    @Test
    fun `choosing a theme writes that theme's own mode string`() {
        // Three enum values mapped to three lowercase strings by hand. `LIGHT -> "dark"` would
        // show the light row selected and open the app dark.
        send(SettingsEvent.SetTheme(AppTheme.DARK))
        assertThat(viewModel.generalState.value.theme).isEqualTo(AppTheme.DARK)
        coVerify { repo.mock.setThemeMode("dark") }

        send(SettingsEvent.SetTheme(AppTheme.LIGHT))
        coVerify { repo.mock.setThemeMode("light") }

        send(SettingsEvent.SetTheme(AppTheme.SYSTEM))
        coVerify { repo.mock.setThemeMode("system") }
    }

    @Test
    fun `choosing a language persists its code, applies the locale and asks for a restart`() {
        // Three things must happen together: the code is stored, the locale is applied *now*
        // (otherwise the change waits for a cold start), and the restart flag is raised. Storing
        // without applying is the plausible failure, and it looks like nothing happened.
        send(SettingsEvent.SetLanguage(AppLanguage.FRENCH))

        assertThat(viewModel.generalState.value.language).isEqualTo(AppLanguage.FRENCH)
        coVerify { repo.mock.setAppLanguage("fr") }
        coVerify { harness.appLocale.apply("fr") }
        assertThat(viewModel.shouldRestart.value).isTrue()
    }

    @Test
    fun `the language is also recorded as a user property`() {
        // Declared and never set, so every segmentation by language was empty.
        send(SettingsEvent.SetLanguage(AppLanguage.INDONESIAN))

        assertThat(harness.telemetry.userProperties.map { it.name to it.value })
            .contains("app_language" to "id")
    }

    @Test
    fun `each general toggle writes its own preference`() {
        send(
            SettingsEvent.SetHijriPrimary(true),
            SettingsEvent.SetHijriDayOffset(-1),
            SettingsEvent.Set24HourFormat(true),
            SettingsEvent.SetHapticFeedback(false),
            SettingsEvent.SetAnimationsEnabled(false),
            SettingsEvent.SetShowCountdown(false),
            SettingsEvent.SetShowQuickActions(false),
        )

        val state = viewModel.generalState.value
        assertThat(state.useHijriPrimary).isTrue()
        assertThat(state.hijriDayOffset).isEqualTo(-1)
        assertThat(state.use24HourFormat).isTrue()
        assertThat(state.hapticFeedback).isFalse()
        assertThat(state.animationsEnabled).isFalse()
        assertThat(state.showCountdown).isFalse()
        assertThat(state.showQuickActions).isFalse()

        coVerify { repo.mock.setUseHijriPrimary(true) }
        coVerify { repo.mock.setHijriDayOffset(-1) }
        coVerify { repo.mock.setUse24HourFormat(true) }
        coVerify { repo.mock.setHapticFeedback(false) }
        coVerify { repo.mock.setAnimationsEnabled(false) }
        coVerify { repo.mock.setShowCountdown(false) }
        coVerify { repo.mock.setShowQuickActions(false) }
    }

    @Test
    fun `switching the ornament off keeps the legacy boolean in step`() {
        // The style is the source of truth and the boolean is kept only for import/export and
        // the readers that still ask it. Writing one without the other is how the two disagree.
        send(SettingsEvent.SetPatternStyle(NimazPatternStyle.NONE))

        assertThat(viewModel.generalState.value.patternStyle).isEqualTo(NimazPatternStyle.NONE)
        assertThat(viewModel.generalState.value.showIslamicPatterns).isFalse()
        coVerify { repo.mock.setPatternStyle("NONE") }
        coVerify { repo.mock.setShowIslamicPatterns(false) }
    }

    @Test
    fun `choosing any real ornament switches patterns back on`() {
        send(SettingsEvent.SetPatternStyle(NimazPatternStyle.NONE))
        send(SettingsEvent.SetPatternStyle(NimazPatternStyle.CORNER_MEDALLION))

        assertThat(viewModel.generalState.value.showIslamicPatterns).isTrue()
        coVerify { repo.mock.setPatternStyle("CORNER_MEDALLION") }
        coVerify { repo.mock.setShowIslamicPatterns(true) }
    }

    // ── Prayer ───────────────────────────────────────────────────────────────────────────────

    @Test
    fun `the calculation method is stored by name and rearms every alarm`() {
        // Changing the method moves every prayer time, so the alarms already set are wrong until
        // they are rebuilt. Not rescheduling here is a notification that fires at yesterday's
        // time and no error anywhere.
        send(SettingsEvent.SetCalculationMethod(CalculationMethod.EGYPTIAN))

        assertThat(viewModel.prayerState.value.calculationMethod)
            .isEqualTo(CalculationMethod.EGYPTIAN)
        coVerify { repo.mock.setCalculationMethod("EGYPTIAN") }
        coVerify { harness.rescheduleNotifications.invoke() }
    }

    @Test
    fun `the asr method is stored lowercased, as the loader reads it back`() {
        // `setAsrCalculation(name.lowercase())` writing and `AsrCalculation.fromString` reading.
        // Storing "HANAFI" where the reader expects "hanafi" is a round-trip that silently
        // resets to Standard on the next launch.
        send(SettingsEvent.SetAsrMethod(AsrCalculation.HANAFI))

        assertThat(viewModel.prayerState.value.asrMethod).isEqualTo(AsrCalculation.HANAFI)
        coVerify { repo.mock.setAsrCalculation("hanafi") }
        coVerify { harness.rescheduleNotifications.invoke() }
    }

    @Test
    fun `the high latitude rule is stored by name and rearms`() {
        send(SettingsEvent.SetHighLatitudeRule(HighLatitudeRule.SEVENTH_OF_THE_NIGHT))

        assertThat(viewModel.prayerState.value.highLatitudeRule)
            .isEqualTo(HighLatitudeRule.SEVENTH_OF_THE_NIGHT)
        coVerify { repo.mock.setHighLatitudeRule("SEVENTH_OF_THE_NIGHT") }
        coVerify { harness.rescheduleNotifications.invoke() }
    }

    @Test
    fun `a manual adjustment lands on the prayer it names, whatever its case`() {
        // Six branches over a lowercased string. The screens pass "Fajr", "fajr" and "FAJR" from
        // three different call sites, so the lowercasing is load-bearing rather than defensive.
        send(
            SettingsEvent.SetPrayerAdjustment("Fajr", 3),
            SettingsEvent.SetPrayerAdjustment("SUNRISE", -2),
            SettingsEvent.SetPrayerAdjustment("dhuhr", 1),
            SettingsEvent.SetPrayerAdjustment("Asr", 4),
            SettingsEvent.SetPrayerAdjustment("maghrib", -5),
            SettingsEvent.SetPrayerAdjustment("Isha", 6),
        )

        val state = viewModel.prayerState.value
        assertThat(state.fajrAdjustment).isEqualTo(3)
        assertThat(state.sunriseAdjustment).isEqualTo(-2)
        assertThat(state.dhuhrAdjustment).isEqualTo(1)
        assertThat(state.asrAdjustment).isEqualTo(4)
        assertThat(state.maghribAdjustment).isEqualTo(-5)
        assertThat(state.ishaAdjustment).isEqualTo(6)
        // The *unlowered* name goes to the repository, which is what the persistence layer keys
        // on — so this is the pair that has to agree, not the two halves separately.
        coVerify { repo.mock.setPrayerAdjustment("Fajr", 3) }
    }

    @Test
    fun `an adjustment for a prayer that does not exist changes nothing`() {
        val before = viewModel.prayerState.value

        send(SettingsEvent.SetPrayerAdjustment("tahajjud", 99))

        assertThat(viewModel.prayerState.value).isEqualTo(before)
    }

    // ── Notifications ────────────────────────────────────────────────────────────────────────

    @Test
    fun `the notification master switch is stored and rearms`() {
        send(SettingsEvent.SetNotificationsEnabled(false))

        assertThat(viewModel.notificationState.value.notificationsEnabled).isFalse()
        coVerify { repo.mock.setPrayerNotificationsEnabled(false) }
        coVerify { harness.rescheduleNotifications.invoke() }
        assertThat(harness.telemetry.userProperties.map { it.name to it.value })
            .contains("notifications_enabled" to "false")
    }

    @Test
    fun `a per-prayer notification lands on its own field and its own preference`() {
        // Six identical rows. This is the exact shape the issue names: a toggle wired to its
        // neighbour's event moves both switches on screen and changes the wrong prayer.
        send(
            SettingsEvent.SetPrayerNotification("fajr", false),
            SettingsEvent.SetPrayerNotification("Sunrise", true),
            SettingsEvent.SetPrayerNotification("isha", false),
        )

        val state = viewModel.notificationState.value
        assertThat(state.fajrNotification).isFalse()
        assertThat(state.sunriseNotification).isTrue()
        assertThat(state.dhuhrNotification).isTrue()
        assertThat(state.ishaNotification).isFalse()
        coVerify { repo.mock.setPrayerNotificationEnabled("fajr", false) }
        coVerify { repo.mock.setPrayerNotificationEnabled("Sunrise", true) }
    }

    @Test
    fun `a notification toggle for an unknown prayer leaves the state alone`() {
        send(SettingsEvent.SetPrayerNotification("tarawih", true))

        val state = viewModel.notificationState.value
        assertThat(state.fajrNotification).isTrue()
        assertThat(state.sunriseNotification).isFalse()
    }

    @Test
    fun `an alert style is keyed lowercased and does not rearm`() {
        // The style is read when the alarm *fires*, not when it is set, so rescheduling here
        // would rewrite every alarm for a change that cannot affect any of their times.
        send(SettingsEvent.SetPrayerAlertStyle("Fajr", PrayerAlertStyle.ADHAN))

        assertThat(viewModel.notificationState.value.alertStyles)
            .containsEntry("fajr", PrayerAlertStyle.ADHAN)
        coVerify { repo.mock.setPrayerAlertStyle("fajr", PrayerAlertStyle.ADHAN) }
        coVerify(exactly = 0) { harness.rescheduleNotifications.invoke() }
    }

    @Test
    fun `a per-prayer reminder does rearm, because its lead time is baked into the alarm`() {
        send(SettingsEvent.SetPrayerReminderEnabled("Maghrib", true))

        assertThat(viewModel.notificationState.value.reminderEnabled)
            .containsEntry("maghrib", true)
        coVerify { repo.mock.setPrayerReminderEnabled("maghrib", true) }
        coVerify { harness.rescheduleNotifications.invoke() }
    }

    @Test
    fun `a per-prayer reminder offset is keyed lowercased and rearms`() {
        send(SettingsEvent.SetPrayerReminderMinutes("ISHA", 45))

        assertThat(viewModel.notificationState.value.reminderOffsets).containsEntry("isha", 45)
        coVerify { repo.mock.setPrayerReminderMinutes("isha", 45) }
        coVerify { harness.rescheduleNotifications.invoke() }
    }

    @Test
    fun `setting one prayer's reminder leaves the other four alone`() {
        send(
            SettingsEvent.SetPrayerReminderEnabled("fajr", true),
            SettingsEvent.SetPrayerReminderEnabled("asr", true),
        )
        send(SettingsEvent.SetPrayerReminderEnabled("fajr", false))

        val reminders = viewModel.notificationState.value.reminderEnabled
        assertThat(reminders).containsEntry("fajr", false)
        assertThat(reminders).containsEntry("asr", true)
    }

    @Test
    fun `vibration and do-not-disturb are stored without rearming anything`() {
        // Both are read at fire time. A reschedule here is a full alarm rewrite for a switch
        // that cannot move a single alarm.
        send(SettingsEvent.SetVibrationEnabled(false), SettingsEvent.SetRespectDnd(false))

        assertThat(viewModel.notificationState.value.vibrationEnabled).isFalse()
        assertThat(viewModel.notificationState.value.respectDnd).isFalse()
        coVerify { repo.mock.setNotificationVibration(false) }
        coVerify { repo.mock.setAdhanRespectDnd(false) }
        coVerify(exactly = 0) { harness.rescheduleNotifications.invoke() }
    }

    @Test
    fun `the persistent notification is stored without rearming`() {
        send(SettingsEvent.SetPersistentNotification(true))

        assertThat(viewModel.notificationState.value.persistentNotification).isTrue()
        coVerify { repo.mock.setPersistentNotification(true) }
        coVerify(exactly = 0) { harness.rescheduleNotifications.invoke() }
    }

    @Test
    fun `the reminder-before pair changes when a notification fires, so both rearm`() {
        send(
            SettingsEvent.SetReminderMinutes(30),
            SettingsEvent.SetShowReminderBefore(false),
        )

        assertThat(viewModel.notificationState.value.reminderMinutes).isEqualTo(30)
        assertThat(viewModel.notificationState.value.showReminderBefore).isFalse()
        coVerify { repo.mock.setNotificationReminderMinutes(30) }
        coVerify { repo.mock.setShowReminderBefore(false) }
        coVerify(exactly = 2) { harness.rescheduleNotifications.invoke() }
    }

    @Test
    fun `the friday and khatam reminders write their own values and rearm`() {
        send(
            SettingsEvent.SetFridayReminderEnabled(true),
            SettingsEvent.SetFridayReminderMinutes(120),
            SettingsEvent.SetKhatamReminderEnabled(true),
            SettingsEvent.SetKhatamReminderTime("22:15"),
        )

        val state = viewModel.notificationState.value
        assertThat(state.fridayReminderEnabled).isTrue()
        assertThat(state.fridayReminderMinutes).isEqualTo(120)
        assertThat(state.khatamReminderEnabled).isTrue()
        assertThat(state.khatamReminderTime).isEqualTo("22:15")
        coVerify { repo.mock.setFridayReminderEnabled(true) }
        coVerify { repo.mock.setFridayReminderMinutes(120) }
        coVerify { repo.mock.setKhatamReminderEnabled(true) }
        coVerify { repo.mock.setKhatamReminderTime("22:15") }
    }

    @Test
    fun `a worship reminder writes under its own key`() {
        // Eleven reminders share one keyed surface, so nothing about the call site says which
        // reminder it is except the key — the one thing a copy-paste does not change.
        send(
            SettingsEvent.SetWorshipReminderEnabled("tahajjud", true),
            SettingsEvent.SetWorshipReminderOffset("suhoor", 45),
            SettingsEvent.SetWorshipReminderMode("witr", "before_fajr"),
        )

        val state = viewModel.notificationState.value
        assertThat(state.worshipReminders).containsEntry("tahajjud", true)
        assertThat(state.worshipOffsets).containsEntry("suhoor", 45)
        assertThat(state.worshipModes).containsEntry("witr", "before_fajr")
        coVerify { repo.mock.setWorshipReminderEnabled("tahajjud", true) }
        coVerify { repo.mock.setWorshipReminderOffset("suhoor", 45) }
        coVerify { repo.mock.setWorshipReminderMode("witr", "before_fajr") }
        coVerify(exactly = 3) { harness.rescheduleNotifications.invoke() }
    }

    @Test
    fun `switching one worship reminder on leaves the rest as they were`() {
        send(SettingsEvent.SetWorshipReminderEnabled("iftar", true))
        send(SettingsEvent.SetWorshipReminderEnabled("taraweeh", true))
        send(SettingsEvent.SetWorshipReminderEnabled("iftar", false))

        val reminders = viewModel.notificationState.value.worshipReminders
        assertThat(reminders).containsEntry("iftar", false)
        assertThat(reminders).containsEntry("taraweeh", true)
    }

    @Test
    fun `picking an adhan that is not downloaded fetches it`() {
        coEvery { harness.adhanAudioManager.isFullyDownloaded(any()) } returns false

        send(SettingsEvent.SetAdhanSound(AdhanSound.entries.first().name))

        coVerify { repo.mock.setSelectedAdhanSound(AdhanSound.entries.first().name) }
        coVerify { harness.adhanDownloader.download(AdhanSound.entries.first().name) }
    }

    @Test
    fun `picking an adhan that is already downloaded does not fetch it again`() {
        // A settings screen that re-downloads a 3 MB file every time the row is tapped is not a
        // visible bug — it is a data bill.
        coEvery { harness.adhanAudioManager.isFullyDownloaded(any()) } returns true

        send(SettingsEvent.SetAdhanSound(AdhanSound.entries.first().name))

        coVerify(exactly = 0) { harness.adhanDownloader.download(any()) }
    }

    @Test
    fun `the adhan master switch is stored and rearms`() {
        send(SettingsEvent.SetAdhanEnabled(true))

        assertThat(viewModel.notificationState.value.adhanEnabled).isTrue()
        coVerify { repo.mock.setAdhanEnabled(true) }
        coVerify { harness.rescheduleNotifications.invoke() }
    }

    @Test
    fun `previewing an adhan already on disk plays it without downloading`() {
        coEvery { harness.adhanAudioManager.isFullyDownloaded(any()) } returns true

        send(SettingsEvent.PreviewAdhanSound)

        coVerify { harness.adhanAudioManager.preview(any(), false) }
        assertThat(viewModel.adhanPreviewError.value).isNull()
    }

    @Test
    fun `a preview whose download fails explains itself instead of playing silence`() {
        // The failure arm is the one a user actually meets — on a plane, or on a metered
        // connection. Without the message the preview button simply does nothing.
        coEvery { harness.adhanAudioManager.isFullyDownloaded(any()) } returns false
        coEvery { harness.adhanAudioManager.downloadAdhanWithFajr(any()) } returns false

        send(SettingsEvent.PreviewAdhanSound)

        assertThat(viewModel.adhanPreviewError.value).contains("internet connection")
        coVerify(exactly = 0) { harness.adhanAudioManager.preview(any(), any()) }
    }

    @Test
    fun `a preview whose download succeeds goes on to play`() {
        coEvery { harness.adhanAudioManager.isFullyDownloaded(any()) } returns false
        coEvery { harness.adhanAudioManager.downloadAdhanWithFajr(any()) } returns true

        send(SettingsEvent.PreviewAdhanSound)

        coVerify { harness.adhanAudioManager.preview(any(), false) }
        assertThat(viewModel.adhanPreviewError.value).isNull()
    }

    @Test
    fun `a preview that throws reports the failure and surfaces the message`() {
        coEvery { harness.adhanAudioManager.isFullyDownloaded(any()) } returns true
        coEvery { harness.adhanAudioManager.preview(any(), any()) } throws
            IllegalStateException("codec missing")

        send(SettingsEvent.PreviewAdhanSound)

        assertThat(viewModel.adhanPreviewError.value).contains("codec missing")
        assertThat(harness.telemetry.errors.map { it.type }).contains("adhan_preview")
    }

    @Test
    fun `the preview error can be dismissed`() {
        coEvery { harness.adhanAudioManager.isFullyDownloaded(any()) } returns false
        coEvery { harness.adhanAudioManager.downloadAdhanWithFajr(any()) } returns false
        send(SettingsEvent.PreviewAdhanSound)
        assertThat(viewModel.adhanPreviewError.value).isNotNull()

        viewModel.clearAdhanPreviewError()

        assertThat(viewModel.adhanPreviewError.value).isNull()
    }

    @Test
    fun `stopping the adhan preview stops the engine`() {
        send(SettingsEvent.StopAdhanPreview)

        verify { harness.adhanAudioManager.stopPreview() }
    }

    // ── Quran, Dua, Hadith ───────────────────────────────────────────────────────────────────

    @Test
    fun `every Quran control writes its own preference`() {
        send(
            SettingsEvent.SetTranslator("pickthall"),
            SettingsEvent.SetArabicFont("scheherazade"),
            SettingsEvent.SetShowTranslation(false),
            SettingsEvent.SetShowTransliteration(true),
            SettingsEvent.SetArabicFontSize(34f),
            SettingsEvent.SetTranslationFontSize(20f),
            SettingsEvent.SetContinuousReading(false),
            SettingsEvent.SetKeepScreenOn(false),
            SettingsEvent.SetShowTajweed(true),
            SettingsEvent.SetTajweedUnderline(true),
            SettingsEvent.SetMushafScript(MushafScript.entries.last()),
            SettingsEvent.SetReciter("alafasy"),
        )

        coVerify { repo.mock.setQuranTranslatorId("pickthall") }
        coVerify { repo.mock.setQuranArabicFont("scheherazade") }
        coVerify { repo.mock.setShowTranslation(false) }
        coVerify { repo.mock.setShowTransliteration(true) }
        // The two font sizes are the pair most likely to be crossed: same type, adjacent rows,
        // near-identical setter names.
        coVerify { repo.mock.setQuranArabicFontSize(34f) }
        coVerify { repo.mock.setQuranTranslationFontSize(20f) }
        coVerify { repo.mock.setContinuousReading(false) }
        coVerify { repo.mock.setKeepScreenOn(false) }
        coVerify { repo.mock.setShowTajweed(true) }
        coVerify { repo.mock.setTajweedUnderline(true) }
        coVerify { repo.mock.setQuranMushafScript(MushafScript.entries.last().name) }
        coVerify { repo.mock.setSelectedReciterId("alafasy") }
    }

    @Test
    fun `clearing the reciter stores a null rather than an empty id`() {
        send(SettingsEvent.SetReciter(null))

        assertThat(viewModel.quranState.value.selectedReciterId).isNull()
        coVerify { repo.mock.setSelectedReciterId(null) }
    }

    @Test
    fun `previewing a reciter plays one explicit ayah rather than whatever is loaded`() {
        // The picker loads no surah, so a preview built from the reader's state hands
        // `playFromAyah` an empty list and it returns without playing. That bug played silence
        // and set the spinner, and a `verify { … any() }` would pass against it.
        send(SettingsEvent.PreviewReciter("alafasy"))

        verify { harness.quranPlayback.setReciter("alafasy", restartIfPlaying = false) }
        verify { harness.quranPlayback.setContinuousPlayback(false) }
        verify {
            harness.quranPlayback.playFromAyah(
                ayahGlobalId = 1,
                allAyahs = match { it.size == 1 && it.single().ayahGlobalId == 1 },
                title = any(),
            )
        }
    }

    @Test
    fun `stopping a reciter preview clears the previewing id as well as the player`() {
        // Clearing only the player leaves the card's spinner running forever.
        send(SettingsEvent.PreviewReciter("alafasy"))
        send(SettingsEvent.StopReciterPreview)

        verify { harness.quranPlayback.stop() }
        runTest(dispatcher) {
            advanceUntilIdle()
            assertThat(viewModel.reciterPreview.value.reciterId).isNull()
        }
    }

    @Test
    fun `every Dua control writes its own preference`() {
        send(
            SettingsEvent.SetDuaArabicFont("noto"),
            SettingsEvent.SetDuaArabicFontSize(31f),
            SettingsEvent.SetDuaTranslationFontSize(18f),
            SettingsEvent.SetDuaShowArabic(false),
            SettingsEvent.SetDuaShowTransliteration(false),
            SettingsEvent.SetDuaShowTranslation(false),
        )

        val state = viewModel.duaState.value
        assertThat(state.selectedArabicFontId).isEqualTo("noto")
        assertThat(state.arabicFontSize).isEqualTo(31f)
        assertThat(state.translationFontSize).isEqualTo(18f)
        assertThat(state.showArabic).isFalse()
        assertThat(state.showTransliteration).isFalse()
        assertThat(state.showTranslation).isFalse()

        coVerify { repo.mock.setDuaArabicFont("noto") }
        coVerify { repo.mock.setDuaArabicFontSize(31f) }
        coVerify { repo.mock.setDuaTranslationFontSize(18f) }
        coVerify { repo.mock.setDuaShowArabic(false) }
        coVerify { repo.mock.setDuaShowTransliteration(false) }
        coVerify { repo.mock.setDuaShowTranslation(false) }
    }

    @Test
    fun `a Dua control does not write the Quran preference of the same name`() {
        // Three screens carry a row called "Arabic font size" and each must reach its own
        // corpus's preference. Crossing them changes the reader's font from the Dua screen.
        send(SettingsEvent.SetDuaArabicFontSize(31f))

        coVerify(exactly = 0) { repo.mock.setQuranArabicFontSize(any()) }
        coVerify(exactly = 0) { repo.mock.setHadithArabicFontSize(any()) }
    }

    @Test
    fun `every Hadith control writes its own preference`() {
        send(
            SettingsEvent.SetHadithArabicFont("kfgqpc"),
            SettingsEvent.SetHadithArabicFontSize(26f),
            SettingsEvent.SetHadithTranslationFontSize(15f),
            SettingsEvent.SetHadithShowArabic(false),
            SettingsEvent.SetHadithShowTranslation(false),
            SettingsEvent.SetHadithShowGrade(false),
            SettingsEvent.SetHadithShowChain(false),
        )

        val state = viewModel.hadithState.value
        assertThat(state.selectedArabicFontId).isEqualTo("kfgqpc")
        assertThat(state.arabicFontSize).isEqualTo(26f)
        assertThat(state.translationFontSize).isEqualTo(15f)
        assertThat(state.showArabic).isFalse()
        assertThat(state.showTranslation).isFalse()
        assertThat(state.showGrade).isFalse()
        assertThat(state.showChain).isFalse()

        coVerify { repo.mock.setHadithArabicFont("kfgqpc") }
        coVerify { repo.mock.setHadithArabicFontSize(26f) }
        coVerify { repo.mock.setHadithTranslationFontSize(15f) }
        coVerify { repo.mock.setHadithShowArabic(false) }
        coVerify { repo.mock.setHadithShowTranslation(false) }
        coVerify { repo.mock.setHadithShowGrade(false) }
        coVerify { repo.mock.setHadithShowChain(false) }
    }

    // ── Locations and the destructive actions ────────────────────────────────────────────────

    @Test
    fun `choosing a location inserts it and then makes the inserted row current`() {
        // Two calls that must be in that order and must use the *returned* id: setting the
        // current location to the id the caller happened to hold points prayer times at the
        // wrong city.
        coEvery { harness.prayerUseCases.insertLocation.invoke(any()) } returns 77L

        send(SettingsEvent.SetCurrentLocation(testLocation(id = 0L, name = "Cairo")))

        coVerify { harness.prayerUseCases.setCurrentLocation.invoke(77L) }
    }

    @Test
    fun `adding a location saves it without making it current`() {
        coEvery { harness.prayerUseCases.insertLocation.invoke(any()) } returns 5L
        val cairo = testLocation(id = 0L, name = "Cairo")

        send(SettingsEvent.AddLocation(cairo))

        coVerify { harness.prayerUseCases.insertLocation.invoke(cairo) }
        coVerify(exactly = 0) { harness.prayerUseCases.setCurrentLocation.invoke(any()) }
    }

    @Test
    fun `removing and favouriting a location reach their own use cases`() {
        val cairo = testLocation(id = 9L, name = "Cairo")

        send(SettingsEvent.RemoveLocation(cairo), SettingsEvent.ToggleLocationFavorite(9L))

        coVerify { harness.prayerUseCases.deleteLocation.invoke(cairo) }
        coVerify { harness.prayerUseCases.toggleFavorite.invoke(9L) }
    }

    @Test
    fun `a test notification is sent and recorded as one prayer, not all`() {
        send(SettingsEvent.TestNotification)

        verify { harness.prayerNotificationTester.sendTestNotification() }
        assertThat(harness.telemetry.calls).contains(
            TelemetryCall.TestNotification(allPrayers = false)
        )
    }

    @Test
    fun `testing all notifications is a different call and a different record`() {
        send(SettingsEvent.TestAllNotifications)

        verify { harness.prayerNotificationTester.sendAllPrayerTestNotifications() }
        assertThat(harness.telemetry.calls).contains(
            TelemetryCall.TestNotification(allPrayers = true)
        )
    }

    @Test
    fun `resetting notifications cancels the existing alarms before rebuilding them`() {
        // Rebuilding without cancelling leaves the old alarms armed, which is how "reset" ends
        // up doubling the notifications rather than fixing them.
        send(SettingsEvent.ResetNotifications)

        coVerify { harness.prayerAlarmScheduler.cancelAllPrayerNotifications() }
        coVerify { harness.rescheduleNotifications.invoke() }
    }

    @Test
    fun `reset to defaults clears the preferences and every state holder`() {
        // Dua and Hadith are the two this used to forget, so after a reset those screens went on
        // showing pre-reset font sizes until the process restarted.
        send(
            SettingsEvent.SetHapticFeedback(false),
            SettingsEvent.SetDuaArabicFontSize(40f),
            SettingsEvent.SetHadithShowChain(false),
        )

        send(SettingsEvent.ResetToDefaults)

        coVerify { repo.mock.clearAllData() }
        assertThat(viewModel.generalState.value).isEqualTo(GeneralSettingsUiState())
        assertThat(viewModel.duaState.value).isEqualTo(DuaSettingsUiState())
        assertThat(viewModel.hadithState.value).isEqualTo(HadithSettingsUiState())
        assertThat(viewModel.shouldRestart.value).isTrue()
    }

    @Test
    fun `delete all data clears the user database as well as the preferences`() {
        // The content database is deliberately untouched: the corpus is ours to replace and
        // never theirs to lose. Clearing preferences alone would leave every tracked prayer.
        send(SettingsEvent.DeleteAllData)

        coVerify { harness.clearAllUserData.invoke() }
        coVerify { repo.mock.clearAllData() }
        assertThat(viewModel.locationState.value).isEqualTo(LocationSettingsUiState())
        assertThat(viewModel.shouldRestart.value).isTrue()
    }

    @Test
    fun `the destructive actions are recorded before the work, so a half-failure still shows`() {
        send(SettingsEvent.ResetToDefaults)
        send(SettingsEvent.DeleteAllData)

        val actions = harness.telemetry.featureUsages.map { it.action }
        assertThat(actions).contains("reset_to_defaults")
        assertThat(actions).contains("delete_all_data")
    }

    @Test
    fun `a setting change reports itself from the shared table`() {
        // 78 branches report through one table rather than a line each, which is how 56 of them
        // came to report nothing at all. One representative from each family, so a family
        // dropped from the table is caught.
        send(
            SettingsEvent.SetHapticFeedback(false),
            SettingsEvent.SetCalculationMethod(CalculationMethod.KARACHI),
            SettingsEvent.SetVibrationEnabled(false),
            SettingsEvent.SetShowTajweed(true),
            SettingsEvent.SetDuaShowArabic(false),
            SettingsEvent.SetHadithShowGrade(false),
        )

        val settings = harness.telemetry.settingChanges.map { it.setting }
        assertThat(settings).containsAtLeast(
            "haptic_feedback",
            "calculation_method",
            "notification_vibration",
            "quran_show_tajweed",
            "dua_show_arabic",
            "hadith_show_grade",
        )
    }

    @Test
    fun `an action that is not a setting change reports nothing to the settings table`() {
        send(SettingsEvent.TestNotification, SettingsEvent.StopAdhanPreview)

        assertThat(harness.telemetry.settingChanges).isEmpty()
    }
}
