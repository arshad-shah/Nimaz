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
	@Insert(entity = LocalPrayersTracker::class , onConflict = OnConflictStrategy.REPLACE)
	suspend fun saveTracker(tracker : LocalPrayersTracker)

	//update a tracker
	@Update(entity = LocalPrayersTracker::class , onConflict = OnConflictStrategy.REPLACE)
	suspend fun updateTracker(tracker : LocalPrayersTracker)

	//delete a tracker
	@Delete(entity = LocalPrayersTracker::class)
	suspend fun deleteTracker(tracker : LocalPrayersTracker)

	//delete all trackers
	@Query("DELETE FROM PrayersTracker")
	suspend fun deleteAllTrackers()

	@Query("SELECT EXISTS(SELECT * FROM PrayersTracker WHERE date = :date)")
	suspend fun trackerExistsForDate(date : String) : Boolean

	//find out which dates have trackers
	@Query("SELECT date FROM PrayersTracker")
	suspend fun getDatesWithTrackers() : List<String>

	//get progress for a specific date
	@Query("SELECT progress FROM PrayersTracker WHERE date = :date")
	suspend fun getProgressForDate(date : String) : Int
}