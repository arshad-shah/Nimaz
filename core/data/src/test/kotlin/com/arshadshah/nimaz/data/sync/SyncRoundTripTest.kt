package com.arshadshah.nimaz.data.sync

import com.arshadshah.nimaz.core.datastore.PreferencesDataStore
import com.arshadshah.nimaz.data.local.database.dao.AsmaUlHusnaDao
import com.arshadshah.nimaz.data.local.database.dao.AsmaUnNabiDao
import com.arshadshah.nimaz.data.local.database.dao.DuaDao
import com.arshadshah.nimaz.data.local.database.dao.FastingDao
import com.arshadshah.nimaz.data.local.database.dao.HadithDao
import com.arshadshah.nimaz.data.local.database.dao.KhatamDao
import com.arshadshah.nimaz.data.local.database.dao.LocationDao
import com.arshadshah.nimaz.data.local.database.dao.PrayerDao
import com.arshadshah.nimaz.data.local.database.dao.ProphetDao
import com.arshadshah.nimaz.data.local.database.dao.QaidaDao
import com.arshadshah.nimaz.data.local.database.dao.QuranDao
import com.arshadshah.nimaz.data.local.database.dao.TafseerDao
import com.arshadshah.nimaz.data.local.database.dao.TasbihDao
import com.arshadshah.nimaz.data.local.database.dao.ZakatDao
import com.arshadshah.nimaz.data.local.database.NimazDatabase
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
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Export from a populated phone, import into an empty one, and assert the second phone now
 * holds what the first did.
 *
 * A wrong import silently overwrites another device's records and there is no undo, so the two
 * halves are only worth anything together: an exporter that drops a column and an importer that
 * never reads it agree perfectly, and the user loses the data with nothing to see.
 *
 * The properties pinned here are the ones a single-sided test cannot state:
 *
 *  - **row ids never travel.** `id` is `autoGenerate` on both phones, so both have handed out
 *    1, 2, 3… Khatams merge on `createdAt`, prayers on `date`+`prayerName`, fasts on `date`,
 *    presets on `name`, sessions on `startedAt`, highlights on the span they cover, locations
 *    on coordinates. Merging on the id meant an incoming record overwrote an unrelated local
 *    one that happened to hold it;
 *  - **a khatam's children are re-parented** through the sender-id → local-id map the parent
 *    import returns. Without it another device's read verses attached to whichever local khatam
 *    held that id and inflated its progress;
 *  - **the merge is additive, because the payload carries no tombstones.** It lists what the
 *    sending device *has*, so a flag set on either side stays set and nothing is deleted for
 *    being absent.
 */
class SyncRoundTripTest {

    // The sending phone.
    private val srcPrayerDao: PrayerDao = mockk(relaxed = true)
    private val srcFastingDao: FastingDao = mockk(relaxed = true)
    private val srcTasbihDao: TasbihDao = mockk(relaxed = true)
    private val srcSessionDao: TasbihSessionDao = mockk(relaxed = true)
    private val srcKhatamDao: KhatamDao = mockk(relaxed = true)
    private val srcTafseerUserDao: TafseerUserDao = mockk(relaxed = true)
    private val srcZakatDao: ZakatDao = mockk(relaxed = true)
    private val srcBookmarkDao: BookmarkDao = mockk(relaxed = true)
    private val srcProgressDao: ProgressDao = mockk(relaxed = true)
    private val srcReadingProgressDao: ReadingProgressDao = mockk(relaxed = true)
    private val srcLocationDao: LocationDao = mockk(relaxed = true)
    private val srcPreferences: PreferencesDataStore = mockk(relaxed = true)

    // The receiving phone.
    private val prayerDao: PrayerDao = mockk(relaxed = true)
    private val fastingDao: FastingDao = mockk(relaxed = true)
    private val tasbihDao: TasbihDao = mockk(relaxed = true)
    private val sessionDao: TasbihSessionDao = mockk(relaxed = true)
    private val khatamDao: KhatamDao = mockk(relaxed = true)
    private val tafseerUserDao: TafseerUserDao = mockk(relaxed = true)
    private val zakatDao: ZakatDao = mockk(relaxed = true)
    private val bookmarkDao: BookmarkDao = mockk(relaxed = true)
    private val progressDao: ProgressDao = mockk(relaxed = true)
    private val readingProgressDao: ReadingProgressDao = mockk(relaxed = true)
    private val locationDao: LocationDao = mockk(relaxed = true)
    private val preferences: PreferencesDataStore = mockk(relaxed = true)

