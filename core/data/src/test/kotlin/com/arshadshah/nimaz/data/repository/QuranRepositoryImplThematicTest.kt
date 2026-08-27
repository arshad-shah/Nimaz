package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.data.local.database.dao.QuranDao
import com.arshadshah.nimaz.data.local.database.entity.AyahThemeEntity
import com.arshadshah.nimaz.data.local.database.entity.QuranTopicAyahEntity
import com.arshadshah.nimaz.data.local.database.entity.QuranTopicEntity
import com.arshadshah.nimaz.data.local.database.entity.SurahOverviewEntity
import com.arshadshah.nimaz.data.local.database.entity.SurahOverviewSectionEntity
import com.arshadshah.nimaz.data.local.database.entity.TopicWithSurahCount
import com.arshadshah.nimaz.data.local.search.ContentSearchIndex
import com.arshadshah.nimaz.data.local.search.SearchKind
import com.arshadshah.nimaz.data.local.user.BookmarkDao
import com.arshadshah.nimaz.data.local.user.ReadingProgressDao
import com.arshadshah.nimaz.domain.model.SurahOverviewGroup
import com.arshadshah.nimaz.domain.model.TopicTree
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * The thematic layer (schemaVersion 24) — the half of [QuranRepositoryImpl] that turns three
 * overlapping topic hierarchies into something a screen can render.
 *
 * What is pinned here is the logic that fails *quietly*, not the mapping:
 *
 *  - a breadcrumb is a loop over parent columns that are **content**, regenerated per release
 *    elsewhere. A cycle in them hangs a screen forever, and the two visited-set guards are the
 *    only thing between the app and that. Neither is exercised by data that ships, which is
 *    exactly why they need a test;
 *  - `getTopicBreadcrumbs` picks the hierarchy *per topic*, not per call, because search spans
 *    all three. Get that wrong and a result from the ontology comes back pathless while the
 *    thematic tab is selected — a blank breadcrumb, no error;
 *  - `getTopicDetail` cites the whole **subtree**, not the node. Asking only for a node's own
 *    citations is what made "Doctrine" open on "0 verses";
 *  - `searchTopics` re-sorts by index rank because `getTopics` does not preserve it, so a
 *    relevance-ordered search that silently comes back in id order looks like it works;
 *  - `hasThematicContent` is memoised, and a memo that re-queries is invisible except as a
 *    slow screen.
 */
class QuranRepositoryImplThematicTest {

    private lateinit var quranDao: QuranDao
    private lateinit var bookmarkDao: BookmarkDao
    private lateinit var readingProgressDao: ReadingProgressDao
    private lateinit var searchIndex: ContentSearchIndex
    private lateinit var repository: QuranRepositoryImpl

    @Before
    fun setUp() {
        quranDao = mockk(relaxed = true)
        bookmarkDao = mockk(relaxed = true)
        readingProgressDao = mockk(relaxed = true)
        searchIndex = mockk(relaxed = true)
        coEvery { searchIndex.isAvailable() } returns false
        repository = QuranRepositoryImpl(quranDao, bookmarkDao, readingProgressDao, searchIndex)
    }

    // ── the surah overview ────────────────────────────────────────────────────

    @Test
    fun `a surah with no overview row reports none rather than an empty one`() = runTest {
        coEvery { quranDao.getSurahOverview(9) } returns null

        assertThat(repository.getSurahOverview(9)).isNull()
        // The sections query must not run for a surah that has no overview to hang them on.
        coVerify(exactly = 0) { quranDao.getSurahOverviewSections(any()) }
    }

    @Test
    fun `an overview carries its sections in the group each one declares`() = runTest {
        coEvery { quranDao.getSurahOverview(2) } returns
            SurahOverviewEntity(surahNumber = 2, summary = "The longest surah.")
        coEvery { quranDao.getSurahOverviewSections(2) } returns listOf(
            section(position = 1, heading = "Guidance", group = "name"),
            section(position = 2, heading = "Law", group = "theme"),
        )

        val overview = repository.getSurahOverview(2)!!

        assertThat(overview.summary).isEqualTo("The longest surah.")
        assertThat(overview.sections.map { it.heading }).containsExactly("Guidance", "Law").inOrder()
        assertThat(overview.sections.map { it.group })
            .containsExactly(SurahOverviewGroup.NAME, SurahOverviewGroup.THEME)
            .inOrder()
    }

