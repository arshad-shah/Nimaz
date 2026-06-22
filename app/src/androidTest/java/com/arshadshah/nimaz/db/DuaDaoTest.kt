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
    fun toggleFavorite_reflectsInFavoritesAndFlag() = runTest {
        seed()

        dao.toggleFavorite(duaId = 1, categoryId = 1)
        assertThat(dao.isDuaFavorite(1).first()).isTrue()
        assertThat(dao.getFavoriteDuas().first()).hasSize(1)

        dao.toggleFavorite(duaId = 1, categoryId = 1)
        assertThat(dao.isDuaFavorite(1).first()).isFalse()
    }

    @Test
    fun progress_incrementsTowardTarget() = runTest {
        seed()

        dao.incrementDuaProgress(duaId = 1, date = TestData.DAY, targetCount = 3)
        dao.incrementDuaProgress(duaId = 1, date = TestData.DAY, targetCount = 3)

        val progress = dao.getProgressForDuaOnDate(1, TestData.DAY)
        assertThat(progress).isNotNull()
        assertThat(progress!!.completedCount).isEqualTo(2)
    }

    @Test
    fun deleteAllUserData_clearsBookmarksAndProgress() = runTest {
        seed()
        dao.toggleFavorite(duaId = 1, categoryId = 1)
        dao.incrementDuaProgress(duaId = 1, date = TestData.DAY, targetCount = 3)

        dao.deleteAllUserData()

        assertThat(dao.getAllBookmarksSync()).isEmpty()
        assertThat(dao.getAllProgressSync()).isEmpty()
    }
}
