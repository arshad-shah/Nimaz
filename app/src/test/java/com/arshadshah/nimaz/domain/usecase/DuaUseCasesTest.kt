package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.Dua
import com.arshadshah.nimaz.domain.model.DuaCategory
import com.arshadshah.nimaz.domain.model.DuaOccasion
import com.arshadshah.nimaz.domain.repository.DuaRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class DuaUseCasesTest {

    private lateinit var repo: DuaRepository
    private lateinit var useCases: DuaUseCases

    private val category = DuaCategory(
        id = "morning", nameArabic = "الصباح", nameEnglish = "Morning",
        description = null, iconName = "sun", displayOrder = 1, duaCount = 5
    )

    private val dua = Dua(
        id = "dua_1", categoryId = "morning",
        titleArabic = "دعاء الصباح", titleEnglish = "Morning Dua",
        textArabic = "بِسْمِ اللَّهِ", textTransliteration = "Bismillah",
        textEnglish = "In the name of Allah", reference = "Quran 1:1",
        occasion = DuaOccasion.MORNING, benefits = null, repeatCount = 1,
        audioUrl = null, displayOrder = 1, isFavorite = false, isBookmarked = false
    )

    @Before
    fun setUp() {
        repo = mockk(relaxed = true)
        useCases = DuaUseCases(
            getAllCategories = GetAllCategoriesUseCase(repo),
            getCategoryById = GetCategoryByIdUseCase(repo),
            getDuaById = GetDuaByIdUseCase(repo),
            getDuasByCategory = GetDuasByCategoryUseCase(repo),
            getDuasByOccasion = GetDuasByOccasionUseCase(repo),
            getFavoriteDuas = GetFavoriteDuasUseCase(repo),
            getProgressForDate = GetProgressForDateUseCase(repo),
            isDuaFavorite = IsDuaFavoriteUseCase(repo),
            searchDuas = SearchDuasUseCase(repo),
            toggleFavorite = ToggleDuaFavoriteUseCase(repo),
            getAllBookmarks = GetDuaBookmarksUseCase(repo),
            insertBookmark = InsertDuaBookmarkUseCase(repo),
            updateBookmark = UpdateDuaBookmarkUseCase(repo),
            deleteBookmark = DeleteDuaBookmarkUseCase(repo),
            getDailyDua = GetDailyDuaUseCase(repo)
        )
    }

    @Test
    fun `getAllCategories returns flow of categories`() = runTest {
        every { repo.getAllCategories() } returns flowOf(listOf(category))

        val result = useCases.getAllCategories().first()

        assertThat(result).hasSize(1)
        assertThat(result[0].nameEnglish).isEqualTo("Morning")
    }

    @Test
    fun `getCategoryById returns category when found`() = runTest {
        coEvery { repo.getCategoryById("morning") } returns category

        val result = useCases.getCategoryById("morning")

        assertThat(result).isNotNull()
        assertThat(result!!.id).isEqualTo("morning")
    }

    @Test
    fun `getDuaById returns dua when found`() = runTest {
        coEvery { repo.getDuaById("dua_1") } returns dua

        val result = useCases.getDuaById("dua_1")

        assertThat(result).isNotNull()
        assertThat(result!!.titleEnglish).isEqualTo("Morning Dua")
    }

    @Test
    fun `getDuaById returns null when not found`() = runTest {
        coEvery { repo.getDuaById(any()) } returns null

        assertThat(useCases.getDuaById("unknown")).isNull()
    }

    @Test
    fun `getDuasByCategory returns flow of duas`() = runTest {
        every { repo.getDuasByCategory("morning") } returns flowOf(listOf(dua))

        val result = useCases.getDuasByCategory("morning").first()

        assertThat(result).hasSize(1)
        assertThat(result[0].id).isEqualTo("dua_1")
    }

    @Test
    fun `isDuaFavorite returns flow of boolean`() = runTest {
        every { repo.isDuaFavorite("dua_1") } returns flowOf(true)

        val result = useCases.isDuaFavorite("dua_1").first()

        assertThat(result).isTrue()
    }

    @Test
    fun `toggleFavorite delegates to repo`() = runTest {
        useCases.toggleFavorite("dua_1", "morning")
        coVerify { repo.toggleFavorite("dua_1", "morning") }
    }
}
