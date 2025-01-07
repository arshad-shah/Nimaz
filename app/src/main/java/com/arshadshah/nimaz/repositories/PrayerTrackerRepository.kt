package com.arshadshah.nimaz.repositories

import com.arshadshah.nimaz.data.local.DataStore
import com.arshadshah.nimaz.data.local.models.LocalPrayersTracker
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrayerTrackerRepository @Inject constructor(
    private val dataStore: DataStore
) {
    private suspend fun trackerExistsForDate(date: LocalDate): Boolean {
        return dataStore.checkIfTrackerExists(date)
    }

    suspend fun getTrackerForDate(date: LocalDate): LocalPrayersTracker {
        if (!trackerExistsForDate(date)) {
            val tracker = LocalPrayersTracker(date)
            return saveTrackerForDate(tracker)
        }
        return dataStore.getTrackerForDate(date)
    }

    suspend fun saveTrackerForDate(tracker: LocalPrayersTracker): LocalPrayersTracker {
        dataStore.saveTracker(tracker)
        return getTrackerForDate(tracker.date)
    }

    suspend fun updateSpecificPrayer(
        date: LocalDate,
        prayerName: String,
        prayerDone: Boolean
    ): LocalPrayersTracker {
        dataStore.updateSpecificPrayer(date, prayerName, prayerDone)
        return getTrackerForDate(date)
    }

    suspend fun getPrayersForDate(date: LocalDate): Flow<LocalPrayersTracker> {
        // check if tracker exists
        if (!trackerExistsForDate(date)) {
            val tracker = LocalPrayersTracker(date)
            saveTrackerForDate(tracker)
        }
        return dataStore.getPrayersForDate(date)
    }

}