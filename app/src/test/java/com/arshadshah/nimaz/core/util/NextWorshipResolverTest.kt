package com.arshadshah.nimaz.core.util

import com.arshadshah.nimaz.domain.model.WorshipReminderType
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Integration test for [NextWorshipResolver] using the REAL [PrayerTimeCalculator] and a mocked
 * [SettingsRepository]. Verifies enabled-filtering, the "near" window, and nearest selection.
 */
class NextWorshipResolverTest {

    private lateinit var settings: SettingsRepository
    private lateinit var resolver: NextWorshipResolver

    @Before
    fun setUp() {
        settings = mockk(relaxed = true)
        // Makkah — always yields sensible prayer/sunnah times.
        every { settings.latitude } returns flowOf(21.4225)
        every { settings.longitude } returns flowOf(39.8262)
        every { settings.calculationMethod } returns flowOf("MUSLIM_WORLD_LEAGUE")
        every { settings.asrCalculation } returns flowOf("STANDARD")
        every { settings.highLatitudeRule } returns flowOf("")
        every { settings.hijriDayOffset } returns flowOf(0)
        every { settings.fajrAdjustment } returns flowOf(0)
        every { settings.sunriseAdjustment } returns flowOf(0)
        every { settings.dhuhrAdjustment } returns flowOf(0)
        every { settings.asrAdjustment } returns flowOf(0)
        every { settings.maghribAdjustment } returns flowOf(0)
        every { settings.ishaAdjustment } returns flowOf(0)
        every { settings.worshipReminderEnabled(any()) } returns flowOf(false)
        every { settings.worshipReminderOffset(any(), any()) } answers { flowOf(secondArg()) }
        resolver = NextWorshipResolver(PrayerTimeCalculator(), settings)
    }

    @Test
    fun `returns null when nothing enabled`() = runBlocking {
        val now = LocalDate.of(2026, 3, 10).atTime(21, 0)
        assertThat(resolver.nearest(now)).isNull()
    }

    @Test
    fun `returns null when no location`() = runBlocking {
        every { settings.latitude } returns flowOf(0.0)
        every { settings.longitude } returns flowOf(0.0)
        every { settings.worshipReminderEnabled(WorshipReminderType.TAHAJJUD.key) } returns flowOf(true)
        assertThat(resolver.nearest(LocalDate.of(2026, 3, 10).atTime(21, 0))).isNull()
    }

    @Test
    fun `surfaces the enabled Tahajjud reminder in the evening`() = runBlocking {
        every { settings.worshipReminderEnabled(WorshipReminderType.TAHAJJUD.key) } returns flowOf(true)
        // 22:00 — the last third (early morning) is within the 14h near-window.
        val occ = resolver.nearest(LocalDate.of(2026, 3, 10).atTime(22, 0))
        assertThat(occ).isNotNull()
        assertThat(occ!!.type).isEqualTo(WorshipReminderType.TAHAJJUD)
        assertThat(occ.triggerAt).isGreaterThan(LocalDate.of(2026, 3, 10).atTime(22, 0))
    }

    @Test
    fun `filters out reminders beyond the near window`() = runBlocking {
        // Enable only morning adhkar (fires after Fajr, ~05:30). At 06:00, the next one is
        // ~tomorrow 05:30 → ~23.5h away → outside the 14h window → nothing surfaced.
        every { settings.worshipReminderEnabled(WorshipReminderType.ADHKAR_MORNING.key) } returns flowOf(true)
        val occ = resolver.nearest(LocalDate.of(2026, 3, 10).atTime(6, 0))
        assertThat(occ).isNull()
    }
}
