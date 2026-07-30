package com.arshadshah.nimaz.data.sync

import com.arshadshah.nimaz.data.local.database.dao.AsmaUlHusnaDao
import com.arshadshah.nimaz.data.local.database.dao.AsmaUnNabiDao
import com.arshadshah.nimaz.data.local.database.dao.DuaDao
import com.arshadshah.nimaz.data.local.database.dao.FastingDao
import com.arshadshah.nimaz.data.local.user.ProgressDao
import com.arshadshah.nimaz.data.local.user.ProgressKind
import com.arshadshah.nimaz.data.local.user.ReadingProgressDao
import com.arshadshah.nimaz.data.local.user.BookmarkDao
import com.arshadshah.nimaz.data.local.user.BookmarkEntity
import com.arshadshah.nimaz.data.local.user.BookmarkKind
import com.arshadshah.nimaz.data.local.database.dao.HadithDao
import com.arshadshah.nimaz.data.local.database.dao.KhatamDao
import com.arshadshah.nimaz.data.local.database.dao.LocationDao
import com.arshadshah.nimaz.data.local.database.dao.PrayerDao
import com.arshadshah.nimaz.data.local.database.dao.ProphetDao
import com.arshadshah.nimaz.data.local.database.dao.QaidaDao
import com.arshadshah.nimaz.data.local.database.dao.QuranDao
import com.arshadshah.nimaz.data.local.database.dao.TafseerDao
import com.arshadshah.nimaz.data.local.database.dao.TasbihDao
import com.arshadshah.nimaz.data.local.user.TafseerUserDao
import com.arshadshah.nimaz.data.local.user.TasbihSessionDao
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
    private val sessionDao: TasbihSessionDao,
    private val khatamDao: KhatamDao,
    private val tafseerDao: TafseerDao,
    private val tafseerUserDao: TafseerUserDao,
    private val zakatDao: ZakatDao,
    private val asmaUlHusnaDao: AsmaUlHusnaDao,
    private val asmaUnNabiDao: AsmaUnNabiDao,
    private val prophetDao: ProphetDao,
    private val hadithDao: HadithDao,
    private val bookmarkDao: BookmarkDao,
    private val progressDao: ProgressDao,
    private val readingProgressDao: ReadingProgressDao,
    private val duaDao: DuaDao,
    private val qaidaDao: QaidaDao,
    private val locationDao: LocationDao,
    private val preferencesDataStore: PreferencesDataStore
) {
    suspend fun export(onProgress: suspend (String) -> Unit = {}): SyncPayload {
        // Quran
        onProgress("Exporting Quran data...")
        // The wire format is unchanged on purpose — a phone on this version has to sync with
        // one that still keeps seven bookmark tables — so the consolidated rows are read back
        // out into the shapes the payload has always had. A verse that is both bookmarked and
        // favourited appears in both lists, exactly as it did when it was a row in each table.
        val marks = bookmarkDao.all()
        val bookmarks = marks.filter { it.kind == BookmarkKind.AYAH && it.bookmarked }.map {
            SyncBookmark(
                0,
                it.targetId,
                it.contextId ?: 0,
                it.ordinal ?: 0,
                it.note,
                it.colour,
                it.createdAt,
                it.updatedAt
            )
        }
        val favorites = marks.filter { it.kind == BookmarkKind.AYAH && it.favourite }.map {
            SyncFavorite(it.targetId, it.contextId ?: 0, it.ordinal ?: 0, it.createdAt, it.updatedAt)
        }
        val progress = readingProgressDao.get()?.let {
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
        val sessions = sessionDao.getAllSessionsSync().map {
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
        val highlights = tafseerUserDao.getAllHighlightsSync().map {
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
        val notes = tafseerUserDao.getAllNotesSync().map {
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
        val asmaUlHusnaBookmarks = marks.filter { it.kind == BookmarkKind.ASMA_UL_HUSNA }.map {
            SyncNameBookmark(0, it.targetId, it.favourite, it.createdAt)
        }
        val asmaUnNabiBookmarks = marks.filter { it.kind == BookmarkKind.ASMA_UN_NABI }.map {
            SyncNameBookmark(0, it.targetId, it.favourite, it.createdAt)
        }
        val prophetBookmarks = marks.filter { it.kind == BookmarkKind.PROPHET }.map {
            SyncNameBookmark(0, it.targetId, it.favourite, it.createdAt)
        }

        // Hadith & Dua bookmarks
        onProgress("Exporting hadith & dua bookmarks...")
        // The payload shape is unchanged on purpose: a phone on this version has to be able
        // to sync with one that still has seven bookmark tables, so the wire format keeps the
        // old field names and the consolidated row is mapped onto them.
        val hadithBookmarks = bookmarkDao.all()
            .filter { it.kind == BookmarkKind.HADITH }
            .map {
                SyncHadithBookmark(
                    0,
                    it.targetId,
                    it.contextId ?: 0,
                    it.ordinal ?: 0,
                    it.note,
                    it.colour,
                    it.createdAt,
                    it.updatedAt
                )
            }
        val duaBookmarks = marks.filter { it.kind == BookmarkKind.DUA }.map {
            SyncDuaBookmark(
                0,
                it.targetId,
                it.contextId ?: 0,
                it.note,
                it.favourite,
                it.createdAt,
                it.updatedAt
            )
        }
        val counts = progressDao.all()
        val duaProgress = counts.filter { it.kind == ProgressKind.DUA }.map {
            SyncDuaProgress(
                0,
                it.targetId,
                it.date,
                it.completed,
                it.total ?: 0,
                it.isCompleted,
                it.createdAt
            )
        }

        // Qaida learning progress
        onProgress("Exporting Qaida progress...")
        val qaidaLessonProgress = counts.filter { it.kind == ProgressKind.QAIDA_LESSON }.map {
            SyncQaidaLessonProgress(
                it.targetId,
                it.state.orEmpty(),
                it.score ?: 0,
                it.resumeId,
                it.completed,
                it.total ?: 0,
                it.updatedAt
            )
        }
        val qaidaCellProgress = counts.filter { it.kind == ProgressKind.QAIDA_CELL }.map {
            SyncQaidaCellProgress(
                it.contextId ?: 0,
                it.targetId,
                it.completed,
                it.isCompleted,
                it.updatedAt
            )
        }

        // Saved (favorite) locations only — current-location flag is not carried.
        onProgress("Exporting saved locations...")
        val favoriteLocations = locationDao.getFavoriteLocationsSync().map {
            SyncLocation(
                it.name,
                it.latitude,
                it.longitude,
                it.timezone,
                it.country,
                it.city,
                it.calculationMethod,
                it.asrCalculation,
                it.highLatitudeRule,
                it.fajrAngle,
                it.ishaAngle,
                it.createdAt,
                it.updatedAt
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
            favoriteLocations = favoriteLocations,
            preferences = preferences
        )
    }
}