    private val exporter by lazy {
        SyncDataExporter(
            mockk<QuranDao>(relaxed = true), srcPrayerDao, srcFastingDao, srcTasbihDao,
            srcSessionDao, srcKhatamDao, mockk<TafseerDao>(relaxed = true), srcTafseerUserDao,
            srcZakatDao, mockk<AsmaUlHusnaDao>(relaxed = true), mockk<AsmaUnNabiDao>(relaxed = true),
            mockk<ProphetDao>(relaxed = true), mockk<HadithDao>(relaxed = true), srcBookmarkDao,
            srcProgressDao, srcReadingProgressDao, mockk<DuaDao>(relaxed = true),
            mockk<QaidaDao>(relaxed = true), srcLocationDao, srcPreferences,
        )
    }

    private val importer by lazy {
        SyncDataImporter(
            database = mockk<NimazDatabase>(relaxed = true),
            quranDao = mockk(relaxed = true),
            prayerDao = prayerDao,
            fastingDao = fastingDao,
            tasbihDao = tasbihDao,
            sessionDao = sessionDao,
            khatamDao = khatamDao,
            tafseerDao = mockk(relaxed = true),
            tafseerUserDao = tafseerUserDao,
            zakatDao = zakatDao,
            asmaUlHusnaDao = mockk(relaxed = true),
            asmaUnNabiDao = mockk(relaxed = true),
            prophetDao = mockk(relaxed = true),
            hadithDao = mockk(relaxed = true),
            bookmarkDao = bookmarkDao,
            progressDao = progressDao,
            readingProgressDao = readingProgressDao,
            duaDao = mockk(relaxed = true),
            qaidaDao = mockk(relaxed = true),
            locationDao = locationDao,
            preferencesDataStore = preferences,
        )
    }

    /**
     * The receiving phone's `bookmarks` table, which has to be a **real** store rather than a
     * stub: `importBookmarks` and `importFavorites` both read it and both write it, and the
     * whole point of the merge is that the second one sees what the first wrote.
     */
    private val markStore = mutableListOf<BookmarkEntity>()

    /** Every upsert, in order — the merge is asserted on what was written, not on what is left. */
    private val marks = mutableListOf<BookmarkEntity>()
    private val progressRows = mutableListOf<ProgressEntity>()
    private val locations = mutableListOf<LocationEntity>()
    private val prayerRecords = mutableListOf<List<PrayerRecordEntity>>()
    private val fastRecords = mutableListOf<List<FastRecordEntity>>()
    private val makeupFasts = mutableListOf<MakeupFastEntity>()
    private val presets = mutableListOf<TasbihPresetEntity>()
    private val sessions = mutableListOf<TasbihSessionEntity>()
    private val khatamsInserted = mutableListOf<KhatamEntity>()
    private val khatamsUpdated = mutableListOf<KhatamEntity>()
    private val khatamAyahs = mutableListOf<List<KhatamAyahEntity>>()
    private val dailyLogs = mutableListOf<KhatamDailyLogEntity>()
    private val highlights = mutableListOf<List<TafseerHighlightEntity>>()
    private val notes = mutableListOf<List<TafseerNoteEntity>>()
    private val zakats = mutableListOf<List<ZakatHistoryEntity>>()
    private val readingProgress = mutableListOf<ReadingProgressEntity>()
    private val importedPreferences = mutableListOf<Map<String, String>>()

    @Before
    fun setUp() {
        SyncFixtures.populate(
            prayerDao = srcPrayerDao,
            fastingDao = srcFastingDao,
            tasbihDao = srcTasbihDao,
            sessionDao = srcSessionDao,
            khatamDao = srcKhatamDao,
            tafseerUserDao = srcTafseerUserDao,
            zakatDao = srcZakatDao,
            bookmarkDao = srcBookmarkDao,
            progressDao = srcProgressDao,
            readingProgressDao = srcReadingProgressDao,
            locationDao = srcLocationDao,
            preferences = srcPreferences,
        )
        emptyReceiver()
        recordReceiverWrites()
    }

