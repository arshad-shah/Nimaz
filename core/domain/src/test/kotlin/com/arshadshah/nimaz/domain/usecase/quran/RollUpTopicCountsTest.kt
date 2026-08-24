package com.arshadshah.nimaz.domain.usecase.quran

import com.arshadshah.nimaz.domain.model.QuranTopic
import com.arshadshah.nimaz.domain.model.TopicTree
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The number under each subject in the browser: its own citations plus everything beneath it.
 *
 * Without the roll-up the tree opened on three bare words reading "0 verses", because the Qur'an
 * is not cited against "Doctrine" — it is cited against the things under it. So the property to
 * pin is that a branch reports its **subtree**, and that a leaf still reports itself.
 *
 * The cycle guard is the other half, and it is not defensive decoration. The parent columns are
 * *content*, regenerated per release by a separate repository, so a cycle in them is a shipping
 * possibility rather than an impossible state — and an unguarded fold would hang the subject
 * browser rather than mislabel one row. A data fault has to cost a wrong number, not the app.
 *
 * Which parent column counts depends on the tree, so the same topics must roll up differently
 * under each hierarchy. That is the bug the whole `parentIn` indirection exists to prevent:
 * a THEMATIC roll-up reading the ontology's parents gives every number a plausible wrong value.
 */
class RollUpTopicCountsTest {

    private val rollUp = RollUpTopicCounts()

    private fun topic(
        id: Int,
        ayahCount: Int = 0,
        thematicParent: Int? = null,
        ontologyParent: Int? = null,
        indexParent: Int? = null,
        isThematic: Boolean = true,
        isOntology: Boolean = false,
    ) = QuranTopic(
        id = id,
        name = "topic $id",
        arabicName = "",
        description = "",
        wikiLink = "",
        ayahCount = ayahCount,
        parentId = indexParent,
        thematicParentId = thematicParent,
        ontologyParentId = ontologyParent,
        isThematic = isThematic,
        isOntology = isOntology,
        relatedTopicIds = emptyList(),
    )

    //  1 Doctrine (0 of its own)
    //  |- 11 God (4)
    //  |   \- 111 The names of God (6)
    //  \- 13 Mercy (2)
    private val doctrine = topic(1, ayahCount = 0)
    private val god = topic(11, ayahCount = 4, thematicParent = 1)
    private val names = topic(111, ayahCount = 6, thematicParent = 11)
    private val mercy = topic(13, ayahCount = 2, thematicParent = 1)
    private val tree = listOf(doctrine, god, names, mercy)

    @Test
    fun `a branch reports everything beneath it, not just its own citations`() {
        val counts = rollUp(tree, TopicTree.THEMATIC)

        // 0 + (4 + 6) + 2
        assertThat(counts[1]).isEqualTo(12)
    }

    @Test
    fun `an intermediate branch reports its own subtree only`() {
        assertThat(rollUp(tree, TopicTree.THEMATIC)[11]).isEqualTo(10)
    }

    @Test
    fun `a leaf reports itself`() {
        val counts = rollUp(tree, TopicTree.THEMATIC)

        assertThat(counts[13]).isEqualTo(2)
        assertThat(counts[111]).isEqualTo(6)
    }

    @Test
    fun `every topic gets a number`() {
        assertThat(rollUp(tree, TopicTree.THEMATIC).keys).containsExactly(1, 11, 111, 13)
    }

    @Test
    fun `nothing in means nothing out`() {
        assertThat(rollUp(emptyList(), TopicTree.THEMATIC)).isEmpty()
    }

    // ---- The tree decides which parent column is read ----

    @Test
    fun `the same topics roll up differently under a different hierarchy`() {
        // Under the ontology, `names` hangs off Doctrine directly rather than off God.
        val ontology = listOf(
            topic(1, ayahCount = 0, ontologyParent = null, isOntology = true),
            topic(11, ayahCount = 4, ontologyParent = 1, isOntology = true),
            topic(111, ayahCount = 6, ontologyParent = 1, isOntology = true),
            topic(13, ayahCount = 2, ontologyParent = 1, isOntology = true),
        )

        val counts = rollUp(ontology, TopicTree.ONTOLOGY)

        assertThat(counts[1]).isEqualTo(12)
        assertThat(counts[11]).isEqualTo(4)
    }

