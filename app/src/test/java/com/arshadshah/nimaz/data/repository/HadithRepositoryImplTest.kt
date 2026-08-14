package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.data.local.database.dao.HadithDao
import com.arshadshah.nimaz.data.local.database.entity.HadithBookEntity
import com.arshadshah.nimaz.data.local.database.entity.HadithEntity
import com.arshadshah.nimaz.data.local.search.ContentSearchIndex
import com.arshadshah.nimaz.data.local.user.BookmarkDao
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class HadithRepositoryImplTest {

    private lateinit var hadithDao: HadithDao
    private lateinit var bookmarkDao: BookmarkDao
    private lateinit var searchIndex: ContentSearchIndex
    private lateinit var repository: HadithRepositoryImpl

    private fun makeBookEntity(id: Int = 1) = HadithBookEntity(
        id = id, nameEnglish = "Sahih al-Bukhari", nameArabic = "البخاري",
        author = "Al-Bukhari", hadithCount = 7563, description = "Authentic collection",
        icon = "bukhari_icon"
    )

    private fun makeHadithEntity(id: Int = 1, bookId: Int = 1, chapterId: Int = 1) = HadithEntity(
        id = id, bookId = bookId, chapterId = chapterId, numberInBook = id,
        numberInChapter = id, textArabic = "إنما الأعمال بالنيات",
        textEnglish = "Actions are by intentions", narrator = "Umar",
        grade = "sahih", reference = "bukhari:1", narratorChain = null
    )

    @Before
    fun setUp() {
        hadithDao = mockk(relaxed = true)
        bookmarkDao = mockk(relaxed = true)
        searchIndex = mockk(relaxed = true)
        repository = HadithRepositoryImpl(hadithDao, bookmarkDao, searchIndex)
    }

    @Test
    fun `getAllBooks returns flow of mapped books`() = runTest {
        every { hadithDao.getAllBooks() } returns flowOf(listOf(makeBookEntity(1)))

        val result = repository.getAllBooks().first()

        assertThat(result).hasSize(1)
        assertThat(result[0].nameEnglish).isEqualTo("Sahih al-Bukhari")
    }

    @Test
    fun `getAllBooks returns empty when no books`() = runTest {
        every { hadithDao.getAllBooks() } returns flowOf(emptyList())

        val result = repository.getAllBooks().first()

        assertThat(result).isEmpty()
    }

    @Test
    fun `getBookById returns mapped book for numeric id`() = runTest {
        coEvery { hadithDao.getBookById(1) } returns makeBookEntity(1)

        val result = repository.getBookById("1")

        assertThat(result).isNotNull()
        assertThat(result!!.nameEnglish).isEqualTo("Sahih al-Bukhari")
    }

    @Test
    fun `getBookById returns null for non-numeric id`() = runTest {
        val result = repository.getBookById("not-a-number")
        assertThat(result).isNull()
    }

    @Test
    fun `getBookById returns null when not found`() = runTest {
        coEvery { hadithDao.getBookById(any()) } returns null

        assertThat(repository.getBookById("999")).isNull()
    }

    @Test
    fun `getHadithsByChapter returns flow of hadiths`() = runTest {
        every { hadithDao.getHadithsByChapter(1) } returns flowOf(
            listOf(makeHadithEntity(1), makeHadithEntity(2))
        )

        val result = repository.getHadithsByChapter("1").first()

        assertThat(result).hasSize(2)
    }

    @Test
    fun `getHadithById returns mapped hadith`() = runTest {
        coEvery { hadithDao.getHadithById(1) } returns makeHadithEntity(1)

        val result = repository.getHadithById("1")

        assertThat(result).isNotNull()
        assertThat(result!!.textEnglish).isEqualTo("Actions are by intentions")
        assertThat(result.reference).isEqualTo("bukhari:1")
    }

    @Test
    fun `getHadithById returns null for non-numeric id`() = runTest {
        assertThat(repository.getHadithById("not-a-number")).isNull()
    }

    @Test
    fun `getHadithCount delegates to dao`() = runTest {
        coEvery { hadithDao.getHadithCount() } returns 7563

        assertThat(repository.getHadithCount()).isEqualTo(7563)
    }

    @Test
    fun `getHadithOfTheDay returns null when no hadiths in dao`() = runTest {
        coEvery { hadithDao.getHadithCount() } returns 0

        val result = repository.getHadithOfTheDay()

        assertThat(result).isNull()
    }
}
