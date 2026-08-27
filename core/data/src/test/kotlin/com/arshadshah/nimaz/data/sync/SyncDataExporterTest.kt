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
import com.arshadshah.nimaz.data.local.user.BookmarkKind
import com.arshadshah.nimaz.data.local.user.ProgressDao
import com.arshadshah.nimaz.data.local.user.ProgressKind
import com.arshadshah.nimaz.data.local.user.ReadingProgressDao
import com.arshadshah.nimaz.data.local.user.TafseerUserDao
import com.arshadshah.nimaz.data.local.user.TasbihSessionDao
import com.arshadshah.nimaz.data.local.user.BookmarkDao
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * What actually leaves the device when the user taps Send.
 *
 * Every field here reaches another phone's database and there is no undo, so the failure mode
 * of an export is not an error — it is a record that quietly never arrives, or arrives stripped
 * of half its columns. Three things in particular are only visible from this side:
 *
 *  - the payload keeps the **old seven-table wire format** on purpose, because a phone on this
 *    version has to sync with one that still has seven bookmark tables. So the consolidated
 *    `bookmarks` row is *fanned back out* by kind and by flag, and a verse that is both
 *    bookmarked and favourited has to appear in **both** lists — as it did when it was a row in
 *    each table. A filter that reads `bookmarked` where it means `favourite` silently drops one;
 *  - progress rows are one table too (`ProgressKind`), so dua counts, qaida lessons and qaida
 *    cells all come out of `progressDao.all()` and are told apart by nothing but the kind;
 *  - **only favourite locations travel, and the current-location flag never does** — importing
 *    one that carried it would move the receiving device's prayer times to another city.
 */
class SyncDataExporterTest {

    private val quranDao: QuranDao = mockk(relaxed = true)
    private val prayerDao: PrayerDao = mockk(relaxed = true)
    private val fastingDao: FastingDao = mockk(relaxed = true)
    private val tasbihDao: TasbihDao = mockk(relaxed = true)
    private val sessionDao: TasbihSessionDao = mockk(relaxed = true)
    private val khatamDao: KhatamDao = mockk(relaxed = true)
    private val tafseerDao: TafseerDao = mockk(relaxed = true)
    private val tafseerUserDao: TafseerUserDao = mockk(relaxed = true)
    private val zakatDao: ZakatDao = mockk(relaxed = true)
    private val asmaUlHusnaDao: AsmaUlHusnaDao = mockk(relaxed = true)
    private val asmaUnNabiDao: AsmaUnNabiDao = mockk(relaxed = true)
    private val prophetDao: ProphetDao = mockk(relaxed = true)
    private val hadithDao: HadithDao = mockk(relaxed = true)
    private val bookmarkDao: BookmarkDao = mockk(relaxed = true)
    private val progressDao: ProgressDao = mockk(relaxed = true)
    private val readingProgressDao: ReadingProgressDao = mockk(relaxed = true)
    private val duaDao: DuaDao = mockk(relaxed = true)
    private val qaidaDao: QaidaDao = mockk(relaxed = true)
    private val locationDao: LocationDao = mockk(relaxed = true)
    private val preferences: PreferencesDataStore = mockk(relaxed = true)

    private val exporter by lazy {
        SyncDataExporter(
            quranDao, prayerDao, fastingDao, tasbihDao, sessionDao, khatamDao, tafseerDao,
            tafseerUserDao, zakatDao, asmaUlHusnaDao, asmaUnNabiDao, prophetDao, hadithDao,
            bookmarkDao, progressDao, readingProgressDao, duaDao, qaidaDao, locationDao,
            preferences,
        )
    }

    @Before
    fun setUp() = SyncFixtures.populate(
        prayerDao = prayerDao,
        fastingDao = fastingDao,
        tasbihDao = tasbihDao,
        sessionDao = sessionDao,
        khatamDao = khatamDao,
        tafseerUserDao = tafseerUserDao,
        zakatDao = zakatDao,
        bookmarkDao = bookmarkDao,
        progressDao = progressDao,
        readingProgressDao = readingProgressDao,
        locationDao = locationDao,
        preferences = preferences,
    )

