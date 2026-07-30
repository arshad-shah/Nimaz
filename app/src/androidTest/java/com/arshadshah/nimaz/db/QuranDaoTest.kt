package com.arshadshah.nimaz.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arshadshah.nimaz.support.NimazDbRule
import com.arshadshah.nimaz.data.local.user.BookmarkKind
import com.arshadshah.nimaz.support.TestData
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Quran content reads plus user data: bookmarks, favorites, reading progress. */
@RunWith(AndroidJUnit4::class)
class QuranDaoTest {

    @get:Rule
    val dbRule = NimazDbRule()
    private val dao get() = dbRule.db.quranDao()

    private suspend fun seedFatihah() {
        dao.insertSurahs(listOf(TestData.surah(id = 1, versesCount = 3)))
        dao.insertAyahs(
            (1..3).map { TestData.ayah(id = it, surahId = 1, numberInSurah = it, numberGlobal = it) }
        )
    }

    @Test
    fun surahsAndAyahs_roundTrip() = runTest {
        seedFatihah()

        assertThat(dao.getAllSurahs().first()).hasSize(1)
        assertThat(dao.getSurahByNumber(1)!!.nameEnglish).isEqualTo("Al-Fatihah")
        assertThat(dao.getAyahsBySurah(1).first()).hasSize(3)
        assertThat(dao.getAyahsByJuz(1).first()).hasSize(3)
        assertThat(dao.getAyahsByPage(1).first()).hasSize(3)
    }

    /**
     * A verse can be bookmarked *and* favourited. That was a row in each of two tables and is
     * now two flags on one row, so the interesting case is that clearing one leaves the other —
     * with its note and colour — alone.
     */
    @Test
    fun bookmarkAndFavourite_areFlagsOnOneRow() = runTest {
        val marks = dbRule.userDb.bookmarkDao()

        marks.upsert(TestData.bookmark(BookmarkKind.AYAH, 2, contextId = 1, ordinal = 2, note = "note"))
        assertThat(marks.observeIsBookmarked(BookmarkKind.AYAH, 2).first()).isTrue()
        assertThat(marks.observeIsFavourite(BookmarkKind.AYAH, 2).first()).isFalse()

        // Favourite the same verse: one row, both flags.
        marks.upsert(marks.find(BookmarkKind.AYAH, 2)!!.copy(favourite = true))
        assertThat(marks.all()).hasSize(1)
        assertThat(marks.bookmarks(BookmarkKind.AYAH).first()).hasSize(1)
        assertThat(marks.favourites(BookmarkKind.AYAH).first()).hasSize(1)

        // Clearing the favourite keeps the bookmark and its note.
        marks.clearFavourite(BookmarkKind.AYAH, 2, now = 2_000)
        val remaining = marks.find(BookmarkKind.AYAH, 2)!!
        assertThat(remaining.bookmarked).isTrue()
        assertThat(remaining.favourite).isFalse()
        assertThat(remaining.note).isEqualTo("note")
    }

    @Test
    fun marksCarryTheSurahAndTheAyahNumber() = runTest {
        val marks = dbRule.userDb.bookmarkDao()
        marks.upsert(TestData.bookmark(BookmarkKind.AYAH, 262, contextId = 2, ordinal = 255))

        val row = marks.find(BookmarkKind.AYAH, 262)!!
        // What `surahNumber` and `ayahNumber` used to be: a mark is renderable without the corpus.
        assertThat(row.contextId).isEqualTo(2)
        assertThat(row.ordinal).isEqualTo(255)
        assertThat(marks.bookmarkedIds(BookmarkKind.AYAH)).containsExactly(262)
    }

    @Test
    fun readingProgress_isPersistedAndUpdated() = runTest {
        val progress = dbRule.userDb.readingProgressDao()

        progress.upsert(TestData.readingProgress(lastReadSurah = 2, lastReadAyah = 5))
        assertThat(progress.get()!!.lastReadSurah).isEqualTo(2)

        progress.upsert(progress.get()!!.copy(lastReadSurah = 18, lastReadAyah = 10))

        val updated = progress.observe().first()!!
        assertThat(updated.lastReadSurah).isEqualTo(18)
        assertThat(updated.lastReadAyah).isEqualTo(10)
    }

    /**
     * The whole point of the split: wiping the user's data cannot touch the corpus, because the
     * corpus is in another file.
     */
    @Test
    fun clearingUserDataLeavesTheCorpusAlone() = runTest {
        seedFatihah()
        val marks = dbRule.userDb.bookmarkDao()
        marks.upsert(TestData.bookmark(BookmarkKind.AYAH, 1, contextId = 1, ordinal = 1))
        marks.upsert(TestData.favourite(BookmarkKind.AYAH, 2))

        marks.clear()
        dbRule.userDb.readingProgressDao().clear()

        assertThat(marks.all()).isEmpty()
        assertThat(dao.getAllSurahs().first()).hasSize(1)
        assertThat(dao.getAyahsBySurah(1).first()).hasSize(3)
    }
}
