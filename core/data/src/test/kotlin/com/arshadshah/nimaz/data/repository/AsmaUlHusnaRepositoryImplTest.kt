package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.data.local.database.dao.AsmaUlHusnaDao
import com.arshadshah.nimaz.data.local.database.entity.AsmaUlHusnaEntity
import com.arshadshah.nimaz.data.local.user.BookmarkDao
import com.arshadshah.nimaz.data.local.user.BookmarkEntity
import com.arshadshah.nimaz.data.local.user.BookmarkKind
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class AsmaUlHusnaRepositoryImplTest {

    private lateinit var dao: AsmaUlHusnaDao
    private lateinit var bookmarkDao: BookmarkDao
    private lateinit var repository: AsmaUlHusnaRepositoryImpl

    private fun makeEntity(id: Int, nameEnglish: String = "The Merciful") = AsmaUlHusnaEntity(
        id = id, number = id, nameArabic = "الرحيم",
        nameTransliteration = "Ar-Rahim", nameEnglish = nameEnglish,
        meaning = "The Merciful", explanation = "Very merciful",
        benefits = "Great benefits", quranReferences = "[]",
        usageInDua = "Use in morning dua", displayOrder = id
    )

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        bookmarkDao = mockk(relaxed = true)
        repository = AsmaUlHusnaRepositoryImpl(dao, bookmarkDao)

        every { bookmarkDao.favourites(BookmarkKind.ASMA_UL_HUSNA) } returns flowOf(emptyList())
    }

    @Test
    fun `getAllNames returns mapped domain objects`() = runTest {
        every { dao.getAllNames() } returns flowOf(listOf(makeEntity(1), makeEntity(2)))

        val result = repository.getAllNames().first()

        assertThat(result).hasSize(2)
        assertThat(result[0].id).isEqualTo(1)
        assertThat(result[1].id).isEqualTo(2)
    }

    @Test
    fun `getAllNames marks favorited names`() = runTest {
        val bookmark = BookmarkEntity(
            kind = BookmarkKind.ASMA_UL_HUSNA, targetId = 1,
            bookmarked = false, favourite = true,
            createdAt = 0L, updatedAt = 0L
        )
        every { dao.getAllNames() } returns flowOf(listOf(makeEntity(1), makeEntity(2)))
        every { bookmarkDao.favourites(BookmarkKind.ASMA_UL_HUSNA) } returns flowOf(listOf(bookmark))

        val result = repository.getAllNames().first()

        assertThat(result[0].isFavorite).isTrue()
        assertThat(result[1].isFavorite).isFalse()
    }

    @Test
    fun `getNameById returns mapped domain object`() = runTest {
        coEvery { dao.getNameById(1) } returns makeEntity(1, "The King")
        coEvery { bookmarkDao.find(BookmarkKind.ASMA_UL_HUSNA, 1) } returns null

        val result = repository.getNameById(1)

        assertThat(result).isNotNull()
        assertThat(result!!.nameEnglish).isEqualTo("The King")
        assertThat(result.isFavorite).isFalse()
    }

    @Test
    fun `getNameById returns null when not found`() = runTest {
        coEvery { dao.getNameById(any()) } returns null

        assertThat(repository.getNameById(999)).isNull()
    }

    @Test
    fun `entity with JSON quranReferences maps correctly`() = runTest {
        val entity = makeEntity(1).copy(quranReferences = """["2:255","59:22"]""")
        every { dao.getAllNames() } returns flowOf(listOf(entity))

        val result = repository.getAllNames().first()

        assertThat(result[0].quranReferences).containsExactly("2:255", "59:22")
    }

    @Test
    fun `entity with empty JSON array maps to empty list`() = runTest {
        val entity = makeEntity(1).copy(quranReferences = "[]")
        every { dao.getAllNames() } returns flowOf(listOf(entity))

        val result = repository.getAllNames().first()

        assertThat(result[0].quranReferences).isEmpty()
    }

    @Test
    fun `isFavorite returns false when not bookmarked`() = runTest {
        coEvery { bookmarkDao.find(BookmarkKind.ASMA_UL_HUSNA, 1) } returns null

        assertThat(repository.isFavorite(1)).isFalse()
    }
}
