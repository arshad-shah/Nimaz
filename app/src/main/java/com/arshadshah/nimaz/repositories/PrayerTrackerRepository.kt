package com.arshadshah.nimaz.repositories

import com.arshadshah.nimaz.data.local.DataStore
import com.arshadshah.nimaz.data.local.models.LocalPrayersTracker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrayerTrackerRepository @Inject constructor(
    private val dataStore: DataStore
) {
    // Cache for monthly data to reduce database hits
    private var cachedMonth: YearMonth? = null
    private var monthlyTrackers: List<LocalPrayersTracker>? = null

    suspend fun getTrackerForDate(date: LocalDate): LocalPrayersTracker {
        if (!dataStore.checkIfTrackerExists(date)) {
            val tracker = LocalPrayersTracker(date)
            dataStore.saveTracker(tracker)
            return tracker
        }
        return dataStore.getTrackerForDate(date)
    }

    suspend fun observePrayersForDate(date: LocalDate): Flow<LocalPrayersTracker> {
        if (!dataStore.checkIfTrackerExists(date)) {
            val tracker = LocalPrayersTracker(date)
            dataStore.saveTracker(tracker)
            return dataStore.getPrayersForDate(date)
                .distinctUntilChanged()
        }
        return dataStore.getPrayersForDate(date)
            .distinctUntilChanged()
    }

    fun observeTrackersForMonth(yearMonth: YearMonth): Flow<List<LocalPrayersTracker>> {
        val firstDay = yearMonth.atDay(1)
        val lastDay = yearMonth.atEndOfMonth()

        return dataStore.getTrackersForMonth(firstDay, lastDay)
            .distinctUntilChanged()
            .map { trackers ->
                trackers.ifEmpty {
                    // Create trackers for the entire month if none exist
                    val newTrackers = (1..yearMonth.lengthOfMonth()).map { day ->
                        LocalPrayersTracker(yearMonth.atDay(day))
                    }
                    newTrackers.forEach { dataStore.saveTracker(it) }
                    newTrackers
                }
            }
    }

    fun observeTrackersForWeek(date: LocalDate): Flow<List<LocalPrayersTracker>> {
        val startDate = date.with(DayOfWeek.MONDAY)
        val endDate = date.with(DayOfWeek.SUNDAY)

        return dataStore.getTrackersForWeek(startDate, endDate)
            .distinctUntilChanged()
            .map { trackers ->
                // Check for missing dates and fill them in
                val completeWeek = (0L..ChronoUnit.DAYS.between(startDate, endDate))
                    .map { days -> startDate.plusDays(days) }
                    .map { date ->
                        trackers.find { it.date == date } ?: LocalPrayersTracker(date = date)
                    }

                // If the week was completely empty, save the new trackers
                if (trackers.isEmpty()) {
                    completeWeek.forEach { dataStore.saveTracker(it) }
                }

                completeWeek
            }
    }

    suspend fun updateSpecificPrayer(date: LocalDate, prayerName: String, completed: Boolean) {
        dataStore.updateSpecificPrayer(date, prayerName, completed)
    }

    suspend fun updateMenstruationStatus(date: LocalDate, isMenstruating: Boolean) {
        dataStore.updateIsMenstruating(date, isMenstruating)
    }

    fun observeMenstruationStatus(date: LocalDate): Flow<Boolean> {
        return dataStore.getMenstruatingState(date)
            .distinctUntilChanged()
    }

    // Clear cache when no longer needed
    fun clearCache() {
        cachedMonth = null
        monthlyTrackers = null
    }
}