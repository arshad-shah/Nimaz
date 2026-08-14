package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.HelpGuideDetail
import com.arshadshah.nimaz.domain.model.HelpSearchResult
import com.arshadshah.nimaz.domain.model.HelpTopic
import com.arshadshah.nimaz.domain.model.HelpTopicDetail
import com.arshadshah.nimaz.domain.repository.HelpRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class HelpUseCasesTest {

    private lateinit var repo: HelpRepository
    private lateinit var useCases: HelpUseCases

    private val topic = HelpTopic(
        id = "prayer", iconKey = "prayer_icon", colorKey = "blue",
        title = "Prayer Help", subtitle = "Learn how to pray",
        order = 1, itemCount = 5
    )

    private val searchResult = HelpSearchResult(
        topicId = "prayer", itemId = "q1", isGuide = false,
        title = "How to pray Fajr", snippet = "Fajr has 2 rakats"
    )

    @Before
    fun setUp() {
        repo = mockk(relaxed = true)
        useCases = HelpUseCases(
            getTopics = GetHelpTopicsUseCase(repo),
            getTopicDetail = GetHelpTopicDetailUseCase(repo),
            getGuide = GetHelpGuideUseCase(repo),
            search = SearchHelpUseCase(repo)
        )
    }

    @Test
    fun `getTopics returns flow of topics`() = runTest {
        every { repo.getTopics("en") } returns flowOf(listOf(topic))

        val result = useCases.getTopics("en").first()

        assertThat(result).hasSize(1)
        assertThat(result[0].title).isEqualTo("Prayer Help")
    }

    @Test
    fun `getTopics returns empty flow when no topics`() = runTest {
        every { repo.getTopics("en") } returns flowOf(emptyList())

        val result = useCases.getTopics("en").first()

        assertThat(result).isEmpty()
    }

    @Test
    fun `getTopicDetail returns topic detail when found`() = runTest {
        val detail = HelpTopicDetail(
            topic = topic, questions = emptyList(), guides = emptyList()
        )
        every { repo.getTopicDetail("prayer", "en") } returns flowOf(detail)

        val result = useCases.getTopicDetail("prayer", "en").first()

        assertThat(result).isNotNull()
        assertThat(result!!.topic.id).isEqualTo("prayer")
    }

    @Test
    fun `getTopicDetail returns null when topic not found`() = runTest {
        every { repo.getTopicDetail(any(), any()) } returns flowOf(null)

        val result = useCases.getTopicDetail("unknown", "en").first()

        assertThat(result).isNull()
    }

    @Test
    fun `getGuide returns guide detail when found`() = runTest {
        val guide = HelpGuideDetail(
            id = "guide1", title = "Prayer Guide",
            estimatedMinutes = 5, steps = emptyList()
        )
        every { repo.getGuide("guide1", "en") } returns flowOf(guide)

        val result = useCases.getGuide("guide1", "en").first()

        assertThat(result).isNotNull()
        assertThat(result!!.title).isEqualTo("Prayer Guide")
    }

    @Test
    fun `search returns matching results`() = runTest {
        every { repo.search("fajr", "en") } returns flowOf(listOf(searchResult))

        val result = useCases.search("fajr", "en").first()

        assertThat(result).hasSize(1)
        assertThat(result[0].title).isEqualTo("How to pray Fajr")
    }

    @Test
    fun `search returns empty for blank query`() = runTest {
        every { repo.search("", "en") } returns flowOf(emptyList())

        val result = useCases.search("", "en").first()

        assertThat(result).isEmpty()
    }
}
