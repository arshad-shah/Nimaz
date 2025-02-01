package com.arshadshah.nimaz.repositories

import com.arshadshah.nimaz.data.local.DataStore
import com.arshadshah.nimaz.data.local.models.LocalFastTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FastTrackerRepository @Inject constructor(
    private val dataStore: DataStore<Any?>
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val mutex = Mutex()

    // Cache implementation using StateFlow
    private data class CacheEntry(
        val yearMonth: YearMonth,
        val trackers: List<LocalFastTracker>
    )

    private val _monthlyCache = MutableStateFlow<CacheEntry?>(null)
    private val monthlyCache = _monthlyCache.asStateFlow()

    init {
        // Initialize cache with current month's data
        scope.launch {
            val currentMonth = YearMonth.now()
            prefetchMonth(currentMonth)
        }
    }

    private suspend fun prefetchMonth(yearMonth: YearMonth) {
        val firstDay = yearMonth.atDay(1)
        val lastDay = yearMonth.atEndOfMonth()

        val trackers = dataStore.getFastTrackersForMonth(firstDay, lastDay)
            .first()
            .ifEmpty {
                (1..yearMonth.lengthOfMonth()).map { day ->
                    LocalFastTracker(
                        date = yearMonth.atDay(day),
                        isFasting = false
                    )
                }.onEach { dataStore.saveFastTracker(it) }
            }

        _monthlyCache.value = CacheEntry(yearMonth, trackers)
    }

    suspend fun getTrackerForDate(date: LocalDate): LocalFastTracker = mutex.withLock {
        val yearMonth = YearMonth.from(date)
        val cached = monthlyCache.value

        if (cached != null) {
            if (cached.yearMonth == yearMonth) {
                cached.trackers.find { it.date == date }
            } else {
                null
            }?.let { return it }
        }

        if (!dataStore.fastTrackerExistsForDate(date)) {
            val tracker = LocalFastTracker(date = date, isFasting = false)
            dataStore.saveFastTracker(tracker)
            updateCache(tracker)
            return tracker
        }

        return dataStore.getFastTrackerForDate(date).also {
            updateCache(it)
        }
    }

    fun observeFastingForMonth(yearMonth: YearMonth): Flow<List<LocalFastTracker>> {
        val firstDay = yearMonth.atDay(1)
        val lastDay = yearMonth.atEndOfMonth()

        return flow {
            // First emit from cache if available
            monthlyCache.value?.let { cached ->
                if (cached.yearMonth == yearMonth) {
                    emit(cached.trackers)
                }
            }

            // Then observe database changes
            dataStore.getFastTrackersForMonth(firstDay, lastDay)
                .distinctUntilChanged()
                .collect { trackers ->
                    if (trackers.isEmpty()) {
                        // Create trackers for each day in the month if none exist
                        val newTrackers = (1..yearMonth.lengthOfMonth()).map { day ->
                            LocalFastTracker(
                                date = yearMonth.atDay(day),
                                isFasting = false
                            )
                        }
                        newTrackers.forEach { dataStore.saveFastTracker(it) }
                        emit(newTrackers)
                        updateCache(CacheEntry(yearMonth, newTrackers))
                    } else {
                        emit(trackers)
                        updateCache(CacheEntry(yearMonth, trackers))
                    }
                }
        }.distinctUntilChanged()
    }

    suspend fun updateFasting(date: LocalDate, isFasting: Boolean) = withContext(Dispatchers.IO) {
        try {
            val tracker = getTrackerForDate(date)
            val updatedTracker = tracker.copy(isFasting = isFasting)
            dataStore.updateFastTracker(updatedTracker)

            // Update the cache with the new tracker
            val yearMonth = YearMonth.from(date)
            val currentCache = monthlyCache.value
            if (currentCache?.yearMonth == yearMonth) {
                val updatedTrackers = currentCache?.trackers?.map {
                    if (it.date == date) updatedTracker else it
                }
                _monthlyCache.value = updatedTrackers?.let { CacheEntry(yearMonth, it) }
            }
        } catch (e: Exception) {
            throw Exception("Failed to update fasting status: ${e.message}")
        }
    }


    suspend fun isFastingForDate(date: LocalDate): Boolean {
        return try {
            getTrackerForDate(date).isFasting
        } catch (e: Exception) {
            false
        }
    }

    fun observeFastingForDate(date: LocalDate): Flow<Boolean> {
        return dataStore.isFastingForDate(date)
            .distinctUntilChanged()
            .onEach { isFasting ->
                // Update cache when observing changes
                val currentTracker = getTrackerForDate(date)
                if (currentTracker.isFasting != isFasting) {
                    updateCache(currentTracker.copy(isFasting = isFasting))
                }
            }
    }

    suspend fun updateBatchFasting(dates: List<LocalDate>, isFasting: Boolean) = mutex.withLock {
        dates.forEach { date ->
            val tracker = getTrackerForDate(date)
            val updatedTracker = tracker.copy(isFasting = isFasting)
            dataStore.updateFastTracker(updatedTracker)
            updateCache(updatedTracker)
        }
    }

    private suspend fun updateCache(tracker: LocalFastTracker) {
        val yearMonth = YearMonth.from(tracker.date)
        val currentCache = monthlyCache.value

        if (currentCache?.yearMonth == yearMonth) {
            val updatedTrackers = currentCache?.trackers?.map {
                if (it.date == tracker.date) tracker else it
            }
            _monthlyCache.value = updatedTrackers?.let { CacheEntry(yearMonth, it) }
        }
    }

    private suspend fun updateCache(entry: CacheEntry) {
        _monthlyCache.value = entry
    }

    fun isValidForFastingTracking(date: LocalDate): Boolean {
        return !date.isAfter(LocalDate.now())
    }

    fun clearCache() {
        _monthlyCache.value = null
    }
}