package com.arshadshah.nimaz.data.sync

import com.arshadshah.nimaz.core.datastore.PreferencesDataStore
import com.arshadshah.nimaz.data.local.database.dao.FastingDao
import com.arshadshah.nimaz.data.local.database.dao.KhatamDao
import com.arshadshah.nimaz.data.local.database.dao.LocationDao
import com.arshadshah.nimaz.data.local.database.dao.PrayerDao
import com.arshadshah.nimaz.data.local.database.dao.TasbihDao
import com.arshadshah.nimaz.data.local.database.dao.ZakatDao
import com.arshadshah.nimaz.data.local.database.entity.FastRecordEntity
import com.arshadshah.nimaz.data.local.database.entity.KhatamAyahEntity
import com.arshadshah.nimaz.data.local.database.entity.KhatamDailyLogEntity
import com.arshadshah.nimaz.data.local.database.entity.KhatamEntity
import com.arshadshah.nimaz.data.local.database.entity.LocationEntity
import com.arshadshah.nimaz.data.local.database.entity.MakeupFastEntity
import com.arshadshah.nimaz.data.local.database.entity.PrayerRecordEntity
import com.arshadshah.nimaz.data.local.database.entity.ReadingProgressEntity
import com.arshadshah.nimaz.data.local.database.entity.TafseerHighlightEntity
import com.arshadshah.nimaz.data.local.database.entity.TafseerNoteEntity
import com.arshadshah.nimaz.data.local.database.entity.TasbihPresetEntity
import com.arshadshah.nimaz.data.local.database.entity.TasbihSessionEntity
import com.arshadshah.nimaz.data.local.database.entity.ZakatHistoryEntity
import com.arshadshah.nimaz.data.local.user.BookmarkDao
import com.arshadshah.nimaz.data.local.user.BookmarkEntity
import com.arshadshah.nimaz.data.local.user.BookmarkKind
import com.arshadshah.nimaz.data.local.user.ProgressDao
import com.arshadshah.nimaz.data.local.user.ProgressEntity
import com.arshadshah.nimaz.data.local.user.ProgressKind
import com.arshadshah.nimaz.data.local.user.ReadingProgressDao
import com.arshadshah.nimaz.data.local.user.TafseerUserDao
import com.arshadshah.nimaz.data.local.user.TasbihSessionDao
import io.mockk.coEvery

/**
 * A device with one of everything on it, and the same device with nothing on it.
 *
 * Both the exporter and the importer tests need a populated set of DAOs, and the two halves
 * only prove a round trip if they agree on the rows — so the rows live here once.
 */
internal object SyncFixtures {

    /** A verse that is bookmarked *and* favourited: one row, two flags, two wire lists. */
    const val BOTH_AYAH = 262

    /** A verse that is only favourited — it must not travel as a bookmark. */
    const val FAVOURITE_ONLY_AYAH = 300

    const val HADITH_ID = 5001
    const val DUA_ID = 42

    fun populate(
        prayerDao: PrayerDao,
        fastingDao: FastingDao,
        tasbihDao: TasbihDao,
        sessionDao: TasbihSessionDao,
        khatamDao: KhatamDao,
        tafseerUserDao: TafseerUserDao,
        zakatDao: ZakatDao,
        bookmarkDao: BookmarkDao,
        progressDao: ProgressDao,
        readingProgressDao: ReadingProgressDao,
        locationDao: LocationDao,
        preferences: PreferencesDataStore,
    ) {
        coEvery { bookmarkDao.all() } returns marks()
        coEvery { progressDao.all() } returns progressRows()
        coEvery { readingProgressDao.get() } returns ReadingProgressEntity(
            id = 1,
            lastReadSurah = 2, lastReadAyah = 255, lastReadPage = 42, lastReadJuz = 3,
            totalAyahsRead = 900, currentKhatmaCount = 2, updatedAt = 1_000L,
        )
        coEvery { prayerDao.getAllPrayerRecords() } returns listOf(prayerRecord())
        coEvery { fastingDao.getAllFastRecords() } returns listOf(fastRecord())
        coEvery { fastingDao.getAllMakeupFastsSync() } returns listOf(makeupFast())
        coEvery { tasbihDao.getAllPresetsSync() } returns listOf(preset())
        coEvery { sessionDao.getAllSessionsSync() } returns listOf(session())
        coEvery { khatamDao.getAllKhatamsSync() } returns listOf(khatam(1L), khatam(2L))
        coEvery { khatamDao.getKhatamAyahsSync(1L) } returns
            listOf(KhatamAyahEntity(khatamId = 1L, ayahId = 1, readAt = 10L, updatedAt = 10L))
        coEvery { khatamDao.getKhatamAyahsSync(2L) } returns
            listOf(KhatamAyahEntity(khatamId = 2L, ayahId = 2, readAt = 20L, updatedAt = 20L))
        coEvery { khatamDao.getDailyLogsSync(1L) } returns
            listOf(KhatamDailyLogEntity(khatamId = 1L, date = 1L, ayahsRead = 20, updatedAt = 10L))
        coEvery { khatamDao.getDailyLogsSync(2L) } returns
            listOf(KhatamDailyLogEntity(khatamId = 2L, date = 2L, ayahsRead = 30, updatedAt = 20L))
        coEvery { tafseerUserDao.getAllHighlightsSync() } returns listOf(highlight())
        coEvery { tafseerUserDao.getAllNotesSync() } returns listOf(note())
        coEvery { zakatDao.getAllHistorySync() } returns listOf(zakat())
        coEvery { locationDao.getFavoriteLocationsSync() } returns listOf(location())
        coEvery { preferences.exportAllPreferences() } returns mapOf("theme" to "dark")
    }

