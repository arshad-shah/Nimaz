package com.arshadshah.nimaz.testing

import com.arshadshah.nimaz.domain.model.PrayerAlertStyle
import com.arshadshah.nimaz.domain.model.UserPreferences
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A [SettingsRepository] whose **reads are real flows** and whose writes are recorded by mockk.
 *
 * The distinction is the whole reason this exists. `SettingsViewModel.loadSettings()` reads
 * fifty-odd preferences with `.first()`, and a `mockk(relaxed = true)` answers each of those
 * properties with a *relaxed `Flow`* — an object whose `collect` returns without ever emitting.
 * `first()` on that throws `NoSuchElementException`, which `launchSafely` swallows, so the loader
 * dies on its first line and every one of the 152 lines after it is dead. The module's existing
 * `SettingsViewModelTest` stubbed the fifteen flows its own assertions needed and left the rest;
 * the result reads as a passing test of a ViewModel whose constructor never finished its work.
 *
 * Every flow here is a [MutableStateFlow], so a test can also *change* a preference after
 * construction and watch the reactive rollups ([SettingsViewModel.notificationSummary],
 * `observeQuranSettings`) follow it — which is the behaviour those two exist for.
 *
 * Writes stay on the mockk so `coVerify { repo.setHapticFeedback(false) }` keeps working: the
 * question this module's tests ask most often is "did the control the user touched write the
 * preference it names, or its neighbour's", and a verify against the exact setter is how that is
 * asked.
 */
class SettingsRepositoryStub {

    // --- General -----------------------------------------------------------------------------
    val themeMode = MutableStateFlow("system")
    val appLanguage = MutableStateFlow("en")
    val showIslamicPatterns = MutableStateFlow(true)
    val patternStyle = MutableStateFlow("CORNER_MEDALLION")
    val animationsEnabled = MutableStateFlow(true)
    val showCountdown = MutableStateFlow(true)
    val showQuickActions = MutableStateFlow(true)
    val hapticFeedback = MutableStateFlow(true)
    val use24HourFormat = MutableStateFlow(false)
    val useHijriPrimary = MutableStateFlow(false)
    val hijriDayOffset = MutableStateFlow(0)

    // --- Prayer ------------------------------------------------------------------------------
    val calculationMethod = MutableStateFlow("MUSLIM_WORLD_LEAGUE")
    val asrCalculation = MutableStateFlow("standard")
    val highLatitudeRule = MutableStateFlow("MIDDLE_OF_THE_NIGHT")
    val fajrAdjustment = MutableStateFlow(0)
    val sunriseAdjustment = MutableStateFlow(0)
    val dhuhrAdjustment = MutableStateFlow(0)
    val asrAdjustment = MutableStateFlow(0)
    val maghribAdjustment = MutableStateFlow(0)
    val ishaAdjustment = MutableStateFlow(0)

    // --- Notifications -----------------------------------------------------------------------
    val prayerNotificationsEnabled = MutableStateFlow(true)
    val adhanEnabled = MutableStateFlow(false)
    val notificationVibration = MutableStateFlow(true)
    val notificationReminderMinutes = MutableStateFlow(15)
    val showReminderBefore = MutableStateFlow(true)
    val persistentNotification = MutableStateFlow(false)
    val fridayReminderEnabled = MutableStateFlow(false)
    val fridayReminderMinutes = MutableStateFlow(60)
    val khatamReminderEnabled = MutableStateFlow(false)
    val khatamReminderTime = MutableStateFlow("06:00")
    val adhanRespectDnd = MutableStateFlow(true)
    val selectedAdhanSound = MutableStateFlow("MISHARY")
    val fajrNotificationEnabled = MutableStateFlow(true)

    /** Off by default, and `:core:datastore` pins that it is. The screens must agree. */
    val sunriseNotificationEnabled = MutableStateFlow(false)
    val dhuhrNotificationEnabled = MutableStateFlow(true)
    val asrNotificationEnabled = MutableStateFlow(true)
    val maghribNotificationEnabled = MutableStateFlow(true)
    val ishaNotificationEnabled = MutableStateFlow(true)

    /** Per-prayer, so a test can make one prayer differ from the other four. */
    val alertStyles = PrayerAlertStyle.PRAYER_KEYS
        .associateWith { MutableStateFlow(PrayerAlertStyle.NOTIFICATION) }
    val reminderEnabled = PrayerAlertStyle.PRAYER_KEYS.associateWith { MutableStateFlow(false) }
    val reminderMinutes = PrayerAlertStyle.PRAYER_KEYS.associateWith { MutableStateFlow(15) }
    val worshipEnabled = mutableMapOf<String, MutableStateFlow<Boolean>>()
    val worshipOffsets = mutableMapOf<String, MutableStateFlow<Int>>()
    val worshipModes = mutableMapOf<String, MutableStateFlow<String>>()

