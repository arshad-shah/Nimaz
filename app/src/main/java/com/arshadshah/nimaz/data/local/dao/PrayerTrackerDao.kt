package com.arshadshah.nimaz.data.local.dao

import androidx.room.*
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_ASR
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_DHUHR
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_FAJR
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_ISHA
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_MAGHRIB
import com.arshadshah.nimaz.data.local.models.LocalPrayersTracker
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface PrayerTrackerDao {

    //get trtacker for a specific date
    @Query("SELECT * FROM PrayersTracker WHERE date = :date")
    suspend fun getTrackerForDate(date: LocalDate): LocalPrayersTracker

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
    suspend fun trackerExistsForDate(date: LocalDate): Boolean

    //get progress for a specific date
    @Query("SELECT progress FROM PrayersTracker WHERE date = :date")
    suspend fun getProgressForDate(date: LocalDate): Int

    suspend fun updateSpecificPrayer(date: LocalDate, prayerName: String, prayerDone: Boolean) {
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
    fun getPrayersForDate(date: LocalDate): Flow<LocalPrayersTracker>

    @Query("SELECT * FROM PrayersTracker WHERE date BETWEEN :startDate AND :endDate")
    fun getTrackersForMonth(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<LocalPrayersTracker>>

    // update menstruation status
    @Query("UPDATE PrayersTracker SET isMenstruating = :isMenstruating WHERE date = :date")
    suspend fun updateMenstruationStatus(date: LocalDate, isMenstruating: Boolean)

    @Query("SELECT isMenstruating FROM PrayersTracker WHERE date = :date")
    fun getMenstruatingState(date: LocalDate): Flow<Boolean>

    @Query("SELECT * FROM PrayersTracker WHERE date BETWEEN :startDate AND :endDate")
    fun getTrackersForWeek(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<LocalPrayersTracker>>
}