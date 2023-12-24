package com.arshadshah.nimaz.data.remote.repositories

import com.arshadshah.nimaz.data.local.models.LocalPrayersTracker
import com.arshadshah.nimaz.data.remote.models.PrayerTracker
import com.arshadshah.nimaz.utils.LocalDataStore
import kotlinx.coroutines.flow.Flow

object PrayerTrackerRepository {

    suspend fun trackerExistsForDate(date: String): Boolean {
        val dataStore = LocalDataStore.getDataStore()
        return dataStore.checkIfTrackerExists(date)
    }

    suspend fun getTrackerForDate(date: String): PrayerTracker {
        val dataStore = LocalDataStore.getDataStore()
        if (!trackerExistsForDate(date)) {
            val tracker = PrayerTracker(date)
            return saveTrackerForDate(tracker)
        }
        return dataStore.getTrackerForDate(date)
    }

    suspend fun saveTrackerForDate(tracker: PrayerTracker): PrayerTracker {
        val dataStore = LocalDataStore.getDataStore()
        dataStore.saveTracker(tracker)
        return getTrackerForDate(tracker.date)
    }

    suspend fun updateTracker(tracker: PrayerTracker): PrayerTracker {
        val dataStore = LocalDataStore.getDataStore()
        dataStore.updateTracker(tracker)
        return getTrackerForDate(tracker.date)
    }

    suspend fun updateSpecificPrayer(
        date: String,
        prayerName: String,
        prayerDone: Boolean
    ): PrayerTracker {
        val dataStore = LocalDataStore.getDataStore()
        dataStore.updateSpecificPrayer(date, prayerName, prayerDone)
        return getTrackerForDate(date)
    }

    suspend fun getPrayersForDate(date: String): Flow<LocalPrayersTracker> {
        val dataStore = LocalDataStore.getDataStore()
        // check if tracker exists
        if (!trackerExistsForDate(date)) {
            val tracker = PrayerTracker(date)
            saveTrackerForDate(tracker)
        }
        return dataStore.getPrayersForDate(date)
    }

    suspend fun getAllTrackers(): List<PrayerTracker> {
        val dataStore = LocalDataStore.getDataStore()
        return dataStore.getAllTrackers()
    }
}