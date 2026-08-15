package com.arshadshah.nimaz.domain.repository

import com.arshadshah.nimaz.domain.model.Location
import com.arshadshah.nimaz.domain.model.PrayerCalculationSettings
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerRecord
import com.arshadshah.nimaz.domain.model.PrayerStats
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.PrayerTime
import com.arshadshah.nimaz.domain.model.PrayerTimes
import com.arshadshah.nimaz.domain.model.SunnahNightTimes
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface PrayerRepository {
    // Today's prayer records — shared flow for cross-screen sync
    fun getTodayPrayerRecords(): Flow<Map<PrayerName, PrayerStatus>>

    // Prayer times calculation
    //
    // Two sources of location, and only one of them is the user's. This overload takes a
    // `Location` — a row in the `locations` table, written only by *searching for and picking* a
    // place. Detecting by GPS, onboarding and the home screen's own picker all write the
    // preference store instead, so for those users the table is empty and this has nothing to be
    // called with. Prefer the [PrayerCalculationSettings] overload below: it reads the
    // preferences every other prayer-time surface reads, and applies the method, school,
    // high-latitude rule and per-prayer adjustments that a `Location` row's own columns do not
    // carry. This one remains for the location *browser*, which genuinely is asking about a
    // specific saved row rather than about the user's own position.
    fun getPrayerTimesForDate(date: LocalDate, location: Location): PrayerTimes

    /**
     * The day's five times as wall-clock times, from the user's own calculation settings.
     *
     * [getDaySchedule] in the shape a day *card* wants: the same instants, the same adjustments,
     * named rather than listed. The zone is the device's, which is the zone "has this prayer
     * passed?" is being asked in.
     */
    fun getPrayerTimesForDate(date: LocalDate, settings: PrayerCalculationSettings): PrayerTimes
    fun getPrayerTimesForRange(
        startDate: LocalDate,
        endDate: LocalDate,
        location: Location
    ): List<PrayerTimes>

    /**
     * The user's own calculation settings — location, method, school, high-latitude rule and the
     * six per-prayer adjustments — resolved from preferences and re-emitted when any of them
     * changes.
     *
     * The single place the persisted strings are parsed. Four ViewModels each did it with their
     * own `try { valueOf(s) } catch { MWL }`, which throws on every alias the app itself writes
     * ("MWL", "ISNA", "MAKKAH") and then silently substituted Muslim World League — so a user on
     * ISNA got MWL prayer times, for ever, with no signal.
     */
    fun observeCalculationSettings(): Flow<PrayerCalculationSettings>

    /**
     * The prayer times for [date] under [settings]. Pure and synchronous, so a caller holding a
     * settings snapshot can compute a month of days without a suspension point per day.
     */
    fun getDaySchedule(date: LocalDate, settings: PrayerCalculationSettings): List<PrayerTime>

    /**
     * The prayer times for [date] under whatever the user has currently set.
     *
     * The convenience the calculator never offered: its `getPrayerTimes` defaulted all four
     * calculation arguments, so *forgetting* to pass the user's settings compiled, ran, and
     * produced plausible times for the wrong configuration. A caller that does not care about
     * the settings now gets the right ones instead of the defaults.
     */
    suspend fun getDaySchedule(date: LocalDate): List<PrayerTime>

    /** The Sunnah night instants for the night beginning on [date], under [settings]. */
    fun getSunnahNightTimes(
        date: LocalDate,
        settings: PrayerCalculationSettings,
    ): SunnahNightTimes

    /** The Sunnah night instants for the night beginning on [date], under the user's settings. */
    suspend fun getSunnahNightTimes(date: LocalDate): SunnahNightTimes

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
    suspend fun markUnrecordedAsMissed(from: Long, to: Long): Int

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

    /** The most recently used locations, newest first. */
    fun getRecentLocations(limit: Int): Flow<List<Location>>

    /**
     * Makes [location] the one and only current location, inserting it or refreshing the stored
     * row at the same coordinates, in one transaction. Returns that row's id.
     */
    suspend fun saveCurrentLocation(location: Location, now: Long): Long
    suspend fun toggleFavorite(id: Long)
}
