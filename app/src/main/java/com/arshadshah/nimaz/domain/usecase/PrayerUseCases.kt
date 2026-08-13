package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.Location
import com.arshadshah.nimaz.domain.model.PrayerCalculationSettings
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerRecord
import com.arshadshah.nimaz.domain.model.PrayerStats
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.PrayerTime
import com.arshadshah.nimaz.domain.model.PrayerTimes
import com.arshadshah.nimaz.domain.model.SunnahNightTimes
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
    val observeCalculationSettings: ObservePrayerCalculationSettingsUseCase,
    val getDaySchedule: GetDayPrayerScheduleUseCase,
    val getSunnahNightTimes: GetSunnahNightTimesUseCase,
    val getCurrentStreak: GetCurrentStreakUseCase,
    val getLongestStreak: GetLongestStreakUseCase,
    val getMissedPrayersRequiringQada: GetMissedPrayersRequiringQadaUseCase,
    val markUnrecordedAsMissed: MarkUnrecordedAsMissedUseCase,
    val getPrayerStats: GetPrayerStatsUseCase,
    val getCurrentLocation: GetCurrentLocationUseCase,
    val getAllLocations: GetAllLocationsUseCase,
    val getFavoriteLocations: GetFavoriteLocationsUseCase,
    val insertLocation: InsertLocationUseCase,
    val deleteLocation: DeleteLocationUseCase,
    val setCurrentLocation: SetCurrentLocationUseCase,
    val getRecentLocations: GetRecentLocationsUseCase,
    val saveCurrentLocation: SaveCurrentLocationUseCase,
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

/**
 * The user's prayer-time calculation settings, re-emitted whenever any of them changes.
 *
 * Five ViewModels injected the concrete `core/util/PrayerTimeCalculator` and assembled these
 * themselves. A concrete class with no interface cannot be faked, so every prayer-time path in
 * those five was untestable without the real astronomical library — which is why a bug as plain
 * as Fast Tracker ignoring the calculation method could ship and survive.
 */
class ObservePrayerCalculationSettingsUseCase @Inject constructor(
    private val repository: PrayerRepository,
) {
    operator fun invoke(): Flow<PrayerCalculationSettings> = repository.observeCalculationSettings()
}

/**
 * One day's prayer times.
 *
 * Two shapes deliberately. Pass a [PrayerCalculationSettings] when you already hold one and are
 * computing many days (the month view): the call is pure and synchronous, so thirty days is one
 * pass rather than thirty preference reads. Omit it and the user's current settings are read for
 * you — which is the shape `FastingViewModel` needed and did not have, and so took the
 * calculator's four defaults instead.
 */
class GetDayPrayerScheduleUseCase @Inject constructor(
    private val repository: PrayerRepository,
) {
    operator fun invoke(date: LocalDate, settings: PrayerCalculationSettings): List<PrayerTime> =
        repository.getDaySchedule(date, settings)

    suspend operator fun invoke(date: LocalDate): List<PrayerTime> =
        repository.getDaySchedule(date)
}

/** The middle and last third of the night beginning on a date, under the user's settings. */
class GetSunnahNightTimesUseCase @Inject constructor(
    private val repository: PrayerRepository,
) {
    operator fun invoke(date: LocalDate, settings: PrayerCalculationSettings): SunnahNightTimes =
        repository.getSunnahNightTimes(date, settings)

    suspend operator fun invoke(date: LocalDate): SunnahNightTimes =
        repository.getSunnahNightTimes(date)
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

/**
 * Confirm a range of unrecorded prayers as missed.
 *
 * The only way a prayer enters the qada list. Nothing marks a prayer missed on the user's behalf.
 */
class MarkUnrecordedAsMissedUseCase(private val repository: PrayerRepository) {
    suspend operator fun invoke(from: Long, to: Long): Int =
        repository.markUnrecordedAsMissed(from, to)
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

/** The most recently used locations, newest first — ordered by the database, not the caller. */
class GetRecentLocationsUseCase @Inject constructor(private val repository: PrayerRepository) {
    operator fun invoke(limit: Int = DEFAULT_LIMIT): Flow<List<Location>> =
        repository.getRecentLocations(limit)

    private companion object {
        const val DEFAULT_LIMIT = 5
    }
}

/**
 * Records a chosen location as *the* current one.
 *
 * The caller passes where the user picked, not a fully-formed row: composing a `Location` in the
 * ViewModel is what let it invent an id of 0 against an autogenerate primary key and insert a
 * duplicate on every selection.
 */
class SaveCurrentLocationUseCase @Inject constructor(private val repository: PrayerRepository) {
    suspend operator fun invoke(
        name: String,
        country: String,
        latitude: Double,
        longitude: Double,
        timezone: String,
        now: Long = System.currentTimeMillis(),
    ): Long = repository.saveCurrentLocation(
        Location(
            id = 0,
            name = name,
            latitude = latitude,
            longitude = longitude,
            timezone = timezone,
            country = country,
            city = name,
            isCurrentLocation = true,
            isFavorite = false,
            calculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE,
            asrCalculation = AsrCalculation.STANDARD,
            highLatitudeRule = null,
            fajrAngle = null,
            ishaAngle = null,
        ),
        now,
    )
}

class ToggleLocationFavoriteUseCase @Inject constructor(private val repository: PrayerRepository) {
    suspend operator fun invoke(id: Long) = repository.toggleFavorite(id)
}