    /** The receiving phone starts with nothing; individual tests override a table. */
    private fun emptyReceiver() {
        coEvery { bookmarkDao.all() } answers { markStore.toList() }
        coEvery { progressDao.all() } returns emptyList()
        coEvery { readingProgressDao.get() } returns null
        coEvery { prayerDao.getAllPrayerRecords() } returns emptyList()
        coEvery { fastingDao.getAllFastRecords() } returns emptyList()
        coEvery { fastingDao.getAllMakeupFastsSync() } returns emptyList()
        coEvery { tasbihDao.getAllPresetsSync() } returns emptyList()
        coEvery { sessionDao.getAllSessionsSync() } returns emptyList()
        coEvery { khatamDao.getAllKhatamsSync() } returns emptyList()
        coEvery { khatamDao.getDailyLog(any(), any()) } returns null
        coEvery { tafseerUserDao.getAllHighlightsSync() } returns emptyList()
        coEvery { tafseerUserDao.getAllNotesSync() } returns emptyList()
        coEvery { zakatDao.getAllHistorySync() } returns emptyList()
        coEvery { locationDao.getAllLocationsSync() } returns emptyList()
    }

    private fun recordReceiverWrites() {
        coEvery { bookmarkDao.upsert(any()) } answers {
            val row = firstArg<BookmarkEntity>()
            markStore.removeAll { it.kind == row.kind && it.targetId == row.targetId }
            markStore += row
            marks += row
        }
        coEvery { progressDao.upsert(capture(progressRows)) } returns Unit
        coEvery { locationDao.insertLocation(capture(locations)) } returns 1L
        coEvery { prayerDao.insertPrayerRecords(capture(prayerRecords)) } returns Unit
        coEvery { fastingDao.insertFastRecords(capture(fastRecords)) } returns Unit
        coEvery { fastingDao.insertMakeupFast(capture(makeupFasts)) } returns Unit
        coEvery { tasbihDao.insertPreset(capture(presets)) } returns 1L
        coEvery { sessionDao.insertSession(capture(sessions)) } returns 1L
        // Room hands out fresh local ids; the sender's are never reused.
        coEvery { khatamDao.insertKhatam(capture(khatamsInserted)) } answers
            { 100L + khatamsInserted.size }
        coEvery { khatamDao.updateKhatam(capture(khatamsUpdated)) } returns Unit
        coEvery { khatamDao.insertAyahs(capture(khatamAyahs)) } returns Unit
        coEvery { khatamDao.upsertDailyLog(capture(dailyLogs)) } returns Unit
        coEvery { tafseerUserDao.insertHighlights(capture(highlights)) } returns Unit
        coEvery { tafseerUserDao.insertNotes(capture(notes)) } returns Unit
        coEvery { zakatDao.insertCalculations(capture(zakats)) } returns Unit
        coEvery { readingProgressDao.upsert(capture(readingProgress)) } returns Unit
        coEvery { preferences.importPreferences(capture(importedPreferences)) } returns Unit
    }

    private suspend fun roundTrip() = importer.import(exporter.export())

    // ── every category survives the trip ──────────────────────────────────────

    @Test
    fun `an empty phone ends up holding what the sending phone had`() = runTest {
        roundTrip()

        assertThat(marks.filter { it.kind == BookmarkKind.AYAH }.map { it.targetId })
            .containsAtLeast(SyncFixtures.BOTH_AYAH, SyncFixtures.FAVOURITE_ONLY_AYAH)
        assertThat(prayerRecords.flatten().map { it.prayerName }).containsExactly("fajr")
        assertThat(fastRecords.flatten().map { it.fastType }).containsExactly("ramadan")
        assertThat(makeupFasts.map { it.reason }).containsExactly("travel")
        assertThat(presets.map { it.name }).containsExactly("SubhanAllah")
        assertThat(sessions.map { it.startedAt }).containsExactly(5_000L)
        assertThat(khatamsInserted.map { it.name }).containsExactly("Khatam 1", "Khatam 2")
        assertThat(highlights.flatten().map { it.tafseerId }).containsExactly("ibn-kathir")
        assertThat(notes.flatten().map { it.text }).containsExactly("note text")
        assertThat(zakats.flatten().map { it.zakatDue }).containsExactly(100.0)
        assertThat(locations.map { it.city }).containsExactly("Dublin")
        assertThat(readingProgress.single().totalAyahsRead).isEqualTo(900)
        assertThat(importedPreferences.single()).containsExactly("theme", "dark")
    }

