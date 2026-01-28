package com.arshadshah.nimaz.domain.repository

import com.arshadshah.nimaz.domain.model.Location
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerRecord
import com.arshadshah.nimaz.domain.model.PrayerStats
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.PrayerTimes
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface PrayerRepository {
    // Prayer times calculation
    fun getPrayerTimesForDate(date: LocalDate, location: Location): PrayerTimes
    fun getPrayerTimesForRange(startDate: LocalDate, endDate: LocalDate, location: Location): List<PrayerTimes>

    // Prayer records
    fun getPrayerRecordsForDate(date: Long): Flow<List<PrayerRecord>>
    fun getPrayerRecordsInRange(startDate: Long, endDate: Long): Flow<List<PrayerRecord>>
    suspend fun getPrayerRecord(date: Long, prayerName: PrayerName): PrayerRecord?
    fun getPrayerRecordsByStatus(status: PrayerStatus): Flow<List<PrayerRecord>>
    fun getMissedPrayersRequiringQada(): Flow<List<PrayerRecord>>

    // Prayer record operations
    suspend fun insertPrayerRecord(record: PrayerRecord)
    suspend fun insertPrayerRecords(records: List<PrayerRecord>)
    suspend fun updatePrayerStatus(
        date: Long,
        prayerName: PrayerName,
        status: PrayerStatus,
        prayedAt: Long?,
        isJamaah: Boolean
    )

    // Statistics
    suspend fun getPrayerStats(startDate: Long, endDate: Long): PrayerStats
    suspend fun getCurrentStreak(currentDate: Long): Int
    suspend fun getLongestStreak(): Int
    suspend fun markPastPrayersAsMissed(): Int

    // Location operations
    fun getAllLocations(): Flow<List<Location>>
    fun getCurrentLocation(): Flow<Location?>
    suspend fun getCurrentLocationSync(): Location?
    fun getFavoriteLocations(): Flow<List<Location>>
    suspend fun getLocationById(id: Long): Location?
    fun searchLocations(query: String): Flow<List<Location>>
    suspend fun insertLocation(location: Location): Long
    suspend fun updateLocation(location: Location)
    suspend fun deleteLocation(location: Location)
    suspend fun setCurrentLocation(id: Long)
    suspend fun toggleFavorite(id: Long)
}
