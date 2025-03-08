package com.arshadshah.nimaz.data.local.systems

import com.arshadshah.nimaz.data.local.dao.FastTrackerDao
import com.arshadshah.nimaz.data.local.dao.PrayerTimesDao
import com.arshadshah.nimaz.data.local.dao.PrayerTrackerDao
import com.arshadshah.nimaz.data.local.models.LocalFastTracker
import com.arshadshah.nimaz.data.local.models.LocalPrayerTimes
import com.arshadshah.nimaz.data.local.models.LocalPrayersTracker
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

class PrayerSystem @Inject constructor(
    private val prayerTrackerDao: PrayerTrackerDao,
    private val prayerTimesDao: PrayerTimesDao,
    private val fastTrackerDao: FastTrackerDao
) {
    // Prayer Tracker Operations
    suspend fun getTrackerForDate(date: LocalDate) = prayerTrackerDao.getTrackerForDate(date)

    fun getTrackersForMonth(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<LocalPrayersTracker>> = prayerTrackerDao.getTrackersForMonth(startDate, endDate)

    fun getTrackersForWeek(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<LocalPrayersTracker>> = prayerTrackerDao.getTrackersForWeek(startDate, endDate)

    suspend fun getAllTrackers() = prayerTrackerDao.getAllTrackers()

    suspend fun saveTracker(tracker: LocalPrayersTracker) = prayerTrackerDao.saveTracker(tracker)

    suspend fun updateTracker(tracker: LocalPrayersTracker) =
        prayerTrackerDao.updateTracker(tracker)

    suspend fun updateSpecificPrayer(
        date: LocalDate,
        prayerName: String,
        prayerDone: Boolean
    ) = prayerTrackerDao.updateSpecificPrayer(date, prayerName, prayerDone)

    fun getPrayersForDate(date: LocalDate) = prayerTrackerDao.getPrayersForDate(date)

    suspend fun checkIfTrackerExists(date: LocalDate) = prayerTrackerDao.trackerExistsForDate(date)

    fun getMenstruatingState(date: LocalDate): Flow<Boolean> =
        prayerTrackerDao.getMenstruatingState(date)

    suspend fun updateIsMenstruating(date: LocalDate, isMenstruating: Boolean) {
        fastTrackerDao.updateIsMenstruating(date, isMenstruating)
        prayerTrackerDao.updateMenstruationStatus(date, isMenstruating)
    }

    // Prayer Times Operations
    suspend fun getPrayerTimesForADate(date: String) = prayerTimesDao.getPrayerTimesForADate(date)

    suspend fun saveAllPrayerTimes(prayerTimes: LocalPrayerTimes) =
        prayerTimesDao.insert(prayerTimes)

    suspend fun countPrayerTimes() = prayerTimesDao.count()

    // Fast Tracker Operations
    suspend fun getFastTrackerForDate(date: LocalDate) =
        fastTrackerDao.getFastTrackerForDate(date)

    fun getFastTrackersForMonth(
        firstDay: LocalDate,
        lastDay: LocalDate
    ): Flow<List<LocalFastTracker>> = fastTrackerDao.getFastTrackersForMonth(firstDay, lastDay)

    fun isFastingForDate(date: LocalDate) = fastTrackerDao.isFastingForDate(date)

    suspend fun saveFastTracker(tracker: LocalFastTracker) =
        fastTrackerDao.saveFastTracker(tracker)

    suspend fun updateFastTracker(tracker: LocalFastTracker) =
        fastTrackerDao.updateFastTracker(tracker)

    suspend fun fastTrackerExistsForDate(date: LocalDate) =
        fastTrackerDao.fastTrackerExistsForDate(date)
}