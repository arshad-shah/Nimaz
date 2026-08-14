package com.arshadshah.nimaz.domain.usecase.quran

import com.arshadshah.nimaz.domain.model.QuranTopic
import com.arshadshah.nimaz.domain.model.TopicTree
import javax.inject.Inject

/**
 * How many verses sit beneath each subject, its whole subtree included.
 *
 * The browser's rows carried each topic's **own** citation count, which for a branch is usually
 * zero: the Qur'an is not cited against "Doctrine", it is cited against the things under it. So
 * the home screen advertised 2,512 hand-indexed subjects and the tree opened on three bare words
 * reading "0 verses" — the most discouraging first screen in the app, and factually a lie about
 * how much was there.
 *
 * A fold over the whole hierarchy rather than a query per node: 2,512 queries to label one list
 * is the shape this exists to avoid. Computed **once per tree load** and cached by the
 * ViewModel — never per composition.
 *
 * The visited set is not defensive decoration. The parent columns are content, regenerated per
 * release by a separate repository, and a cycle in them would otherwise hang the subject browser
 * rather than mislabel it — a data fault taking the app down instead of one row's number being
 * wrong.
 */
class RollUpTopicCounts @Inject constructor() {

    operator fun invoke(topics: List<QuranTopic>, tree: TopicTree): Map<Int, Int> {
        if (topics.isEmpty()) return emptyMap()

        val byId = topics.associateBy { it.id }
        val childrenOf = topics
            .mapNotNull { topic -> topic.parentIn(tree)?.let { parent -> parent to topic } }
            .groupBy({ it.first }, { it.second })

        val totals = HashMap<Int, Int>(topics.size)

        fun total(topic: QuranTopic, ancestors: MutableSet<Int>): Int {
            totals[topic.id]?.let { return it }
            // A node already on the path back to the root closes a cycle. Count it once, at the
            // depth it was first met, and stop.
            if (!ancestors.add(topic.id)) return 0

            val subtotal = topic.ayahCount +
                childrenOf[topic.id].orEmpty().sumOf { total(it, ancestors) }

            ancestors.remove(topic.id)
            totals[topic.id] = subtotal
            return subtotal
        }

        topics.forEach { topic ->
            if (topic.id !in totals) total(topic, mutableSetOf())
        }
        // A parent naming a topic that is not in the list is a broken edge, not a node.
        return totals.filterKeys { it in byId }
    }
}