    @Test
    fun `a hierarchy whose parent column is empty leaves every topic standing alone`() {
        // The thematic parents are set; asked for the ontology, none of them applies.
        val counts = rollUp(tree, TopicTree.ONTOLOGY)

        assertThat(counts[1]).isEqualTo(0)
        assertThat(counts[11]).isEqualTo(4)
        assertThat(counts[111]).isEqualTo(6)
    }

    @Test
    fun `the index hierarchy reads the plain parent column`() {
        val indexed = listOf(
            topic(1, ayahCount = 1, indexParent = null),
            topic(2, ayahCount = 3, indexParent = 1),
        )

        assertThat(rollUp(indexed, TopicTree.INDEX)[1]).isEqualTo(4)
    }

    // ---- Broken content ----

    @Test
    fun `a cycle in the parent columns costs a wrong number, not the app`() {
        // 1 -> 2 -> 1. Regenerated content could ship this; an unguarded fold would not return.
        val cyclic = listOf(
            topic(1, ayahCount = 5, thematicParent = 2),
            topic(2, ayahCount = 7, thematicParent = 1),
        )

        val counts = rollUp(cyclic, TopicTree.THEMATIC)

        assertThat(counts.keys).containsExactly(1, 2)
        counts.values.forEach { assertThat(it).isAtLeast(0) }
    }

    @Test
    fun `a topic that is its own parent still returns`() {
        val counts = rollUp(listOf(topic(1, ayahCount = 5, thematicParent = 1)), TopicTree.THEMATIC)

        assertThat(counts[1]).isEqualTo(5)
    }

    @Test
    fun `a parent naming a topic that is not in the list is a broken edge, not a node`() {
        val orphan = listOf(topic(11, ayahCount = 4, thematicParent = 99))

        val counts = rollUp(orphan, TopicTree.THEMATIC)

        assertThat(counts.keys).containsExactly(11)
        assertThat(counts[11]).isEqualTo(4)
    }

    // ---- The indirection the roll-up is built on ----

    @Test
    fun `parentIn reads the column the tree names`() {
        val t = topic(5, thematicParent = 1, ontologyParent = 2, indexParent = 3)

        assertThat(t.parentIn(TopicTree.THEMATIC)).isEqualTo(1)
        assertThat(t.parentIn(TopicTree.ONTOLOGY)).isEqualTo(2)
        assertThat(t.parentIn(TopicTree.INDEX)).isEqualTo(3)
    }

    @Test
    fun `belongsTo answers per hierarchy, and the index holds everything`() {
        val thematicOnly = topic(5, isThematic = true, isOntology = false)

        assertThat(thematicOnly.belongsTo(TopicTree.THEMATIC)).isTrue()
        assertThat(thematicOnly.belongsTo(TopicTree.ONTOLOGY)).isFalse()
        // The index is the flat fallback: every subject is in it.
        assertThat(thematicOnly.belongsTo(TopicTree.INDEX)).isTrue()
    }

    @Test
    fun `a subject with no tree of its own opens in the index rather than an empty outline`() {
        // 1,817 subjects the curated outline does not place. Defaulting them to THEMATIC is what
        // produced a detail screen with no breadcrumb and no subtopics.
        val placed = topic(1, isThematic = true, isOntology = true)
        val ontologyOnly = topic(2, isThematic = false, isOntology = true)
        val neither = topic(3, isThematic = false, isOntology = false)

        assertThat(placed.homeTree).isEqualTo(TopicTree.THEMATIC)
        assertThat(ontologyOnly.homeTree).isEqualTo(TopicTree.ONTOLOGY)
        assertThat(neither.homeTree).isEqualTo(TopicTree.INDEX)
    }
}
