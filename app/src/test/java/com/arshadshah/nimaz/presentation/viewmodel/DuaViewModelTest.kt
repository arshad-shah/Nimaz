package com.arshadshah.nimaz.presentation.viewmodel

import com.arshadshah.nimaz.domain.model.DuaCategory
import com.arshadshah.nimaz.domain.model.DuaProgress
import com.arshadshah.nimaz.domain.repository.DuaRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DuaViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: DuaRepository
    private lateinit var viewModel: DuaViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        every { repository.getAllCategories() } returns flowOf(emptyList())
        every { repository.getFavoriteDuas() } returns flowOf(emptyList())
        every { repository.getProgressForDate(any()) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = DuaViewModel(repository)

    private fun category(
        id: String,
        nameEnglish: String,
        nameArabic: String = "",
        description: String? = null
    ) = DuaCategory(
        id = id, nameArabic = nameArabic, nameEnglish = nameEnglish,
        description = description, iconName = null, displayOrder = 0, duaCount = 0
    )

    private fun progress(duaId: String, completedCount: Int) = DuaProgress(
        id = 1, duaId = duaId, date = 0L, completedCount = completedCount,
        targetCount = 33, isCompleted = false, createdAt = 0L
    )

    // ── Init ────────────────────────────────────────────────────────

    @Test
    fun `init loads categories and finishes loading`() = runTest {
        val cats = listOf(category("1", "Morning"), category("2", "Evening"))
        every { repository.getAllCategories() } returns flowOf(cats)

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.collectionState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.categories).hasSize(2)
        assertThat(state.filteredCategories).hasSize(2)
    }

    // ── Loading a category ──────────────────────────────────────────

    @Test
    fun `LoadCategory populates the category and its duas`() = runTest {
        val cat = category("morning", "Morning Adhkar")
        coEvery { repository.getCategoryById("morning") } returns cat
        every { repository.getDuasByCategory("morning") } returns flowOf(emptyList())

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(DuaEvent.LoadCategory("morning"))
        advanceUntilIdle()

        val state = viewModel.categoryState.value
        assertThat(state.category).isEqualTo(cat)
        assertThat(state.isLoading).isFalse()
    }

    // ── Search ──────────────────────────────────────────────────────

    @Test
    fun `Search with a blank query resets the search state`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(DuaEvent.Search("   "))
        advanceUntilIdle()

        assertThat(viewModel.searchState.value.query).isEmpty()
        assertThat(viewModel.searchState.value.isSearching).isFalse()
        coVerify(exactly = 0) { repository.searchDuas(any()) }
    }

    @Test
    fun `Search with a query collects results from the repository`() = runTest {
        every { repository.searchDuas("rain") } returns flowOf(emptyList())

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(DuaEvent.Search("rain"))
        advanceUntilIdle()

        assertThat(viewModel.searchState.value.query).isEqualTo("rain")
        assertThat(viewModel.searchState.value.isSearching).isFalse()
        coVerify { repository.searchDuas("rain") }
    }

    @Test
    fun `SearchCategories filters the loaded categories by english name`() = runTest {
        val cats = listOf(
            category("1", "Morning Adhkar"),
            category("2", "Evening Adhkar"),
            category("3", "Travel")
        )
        every { repository.getAllCategories() } returns flowOf(cats)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(DuaEvent.SearchCategories("morning"))

        val state = viewModel.collectionState.value
        assertThat(state.searchQuery).isEqualTo("morning")
        assertThat(state.filteredCategories.map { it.id }).containsExactly("1")
    }

    @Test
    fun `ClearSearch restores all categories`() = runTest {
        val cats = listOf(category("1", "Morning"), category("2", "Evening"))
        every { repository.getAllCategories() } returns flowOf(cats)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(DuaEvent.SearchCategories("morning"))
        assertThat(viewModel.collectionState.value.filteredCategories).hasSize(1)

        viewModel.onEvent(DuaEvent.ClearSearch)
        assertThat(viewModel.collectionState.value.filteredCategories).hasSize(2)
        assertThat(viewModel.collectionState.value.searchQuery).isEmpty()
    }

    // ── Favorites & progress ────────────────────────────────────────

    @Test
    fun `ToggleFavorite delegates to the repository`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(DuaEvent.ToggleFavorite("dua1", "cat1"))
        advanceUntilIdle()

        coVerify { repository.toggleFavorite("dua1", "cat1") }
    }

    @Test
    fun `IncrementProgress increments then reloads the progress`() = runTest {
        coEvery { repository.getProgressForDuaOnDate("dua1", any()) } returns progress("dua1", 1)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(DuaEvent.IncrementProgress("dua1", 33))
        advanceUntilIdle()

        coVerify { repository.incrementDuaProgress("dua1", any(), 33) }
        assertThat(viewModel.readerState.value.progress?.completedCount).isEqualTo(1)
    }

    @Test
    fun `DecrementProgress decrements when the current count is positive`() = runTest {
        // Seed reader progress via LoadDua so completedCount starts at 3.
        coEvery { repository.getDuaById("dua1") } returns null
        coEvery { repository.getProgressForDuaOnDate("dua1", any()) } returns progress("dua1", 3)
        every { repository.isDuaFavorite("dua1") } returns flowOf(false)

        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(DuaEvent.LoadDua("dua1"))
        advanceUntilIdle()

        viewModel.onEvent(DuaEvent.DecrementProgress("dua1"))
        advanceUntilIdle()

        coVerify { repository.decrementDuaProgress("dua1", any()) }
    }

    @Test
    fun `DecrementProgress does nothing when the count is already zero`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        // readerState.progress is null (count treated as 0).
        viewModel.onEvent(DuaEvent.DecrementProgress("dua1"))
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.decrementDuaProgress(any(), any()) }
    }

    // ── Reader display toggles ──────────────────────────────────────

    @Test
    fun `ToggleArabic flips the showArabic flag`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        assertThat(viewModel.readerState.value.showArabic).isTrue()
        viewModel.onEvent(DuaEvent.ToggleArabic)
        assertThat(viewModel.readerState.value.showArabic).isFalse()
    }

    @Test
    fun `SetFontSize and SetArabicFontSize update the reader state`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(DuaEvent.SetFontSize(22f))
        viewModel.onEvent(DuaEvent.SetArabicFontSize(36f))

        assertThat(viewModel.readerState.value.fontSize).isEqualTo(22f)
        assertThat(viewModel.readerState.value.arabicFontSize).isEqualTo(36f)
    }
}