    // ── the consolidated bookmark row, fanned back out ────────────────────────

    @Test
    fun `a verse that is both bookmarked and favourited travels in both lists`() = runTest {
        val payload = exporter.export()

        // One row, two flags — and the wire format still has two tables.
        assertThat(payload.bookmarks.map { it.ayahId }).contains(SyncFixtures.BOTH_AYAH)
        assertThat(payload.favorites.map { it.ayahId }).contains(SyncFixtures.BOTH_AYAH)
    }

    @Test
    fun `a verse that is only favourited does not travel as a bookmark`() = runTest {
        val payload = exporter.export()

        assertThat(payload.favorites.map { it.ayahId }).contains(SyncFixtures.FAVOURITE_ONLY_AYAH)
        assertThat(payload.bookmarks.map { it.ayahId })
            .doesNotContain(SyncFixtures.FAVOURITE_ONLY_AYAH)
    }

    @Test
    fun `a bookmark carries the surah and verse it was filed under`() = runTest {
        val bookmark = exporter.export().bookmarks.single { it.ayahId == SyncFixtures.BOTH_AYAH }

        assertThat(bookmark.surahNumber).isEqualTo(2)
        assertThat(bookmark.ayahNumber).isEqualTo(255)
        assertThat(bookmark.note).isEqualTo("memorise")
        assertThat(bookmark.color).isEqualTo("#FF0000")
    }

    @Test
    fun `marks of other kinds do not leak into the Quran lists`() = runTest {
        val payload = exporter.export()

        // Hadith, dua and the three name kinds share the table with ayah marks.
        assertThat(payload.bookmarks.map { it.ayahId })
            .containsNoneOf(SyncFixtures.HADITH_ID, SyncFixtures.DUA_ID)
        assertThat(payload.hadithBookmarks.map { it.hadithId })
            .containsExactly(SyncFixtures.HADITH_ID)
        assertThat(payload.duaBookmarks.map { it.duaId }).containsExactly(SyncFixtures.DUA_ID)
    }

    @Test
    fun `each name kind exports into its own list`() = runTest {
        val payload = exporter.export()

        assertThat(payload.asmaUlHusnaBookmarks.map { it.refId }).containsExactly(7)
        assertThat(payload.asmaUnNabiBookmarks.map { it.refId }).containsExactly(8)
        assertThat(payload.prophetBookmarks.map { it.refId }).containsExactly(9)
    }

    // ── the shared progress table, told apart by kind ─────────────────────────

    @Test
    fun `dua, qaida lesson and qaida cell progress separate by kind`() = runTest {
        val payload = exporter.export()

        assertThat(payload.duaProgress.map { it.duaId }).containsExactly(SyncFixtures.DUA_ID)
        assertThat(payload.qaidaLessonProgress.map { it.lessonId }).containsExactly(3)
        assertThat(payload.qaidaCellProgress.map { it.cellId }).containsExactly(11)
    }

    @Test
    fun `a qaida lesson exports its state, stars and resume point`() = runTest {
        val lesson = exporter.export().qaidaLessonProgress.single()

        assertThat(lesson.status).isEqualTo("COMPLETED")
        assertThat(lesson.stars).isEqualTo(3)
        assertThat(lesson.lastCellId).isEqualTo(11)
        assertThat(lesson.completedCells).isEqualTo(12)
        assertThat(lesson.totalCells).isEqualTo(12)
    }

    @Test
    fun `a qaida cell exports the lesson it belongs to`() = runTest {
        val cell = exporter.export().qaidaCellProgress.single()

        assertThat(cell.lessonId).isEqualTo(3)
        assertThat(cell.heardCount).isEqualTo(4)
        assertThat(cell.isCompleted).isTrue()
    }