    @Test
    fun `a section whose group is not a known wire value still renders`() = runTest {
        coEvery { quranDao.getSurahOverview(1) } returns
            SurahOverviewEntity(surahNumber = 1, summary = "The Opening.")
        coEvery { quranDao.getSurahOverviewSections(1) } returns
            listOf(section(position = 1, heading = "Odd", group = "a-group-from-a-newer-corpus"))

        val sections = repository.getSurahOverview(1)!!.sections

        // A corpus that adds a group must not make an older build drop the section.
        assertThat(sections).hasSize(1)
        assertThat(sections.single().group).isEqualTo(SurahOverviewGroup.OTHER)
    }

    // ── themes ────────────────────────────────────────────────────────────────

    @Test
    fun `a surah's themes come back as verse spans, not as single verses`() = runTest {
        coEvery { quranDao.getThemesForSurah(12) } returns listOf(
            AyahThemeEntity(
                surahNumber = 12, ayahFrom = 4, ayahTo = 6,
                theme = "The dream", keywords = "dream,stars", ayahCount = 3,
            )
        )

        val theme = repository.getThemesForSurah(12).single()

        assertThat(theme.ayahFrom).isEqualTo(4)
        assertThat(theme.ayahTo).isEqualTo(6)
        assertThat(theme.ayahCount).isEqualTo(3)
        assertThat(theme.theme).isEqualTo("The dream")
    }

    @Test
    fun `a verse outside every theme span reports no theme`() = runTest {
        coEvery { quranDao.getThemeForAyah(12, 99) } returns null

        assertThat(repository.getThemeForAyah(12, 99)).isNull()
    }

    @Test
    fun `a verse inside a span reports the span's theme`() = runTest {
        coEvery { quranDao.getThemeForAyah(12, 5) } returns AyahThemeEntity(
            surahNumber = 12, ayahFrom = 4, ayahTo = 6,
            theme = "The dream", keywords = "dream", ayahCount = 3,
        )

        assertThat(repository.getThemeForAyah(12, 5)!!.theme).isEqualTo("The dream")
    }

    // ── the three hierarchies ─────────────────────────────────────────────────

    @Test
    fun `each tree is queried by its own wire name, not by its enum name`() = runTest {
        coEvery { quranDao.getRootTopics(any()) } returns emptyList()

        repository.getTopicTreeRoots(TopicTree.ONTOLOGY)

        // The wire value is what the corpus stores; the enum name is uppercase and matches nothing.
        coVerify { quranDao.getRootTopics("ontology") }
    }

    @Test
    fun `a topic's children are read from the tree being browsed`() = runTest {
        coEvery { quranDao.getChildTopics("index", 7) } returns listOf(topic(id = 8, name = "Musa"))

        val children = repository.getTopicChildren(topicId = 7, tree = TopicTree.INDEX)

        assertThat(children.map { it.name }).containsExactly("Musa")
    }

    @Test
    fun `the branch ids arrive as a set so a screen can test membership`() = runTest {
        coEvery { quranDao.getBranchTopicIds("thematic") } returns listOf(1, 2, 2, 3)

        assertThat(repository.getBranchTopicIds(TopicTree.THEMATIC)).containsExactly(1, 2, 3)
    }

    @Test
    fun `a topic's comma separated related ids become a list of ints`() = runTest {
        coEvery { quranDao.getAllTopics() } returns
            listOf(topic(id = 1, relatedTopicIds = "4, 5 ,not-a-number,6"))

        val related = repository.getAllTopics().single().relatedTopicIds

        // Whitespace and a junk entry from a hand-edited corpus must not lose the real ids.
        assertThat(related).containsExactly(4, 5, 6).inOrder()
    }

    @Test
    fun `a topic with no related ids reports an empty list rather than a zero`() = runTest {
        coEvery { quranDao.getAllTopics() } returns listOf(topic(id = 1, relatedTopicIds = ""))

        assertThat(repository.getAllTopics().single().relatedTopicIds).isEmpty()
    }

