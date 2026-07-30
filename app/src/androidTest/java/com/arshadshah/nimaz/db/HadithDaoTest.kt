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
    fun bookmarkedHadith_keepsItsBookAndNumber() = runTest {
        seed()
        val marks = dbRule.userDb.bookmarkDao()
        assertThat(marks.observeIsBookmarked(BookmarkKind.HADITH, 1).first()).isFalse()

        marks.upsert(TestData.bookmark(BookmarkKind.HADITH, 1, contextId = 1, ordinal = 1))
        assertThat(marks.observeIsBookmarked(BookmarkKind.HADITH, 1).first()).isTrue()
        assertThat(marks.bookmarks(BookmarkKind.HADITH).first()).hasSize(1)
        assertThat(marks.inContext(BookmarkKind.HADITH, 1).first()).hasSize(1)

        marks.delete(BookmarkKind.HADITH, 1)
        assertThat(marks.observeIsBookmarked(BookmarkKind.HADITH, 1).first()).isFalse()
    }

    @Test
    fun clearingUserDataLeavesTheHadithsAlone() = runTest {
        seed()
        val marks = dbRule.userDb.bookmarkDao()
        marks.upsert(TestData.bookmark(BookmarkKind.HADITH, 2, contextId = 1, ordinal = 2))

        marks.clear()

        assertThat(marks.all()).isEmpty()
        assertThat(dao.getAllBooks().first()).isNotEmpty()
    }
}
