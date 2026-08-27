package com.arshadshah.nimaz.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.arshadshah.nimaz.data.local.database.entity.LocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Query("SELECT * FROM locations ORDER BY isFavorite DESC, name ASC")
    fun getAllLocations(): Flow<List<LocationEntity>>

    /**
     * The most recently used locations, newest first.
     *
     * [getAllLocations] orders by `isFavorite DESC, name ASC`, so taking the first five of it
     * gave an **alphabetical** "recent" row in which a newly saved location never appeared —
     * save Zurich after Amsterdam…Edinburgh and it is still those five. `updatedAt` has been on
     * this table since it was created, so recency needed no schema change, only a query that
     * asks for it.
     */
    @Query("SELECT * FROM locations ORDER BY updatedAt DESC LIMIT :limit")
    fun getRecentLocations(limit: Int): Flow<List<LocationEntity>>

    /**
     * A stored location at the same place, to three decimals — about 100 m, which is well
     * inside the precision a prayer-time calculation needs and far enough apart that two
     * genuinely different cities never collide.
     */
    @Query(
        "SELECT * FROM locations WHERE ROUND(latitude, 3) = ROUND(:latitude, 3) " +
            "AND ROUND(longitude, 3) = ROUND(:longitude, 3) LIMIT 1"
    )
    suspend fun findByCoordinates(latitude: Double, longitude: Double): LocationEntity?

    @Query("SELECT * FROM locations WHERE isCurrentLocation = 1 LIMIT 1")
    fun getCurrentLocation(): Flow<LocationEntity?>

    @Query("SELECT * FROM locations WHERE isCurrentLocation = 1 LIMIT 1")
    suspend fun getCurrentLocationSync(): LocationEntity?

    @Query("SELECT * FROM locations WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteLocations(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE isFavorite = 1")
    suspend fun getFavoriteLocationsSync(): List<LocationEntity>

    @Query("SELECT * FROM locations")
    suspend fun getAllLocationsSync(): List<LocationEntity>

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

    /**
     * Makes [location] the one and only current location, in a single transaction.
     *
     * `insertLocation` is `@Insert(onConflict = REPLACE)` on an **autogenerate** primary key, so
     * it always inserts and never replaces. Every selection therefore added a row — the table
     * grew without bound, and after the second selection several rows carried
     * `isCurrentLocation = 1`. Both current-location reads are `… WHERE isCurrentLocation = 1
     * LIMIT 1`, which with no ORDER BY returns the lowest rowid: **the oldest**. Select London
     * then Cairo and `getCurrentLocationSync()` answered London, so widgets and workers
     * disagreed with the screen.
     *
     * Clearing and setting must be one transaction, or a reader between the two statements sees
     * no current location at all.
     */
    @Transaction
    suspend fun saveCurrentLocation(location: LocationEntity, now: Long): Long {
        clearCurrentLocation()
        val existing = findByCoordinates(location.latitude, location.longitude)
        if (existing == null) {
            return insertLocation(location.copy(isCurrentLocation = true, updatedAt = now))
        }
        // Re-selecting somewhere already saved refreshes it rather than duplicating it — which
        // is also what makes it the newest entry in the recent row.
        updateLocation(
            existing.copy(
                name = location.name,
                country = location.country,
                city = location.city,
                timezone = location.timezone,
                isCurrentLocation = true,
                updatedAt = now,
            )
        )
        return existing.id
    }

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