    @Test
    fun `the thematic and ontology flags are read as flags, not as counts`() = runTest {
        coEvery { quranDao.getAllTopics() } returns
            listOf(topic(id = 1, isThematic = 1, isOntology = 0))

        val subject = repository.getAllTopics().single()

        assertThat(subject.isThematic).isTrue()
        assertThat(subject.isOntology).isFalse()
    }

    // ── breadcrumbs ───────────────────────────────────────────────────────────

    @Test
    fun `asking for no breadcrumbs queries nothing`() = runTest {
        assertThat(repository.getTopicBreadcrumbs(emptyList(), TopicTree.THEMATIC)).isEmpty()

        coVerify(exactly = 0) { quranDao.getTopics(any()) }
    }

    @Test
    fun `a breadcrumb climbs to the root and reads root first`() = runTest {
        // 3 -> 2 -> 1 in the thematic tree.
        val leaf = topic(id = 3, name = "Leaf", thematicParentId = 2, isThematic = 1)
        val mid = topic(id = 2, name = "Mid", thematicParentId = 1, isThematic = 1)
        val root = topic(id = 1, name = "Root", thematicParentId = null, isThematic = 1)
        coEvery { quranDao.getTopics(listOf(3)) } returns listOf(leaf)
        coEvery { quranDao.getTopics(listOf(2)) } returns listOf(mid)
        coEvery { quranDao.getTopics(listOf(1)) } returns listOf(root)

        val trail = repository.getTopicBreadcrumbs(listOf(3), TopicTree.THEMATIC).getValue(3)

        assertThat(trail.map { it.name }).containsExactly("Root", "Mid").inOrder()
    }

    @Test
    fun `a cycle in the corpus's parents terminates instead of hanging`() = runTest {
        // 1 -> 2 -> 1. Content is validated acyclic at import; the next corpus might not be.
        val a = topic(id = 1, name = "A", thematicParentId = 2, isThematic = 1)
        val b = topic(id = 2, name = "B", thematicParentId = 1, isThematic = 1)
        coEvery { quranDao.getTopics(listOf(1)) } returns listOf(a)
        coEvery { quranDao.getTopics(listOf(2)) } returns listOf(b)

        val trail = repository.getTopicBreadcrumbs(listOf(1), TopicTree.THEMATIC).getValue(1)

        // B is reached once; the step back to A is refused by the visited set.
        assertThat(trail.map { it.name }).containsExactly("B")
    }

    @Test
    fun `an ontology topic keeps its path while the thematic tab is selected`() = runTest {
        // Search spans all three trees, so a result the thematic outline does not place must
        // still come back with the path the ontology gives it.
        val leaf = topic(id = 5, name = "Camel", ontologyParentId = 4, isThematic = 0, isOntology = 1)
        val parent = topic(id = 4, name = "Living Creation", isThematic = 0, isOntology = 1)
        coEvery { quranDao.getTopics(listOf(5)) } returns listOf(leaf)
        coEvery { quranDao.getTopics(listOf(4)) } returns listOf(parent)

        val trail = repository.getTopicBreadcrumbs(listOf(5), TopicTree.THEMATIC).getValue(5)

        assertThat(trail.map { it.name }).containsExactly("Living Creation")
    }

    @Test
    fun `two topics at different depths each climb only as far as their own root`() = runTest {
        val deep = topic(id = 3, name = "Deep", thematicParentId = 2, isThematic = 1)
        val mid = topic(id = 2, name = "Mid", thematicParentId = 1, isThematic = 1)
        val root = topic(id = 1, name = "Root", isThematic = 1)
        val shallow = topic(id = 9, name = "Shallow", thematicParentId = 1, isThematic = 1)
        coEvery { quranDao.getTopics(listOf(3, 9)) } returns listOf(deep, shallow)
        coEvery { quranDao.getTopics(listOf(2, 1)) } returns listOf(mid, root)
        coEvery { quranDao.getTopics(listOf(1)) } returns listOf(root)

        val trails = repository.getTopicBreadcrumbs(listOf(3, 9), TopicTree.THEMATIC)

        assertThat(trails.getValue(3).map { it.name }).containsExactly("Root", "Mid").inOrder()
        assertThat(trails.getValue(9).map { it.name }).containsExactly("Root")
    }

