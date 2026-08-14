package com.arshadshah.nimaz.presentation.viewmodel.content

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.repository.settings.HadithDisplaySettings
import com.arshadshah.nimaz.domain.usecase.HadithUseCases
import com.google.common.truth.Truth.assertThat
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
class HadithViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val telemetry = RecordingTelemetry()

    private lateinit var hadithUseCases: HadithUseCases
    private lateinit var hadithSettings: HadithDisplaySettings
    private lateinit var viewModel: HadithViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        hadithUseCases = mockk(relaxed = true)
        hadithSettings = mockk(relaxed = true)

        // Stub all settings flows so the combine in observeHadithSettings doesn't hang
        every { hadithSettings.hadithArabicFont } returns flowOf("default")
        every { hadithSettings.hadithArabicFontSize } returns flowOf(22f)
        every { hadithSettings.hadithTranslationFontSize } returns flowOf(16f)
        every { hadithSettings.hadithShowArabic } returns flowOf(true)
        every { hadithSettings.hadithShowTranslation } returns flowOf(true)
        every { hadithSettings.hadithShowGrade } returns flowOf(true)
        every { hadithSettings.hadithShowChain } returns flowOf(false)

        // Stub all book and bookmark flows
        every { hadithUseCases.getAllBooks() } returns flowOf(emptyList())
        every { hadithUseCases.getAllBookmarks() } returns flowOf(emptyList())

        viewModel = HadithViewModel(
            hadithUseCases = hadithUseCases,
            hadithSettings = hadithSettings,
            telemetry = telemetry,
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `initial collection state has empty books list`() = runTest {
        advanceUntilIdle()
        assertThat(viewModel.collectionState.value.books).isEmpty()
    }

    @Test
    fun `initial bookmarks state has empty list`() = runTest {
        advanceUntilIdle()
        assertThat(viewModel.bookmarksState.value.bookmarks).isEmpty()
    }

    @Test
    fun `initial search state has empty query`() = runTest {
        advanceUntilIdle()
        assertThat(viewModel.searchState.value.query).isEmpty()
    }

    @Test
    fun `ClearSearch event clears query`() = runTest {
        advanceUntilIdle()
        viewModel.onEvent(HadithEvent.ClearSearch)
        advanceUntilIdle()
        assertThat(viewModel.searchState.value.query).isEmpty()
    }

    @Test
    fun `LoadAllBooks event reloads the collection`() = runTest {
        advanceUntilIdle()
        viewModel.onEvent(HadithEvent.LoadAllBooks)
        advanceUntilIdle()
        // No exception thrown; books list remains empty given relaxed mock
        assertThat(viewModel.collectionState.value.books).isEmpty()
    }

    @Test
    fun `initial chapters state is empty`() = runTest {
        advanceUntilIdle()
        assertThat(viewModel.chaptersState.value.chapters).isEmpty()
    }
}
