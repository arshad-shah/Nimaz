package com.arshadshah.nimaz.viewModel

import com.arshadshah.nimaz.data.local.DataStore
import com.arshadshah.nimaz.data.local.models.LocalFastTracker
import com.arshadshah.nimaz.data.local.models.LocalPrayersTracker
import com.arshadshah.nimaz.repositories.PrayerTrackerRepository
import com.arshadshah.nimaz.ui.components.calender.ImportanceLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.YearMonth

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class CalendarViewModelTest {

    @get:Rule
    val mainDispatcherRule = TestDispatcherRule()

    @Mock
    private lateinit var dataStore: DataStore

    @Mock
    private lateinit var prayerTrackerRepository: PrayerTrackerRepository

    private lateinit var viewModel: CalendarViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = CalendarViewModel(dataStore, prayerTrackerRepository)
    }

    @Ignore("Test is very flaky")
    @Test
    fun `init should load current month data`() = runTest {
        // Given
        val currentDate = LocalDate.now()
        val currentMonth = YearMonth.from(currentDate)
        val emptyPrayers = emptyList<com.arshadshah.nimaz.data.local.models.LocalPrayersTracker>()
        val emptyFasts = emptyList<com.arshadshah.nimaz.data.local.models.LocalFastTracker>()
        val startDate = currentMonth.atDay(1)
        val endDate = currentMonth.atEndOfMonth()

        whenever(dataStore.getTrackersForMonth(startDate, endDate)).thenReturn(flowOf(emptyPrayers))
        whenever(dataStore.getFastTrackersForMonth(startDate, endDate)).thenReturn(flowOf(emptyFasts))
        whenever(dataStore.getMenstruatingState(any())).thenReturn(flowOf(false))

        // When


        // Then
        advanceUntilIdle()
        verify(dataStore, times(1)).getTrackersForMonth(startDate, endDate)
        verify(dataStore, times(1)).getFastTrackersForMonth(startDate, endDate)
    }

    @Ignore("Test is very flaky")
    @Test
    fun `updatePrayer should update prayer tracker and refresh states`() = runTest {
        // Given
        val date = LocalDate.now()
        val prayerName = "Fajr"
        val completed = true
        val updatedTracker = LocalPrayersTracker(date)

        whenever(prayerTrackerRepository.trackerExistsForDate(date)).thenReturn(false)
        whenever(prayerTrackerRepository.saveTrackerForDate(any())).thenReturn(updatedTracker)
        whenever(prayerTrackerRepository.updateSpecificPrayer(date, prayerName, completed))
            .thenReturn(updatedTracker)

        // When
        viewModel.updatePrayer(date, prayerName, completed)
        advanceUntilIdle()

        // Then
        verify(prayerTrackerRepository).trackerExistsForDate(date)
        verify(prayerTrackerRepository).saveTrackerForDate(any())
        verify(prayerTrackerRepository).updateSpecificPrayer(date, prayerName, completed)
        assert(!viewModel.prayerLoading.value)
    }

    @Test
    fun `onDateSelected should update current date and load new month if needed`() = runTest {
        // Given
        val newDate = LocalDate.now().plusMonths(1)
        val emptyPrayers = emptyList<LocalPrayersTracker>()
        val emptyFasts = emptyList<LocalFastTracker>()

        whenever(dataStore.getTrackersForMonth(any(), any())).thenReturn(flowOf(emptyPrayers))
        whenever(dataStore.getFastTrackersForMonth(any(), any())).thenReturn(flowOf(emptyFasts))
        whenever(dataStore.getMenstruatingState(any())).thenReturn(flowOf(false))

        // When
        viewModel.onDateSelected(newDate)
        advanceUntilIdle()

        // Then
        verify(dataStore).getTrackersForMonth(
            eq(YearMonth.from(newDate).atDay(1)),
            eq(YearMonth.from(newDate).atEndOfMonth())
        )
    }
}

// Test rule for handling coroutines in tests
class TestDispatcherRule : TestWatcher() {
    private val testDispatcher = StandardTestDispatcher()

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}