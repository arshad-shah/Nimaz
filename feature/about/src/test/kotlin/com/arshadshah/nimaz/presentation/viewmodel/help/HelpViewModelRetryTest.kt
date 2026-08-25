package com.arshadshah.nimaz.presentation.viewmodel.help

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.model.HelpGuideDetail
import com.arshadshah.nimaz.domain.model.HelpTopic
import com.arshadshah.nimaz.domain.model.HelpTopicDetail
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
 * Retry, which is one event serving three independent surfaces.
 *
 * Help home, a topic and a guide each have their own state, their own loader and their own error,
 * and all three are reached from the same `HelpEvent.Retry` — so the event has to re-run **only**
 * what is actually failing. The failure mode is not a crash: re-running a healthy surface throws
 * its content back into a loading state, which on the Help home means the topic grid blinks out
 * from under a reader who was retrying something else entirely, and on a topic means losing the
 * page they were on.
 *
 * The home retry needs its own mechanism, and that is the reason for `retryTick`. Topics are
 * collected from `appLanguage.flatMapLatest { … }`, and `appLanguage` is a `StateFlow` of a
 * preference: when the language has not changed there is nothing to re-emit, so a retry that only
 * re-collects would do nothing at all and the button would look broken.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HelpViewModelRetryTest {

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
        itemCount = 1,
    )

    private val topicDetail = HelpTopicDetail(topic = topic, questions = emptyList(), guides = emptyList())

    private val guide = HelpGuideDetail(
        id = "make-wudu",
        title = "Make wudu",
        estimatedMinutes = 3,
        steps = emptyList(),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        useCases = mockk(relaxed = true)
        prefs = mockk(relaxed = true)
        every { prefs.appLanguage } returns language
        every { useCases.search(any(), any()) } returns flowOf(emptyList())
        every { useCases.getTopics(any()) } returns flowOf(listOf(topic))
        every { useCases.getTopicDetail(any(), any()) } returns flowOf(topicDetail)
        every { useCases.getGuide(any(), any()) } returns flowOf(guide)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `a retry re-runs the topic list even though the language has not changed`() = runTest {
        var attempt = 0
        every { useCases.getTopics(any()) } answers {
            attempt++
            if (attempt == 1) flow { throw IllegalStateException("database is locked") }
            else flowOf(listOf(topic))
        }

        val vm = HelpViewModel(useCases, prefs, telemetry)
        advanceUntilIdle()
        assertThat(vm.homeState.value.error).isNotNull()

        vm.onEvent(HelpEvent.Retry)
        advanceUntilIdle()

        assertThat(vm.homeState.value.topics).containsExactly(topic)
        assertThat(vm.homeState.value.error).isNull()
        assertThat(vm.homeState.value.isLoading).isFalse()
    }

    @Test
    fun `a retry from a failed topic reloads that topic and leaves Help home alone`() = runTest {
        var attempt = 0
        every { useCases.getTopicDetail("wudu", any()) } answers {
            attempt++
            if (attempt == 1) flow { throw IllegalStateException("locked") } else flowOf(topicDetail)
        }

        val vm = HelpViewModel(useCases, prefs, telemetry)
        advanceUntilIdle()
        vm.onEvent(HelpEvent.LoadTopic("wudu"))
        advanceUntilIdle()
        assertThat(vm.topicState.value.error).isNotNull()

        val topicsBefore = vm.homeState.value.topics

        vm.onEvent(HelpEvent.Retry)
        advanceUntilIdle()

        assertThat(vm.topicState.value.detail).isEqualTo(topicDetail)
        assertThat(vm.topicState.value.error).isNull()
        // Help home never went back to loading: it was not the thing that failed.
        assertThat(vm.homeState.value.topics).isEqualTo(topicsBefore)
        assertThat(vm.homeState.value.isLoading).isFalse()
    }

    @Test
    fun `a retry from a failed guide reloads that guide and leaves the topic alone`() = runTest {
        var attempt = 0
        every { useCases.getGuide("make-wudu", any()) } answers {
            attempt++
            if (attempt == 1) flow { throw IllegalStateException("locked") } else flowOf(guide)
        }

        val vm = HelpViewModel(useCases, prefs, telemetry)
        advanceUntilIdle()
        vm.onEvent(HelpEvent.LoadTopic("wudu"))
        vm.onEvent(HelpEvent.LoadGuide("make-wudu"))
        advanceUntilIdle()
        assertThat(vm.guideState.value.error).isNotNull()
        assertThat(vm.topicState.value.error).isNull()

        vm.onEvent(HelpEvent.Retry)
        advanceUntilIdle()

        assertThat(vm.guideState.value.guide).isEqualTo(guide)
        assertThat(vm.guideState.value.error).isNull()
        assertThat(vm.topicState.value.detail).isEqualTo(topicDetail)
    }

    @Test
    fun `a retry with nothing failing re-runs nothing`() = runTest {
        val vm = HelpViewModel(useCases, prefs, telemetry)
        advanceUntilIdle()
        vm.onEvent(HelpEvent.LoadTopic("wudu"))
        advanceUntilIdle()

        vm.onEvent(HelpEvent.Retry)
        advanceUntilIdle()

        // Nothing is thrown back into loading — a retry on a healthy Help screen is a no-op,
        // not a flicker.
        assertThat(vm.homeState.value.isLoading).isFalse()
        assertThat(vm.homeState.value.topics).containsExactly(topic)
        assertThat(vm.topicState.value.isLoading).isFalse()
        assertThat(vm.topicState.value.detail).isEqualTo(topicDetail)
    }
}
