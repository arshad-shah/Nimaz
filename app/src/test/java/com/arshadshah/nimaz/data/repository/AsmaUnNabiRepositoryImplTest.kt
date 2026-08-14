package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.data.local.database.dao.AsmaUnNabiDao
import com.arshadshah.nimaz.data.local.database.entity.AsmaUnNabiEntity
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

class AsmaUnNabiRepositoryImplTest {

    private lateinit var dao: AsmaUnNabiDao
    private lateinit var bookmarkDao: BookmarkDao
    private lateinit var repository: AsmaUnNabiRepositoryImpl

    private fun makeEntity(id: Int, nameEnglish: String = "The Praised") = AsmaUnNabiEntity(
        id = id, number = id, nameArabic = "المحمود",
        nameTransliteration = "Al-Mahmood", nameEnglish = nameEnglish,
        meaning = "Praised One", explanation = "The name praised by all",
        source = "Quran 17:79", displayOrder = id
    )

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        bookmarkDao = mockk(relaxed = true)
        repository = AsmaUnNabiRepositoryImpl(dao, bookmarkDao)

        every { bookmarkDao.favourites(BookmarkKind.ASMA_UN_NABI) } returns flowOf(emptyList())
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
            kind = BookmarkKind.ASMA_UN_NABI, targetId = 2,
            bookmarked = false, favourite = true,
            createdAt = 0L, updatedAt = 0L
        )
        every { dao.getAllNames() } returns flowOf(listOf(makeEntity(1), makeEntity(2)))
        every { bookmarkDao.favourites(BookmarkKind.ASMA_UN_NABI) } returns flowOf(listOf(bookmark))

        val result = repository.getAllNames().first()

        assertThat(result[0].isFavorite).isFalse()
        assertThat(result[1].isFavorite).isTrue()
    }

    @Test
    fun `getNameById returns mapped domain object`() = runTest {
        coEvery { dao.getNameById(3) } returns makeEntity(3, "The Trustworthy")
        coEvery { bookmarkDao.find(BookmarkKind.ASMA_UN_NABI, 3) } returns null

        val result = repository.getNameById(3)

        assertThat(result).isNotNull()
        assertThat(result!!.nameEnglish).isEqualTo("The Trustworthy")
        assertThat(result.isFavorite).isFalse()
    }

    @Test
    fun `getNameById returns null when not found`() = runTest {
        coEvery { dao.getNameById(any()) } returns null

        assertThat(repository.getNameById(999)).isNull()
    }

    @Test
    fun `entity fields map correctly to domain`() = runTest {
        val entity = makeEntity(1)
        every { dao.getAllNames() } returns flowOf(listOf(entity))

        val result = repository.getAllNames().first()
        val domain = result[0]

        assertThat(domain.nameArabic).isEqualTo("المحمود")
        assertThat(domain.nameTransliteration).isEqualTo("Al-Mahmood")
        assertThat(domain.meaning).isEqualTo("Praised One")
        assertThat(domain.source).isEqualTo("Quran 17:79")
        assertThat(domain.displayOrder).isEqualTo(1)
    }

    @Test
    fun `isFavorite returns false when no bookmark`() = runTest {
        coEvery { bookmarkDao.find(BookmarkKind.ASMA_UN_NABI, 1) } returns null

        assertThat(repository.isFavorite(1)).isFalse()
    }

    @Test
    fun `getAllNames returns empty list when no names`() = runTest {
        every { dao.getAllNames() } returns flowOf(emptyList())

        val result = repository.getAllNames().first()

        assertThat(result).isEmpty()
    }
}
