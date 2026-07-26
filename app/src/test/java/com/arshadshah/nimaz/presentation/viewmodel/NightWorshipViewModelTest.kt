package com.arshadshah.nimaz.presentation.viewmodel

import com.arshadshah.nimaz.core.util.PrayerTimeCalculator
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The night worship hub's ViewModel.
 *
 * Two things are worth pinning here, and both are cheap to get wrong:
 *
 * 1. **Fajr comes from tomorrow.** The window runs from tonight's last third to the *next* Fajr.
 *    Taking today's Fajr would close the window at a time already past, leaving the hub
 *    permanently reading "tonight's window has passed" — for a screen you reach from a card that
 *    just told you it is time to pray.
 * 2. **The counter moves in pairs**, because night prayer is offered two by two (Tirmidhi 437).
 *
 * The ViewModel publishes instants and no elapsed time at all, which is why there is nothing here
 * about countdowns: those are derived in the composable from the shared ticker. A ViewModel that
 * pushed "now" as state is what froze the timers in the first place.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NightWorshipViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    /**
     * The real calculator — it is pure and Android-free, so there is nothing to gain from faking
     * astronomy, and a fake would let a wrong-day Fajr bug through unnoticed.
     */
    private val calculator = PrayerTimeCalculator()

    private lateinit var settings: SettingsRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        settings = mockk(relaxed = true) {
            every { latitude } returns flowOf(LONDON_LAT)
            every { longitude } returns flowOf(LONDON_LON)
            every { calculationMethod } returns flowOf("MUSLIM_WORLD_LEAGUE")
            every { asrCalculation } returns flowOf("STANDARD")
            every { highLatitudeRule } returns flowOf("MIDDLE_OF_THE_NIGHT")
        }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `publishes tonight's window with fajr after the last third`() = runTest(dispatcher) {
        val viewModel = NightWorshipViewModel(calculator, settings)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertNotNull("last third not resolved", state.lastThirdAt)
        assertNotNull("fajr not resolved", state.fajrAt)

        // The whole point of taking Fajr from tomorrow: the window must be forward-going.
        assertTrue(
            "Window is inverted — Fajr (${state.fajrAt}) is not after the last third " +
                "(${state.lastThirdAt}); the hub would always read as closed",
            state.fajrAt!! > state.lastThirdAt!!,
        )
    }

    @Test
    fun `stops loading once the night times resolve`() = runTest(dispatcher) {
        val viewModel = NightWorshipViewModel(calculator, settings)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, viewModel.state.value.isLoading)
        assertEquals(null, viewModel.state.value.error)
    }

    @Test
    fun `rakah counter moves two at a time and resets`() = runTest(dispatcher) {
        val viewModel = NightWorshipViewModel(calculator, settings)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, viewModel.state.value.rakahCount)

        viewModel.onEvent(NightWorshipEvent.AddRakahPair)
        viewModel.onEvent(NightWorshipEvent.AddRakahPair)
        assertEquals("Night prayer is prayed two by two", 4, viewModel.state.value.rakahCount)

        viewModel.onEvent(NightWorshipEvent.ResetRakahs)
        assertEquals(0, viewModel.state.value.rakahCount)
    }

    private companion object {
        const val LONDON_LAT = 51.5074
        const val LONDON_LON = -0.1278
    }
}