    @Test
    fun `a verse bookmarked and favourited arrives with both flags set`() = runTest {
        roundTrip()

        val row = marks.last { it.kind == BookmarkKind.AYAH && it.targetId == SyncFixtures.BOTH_AYAH }
        assertThat(row.bookmarked).isTrue()
        assertThat(row.favourite).isTrue()
        assertThat(row.note).isEqualTo("memorise")
    }

    @Test
    fun `a verse that was only favourited does not arrive bookmarked`() = runTest {
        roundTrip()

        val row = marks.last {
            it.kind == BookmarkKind.AYAH && it.targetId == SyncFixtures.FAVOURITE_ONLY_AYAH
        }
        assertThat(row.favourite).isTrue()
        assertThat(row.bookmarked).isFalse()
    }

    @Test
    fun `the three progress kinds arrive under their own kinds`() = runTest {
        roundTrip()

        assertThat(progressRows.map { it.kind })
            .containsAtLeast(ProgressKind.DUA, ProgressKind.QAIDA_LESSON, ProgressKind.QAIDA_CELL)
        val cell = progressRows.last { it.kind == ProgressKind.QAIDA_CELL }
        assertThat(cell.contextId).isEqualTo(3)
        assertThat(cell.completed).isEqualTo(4)
    }

    @Test
    fun `a qaida lesson's completed flag is derived from its state, not carried`() = runTest {
        roundTrip()

        val lesson = progressRows.last { it.kind == ProgressKind.QAIDA_LESSON }
        assertThat(lesson.state).isEqualTo("COMPLETED")
        assertThat(lesson.isCompleted).isTrue()
        assertThat(lesson.score).isEqualTo(3)
        assertThat(lesson.resumeId).isEqualTo(11)
    }

    // ── ids never travel ──────────────────────────────────────────────────────

    @Test
    fun `a khatam's verses attach to the local khatam, not to the sender's id`() = runTest {
        roundTrip()

        // The sender's khatams are 1 and 2; Room handed out 101 and 102 here. A child that
        // kept the sender's id would inflate whichever local khatam happens to hold it.
        assertThat(khatamAyahs.flatten().map { it.khatamId }).containsExactly(101L, 102L)
        assertThat(dailyLogs.map { it.khatamId }).containsExactly(101L, 102L)
    }

    @Test
    fun `an inserted khatam does not carry the sender's row id`() = runTest {
        roundTrip()

        assertThat(khatamsInserted.map { it.id }).containsExactly(0L, 0L)
        // createdAt is what identifies a khatam across devices, so it must survive intact.
        assertThat(khatamsInserted.map { it.createdAt }).containsExactly(6_001L, 6_002L)
    }

    @Test
    fun `a khatam already here is updated in place rather than duplicated`() = runTest {
        coEvery { khatamDao.getAllKhatamsSync() } returns
            listOf(SyncFixtures.khatam(1L).copy(id = 77L, name = "stale", updatedAt = 0L))

        roundTrip()

        // Matched on createdAt, updated under the *local* id, and its children re-parented to it.
        assertThat(khatamsUpdated.map { it.id }).containsExactly(77L)
        assertThat(khatamsUpdated.single().name).isEqualTo("Khatam 1")
        assertThat(khatamAyahs.flatten().map { it.khatamId }).contains(77L)
    }

    @Test
    fun `a khatam that is newer here keeps its local row but still adopts the children`() =
        runTest {
            coEvery { khatamDao.getAllKhatamsSync() } returns
                listOf(SyncFixtures.khatam(1L).copy(id = 88L, name = "mine", updatedAt = 999_999L))

            roundTrip()

            assertThat(khatamsUpdated).isEmpty()
            assertThat(khatamsInserted.map { it.name }).containsExactly("Khatam 2")
            // The id is still mapped, or this khatam's verses would have nowhere to land.
            assertThat(khatamAyahs.flatten().map { it.khatamId }).contains(88L)
        }

    @Test
    fun `a child whose parent is not in the payload is dropped rather than misfiled`() = runTest {
        importer.importKhatamData(SyncPayload(khatamAyahs = listOf(
            SyncKhatamAyah(khatamId = 404L, ayahId = 1, readAt = 1L, updatedAt = 1L)
        )))

        assertThat(khatamAyahs).isEmpty()
        assertThat(dailyLogs).isEmpty()
    }