    // --- Quran / Dua / Hadith ------------------------------------------------------------------
    val quranTranslatorId = MutableStateFlow("sahih_international")
    val quranArabicFont = MutableStateFlow("amiri")
    val selectedReciterId = MutableStateFlow<String?>(null)
    val quranMushafScript = MutableStateFlow("DEFAULT")
    val showTranslation = MutableStateFlow(true)
    val showTransliteration = MutableStateFlow(false)
    val quranArabicFontSize = MutableStateFlow(28f)
    val quranTranslationFontSize = MutableStateFlow(16f)
    val continuousReading = MutableStateFlow(true)
    val keepScreenOn = MutableStateFlow(true)
    val showTajweed = MutableStateFlow(false)
    val tajweedUnderline = MutableStateFlow(false)

    val duaArabicFont = MutableStateFlow("amiri")
    val duaArabicFontSize = MutableStateFlow(28f)
    val duaTranslationFontSize = MutableStateFlow(16f)
    val duaShowArabic = MutableStateFlow(true)
    val duaShowTransliteration = MutableStateFlow(true)
    val duaShowTranslation = MutableStateFlow(true)

    val hadithArabicFont = MutableStateFlow("amiri")
    val hadithArabicFontSize = MutableStateFlow(24f)
    val hadithTranslationFontSize = MutableStateFlow(16f)
    val hadithShowArabic = MutableStateFlow(true)
    val hadithShowTranslation = MutableStateFlow(true)
    val hadithShowGrade = MutableStateFlow(true)
    val hadithShowChain = MutableStateFlow(true)

    val userPreferences = MutableStateFlow(
        UserPreferences(
            onboardingCompleted = true,
            themeMode = "system",
            dynamicColor = false,
            appLanguage = "en",
            calculationMethod = "MUSLIM_WORLD_LEAGUE",
            asrCalculation = "standard",
            latitude = 51.5074,
            longitude = -0.1278,
            locationName = "London",
            prayerNotificationsEnabled = true,
            quranTranslatorId = "sahih_international",
            showTranslation = true,
        )
    )

