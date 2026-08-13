package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.core.util.PrayerTimeCalculator
import com.arshadshah.nimaz.core.util.mapItems
import com.arshadshah.nimaz.core.util.toUtcMidnightMillis
import com.arshadshah.nimaz.data.local.database.dao.LocationDao
import com.arshadshah.nimaz.data.local.database.dao.PrayerDao
import com.arshadshah.nimaz.data.local.database.entity.LocationEntity
import com.arshadshah.nimaz.data.local.database.entity.PrayerRecordEntity
import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.HighLatitudeRule
import com.arshadshah.nimaz.domain.model.Location
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerCalculationSettings
import com.arshadshah.nimaz.domain.model.PrayerRecord
import com.arshadshah.nimaz.domain.model.PrayerStats
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.PrayerTime
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.domain.model.PrayerTimes
import com.arshadshah.nimaz.domain.model.SunnahNightTimes
import com.arshadshah.nimaz.domain.model.resolveLocation
import com.arshadshah.nimaz.domain.repository.PrayerRepository
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrayerRepositoryImpl @Inject constructor(
    private val prayerDao: PrayerDao,
    private val locationDao: LocationDao,
    private val prayerTimeCalculator: PrayerTimeCalculator,
    private val settingsRepository: SettingsRepository,
) : PrayerRepository {

    /**
     * The user's calculation settings, parsed once.
     *
     * `fromString` rather than `valueOf`: the persisted values include aliases the app itself
     * writes ("MWL", "ISNA", "MAKKAH", "hanafi"), and `valueOf` throws on every one of them. The
     * four ViewModels that each rolled this themselves caught that throw and substituted Muslim
     * World League, which is how a user on ISNA silently got MWL times.
     */
    override fun observeCalculationSettings(): Flow<PrayerCalculationSettings> {
        val locationFlow = combine(
            settingsRepository.latitude,
            settingsRepository.longitude,
            settingsRepository.locationName,
        ) { lat, lng, name -> resolveLocation(lat, lng, name) }

        val methodFlow = combine(
            settingsRepository.calculationMethod,
            settingsRepository.asrCalculation,
            settingsRepository.highLatitudeRule,
        ) { method, asr, highLat ->
            Triple(
                CalculationMethod.fromString(method),
                AsrCalculation.fromString(asr),
                HighLatitudeRule.fromString(highLat),
            )
        }

        // Six flows, so the vararg `combine` — the arity-specific overloads stop at five. The
        // order of the arguments is the order of [ADJUSTED_PRAYERS].
        val adjustmentsFlow = combine(
            settingsRepository.fajrAdjustment,
            settingsRepository.sunriseAdjustment,
            settingsRepository.dhuhrAdjustment,
            settingsRepository.asrAdjustment,
            settingsRepository.maghribAdjustment,
            settingsRepository.ishaAdjustment,
        ) { minutes: Array<Int> -> ADJUSTED_PRAYERS.zip(minutes.toList()).toMap() }

        return combine(locationFlow, methodFlow, adjustmentsFlow) { location, method, adjustments ->
            PrayerCalculationSettings(
                location = location,
                calculationMethod = method.first,
                asrCalculation = method.second,
                highLatitudeRule = method.third,
                adjustments = adjustments,
            )
        }
    }

    override fun getDaySchedule(
        date: LocalDate,
        settings: PrayerCalculationSettings,
    ): List<PrayerTime> = prayerTimeCalculator.getPrayerTimes(
        latitude = settings.location.latitude,
        longitude = settings.location.longitude,
        date = date,
        calculationMethod = settings.calculationMethod,
        asrCalculation = settings.asrCalculation,
        highLatitudeRule = settings.highLatitudeRule,
        adjustments = settings.adjustments,
    )

    override suspend fun getDaySchedule(date: LocalDate): List<PrayerTime> =
        getDaySchedule(date, observeCalculationSettings().first())

    override fun getSunnahNightTimes(
        date: LocalDate,
        settings: PrayerCalculationSettings,
    ): SunnahNightTimes {
        val sunnah = prayerTimeCalculator.getSunnahTimes(
            latitude = settings.location.latitude,
            longitude = settings.location.longitude,
            date = date,
            calculationMethod = settings.calculationMethod,
            asrCalculation = settings.asrCalculation,
            highLatitudeRule = settings.highLatitudeRule,
        )
        return SunnahNightTimes(
            middleOfTheNight = sunnah.middleOfTheNight,
            lastThirdOfTheNight = sunnah.lastThirdOfTheNight,
        )
    }

    override suspend fun getSunnahNightTimes(date: LocalDate): SunnahNightTimes =
        getSunnahNightTimes(date, observeCalculationSettings().first())

    override fun getTodayPrayerRecords(): Flow<Map<PrayerName, PrayerStatus>> {
        val todayEpoch = LocalDate.now().toUtcMidnightMillis()
        return prayerDao.getPrayerRecordsForDate(todayEpoch).map { entities ->
            entities.associate { PrayerName.fromString(it.prayerName) to PrayerStatus.fromString(it.status) }
        }
    }

    override fun getPrayerTimesForDate(date: LocalDate, location: Location): PrayerTimes {
        return prayerTimeCalculator.calculatePrayerTimes(date, location)
    }

    override fun getPrayerTimesForRange(
        startDate: LocalDate,
        endDate: LocalDate,
        location: Location
    ): List<PrayerTimes> {
        return prayerTimeCalculator.calculatePrayerTimesForRange(startDate, endDate, location)
    }

    override fun getPrayerRecordsForDate(date: Long): Flow<List<PrayerRecord>> {
        return prayerDao.getPrayerRecordsForDate(date).mapItems { it.toDomain() }
    }

    override fun getPrayerRecordsInRange(startDate: Long, endDate: Long): Flow<List<PrayerRecord>> {
        return prayerDao.getPrayerRecordsInRange(startDate, endDate).mapItems { it.toDomain() }
    }

    override suspend fun getPrayerRecord(date: Long, prayerName: PrayerName): PrayerRecord? {
        return prayerDao.getPrayerRecord(date, prayerName.name.lowercase())?.toDomain()
    }

    override fun getPrayerRecordsByStatus(status: PrayerStatus): Flow<List<PrayerRecord>> {
        return prayerDao.getPrayerRecordsByStatus(status.name.lowercase())
            .mapItems { it.toDomain() }
    }

    override fun getMissedPrayersRequiringQada(): Flow<List<PrayerRecord>> {
        return prayerDao.getMissedPrayersRequiringQada().mapItems { it.toDomain() }
    }

    override suspend fun insertPrayerRecord(record: PrayerRecord) {
        prayerDao.insertPrayerRecord(record.toEntity())
    }

    override suspend fun insertPrayerRecords(records: List<PrayerRecord>) {
        prayerDao.insertPrayerRecords(records.map { it.toEntity() })
    }

    override suspend fun updatePrayerStatus(
        date: Long,
        prayerName: PrayerName,
        status: PrayerStatus,
        prayedAt: Long?,
        isJamaah: Boolean
    ) {
        // Check if record exists, if not create it first
        val existingRecord = prayerDao.getPrayerRecord(date, prayerName.name.lowercase())
        if (existingRecord == null) {
            // Create a new record
            val newRecord = PrayerRecordEntity(
                id = 0, // Auto-generate
                date = date,
                prayerName = prayerName.name.lowercase(),
                status = status.name.lowercase(),
                prayedAt = prayedAt,
                scheduledTime = date, // Use date as scheduled time placeholder
                isJamaah = isJamaah,
                isQadaFor = null,
                note = null,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            prayerDao.insertPrayerRecord(newRecord)
        } else {
            prayerDao.updatePrayerStatus(
                date = date,
                prayerName = prayerName.name.lowercase(),
                status = status.name.lowercase(),
                prayedAt = prayedAt,
                isJamaah = isJamaah
            )
        }
    }

    override suspend fun getPrayerStats(startDate: Long, endDate: Long): PrayerStats {
        val prayedCount = prayerDao.getPrayedCountInRange(startDate, endDate)
        val missedCount = prayerDao.getMissedCountInRange(startDate, endDate)
        val jamaahCount = prayerDao.getJamaahCountInRange(startDate, endDate)
        val prayedByPrayer = prayerDao.getPrayedCountByPrayer(startDate, endDate)
            .associate { PrayerName.fromString(it.prayerName) to it.count }
        val missedByPrayer = prayerDao.getMissedCountByPrayer(startDate, endDate)
            .associate { PrayerName.fromString(it.prayerName) to it.count }

        // Get perfect days list for streak calculation
        val perfectDays = prayerDao.getPerfectDays()
        val perfectDaysCount = prayerDao.getPerfectDaysCount(startDate, endDate)

        // Calculate current streak and longest streak from perfect days
        val (currentStreak, longestStreak) = calculateStreaks(perfectDays)

        return PrayerStats(
            totalPrayed = prayedCount,
            totalMissed = missedCount,
            totalJamaah = jamaahCount,
            prayedByPrayer = prayedByPrayer,
            missedByPrayer = missedByPrayer,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            perfectDays = perfectDaysCount,
            startDate = startDate,
            endDate = endDate
        )
    }

    override suspend fun getCurrentStreak(currentDate: Long): Int {
        val perfectDays = prayerDao.getPerfectDays()
        return calculateStreaks(perfectDays).first
    }

    override suspend fun getLongestStreak(): Int {
        val perfectDays = prayerDao.getPerfectDays()
        return calculateStreaks(perfectDays).second
    }

    override suspend fun markUnrecordedAsMissed(from: Long, to: Long): Int =
        prayerDao.markUnrecordedAsMissed(from, to)

    /**
     * Calculate current streak and longest streak from a list of perfect days.
     * Perfect days are dates (epoch millis at start of day) where all 5 prayers were completed.
     * The list is sorted descending (most recent first).
     *
     * @return Pair of (currentStreak, longestStreak)
     */
    private fun calculateStreaks(perfectDays: List<Long>): Pair<Int, Int> {
        if (perfectDays.isEmpty()) return Pair(0, 0)

        val today = LocalDate.now()
        val todayEpoch = today.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        val yesterdayEpoch =
            today.minusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        val oneDayMillis = 24 * 60 * 60 * 1000L

        // Convert to sorted set for efficient lookup (ascending order)
        val perfectDaysSet = perfectDays.toSortedSet()

        // Calculate current streak - count consecutive days from today or yesterday backwards
        var currentStreak = 0
        val startingDay = if (perfectDaysSet.contains(todayEpoch)) {
            todayEpoch
        } else if (perfectDaysSet.contains(yesterdayEpoch)) {
            yesterdayEpoch
        } else {
            null
        }

        if (startingDay != null) {
            var checkDay = startingDay
            while (perfectDaysSet.contains(checkDay)) {
                currentStreak++
                checkDay -= oneDayMillis
            }
        }

        // Calculate longest streak
        var longestStreak = 0
        var tempStreak = 0
        var previousDay: Long? = null

        for (day in perfectDaysSet) {
            if (previousDay == null || day - previousDay == oneDayMillis) {
                tempStreak++
            } else {
                longestStreak = maxOf(longestStreak, tempStreak)
                tempStreak = 1
            }
            previousDay = day
        }
        longestStreak = maxOf(longestStreak, tempStreak)

        return Pair(currentStreak, longestStreak)
    }

    // Location operations
    override fun getAllLocations(): Flow<List<Location>> {
        return locationDao.getAllLocations().mapItems { it.toDomain() }
    }

    override fun getCurrentLocation(): Flow<Location?> {
        return locationDao.getCurrentLocation().map { it?.toDomain() }
    }

    override suspend fun getCurrentLocationSync(): Location? {
        return locationDao.getCurrentLocationSync()?.toDomain()
    }

    override fun getFavoriteLocations(): Flow<List<Location>> {
        return locationDao.getFavoriteLocations().mapItems { it.toDomain() }
    }

    override suspend fun getLocationById(id: Long): Location? {
        return locationDao.getLocationById(id)?.toDomain()
    }

    override fun searchLocations(query: String): Flow<List<Location>> {
        return locationDao.searchLocations(query).mapItems { it.toDomain() }
    }

    override suspend fun insertLocation(location: Location): Long {
        return locationDao.insertLocation(location.toEntity())
    }

    override suspend fun updateLocation(location: Location) {
        locationDao.updateLocation(location.toEntity())
    }

    override suspend fun deleteLocation(location: Location) {
        locationDao.deleteLocation(location.toEntity())
    }

    override suspend fun setCurrentLocation(id: Long) {
        locationDao.clearCurrentLocation()
        locationDao.setCurrentLocation(id)
    }

    override fun getRecentLocations(limit: Int): Flow<List<Location>> {
        return locationDao.getRecentLocations(limit).mapItems { it.toDomain() }
    }

    override suspend fun saveCurrentLocation(location: Location, now: Long): Long {
        return locationDao.saveCurrentLocation(location.toEntity(), now)
    }

    override suspend fun toggleFavorite(id: Long) {
        locationDao.toggleFavorite(id)
    }

    // Mapping functions
    private fun PrayerRecordEntity.toDomain(): PrayerRecord {
        return PrayerRecord(
            id = id,
            date = date,
            prayerName = PrayerName.fromString(prayerName),
            status = PrayerStatus.fromString(status),
            prayedAt = prayedAt,
            scheduledTime = scheduledTime,
            isJamaah = isJamaah,
            isQadaFor = isQadaFor,
            note = note,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun PrayerRecord.toEntity(): PrayerRecordEntity {
        return PrayerRecordEntity(
            id = id,
            date = date,
            prayerName = prayerName.name.lowercase(),
            status = status.name.lowercase(),
            prayedAt = prayedAt,
            scheduledTime = scheduledTime,
            isJamaah = isJamaah,
            isQadaFor = isQadaFor,
            note = note,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun LocationEntity.toDomain(): Location {
        return Location(
            id = id,
            name = name,
            latitude = latitude,
            longitude = longitude,
            timezone = timezone,
            country = country,
            city = city,
            isCurrentLocation = isCurrentLocation,
            isFavorite = isFavorite,
            calculationMethod = CalculationMethod.fromString(calculationMethod),
            asrCalculation = AsrCalculation.fromString(asrCalculation),
            highLatitudeRule = HighLatitudeRule.fromString(highLatitudeRule),
            fajrAngle = fajrAngle,
            ishaAngle = ishaAngle
        )
    }

    private fun Location.toEntity(): LocationEntity {
        return LocationEntity(
            id = id,
            name = name,
            latitude = latitude,
            longitude = longitude,
            timezone = timezone,
            country = country,
            city = city,
            isCurrentLocation = isCurrentLocation,
            isFavorite = isFavorite,
            calculationMethod = calculationMethod.name,
            asrCalculation = asrCalculation.name.lowercase(),
            highLatitudeRule = highLatitudeRule?.name?.lowercase(),
            fajrAngle = fajrAngle,
            ishaAngle = ishaAngle
        )
    }

    private companion object {
        /** The prayers an adjustment applies to, in the order their preference flows are combined. */
        val ADJUSTED_PRAYERS = listOf(
            PrayerType.FAJR,
            PrayerType.SUNRISE,
            PrayerType.DHUHR,
            PrayerType.ASR,
            PrayerType.MAGHRIB,
            PrayerType.ISHA,
        )
    }
}
