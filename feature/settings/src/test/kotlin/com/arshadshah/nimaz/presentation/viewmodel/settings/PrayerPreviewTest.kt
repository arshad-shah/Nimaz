package com.arshadshah.nimaz.presentation.viewmodel.settings

import app.cash.turbine.test
import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.HighLatitudeRule
import com.arshadshah.nimaz.domain.model.PrayerCalculationSettings
import com.arshadshah.nimaz.domain.model.PrayerTimes
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.domain.model.ResolvedLocation
import com.arshadshah.nimaz.testing.SettingsViewModelHarness
import com.arshadshah.nimaz.testing.testLocation
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * The prayer settings' live preview.
 *
 * It must be built from the same settings stream reminders read — so it can never show a time the
 * app will not use — and it must say *which* times the last change moved, which is how the screen
 * shows a reader what a setting does. The calculation itself is the domain's; here it is a fake
 * that moves Fajr with the method and Asr with the school, which is exactly the shape the real
 * astronomy has.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PrayerPreviewTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val today = LocalDate.of(2026, 9, 23)
    private val harness = SettingsViewModelHarness(today = today)

    private val dublin = ResolvedLocation(latitude = 53.35, longitude = -6.26, name = "Dublin", isFallback = false)
    private val settings = MutableStateFlow(
        PrayerCalculationSettings(
            location = dublin,
            calculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE,
            asrCalculation = AsrCalculation.STANDARD,
            highLatitudeRule = HighLatitudeRule.MIDDLE_OF_THE_NIGHT,
            adjustments = emptyMap(),
        )
    )

    private fun at(h: Int, m: Int) = LocalDateTime.of(today.year, today.month, today.dayOfMonth, h, m)

    /** Fajr moves with the method, Asr with the school, everything else stays. */
    private fun fakeTimes(s: PrayerCalculationSettings) = PrayerTimes(
        fajr = at(5, 13 + s.calculationMethod.ordinal),
        sunrise = at(7, 12),
        dhuhr = at(13, 17),
        asr = if (s.asrCalculation == AsrCalculation.HANAFI) at(17, 18) else at(16, 31),
        maghrib = at(19, 21),
        isha = at(21, 13),
        date = today,
        location = testLocation(),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { harness.prayerUseCases.observeCalculationSettings() } returns settings
        every {
            harness.prayerUseCases.getPrayerTimesForDate.invoke(any<LocalDate>(), any<PrayerCalculationSettings>())
        } answers { fakeTimes(secondArg()) }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `the preview shows today's times under the stored settings, with nothing marked`() = runTest {
        harness.build().prayerPreview.test {
            // With an unconfined dispatcher the computed value lands before the seed is observed.
            val preview = awaitItem()
            assertThat(preview.times[PrayerType.FAJR]).isEqualTo(at(5, 13))
            assertThat(preview.times[PrayerType.ASR]).isEqualTo(at(16, 31))
            assertThat(preview.changed).isEmpty()
            assertThat(preview.locationName).isEqualTo("Dublin")
            assertThat(preview.latitude).isEqualTo(53.35)
        }
    }

    @Test
    fun `a change marks exactly the times it moved`() = runTest {
        harness.build().prayerPreview.test {
            awaitItem()
            settings.value = settings.value.copy(asrCalculation = AsrCalculation.HANAFI)
            val preview = awaitItem()
            assertThat(preview.times[PrayerType.ASR]).isEqualTo(at(17, 18))
            assertThat(preview.changed).containsExactly(PrayerType.ASR)
        }
    }

    @Test
    fun `the pickers get today's Fajr under every method and Asr under both schools`() = runTest {
        harness.build().prayerPreview.test {
            val preview = awaitItem()
            assertThat(preview.methodFajr.keys).containsExactlyElementsIn(CalculationMethod.entries)
            assertThat(preview.methodFajr[CalculationMethod.EGYPTIAN])
                .isEqualTo(at(5, 13 + CalculationMethod.EGYPTIAN.ordinal))
            assertThat(preview.asrTimes).containsExactly(
                AsrCalculation.STANDARD, at(16, 31),
                AsrCalculation.HANAFI, at(17, 18),
            )
        }
    }

    @Test
    fun `a fallback location is not named`() = runTest {
        // The screen would otherwise tell the reader about the latitude of a city they are not in.
        settings.value = settings.value.copy(location = dublin.copy(isFallback = true))
        harness.build().prayerPreview.test {
            val preview = awaitItem()
            assertThat(preview.locationName).isNull()
            assertThat(preview.isFallbackLocation).isTrue()
        }
    }
}
