package com.arshadshah.nimaz.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.arshadshah.nimaz.data.local.models.LocalPrayerTimes


@Dao
interface PrayerTimesDao {

    //get prayers with most recent timestamp
    @Query("SELECT * FROM prayer_times")
    suspend fun getPrayerTimes(): LocalPrayerTimes

    //get prayers for a specific date
    @Query("SELECT * FROM prayer_times WHERE date = :date")
    suspend fun getPrayerTimesForADate(date: String): LocalPrayerTimes

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(prayerTimes: LocalPrayerTimes)

    //delete all prayer times
    @Query("DELETE FROM prayer_times")
    suspend fun deleteAllPrayerTimes()

    @Query("SELECT COUNT(*) FROM prayer_times")
    suspend fun count(): Int
}