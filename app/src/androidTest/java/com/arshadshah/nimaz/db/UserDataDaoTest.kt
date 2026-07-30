package com.arshadshah.nimaz.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arshadshah.nimaz.support.NimazDbRule
import com.arshadshah.nimaz.data.local.user.BookmarkKind
import com.arshadshah.nimaz.data.local.user.ProgressKind
import com.arshadshah.nimaz.support.TestData
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Round-trip coverage for the remaining user-data DAOs that don't warrant a class of
 * their own: tafseer annotations, Qaida lesson progress, the names-of-Allah /
 * names-of-the-Prophet / prophets bookmark tables, saved locations, and the Islamic
 * events calendar reads.
 */
@RunWith(AndroidJUnit4::class)
class UserDataDaoTest {

    @get:Rule
    val dbRule = NimazDbRule()

    @Test
    fun tafseer_highlightsAndNotes_roundTrip() = runTest {
        val dao = dbRule.userDb.tafseerUserDao()

        dao.insertHighlight(TestData.tafseerHighlight(ayahId = 5))
        dao.insertNote(TestData.tafseerNote(ayahId = 5, text = "reflection"))

        assertThat(dao.getHighlightsForAyah(5, "ibn-kathir").first()).hasSize(1)
        assertThat(dao.getNotesForAyah(5, "ibn-kathir").first().first().text)
            .isEqualTo("reflection")

        dao.deleteAllUserData()
        assertThat(dao.getAllHighlightsSync()).isEmpty()
        assertThat(dao.getAllNotesSync()).isEmpty()
    }

    /**
     * A commentary block covers a contiguous ayah range (#329): a lookup for any
     * ayah inside the range must resolve to the same block, and a highlight/note
     * made on one ayah of that range must surface for the whole range — not just
     * the exact ayah it was created on.
     */
    @Test
    fun tafseer_blockRange_resolvesAndGathersHighlightsAcrossAyahs() = runTest {
        val quranDao = dbRule.db.quranDao()
        val content = dbRule.db.tafseerDao()
        val dao = dbRule.userDb.tafseerUserDao()

        // The surah first: `ayahs.surah_id` is a foreign key onto `surahs`, so
        // inserting an ayah for a surah that is not there fails with
        // SQLITE_CONSTRAINT_FOREIGNKEY rather than with anything about this test.
        quranDao.insertSurahs(listOf(TestData.surah(id = 1, versesCount = 7)))
        // Ayahs 5, 6 and 7 of surah 1 — the block below covers all three.
        quranDao.insertAyahs(
            listOf(
                TestData.ayah(id = 5, surahId = 1, numberInSurah = 5),
                TestData.ayah(id = 6, surahId = 1, numberInSurah = 6),
                TestData.ayah(id = 7, surahId = 1, numberInSurah = 7),
            )
        )
        // No @Insert exists for tafseer_blocks (content arrives with the shipped
        // artifact, never written by the app), so the fixture row goes in directly.
        dbRule.db.openHelper.writableDatabase.execSQL(
            "INSERT INTO tafseer_blocks (tafseer_id, surah_number, ayah_start, ayah_end, text) " +
                "VALUES ('ibn-kathir', 1, 5, 7, 'commentary spanning three ayahs')"
        )

        assertThat(content.getTafseerForAyah(1, 5, "ibn-kathir")?.text)
            .isEqualTo("commentary spanning three ayahs")
        assertThat(content.getTafseerForAyah(1, 7, "ibn-kathir")?.text)
            .isEqualTo("commentary spanning three ayahs")
        assertThat(content.getTafseerForAyah(1, 8, "ibn-kathir")).isNull()

        // Highlight/note made on ayah 6 (the middle of the range, not its start).
        dao.insertHighlight(TestData.tafseerHighlight(ayahId = 6, tafseerId = "ibn-kathir"))
        dao.insertNote(TestData.tafseerNote(ayahId = 6, tafseerId = "ibn-kathir", text = "note"))

        // Two databases: the span resolves against the verses, the marks against the user's
        // rows. This is the cross-database read the old INNER JOIN used to do in one query.
        val ayahIds = quranDao.getAyahIdsInRange(1, 5, 7)
        assertThat(ayahIds).containsExactly(5, 6, 7).inOrder()
        assertThat(dao.getHighlightsForRange("ibn-kathir", ayahIds).first()).hasSize(1)
        assertThat(dao.getNotesForRange("ibn-kathir", ayahIds).first()).hasSize(1)
    }

