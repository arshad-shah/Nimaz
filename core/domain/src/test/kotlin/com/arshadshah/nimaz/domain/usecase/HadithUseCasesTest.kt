package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.Hadith
import com.arshadshah.nimaz.domain.model.HadithBook
import com.arshadshah.nimaz.domain.model.HadithGrade
import com.arshadshah.nimaz.domain.repository.HadithRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class HadithUseCasesTest {

    private lateinit var repo: HadithRepository
    private lateinit var useCases: HadithUseCases

    private val book = HadithBook(
        id = "bukhari", nameArabic = "البخاري", nameEnglish = "Sahih al-Bukhari",
        authorName = "Al-Bukhari", authorArabic = "البخاري",
        totalHadiths = 7563, totalChapters = 97,
        description = null, displayOrder = 1
    )

    private val hadith = Hadith(
        id = "bukhari_1", bookId = "bukhari", chapterId = "1",
        hadithNumber = 1, hadithNumberInBook = 1,
        textArabic = "إنما الأعمال بالنيات", textEnglish = "Actions are by intentions",
        narratorChain = "Umar ibn al-Khattab", narratorName = "Umar",
        grade = HadithGrade.SAHIH, gradeArabic = "صحيح",
        reference = "bukhari:1", isBookmarked = false
    )

    @Before
    fun setUp() {
        repo = mockk(relaxed = true)
        useCases = HadithUseCases(
            getAllBooks = GetAllBooksUseCase(repo),
            getBookById = GetBookByIdUseCase(repo),
            getChaptersByBook = GetChaptersByBookUseCase(repo),
            getChapterById = GetChapterByIdUseCase(repo),
            getHadithsByChapter = GetHadithsByChapterUseCase(repo),
            getHadithById = GetHadithByIdUseCase(repo),
            getHadithByNumber = GetHadithByNumberUseCase(repo),
            getHadithByReference = GetHadithByReferenceUseCase(repo),
            getHadithsByGrade = GetHadithsByGradeUseCase(repo),
            getHadithOfTheDay = GetHadithOfTheDayUseCase(repo),
            searchHadiths = SearchHadithsUseCase(repo),
            searchHadithsInBook = SearchHadithsInBookUseCase(repo),
            getAllBookmarks = GetAllBookmarksUseCase(repo),
            isHadithBookmarked = IsHadithBookmarkedUseCase(repo),
            toggleBookmark = ToggleBookmarkUseCase(repo),
            insertBookmark = InsertHadithBookmarkUseCase(repo),
            updateBookmark = UpdateHadithBookmarkUseCase(repo),
            deleteBookmark = DeleteHadithBookmarkUseCase(repo),
            getDailyHadith = GetDailyHadithUseCase(repo)
        )
    }

    @Test
    fun `getAllBooks returns flow of books`() = runTest {
        every { repo.getAllBooks() } returns flowOf(listOf(book))

        val result = useCases.getAllBooks().first()

        assertThat(result).hasSize(1)
        assertThat(result[0].nameEnglish).isEqualTo("Sahih al-Bukhari")
    }

    @Test
    fun `getBookById returns book when found`() = runTest {
        coEvery { repo.getBookById("bukhari") } returns book

        val result = useCases.getBookById("bukhari")

        assertThat(result).isNotNull()
        assertThat(result!!.id).isEqualTo("bukhari")
    }

    @Test
    fun `getBookById returns null when not found`() = runTest {
        coEvery { repo.getBookById(any()) } returns null

        assertThat(useCases.getBookById("unknown")).isNull()
    }

    @Test
    fun `getHadithById returns correct hadith`() = runTest {
        coEvery { repo.getHadithById("bukhari_1") } returns hadith

        val result = useCases.getHadithById("bukhari_1")

        assertThat(result).isNotNull()
        assertThat(result!!.textEnglish).isEqualTo("Actions are by intentions")
    }

    @Test
    fun `getHadithByReference resolves canonical reference`() = runTest {
        coEvery { repo.getHadithByReference("bukhari:1") } returns hadith

        val result = useCases.getHadithByReference("bukhari:1")

        assertThat(result).isNotNull()
        assertThat(result!!.reference).isEqualTo("bukhari:1")
    }

    @Test
    fun `getHadithsByChapter returns flow of hadiths`() = runTest {
        every { repo.getHadithsByChapter("1") } returns flowOf(listOf(hadith))

        val result = useCases.getHadithsByChapter("1").first()

        assertThat(result).hasSize(1)
        assertThat(result[0].grade).isEqualTo(HadithGrade.SAHIH)
    }

    @Test
    fun `getHadithOfTheDay returns a hadith`() = runTest {
        coEvery { repo.getHadithOfTheDay() } returns hadith

        val result = useCases.getHadithOfTheDay()

        assertThat(result).isNotNull()
    }

    @Test
    fun `getHadithOfTheDay returns null when no hadiths`() = runTest {
        coEvery { repo.getHadithOfTheDay() } returns null

        assertThat(useCases.getHadithOfTheDay()).isNull()
    }
}
