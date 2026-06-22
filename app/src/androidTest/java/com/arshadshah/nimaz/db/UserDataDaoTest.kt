package com.arshadshah.nimaz.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arshadshah.nimaz.support.NimazDbRule
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
        val dao = dbRule.db.tafseerDao()

        dao.insertHighlight(TestData.tafseerHighlight(ayahId = 5))
        dao.insertNote(TestData.tafseerNote(ayahId = 5, text = "reflection"))

        assertThat(dao.getHighlightsForAyah(5, "ibn-kathir").first()).hasSize(1)
        assertThat(dao.getNotesForAyah(5, "ibn-kathir").first().first().text)
            .isEqualTo("reflection")

        dao.deleteAllUserData()
        assertThat(dao.getAllHighlightsSync()).isEmpty()
        assertThat(dao.getAllNotesSync()).isEmpty()
    }

    @Test
    fun qaida_lessonProgress_upsertsAndReads() = runTest {
        val dao = dbRule.db.qaidaDao()

        dao.upsertLessonProgress(TestData.qaidaLessonProgress(lessonId = 1, stars = 1))
        assertThat(dao.getLessonProgress(1)!!.stars).isEqualTo(1)

        // Upsert again with the same PK should replace, not duplicate.
        dao.upsertLessonProgress(TestData.qaidaLessonProgress(lessonId = 1, stars = 3))
        assertThat(dao.getLessonProgress(1)!!.stars).isEqualTo(3)
        assertThat(dao.getAllProgress().first()).hasSize(1)
    }

    @Test
    fun asmaUlHusna_bookmarkToggle() = runTest {
        val dao = dbRule.db.asmaUlHusnaDao()

        dao.toggleFavorite(7)
        assertThat(dao.isBookmarked(7)).isTrue()
        assertThat(dao.getAllBookmarks().first()).hasSize(1)

        dao.toggleFavorite(7)
        assertThat(dao.isBookmarked(7)).isFalse()
    }

    @Test
    fun asmaUnNabi_and_prophet_bookmarks() = runTest {
        val nabi = dbRule.db.asmaUnNabiDao()
        val prophet = dbRule.db.prophetDao()

        nabi.insertBookmark(TestData.asmaNabiBookmark(nameId = 3))
        prophet.insertBookmark(TestData.prophetBookmark(prophetId = 4))

        assertThat(nabi.isBookmarked(3)).isTrue()
        assertThat(prophet.isBookmarked(4)).isTrue()

        nabi.removeBookmark(3)
        prophet.removeBookmark(4)
        assertThat(nabi.getAllBookmarksSync()).isEmpty()
        assertThat(prophet.getAllBookmarksSync()).isEmpty()
    }

    @Test
    fun location_insertSetCurrentAndFavorite() = runTest {
        val dao = dbRule.db.locationDao()

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
