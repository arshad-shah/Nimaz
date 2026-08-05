package com.arshadshah.nimaz.presentation.viewmodel.content

import java.time.LocalDate
import com.arshadshah.nimaz.core.time.FakeTodayProvider
import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.model.Dua
import com.arshadshah.nimaz.domain.model.DuaCategory
import com.arshadshah.nimaz.domain.model.DuaOccasion
import com.arshadshah.nimaz.domain.model.DuaSearchResult
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.usecase.DuaUseCases
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Every load in [DuaViewModel] must be scoped to the thing currently on screen.
 *
 * `loadCategory`, `loadDua`, `loadDuasByOccasion`, `search` and `loadProgressForDate` each
 * launched a bare `viewModelScope.launch { roomFlow.collect { … } }` with no handle. Room
 * flows never complete, so every category opened, every dua read and every keystroke typed
 * left a live collector behind — all of them writing the same state.
 *
 * Two consequences, both user-visible:
 * - **Stale wins.** Room re-emits to all live collectors when the table changes, so an
 *   earlier collector can land after a later one and replace what is on screen.
 * - **Cross-contamination.** `loadCategory` and `loadDuasByOccasion` write the *same*
 *   `_categoryState`, so an occasion list and a category list fight over one surface.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DuaViewModelLoadScopeTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var duaUseCases: DuaUseCases
    private lateinit var settingsRepository: SettingsRepository

    private val duasByCategory = mapOf(
        "morning" to MutableStateFlow(listOf(dua("m1", "morning"))),
        "evening" to MutableStateFlow(listOf(dua("e1", "evening")))
    )
    private val duasByOccasion = MutableStateFlow(listOf(dua("occ1", "travel")))
    private val searchResults = mutableMapOf(
        "du" to MutableStateFlow(listOf(result("slow"))),
        "dua" to MutableStateFlow(listOf(result("fast")))
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        duaUseCases = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)

        coEvery { duaUseCases.getCategoryById(any()) } answers {
            DuaCategory(
                id = firstArg(),
                nameArabic = "",
                nameEnglish = firstArg(),
                description = null,
                iconName = null,
                displayOrder = 0,
                duaCount = 1
            )
        }
        every { duaUseCases.getDuasByCategory(any()) } answers {
            duasByCategory.getValue(firstArg<String>())
        }
        every { duaUseCases.getDuasByOccasion(any()) } returns duasByOccasion
        every { duaUseCases.searchDuas(any()) } answers { searchResults.getValue(firstArg()) }
        coEvery { duaUseCases.getDuaById(any()) } answers {
            val id = firstArg<String>()
            dua(id, if (id.startsWith("m")) "morning" else "evening")
        }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = DuaViewModel(duaUseCases, settingsRepository, FakeTodayProvider(LocalDate.now()), RecordingTelemetry())

    @Test
    fun `a previously opened category cannot overwrite the category on screen`() = runTest {
        val vm = viewModel()

        vm.onEvent(DuaEvent.LoadCategory("morning"))
        advanceUntilIdle()
        vm.onEvent(DuaEvent.LoadCategory("evening"))
        advanceUntilIdle()

        assertThat(vm.categoryState.value.duas.map { it.id }).containsExactly("e1")

        // Room wakes every live collector when the duas table changes.
        duasByCategory.getValue("morning").value = listOf(dua("m1", "morning"), dua("m2", "morning"))
        advanceUntilIdle()

        assertThat(vm.categoryState.value.duas.map { it.id }).containsExactly("e1")
    }

    @Test
    fun `an occasion list and a category list do not fight over the same surface`() = runTest {
        // Both write _categoryState, so whichever collector is left alive wins.
        val vm = viewModel()

        vm.onEvent(DuaEvent.LoadDuasByOccasion(DuaOccasion.TRAVELING))
        advanceUntilIdle()
        vm.onEvent(DuaEvent.LoadCategory("morning"))
        advanceUntilIdle()

        assertThat(vm.categoryState.value.duas.map { it.id }).containsExactly("m1")

        duasByOccasion.value = listOf(dua("occ1", "travel"), dua("occ2", "travel"))
        advanceUntilIdle()

        assertThat(vm.categoryState.value.duas.map { it.id }).containsExactly("m1")
    }

    @Test
    fun `a previously opened dua cannot overwrite the reader`() = runTest {
        val vm = viewModel()

        vm.onEvent(DuaEvent.LoadDua("m1"))
        advanceUntilIdle()
        vm.onEvent(DuaEvent.LoadDua("e1"))
        advanceUntilIdle()

        assertThat(vm.readerState.value.duas.map { it.id }).containsExactly("e1")

        duasByCategory.getValue("morning").value = listOf(dua("m1", "morning"), dua("m2", "morning"))
        advanceUntilIdle()

        assertThat(vm.readerState.value.duas.map { it.id }).containsExactly("e1")
    }

    @Test
    fun `reopening a category re-subscribes to it`() = runTest {
        // Cancellation is scoped to "not the current request", not "load once" — going
        // back to a category must show it again, and stay live for its updates.
        val vm = viewModel()

        vm.onEvent(DuaEvent.LoadCategory("morning"))
        advanceUntilIdle()
        vm.onEvent(DuaEvent.LoadCategory("evening"))
        advanceUntilIdle()
        vm.onEvent(DuaEvent.LoadCategory("morning"))
        advanceUntilIdle()

        assertThat(vm.categoryState.value.duas.map { it.id }).containsExactly("m1")

        duasByCategory.getValue("morning").value = listOf(dua("m1", "morning"), dua("m2", "morning"))
        advanceUntilIdle()

        assertThat(vm.categoryState.value.duas.map { it.id }).containsExactly("m1", "m2")
    }
}

private fun result(id: String) = DuaSearchResult(
    dua = dua(id, "morning"),
    categoryName = "Morning",
    matchedText = id
)

private fun dua(id: String, categoryId: String) = Dua(
    id = id,
    categoryId = categoryId,
    titleArabic = "",
    titleEnglish = id,
    textArabic = "",
    textTransliteration = null,
    textEnglish = "",
    reference = null,
    occasion = null,
    benefits = null,
    repeatCount = null,
    audioUrl = null,
    displayOrder = 0
)
