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

    @Test
    fun bookmark_toggleReflectsInIsBookmarkedFlow() = runTest {
        seedFatihah()
        assertThat(dao.isAyahBookmarked(2).first()).isFalse()

        dao.toggleBookmark(ayahId = 2, surahNumber = 1, ayahNumber = 2)
        assertThat(dao.isAyahBookmarked(2).first()).isTrue()
        assertThat(dao.getAllBookmarks().first()).hasSize(1)

        dao.toggleBookmark(ayahId = 2, surahNumber = 1, ayahNumber = 2)
        assertThat(dao.isAyahBookmarked(2).first()).isFalse()
    }

    @Test
    fun favorite_toggleAddsAndRemoves() = runTest {
        seedFatihah()

        dao.toggleFavorite(ayahId = 3, surahNumber = 1, ayahNumber = 3)
        assertThat(dao.isAyahFavorite(3).first()).isTrue()
        assertThat(dao.getFavoriteAyahIds().first()).contains(3)

        dao.toggleFavorite(ayahId = 3, surahNumber = 1, ayahNumber = 3)
        assertThat(dao.isAyahFavorite(3).first()).isFalse()
    }

    @Test
    fun readingProgress_isPersistedAndUpdated() = runTest {
        dao.insertReadingProgress(TestData.readingProgress(lastReadSurah = 2, lastReadAyah = 5))
        assertThat(dao.getReadingProgress().first()!!.lastReadSurah).isEqualTo(2)

        dao.updateReadingPosition(surah = 18, ayah = 10, page = 295, juz = 15)

        val updated = dao.getReadingProgress().first()!!
        assertThat(updated.lastReadSurah).isEqualTo(18)
        assertThat(updated.lastReadAyah).isEqualTo(10)
    }

    @Test
    fun deleteAllUserData_keepsContentButClearsBookmarksAndFavorites() = runTest {
        seedFatihah()
        dao.toggleBookmark(ayahId = 1, surahNumber = 1, ayahNumber = 1)
        dao.toggleFavorite(ayahId = 1, surahNumber = 1, ayahNumber = 1)

        dao.deleteAllUserData()

        assertThat(dao.getAllBookmarks().first()).isEmpty()
        assertThat(dao.getAllFavorites().first()).isEmpty()
        // Scripture content must survive a user-data wipe.
        assertThat(dao.getAllSurahs().first()).hasSize(1)
    }
}
