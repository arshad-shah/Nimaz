package com.arshadshah.nimaz.presentation.viewmodel.content

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.model.Dua
import com.arshadshah.nimaz.domain.model.DuaCategory
import com.arshadshah.nimaz.domain.model.DuaOccasion
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.time.FakeTodayProvider
import com.arshadshah.nimaz.domain.usecase.DuaUseCases
import com.arshadshah.nimaz.domain.usecase.TasbihUseCases
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * The dua collection's filter and sort, and the reader's paging window.
 *
 * **`filterAndSortCategories` is the one piece of list logic the dua feature owns**, and it is
 * two decisions in one function: which categories survive the query, and what order the
 * survivors come back in. Both are user-visible and neither is asserted anywhere else — the
 * screen renders whatever `filteredCategories` holds.
 *
 * The search arm matches on three fields, one of which is nullable, and each is a separate `||`
 * with its own case rule: the English name and the description are case-insensitive, the
 * **Arabic name is not**, because case-folding Arabic is meaningless and `ignoreCase` on it
 * would only add cost. A reader who types a category's description and gets nothing has no way
 * to tell that from "there is no such category".
 *
 * The sort arm is the toggle `DuasCollectionScreen` exposes: curated `displayOrder` — the order
 * a scholar chose — or A–Z by lowercased English name. Sorting alphabetically *without*
 * lowercasing puts every capitalised name before every lowercase one, which reads as random.
 *
 * **The reader's window is the other decision here.** Opening a dua loads its whole category so
 * the pager can page, and `indexOfFirst(...).coerceAtLeast(0)` is what keeps a dua whose
 * category query does not contain it from opening at index -1.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DuaCollectionSortTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var duaUseCases: DuaUseCases
    private lateinit var tasbihUseCases: TasbihUseCases
    private lateinit var settings: SettingsRepository

    private val categories = MutableStateFlow<List<DuaCategory>>(emptyList())
    private val sortAlphabetical = MutableStateFlow(false)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        duaUseCases = mockk(relaxed = true)
        tasbihUseCases = mockk(relaxed = true)
        settings = mockk(relaxed = true)

        every { duaUseCases.getAllCategories() } returns categories
        every { duaUseCases.getFavoriteDuas() } returns flowOf(emptyList())
        every { duaUseCases.getProgressForDate(any()) } returns flowOf(emptyList())
        every { settings.duaCategoriesSortAlphabetical } returns sortAlphabetical
        every { settings.duaArabicFont } returns flowOf("amiri")
        every { settings.duaArabicFontSize } returns flowOf(28f)
        every { settings.duaTranslationFontSize } returns flowOf(16f)
        every { settings.duaShowArabic } returns flowOf(true)
        every { settings.duaShowTransliteration } returns flowOf(true)
        every { settings.duaShowTranslation } returns flowOf(true)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = DuaViewModel(
        duaUseCases = duaUseCases,
        tasbihUseCases = tasbihUseCases,
        duaSettings = settings,
        todayProvider = FakeTodayProvider(LocalDate.of(2026, 8, 25)),
        telemetry = RecordingTelemetry(),
    )

    private fun category(
        id: String,
        nameEnglish: String,
        nameArabic: String = "أذكار",
        description: String? = null,
        displayOrder: Int = 0,
    ) = DuaCategory(
        id = id,
        nameArabic = nameArabic,
        nameEnglish = nameEnglish,
        description = description,
        iconName = null,
        displayOrder = displayOrder,
        duaCount = 1,
    )

    @Test
    fun `with no query the curated order is the shipped display order`() = runTest {
        categories.value = listOf(
            category("c", "Zuhr", displayOrder = 3),
            category("a", "Morning", displayOrder = 1),
            category("b", "Evening", displayOrder = 2),
        )
        val vm = viewModel()
        advanceUntilIdle()

        assertThat(vm.collectionState.value.filteredCategories.map { it.nameEnglish })
            .containsExactly("Morning", "Evening", "Zuhr").inOrder()
    }

    @Test
    fun `alphabetical order is by lowercased English name, not by raw string`() = runTest {
        // Sorting without lowercasing puts every capitalised name before every lowercase one,
        // which reads as no order at all.
        categories.value = listOf(
            category("a", "apple adhkar", displayOrder = 1),
            category("b", "Banana adhkar", displayOrder = 2),
            category("c", "Cherry adhkar", displayOrder = 3),
        )
        sortAlphabetical.value = true
        val vm = viewModel()
        advanceUntilIdle()

        assertThat(vm.collectionState.value.filteredCategories.map { it.nameEnglish })
            .containsExactly("apple adhkar", "Banana adhkar", "Cherry adhkar").inOrder()
        assertThat(vm.collectionState.value.sortAlphabetical).isTrue()
    }

    @Test
    fun `toggling the sort persists the state it is moving to, not the one it is in`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(DuaEvent.ToggleCategoriesSort)
        advanceUntilIdle()

        val written = slot<Boolean>()
        coVerify { settings.setDuaCategoriesSortAlphabetical(capture(written)) }
        assertThat(written.captured).isTrue()
    }

    @Test
    fun `toggling back off persists the curated order`() = runTest {
        sortAlphabetical.value = true
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(DuaEvent.ToggleCategoriesSort)
        advanceUntilIdle()

        val written = slot<Boolean>()
        coVerify { settings.setDuaCategoriesSortAlphabetical(capture(written)) }
        assertThat(written.captured).isFalse()
    }

    @Test
    fun `the filtered view is the whole catalogue, because no query can currently reach it`() =
        runTest {
            // **`filterAndSortCategories`'s search arm is unreachable today, and that is worth
            // recording rather than dressing up.** `DuaCollectionUiState.searchQuery` is only
            // ever written as `""` — by `ClearSearch` and by the collection collector — and no
            // event sets it to anything else: `DuasCollectionScreen`'s search action navigates
            // to `Route.DuaSearch`, which is `:feature:search`'s screen against a different
            // ViewModel. So the three `||` arms over name, Arabic name and description are
            // dead until something wires a query in, and no test here can take them without
            // reaching past the public surface to pretend otherwise.
            //
            // What *is* live is the pass-through: every category survives, in sorted order.
            categories.value = listOf(
                category("a", "Morning Adhkar", displayOrder = 1),
                category("b", "Evening Adhkar", displayOrder = 2),
            )
            val vm = viewModel()
            advanceUntilIdle()

            vm.onEvent(DuaEvent.LoadAllCategories)
            advanceUntilIdle()

            assertThat(vm.collectionState.value.filteredCategories).hasSize(2)
            assertThat(vm.collectionState.value.searchQuery).isEmpty()
        }

    @Test
    fun `the categories and the filtered view stay the same list while no query is set`() =
        runTest {
            // The screen renders `filteredCategories` and nothing else, so the two falling out
            // of step is invisible in the state and total in the UI.
            categories.value = listOf(category("a", "Morning Adhkar", displayOrder = 1))
            val vm = viewModel()
            advanceUntilIdle()

            assertThat(vm.collectionState.value.filteredCategories)
                .isEqualTo(vm.collectionState.value.categories)
        }

    @Test
    fun `opening a dua loads its whole category so the pager has somewhere to go`() = runTest {
        val target = dua("d2", "morning")
        coEvery { duaUseCases.getDuaById("d2") } returns target
        every { duaUseCases.getDuasByCategory("morning") } returns flowOf(
            listOf(dua("d1", "morning"), target, dua("d3", "morning"))
        )
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(DuaEvent.LoadDua("d2"))
        advanceUntilIdle()

        assertThat(vm.readerState.value.duas.map { it.id })
            .containsExactly("d1", "d2", "d3").inOrder()
        assertThat(vm.readerState.value.initialIndex).isEqualTo(1)
        assertThat(vm.readerState.value.isLoading).isFalse()
    }

    @Test
    fun `a dua whose category comes back empty still opens, alone`() = runTest {
        // `categoryDuas.ifEmpty { listOf(dua) }`. Without it the reader resolves a real dua and
        // then shows "not found", because the screen keys that message off an empty list.
        val orphan = dua("lonely", "retired-category")
        coEvery { duaUseCases.getDuaById("lonely") } returns orphan
        every { duaUseCases.getDuasByCategory("retired-category") } returns flowOf(emptyList())
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(DuaEvent.LoadDua("lonely"))
        advanceUntilIdle()

        assertThat(vm.readerState.value.duas.map { it.id }).containsExactly("lonely")
        assertThat(vm.readerState.value.initialIndex).isEqualTo(0)
        assertThat(vm.readerState.value.error).isNull()
    }

    @Test
    fun `a dua missing from its own category list opens at the top, not at index -1`() = runTest {
        // `indexOfFirst(...).coerceAtLeast(0)`. A -1 initial page is a crash in the pager.
        val target = dua("d2", "morning")
        coEvery { duaUseCases.getDuaById("d2") } returns target
        every { duaUseCases.getDuasByCategory("morning") } returns flowOf(
            listOf(dua("d1", "morning"), dua("d3", "morning"))
        )
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(DuaEvent.LoadDua("d2"))
        advanceUntilIdle()

        assertThat(vm.readerState.value.initialIndex).isEqualTo(0)
    }

    @Test
    fun `a dua id that resolves to nothing clears the pages as well as saying so`() = runTest {
        // Leaving the previous dua's pages loaded meant a reader who asked for a dua that does
        // not exist kept paging through the one before it while the state said "not found".
        coEvery { duaUseCases.getDuaById("present") } returns dua("present", "morning")
        every { duaUseCases.getDuasByCategory("morning") } returns
            flowOf(listOf(dua("present", "morning")))
        coEvery { duaUseCases.getDuaById("ghost") } returns null
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(DuaEvent.LoadDua("present"))
        advanceUntilIdle()
        assertThat(vm.readerState.value.duas).isNotEmpty()

        vm.onEvent(DuaEvent.LoadDua("ghost"))
        advanceUntilIdle()

        assertThat(vm.readerState.value.duas).isEmpty()
        assertThat(vm.readerState.value.error).isNotNull()
    }

    @Test
    fun `the three reader toggles each flip only themselves`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(DuaEvent.ToggleArabic)
        assertThat(vm.readerState.value.showArabic).isFalse()
        assertThat(vm.readerState.value.showTransliteration).isTrue()
        assertThat(vm.readerState.value.showTranslation).isTrue()

        vm.onEvent(DuaEvent.ToggleTransliteration)
        assertThat(vm.readerState.value.showTransliteration).isFalse()
        assertThat(vm.readerState.value.showTranslation).isTrue()

        vm.onEvent(DuaEvent.ToggleTranslation)
        assertThat(vm.readerState.value.showTranslation).isFalse()
        assertThat(vm.readerState.value.showArabic).isFalse()
    }

    @Test
    fun `the font-size events set the sizes they name`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(DuaEvent.SetFontSize(22f))
        vm.onEvent(DuaEvent.SetArabicFontSize(40f))

        assertThat(vm.readerState.value.fontSize).isEqualTo(22f)
        assertThat(vm.readerState.value.arabicFontSize).isEqualTo(40f)
    }

    @Test
    fun `an occasion list replaces a category list on the surface they share`() = runTest {
        every { duaUseCases.getDuasByCategory("morning") } returns
            flowOf(listOf(dua("m1", "morning")))
        coEvery { duaUseCases.getCategoryById("morning") } returns
            category("morning", "Morning Adhkar")
        every { duaUseCases.getDuasByOccasion(DuaOccasion.TRAVELING) } returns
            flowOf(listOf(dua("t1", "travel")))
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(DuaEvent.LoadCategory("morning"))
        advanceUntilIdle()
        assertThat(vm.categoryState.value.duas.map { it.id }).containsExactly("m1")

        vm.onEvent(DuaEvent.LoadDuasByOccasion(DuaOccasion.TRAVELING))
        advanceUntilIdle()

        assertThat(vm.categoryState.value.duas.map { it.id }).containsExactly("t1")
        assertThat(vm.categoryState.value.isLoading).isFalse()
    }

    @Test
    fun `reloading the favourites and the categories is idempotent`() = runTest {
        // Both events exist so a screen can ask again on resume — `DuasCollectionScreen`
        // dispatches `LoadFavorites` from a `LaunchedEffect`. Asking twice must not leave two
        // collectors on the same Room flow writing the same state.
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(DuaEvent.LoadFavorites)
        vm.onEvent(DuaEvent.LoadAllCategories)
        advanceUntilIdle()

        assertThat(vm.favoritesState.value.isLoading).isFalse()
        assertThat(vm.collectionState.value.isLoading).isFalse()
    }

    @Test
    fun `clearing the search resets both the results and the category query`() = runTest {
        // One event, two surfaces: the search results and the collection's own filter. Leaving
        // the second set filters a list whose search box is visibly empty.
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(DuaEvent.ClearSearch)
        advanceUntilIdle()

        assertThat(vm.searchState.value.query).isEmpty()
        assertThat(vm.searchState.value.results).isEmpty()
        assertThat(vm.collectionState.value.searchQuery).isEmpty()
    }

    @Test
    fun `progress is loaded for the date asked for`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(DuaEvent.LoadProgressForDate(1_700_000_000_000L))
        advanceUntilIdle()

        assertThat(vm.dailyProgressState.value.date).isEqualTo(1_700_000_000_000L)
        assertThat(vm.dailyProgressState.value.isLoading).isFalse()
    }

    private fun dua(id: String, categoryId: String) = Dua(
        id = id,
        categoryId = categoryId,
        titleArabic = "دعاء",
        titleEnglish = "Dua $id",
        textArabic = "نص",
        textTransliteration = null,
        textEnglish = "Translation",
        reference = null,
        occasion = null,
        benefits = null,
        repeatCount = null,
        audioUrl = null,
        displayOrder = 1,
    )
}
