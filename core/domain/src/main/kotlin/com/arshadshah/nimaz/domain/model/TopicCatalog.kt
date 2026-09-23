package com.arshadshah.nimaz.domain.model

/**
 * The whole subject index in memory: every topic, the verses each one is cited against, and the
 * three hierarchies built over them.
 *
 * It exists because the browser's cards ask questions no single row can answer — "how many
 * verses sit beneath Stories", "which of Doctrine's branches is biggest", "where does
 * *patience during adversity* sit" — and answering them per row was either a query per node or
 * a wrong number. 2,512 topics and 30,687 citations fit comfortably in memory, so the catalogue
 * is built once per screen and every one of those questions becomes a lookup.
 *
 * ## Counts are distinct verses
 *
 * [verseCount] is the size of the *set* of verses beneath a subject, not the sum of its
 * children's counts. A verse cited under both "Prayer" and "Charity" is one verse of Worship, and
 * the sum reported it twice — "Relationship with Allah" read 1,261 when 1,041 verses speak to
 * it. The detail screen already listed the distinct set, so the card and the screen it opened
 * disagreed about the same subject.
 *
 * ## Broken content costs a wrong number, not the app
 *
 * The parent columns are content, regenerated per release by a separate repository, so a cycle
 * or a dangling parent is a shipping possibility. Every walk here carries a visited set: a cycle
 * is cut where it closes and a parent that is not in the catalogue ends the walk.
 */
class TopicCatalog(
    topics: List<QuranTopic>,
    ayahIdsByTopic: Map<Int, List<Int>>,
) {
    private val byId: Map<Int, QuranTopic> = topics.associateBy { it.id }
    private val ownAyahs: Map<Int, List<Int>> = ayahIdsByTopic

    private val childIds: Map<TopicTree, Map<Int, List<Int>>> = TopicTree.entries.associateWith { tree ->
        topics.mapNotNull { topic ->
            topic.parentIn(tree)
                ?.takeIf { it != topic.id && it in byId }
                ?.let { parent -> parent to topic.id }
        }.groupBy({ it.first }, { it.second })
    }

    private val verseSets: Map<TopicTree, HashMap<Int, Set<Int>>> =
        TopicTree.entries.associateWith { HashMap() }

    val isEmpty: Boolean get() = byId.isEmpty()

    fun topic(id: Int): QuranTopic? = byId[id]

    /**
     * The top of [tree]: topics that belong to it and have no parent in it.
     *
     * A topic whose parent is missing from the catalogue is a root too — a broken edge should
     * surface the subject at the top rather than lose it altogether.
     */
    fun roots(tree: TopicTree): List<QuranTopic> = byId.values
        .filter { it.belongsTo(tree) && it.parentIn(tree).let { p -> p == null || p !in byId || p == it.id } }
        .sortedWith(biggestFirst(tree))

    /** [id]'s children in [tree], the most-cited first and then alphabetically. */
    fun children(id: Int, tree: TopicTree): List<QuranTopic> =
        childIds.getValue(tree)[id].orEmpty()
            .mapNotNull { byId[it] }
            .sortedWith(biggestFirst(tree))

    fun childCount(id: Int, tree: TopicTree): Int = childIds.getValue(tree)[id].orEmpty().size

    /** How many distinct verses are cited under [id] or anything beneath it in [tree]. */
    fun verseCount(id: Int, tree: TopicTree): Int = verses(id, tree).size

    /** [id]'s ancestors in [tree], root-first, excluding [id] itself. */
    fun path(id: Int, tree: TopicTree): List<QuranTopic> {
        val trail = ArrayDeque<QuranTopic>()
        val seen = mutableSetOf(id)
        var parentId = byId[id]?.parentIn(tree)
        while (parentId != null && seen.add(parentId)) {
            val parent = byId[parentId] ?: break
            trail.addFirst(parent)
            parentId = parent.parentIn(tree)
        }
        return trail.toList()
    }

    /**
     * Subjects whose English or Arabic name contains [query], best first.
     *
     * Names that *start* with the query outrank ones that merely contain it — "Patience" before
     * "Impatience" — and then the more-cited subject wins, each counted in the hierarchy it
     * would open in. In memory, because the catalogue is already here and a round trip to
     * SQLite per settled keystroke would buy nothing but latency.
     */
    fun search(query: String, limit: Int = SEARCH_LIMIT): List<QuranTopic> {
        val needle = query.trim()
        if (needle.isEmpty()) return emptyList()
        return byId.values
            .filter { it.name.contains(needle, ignoreCase = true) || it.arabicName.contains(needle) }
            .sortedWith(
                compareBy<QuranTopic> { if (it.name.startsWith(needle, ignoreCase = true)) 0 else 1 }
                    .thenByDescending { verseCount(it.id, it.homeTree) }
                    .thenBy { it.name.lowercase() }
            )
            .take(limit)
    }

    private fun biggestFirst(tree: TopicTree): Comparator<QuranTopic> =
        compareByDescending<QuranTopic> { verseCount(it.id, tree) }.thenBy { it.name.lowercase() }

    private fun verses(id: Int, tree: TopicTree): Set<Int> {
        val memo = verseSets.getValue(tree)
        memo[id]?.let { return it }
        return collect(id, tree, memo, mutableSetOf())
    }

    private fun collect(
        id: Int,
        tree: TopicTree,
        memo: HashMap<Int, Set<Int>>,
        onPath: MutableSet<Int>,
    ): Set<Int> {
        memo[id]?.let { return it }
        // Already on the way down to here: this edge closes a cycle. Contribute nothing.
        if (!onPath.add(id)) return emptySet()
        val children = childIds.getValue(tree)[id].orEmpty()
        val result: Set<Int> = if (children.isEmpty()) {
            ownAyahs[id].orEmpty().toHashSet()
        } else {
            HashSet<Int>(ownAyahs[id].orEmpty()).apply {
                children.forEach { addAll(collect(it, tree, memo, onPath)) }
            }
        }
        onPath.remove(id)
        memo[id] = result
        return result
    }

    companion object {
        const val SEARCH_LIMIT = 60
    }
}
