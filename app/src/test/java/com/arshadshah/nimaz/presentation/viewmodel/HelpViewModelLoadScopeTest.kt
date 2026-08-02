package com.arshadshah.nimaz.presentation.viewmodel

import com.arshadshah.nimaz.domain.model.HelpGuideDetail
import com.arshadshah.nimaz.domain.model.HelpItem
import com.arshadshah.nimaz.domain.model.HelpTopic
import com.arshadshah.nimaz.domain.model.HelpTopicDetail
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.usecase.GetHelpGuideUseCase
import com.arshadshah.nimaz.domain.usecase.GetHelpTopicDetailUseCase
import com.arshadshah.nimaz.domain.usecase.GetHelpTopicsUseCase
import com.arshadshah.nimaz.domain.usecase.HelpUseCases
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
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

/**
 * Topic and guide loads in [HelpViewModel] must be scoped to the screen the user is on.
 *
 * `loadTopic` / `loadGuide` collect `language.flatMapLatest { … }`. `language` is a
 * `StateFlow`, which never completes, so the collector outlives the load no matter what the
 * inner repository flow does — and neither was cancelled. Opening five help topics left five
 * live collectors, all writing `_topicState`, so a re-emission for any earlier topic replaced
 * the one being read.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HelpViewModelLoadScopeTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var useCases: HelpUseCases
    private lateinit var prefs: SettingsRepository

    private val topicDetails = mapOf(
        "t1" to MutableStateFlow(detail("t1", "Prayer Times")),
        "t2" to MutableStateFlow(detail("t2", "Qibla"))
    )
    private val guides = mapOf(
        "g1" to MutableStateFlow(guide("g1", "Set up adhan")),
        "g2" to MutableStateFlow(guide("g2", "Track fasts"))
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        prefs = mockk(relaxed = true)
        every { prefs.appLanguage } returns MutableStateFlow("en")

        useCases = mockk(relaxed = true)
        val getTopics = mockk<GetHelpTopicsUseCase>()
        every { getTopics.invoke(any()) } returns flowOf(emptyList())
        every { useCases.getTopics } returns getTopics

        val getTopicDetail = mockk<GetHelpTopicDetailUseCase>()
        every { getTopicDetail.invoke(any(), any()) } answers {
            topicDetails.getValue(firstArg<String>())
        }
        every { useCases.getTopicDetail } returns getTopicDetail

        val getGuide = mockk<GetHelpGuideUseCase>()
        every { getGuide.invoke(any(), any()) } answers { guides.getValue(firstArg<String>()) }
        every { useCases.getGuide } returns getGuide
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `a previously opened topic cannot overwrite the topic on screen`() = runTest {
        val vm = HelpViewModel(useCases, prefs)

        vm.onEvent(HelpEvent.LoadTopic("t1"))
        advanceUntilIdle()
        vm.onEvent(HelpEvent.LoadTopic("t2"))
        advanceUntilIdle()

        assertThat(vm.topicState.value.detail?.topic?.id).isEqualTo("t2")

        topicDetails.getValue("t1").value = detail("t1", "Prayer Times (revised)")
        advanceUntilIdle()

        assertThat(vm.topicState.value.detail?.topic?.id).isEqualTo("t2")
    }

    @Test
    fun `a previously opened guide cannot overwrite the guide on screen`() = runTest {
        val vm = HelpViewModel(useCases, prefs)

        vm.onEvent(HelpEvent.LoadGuide("g1"))
        advanceUntilIdle()
        vm.onEvent(HelpEvent.LoadGuide("g2"))
        advanceUntilIdle()

        assertThat(vm.guideState.value.guide?.id).isEqualTo("g2")

        guides.getValue("g1").value = guide("g1", "Set up adhan (revised)")
        advanceUntilIdle()

        assertThat(vm.guideState.value.guide?.id).isEqualTo("g2")
    }

    @Test
    fun `reopening a topic re-subscribes to it`() = runTest {
        val vm = HelpViewModel(useCases, prefs)

        vm.onEvent(HelpEvent.LoadTopic("t1"))
        advanceUntilIdle()
        vm.onEvent(HelpEvent.LoadTopic("t2"))
        advanceUntilIdle()
        vm.onEvent(HelpEvent.LoadTopic("t1"))
        advanceUntilIdle()

        assertThat(vm.topicState.value.detail?.topic?.id).isEqualTo("t1")

        topicDetails.getValue("t1").value = detail("t1", "Prayer Times (revised)")
        advanceUntilIdle()

        assertThat(vm.topicState.value.detail?.topic?.title).isEqualTo("Prayer Times (revised)")
    }

    private fun detail(id: String, title: String) = HelpTopicDetail(
        topic = HelpTopic(id, "schedule", "indigo", title, "sub", 1, 0),
        questions = emptyList<HelpItem.HelpQuestion>(),
        guides = emptyList<HelpItem.HelpGuide>()
    )

    private fun guide(id: String, title: String) = HelpGuideDetail(
        id = id,
        title = title,
        estimatedMinutes = null,
        steps = emptyList()
    )
}
