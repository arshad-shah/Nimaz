package com.arshadshah.nimaz.presentation.viewmodel.content

import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.model.HadithBook
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.usecase.HadithUseCases
import com.arshadshah.nimaz.presentation.components.molecules.NimazErrorKind
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
 * Hadith detected four kinds of failure and told the reader about none of them: none of
 * the three screens contained the substring `error`, so a failed load stopped the spinner
 * and left a blank page.
 *
 * Two of the four were worse than invisible. `loadAllBooks` and `loadHadithOfTheDay` were
 * bare `viewModelScope.launch { … collect { … } }` with no `try` at all — and
 * `viewModelScope` is a `SupervisorJob`, so a Room failure there is not contained: it
 * reaches the thread's uncaught handler and takes the app down. The first test here
 * passes only because that path is now caught; before, it propagated out of the test.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HadithFailureTest {

    private val dispatcher = StandardTestDispatcher()
    private val telemetry = RecordingTelemetry()
    private lateinit var useCases: HadithUseCases
    private lateinit var settings: SettingsRepository

    private val bukhari = HadithBook(
        id = "bukhari",
        nameArabic = "صحيح البخاري",
        nameEnglish = "Sahih al-Bukhari",
        authorName = "Imam al-Bukhari",
        authorArabic = "الإمام البخاري",
        totalHadiths = 7563,
        totalChapters = 97,
        description = null,
        displayOrder = 0,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        useCases = mockk(relaxed = true)
        settings = mockk(relaxed = true)
        every { useCases.getAllBooks() } returns flowOf(listOf(bukhari))
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = HadithViewModel(useCases, settings, telemetry)

    @Test
    fun `a failing book list is caught, reported and shown`() = runTest(dispatcher) {
        every { useCases.getAllBooks() } returns flow { throw IllegalStateException("db locked") }

        // Before launchSafely this threw out of the ViewModel's init and failed the test
        // by propagation — which is what it does to the app.
        val viewModel = viewModel()
        advanceUntilIdle()

        val state = viewModel.collectionState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.error?.message).isEqualTo(R.string.hadith_books_load_failed)
        assertThat(state.error?.details).isEqualTo("db locked")
        assertThat(telemetry.errors).isNotEmpty()
    }

    @Test
    fun `a failing chapter list stops loading and says which collection failed`() =
        runTest(dispatcher) {
            coEvery { useCases.getBookById("bukhari") } returns bukhari
            every { useCases.getChaptersByBook("bukhari") } returns
                flow { throw IllegalStateException("no such table") }

            val viewModel = viewModel()
            viewModel.onEvent(HadithEvent.LoadBook("bukhari"))
            advanceUntilIdle()

            val state = viewModel.chaptersState.value
            assertThat(state.isLoading).isFalse()
            assertThat(state.error?.message).isEqualTo(R.string.hadith_chapters_load_failed)
            // The exception text is carried, but as detail — never as the readable copy.
            assertThat(state.error?.details).isEqualTo("no such table")
        }

    @Test
    fun `a hadith that is not there is an answer, not a failure`() = runTest(dispatcher) {
        coEvery { useCases.getHadithById("nope") } returns null

        val viewModel = viewModel()
        telemetry.clear()
        viewModel.onEvent(HadithEvent.LoadHadithById("nope"))
        advanceUntilIdle()

        val state = viewModel.readerState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.error?.kind).isEqualTo(NimazErrorKind.NOT_FOUND)
        assertThat(state.error?.message).isEqualTo(R.string.hadith_not_found)
        // Nothing went wrong, so nothing is reported. It used to set the English literal
        // "Hadith not found" as the state's error string.
        assertThat(telemetry.errors).isEmpty()
    }

    @Test
    fun `retry clears the error and asks again`() = runTest(dispatcher) {
        coEvery { useCases.getBookById("bukhari") } returns bukhari
        every { useCases.getChaptersByBook("bukhari") } returns
            flow { throw IllegalStateException("no such table") }

        val viewModel = viewModel()
        viewModel.onEvent(HadithEvent.LoadBook("bukhari"))
        advanceUntilIdle()
        assertThat(viewModel.chaptersState.value.error).isNotNull()

        every { useCases.getChaptersByBook("bukhari") } returns flowOf(emptyList())
        viewModel.onEvent(HadithEvent.Retry)
        advanceUntilIdle()

        assertThat(viewModel.chaptersState.value.error).isNull()
    }
}
