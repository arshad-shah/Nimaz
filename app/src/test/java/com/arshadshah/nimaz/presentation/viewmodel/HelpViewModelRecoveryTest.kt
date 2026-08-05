package com.arshadshah.nimaz.presentation.viewmodel

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.model.HelpSearchResult
import com.arshadshah.nimaz.domain.model.HelpTopic
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.usecase.HelpUseCases
import com.google.common.truth.Truth.assertThat
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
 * `Flow.catch` **completes** the flow it is applied to. Both Help collectors put it
 * *outside* the `flatMapLatest`, so one transient failure ended the whole chain —
 * `language` is a `StateFlow` that never completes on its own, but once an upstream
 * throw was caught the `collect` returned and the coroutine finished.
 *
 * The user-visible result: open Help while the content database is momentarily
 * locked and the topic list is empty **for the ViewModel's entire lifetime**.
 * Changing the app language, retrying, and navigating in and out of Help do not
 * recover it.
 *
 * These tests fail against the previous implementation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HelpViewModelRecoveryTest {

    private val dispatcher = StandardTestDispatcher()
    private val telemetry = RecordingTelemetry()
    private lateinit var useCases: HelpUseCases
    private lateinit var prefs: SettingsRepository
    private val language = MutableStateFlow("en")

    private val topic = HelpTopic(
        id = "wudu",
        iconKey = "water",
        colorKey = "blue",
        title = "Wudu",
        subtitle = "How to perform wudu",
        order = 0,
        itemCount = 3,
    )

    private val hit = HelpSearchResult(
        topicId = "wudu",
        itemId = null,
        isGuide = false,
        title = "Wudu",
        snippet = "How to perform wudu",
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        useCases = mockk(relaxed = true)
        prefs = mockk(relaxed = true)
        every { prefs.appLanguage } returns language
        every { useCases.search(any(), any()) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `topics recover after a transient failure when the language changes`() = runTest {
        var attempt = 0
        every { useCases.getTopics(any()) } answers {
            attempt++
            if (attempt == 1) flow { throw IllegalStateException("database is locked") }
            else flowOf(listOf(topic))
        }

        val vm = HelpViewModel(useCases, prefs, telemetry)
        advanceUntilIdle()

        // First attempt failed and was reported.
        assertThat(vm.homeState.value.topics).isEmpty()
        assertThat(vm.homeState.value.isLoading).isFalse()
        assertThat(telemetry.errors.map { it.domain }).contains("help")

        // The outer collector must still be alive: a language change re-resolves topics.
        language.value = "tr"
        advanceUntilIdle()

        assertThat(vm.homeState.value.topics).containsExactly(topic)
        assertThat(vm.homeState.value.error).isNull()
    }

    @Test
    fun `search results are never shown under a query that has since been cleared`() = runTest {
        every { useCases.getTopics(any()) } returns flowOf(emptyList())
        every { useCases.search("wudu", any()) } returns flowOf(listOf(hit))

        val vm = HelpViewModel(useCases, prefs, telemetry)
        advanceUntilIdle()

        vm.onEvent(HelpEvent.Search("wudu"))
        advanceUntilIdle()
        assertThat(vm.homeState.value.results).isNotEmpty()

        vm.onEvent(HelpEvent.Search(""))
        advanceUntilIdle()

        // isSearching used to be read from query.value at emission time — a different,
        // undebounced flow — so results and the query could disagree.
        assertThat(vm.homeState.value.results).isEmpty()
        assertThat(vm.homeState.value.isSearching).isFalse()
    }

    @Test
    fun `search is logged once per debounced query, not once per keystroke`() = runTest {
        every { useCases.getTopics(any()) } returns flowOf(emptyList())
        every { useCases.search(any(), any()) } returns flowOf(listOf(hit))

        val vm = HelpViewModel(useCases, prefs, telemetry)
        advanceUntilIdle()

        // HelpScreen wires Search to onQueryChange, so typing emits one event per character.
        "wudu".forEachIndexed { index, _ -> vm.onEvent(HelpEvent.Search("wudu".take(index + 1))) }
        advanceUntilIdle()

        assertThat(telemetry.searches).hasSize(1)
        assertThat(telemetry.searches.single().queryLength).isEqualTo(4)
    }
}