    @Test
    fun `a lesson with no recorded total exports zero rather than dropping the row`() = runTest {
        coEvery { progressDao.all() } returns listOf(
            SyncFixtures.progress(ProgressKind.QAIDA_LESSON, targetId = 4, total = null)
        )

        val lesson = exporter.export().qaidaLessonProgress.single()

        assertThat(lesson.totalCells).isEqualTo(0)
        assertThat(lesson.status).isEmpty()
    }

    // ── the parent-child tables ───────────────────────────────────────────────

    @Test
    fun `every khatam's ayahs and daily logs are gathered, not just the active one's`() = runTest {
        val payload = exporter.export()

        assertThat(payload.khatams.map { it.id }).containsExactly(1L, 2L)
        assertThat(payload.khatamAyahs.map { it.khatamId }).containsExactly(1L, 2L)
        assertThat(payload.khatamDailyLogs.map { it.khatamId }).containsExactly(1L, 2L)
    }

    @Test
    fun `a khatam exports the fields the receiving device needs to reschedule its reminder`() =
        runTest {
            val khatam = exporter.export().khatams.first { it.id == 1L }

            assertThat(khatam.reminderEnabled).isTrue()
            assertThat(khatam.reminderTime).isEqualTo("06:30")
            assertThat(khatam.dailyTarget).isEqualTo(20)
            assertThat(khatam.isActive).isTrue()
        }

    // ── everything else that has to arrive whole ──────────────────────────────

    @Test
    fun `prayer, fast and makeup records carry their timestamps`() = runTest {
        val payload = exporter.export()

        assertThat(payload.prayerRecords.single().prayerName).isEqualTo("fajr")
        assertThat(payload.prayerRecords.single().isJamaah).isTrue()
        assertThat(payload.fastRecords.single().fastType).isEqualTo("ramadan")
        assertThat(payload.fastRecords.single().hijriMonth).isEqualTo(9)
        assertThat(payload.makeupFasts.single().fidyaAmount).isEqualTo(5.0)
    }

    @Test
    fun `tasbih presets and sessions travel with the counts they hold`() = runTest {
        val payload = exporter.export()

        assertThat(payload.tasbihPresets.single().name).isEqualTo("SubhanAllah")
        assertThat(payload.tasbihPresets.single().targetCount).isEqualTo(33)
        assertThat(payload.tasbihSessions.single().currentCount).isEqualTo(33)
        assertThat(payload.tasbihSessions.single().totalLaps).isEqualTo(1)
    }

    @Test
    fun `tafseer highlights carry the span they cover, which is what identifies them`() = runTest {
        val highlight = exporter.export().tafseerHighlights.single()

        // The row id cannot identify a highlight: it is autoGenerate, so both phones hand out 1.
        assertThat(highlight.ayahId).isEqualTo(262)
        assertThat(highlight.tafseerId).isEqualTo("ibn-kathir")
        assertThat(highlight.startOffset).isEqualTo(10)
        assertThat(highlight.endOffset).isEqualTo(40)
        assertThat(exporter.export().tafseerNotes.single().text).isEqualTo("note text")
    }

    @Test
    fun `a zakat calculation exports what it was worked out from`() = runTest {
        val history = exporter.export().zakatHistory.single()

        assertThat(history.netWorth).isEqualTo(4000.0)
        assertThat(history.zakatDue).isEqualTo(100.0)
        assertThat(history.nisabType).isEqualTo("silver")
        assertThat(history.isPaid).isTrue()
    }

    @Test
    fun `only favourite locations are exported and none of them carries a current flag`() =
        runTest {
            val payload = exporter.export()

            // `getFavoriteLocationsSync` is the query; the payload type has no field for
            // `isCurrentLocation` at all, which is what stops a sync moving the other phone's
            // prayer times to this city.
            assertThat(payload.favoriteLocations.map { it.city }).containsExactly("Dublin")
            assertThat(payload.favoriteLocations.single().calculationMethod).isEqualTo("MWL")
            assertThat(payload.favoriteLocations.single().fajrAngle).isEqualTo(18.0)
        }