    fun empty(
        prayerDao: PrayerDao,
        fastingDao: FastingDao,
        tasbihDao: TasbihDao,
        sessionDao: TasbihSessionDao,
        khatamDao: KhatamDao,
        tafseerUserDao: TafseerUserDao,
        zakatDao: ZakatDao,
        bookmarkDao: BookmarkDao,
        progressDao: ProgressDao,
        readingProgressDao: ReadingProgressDao,
        locationDao: LocationDao,
        preferences: PreferencesDataStore,
    ) {
        coEvery { bookmarkDao.all() } returns emptyList()
        coEvery { progressDao.all() } returns emptyList()
        coEvery { readingProgressDao.get() } returns null
        coEvery { prayerDao.getAllPrayerRecords() } returns emptyList()
        coEvery { fastingDao.getAllFastRecords() } returns emptyList()
        coEvery { fastingDao.getAllMakeupFastsSync() } returns emptyList()
        coEvery { tasbihDao.getAllPresetsSync() } returns emptyList()
        coEvery { sessionDao.getAllSessionsSync() } returns emptyList()
        coEvery { khatamDao.getAllKhatamsSync() } returns emptyList()
        coEvery { tafseerUserDao.getAllHighlightsSync() } returns emptyList()
        coEvery { tafseerUserDao.getAllNotesSync() } returns emptyList()
        coEvery { zakatDao.getAllHistorySync() } returns emptyList()
        coEvery { locationDao.getAllLocationsSync() } returns emptyList()
        coEvery { locationDao.getFavoriteLocationsSync() } returns emptyList()
        coEvery { preferences.exportAllPreferences() } returns emptyMap()
    }

    fun marks() = listOf(
        BookmarkEntity(
            kind = BookmarkKind.AYAH, targetId = BOTH_AYAH, bookmarked = true, favourite = true,
            note = "memorise", colour = "#FF0000", contextId = 2, ordinal = 255,
            createdAt = 100L, updatedAt = 200L,
        ),
        BookmarkEntity(
            kind = BookmarkKind.AYAH, targetId = FAVOURITE_ONLY_AYAH,
            bookmarked = false, favourite = true, contextId = 3, ordinal = 1,
            createdAt = 110L, updatedAt = 210L,
        ),
        BookmarkEntity(
            kind = BookmarkKind.HADITH, targetId = HADITH_ID, bookmarked = true,
            note = "hadith note", colour = "#00FF00", contextId = 1, ordinal = 12,
            createdAt = 120L, updatedAt = 220L,
        ),
        BookmarkEntity(
            kind = BookmarkKind.DUA, targetId = DUA_ID, bookmarked = true, favourite = true,
            note = "dua note", contextId = 4, createdAt = 130L, updatedAt = 230L,
        ),
        BookmarkEntity(
            kind = BookmarkKind.ASMA_UL_HUSNA, targetId = 7, favourite = true,
            createdAt = 140L, updatedAt = 240L,
        ),
        BookmarkEntity(
            kind = BookmarkKind.ASMA_UN_NABI, targetId = 8, favourite = true,
            createdAt = 150L, updatedAt = 250L,
        ),
        BookmarkEntity(
            kind = BookmarkKind.PROPHET, targetId = 9, favourite = true,
            createdAt = 160L, updatedAt = 260L,
        ),
    )

