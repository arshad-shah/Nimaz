package com.arshadshah.nimaz.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.arshadshah.nimaz.data.local.database.entity.LocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Query("SELECT * FROM locations ORDER BY isFavorite DESC, name ASC")
    fun getAllLocations(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE isCurrentLocation = 1 LIMIT 1")
    fun getCurrentLocation(): Flow<LocationEntity?>

    @Query("SELECT * FROM locations WHERE isCurrentLocation = 1 LIMIT 1")
    suspend fun getCurrentLocationSync(): LocationEntity?

    @Query("SELECT * FROM locations WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteLocations(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE id = :id")
    suspend fun getLocationById(id: Long): LocationEntity?

    @Query("SELECT * FROM locations WHERE name LIKE '%' || :query || '%' OR city LIKE '%' || :query || '%' OR country LIKE '%' || :query || '%'")
    fun searchLocations(query: String): Flow<List<LocationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationEntity): Long

    @Update
    suspend fun updateLocation(location: LocationEntity)

    @Delete
    suspend fun deleteLocation(location: LocationEntity)

    @Query("UPDATE locations SET isCurrentLocation = 0 WHERE isCurrentLocation = 1")
    suspend fun clearCurrentLocation()

    @Query("UPDATE locations SET isCurrentLocation = 1 WHERE id = :id")
    suspend fun setCurrentLocation(id: Long)

    @Query("UPDATE locations SET isFavorite = NOT isFavorite, updatedAt = :timestamp WHERE id = :id")
    suspend fun toggleFavorite(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE locations SET calculationMethod = :method, asrCalculation = :asrMethod, fajrAngle = :fajrAngle, ishaAngle = :ishaAngle, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateCalculationSettings(
        id: Long,
        method: String,
        asrMethod: String,
        fajrAngle: Double?,
        ishaAngle: Double?,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM locations")
    suspend fun deleteAllUserData()
}
