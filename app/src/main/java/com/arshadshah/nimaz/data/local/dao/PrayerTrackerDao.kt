package com.arshadshah.nimaz.data.local.dao

import androidx.room.*
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_ASR
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_DHUHR
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_FAJR
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_ISHA
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_MAGHRIB
import com.arshadshah.nimaz.data.local.models.LocalPrayersTracker
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerTrackerDao {

    //get trtacker for a specific date
    @Query("SELECT * FROM PrayersTracker WHERE date = :date")
    suspend fun getTrackerForDate(date: String): LocalPrayersTracker

    //get all the trackers
    @Query("SELECT * FROM PrayersTracker")
    suspend fun getAllTrackers(): List<LocalPrayersTracker>

    //save a tracker
    @Insert(entity = LocalPrayersTracker::class, onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTracker(tracker: LocalPrayersTracker)

    //update a tracker
    @Update(entity = LocalPrayersTracker::class, onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateTracker(tracker: LocalPrayersTracker)

    //delete a tracker
    @Delete(entity = LocalPrayersTracker::class)
    suspend fun deleteTracker(tracker: LocalPrayersTracker)

    //delete all trackers
    @Query("DELETE FROM PrayersTracker")
    suspend fun deleteAllTrackers()

    @Query("SELECT EXISTS(SELECT * FROM PrayersTracker WHERE date = :date)")
    suspend fun trackerExistsForDate(date: String): Boolean

    //find out which dates have trackers
    @Query("SELECT date FROM PrayersTracker")
    suspend fun getDatesWithTrackers(): List<String>

    //get progress for a specific date
    @Query("SELECT progress FROM PrayersTracker WHERE date = :date")
    suspend fun getProgressForDate(date: String): Int

    suspend fun updateSpecificPrayer(date: String, prayerName: String, prayerDone: Boolean) {
        val dailyPrayer = getTrackerForDate(date)

        val updatedPrayer = when (prayerName) {
            PRAYER_NAME_FAJR -> dailyPrayer.copy(fajr = prayerDone)
            PRAYER_NAME_DHUHR -> dailyPrayer.copy(dhuhr = prayerDone)
            PRAYER_NAME_ASR -> dailyPrayer.copy(asr = prayerDone)
            PRAYER_NAME_MAGHRIB -> dailyPrayer.copy(maghrib = prayerDone)
            PRAYER_NAME_ISHA -> dailyPrayer.copy(isha = prayerDone)
            else -> return
        }

        val completedPrayers = listOf(
            updatedPrayer.fajr,
            updatedPrayer.dhuhr,
            updatedPrayer.asr,
            updatedPrayer.maghrib,
            updatedPrayer.isha
        ).count { it }

        val newProgress = (completedPrayers / 5.0 * 100).toInt()
        updateTracker(updatedPrayer.copy(progress = newProgress))
    }

    @Query("SELECT * FROM PrayersTracker WHERE date = :date")
    fun getPrayersForDate(date: String): Flow<LocalPrayersTracker>

}