    @Test
    fun `an id the corpus does not know gets no trail rather than an empty one`() = runTest {
        coEvery { quranDao.getTopics(listOf(404)) } returns emptyList()

        assertThat(repository.getTopicBreadcrumbs(listOf(404), TopicTree.THEMATIC)).isEmpty()
    }

    @Test
    fun `a parent the corpus points at but does not hold ends the trail`() = runTest {
        val leaf = topic(id = 3, name = "Leaf", thematicParentId = 99, isThematic = 1)
        coEvery { quranDao.getTopics(listOf(3)) } returns listOf(leaf)
        coEvery { quranDao.getTopics(listOf(99)) } returns emptyList()

        assertThat(repository.getTopicBreadcrumbs(listOf(3), TopicTree.THEMATIC).getValue(3))
            .isEmpty()
    }

    // ── the detail screen ─────────────────────────────────────────────────────

    @Test
    fun `a topic id that does not exist opens nothing rather than an empty screen`() = runTest {
        coEvery { quranDao.getTopic(1) } returns null

        assertThat(repository.getTopicDetail(1, TopicTree.THEMATIC)).isNull()
        coVerify(exactly = 0) { quranDao.getTopicAyahsIn(any()) }
    }

    @Test
    fun `a branch cites the verses of its whole subtree, not just its own`() = runTest {
        // "Doctrine" holds no citations itself; its children carry them all.
        coEvery { quranDao.getTopic(1) } returns topic(id = 1, name = "Doctrine", isThematic = 1)
        coEvery { quranDao.getChildTopics("thematic", 1) } returns
            listOf(topic(id = 2, name = "Tawhid", thematicParentId = 1, isThematic = 1))
        coEvery { quranDao.getChildTopics("thematic", 2) } returns
            listOf(topic(id = 3, name = "Names", thematicParentId = 2, isThematic = 1))
        coEvery { quranDao.getChildTopics("thematic", 3) } returns emptyList()
        coEvery { quranDao.getTopicAyahsIn(listOf(1, 2, 3)) } returns listOf(
            QuranTopicAyahEntity(topicId = 3, ayahId = 262, surahNumber = 2, ayahNumber = 255)
        )

        val detail = repository.getTopicDetail(1, TopicTree.THEMATIC)!!

        assertThat(detail.citations.map { it.ayahId }).containsExactly(262)
        assertThat(detail.children.map { it.name }).containsExactly("Tawhid")
    }

    @Test
    fun `a cycle among children stops the subtree walk`() = runTest {
        coEvery { quranDao.getTopic(1) } returns topic(id = 1, name = "A", isThematic = 1)
        coEvery { quranDao.getChildTopics("thematic", 1) } returns listOf(topic(id = 2, name = "B"))
        coEvery { quranDao.getChildTopics("thematic", 2) } returns listOf(topic(id = 1, name = "A"))
        coEvery { quranDao.getTopicAyahsIn(any()) } returns emptyList()

        val detail = repository.getTopicDetail(1, TopicTree.THEMATIC)!!

        assertThat(detail.children.map { it.name }).containsExactly("B")
        // The walk visits each id once, so it terminates: 2 points back at 1 and 1 is not
        // re-expanded. Without the visited set this never returns.
        coVerify { quranDao.getTopicAyahsIn(listOf(1, 2)) }
    }

    @Test
    fun `a detail's breadcrumb walks upwards and stops at a cycle`() = runTest {
        coEvery { quranDao.getTopic(3) } returns
            topic(id = 3, name = "Leaf", thematicParentId = 2, isThematic = 1)
        coEvery { quranDao.getTopic(2) } returns
            topic(id = 2, name = "Mid", thematicParentId = 3, isThematic = 1)
        coEvery { quranDao.getTopicAyahsIn(any()) } returns emptyList()

        val detail = repository.getTopicDetail(3, TopicTree.THEMATIC)!!

        assertThat(detail.breadcrumb.map { it.name }).containsExactly("Mid")
    }

    @Test
    fun `a detail's breadcrumb ends where the corpus loses the parent`() = runTest {
        coEvery { quranDao.getTopic(3) } returns
            topic(id = 3, name = "Leaf", thematicParentId = 77, isThematic = 1)
        coEvery { quranDao.getTopic(77) } returns null
        coEvery { quranDao.getTopicAyahsIn(any()) } returns emptyList()

        assertThat(repository.getTopicDetail(3, TopicTree.THEMATIC)!!.breadcrumb).isEmpty()
    }

