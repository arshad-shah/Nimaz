package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.Location
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerRecord
import com.arshadshah.nimaz.domain.model.PrayerStats
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.PrayerTimes
import com.arshadshah.nimaz.domain.repository.PrayerRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

data class PrayerUseCases(
    val getPrayerRecordsForDate: GetPrayerRecordsForDateUseCase,
    val getPrayerRecordsInRange: GetPrayerRecordsInRangeUseCase,
    val getTodayPrayerRecords: GetTodayPrayerRecordsUseCase,
    val updatePrayerStatus: UpdatePrayerStatusUseCase,
    val getPrayerTimesForDate: GetPrayerTimesForDateUseCase,
    val getCurrentStreak: GetCurrentStreakUseCase,
    val getLongestStreak: GetLongestStreakUseCase,
    val getMissedPrayersRequiringQada: GetMissedPrayersRequiringQadaUseCase,
    val getPrayerStats: GetPrayerStatsUseCase,
    val getCurrentLocation: GetCurrentLocationUseCase,
    val getAllLocations: GetAllLocationsUseCase,
    val getFavoriteLocations: GetFavoriteLocationsUseCase,
    val insertLocation: InsertLocationUseCase,
    val deleteLocation: DeleteLocationUseCase,
    val setCurrentLocation: SetCurrentLocationUseCase,
    val toggleFavorite: ToggleLocationFavoriteUseCase
)

class GetPrayerRecordsForDateUseCase @Inject constructor(private val repository: PrayerRepository) {
    operator fun invoke(date: Long): Flow<List<PrayerRecord>> =
        repository.getPrayerRecordsForDate(date)
}

class GetPrayerRecordsInRangeUseCase @Inject constructor(private val repository: PrayerRepository) {
    operator fun invoke(startDate: Long, endDate: Long): Flow<List<PrayerRecord>> =
        repository.getPrayerRecordsInRange(startDate, endDate)
}

class GetTodayPrayerRecordsUseCase @Inject constructor(private val repository: PrayerRepository) {
    operator fun invoke(): Flow<Map<PrayerName, PrayerStatus>> =
        repository.getTodayPrayerRecords()
}

class UpdatePrayerStatusUseCase @Inject constructor(private val repository: PrayerRepository) {
    suspend operator fun invoke(
        date: Long,
        prayerName: PrayerName,
        status: PrayerStatus,
        prayedAt: Long?,
        isJamaah: Boolean
    ) = repository.updatePrayerStatus(date, prayerName, status, prayedAt, isJamaah)
}

class GetPrayerTimesForDateUseCase @Inject constructor(private val repository: PrayerRepository) {
    operator fun invoke(date: LocalDate, location: Location): PrayerTimes =
        repository.getPrayerTimesForDate(date, location)
}

class GetCurrentStreakUseCase @Inject constructor(private val repository: PrayerRepository) {
    suspend operator fun invoke(currentDate: Long): Int = repository.getCurrentStreak(currentDate)
}

class GetLongestStreakUseCase @Inject constructor(private val repository: PrayerRepository) {
    suspend operator fun invoke(): Int = repository.getLongestStreak()
}

class GetMissedPrayersRequiringQadaUseCase @Inject constructor(private val repository: PrayerRepository) {
    operator fun invoke(): Flow<List<PrayerRecord>> = repository.getMissedPrayersRequiringQada()
}

class GetPrayerStatsUseCase @Inject constructor(private val repository: PrayerRepository) {
    suspend operator fun invoke(startDate: Long, endDate: Long): PrayerStats =
        repository.getPrayerStats(startDate, endDate)
}

class GetCurrentLocationUseCase @Inject constructor(private val repository: PrayerRepository) {
    operator fun invoke(): Flow<Location?> = repository.getCurrentLocation()
}

class GetAllLocationsUseCase @Inject constructor(private val repository: PrayerRepository) {
    operator fun invoke(): Flow<List<Location>> = repository.getAllLocations()
}

class GetFavoriteLocationsUseCase @Inject constructor(private val repository: PrayerRepository) {
    operator fun invoke(): Flow<List<Location>> = repository.getFavoriteLocations()
}

class InsertLocationUseCase @Inject constructor(private val repository: PrayerRepository) {
    suspend operator fun invoke(location: Location): Long = repository.insertLocation(location)
}

class DeleteLocationUseCase @Inject constructor(private val repository: PrayerRepository) {
    suspend operator fun invoke(location: Location) = repository.deleteLocation(location)
}

class SetCurrentLocationUseCase @Inject constructor(private val repository: PrayerRepository) {
    suspend operator fun invoke(id: Long) = repository.setCurrentLocation(id)
}

class ToggleLocationFavoriteUseCase @Inject constructor(private val repository: PrayerRepository) {
    suspend operator fun invoke(id: Long) = repository.toggleFavorite(id)
}
