package com.arshadshah.nimaz.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.arshadshah.nimaz.data.local.database.entity.IslamicEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IslamicEventDao {
    @Query("SELECT * FROM islamic_events ORDER BY hijri_month, hijri_day")
    fun getAllEvents(): Flow<List<IslamicEventEntity>>

    @Query("SELECT * FROM islamic_events WHERE hijri_month = :month ORDER BY hijri_day")
    fun getEventsByMonth(month: Int): Flow<List<IslamicEventEntity>>

    @Query("SELECT * FROM islamic_events WHERE hijri_month = :month AND hijri_day = :day")
    fun getEventsForDate(month: Int, day: Int): Flow<List<IslamicEventEntity>>

    @Query("SELECT * FROM islamic_events WHERE is_holiday = 1 ORDER BY hijri_month, hijri_day")
    fun getHolidays(): Flow<List<IslamicEventEntity>>

    @Query("SELECT * FROM islamic_events WHERE event_type = :eventType ORDER BY hijri_month, hijri_day")
    fun getEventsByType(eventType: String): Flow<List<IslamicEventEntity>>

    @Query("SELECT * FROM islamic_events WHERE id = :id")
    suspend fun getEventById(id: Int): IslamicEventEntity?

    @Query("SELECT * FROM islamic_events WHERE name_english LIKE '%' || :query || '%' OR name_arabic LIKE '%' || :query || '%'")
    fun searchEvents(query: String): Flow<List<IslamicEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<IslamicEventEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: IslamicEventEntity)

    @Update
    suspend fun updateEvent(event: IslamicEventEntity)
}