    @Test
    fun `related topics are fetched only when the topic declares some`() = runTest {
        coEvery { quranDao.getTopic(1) } returns topic(id = 1, relatedTopicIds = "")
        coEvery { quranDao.getTopicAyahsIn(any()) } returns emptyList()

        val detail = repository.getTopicDetail(1, TopicTree.THEMATIC)!!

        assertThat(detail.related).isEmpty()
        coVerify(exactly = 0) { quranDao.getTopics(any()) }
    }

    @Test
    fun `a topic that declares related ids resolves them to topics`() = runTest {
        coEvery { quranDao.getTopic(1) } returns topic(id = 1, relatedTopicIds = "4,5")
        coEvery { quranDao.getTopics(listOf(4, 5)) } returns
            listOf(topic(id = 4, name = "Sabr"), topic(id = 5, name = "Shukr"))
        coEvery { quranDao.getTopicAyahsIn(any()) } returns emptyList()

        val detail = repository.getTopicDetail(1, TopicTree.THEMATIC)!!

        assertThat(detail.related.map { it.name }).containsExactly("Sabr", "Shukr")
        assertThat(detail.tree).isEqualTo(TopicTree.THEMATIC)
    }

    // ── a surah's subjects ────────────────────────────────────────────────────

    @Test
    fun `a surah's subjects carry the count of verses in this surah, not across the Quran`() =
        runTest {
            coEvery { quranDao.getTopicsForSurah(112) } returns listOf(
                TopicWithSurahCount(topic = topic(id = 1, name = "Allah", ayahCount = 153), versesHere = 2)
            )

            val subject = repository.getTopicsForSurah(112).single()

            // Ordering a surah's subjects by the Quran-wide count is what this projection exists
            // to prevent; the domain model must expose the local count.
            assertThat(subject.versesInSurah).isEqualTo(2)
            assertThat(subject.topic.ayahCount).isEqualTo(153)
        }

    @Test
    fun `a verse's subjects come back as topics`() = runTest {
        coEvery { quranDao.getTopicsForAyah(262) } returns listOf(topic(id = 1, name = "Tawhid"))

        assertThat(repository.getTopicsForAyah(262).map { it.name }).containsExactly("Tawhid")
    }

    @Test
    fun `a surah's subject count is asked of the database, not derived from a list`() = runTest {
        coEvery { quranDao.countTopicsForSurah(2) } returns 431

        assertThat(repository.countTopicsForSurah(2)).isEqualTo(431)
        coVerify(exactly = 0) { quranDao.getTopicsForSurah(any()) }
    }

    // ── topic search ──────────────────────────────────────────────────────────

    @Test
    fun `a blank query searches nothing`() = runTest {
        assertThat(repository.searchTopics("   ", limit = 10)).isEmpty()

        coVerify(exactly = 0) { quranDao.searchTopicsByName(any(), any()) }
    }

    @Test
    fun `without an index the search falls back to the English name`() = runTest {
        coEvery { searchIndex.isAvailable() } returns false
        coEvery { quranDao.searchTopicsByName("musa", 5) } returns listOf(topic(id = 1, name = "Musa"))

        assertThat(repository.searchTopics("  musa  ", limit = 5).map { it.name })
            .containsExactly("Musa")
    }

    @Test
    fun `with an index the results keep relevance order, not id order`() = runTest {
        coEvery { searchIndex.isAvailable() } returns true
        coEvery { searchIndex.refs("resurrection", SearchKind.TOPIC, null, any()) } returns listOf("9", "3", "7")
        // getTopics does not preserve the order it was asked in.
        coEvery { quranDao.getTopics(listOf(9, 3, 7)) } returns
            listOf(topic(id = 3, name = "C"), topic(id = 7, name = "B"), topic(id = 9, name = "A"))

        val names = repository.searchTopics("resurrection", limit = 10).map { it.name }

        assertThat(names).containsExactly("A", "C", "B").inOrder()
    }

