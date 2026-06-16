package com.arshadshah.nimaz.presentation.viewmodel

import com.arshadshah.nimaz.data.local.database.dao.IslamicEventDao
import com.arshadshah.nimaz.data.local.database.entity.IslamicEventEntity
import com.arshadshah.nimaz.domain.model.IslamicEventType
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var dao: IslamicEventDao
    private lateinit var viewModel: CalendarViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        dao = mockk(relaxed = true)
        every { dao.getAllEvents() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = CalendarViewModel(dao)

    private fun event(
        id: Int = 1,
        hijriMonth: Int,
        hijriDay: Int,
        eventType: String = "holiday",
        isHoliday: Int = 1
    ) = IslamicEventEntity(
        id = id, nameEnglish = "Event $id", nameArabic = "حدث",
        hijriMonth = hijriMonth, hijriDay = hijriDay, eventType = eventType,
        description = "desc", isHoliday = isHoliday
    )

    // ── Init ────────────────────────────────────────────────────────

    @Test
    fun `init loads events and clears the events loading flag`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        assertThat(viewModel.eventsState.value.isLoading).isFalse()
        assertThat(viewModel.calendarState.value.error).isNull()
    }

    @Test
    fun `init records an error when loading events fails`() = runTest {
        every { dao.getAllEvents() } returns flow { throw RuntimeException("boom") }

        viewModel = createViewModel()
        advanceUntilIdle()

        assertThat(viewModel.calendarState.value.error).contains("boom")
        assertThat(viewModel.calendarState.value.isLoading).isFalse()
    }

    @Test
    fun `init selects today and loads the current Gregorian month`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.calendarState.value
        assertThat(state.selectedDate).isEqualTo(LocalDate.now())
        assertThat(state.selectedHijriDate).isNotNull()
        assertThat(state.currentMonth).isNotNull()
        assertThat(state.isLoading).isFalse()
    }

    // ── Gregorian month generation ──────────────────────────────────

    @Test
    fun `NavigateToMonth builds a full month of days`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(CalendarEvent.NavigateToMonth(1, 2025)) // January = 31 days
        advanceUntilIdle()

        val month = viewModel.calendarState.value.currentMonth
        assertThat(month).isNotNull()
        assertThat(month!!.days).hasSize(31)
        assertThat(month.days.all { it.isCurrentMonth }).isTrue()
        assertThat(viewModel.calendarState.value.isLoading).isFalse()
    }

    @Test
    fun `NavigateToNextMonth advances from the current month`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(CalendarEvent.NavigateToMonth(1, 2025))
        advanceUntilIdle()
        viewModel.onEvent(CalendarEvent.NavigateToNextMonth)
        advanceUntilIdle()

        // February 2025 has 28 days.
        assertThat(viewModel.calendarState.value.currentMonth!!.days).hasSize(28)
    }

    // ── View mode ───────────────────────────────────────────────────

    @Test
    fun `SetViewMode updates the calendar view mode`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(CalendarEvent.SetViewMode(CalendarViewMode.HIJRI))
        assertThat(viewModel.calendarState.value.viewMode).isEqualTo(CalendarViewMode.HIJRI)
    }

    // ── Year overview ───────────────────────────────────────────────

    @Test
    fun `NavigateToYear builds twelve Gregorian months`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(CalendarEvent.NavigateToYear(2025, isHijri = false))
        advanceUntilIdle()

        val state = viewModel.yearState.value
        assertThat(state.year).isEqualTo(2025)
        assertThat(state.months).hasSize(12)
        assertThat(state.isLoading).isFalse()
    }

    // ── Event mapping + Hijri-day filtering ─────────────────────────

    @Test
    fun `Hijri month days surface events mapped from entities by month and day`() = runTest {
        // A "night" event on 27 Ramadan (month 9). Filtering is purely by hijri
        // month/day, so this also exercises the entity -> domain mapping.
        every { dao.getAllEvents() } returns flowOf(
            listOf(event(id = 1, hijriMonth = 9, hijriDay = 27, eventType = "night", isHoliday = 0))
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(CalendarEvent.NavigateToHijriMonth(9, 1446))
        advanceUntilIdle()

        val state = viewModel.hijriState.value
        assertThat(state.currentHijriMonth).isEqualTo(9)
        val day27 = state.days.firstOrNull { it.hijriDate.day == 27 }
        assertThat(day27).isNotNull()
        val mapped = day27!!.events.single()
        assertThat(mapped.eventType).isEqualTo(IslamicEventType.NIGHT)
        assertThat(mapped.isNightOfPower).isTrue()
        assertThat(mapped.isHoliday).isFalse()
    }

    @Test
    fun `unknown event type maps to the HOLIDAY fallback`() = runTest {
        every { dao.getAllEvents() } returns flowOf(
            listOf(event(id = 2, hijriMonth = 1, hijriDay = 10, eventType = "totally-unknown"))
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(CalendarEvent.NavigateToHijriMonth(1, 1446))
        advanceUntilIdle()

        val day10 = viewModel.hijriState.value.days.firstOrNull { it.hijriDate.day == 10 }
        assertThat(day10).isNotNull()
        assertThat(day10!!.events.single().eventType).isEqualTo(IslamicEventType.HOLIDAY)
    }
}
