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

/** Dua categories/content, favorites, and per-day recitation progress. */
@RunWith(AndroidJUnit4::class)
class DuaDaoTest {

    @get:Rule
    val dbRule = NimazDbRule()
    private val dao get() = dbRule.db.duaDao()

    private suspend fun seed() {
        dao.insertCategories(listOf(TestData.duaCategory(id = 1, nameEnglish = "Morning")))
        dao.insertDuas(
            listOf(
                TestData.dua(id = 1, categoryId = 1, titleEnglish = "Morning Dua"),
                TestData.dua(id = 2, categoryId = 1, titleEnglish = "Evening Dua"),
            )
        )
    }

    @Test
    fun categoriesAndDuas_roundTrip() = runTest {
        seed()

        assertThat(dao.getAllCategories().first()).hasSize(1)
        assertThat(dao.getDuasByCategory(1).first()).hasSize(2)
        assertThat(dao.getDuaById(1)!!.titleEnglish).isEqualTo("Morning Dua")
    }

    @Test
    fun search_matchesByTitle() = runTest {
        seed()

        val results = dao.searchDuas("Evening").first()

        assertThat(results.map { it.id }).contains(2)
    }

    @Test
    fun favouriteDua_isARowInTheUsersDatabase() = runTest {
        seed()
        val marks = dbRule.userDb.bookmarkDao()

        marks.upsert(TestData.favourite(BookmarkKind.DUA, 1, bookmarked = true))
        assertThat(marks.observeIsFavourite(BookmarkKind.DUA, 1).first()).isTrue()
        assertThat(marks.favourites(BookmarkKind.DUA).first()).hasSize(1)
        // The category the dua belongs to travels with the mark.
        marks.upsert(TestData.bookmark(BookmarkKind.DUA, 2, contextId = 1))
        assertThat(marks.inContext(BookmarkKind.DUA, 1).first().map { it.targetId })
            .containsExactly(2)

        marks.delete(BookmarkKind.DUA, 1)
        assertThat(marks.observeIsFavourite(BookmarkKind.DUA, 1).first()).isFalse()
    }

    @Test
    fun progress_incrementsTowardTarget() = runTest {
        seed()
        val progress = dbRule.userDb.progressDao()

        progress.increment(ProgressKind.DUA, targetId = 1, date = TestData.DAY, target = 3)
        progress.increment(ProgressKind.DUA, targetId = 1, date = TestData.DAY, target = 3)

        val row = progress.find(ProgressKind.DUA, 1, TestData.DAY)
        assertThat(row).isNotNull()
        assertThat(row!!.completed).isEqualTo(2)
        assertThat(row.total).isEqualTo(3)
        assertThat(row.isCompleted).isFalse()

        // The third reaches the target, and a fourth is still counted.
        progress.increment(ProgressKind.DUA, targetId = 1, date = TestData.DAY, target = 3)
        assertThat(progress.find(ProgressKind.DUA, 1, TestData.DAY)!!.isCompleted).isTrue()

        progress.decrement(ProgressKind.DUA, targetId = 1, date = TestData.DAY)
        assertThat(progress.find(ProgressKind.DUA, 1, TestData.DAY)!!.completed).isEqualTo(2)
    }

    /** Per-day, so yesterday's count is its own row and is not overwritten. */
    @Test
    fun progress_isKeptPerDay() = runTest {
        seed()
        val progress = dbRule.userDb.progressDao()

        progress.increment(ProgressKind.DUA, targetId = 1, date = TestData.DAY, target = 3)
        progress.increment(ProgressKind.DUA, targetId = 1, date = TestData.DAY + 86_400_000, target = 3)

        assertThat(progress.ofKind(ProgressKind.DUA).first()).hasSize(2)
        assertThat(progress.onDate(ProgressKind.DUA, TestData.DAY).first()).hasSize(1)
    }

    @Test
    fun clearingUserDataLeavesTheDuasAlone() = runTest {
        seed()
        val marks = dbRule.userDb.bookmarkDao()
        marks.upsert(TestData.favourite(BookmarkKind.DUA, 1))
        dbRule.userDb.progressDao()
            .increment(ProgressKind.DUA, targetId = 1, date = TestData.DAY, target = 3)

        marks.clear()
        dbRule.userDb.progressDao().deleteKind(ProgressKind.DUA)

        assertThat(marks.all()).isEmpty()
        assertThat(dbRule.userDb.progressDao().all()).isEmpty()
        // The content is in the other database and untouched.
        assertThat(dao.getAllCategories().first()).isNotEmpty()
    }
}
