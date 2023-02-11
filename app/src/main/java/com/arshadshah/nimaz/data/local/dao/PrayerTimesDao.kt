package com.arshadshah.nimaz.data.local.dao

import androidx.room.*
import com.arshadshah.nimaz.data.local.models.LocalPrayerTimes


@Dao
interface PrayerTimesDao
{

	//get prayers with most recent timestamp
	@Query("SELECT * FROM prayer_times WHERE timestamp = (SELECT MAX(timestamp) FROM prayer_times)")
	suspend fun getPrayerTimes() : LocalPrayerTimes

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insert(prayerTimes : LocalPrayerTimes)

	//delete all prayer times
	@Query("DELETE FROM prayer_times")
	suspend fun deleteAllPrayerTimes()

	@Query("SELECT COUNT(*) FROM prayer_times")
	suspend fun count() : Int
}