package com.arshadshah.nimaz.presentation.viewmodel.content

import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazErrorKind
import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.model.Dua
import com.arshadshah.nimaz.domain.model.DuaBookmark
import com.arshadshah.nimaz.domain.model.DuaCategory
import com.arshadshah.nimaz.domain.model.DuaProgress
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.usecase.DuaUseCases
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * What [DuaViewModel] does when the content database does not cooperate.
 *
 * Four of its loaders collected Room flows with no `try`/`catch` at all, while two others in
 * the same file had one — so it read as oversight rather than decision. A throw killed the
 * collector silently and pinned `isLoading` to `true`: a spinner that never resolves and no
 * report anywhere.
 *
 * The other half is what the user is shown when something does go wrong. The category screen
 * rendered `e.message` verbatim, so a content-database fault surfaced as
 * `SQLiteException: no such table: duas` in whatever language SQLite writes in.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DuaViewModelErrorPathTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var duaUseCases: DuaUseCases
    private lateinit var settingsRepository: SettingsRepository
    private val telemetry = RecordingTelemetry()

    private val morningDuas = MutableStateFlow(listOf(dua("m1", "morning")))

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        duaUseCases = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)

        every { duaUseCases.getAllCategories() } returns flowOf(emptyList())
        every { duaUseCases.getFavoriteDuas() } returns flowOf(emptyList<DuaBookmark>())
        every { duaUseCases.getProgressForDate(any()) } returns flowOf(emptyList<DuaProgress>())
        every { duaUseCases.getDuasByCategory(any()) } returns morningDuas
        every { settingsRepository.duaCategoriesSortAlphabetical } returns flowOf(false)
        coEvery { duaUseCases.getCategoryById(any()) } answers { category(firstArg()) }
        coEvery { duaUseCases.getDuaById(any()) } answers {
            if (firstArg<String>() == "m1") dua("m1", "morning") else null
        }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = DuaViewModel(duaUseCases, settingsRepository, telemetry)

    @Test
    fun `a failing category list clears the spinner instead of hanging on it`() = runTest {
        every { duaUseCases.getAllCategories() } returns
            flow { throw IllegalStateException("no such table: dua_categories") }

        val vm = viewModel()
        advanceUntilIdle()

        assertThat(vm.collectionState.value.isLoading).isFalse()
        assertThat(telemetry.errors.map { it.type }).contains("load_categories")
    }

    @Test
    fun `a failing favourites load clears the spinner instead of hanging on it`() = runTest {
        every { duaUseCases.getFavoriteDuas() } returns
            flow { throw IllegalStateException("no such table: dua_bookmarks") }

        val vm = viewModel()
        advanceUntilIdle()

        assertThat(vm.favoritesState.value.isLoading).isFalse()
        assertThat(telemetry.errors.map { it.type }).contains("load_favorites")
    }

    @Test
    fun `a failing progress load clears the spinner instead of hanging on it`() = runTest {
        every { duaUseCases.getProgressForDate(any()) } returns
            flow { throw IllegalStateException("no such table: dua_progress") }

        val vm = viewModel()
        advanceUntilIdle()

        assertThat(vm.dailyProgressState.value.isLoading).isFalse()
        assertThat(telemetry.errors.map { it.type }).contains("load_progress")
    }

    @Test
    fun `a failing occasion list clears the spinner instead of hanging on it`() = runTest {
        every { duaUseCases.getDuasByOccasion(any()) } returns
            flow { throw IllegalStateException("no such table: duas") }

        val vm = viewModel()
        vm.onEvent(DuaEvent.LoadDuasByOccasion(com.arshadshah.nimaz.domain.model.DuaOccasion.TRAVELING))
        advanceUntilIdle()

        assertThat(vm.categoryState.value.isLoading).isFalse()
        assertThat(telemetry.errors.map { it.type }).contains("load_occasion")
    }

    @Test
    fun `what the user is shown is translated copy, never the database's own words`() = runTest {
        every { duaUseCases.getDuasByCategory(any()) } returns
            flow { throw IllegalStateException("no such table: duas") }

        val vm = viewModel()
        vm.onEvent(DuaEvent.LoadCategory("morning"))
        advanceUntilIdle()

        // A string resource, resolved by the screen in the user's language — not `e.message`.
        assertThat(vm.categoryState.value.error?.message)
            .isEqualTo(R.string.dua_category_load_failed)
        // The exception's own words are carried, but as detail the component hides behind a
        // toggle — never as the readable message.
        assertThat(vm.categoryState.value.error?.details).isEqualTo("no such table: duas")
        // And it still reaches the crash report, where it belongs.
        assertThat(telemetry.exceptions.map { it.message })
            .contains("no such table: duas")
    }

    @Test
    fun `a dua that does not exist replaces the one on screen rather than sitting under it`() =
        runTest {
            val vm = viewModel()

            vm.onEvent(DuaEvent.LoadDua("m1"))
            advanceUntilIdle()
            assertThat(vm.readerState.value.duas.map { it.id }).containsExactly("m1")

            vm.onEvent(DuaEvent.LoadDua("gone"))
            advanceUntilIdle()

            // Otherwise the reader keeps paging through the previous dua while its state
            // says the requested one was not found.
            assertThat(vm.readerState.value.duas).isEmpty()
            assertThat(vm.readerState.value.error?.message)
                .isEqualTo(R.string.dua_reader_not_found)
            // A dua that is not there is an answer, not a failure.
            assertThat(vm.readerState.value.error?.kind).isEqualTo(NimazErrorKind.NOT_FOUND)
            assertThat(vm.readerState.value.isLoading).isFalse()
        }
}

private fun category(id: String) = DuaCategory(
    id = id,
    nameArabic = "",
    nameEnglish = id,
    description = null,
    iconName = null,
    displayOrder = 0,
    duaCount = 1
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
