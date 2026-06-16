package com.arshadshah.nimaz.presentation.viewmodel

import com.arshadshah.nimaz.domain.model.AsmaUlHusna
import com.arshadshah.nimaz.domain.usecase.AsmaUlHusnaUseCases
import com.arshadshah.nimaz.domain.usecase.GetAllAsmaUlHusnaUseCase
import com.arshadshah.nimaz.domain.usecase.GetAsmaUlHusnaByIdUseCase
import com.arshadshah.nimaz.domain.usecase.GetFavoriteAsmaUlHusnaUseCase
import com.arshadshah.nimaz.domain.usecase.SearchAsmaUlHusnaUseCase
import com.arshadshah.nimaz.domain.usecase.ToggleAsmaUlHusnaFavoriteUseCase
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
class AsmaUlHusnaViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getAllNames: GetAllAsmaUlHusnaUseCase
    private lateinit var getNameById: GetAsmaUlHusnaByIdUseCase
    private lateinit var searchNames: SearchAsmaUlHusnaUseCase
    private lateinit var toggleFavorite: ToggleAsmaUlHusnaFavoriteUseCase
    private lateinit var getFavorites: GetFavoriteAsmaUlHusnaUseCase
    private lateinit var useCases: AsmaUlHusnaUseCases
    private lateinit var viewModel: AsmaUlHusnaViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getAllNames = mockk()
        getNameById = mockk()
        searchNames = mockk(relaxed = true)
        toggleFavorite = mockk(relaxed = true)
        getFavorites = mockk()
        useCases = AsmaUlHusnaUseCases(getAllNames, getNameById, searchNames, toggleFavorite, getFavorites)

        every { getAllNames() } returns flowOf(emptyList())
        every { getFavorites() } returns flowOf(emptyList())
        coEvery { getNameById(any()) } returns null
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = AsmaUlHusnaViewModel(useCases)

    private fun name(
        id: Int,
        english: String = "The Most $id",
        meaning: String = "meaning $id",
        isFavorite: Boolean = false
    ) = AsmaUlHusna(
        id = id, number = id, nameArabic = "اسم$id", nameTransliteration = "Translit$id",
        nameEnglish = english, meaning = meaning, explanation = "", benefits = "",
        quranReferences = emptyList(), usageInDua = "", displayOrder = id, isFavorite = isFavorite
    )

    // ── Init / list ─────────────────────────────────────────────────

    @Test
    fun `init populates names and completes loading`() = runTest {
        every { getAllNames() } returns flowOf(listOf(name(1), name(2)))

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.listState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.names).hasSize(2)
        assertThat(state.filteredNames).hasSize(2)
    }

    // ── Search ──────────────────────────────────────────────────────

    @Test
    fun `Search filters names by english title`() = runTest {
        every { getAllNames() } returns flowOf(
            listOf(name(1, english = "The Compassionate"), name(2, english = "The King"))
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(AsmaUlHusnaEvent.Search("king"))

        val state = viewModel.listState.value
        assertThat(state.searchQuery).isEqualTo("king")
        assertThat(state.filteredNames.map { it.id }).containsExactly(2)
    }

    @Test
    fun `ClearSearch restores the full list`() = runTest {
        every { getAllNames() } returns flowOf(listOf(name(1, english = "Compassionate"), name(2, english = "King")))

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(AsmaUlHusnaEvent.Search("king"))
        assertThat(viewModel.listState.value.filteredNames).hasSize(1)

        viewModel.onEvent(AsmaUlHusnaEvent.ClearSearch)
        assertThat(viewModel.listState.value.searchQuery).isEmpty()
        assertThat(viewModel.listState.value.filteredNames).hasSize(2)
    }

    @Test
    fun `ToggleFavoritesFilter switches the source to favorites only`() = runTest {
        every { getAllNames() } returns flowOf(listOf(name(1), name(2)))
        every { getFavorites() } returns flowOf(listOf(name(2, isFavorite = true)))

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(AsmaUlHusnaEvent.ToggleFavoritesFilter)

        val state = viewModel.listState.value
        assertThat(state.showFavoritesOnly).isTrue()
        assertThat(state.filteredNames.map { it.id }).containsExactly(2)
    }

    // ── Detail ──────────────────────────────────────────────────────

    @Test
    fun `LoadDetail loads the name by id`() = runTest {
        coEvery { getNameById(7) } returns name(7, isFavorite = true)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(AsmaUlHusnaEvent.LoadDetail(7))
        advanceUntilIdle()

        val state = viewModel.detailState.value
        assertThat(state.name?.id).isEqualTo(7)
        assertThat(state.isFavorite).isTrue()
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `ToggleFavorite delegates and refreshes the open detail`() = runTest {
        // First read (LoadDetail) is not-favorite; the refresh after toggle is favorite.
        coEvery { getNameById(7) } returnsMany listOf(name(7, isFavorite = false), name(7, isFavorite = true))

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(AsmaUlHusnaEvent.LoadDetail(7))
        advanceUntilIdle()
        assertThat(viewModel.detailState.value.isFavorite).isFalse()

        viewModel.onEvent(AsmaUlHusnaEvent.ToggleFavorite(7))
        advanceUntilIdle()

        coVerify { toggleFavorite(7) }
        assertThat(viewModel.detailState.value.isFavorite).isTrue()
    }
}