    @Test
    fun `the index result is capped at the limit before the rows are fetched`() = runTest {
        coEvery { searchIndex.isAvailable() } returns true
        coEvery { searchIndex.refs("a", SearchKind.TOPIC, null, any()) } returns listOf("1", "2", "3", "4")
        coEvery { quranDao.getTopics(listOf(1, 2)) } returns
            listOf(topic(id = 1), topic(id = 2))

        assertThat(repository.searchTopics("a", limit = 2)).hasSize(2)
        coVerify { quranDao.getTopics(listOf(1, 2)) }
    }

    @Test
    fun `index refs that are not ids are dropped, and duplicates collapse`() = runTest {
        coEvery { searchIndex.isAvailable() } returns true
        coEvery { searchIndex.refs("a", SearchKind.TOPIC, null, any()) } returns
            listOf("1", "not-an-id", "1", "2")
        coEvery { quranDao.getTopics(listOf(1, 2)) } returns listOf(topic(id = 1), topic(id = 2))

        assertThat(repository.searchTopics("a", limit = 10)).hasSize(2)
    }

    @Test
    fun `an index that matches nothing does not query for an empty id list`() = runTest {
        coEvery { searchIndex.isAvailable() } returns true
        coEvery { searchIndex.refs("zzz", SearchKind.TOPIC, null, any()) } returns emptyList()

        assertThat(repository.searchTopics("zzz", limit = 10)).isEmpty()
        coVerify(exactly = 0) { quranDao.getTopics(any()) }
    }

    @Test
    fun `a row the index knows but the table has lost sorts last rather than crashing`() = runTest {
        coEvery { searchIndex.isAvailable() } returns true
        coEvery { searchIndex.refs("a", SearchKind.TOPIC, null, any()) } returns listOf("1", "2")
        // Row 2 is returned; row 1 is not — a stale index against a replaced content database.
        coEvery { quranDao.getTopics(listOf(1, 2)) } returns listOf(topic(id = 2, name = "B"))

        assertThat(repository.searchTopics("a", limit = 10).map { it.name }).containsExactly("B")
    }

    // ── the thematic-content probe ────────────────────────────────────────────

    @Test
    fun `an install whose artifact carries the thematic layer is asked once`() = runTest {
        coEvery { quranDao.countThemes() } returns 1200
        coEvery { quranDao.countTopics() } returns 2512

        assertThat(repository.hasThematicContent()).isTrue()
        assertThat(repository.hasThematicContent()).isTrue()

        // The content database is replaced wholesale by a release, never written to at runtime,
        // so two COUNT(*)s per screen would be two queries to learn a constant.
        coVerify(exactly = 1) { quranDao.countThemes() }
    }

    @Test
    fun `topics without themes does not count as thematic content`() = runTest {
        coEvery { quranDao.countThemes() } returns 0
        coEvery { quranDao.countTopics() } returns 2512

        assertThat(repository.hasThematicContent()).isFalse()
        // Short-circuits: no point counting topics when there are no themes.
        coVerify(exactly = 0) { quranDao.countTopics() }
    }

    @Test
    fun `themes without topics does not count as thematic content`() = runTest {
        coEvery { quranDao.countThemes() } returns 1200
        coEvery { quranDao.countTopics() } returns 0

        assertThat(repository.hasThematicContent()).isFalse()
    }

    // ── fixtures ──────────────────────────────────────────────────────────────

    private fun section(position: Int, heading: String, group: String) =
        SurahOverviewSectionEntity(
            surahNumber = 2,
            position = position,
            heading = heading,
            group = group,
            body = "body",
        )

    private fun topic(
        id: Int,
        name: String = "Topic $id",
        parentId: Int? = null,
        thematicParentId: Int? = null,
        ontologyParentId: Int? = null,
        isThematic: Int = 1,
        isOntology: Int = 0,
        ayahCount: Int = 0,
        relatedTopicIds: String = "",
    ) = QuranTopicEntity(
        topicId = id,
        name = name,
        arabicName = "",
        parentId = parentId,
        thematicParentId = thematicParentId,
        ontologyParentId = ontologyParentId,
        description = "",
        wikiLink = "",
        isThematic = isThematic,
        isOntology = isOntology,
        ayahCount = ayahCount,
        relatedTopicIds = relatedTopicIds,
    )
}
