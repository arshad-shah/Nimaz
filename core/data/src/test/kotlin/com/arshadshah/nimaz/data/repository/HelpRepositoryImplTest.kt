package com.arshadshah.nimaz.data.repository

import app.cash.turbine.test
import com.arshadshah.nimaz.data.local.database.dao.HelpDao
import com.arshadshah.nimaz.data.local.database.entity.HelpItemEntity
import com.arshadshah.nimaz.data.local.database.entity.HelpStepEntity
import com.arshadshah.nimaz.data.local.database.entity.HelpStringEntity
import com.arshadshah.nimaz.data.local.database.entity.HelpTopicEntity
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class HelpRepositoryImplTest {

    private lateinit var dao: HelpDao
    private lateinit var repo: HelpRepositoryImpl

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        repo = HelpRepositoryImpl(dao)
    }

    @Test
    fun topicDetail_fallsBackToEnglishWhenLangMissing() = runTest {
        every { dao.getTopics() } returns flowOf(
            listOf(HelpTopicEntity("t1", 1, "schedule", "indigo"))
        )
        every { dao.getItemsForTopic("t1") } returns flowOf(
            listOf(HelpItemEntity("i1", "t1", "QUESTION", 1, null, null))
        )
        every { dao.getStringsFor("TOPIC", listOf("t1")) } returns flowOf(
            listOf(HelpStringEntity("TOPIC", "t1", "title", "en", "Prayer Times"))
        )
        every { dao.getStringsFor("ITEM", listOf("i1")) } returns flowOf(
            listOf(
                HelpStringEntity("ITEM", "i1", "question", "en", "Why?"),
                HelpStringEntity("ITEM", "i1", "answer", "en", "Because.")
            )
        )

        repo.getTopicDetail("t1", lang = "fr").test {
            val detail = awaitItem()!!
            assertThat(detail.topic.title).isEqualTo("Prayer Times") // EN fallback
            assertThat(detail.questions.single().answer).isEqualTo("Because.")
            awaitComplete()
        }
    }

    @Test
    fun topicDetail_prefersRequestedLanguage() = runTest {
        every { dao.getTopics() } returns flowOf(listOf(HelpTopicEntity("t1", 1, "schedule", "indigo")))
        every { dao.getItemsForTopic("t1") } returns flowOf(emptyList())
        every { dao.getStringsFor("TOPIC", listOf("t1")) } returns flowOf(
            listOf(
                HelpStringEntity("TOPIC", "t1", "title", "en", "Prayer Times"),
                HelpStringEntity("TOPIC", "t1", "title", "fr", "Horaires")
            )
        )
        every { dao.getStringsFor("ITEM", emptyList()) } returns flowOf(emptyList())

        repo.getTopicDetail("t1", lang = "fr").test {
            assertThat(awaitItem()!!.topic.title).isEqualTo("Horaires")
            awaitComplete()
        }
    }

    @Test
    fun guide_parsesPathLabelsJson() = runTest {
        every { dao.getItem("g1") } returns flowOf(HelpItemEntity("g1", "t1", "GUIDE", 1, "tune", 1))
        every { dao.getStepsForItem("g1") } returns flowOf(
            listOf(HelpStepEntity("s1", "g1", 1, "prayer_settings", """["More","Prayer Settings"]"""))
        )
        every { dao.getStringsFor("ITEM", listOf("g1")) } returns flowOf(
            listOf(HelpStringEntity("ITEM", "g1", "title", "en", "Change method"))
        )
        every { dao.getStringsFor("STEP", listOf("s1")) } returns flowOf(
            listOf(
                HelpStringEntity("STEP", "s1", "title", "en", "Open"),
                HelpStringEntity("STEP", "s1", "body", "en", "Go to More.")
            )
        )

        repo.getGuide("g1", lang = "en").test {
            val g = awaitItem()!!
            assertThat(g.steps.single().pathLabels).containsExactly("More", "Prayer Settings")
            assertThat(g.steps.single().deeplinkRoute).isEqualTo("prayer_settings")
            awaitComplete()
        }
    }

    @Test
    fun getTopics_resolvesTitlesAndCountsItems() = runTest {
        every { dao.getTopics() } returns flowOf(listOf(HelpTopicEntity("t1", 1, "schedule", "indigo")))
        every { dao.getAllItems() } returns flowOf(
            listOf(
                HelpItemEntity("i1", "t1", "QUESTION", 1, null, null),
                HelpItemEntity("i2", "t1", "GUIDE", 2, "tune", 1)
            )
        )
        every { dao.getStringsFor("TOPIC", listOf("t1")) } returns flowOf(
            listOf(HelpStringEntity("TOPIC", "t1", "title", "en", "Prayer Times"))
        )

        repo.getTopics(lang = "en").test {
            val topics = awaitItem()
            assertThat(topics.single().title).isEqualTo("Prayer Times")
            assertThat(topics.single().itemCount).isEqualTo(2)
            awaitComplete()
        }
    }
}