    /** The mock every assertion verifies against. Reads are wired to the flows above. */
    val mock: SettingsRepository = mockk(relaxed = true) {
        every { themeMode } returns this@SettingsRepositoryStub.themeMode
        every { appLanguage } returns this@SettingsRepositoryStub.appLanguage
        every { showIslamicPatterns } returns this@SettingsRepositoryStub.showIslamicPatterns
        every { patternStyle } returns this@SettingsRepositoryStub.patternStyle
        every { animationsEnabled } returns this@SettingsRepositoryStub.animationsEnabled
        every { showCountdown } returns this@SettingsRepositoryStub.showCountdown
        every { showQuickActions } returns this@SettingsRepositoryStub.showQuickActions
        every { hapticFeedback } returns this@SettingsRepositoryStub.hapticFeedback
        every { use24HourFormat } returns this@SettingsRepositoryStub.use24HourFormat
        every { useHijriPrimary } returns this@SettingsRepositoryStub.useHijriPrimary
        every { hijriDayOffset } returns this@SettingsRepositoryStub.hijriDayOffset

        every { calculationMethod } returns this@SettingsRepositoryStub.calculationMethod
        every { asrCalculation } returns this@SettingsRepositoryStub.asrCalculation
        every { highLatitudeRule } returns this@SettingsRepositoryStub.highLatitudeRule
        every { fajrAdjustment } returns this@SettingsRepositoryStub.fajrAdjustment
        every { sunriseAdjustment } returns this@SettingsRepositoryStub.sunriseAdjustment
        every { dhuhrAdjustment } returns this@SettingsRepositoryStub.dhuhrAdjustment
        every { asrAdjustment } returns this@SettingsRepositoryStub.asrAdjustment
        every { maghribAdjustment } returns this@SettingsRepositoryStub.maghribAdjustment
        every { ishaAdjustment } returns this@SettingsRepositoryStub.ishaAdjustment

        every {
            prayerNotificationsEnabled
        } returns this@SettingsRepositoryStub.prayerNotificationsEnabled
        every { adhanEnabled } returns this@SettingsRepositoryStub.adhanEnabled
        every { notificationVibration } returns this@SettingsRepositoryStub.notificationVibration
        every {
            notificationReminderMinutes
        } returns this@SettingsRepositoryStub.notificationReminderMinutes
        every { showReminderBefore } returns this@SettingsRepositoryStub.showReminderBefore
        every {
            persistentNotification
        } returns this@SettingsRepositoryStub.persistentNotification
        every { fridayReminderEnabled } returns this@SettingsRepositoryStub.fridayReminderEnabled
        every { fridayReminderMinutes } returns this@SettingsRepositoryStub.fridayReminderMinutes
        every { khatamReminderEnabled } returns this@SettingsRepositoryStub.khatamReminderEnabled
        every { khatamReminderTime } returns this@SettingsRepositoryStub.khatamReminderTime
        every { adhanRespectDnd } returns this@SettingsRepositoryStub.adhanRespectDnd
        every { selectedAdhanSound } returns this@SettingsRepositoryStub.selectedAdhanSound
        every {
            fajrNotificationEnabled
        } returns this@SettingsRepositoryStub.fajrNotificationEnabled
        every {
            sunriseNotificationEnabled
        } returns this@SettingsRepositoryStub.sunriseNotificationEnabled
        every {
            dhuhrNotificationEnabled
        } returns this@SettingsRepositoryStub.dhuhrNotificationEnabled
        every { asrNotificationEnabled } returns this@SettingsRepositoryStub.asrNotificationEnabled
        every {
            maghribNotificationEnabled
        } returns this@SettingsRepositoryStub.maghribNotificationEnabled
        every {
            ishaNotificationEnabled
        } returns this@SettingsRepositoryStub.ishaNotificationEnabled

        every { prayerAlertStyle(any()) } answers {
            this@SettingsRepositoryStub.alertStyles[firstArg<String>()]
                ?: MutableStateFlow(PrayerAlertStyle.NOTIFICATION)
        }
        every { prayerReminderEnabled(any()) } answers {
            this@SettingsRepositoryStub.reminderEnabled[firstArg<String>()]
                ?: MutableStateFlow(false)
        }
        every { prayerReminderMinutes(any()) } answers {
            this@SettingsRepositoryStub.reminderMinutes[firstArg<String>()]
                ?: MutableStateFlow(15)
        }
        every { worshipReminderEnabled(any()) } answers {
            this@SettingsRepositoryStub.worshipEnabled
                .getOrPut(firstArg()) { MutableStateFlow(false) }
        }
        every { worshipReminderOffset(any(), any()) } answers {
            this@SettingsRepositoryStub.worshipOffsets
                .getOrPut(firstArg()) { MutableStateFlow(secondArg()) }
        }
        every { worshipReminderMode(any(), any()) } answers {
            this@SettingsRepositoryStub.worshipModes
                .getOrPut(firstArg()) { MutableStateFlow(secondArg()) }
        }

        every { quranTranslatorId } returns this@SettingsRepositoryStub.quranTranslatorId
        every { quranArabicFont } returns this@SettingsRepositoryStub.quranArabicFont
        every { selectedReciterId } returns this@SettingsRepositoryStub.selectedReciterId
        every { quranMushafScript } returns this@SettingsRepositoryStub.quranMushafScript
        every { showTranslation } returns this@SettingsRepositoryStub.showTranslation
        every { showTransliteration } returns this@SettingsRepositoryStub.showTransliteration
        every { quranArabicFontSize } returns this@SettingsRepositoryStub.quranArabicFontSize
        every {
            quranTranslationFontSize
        } returns this@SettingsRepositoryStub.quranTranslationFontSize
        every { continuousReading } returns this@SettingsRepositoryStub.continuousReading
        every { keepScreenOn } returns this@SettingsRepositoryStub.keepScreenOn
        every { showTajweed } returns this@SettingsRepositoryStub.showTajweed
        every { tajweedUnderline } returns this@SettingsRepositoryStub.tajweedUnderline

        every { duaArabicFont } returns this@SettingsRepositoryStub.duaArabicFont
        every { duaArabicFontSize } returns this@SettingsRepositoryStub.duaArabicFontSize
        every {
            duaTranslationFontSize
        } returns this@SettingsRepositoryStub.duaTranslationFontSize
        every { duaShowArabic } returns this@SettingsRepositoryStub.duaShowArabic
        every {
            duaShowTransliteration
        } returns this@SettingsRepositoryStub.duaShowTransliteration
        every { duaShowTranslation } returns this@SettingsRepositoryStub.duaShowTranslation

        every { hadithArabicFont } returns this@SettingsRepositoryStub.hadithArabicFont
        every { hadithArabicFontSize } returns this@SettingsRepositoryStub.hadithArabicFontSize
        every {
            hadithTranslationFontSize
        } returns this@SettingsRepositoryStub.hadithTranslationFontSize
        every { hadithShowArabic } returns this@SettingsRepositoryStub.hadithShowArabic
        every { hadithShowTranslation } returns this@SettingsRepositoryStub.hadithShowTranslation
        every { hadithShowGrade } returns this@SettingsRepositoryStub.hadithShowGrade
        every { hadithShowChain } returns this@SettingsRepositoryStub.hadithShowChain

        every { userPreferences } returns this@SettingsRepositoryStub.userPreferences
    }
}