    @Test
    fun `a prayer record matched on date and prayer keeps the local row id`() = runTest {
        coEvery { prayerDao.getAllPrayerRecords() } returns listOf(
            SyncFixtures.prayerRecord().copy(id = 55L, status = "missed", updatedAt = 0L)
        )

        roundTrip()

        assertThat(prayerRecords.flatten().single().id).isEqualTo(55L)
        assertThat(prayerRecords.flatten().single().status).isEqualTo("prayed")
    }

    @Test
    fun `a location already saved here is matched on its coordinates`() = runTest {
        coEvery { locationDao.getAllLocationsSync() } returns listOf(
            SyncFixtures.location().copy(id = 66L, name = "old name", updatedAt = 0L)
        )

        roundTrip()

        assertThat(locations.single().id).isEqualTo(66L)
        assertThat(locations.single().name).isEqualTo("Dublin")
    }

    @Test
    fun `an imported location never becomes this device's current location`() = runTest {
        roundTrip()

        // Moving the receiving phone's prayer times to another city is the failure this prevents.
        assertThat(locations.single().isCurrentLocation).isFalse()
        assertThat(locations.single().isFavorite).isTrue()
    }

    @Test
    fun `a location that is current here stays current after the sync`() = runTest {
        coEvery { locationDao.getAllLocationsSync() } returns listOf(
            SyncFixtures.location().copy(id = 66L, isCurrentLocation = true, updatedAt = 0L)
        )

        roundTrip()

        assertThat(locations.single().isCurrentLocation).isTrue()
    }

    @Test
    fun `a highlight is matched on the span it covers, not on its row id`() = runTest {
        coEvery { tafseerUserDao.getAllHighlightsSync() } returns listOf(
            SyncFixtures.highlight().copy(id = 99L, color = "#000000", updatedAt = 0L)
        )

        roundTrip()

        assertThat(highlights.flatten().single().id).isEqualTo(99L)
        assertThat(highlights.flatten().single().color).isEqualTo("#FFFF00")
    }

    @Test
    fun `a highlight over a different span is a different highlight`() = runTest {
        coEvery { tafseerUserDao.getAllHighlightsSync() } returns listOf(
            SyncFixtures.highlight().copy(id = 99L, startOffset = 0, endOffset = 5)
        )

        roundTrip()

        assertThat(highlights.flatten().single().id).isEqualTo(0L)
    }

    // ── the older side never wins ─────────────────────────────────────────────

    @Test
    fun `records that are older than the local ones are not written at all`() = runTest {
        val future = 9_999_999L
        coEvery { prayerDao.getAllPrayerRecords() } returns
            listOf(SyncFixtures.prayerRecord().copy(updatedAt = future))
        coEvery { fastingDao.getAllFastRecords() } returns
            listOf(SyncFixtures.fastRecord().copy(updatedAt = future))
        coEvery { fastingDao.getAllMakeupFastsSync() } returns
            listOf(SyncFixtures.makeupFast().copy(updatedAt = future))
        coEvery { tasbihDao.getAllPresetsSync() } returns
            listOf(SyncFixtures.preset().copy(updatedAt = future))
        coEvery { sessionDao.getAllSessionsSync() } returns
            listOf(SyncFixtures.session().copy(updatedAt = future))
        coEvery { zakatDao.getAllHistorySync() } returns
            listOf(SyncFixtures.zakat().copy(updatedAt = future))
        coEvery { tafseerUserDao.getAllNotesSync() } returns
            listOf(SyncFixtures.note().copy(updatedAt = future))
        coEvery { readingProgressDao.get() } returns ReadingProgressEntity(
            id = 1, lastReadSurah = 9, lastReadAyah = 9, lastReadPage = 9, lastReadJuz = 9,
            totalAyahsRead = 5_000, currentKhatmaCount = 9, updatedAt = future,
        )
        coEvery { locationDao.getAllLocationsSync() } returns
            listOf(SyncFixtures.location().copy(updatedAt = future))

        roundTrip()

        // The empty-list guard matters: an empty INSERT is a write the DAO should never see.
        assertThat(prayerRecords).isEmpty()
        assertThat(fastRecords).isEmpty()
        assertThat(makeupFasts).isEmpty()
        assertThat(presets).isEmpty()
        assertThat(sessions).isEmpty()
        assertThat(zakats).isEmpty()
        assertThat(notes).isEmpty()
        assertThat(readingProgress).isEmpty()
        assertThat(locations).isEmpty()
    }

