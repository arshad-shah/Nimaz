package com.arshadshah.nimaz.repositories

import com.arshadshah.nimaz.data.local.models.LocalPrayersTracker
import com.arshadshah.nimaz.utils.LocalDataStore
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

object PrayerTrackerRepository {

    private suspend fun trackerExistsForDate(date: LocalDate): Boolean {
        val dataStore = LocalDataStore.getDataStore()
        return dataStore.checkIfTrackerExists(date)
    }

    suspend fun getTrackerForDate(date: LocalDate): LocalPrayersTracker {
        val dataStore = LocalDataStore.getDataStore()
        if (!trackerExistsForDate(date)) {
            val tracker = LocalPrayersTracker(date)
            return saveTrackerForDate(tracker)
        }
        return dataStore.getTrackerForDate(date)
    }

    suspend fun saveTrackerForDate(tracker: LocalPrayersTracker): LocalPrayersTracker {
        val dataStore = LocalDataStore.getDataStore()
        dataStore.saveTracker(tracker)
        return getTrackerForDate(tracker.date)
    }

    suspend fun updateTracker(tracker: LocalPrayersTracker): LocalPrayersTracker {
        val dataStore = LocalDataStore.getDataStore()
        dataStore.updateTracker(tracker)
        return getTrackerForDate(tracker.date)
    }

    suspend fun updateSpecificPrayer(
        date: LocalDate,
        prayerName: String,
        prayerDone: Boolean
    ): LocalPrayersTracker {
        val dataStore = LocalDataStore.getDataStore()
        dataStore.updateSpecificPrayer(date, prayerName, prayerDone)
        return getTrackerForDate(date)
    }

    suspend fun getPrayersForDate(date: LocalDate): Flow<LocalPrayersTracker> {
        val dataStore = LocalDataStore.getDataStore()
        // check if tracker exists
        if (!trackerExistsForDate(date)) {
            val tracker = LocalPrayersTracker(date)
            saveTrackerForDate(tracker)
        }
        return dataStore.getPrayersForDate(date)
    }

    suspend fun getAllTrackers(): List<LocalPrayersTracker> {
        val dataStore = LocalDataStore.getDataStore()
        return dataStore.getAllTrackers()
    }
}