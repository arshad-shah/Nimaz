package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.data.local.database.dao.ProphetDao
import com.arshadshah.nimaz.data.local.database.entity.ProphetEntity
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

class ProphetRepositoryImplTest {

    private lateinit var dao: ProphetDao
    private lateinit var bookmarkDao: BookmarkDao
    private lateinit var repository: ProphetRepositoryImpl

    private fun makeEntity(id: Int, nameEnglish: String = "Adam") = ProphetEntity(
        id = id, number = id, nameArabic = "آدم", nameEnglish = nameEnglish,
        nameTransliteration = nameEnglish, titleArabic = "", titleEnglish = "Father",
        storySummary = "First prophet", keyLessons = "[]", quranMentions = "[]",
        era = "Primordial", lineage = "", yearsLived = "930",
        placeOfPreaching = "Earth", miracles = "[]", displayOrder = id
    )

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        bookmarkDao = mockk(relaxed = true)
        repository = ProphetRepositoryImpl(dao, bookmarkDao)

        // Default: no bookmarks
        every { bookmarkDao.favourites(BookmarkKind.PROPHET) } returns flowOf(emptyList())
    }

    @Test
    fun `getAllProphets maps entities to domain models`() = runTest {
        every { dao.getAllProphets() } returns flowOf(listOf(makeEntity(1), makeEntity(2)))

        val result = repository.getAllProphets().first()

        assertThat(result).hasSize(2)
        assertThat(result[0].id).isEqualTo(1)
        assertThat(result[1].id).isEqualTo(2)
    }

    @Test
    fun `getAllProphets marks bookmarked prophets as favorite`() = runTest {
        val bookmark = BookmarkEntity(
            kind = BookmarkKind.PROPHET, targetId = 1,
            bookmarked = false, favourite = true,
            createdAt = 0L, updatedAt = 0L
        )
        every { dao.getAllProphets() } returns flowOf(listOf(makeEntity(1), makeEntity(2)))
        every { bookmarkDao.favourites(BookmarkKind.PROPHET) } returns flowOf(listOf(bookmark))

        val result = repository.getAllProphets().first()

        assertThat(result[0].isFavorite).isTrue()
        assertThat(result[1].isFavorite).isFalse()
    }

    @Test
    fun `getAllProphets returns empty when no prophets`() = runTest {
        every { dao.getAllProphets() } returns flowOf(emptyList())

        val result = repository.getAllProphets().first()

        assertThat(result).isEmpty()
    }

    @Test
    fun `getProphetById maps entity to domain`() = runTest {
        val entity = makeEntity(1, "Adam")
        coEvery { dao.getProphetById(1) } returns entity
        coEvery { bookmarkDao.find(BookmarkKind.PROPHET, 1) } returns null

        val result = repository.getProphetById(1)

        assertThat(result).isNotNull()
        assertThat(result!!.nameEnglish).isEqualTo("Adam")
        assertThat(result.isFavorite).isFalse()
    }

    @Test
    fun `getProphetById returns null when not found`() = runTest {
        coEvery { dao.getProphetById(any()) } returns null

        val result = repository.getProphetById(999)

        assertThat(result).isNull()
    }

    @Test
    fun `getProphetById returns prophet marked as favorite when bookmarked`() = runTest {
        val entity = makeEntity(1)
        val bookmark = BookmarkEntity(
            kind = BookmarkKind.PROPHET, targetId = 1,
            bookmarked = false, favourite = true,
            createdAt = 0L, updatedAt = 0L
        )
        coEvery { dao.getProphetById(1) } returns entity
        coEvery { bookmarkDao.find(BookmarkKind.PROPHET, 1) } returns bookmark

        val result = repository.getProphetById(1)

        assertThat(result!!.isFavorite).isTrue()
    }

    @Test
    fun `isFavorite returns true when bookmarked as favourite`() = runTest {
        val bookmark = BookmarkEntity(
            kind = BookmarkKind.PROPHET, targetId = 1,
            bookmarked = false, favourite = true,
            createdAt = 0L, updatedAt = 0L
        )
        coEvery { bookmarkDao.find(BookmarkKind.PROPHET, 1) } returns bookmark

        assertThat(repository.isFavorite(1)).isTrue()
    }

    @Test
    fun `isFavorite returns false when no bookmark`() = runTest {
        coEvery { bookmarkDao.find(BookmarkKind.PROPHET, 1) } returns null

        assertThat(repository.isFavorite(1)).isFalse()
    }

    @Test
    fun `entity with valid JSON arrays maps to domain correctly`() = runTest {
        val entity = makeEntity(1).copy(
            keyLessons = """["Repentance","Humility"]""",
            quranMentions = """["2:31","7:11"]""",
            miracles = """["Speaking in cradle"]"""
        )
        every { dao.getAllProphets() } returns flowOf(listOf(entity))

        val result = repository.getAllProphets().first()

        assertThat(result[0].keyLessons).containsExactly("Repentance", "Humility")
        assertThat(result[0].quranMentions).containsExactly("2:31", "7:11")
    }
}