    @Test
    fun `an incoming hadith mark does not clear a favourite this device set`() = runTest {
        markStore += BookmarkEntity(
            kind = BookmarkKind.HADITH, targetId = SyncFixtures.HADITH_ID,
            bookmarked = true, favourite = true, createdAt = 1L, updatedAt = 1L,
        )

        roundTrip()

        // The wire format has no field for it, so writing the row blind would silently lose it.
        val row = marks.last { it.kind == BookmarkKind.HADITH }
        assertThat(row.favourite).isTrue()
    }

    @Test
    fun `an incoming name favourite unions with the local one rather than replacing it`() =
        runTest {
            markStore += BookmarkEntity(
                kind = BookmarkKind.PROPHET, targetId = 9, bookmarked = true,
                favourite = false, note = "mine", createdAt = 1L, updatedAt = 5L,
            )

            roundTrip()

            // SyncNameBookmark carries no updatedAt, so there is no timestamp to arbitrate with:
            // a flag set on either side stays set, and the local note survives.
            val row = marks.last { it.kind == BookmarkKind.PROPHET }
            assertThat(row.favourite).isTrue()
            assertThat(row.note).isEqualTo("mine")
            assertThat(row.createdAt).isEqualTo(1L)
        }

    @Test
    fun `an incoming name row can never clear a local favourite`() = runTest {
        markStore += BookmarkEntity(
            kind = BookmarkKind.ASMA_UL_HUSNA, targetId = 7, bookmarked = true,
            favourite = true, createdAt = 1L, updatedAt = 1L,
        )

        importer.importNamesData(
            SyncPayload(asmaUlHusnaBookmarks = listOf(
                SyncNameBookmark(id = 0, refId = 7, isFavorite = false, createdAt = 2L)
            ))
        )

        assertThat(marks.single().favourite).isTrue()
    }

    @Test
    fun `an older incoming bookmark keeps the local note and colour but merges the timestamps`() =
        runTest {
            markStore += BookmarkEntity(
                kind = BookmarkKind.AYAH, targetId = SyncFixtures.BOTH_AYAH,
                bookmarked = true, favourite = false, note = "local note", colour = "#0000FF",
                createdAt = 50L, updatedAt = 9_999L,
            )

            roundTrip()

            val row = marks.first {
                it.kind == BookmarkKind.AYAH && it.targetId == SyncFixtures.BOTH_AYAH
            }
            assertThat(row.note).isEqualTo("local note")
            assertThat(row.colour).isEqualTo("#0000FF")
            // The row's history spans both devices: earliest creation, latest change.
            assertThat(row.createdAt).isEqualTo(50L)
            assertThat(row.updatedAt).isEqualTo(9_999L)
        }

    @Test
    fun `an arriving favourite does not clear the bookmark or its note`() = runTest {
        markStore += BookmarkEntity(
            kind = BookmarkKind.AYAH, targetId = SyncFixtures.FAVOURITE_ONLY_AYAH,
            bookmarked = true, favourite = false, note = "keep me", colour = "#123456",
            createdAt = 1L, updatedAt = 1L,
        )

        roundTrip()

        val row = marks.last {
            it.kind == BookmarkKind.AYAH && it.targetId == SyncFixtures.FAVOURITE_ONLY_AYAH
        }
        assertThat(row.bookmarked).isTrue()
        assertThat(row.favourite).isTrue()
        assertThat(row.note).isEqualTo("keep me")
    }

    // ── nothing to do is not an error ─────────────────────────────────────────

    @Test
    fun `importing an empty payload writes nothing anywhere`() = runTest {
        importer.import(SyncPayload())

        assertThat(marks).isEmpty()
        assertThat(progressRows).isEmpty()
        assertThat(prayerRecords).isEmpty()
        assertThat(khatamsInserted).isEmpty()
        assertThat(locations).isEmpty()
        assertThat(readingProgress).isEmpty()
        // An empty preference map must not reach the datastore — that would be a write for nothing.
        assertThat(importedPreferences).isEmpty()
    }

    @Test
    fun `a payload from a device with no reading history leaves the local position alone`() =
        runTest {
            coEvery { srcReadingProgressDao.get() } returns null

            roundTrip()

            assertThat(readingProgress).isEmpty()
        }

