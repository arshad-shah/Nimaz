package com.arshadshah.nimaz.data.sync

import com.arshadshah.nimaz.data.local.database.NimazDatabase
import com.arshadshah.nimaz.data.local.database.dao.AsmaUlHusnaDao
import com.arshadshah.nimaz.data.local.database.dao.AsmaUnNabiDao
import com.arshadshah.nimaz.data.local.database.dao.DuaDao
import com.arshadshah.nimaz.data.local.database.dao.FastingDao
import com.arshadshah.nimaz.data.local.user.ProgressDao
import com.arshadshah.nimaz.data.local.user.ProgressEntity
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
import com.arshadshah.nimaz.core.datastore.PreferencesDataStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What makes a tafseer highlight *the same highlight* on two devices: the span it covers of a
 * given commentary. The row id cannot — it is `autoGenerate`, so both phones hand out 1, 2, 3…
 */
private data class HighlightKey(
    val ayahId: Int,
    val tafseerId: String,
    val startOffset: Int,
    val endOffset: Int
)

@Singleton
class SyncDataImporter @Inject constructor(
    private val database: NimazDatabase,
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
        importNamesData(payload)
        importHadithDuaData(payload)
        importQaidaData(payload)
        importLocationsData(payload)
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
        // The parents first: they hand back sender-id → local-id so the children attach to
        // the right khatam rather than to whatever locally holds the sender's id.
        val khatamIds = importKhatams(payload.khatams)
        importKhatamAyahs(payload.khatamAyahs, khatamIds)
        importKhatamDailyLogs(payload.khatamDailyLogs, khatamIds)
    }

    suspend fun importTafseerData(payload: SyncPayload) {
        importTafseerHighlights(payload.tafseerHighlights)
        importTafseerNotes(payload.tafseerNotes)
    }

    suspend fun importZakatData(payload: SyncPayload) {
        importZakatHistory(payload.zakatHistory)
    }

    suspend fun importNamesData(payload: SyncPayload) {
        importAsmaUlHusnaBookmarks(payload.asmaUlHusnaBookmarks)
        importAsmaUnNabiBookmarks(payload.asmaUnNabiBookmarks)
        importProphetBookmarks(payload.prophetBookmarks)
    }

    suspend fun importHadithDuaData(payload: SyncPayload) {
        importHadithBookmarks(payload.hadithBookmarks)
        importDuaBookmarks(payload.duaBookmarks)
        importDuaProgress(payload.duaProgress)
    }

    suspend fun importQaidaData(payload: SyncPayload) {
        importQaidaLessonProgress(payload.qaidaLessonProgress)
        importQaidaCellProgress(payload.qaidaCellProgress)
    }

    suspend fun importLocationsData(payload: SyncPayload) {
        importFavoriteLocations(payload.favoriteLocations)
    }

    suspend fun importPreferencesData(payload: SyncPayload) {
        if (payload.preferences.isNotEmpty()) {
            preferencesDataStore.importPreferences(payload.preferences)
        }
    }

    // --- Quran ---

    /**
     * Incoming Quran bookmarks, merged onto the consolidated row.
     *
     * Bookmarking an ayah and favouriting it are two independent acts that share one row, and
     * the row carries a single `updatedAt`. `favourite` is therefore carried over from whatever
     * is already here rather than defaulted — the payload has no field for it, so writing the
     * row blind would silently un-favourite a verse this device had marked. Same reasoning in
     * every kind below.
     *
     * Gating the whole write on `item.updatedAt > local.updatedAt` therefore dropped whichever
     * act happened *earlier*, though nothing about it conflicted: favourite on Monday, bookmark
     * on Tuesday, sync — `importBookmarks` stamps the row Tuesday and `importFavorites` then
     * sees a newer local row and skips, losing the favourite. The mirror case needs no ordering
     * at all: an incoming bookmark older than a local favourite was simply discarded.
     *
     * The payload carries no tombstones — it lists what the sending device *has* — so the merge
     * is additive. A flag set on either side stays set; the timestamp decides only whose note
     * and colour win.
     */
    private suspend fun importBookmarks(incoming: List<SyncBookmark>) {
        val existing = bookmarkDao.all()
            .filter { it.kind == BookmarkKind.AYAH }
            .associateBy { it.targetId }
        for (item in incoming) {
            val local = existing[item.ayahId]
            val incomingIsNewer = local == null || item.updatedAt > local.updatedAt
            bookmarkDao.upsert(
                BookmarkEntity(
                    kind = BookmarkKind.AYAH,
                    targetId = item.ayahId,
                    bookmarked = true,
                    favourite = local?.favourite ?: false,
                    // The note and colour belong to the bookmark, so they follow the timestamp.
                    note = if (incomingIsNewer) item.note else local?.note,
                    colour = if (incomingIsNewer) item.color else local?.colour,
                    contextId = item.surahNumber,
                    ordinal = item.ayahNumber,
                    createdAt = minOf(item.createdAt, local?.createdAt ?: item.createdAt),
                    updatedAt = maxOf(item.updatedAt, local?.updatedAt ?: item.updatedAt)
                )
            )
        }
    }

    /** The favourite half of the merge described on [importBookmarks]. */
    private suspend fun importFavorites(incoming: List<SyncFavorite>) {
        val existing = bookmarkDao.all()
            .filter { it.kind == BookmarkKind.AYAH }
            .associateBy { it.targetId }
        for (item in incoming) {
            val local = existing[item.ayahId]
            bookmarkDao.upsert(
                BookmarkEntity(
                    kind = BookmarkKind.AYAH,
                    targetId = item.ayahId,
                    // A favourite arriving must not clear a bookmark, or its note and colour
                    // with it — it carries neither of its own.
                    bookmarked = local?.bookmarked ?: false,
                    favourite = true,
                    note = local?.note,
                    colour = local?.colour,
                    contextId = item.surahNumber,
                    ordinal = item.ayahNumber,
                    createdAt = minOf(item.createdAt, local?.createdAt ?: item.createdAt),
                    updatedAt = maxOf(item.updatedAt, local?.updatedAt ?: item.updatedAt)
                )
            )
        }
    }

    private suspend fun importReadingProgress(incoming: SyncReadingProgress?) {
        incoming ?: return
        val local = readingProgressDao.get()
        if (local == null || incoming.updatedAt > local.updatedAt) {
            readingProgressDao.upsert(
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
        val existing = fastingDao.getAllMakeupFastsSync().associateBy { it.originalDate }
        for (item in incoming) {
            val local = existing[item.originalDate]
            if (local == null || item.updatedAt > local.updatedAt) {
                fastingDao.insertMakeupFast(
                    MakeupFastEntity(
                        id = local?.id ?: 0,
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
        val existing = tasbihDao.getAllPresetsSync().associateBy { it.name }
        for (item in incoming) {
            val local = existing[item.name]
            if (local == null || item.updatedAt > local.updatedAt) {
                tasbihDao.insertPreset(
                    TasbihPresetEntity(
                        id = local?.id ?: 0,
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
        val existing = sessionDao.getAllSessionsSync().associateBy { it.startedAt }
        for (item in incoming) {
            val local = existing[item.startedAt]
            if (local == null || item.updatedAt > local.updatedAt) {
                sessionDao.insertSession(
                    TasbihSessionEntity(
                        id = local?.id ?: 0,
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

    /**
     * Khatams, matched by **when the khatam was created**, not by row id.
     *
     * `KhatamEntity.id` is Room's `autoGenerate` key, so two phones that have both been used
     * each hold a khatam with id 1. Merging on it meant an incoming khatam overwrote whatever
     * unrelated khatam held the same local id — or, when the incoming one was older, was
     * dropped so nothing arrived at all. `createdAt` is the instant the user started that
     * khatam, which is stable across devices and unique in practice.
     *
     * Returns **sender id → local id**, because khatam ayahs and daily logs reference the
     * parent and anything genuinely new is inserted under a fresh local id.
     */
    private suspend fun importKhatams(incoming: List<SyncKhatam>): Map<Long, Long> {
        val existing = khatamDao.getAllKhatamsSync().associateBy { it.createdAt }
        val localIdBySenderId = mutableMapOf<Long, Long>()
        for (item in incoming) {
            val local = existing[item.createdAt]
            val entity = KhatamEntity(
                // 0 lets Room assign; never reuse the sender's id, which may already belong
                // to a different khatam here.
                id = local?.id ?: 0,
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
            localIdBySenderId[item.id] = when {
                local == null -> khatamDao.insertKhatam(entity)
                item.updatedAt > local.updatedAt -> {
                    khatamDao.updateKhatam(entity)
                    local.id
                }
                // Older than what is here: keep ours, but still map the id so this khatam's
                // ayahs and logs land on the right parent.
                else -> local.id
            }
        }
        return localIdBySenderId
    }

    /**
     * @param localIdBySenderId from [importKhatams]. Without it these rows carried the
     *   *sender's* khatam id, so another device's read ayahs attached to whichever local
     *   khatam happened to hold that id and inflated its progress. A row whose parent is not
     *   in the map has no khatam to belong to and is dropped.
     */
    private suspend fun importKhatamAyahs(
        incoming: List<SyncKhatamAyah>,
        localIdBySenderId: Map<Long, Long>
    ) {
        val entities = incoming.mapNotNull { item ->
            val khatamId = localIdBySenderId[item.khatamId] ?: return@mapNotNull null
            KhatamAyahEntity(
                khatamId = khatamId,
                ayahId = item.ayahId,
                readAt = item.readAt,
                updatedAt = item.updatedAt
            )
        }
        if (entities.isNotEmpty()) khatamDao.insertAyahs(entities)
    }

    /** @param localIdBySenderId see [importKhatamAyahs]. */
    private suspend fun importKhatamDailyLogs(
        incoming: List<SyncKhatamDailyLog>,
        localIdBySenderId: Map<Long, Long>
    ) {
        for (item in incoming) {
            val khatamId = localIdBySenderId[item.khatamId] ?: continue
            val local = khatamDao.getDailyLog(khatamId, item.date)
            if (local == null || item.updatedAt > local.updatedAt) {
                khatamDao.upsertDailyLog(
                    KhatamDailyLogEntity(
                        khatamId = khatamId,
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
        val existing = tafseerUserDao.getAllHighlightsSync()
            .associateBy { HighlightKey(it.ayahId, it.tafseerId, it.startOffset, it.endOffset) }
        val toInsert = mutableListOf<TafseerHighlightEntity>()
        for (item in incoming) {
            val local = existing[
                HighlightKey(item.ayahId, item.tafseerId, item.startOffset, item.endOffset)
            ]
            if (local == null || item.updatedAt > local.updatedAt) {
                toInsert.add(
                    TafseerHighlightEntity(
                        id = local?.id ?: 0,
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
        if (toInsert.isNotEmpty()) tafseerUserDao.insertHighlights(toInsert)
    }

    private suspend fun importTafseerNotes(incoming: List<SyncTafseerNote>) {
        val existing = tafseerUserDao.getAllNotesSync()
            .associateBy { Triple(it.ayahId, it.tafseerId, it.createdAt) }
        val toInsert = mutableListOf<TafseerNoteEntity>()
        for (item in incoming) {
            val local = existing[Triple(item.ayahId, item.tafseerId, item.createdAt)]
            if (local == null || item.updatedAt > local.updatedAt) {
                toInsert.add(
                    TafseerNoteEntity(
                        id = local?.id ?: 0,
                        ayahId = item.ayahId,
                        tafseerId = item.tafseerId,
                        text = item.text,
                        createdAt = item.createdAt,
                        updatedAt = item.updatedAt
                    )
                )
            }
        }
        if (toInsert.isNotEmpty()) tafseerUserDao.insertNotes(toInsert)
    }

    // --- Zakat ---

    private suspend fun importZakatHistory(incoming: List<SyncZakatHistory>) {
        val existing = zakatDao.getAllHistorySync().associateBy { it.calculatedAt }
        val toInsert = mutableListOf<ZakatHistoryEntity>()
        for (item in incoming) {
            val local = existing[item.calculatedAt]
            if (local == null || item.updatedAt > local.updatedAt) {
                toInsert.add(
                    ZakatHistoryEntity(
                        id = local?.id ?: 0,
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

    // --- Names & Prophets ---

    /**
     * Merge for the three name catalogues (Asma ul Husna, Asma un Nabi, Prophets), which share
     * one shape: a mark that is always a bookmark, plus an independent favourite flag.
     *
     * This used to skip the row outright whenever the target already existed locally, so a name
     * bookmarked on this device and favourited on the other stayed un-favourited — the incoming
     * favourite had nowhere to land. [SyncNameBookmark] carries no `updatedAt`, so there is no
     * timestamp to arbitrate with and the union is the only merge available: a flag set on
     * either side stays set. That also means an incoming row can never *clear* a local
     * favourite, which is the behaviour the ayah and hadith importers already had.
     */
    private suspend fun importNameBookmarks(incoming: List<SyncNameBookmark>, kind: String) {
        val existing = bookmarkDao.all()
            .filter { it.kind == kind }
            .associateBy { it.targetId }
        for (item in incoming) {
            val local = existing[item.refId]
            bookmarkDao.upsert(
                BookmarkEntity(
                    kind = kind,
                    targetId = item.refId,
                    bookmarked = true,
                    favourite = (local?.favourite ?: false) || item.isFavorite,
                    note = local?.note,
                    colour = local?.colour,
                    contextId = local?.contextId,
                    ordinal = local?.ordinal,
                    createdAt = minOf(item.createdAt, local?.createdAt ?: item.createdAt),
                    updatedAt = maxOf(item.createdAt, local?.updatedAt ?: item.createdAt)
                )
            )
        }
    }

    private suspend fun importAsmaUlHusnaBookmarks(incoming: List<SyncNameBookmark>) =
        importNameBookmarks(incoming, BookmarkKind.ASMA_UL_HUSNA)

    private suspend fun importAsmaUnNabiBookmarks(incoming: List<SyncNameBookmark>) =
        importNameBookmarks(incoming, BookmarkKind.ASMA_UN_NABI)

    private suspend fun importProphetBookmarks(incoming: List<SyncNameBookmark>) =
        importNameBookmarks(incoming, BookmarkKind.PROPHET)

    // --- Hadith & Dua ---

    private suspend fun importHadithBookmarks(incoming: List<SyncHadithBookmark>) {
        val existing = bookmarkDao.all()
            .filter { it.kind == BookmarkKind.HADITH }
            .associateBy { it.targetId }
        for (item in incoming) {
            val local = existing[item.hadithId]
            if (local == null || item.updatedAt > local.updatedAt) {
                bookmarkDao.upsert(
                    BookmarkEntity(
                        kind = BookmarkKind.HADITH,
                        targetId = item.hadithId,
                        bookmarked = true,
                        // A mark that arrives from another device must not clear a favourite
                        // this device set: the wire format has no field for it.
                        favourite = local?.favourite ?: false,
                        note = item.note,
                        colour = item.color,
                        contextId = item.bookId,
                        ordinal = item.hadithNumber,
                        createdAt = item.createdAt,
                        updatedAt = item.updatedAt
                    )
                )
            }
        }
    }

    private suspend fun importDuaBookmarks(incoming: List<SyncDuaBookmark>) {
        val existing = bookmarkDao.all()
            .filter { it.kind == BookmarkKind.DUA }
            .associateBy { it.targetId }
        for (item in incoming) {
            val local = existing[item.duaId]
            if (local == null || item.updatedAt > local.updatedAt) {
                bookmarkDao.upsert(
                    BookmarkEntity(
                        kind = BookmarkKind.DUA,
                        targetId = item.duaId,
                        bookmarked = true,
                        favourite = item.isFavorite,
                        note = item.note,
                        contextId = item.categoryId,
                        createdAt = item.createdAt,
                        updatedAt = item.updatedAt
                    )
                )
            }
        }
    }

    private suspend fun importDuaProgress(incoming: List<SyncDuaProgress>) {
        val existing = progressDao.all()
            .filter { it.kind == ProgressKind.DUA }
            .associateBy { it.targetId to it.date }
        for (item in incoming) {
            val local = existing[item.duaId to item.date]
            if (local == null || item.createdAt > local.updatedAt) {
                progressDao.upsert(
                    ProgressEntity(
                        kind = ProgressKind.DUA,
                        targetId = item.duaId,
                        date = item.date,
                        completed = item.completedCount,
                        total = item.targetCount,
                        isCompleted = item.isCompleted,
                        createdAt = item.createdAt,
                        updatedAt = item.createdAt
                    )
                )
            }
        }
    }

    // --- Qaida ---

    private suspend fun importQaidaLessonProgress(incoming: List<SyncQaidaLessonProgress>) {
        val existing = progressDao.all()
            .filter { it.kind == ProgressKind.QAIDA_LESSON }
            .associateBy { it.targetId }
        for (item in incoming) {
            val local = existing[item.lessonId]
            if (local == null || item.updatedAt > local.updatedAt) {
                progressDao.upsert(
                    ProgressEntity(
                        kind = ProgressKind.QAIDA_LESSON,
                        targetId = item.lessonId,
                        completed = item.completedCells,
                        total = item.totalCells,
                        isCompleted = item.status == "COMPLETED",
                        state = item.status,
                        score = item.stars,
                        resumeId = item.lastCellId,
                        createdAt = item.updatedAt,
                        updatedAt = item.updatedAt
                    )
                )
            }
        }
    }

    private suspend fun importQaidaCellProgress(incoming: List<SyncQaidaCellProgress>) {
        val existing = progressDao.all()
            .filter { it.kind == ProgressKind.QAIDA_CELL }
            .associateBy { it.targetId }
        for (item in incoming) {
            val local = existing[item.cellId]
            if (local == null || item.lastPracticedAt > local.updatedAt) {
                progressDao.upsert(
                    ProgressEntity(
                        kind = ProgressKind.QAIDA_CELL,
                        targetId = item.cellId,
                        contextId = item.lessonId,
                        completed = item.heardCount,
                        isCompleted = item.isCompleted,
                        createdAt = item.lastPracticedAt,
                        updatedAt = item.lastPracticedAt
                    )
                )
            }
        }
    }

    // --- Locations (favorites only; never touches the current location) ---

    private suspend fun importFavoriteLocations(incoming: List<SyncLocation>) {
        // Match on coordinates so a place isn't duplicated across devices.
        val existing = locationDao.getAllLocationsSync()
            .associateBy { it.latitude to it.longitude }
        for (item in incoming) {
            val local = existing[item.latitude to item.longitude]
            if (local != null && item.updatedAt <= local.updatedAt) continue
            locationDao.insertLocation(
                LocationEntity(
                    id = local?.id ?: 0,
                    name = item.name,
                    latitude = item.latitude,
                    longitude = item.longitude,
                    timezone = item.timezone,
                    country = item.country,
                    city = item.city,
                    // Never change which location is "current" on this device.
                    isCurrentLocation = local?.isCurrentLocation ?: false,
                    isFavorite = true,
                    calculationMethod = item.calculationMethod,
                    asrCalculation = item.asrCalculation,
                    highLatitudeRule = item.highLatitudeRule,
                    fajrAngle = item.fajrAngle,
                    ishaAngle = item.ishaAngle,
                    createdAt = item.createdAt,
                    updatedAt = item.updatedAt
                )
            )
        }
    }
}
