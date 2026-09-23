package com.arshadshah.nimaz.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The subject index in memory, and the numbers every Topics card shows.
 *
 * Three properties carry the feature. A branch reports its **subtree**, because the Qur'an is not
 * cited against "Doctrine" but against the things under it — without that, the browser opened on
 * roots reading "0 verses". The subtree is counted in **distinct verses**, because a verse cited
 * under two siblings is one verse, and summing reported "Relationship with Allah" at 1,261 while
 * its own screen listed 1,041. And a **cycle or dangling parent** in the content costs a wrong
 * number, never a hang: the parent columns are regenerated per release by a separate repository.
 *
 * Which parent column counts depends on the tree, so the same topics must shape differently under
 * each hierarchy — a THEMATIC count reading the ontology's parents gives every number a
 * plausible wrong value.
 */
class TopicCatalogTest {

    private fun topic(
        id: Int,
        name: String = "topic $id",
        arabic: String = "",
        thematicParent: Int? = null,
        ontologyParent: Int? = null,
        indexParent: Int? = null,
        isThematic: Boolean = true,
        isOntology: Boolean = false,
    ) = QuranTopic(
        id = id,
        name = name,
        arabicName = arabic,
        description = "",
        wikiLink = "",
        ayahCount = 0,
        parentId = indexParent,
        thematicParentId = thematicParent,
        ontologyParentId = ontologyParent,
        isThematic = isThematic,
        isOntology = isOntology,
        relatedTopicIds = emptyList(),
    )

    //  1 Doctrine          (none of its own)
    //  |- 11 God           verses 1, 2, 3, 4
    //  |   \- 111 Names    verses 3, 4, 5, 6, 7, 8   (3 and 4 shared with God)
    //  \- 13 Mercy         verses 8, 9               (8 shared with Names)
    private val doctrine = topic(1, "Doctrine")
    private val god = topic(11, "God", thematicParent = 1)
    private val names = topic(111, "The names of God", thematicParent = 11)
    private val mercy = topic(13, "Mercy", thematicParent = 1)
    private val citations = mapOf(
        11 to listOf(1, 2, 3, 4),
        111 to listOf(3, 4, 5, 6, 7, 8),
        13 to listOf(8, 9),
    )
    private val catalog = TopicCatalog(listOf(doctrine, god, names, mercy), citations)

    // ---- Counting ----

    @Test
    fun `a branch reports every distinct verse beneath it`() {
        // 1..9 — not 4 + 6 + 2 = 12, which is what summing the children said.
        assertThat(catalog.verseCount(1, TopicTree.THEMATIC)).isEqualTo(9)
    }

    @Test
    fun `an intermediate branch counts its own citations and its subtree's, once each`() {
        assertThat(catalog.verseCount(11, TopicTree.THEMATIC)).isEqualTo(8)
    }

    @Test
    fun `a leaf reports itself`() {
        assertThat(catalog.verseCount(13, TopicTree.THEMATIC)).isEqualTo(2)
        assertThat(catalog.verseCount(111, TopicTree.THEMATIC)).isEqualTo(6)
    }

    @Test
    fun `a topic nobody cites and nothing sits under counts zero rather than failing`() {
        val bare = TopicCatalog(listOf(topic(5)), emptyMap())
        assertThat(bare.verseCount(5, TopicTree.THEMATIC)).isEqualTo(0)
        assertThat(bare.verseCount(404, TopicTree.THEMATIC)).isEqualTo(0)
    }

    // ---- Shape ----

    @Test
    fun `roots are the tree's parentless topics, biggest first`() {
        val two = TopicCatalog(
            listOf(topic(1), topic(2), topic(21, thematicParent = 2)),
            mapOf(1 to listOf(1), 21 to listOf(1, 2, 3)),
        )
        assertThat(two.roots(TopicTree.THEMATIC).map { it.id }).containsExactly(2, 1).inOrder()
    }

    @Test
    fun `a tree only has the topics that belong to it`() {
        val mixed = TopicCatalog(
            listOf(topic(1, isThematic = true), topic(2, isThematic = false, isOntology = true)),
            emptyMap(),
        )
        assertThat(mixed.roots(TopicTree.THEMATIC).map { it.id }).containsExactly(1)
        assertThat(mixed.roots(TopicTree.ONTOLOGY).map { it.id }).containsExactly(2)
        // The index holds everything.
        assertThat(mixed.roots(TopicTree.INDEX).map { it.id }).containsExactly(1, 2)
    }

    @Test
    fun `children come biggest first, then alphabetically`() {
        assertThat(catalog.children(1, TopicTree.THEMATIC).map { it.id }).containsExactly(11, 13).inOrder()
        assertThat(catalog.childCount(1, TopicTree.THEMATIC)).isEqualTo(2)
        assertThat(catalog.childCount(13, TopicTree.THEMATIC)).isEqualTo(0)
    }