    @Test
    fun `reading progress travels as the running totals, not just the position`() = runTest {
        val progress = exporter.export().readingProgress!!

        assertThat(progress.lastReadSurah).isEqualTo(2)
        assertThat(progress.totalAyahsRead).isEqualTo(900)
        assertThat(progress.currentKhatmaCount).isEqualTo(2)
    }

    @Test
    fun `a device that has never opened the Quran exports no reading progress`() = runTest {
        coEvery { readingProgressDao.get() } returns null

        assertThat(exporter.export().readingProgress).isNull()
    }

    @Test
    fun `preferences are exported as the datastore reports them`() = runTest {
        assertThat(exporter.export().preferences).containsExactly("theme", "dark")
    }

    @Test
    fun `an empty device still produces a well formed payload`() = runTest {
        SyncFixtures.empty(
            prayerDao, fastingDao, tasbihDao, sessionDao, khatamDao, tafseerUserDao, zakatDao,
            bookmarkDao, progressDao, readingProgressDao, locationDao, preferences,
        )

        val payload = exporter.export()

        // Every category reports zero rather than the export failing part-way.
        assertThat(payload.categories().map { it.count }.toSet()).containsExactly(0)
        assertThat(payload.appVersion).isEqualTo(11)
        assertThat(payload.exportedAt).isGreaterThan(0L)
    }

    @Test
    fun `the progress labels name what is being exported at each step`() = runTest {
        val labels = mutableListOf<String>()

        exporter.export { _, _, label -> labels += label }

        assertThat(labels).hasSize(SyncDataExporter.STEP_COUNT)
        assertThat(labels.toSet()).hasSize(SyncDataExporter.STEP_COUNT)
        assertThat(labels.first()).contains("Quran")
        assertThat(labels.last()).contains("preferences")
    }

    @Test
    fun `a mark filed under nothing exports as zero rather than being dropped`() = runTest {
        coEvery { bookmarkDao.all() } returns listOf(
            com.arshadshah.nimaz.data.local.user.BookmarkEntity(
                kind = BookmarkKind.AYAH, targetId = 262, bookmarked = true, favourite = true,
                contextId = null, ordinal = null, createdAt = 1L, updatedAt = 2L,
            ),
            com.arshadshah.nimaz.data.local.user.BookmarkEntity(
                kind = BookmarkKind.HADITH, targetId = 5001, bookmarked = true,
                contextId = null, ordinal = null, createdAt = 1L, updatedAt = 2L,
            ),
            com.arshadshah.nimaz.data.local.user.BookmarkEntity(
                kind = BookmarkKind.DUA, targetId = 42, bookmarked = true,
                contextId = null, createdAt = 1L, updatedAt = 2L,
            ),
        )

        val payload = exporter.export()

        // The wire format has no nullable context, and dropping the row would lose the mark.
        assertThat(payload.bookmarks.single().surahNumber).isEqualTo(0)
        assertThat(payload.bookmarks.single().ayahNumber).isEqualTo(0)
        assertThat(payload.favorites.single().surahNumber).isEqualTo(0)
        assertThat(payload.hadithBookmarks.single().bookId).isEqualTo(0)
        assertThat(payload.duaBookmarks.single().categoryId).isEqualTo(0)
    }

    @Test
    fun `a qaida cell with no lesson recorded exports as lesson zero`() = runTest {
        coEvery { progressDao.all() } returns listOf(
            SyncFixtures.progress(ProgressKind.QAIDA_CELL, targetId = 11, total = null)
        )

        assertThat(exporter.export().qaidaCellProgress.single().lessonId).isEqualTo(0)
    }

    @Test
    fun `a dua count with no target recorded exports as zero`() = runTest {
        coEvery { progressDao.all() } returns listOf(
            SyncFixtures.progress(ProgressKind.DUA, targetId = 42, total = null)
        )

        assertThat(exporter.export().duaProgress.single().targetCount).isEqualTo(0)
    }
}