    fun progressRows() = listOf(
        ProgressEntity(
            kind = ProgressKind.DUA, targetId = DUA_ID, date = 500L, completed = 3, total = 7,
            isCompleted = false, createdAt = 300L, updatedAt = 300L,
        ),
        ProgressEntity(
            kind = ProgressKind.QAIDA_LESSON, targetId = 3, completed = 12, total = 12,
            isCompleted = true, state = "COMPLETED", score = 3, resumeId = 11,
            createdAt = 310L, updatedAt = 310L,
        ),
        ProgressEntity(
            kind = ProgressKind.QAIDA_CELL, targetId = 11, contextId = 3, completed = 4,
            isCompleted = true, createdAt = 320L, updatedAt = 320L,
        ),
    )

    fun progress(kind: String, targetId: Int, total: Int?) = ProgressEntity(
        kind = kind, targetId = targetId, total = total, createdAt = 1L, updatedAt = 1L,
    )

    fun prayerRecord() = PrayerRecordEntity(
        id = 1, date = 1_000L, prayerName = "fajr", status = "prayed", prayedAt = 1_100L,
        scheduledTime = 1_050L, isJamaah = true, isQadaFor = null, note = "n",
        createdAt = 1_000L, updatedAt = 1_200L,
    )

    fun fastRecord() = FastRecordEntity(
        id = 1, date = 2_000L, hijriDate = "1/9/1446", hijriMonth = 9, hijriYear = 1446,
        fastType = "ramadan", status = "fasted", exemptionReason = null,
        suhoorTime = 1_900L, iftarTime = 2_500L, note = null,
        createdAt = 2_000L, updatedAt = 2_200L,
    )

    fun makeupFast() = MakeupFastEntity(
        id = 1, originalDate = 3_000L, originalHijriDate = "2/9/1446", reason = "travel",
        status = "pending", completedDate = null, fidyaAmount = 5.0, note = null,
        createdAt = 3_000L, updatedAt = 3_200L,
    )

    fun preset() = TasbihPresetEntity(
        id = 1, name = "SubhanAllah", arabic = "سبحان الله", transliteration = "SubhanAllah",
        translation = "Glory be to God", targetCount = 33, isCustom = 0, displayOrder = 1,
        updatedAt = 4_000L,
    )

    fun session() = TasbihSessionEntity(
        id = 1, presetId = 1, presetName = "SubhanAllah", date = 5_000L, currentCount = 33,
        targetCount = 33, totalLaps = 1, isCompleted = true, duration = 60_000L,
        startedAt = 5_000L, completedAt = 5_060L, note = null, updatedAt = 5_100L,
    )

    fun khatam(id: Long) = KhatamEntity(
        id = id, name = "Khatam $id", notes = null, status = "active", isActive = id == 1L,
        dailyTarget = 20, deadline = null, reminderEnabled = true, reminderTime = "06:30",
        totalAyahsRead = 100, createdAt = 6_000L + id, startedAt = 6_000L + id,
        completedAt = null, updatedAt = 6_100L + id,
    )

    fun highlight() = TafseerHighlightEntity(
        id = 1, ayahId = 262, tafseerId = "ibn-kathir", startOffset = 10, endOffset = 40,
        color = "#FFFF00", note = "why", createdAt = 7_000L, updatedAt = 7_100L,
    )

    fun note() = TafseerNoteEntity(
        id = 1, ayahId = 262, tafseerId = "ibn-kathir", text = "note text",
        createdAt = 8_000L, updatedAt = 8_100L,
    )

    fun zakat() = ZakatHistoryEntity(
        id = 1, calculatedAt = 9_000L, totalAssets = 5_000.0, totalLiabilities = 1_000.0,
        netWorth = 4_000.0, zakatDue = 100.0, nisabType = "silver", nisabValue = 600.0,
        isPaid = true, paidAt = 9_100L, notes = "paid", updatedAt = 9_200L,
    )

    fun location() = LocationEntity(
        id = 1, name = "Dublin", latitude = 53.35, longitude = -6.26, timezone = "Europe/Dublin",
        country = "Ireland", city = "Dublin", isCurrentLocation = true, isFavorite = true,
        calculationMethod = "MWL", asrCalculation = "standard",
        highLatitudeRule = "middle_of_night", fajrAngle = 18.0, ishaAngle = 17.0,
        createdAt = 10_000L, updatedAt = 10_100L,
    )
}