    @Test
    fun qaida_lessonProgress_upsertsAndReads() = runTest {
        val dao = dbRule.userDb.progressDao()

        dao.upsert(TestData.lessonProgress(lessonId = 1, stars = 1))
        assertThat(dao.find(ProgressKind.QAIDA_LESSON, 1)!!.score).isEqualTo(1)

        // Same key, so an upsert replaces rather than duplicating — (kind, target, date).
        dao.upsert(TestData.lessonProgress(lessonId = 1, stars = 3))
        assertThat(dao.find(ProgressKind.QAIDA_LESSON, 1)!!.score).isEqualTo(3)
        assertThat(dao.ofKind(ProgressKind.QAIDA_LESSON).first()).hasSize(1)
    }

    /**
     * A lesson and one of its cells are two kinds in one table, and must not collide: they can
     * share a target id and are told apart by `kind`, with the cell carrying its lesson in
     * `context_id`.
     */
    @Test
    fun qaida_lessonAndCellProgress_coexist() = runTest {
        val dao = dbRule.userDb.progressDao()

        dao.upsert(TestData.lessonProgress(lessonId = 2, stars = 2))
        dao.upsert(TestData.cellProgress(cellId = 2, lessonId = 2, heard = 4))

        assertThat(dao.find(ProgressKind.QAIDA_LESSON, 2)!!.score).isEqualTo(2)
        assertThat(dao.find(ProgressKind.QAIDA_CELL, 2)!!.completed).isEqualTo(4)
        assertThat(dao.inContext(ProgressKind.QAIDA_CELL, 2).first()).hasSize(1)
    }

    /**
     * The names, the other names and the prophets were three bookmark tables with identical
     * shapes. They are three `kind` values now, and the point of this test is that they stay
     * separate: id 7 favourited as a name of Allah is not id 7 favourited as a prophet.
     */
    @Test
    fun nameAndProphetFavourites_areKeptApartByKind() = runTest {
        val dao = dbRule.userDb.bookmarkDao()

        dao.upsert(TestData.favourite(BookmarkKind.ASMA_UL_HUSNA, 7))
        dao.upsert(TestData.favourite(BookmarkKind.PROPHET, 7))
        dao.upsert(TestData.favourite(BookmarkKind.ASMA_UN_NABI, 3))

        assertThat(dao.find(BookmarkKind.ASMA_UL_HUSNA, 7)!!.favourite).isTrue()
        assertThat(dao.find(BookmarkKind.PROPHET, 7)!!.favourite).isTrue()
        assertThat(dao.find(BookmarkKind.ASMA_UN_NABI, 7)).isNull()
        assertThat(dao.favourites(BookmarkKind.ASMA_UL_HUSNA).first()).hasSize(1)

        dao.delete(BookmarkKind.ASMA_UL_HUSNA, 7)
        assertThat(dao.find(BookmarkKind.ASMA_UL_HUSNA, 7)).isNull()
        // Deleting one kind leaves the other two alone.
        assertThat(dao.find(BookmarkKind.PROPHET, 7)).isNotNull()
        assertThat(dao.all()).hasSize(2)
    }

    @Test
    fun location_insertSetCurrentAndFavorite() = runTest {
        val dao = dbRule.userDb.locationDao()

        val makkah = dao.insertLocation(TestData.location(name = "Makkah", isCurrent = false))
        val madinah = dao.insertLocation(TestData.location(name = "Madinah", isCurrent = false))

        dao.setCurrentLocation(madinah)
        assertThat(dao.getCurrentLocation().first()!!.id).isEqualTo(madinah)

        dao.toggleFavorite(makkah)
        assertThat(dao.getFavoriteLocationsSync().map { it.id }).contains(makkah)
        assertThat(dao.getAllLocationsSync()).hasSize(2)
    }

    @Test
    fun islamicEvents_queryByMonthAndDate() = runTest {
        val dao = dbRule.db.islamicEventDao()

        dao.insertEvents(
            listOf(
                TestData.islamicEvent(id = 1, hijriMonth = 9, hijriDay = 1, eventType = "fast"),
                TestData.islamicEvent(id = 2, hijriMonth = 10, hijriDay = 1, eventType = "holiday", isHoliday = 1),
            )
        )

        assertThat(dao.getAllEvents().first()).hasSize(2)
        assertThat(dao.getEventsByMonth(9).first().map { it.id }).containsExactly(1)
        assertThat(dao.getEventsForDate(10, 1).first().map { it.id }).containsExactly(2)
        assertThat(dao.getHolidays().first().map { it.id }).containsExactly(2)
    }
}
