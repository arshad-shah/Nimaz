package com.arshadshah.nimaz.data.local.dao

import androidx.room.*
import com.arshadshah.nimaz.data.local.models.LocalFastTracker
import kotlinx.coroutines.flow.Flow

@Dao
interface FastTrackerDao {

    //get trtacker for a specific date
    @Query("SELECT * FROM FastTracker WHERE date = :date")
    suspend fun getFastTrackerForDate(date: String): LocalFastTracker

    //get trtacker for a specific date as a flow
    @Query("SELECT * FROM FastTracker WHERE date = :date")
    fun getFastTrackerForDateAsFlow(date: String): Flow<LocalFastTracker>

    //get all the trackers
    @Query("SELECT * FROM FastTracker")
    suspend fun getAllFastTrackers(): List<LocalFastTracker>

    //save a tracker
    @Insert(entity = LocalFastTracker::class, onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveFastTracker(tracker: LocalFastTracker)

    //update a tracker
    @Update(entity = LocalFastTracker::class, onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateFastTracker(tracker: LocalFastTracker)

    //delete a tracker
    @Delete(entity = LocalFastTracker::class)
    suspend fun deleteFastTracker(tracker: LocalFastTracker)

    //delete all trackers
    @Query("DELETE FROM FastTracker")
    suspend fun deleteFastAllTrackers()

    @Query("SELECT EXISTS(SELECT * FROM FastTracker WHERE date = :date)")
    suspend fun fastTrackerExistsForDate(date: String): Boolean

    // is fasting for a specific date
    @Query("SELECT isFasting FROM FastTracker WHERE date = :date")
    fun isFastingForDate(date: String): Flow<Boolean>
}