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

/** Hadith books/collection reads plus bookmark toggling. */
@RunWith(AndroidJUnit4::class)
class HadithDaoTest {

    @get:Rule
    val dbRule = NimazDbRule()
    private val dao get() = dbRule.db.hadithDao()

    private suspend fun seed() {
        dao.insertBooks(listOf(TestData.hadithBook(id = 1, nameEnglish = "Sahih Bukhari")))
        dao.insertHadiths(
            listOf(
                TestData.hadith(id = 1, bookId = 1, chapterId = 1, numberInBook = 1),
                TestData.hadith(id = 2, bookId = 1, chapterId = 1, numberInBook = 2),
            )
        )
    }

    @Test
    fun booksAndHadiths_roundTrip() = runTest {
        seed()

        assertThat(dao.getAllBooks().first()).hasSize(1)
        assertThat(dao.getBookById(1)!!.nameEnglish).isEqualTo("Sahih Bukhari")
        assertThat(dao.getHadithsByBook(1).first()).hasSize(2)
        assertThat(dao.getHadithCount()).isEqualTo(2)
    }

    @Test
    fun search_findsHadithByText() = runTest {
        seed()

        val results = dao.searchHadiths("intentions").first()

        assertThat(results.map { it.id }).contains(1)
    }

    @Test
    fun toggleBookmark_addsAndRemoves() = runTest {
        seed()
        assertThat(dao.isHadithBookmarked(1).first()).isFalse()

        dao.toggleBookmark(hadithId = 1, bookId = 1, hadithNumber = 1)
        assertThat(dao.isHadithBookmarked(1).first()).isTrue()
        assertThat(dao.getAllBookmarks().first()).hasSize(1)

        dao.toggleBookmark(hadithId = 1, bookId = 1, hadithNumber = 1)
        assertThat(dao.isHadithBookmarked(1).first()).isFalse()
    }

    @Test
    fun deleteAllUserData_clearsBookmarks() = runTest {
        seed()
        dao.toggleBookmark(hadithId = 2, bookId = 1, hadithNumber = 2)

        dao.deleteAllUserData()

        assertThat(dao.getAllBookmarksSync()).isEmpty()
    }
}
