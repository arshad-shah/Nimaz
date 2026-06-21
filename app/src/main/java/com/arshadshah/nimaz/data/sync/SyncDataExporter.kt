package com.arshadshah.nimaz.data.sync

import com.arshadshah.nimaz.data.local.database.dao.AsmaUlHusnaDao
import com.arshadshah.nimaz.data.local.database.dao.AsmaUnNabiDao
import com.arshadshah.nimaz.data.local.database.dao.DuaDao
import com.arshadshah.nimaz.data.local.database.dao.FastingDao
import com.arshadshah.nimaz.data.local.database.dao.HadithDao
import com.arshadshah.nimaz.data.local.database.dao.KhatamDao
import com.arshadshah.nimaz.data.local.database.dao.PrayerDao
import com.arshadshah.nimaz.data.local.database.dao.ProphetDao
import com.arshadshah.nimaz.data.local.database.dao.QaidaDao
import com.arshadshah.nimaz.data.local.database.dao.QuranDao
import com.arshadshah.nimaz.data.local.database.dao.TafseerDao
import com.arshadshah.nimaz.data.local.database.dao.TasbihDao
import com.arshadshah.nimaz.data.local.database.dao.ZakatDao
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncDataExporter @Inject constructor(
    private val quranDao: QuranDao,
    private val prayerDao: PrayerDao,
    private val fastingDao: FastingDao,
    private val tasbihDao: TasbihDao,
    private val khatamDao: KhatamDao,
    private val tafseerDao: TafseerDao,
    private val zakatDao: ZakatDao,
    private val asmaUlHusnaDao: AsmaUlHusnaDao,
    private val asmaUnNabiDao: AsmaUnNabiDao,
    private val prophetDao: ProphetDao,
    private val hadithDao: HadithDao,
    private val duaDao: DuaDao,
    private val qaidaDao: QaidaDao,
    private val preferencesDataStore: PreferencesDataStore
) {
    suspend fun export(onProgress: suspend (String) -> Unit = {}): SyncPayload {
        // Quran
        onProgress("Exporting Quran data...")
        val bookmarks = quranDao.getAllBookmarksSync().map {
            SyncBookmark(
                it.id,
                it.ayahId,
                it.surahNumber,
                it.ayahNumber,
                it.note,
                it.color,
                it.createdAt,
                it.updatedAt
            )
        }
        val favorites = quranDao.getAllFavoritesSync().map {
            SyncFavorite(it.ayahId, it.surahNumber, it.ayahNumber, it.createdAt, it.updatedAt)
        }
        val progress = quranDao.getReadingProgressSync()?.let {
            SyncReadingProgress(
                it.lastReadSurah,
                it.lastReadAyah,
                it.lastReadPage,
                it.lastReadJuz,
                it.totalAyahsRead,
                it.currentKhatmaCount,
                it.updatedAt
            )
        }

        // Prayer & Fasting
        onProgress("Exporting prayer records...")
        val prayerRecords = prayerDao.getAllPrayerRecords().map {
            SyncPrayerRecord(
                it.id,
                it.date,
                it.prayerName,
                it.status,
                it.prayedAt,
                it.scheduledTime,
                it.isJamaah,
                it.isQadaFor,
                it.note,
                it.createdAt,
                it.updatedAt
            )
        }

        onProgress("Exporting fasting records...")
        val fastRecords = fastingDao.getAllFastRecords().map {
            SyncFastRecord(
                it.id,
                it.date,
                it.hijriDate,
                it.hijriMonth,
                it.hijriYear,
                it.fastType,
                it.status,
                it.exemptionReason,
                it.suhoorTime,
                it.iftarTime,
                it.note,
                it.createdAt,
                it.updatedAt
            )
        }
        val makeupFasts = fastingDao.getAllMakeupFastsSync().map {
            SyncMakeupFast(
                it.id,
                it.originalDate,
                it.originalHijriDate,
                it.reason,
                it.status,
                it.completedDate,
                it.fidyaAmount,
                it.note,
                it.createdAt,
                it.updatedAt
            )
        }

        // Tasbih
        onProgress("Exporting tasbih data...")
        val presets = tasbihDao.getAllPresetsSync().map {
            SyncTasbihPreset(
                it.id,
                it.name,
                it.arabic,
                it.transliteration,
                it.translation,
                it.targetCount,
                it.isCustom,
                it.displayOrder,
                it.updatedAt
            )
        }
        val sessions = tasbihDao.getAllSessionsSync().map {
            SyncTasbihSession(
                it.id,
                it.presetId,
                it.presetName,
                it.date,
                it.currentCount,
                it.targetCount,
                it.totalLaps,
                it.isCompleted,
                it.duration,
                it.startedAt,
                it.completedAt,
                it.note,
                it.updatedAt
            )
        }

        // Khatam
        onProgress("Exporting khatam data...")
        val khatams = khatamDao.getAllKhatamsSync().map {
            SyncKhatam(
                it.id,
                it.name,
                it.notes,
                it.status,
                it.isActive,
                it.dailyTarget,
                it.deadline,
                it.reminderEnabled,
                it.reminderTime,
                it.totalAyahsRead,
                it.createdAt,
                it.startedAt,
                it.completedAt,
                it.updatedAt
            )
        }
        val allKhatamAyahs = mutableListOf<SyncKhatamAyah>()
        val allKhatamLogs = mutableListOf<SyncKhatamDailyLog>()
        for (khatam in khatams) {
            khatamDao.getKhatamAyahsSync(khatam.id).mapTo(allKhatamAyahs) {
                SyncKhatamAyah(it.khatamId, it.ayahId, it.readAt, it.updatedAt)
            }
            khatamDao.getDailyLogsSync(khatam.id).mapTo(allKhatamLogs) {
                SyncKhatamDailyLog(it.khatamId, it.date, it.ayahsRead, it.updatedAt)
            }
        }

        // Tafseer & Zakat
        onProgress("Exporting tafseer & zakat data...")
        val highlights = tafseerDao.getAllHighlightsSync().map {
            SyncTafseerHighlight(
                it.id,
                it.ayahId,
                it.tafseerId,
                it.startOffset,
                it.endOffset,
                it.color,
                it.note,
                it.createdAt,
                it.updatedAt
            )
        }
        val notes = tafseerDao.getAllNotesSync().map {
            SyncTafseerNote(it.id, it.ayahId, it.tafseerId, it.text, it.createdAt, it.updatedAt)
        }
        val zakatHistory = zakatDao.getAllHistorySync().map {
            SyncZakatHistory(
                it.id,
                it.calculatedAt,
                it.totalAssets,
                it.totalLiabilities,
                it.netWorth,
                it.zakatDue,
                it.nisabType,
                it.nisabValue,
                it.isPaid,
                it.paidAt,
                it.notes,
                it.updatedAt
            )
        }

        // Names & Prophets favorites
        onProgress("Exporting saved names & prophets...")
        val asmaUlHusnaBookmarks = asmaUlHusnaDao.getAllBookmarksSync().map {
            SyncNameBookmark(it.id, it.nameId, it.isFavorite, it.createdAt)
        }
        val asmaUnNabiBookmarks = asmaUnNabiDao.getAllBookmarksSync().map {
            SyncNameBookmark(it.id, it.nameId, it.isFavorite, it.createdAt)
        }
        val prophetBookmarks = prophetDao.getAllBookmarksSync().map {
            SyncNameBookmark(it.id, it.prophetId, it.isFavorite, it.createdAt)
        }

        // Hadith & Dua bookmarks
        onProgress("Exporting hadith & dua bookmarks...")
        val hadithBookmarks = hadithDao.getAllBookmarksSync().map {
            SyncHadithBookmark(
                it.id,
                it.hadithId,
                it.bookId,
                it.hadithNumber,
                it.note,
                it.color,
                it.createdAt,
                it.updatedAt
            )
        }
        val duaBookmarks = duaDao.getAllBookmarksSync().map {
            SyncDuaBookmark(
                it.id,
                it.duaId,
                it.categoryId,
                it.note,
                it.isFavorite,
                it.createdAt,
                it.updatedAt
            )
        }
        val duaProgress = duaDao.getAllProgressSync().map {
            SyncDuaProgress(
                it.id,
                it.duaId,
                it.date,
                it.completedCount,
                it.targetCount,
                it.isCompleted,
                it.createdAt
            )
        }

        // Qaida learning progress
        onProgress("Exporting Qaida progress...")
        val qaidaLessonProgress = qaidaDao.getAllLessonProgressSync().map {
            SyncQaidaLessonProgress(
                it.lessonId,
                it.status,
                it.stars,
                it.lastCellId,
                it.completedCells,
                it.totalCells,
                it.updatedAt
            )
        }
        val qaidaCellProgress = qaidaDao.getAllCellProgressSync().map {
            SyncQaidaCellProgress(
                it.lessonId,
                it.cellId,
                it.heardCount,
                it.isCompleted,
                it.lastPracticedAt
            )
        }

        // Preferences
        onProgress("Exporting preferences...")
        val preferences = preferencesDataStore.exportAllPreferences()

        return SyncPayload(
            exportedAt = System.currentTimeMillis(),
            appVersion = 11,
            bookmarks = bookmarks,
            favorites = favorites,
            readingProgress = progress,
            prayerRecords = prayerRecords,
            fastRecords = fastRecords,
            makeupFasts = makeupFasts,
            tasbihPresets = presets,
            tasbihSessions = sessions,
            khatams = khatams,
            khatamAyahs = allKhatamAyahs,
            khatamDailyLogs = allKhatamLogs,
            tafseerHighlights = highlights,
            tafseerNotes = notes,
            zakatHistory = zakatHistory,
            asmaUlHusnaBookmarks = asmaUlHusnaBookmarks,
            asmaUnNabiBookmarks = asmaUnNabiBookmarks,
            prophetBookmarks = prophetBookmarks,
            hadithBookmarks = hadithBookmarks,
            duaBookmarks = duaBookmarks,
            duaProgress = duaProgress,
            qaidaLessonProgress = qaidaLessonProgress,
            qaidaCellProgress = qaidaCellProgress,
            preferences = preferences
        )
    }
}
