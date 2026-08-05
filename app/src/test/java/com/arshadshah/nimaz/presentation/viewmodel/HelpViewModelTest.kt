package com.arshadshah.nimaz.presentation.viewmodel

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry

import app.cash.turbine.test
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.model.HelpTopic
import com.arshadshah.nimaz.domain.usecase.GetHelpTopicsUseCase
import com.arshadshah.nimaz.domain.usecase.HelpUseCases
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
class HelpViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var useCases: HelpUseCases
    private lateinit var prefs: SettingsRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        prefs = mockk(relaxed = true)
        every { prefs.appLanguage } returns flowOf("en")
        useCases = mockk(relaxed = true)
        val getTopics = mockk<GetHelpTopicsUseCase>()
        every { getTopics.invoke("en") } returns flowOf(
            listOf(HelpTopic("t1", "schedule", "indigo", "Prayer Times", "sub", 1, 3))
        )
        every { useCases.getTopics } returns getTopics
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun loadsTopicsOnInit() = runTest {
        val vm = HelpViewModel(useCases, prefs, RecordingTelemetry())
        advanceUntilIdle()
        vm.homeState.test {
            val state = expectMostRecentItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.topics.single().title).isEqualTo("Prayer Times")
        }
    }
}