    @Test
    fun `the path runs from the root down, and excludes the topic itself`() {
        assertThat(catalog.path(111, TopicTree.THEMATIC).map { it.id }).containsExactly(1, 11).inOrder()
        assertThat(catalog.path(1, TopicTree.THEMATIC)).isEmpty()
    }

    @Test
    fun `the same topics shape differently under a different hierarchy`() {
        // Under the ontology, Names hangs off Doctrine directly rather than off God.
        val topics = listOf(
            topic(1, ontologyParent = null, isOntology = true),
            topic(11, ontologyParent = 1, isOntology = true, thematicParent = 1),
            topic(111, ontologyParent = 1, isOntology = true, thematicParent = 11),
        )
        val both = TopicCatalog(topics, citations)

        assertThat(both.path(111, TopicTree.ONTOLOGY).map { it.id }).containsExactly(1)
        assertThat(both.path(111, TopicTree.THEMATIC).map { it.id }).containsExactly(1, 11).inOrder()
        assertThat(both.verseCount(11, TopicTree.ONTOLOGY)).isEqualTo(4)
        assertThat(both.verseCount(11, TopicTree.THEMATIC)).isEqualTo(8)
    }

    // ---- Search ----

    @Test
    fun `a name that starts with the query outranks one that merely contains it`() {
        val found = TopicCatalog(
            listOf(topic(1, "Impatience"), topic(2, "Patience")),
            mapOf(1 to listOf(1, 2, 3)),
        ).search("patience")
        assertThat(found.map { it.id }).containsExactly(2, 1).inOrder()
    }

    @Test
    fun `search matches the Arabic name, and ignores case in the English`() {
        val found = TopicCatalog(listOf(topic(1, "Allah", arabic = "الله"), topic(2, "Mercy")), emptyMap())
        assertThat(found.search("الله").map { it.id }).containsExactly(1)
        assertThat(found.search("MERCY").map { it.id }).containsExactly(2)
    }

    @Test
    fun `a blank query finds nothing, and the limit is honoured`() {
        val many = TopicCatalog((1..10).map { topic(it, "Mercy $it") }, emptyMap())
        assertThat(many.search("  ")).isEmpty()
        assertThat(many.search("mercy", limit = 3)).hasSize(3)
    }

    // ---- Broken content ----

    @Test
    fun `a cycle in the parent columns costs a wrong number, not the app`() {
        // 1 -> 2 -> 1. Regenerated content could ship this; an unguarded walk would not return.
        val cyclic = TopicCatalog(
            listOf(topic(1, thematicParent = 2), topic(2, thematicParent = 1)),
            mapOf(1 to listOf(1), 2 to listOf(2)),
        )
        assertThat(cyclic.verseCount(1, TopicTree.THEMATIC)).isAtLeast(1)
        assertThat(cyclic.path(1, TopicTree.THEMATIC).size).isAtMost(1)
    }

    @Test
    fun `a topic that is its own parent is a root, not a loop`() {
        val self = TopicCatalog(listOf(topic(1, thematicParent = 1)), mapOf(1 to listOf(7)))
        assertThat(self.roots(TopicTree.THEMATIC).map { it.id }).containsExactly(1)
        assertThat(self.verseCount(1, TopicTree.THEMATIC)).isEqualTo(1)
    }

    @Test
    fun `a parent missing from the catalogue leaves its child standing at the top`() {
        val orphan = TopicCatalog(listOf(topic(11, thematicParent = 99)), mapOf(11 to listOf(1)))
        assertThat(orphan.roots(TopicTree.THEMATIC).map { it.id }).containsExactly(11)
        assertThat(orphan.path(11, TopicTree.THEMATIC)).isEmpty()
    }

    // ---- The indirection the catalogue is built on ----

    @Test
    fun `parentIn reads the column the tree names`() {
        val t = topic(5, thematicParent = 1, ontologyParent = 2, indexParent = 3)
        assertThat(t.parentIn(TopicTree.THEMATIC)).isEqualTo(1)
        assertThat(t.parentIn(TopicTree.ONTOLOGY)).isEqualTo(2)
        assertThat(t.parentIn(TopicTree.INDEX)).isEqualTo(3)
    }

    @Test
    fun `a subject with no tree of its own opens in the index rather than an empty outline`() {
        assertThat(topic(1, isThematic = true, isOntology = true).homeTree).isEqualTo(TopicTree.THEMATIC)
        assertThat(topic(2, isThematic = false, isOntology = true).homeTree).isEqualTo(TopicTree.ONTOLOGY)
        assertThat(topic(3, isThematic = false, isOntology = false).homeTree).isEqualTo(TopicTree.INDEX)
    }
}