    @Test
    fun `the categories list counts what the payload actually holds`() = runTest {
        val payload = exporter.export()

        val counts = payload.categories().associate { it.key to it.count }
        assertThat(counts["bookmarks"]).isEqualTo(1)
        assertThat(counts["favorites"]).isEqualTo(2)
        assertThat(counts["khatams"]).isEqualTo(2)
        assertThat(counts["khatamAyahs"]).isEqualTo(2)
        assertThat(counts["readingProgress"]).isEqualTo(1)
        assertThat(payload.categories().single { it.key == "readingProgress" }.isFlag).isTrue()
    }

    @Test
    fun `every remaining table also refuses a record older than the local one`() = runTest {
        val future = 9_999_999L
        markStore += listOf(
            BookmarkEntity(
                kind = BookmarkKind.HADITH, targetId = SyncFixtures.HADITH_ID,
                bookmarked = true, favourite = false, note = "mine",
                createdAt = 1L, updatedAt = future,
            ),
            BookmarkEntity(
                kind = BookmarkKind.DUA, targetId = SyncFixtures.DUA_ID,
                bookmarked = true, favourite = false, note = "mine",
                createdAt = 1L, updatedAt = future,
            ),
        )
        coEvery { progressDao.all() } returns listOf(
            ProgressEntity(
                kind = ProgressKind.DUA, targetId = SyncFixtures.DUA_ID, date = 500L,
                completed = 9, createdAt = 1L, updatedAt = future,
            ),
            ProgressEntity(
                kind = ProgressKind.QAIDA_LESSON, targetId = 3, completed = 9,
                createdAt = 1L, updatedAt = future,
            ),
            ProgressEntity(
                kind = ProgressKind.QAIDA_CELL, targetId = 11, completed = 9,
                createdAt = 1L, updatedAt = future,
            ),
        )
        coEvery { tafseerUserDao.getAllHighlightsSync() } returns
            listOf(SyncFixtures.highlight().copy(updatedAt = future))
        coEvery { khatamDao.getAllKhatamsSync() } returns
            listOf(SyncFixtures.khatam(1L).copy(id = 77L, updatedAt = future))
        coEvery { khatamDao.getDailyLog(77L, 1L) } returns
            KhatamDailyLogEntity(khatamId = 77L, date = 1L, ayahsRead = 99, updatedAt = future)

        roundTrip()

        // Each of these is a `local == null || incoming.updatedAt > local.updatedAt`, and each
        // one getting it backwards overwrites a record the user made more recently on *this*
        // phone with a stale copy from the other.
        assertThat(marks.none { it.kind == BookmarkKind.HADITH }).isTrue()
        assertThat(marks.none { it.kind == BookmarkKind.DUA }).isTrue()
        assertThat(progressRows).isEmpty()
        assertThat(highlights).isEmpty()
        assertThat(dailyLogs.none { it.khatamId == 77L && it.date == 1L }).isTrue()
    }

    @Test
    fun `a khatam already here keeps the local id for a log that is newer on the wire`() =
        runTest {
            coEvery { khatamDao.getAllKhatamsSync() } returns
                listOf(SyncFixtures.khatam(1L).copy(id = 77L, updatedAt = 0L))
            coEvery { khatamDao.getDailyLog(77L, 1L) } returns
                KhatamDailyLogEntity(khatamId = 77L, date = 1L, ayahsRead = 1, updatedAt = 0L)

            roundTrip()

            val log = dailyLogs.single { it.khatamId == 77L }
            assertThat(log.ayahsRead).isEqualTo(20)
        }

    @Test
    fun `a name favourite this device never set is adopted from the wire`() = runTest {
        // The union runs the other way too: local false, incoming true.
        roundTrip()

        assertThat(marks.single { it.kind == BookmarkKind.ASMA_UL_HUSNA }.favourite).isTrue()
        assertThat(marks.single { it.kind == BookmarkKind.ASMA_UN_NABI }.favourite).isTrue()
    }

    @Test
    fun `a name neither side favourited stays un-favourited`() = runTest {
        markStore += BookmarkEntity(
            kind = BookmarkKind.PROPHET, targetId = 9, bookmarked = true, favourite = false,
            createdAt = 1L, updatedAt = 1L,
        )

        importer.importNamesData(
            SyncPayload(prophetBookmarks = listOf(
                SyncNameBookmark(id = 0, refId = 9, isFavorite = false, createdAt = 2L)
            ))
        )

        assertThat(marks.single().favourite).isFalse()
        assertThat(marks.single().bookmarked).isTrue()
    }
}
