package com.arshadshah.nimaz.data.local.dao

import androidx.room.*
import com.arshadshah.nimaz.data.local.models.LocalPrayersTracker

@Dao
interface PrayerTrackerDao
{
	//get trtacker for a specific date
	@Query("SELECT * FROM PrayersTracker WHERE date = :date")
	suspend fun getTrackerForDate(date : String) : LocalPrayersTracker

	//get all the trackers
	@Query("SELECT * FROM PrayersTracker")
	suspend fun getAllTrackers() : List<LocalPrayersTracker>

	//save a tracker
	@Insert(entity = LocalPrayersTracker::class, onConflict = OnConflictStrategy.REPLACE)
	suspend fun saveTracker(tracker : LocalPrayersTracker)

	//update a tracker
	@Update(entity = LocalPrayersTracker::class, onConflict = OnConflictStrategy.REPLACE)
	suspend fun updateTracker(tracker : LocalPrayersTracker)

	//delete a tracker
	@Delete(entity = LocalPrayersTracker::class)
	suspend fun deleteTracker(tracker : LocalPrayersTracker)

	//delete all trackers
	@Query("DELETE FROM PrayersTracker")
	suspend fun deleteAllTrackers()
}