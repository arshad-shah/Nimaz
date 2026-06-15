package com.arshadshah.nimaz.data.sync

import com.arshadshah.nimaz.data.local.database.NimazDatabase
import com.arshadshah.nimaz.data.local.database.dao.FastingDao
import com.arshadshah.nimaz.data.local.database.dao.KhatamDao
import com.arshadshah.nimaz.data.local.database.dao.PrayerDao
import com.arshadshah.nimaz.data.local.database.dao.QuranDao
import com.arshadshah.nimaz.data.local.database.dao.TafseerDao
import com.arshadshah.nimaz.data.local.database.dao.TasbihDao
import com.arshadshah.nimaz.data.local.database.dao.ZakatDao
import com.arshadshah.nimaz.data.local.database.entity.FastRecordEntity
import com.arshadshah.nimaz.data.local.database.entity.KhatamAyahEntity
import com.arshadshah.nimaz.data.local.database.entity.KhatamDailyLogEntity
import com.arshadshah.nimaz.data.local.database.entity.KhatamEntity
import com.arshadshah.nimaz.data.local.database.entity.MakeupFastEntity
import com.arshadshah.nimaz.data.local.database.entity.PrayerRecordEntity
import com.arshadshah.nimaz.data.local.database.entity.QuranBookmarkEntity
import com.arshadshah.nimaz.data.local.database.entity.QuranFavoriteEntity
import com.arshadshah.nimaz.data.local.database.entity.ReadingProgressEntity
import com.arshadshah.nimaz.data.local.database.entity.TafseerHighlightEntity
import com.arshadshah.nimaz.data.local.database.entity.TafseerNoteEntity
import com.arshadshah.nimaz.data.local.database.entity.TasbihPresetEntity
import com.arshadshah.nimaz.data.local.database.entity.TasbihSessionEntity
import com.arshadshah.nimaz.data.local.database.entity.ZakatHistoryEntity
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncDataImporter @Inject constructor(
    private val database: NimazDatabase,
    private val quranDao: QuranDao,
    private val prayerDao: PrayerDao,
    private val fastingDao: FastingDao,
    private val tasbihDao: TasbihDao,
    private val khatamDao: KhatamDao,
    private val tafseerDao: TafseerDao,
    private val zakatDao: ZakatDao,
    private val preferencesDataStore: PreferencesDataStore
) {
    /**
     * Full import in one transaction (kept for backwards compatibility).
     */
    suspend fun import(payload: SyncPayload) {
        importQuranData(payload)
        importPrayerData(payload)
        importFastingData(payload)
        importTasbihData(payload)
        importKhatamData(payload)
        importTafseerData(payload)
        importZakatData(payload)
        importPreferencesData(payload)
    }

    // --- Granular import methods for step-by-step progress ---

    suspend fun importQuranData(payload: SyncPayload) {
        importBookmarks(payload.bookmarks)
        importFavorites(payload.favorites)
        importReadingProgress(payload.readingProgress)
    }

    suspend fun importPrayerData(payload: SyncPayload) {
        importPrayerRecords(payload.prayerRecords)
    }

    suspend fun importFastingData(payload: SyncPayload) {
        importFastRecords(payload.fastRecords)
        importMakeupFasts(payload.makeupFasts)
    }

    suspend fun importTasbihData(payload: SyncPayload) {
        importTasbihPresets(payload.tasbihPresets)
        importTasbihSessions(payload.tasbihSessions)
    }

    suspend fun importKhatamData(payload: SyncPayload) {
        importKhatams(payload.khatams)
        importKhatamAyahs(payload.khatamAyahs)
        importKhatamDailyLogs(payload.khatamDailyLogs)
    }

    suspend fun importTafseerData(payload: SyncPayload) {
        importTafseerHighlights(payload.tafseerHighlights)
        importTafseerNotes(payload.tafseerNotes)
    }

    suspend fun importZakatData(payload: SyncPayload) {
        importZakatHistory(payload.zakatHistory)
    }

    suspend fun importPreferencesData(payload: SyncPayload) {
        if (payload.preferences.isNotEmpty()) {
            preferencesDataStore.importPreferences(payload.preferences)
        }
    }

    // --- Quran ---

    private suspend fun importBookmarks(incoming: List<SyncBookmark>) {
        val existing = quranDao.getAllBookmarksSync().associateBy { it.ayahId }
        for (item in incoming) {
            val local = existing[item.ayahId]
            if (local == null || item.updatedAt > local.updatedAt) {
                quranDao.insertBookmark(
                    QuranBookmarkEntity(
                        id = local?.id ?: 0,
                        ayahId = item.ayahId,
                        surahNumber = item.surahNumber,
                        ayahNumber = item.ayahNumber,
                        note = item.note,
                        color = item.color,
                        createdAt = item.createdAt,
                        updatedAt = item.updatedAt
                    )
                )
            }
        }
    }

    private suspend fun importFavorites(incoming: List<SyncFavorite>) {
        val existing = quranDao.getAllFavoritesSync().associateBy { it.ayahId }
        for (item in incoming) {
            val local = existing[item.ayahId]
            if (local == null || item.updatedAt > local.updatedAt) {
                quranDao.insertFavorite(
                    QuranFavoriteEntity(
                        ayahId = item.ayahId,
                        surahNumber = item.surahNumber,
                        ayahNumber = item.ayahNumber,
                        createdAt = item.createdAt,
                        updatedAt = item.updatedAt
                    )
                )
            }
        }
    }

    private suspend fun importReadingProgress(incoming: SyncReadingProgress?) {
        incoming ?: return
        val local = quranDao.getReadingProgressSync()
        if (local == null || incoming.updatedAt > local.updatedAt) {
            quranDao.insertReadingProgress(
                ReadingProgressEntity(
                    id = 1,
                    lastReadSurah = incoming.lastReadSurah,
                    lastReadAyah = incoming.lastReadAyah,
                    lastReadPage = incoming.lastReadPage,
                    lastReadJuz = incoming.lastReadJuz,
                    totalAyahsRead = incoming.totalAyahsRead,
                    currentKhatmaCount = incoming.currentKhatmaCount,
                    updatedAt = incoming.updatedAt
                )
            )
        }
    }

    // --- Prayer & Fasting ---

    private suspend fun importPrayerRecords(incoming: List<SyncPrayerRecord>) {
        val existing = prayerDao.getAllPrayerRecords().associateBy { "${it.date}_${it.prayerName}" }
        val toInsert = mutableListOf<PrayerRecordEntity>()
        for (item in incoming) {
            val key = "${item.date}_${item.prayerName}"
            val local = existing[key]
            if (local == null || item.updatedAt > local.updatedAt) {
                toInsert.add(
                    PrayerRecordEntity(
                        id = local?.id ?: 0,
                        date = item.date,
                        prayerName = item.prayerName,
                        status = item.status,
                        prayedAt = item.prayedAt,
                        scheduledTime = item.scheduledTime,
                        isJamaah = item.isJamaah,
                        isQadaFor = item.isQadaFor,
                        note = item.note,
                        createdAt = item.createdAt,
                        updatedAt = item.updatedAt
                    )
                )
            }
        }
        if (toInsert.isNotEmpty()) prayerDao.insertPrayerRecords(toInsert)
    }

    private suspend fun importFastRecords(incoming: List<SyncFastRecord>) {
        val existing = fastingDao.getAllFastRecords().associateBy { it.date }
        val toInsert = mutableListOf<FastRecordEntity>()
        for (item in incoming) {
            val local = existing[item.date]
            if (local == null || item.updatedAt > local.updatedAt) {
                toInsert.add(
                    FastRecordEntity(
                        id = local?.id ?: 0,
                        date = item.date,
                        hijriDate = item.hijriDate,
                        hijriMonth = item.hijriMonth,
                        hijriYear = item.hijriYear,
                        fastType = item.fastType,
                        status = item.status,
                        exemptionReason = item.exemptionReason,
                        suhoorTime = item.suhoorTime,
                        iftarTime = item.iftarTime,
                        note = item.note,
                        createdAt = item.createdAt,
                        updatedAt = item.updatedAt
                    )
                )
            }
        }
        if (toInsert.isNotEmpty()) fastingDao.insertFastRecords(toInsert)
    }

    private suspend fun importMakeupFasts(incoming: List<SyncMakeupFast>) {
        val existing = fastingDao.getAllMakeupFastsSync().associateBy { it.id }
        for (item in incoming) {
            val local = existing[item.id]
            if (local == null || item.updatedAt > local.updatedAt) {
                fastingDao.insertMakeupFast(
                    MakeupFastEntity(
                        id = item.id,
                        originalDate = item.originalDate,
                        originalHijriDate = item.originalHijriDate,
                        reason = item.reason,
                        status = item.status,
                        completedDate = item.completedDate,
                        fidyaAmount = item.fidyaAmount,
                        note = item.note,
                        createdAt = item.createdAt,
                        updatedAt = item.updatedAt
                    )
                )
            }
        }
    }

    // --- Tasbih ---

    private suspend fun importTasbihPresets(incoming: List<SyncTasbihPreset>) {
        val existing = tasbihDao.getAllPresetsSync().associateBy { it.id }
        for (item in incoming) {
            val local = existing[item.id]
            if (local == null || item.updatedAt > local.updatedAt) {
                tasbihDao.insertPreset(
                    TasbihPresetEntity(
                        id = item.id,
                        name = item.name,
                        arabic = item.arabic,
                        transliteration = item.transliteration,
                        translation = item.translation,
                        targetCount = item.targetCount,
                        isCustom = item.isCustom,
                        displayOrder = item.displayOrder,
                        updatedAt = item.updatedAt
                    )
                )
            }
        }
    }

    private suspend fun importTasbihSessions(incoming: List<SyncTasbihSession>) {
        val existing = tasbihDao.getAllSessionsSync().associateBy { it.id }
        for (item in incoming) {
            val local = existing[item.id]
            if (local == null || item.updatedAt > local.updatedAt) {
                tasbihDao.insertSession(
                    TasbihSessionEntity(
                        id = item.id,
                        presetId = item.presetId,
                        presetName = item.presetName,
                        date = item.date,
                        currentCount = item.currentCount,
                        targetCount = item.targetCount,
                        totalLaps = item.totalLaps,
                        isCompleted = item.isCompleted,
                        duration = item.duration,
                        startedAt = item.startedAt,
                        completedAt = item.completedAt,
                        note = item.note,
                        updatedAt = item.updatedAt
                    )
                )
            }
        }
    }

    // --- Khatam ---

    private suspend fun importKhatams(incoming: List<SyncKhatam>) {
        val existing = khatamDao.getAllKhatamsSync().associateBy { it.id }
        for (item in incoming) {
            val local = existing[item.id]
            if (local == null || item.updatedAt > local.updatedAt) {
                if (local != null) {
                    khatamDao.updateKhatam(
                        KhatamEntity(
                            id = item.id,
                            name = item.name,
                            notes = item.notes,
                            status = item.status,
                            isActive = item.isActive,
                            dailyTarget = item.dailyTarget,
                            deadline = item.deadline,
                            reminderEnabled = item.reminderEnabled,
                            reminderTime = item.reminderTime,
                            totalAyahsRead = item.totalAyahsRead,
                            createdAt = item.createdAt,
                            startedAt = item.startedAt,
                            completedAt = item.completedAt,
                            updatedAt = item.updatedAt
                        )
                    )
                } else {
                    khatamDao.insertKhatam(
                        KhatamEntity(
                            id = item.id,
                            name = item.name,
                            notes = item.notes,
                            status = item.status,
                            isActive = item.isActive,
                            dailyTarget = item.dailyTarget,
                            deadline = item.deadline,
                            reminderEnabled = item.reminderEnabled,
                            reminderTime = item.reminderTime,
                            totalAyahsRead = item.totalAyahsRead,
                            createdAt = item.createdAt,
                            startedAt = item.startedAt,
                            completedAt = item.completedAt,
                            updatedAt = item.updatedAt
                        )
                    )
                }
            }
        }
    }

    private suspend fun importKhatamAyahs(incoming: List<SyncKhatamAyah>) {
        val entities = incoming.map {
            KhatamAyahEntity(
                khatamId = it.khatamId,
                ayahId = it.ayahId,
                readAt = it.readAt,
                updatedAt = it.updatedAt
            )
        }
        if (entities.isNotEmpty()) khatamDao.insertAyahs(entities)
    }

    private suspend fun importKhatamDailyLogs(incoming: List<SyncKhatamDailyLog>) {
        for (item in incoming) {
            val local = khatamDao.getDailyLog(item.khatamId, item.date)
            if (local == null || item.updatedAt > local.updatedAt) {
                khatamDao.upsertDailyLog(
                    KhatamDailyLogEntity(
                        khatamId = item.khatamId,
                        date = item.date,
                        ayahsRead = item.ayahsRead,
                        updatedAt = item.updatedAt
                    )
                )
            }
        }
    }

    // --- Tafseer ---

    private suspend fun importTafseerHighlights(incoming: List<SyncTafseerHighlight>) {
        val existing = tafseerDao.getAllHighlightsSync().associateBy { it.id }
        val toInsert = mutableListOf<TafseerHighlightEntity>()
        for (item in incoming) {
            val local = existing[item.id]
            if (local == null || item.updatedAt > local.updatedAt) {
                toInsert.add(
                    TafseerHighlightEntity(
                        id = item.id,
                        ayahId = item.ayahId,
                        tafseerId = item.tafseerId,
                        startOffset = item.startOffset,
                        endOffset = item.endOffset,
                        color = item.color,
                        note = item.note,
                        createdAt = item.createdAt,
                        updatedAt = item.updatedAt
                    )
                )
            }
        }
        if (toInsert.isNotEmpty()) tafseerDao.insertHighlights(toInsert)
    }

    private suspend fun importTafseerNotes(incoming: List<SyncTafseerNote>) {
        val existing = tafseerDao.getAllNotesSync().associateBy { it.id }
        val toInsert = mutableListOf<TafseerNoteEntity>()
        for (item in incoming) {
            val local = existing[item.id]
            if (local == null || item.updatedAt > local.updatedAt) {
                toInsert.add(
                    TafseerNoteEntity(
                        id = item.id,
                        ayahId = item.ayahId,
                        tafseerId = item.tafseerId,
                        text = item.text,
                        createdAt = item.createdAt,
                        updatedAt = item.updatedAt
                    )
                )
            }
        }
        if (toInsert.isNotEmpty()) tafseerDao.insertNotes(toInsert)
    }

    // --- Zakat ---

    private suspend fun importZakatHistory(incoming: List<SyncZakatHistory>) {
        val existing = zakatDao.getAllHistorySync().associateBy { it.id }
        val toInsert = mutableListOf<ZakatHistoryEntity>()
        for (item in incoming) {
            val local = existing[item.id]
            if (local == null || item.updatedAt > local.updatedAt) {
                toInsert.add(
                    ZakatHistoryEntity(
                        id = item.id,
                        calculatedAt = item.calculatedAt,
                        totalAssets = item.totalAssets,
                        totalLiabilities = item.totalLiabilities,
                        netWorth = item.netWorth,
                        zakatDue = item.zakatDue,
                        nisabType = item.nisabType,
                        nisabValue = item.nisabValue,
                        isPaid = item.isPaid,
                        paidAt = item.paidAt,
                        notes = item.notes,
                        updatedAt = item.updatedAt
                    )
                )
            }
        }
        if (toInsert.isNotEmpty()) zakatDao.insertCalculations(toInsert)
    }
}
