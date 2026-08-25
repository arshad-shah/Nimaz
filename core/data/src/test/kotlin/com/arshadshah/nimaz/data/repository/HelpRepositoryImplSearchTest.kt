package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.data.local.database.dao.HelpDao
import com.arshadshah.nimaz.data.local.database.entity.HelpItemEntity
import com.arshadshah.nimaz.data.local.database.entity.HelpStringEntity
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Help search, which is a *localised* search over a table that holds every language at once.
 *
 * Two queries run — the user's language and English — and the results are merged. The merge is
 * where the failures are, and none of them looks like an error:
 *
 *  - **the localised hit must win.** English is the fallback, and the same help item exists in
 *    both, so de-duplicating the wrong way round shows an English title to a user reading in
 *    French;
 *  - **only questions and titles are searched.** Answer and body rows are in the same table, and
 *    letting them through returns a result whose "title" is a paragraph from the middle of a
 *    guide;
 *  - **a hit whose item the corpus has lost is dropped.** A stale string row would otherwise
 *    produce a search result that opens nothing.
 */
class HelpRepositoryImplSearchTest {

    private lateinit var dao: HelpDao
    private lateinit var repository: HelpRepositoryImpl

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        every { dao.getAllItems() } returns flowOf(listOf(item("q1"), item("g1", type = "GUIDE")))
        every { dao.searchStrings(any(), any()) } returns flowOf(emptyList())
        repository = HelpRepositoryImpl(dao)
    }

    @Test
    fun `a blank query searches nothing at all`() = runTest {
        assertThat(repository.search("   ", lang = "fr").first()).isEmpty()

        // Two `LIKE '%%'` queries over every string in every language is not a search.
        verify(exactly = 0) { dao.searchStrings(any(), any()) }
    }

    @Test
    fun `a localised hit and its English twin collapse to the localised one`() = runTest {
        every { dao.searchStrings("fr", "prayer") } returns
            flowOf(listOf(string("q1", "question", "fr", "Comment ?")))
        every { dao.searchStrings("en", "prayer") } returns
            flowOf(listOf(string("q1", "question", "en", "How do I?")))

        val results = repository.search("prayer", lang = "fr").first()

        assertThat(results).hasSize(1)
        assertThat(results.single().title).isEqualTo("Comment ?")
    }

    @Test
    fun `an item with no localised string still surfaces in English`() = runTest {
        every { dao.searchStrings("fr", "prayer") } returns flowOf(emptyList())
        every { dao.searchStrings("en", "prayer") } returns
            flowOf(listOf(string("q1", "question", "en", "How do I?")))

        assertThat(repository.search("prayer", "fr").first().single().title)
            .isEqualTo("How do I?")
    }

    @Test
    fun `a body or answer match is not a search result`() = runTest {
        every { dao.searchStrings("en", "prayer") } returns flowOf(
            listOf(
                string("q1", "answer", "en", "…a paragraph from the middle of the answer…"),
                string("g1", "body", "en", "…a paragraph from the middle of a guide…"),
            )
        )

        assertThat(repository.search("prayer", "en").first()).isEmpty()
    }

    @Test
    fun `a guide is marked as one and a question is not`() = runTest {
        every { dao.searchStrings("en", "prayer") } returns flowOf(
            listOf(
                string("q1", "question", "en", "How do I?"),
                string("g1", "title", "en", "Setting up prayer times"),
            )
        )

        val results = repository.search("prayer", "en").first()

        assertThat(results.single { it.itemId == "q1" }.isGuide).isFalse()
        assertThat(results.single { it.itemId == "g1" }.isGuide).isTrue()
        assertThat(results.single { it.itemId == "g1" }.topicId).isEqualTo("topic")
    }

    @Test
    fun `a string row whose help item no longer exists is dropped`() = runTest {
        every { dao.searchStrings("en", "prayer") } returns
            flowOf(listOf(string("removed", "question", "en", "orphan")))

        // Otherwise the search offers a result that opens nothing.
        assertThat(repository.search("prayer", "en").first()).isEmpty()
    }

    @Test
    fun `a string that belongs to a topic rather than an item is not a result`() = runTest {
        every { dao.searchStrings("en", "prayer") } returns
            flowOf(listOf(string("q1", "title", "en", "Prayer", ownerType = "TOPIC")))

        assertThat(repository.search("prayer", "en").first()).isEmpty()
    }

    @Test
    fun `the search query is built at collection time, not when it is asked for`() = runTest {
        every { dao.searchStrings("en", "prayer") } returns flowOf(emptyList())

        val flow = repository.search("prayer", "en")

        verify(exactly = 0) { dao.searchStrings(any(), any()) }
        flow.first()
        verify { dao.searchStrings("en", "prayer") }
    }

    private fun item(id: String, type: String = "QUESTION") = HelpItemEntity(
        id = id,
        topicId = "topic",
        type = type,
        displayOrder = 1,
        iconKey = null,
        estimatedMinutes = null,
    )

    private fun string(
        ownerId: String,
        fieldKey: String,
        lang: String,
        value: String,
        ownerType: String = "ITEM",
    ) = HelpStringEntity(
        ownerType = ownerType,
        ownerId = ownerId,
        fieldKey = fieldKey,
        langCode = lang,
        value = value,
    )
}
