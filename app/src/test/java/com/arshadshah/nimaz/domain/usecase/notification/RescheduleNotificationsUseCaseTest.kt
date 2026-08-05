package com.arshadshah.nimaz.domain.usecase.notification

import com.arshadshah.nimaz.core.util.PrayerNotificationScheduler
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.HighLatitudeRule
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.domain.model.UserPreferences
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * The alarm set is built from what is persisted, and from nothing else.
 *
 * A prayer the user switched off in Notification Settings used to come back on, because the
 * reschedule read a `SettingsViewModel` snapshot taken at construction — and `hiltViewModel()`
 * gives each settings screen its own instance, so one screen's snapshot went stale the moment
 * another wrote. Turning Isha off on one screen and changing the Asr method on another re-armed
 * the Isha adhan.
 *
 * These tests exist to make that unrepresentable: there is no state to pass in any more.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RescheduleNotificationsUseCaseTest {

    private lateinit var settings: SettingsRepository
    private lateinit var scheduler: PrayerNotificationScheduler

    private val ishaEnabled = MutableStateFlow(true)
    private val notificationsEnabled = MutableStateFlow(true)
    private val highLatitudeRule = MutableStateFlow("MIDDLE_OF_NIGHT")

    @Before
    fun setUp() {
        settings = mockk(relaxed = true)
        scheduler = mockk(relaxed = true)

        every { settings.userPreferences } returns flowOf(preferences())
        every { settings.prayerNotificationsEnabled } returns notificationsEnabled
        every { settings.fajrNotificationEnabled } returns flowOf(true)
        every { settings.sunriseNotificationEnabled } returns flowOf(false)
        every { settings.dhuhrNotificationEnabled } returns flowOf(true)
        every { settings.asrNotificationEnabled } returns flowOf(true)
        every { settings.maghribNotificationEnabled } returns flowOf(true)
        every { settings.ishaNotificationEnabled } returns ishaEnabled
        every { settings.highLatitudeRule } returns highLatitudeRule
        every { settings.fajrAdjustment } returns flowOf(-5)
        every { settings.sunriseAdjustment } returns flowOf(0)
        every { settings.dhuhrAdjustment } returns flowOf(0)
        every { settings.asrAdjustment } returns flowOf(0)
        every { settings.maghribAdjustment } returns flowOf(0)
        every { settings.ishaAdjustment } returns flowOf(0)
        every { settings.fridayReminderEnabled } returns flowOf(false)
        every { settings.fridayReminderMinutes } returns flowOf(60)
        // preReminderMinutesByPrayer() reads a pair of flows per prayer; a relaxed mock
        // returns a Flow that emits nothing, and `.first()` on that throws.
        every { settings.prayerReminderEnabled(any()) } returns flowOf(false)
        every { settings.prayerReminderMinutes(any()) } returns flowOf(0)
    }

    private fun useCase() = RescheduleNotificationsUseCase(settings, scheduler)

    @Test
    fun `a prayer switched off is not re-armed`() = runTest {
        // Switched off on another settings screen, after this one was constructed.
        ishaEnabled.value = false

        useCase().invoke()

        assertThat(scheduledPrayers()).doesNotContain(PrayerType.ISHA)
    }

    @Test
    fun `the prayers that are on are armed`() = runTest {
        useCase().invoke()

        assertThat(scheduledPrayers()).containsExactly(
            PrayerType.FAJR,
            PrayerType.DHUHR,
            PrayerType.ASR,
            PrayerType.MAGHRIB,
            PrayerType.ISHA,
        )
    }

    @Test
    fun `notifications switched off globally schedule nothing`() = runTest {
        // The defaults-window variant: state held `notificationsEnabled = true` until ~40
        // sequential reads finished, so a tap on the first frame armed a full day of alarms
        // for a user who had them off.
        notificationsEnabled.value = false

        useCase().invoke()

        val enabled = slot<Boolean>()
        verify {
            scheduler.scheduleTodaysPrayerNotifications(
                any(), any(), capture(enabled), any(), any(), any(), any(), any(), any(),
                any(), any(),
            )
        }
        assertThat(enabled.captured).isFalse()
    }

    @Test
    fun `the persisted high-latitude rule survives its older spelling`() = runTest {
        // The old code remapped these by hand inside `catch (_: Exception) { null }`, so a
        // value it did not cover silently dropped the correction — hours of error at 60°N.
        useCase().invoke()

        assertThat(capturedRule()).isEqualTo(HighLatitudeRule.MIDDLE_OF_THE_NIGHT)
    }

    @Test
    fun `the newer spelling is accepted too`() = runTest {
        highLatitudeRule.value = "SEVENTH_OF_THE_NIGHT"

        useCase().invoke()

        assertThat(capturedRule()).isEqualTo(HighLatitudeRule.SEVENTH_OF_THE_NIGHT)
    }

    private fun scheduledPrayers(): Set<PrayerType> {
        val captured = slot<Set<PrayerType>>()
        verify {
            scheduler.scheduleTodaysPrayerNotifications(
                any(), any(), any(), capture(captured), any(), any(), any(), any(), any(),
                any(), any(),
            )
        }
        return captured.captured
    }

    private fun capturedRule(): HighLatitudeRule? {
        val captured = mutableListOf<HighLatitudeRule?>()
        verify {
            scheduler.scheduleTodaysPrayerNotifications(
                any(), any(), any(), any(), any(), any(), any(), captureNullable(captured), any(),
                any(), any(),
            )
        }
        return captured.last()
    }
}

private fun preferences() = UserPreferences(
    onboardingCompleted = true,
    themeMode = "system",
    dynamicColor = false,
    appLanguage = "en",
    calculationMethod = CalculationMethod.UMM_AL_QURA.name,
    asrCalculation = "hanafi",
    latitude = 53.35,
    longitude = -6.26,
    locationName = "Dublin",
    prayerNotificationsEnabled = true,
    quranTranslatorId = "en.sahih",
    showTranslation = true,
)
