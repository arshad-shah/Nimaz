package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.data.local.database.dao.DuaDao
import com.arshadshah.nimaz.data.local.database.entity.DuaCategoryEntity
import com.arshadshah.nimaz.data.local.database.entity.DuaEntity
import com.arshadshah.nimaz.data.local.search.ContentSearchIndex
import com.arshadshah.nimaz.data.local.user.BookmarkDao
import com.arshadshah.nimaz.data.local.user.ProgressDao
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class DuaRepositoryImplTest {

    private lateinit var duaDao: DuaDao
    private lateinit var bookmarkDao: BookmarkDao
    private lateinit var progressDao: ProgressDao
    private lateinit var searchIndex: ContentSearchIndex
    private lateinit var repository: DuaRepositoryImpl

    private fun makeCategoryEntity(id: Int, nameEnglish: String = "Morning") = DuaCategoryEntity(
        id = id, nameEnglish = nameEnglish, nameArabic = "الصباح",
        icon = "sun", displayOrder = id, duaCount = 3
    )

    private fun makeDuaEntity(id: Int, categoryId: Int = 1) = DuaEntity(
        id = id, categoryId = categoryId,
        titleEnglish = "Morning Dua $id", titleArabic = "دعاء",
        textArabic = "بسم الله", transliteration = "Bismillah",
        translation = "In the name of Allah", source = "Quran",
        virtue = null, repeatCount = 1, audioFile = null, displayOrder = id
    )

    @Before
    fun setUp() {
        duaDao = mockk(relaxed = true)
        bookmarkDao = mockk(relaxed = true)
        progressDao = mockk(relaxed = true)
        searchIndex = mockk(relaxed = true)
        repository = DuaRepositoryImpl(duaDao, bookmarkDao, progressDao, searchIndex)
    }

    @Test
    fun `getAllCategories returns mapped domain categories`() = runTest {
        every { duaDao.getAllCategories() } returns flowOf(
            listOf(makeCategoryEntity(1), makeCategoryEntity(2, "Evening"))
        )

        val result = repository.getAllCategories().first()

        assertThat(result).hasSize(2)
        assertThat(result[0].nameEnglish).isEqualTo("Morning")
        assertThat(result[1].nameEnglish).isEqualTo("Evening")
    }

    @Test
    fun `getCategoryById returns mapped category`() = runTest {
        coEvery { duaDao.getCategoryById(1) } returns makeCategoryEntity(1)

        val result = repository.getCategoryById("1")

        assertThat(result).isNotNull()
        assertThat(result!!.nameEnglish).isEqualTo("Morning")
    }

    @Test
    fun `getCategoryById returns null for non-numeric id`() = runTest {
        val result = repository.getCategoryById("not-a-number")
        assertThat(result).isNull()
    }

    @Test
    fun `getCategoryById returns null when dao returns null`() = runTest {
        coEvery { duaDao.getCategoryById(any()) } returns null

        assertThat(repository.getCategoryById("999")).isNull()
    }

    @Test
    fun `getDuasByCategory returns flow of mapped duas`() = runTest {
        every { duaDao.getDuasByCategory(1) } returns flowOf(
            listOf(makeDuaEntity(1, 1), makeDuaEntity(2, 1))
        )

        val result = repository.getDuasByCategory("1").first()

        assertThat(result).hasSize(2)
        assertThat(result[0].titleEnglish).isEqualTo("Morning Dua 1")
    }

    @Test
    fun `getDuaById returns mapped dua`() = runTest {
        coEvery { duaDao.getDuaById(1) } returns makeDuaEntity(1)

        val result = repository.getDuaById("1")

        assertThat(result).isNotNull()
        assertThat(result!!.titleEnglish).isEqualTo("Morning Dua 1")
    }

    @Test
    fun `getDuaById returns null for non-numeric id`() = runTest {
        assertThat(repository.getDuaById("abc")).isNull()
    }

    @Test
    fun `getAllCategories returns empty when no categories`() = runTest {
        every { duaDao.getAllCategories() } returns flowOf(emptyList())

        val result = repository.getAllCategories().first()

        assertThat(result).isEmpty()
    }
}
