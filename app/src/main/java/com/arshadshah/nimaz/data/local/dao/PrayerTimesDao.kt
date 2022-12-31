package com.arshadshah.nimaz.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.arshadshah.nimaz.data.local.models.LocalPrayerTimes


@Dao
interface PrayerTimesDao {
    @Query("SELECT * FROM prayer_times WHERE timeStamp = (SELECT MAX(timeStamp) FROM prayer_times)")
    suspend fun getPrayerTimes(): LocalPrayerTimes

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(prayerTimes: LocalPrayerTimes)

    @Query("SELECT COUNT(*) FROM prayer_times")
    suspend fun count(): Int
}