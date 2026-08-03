package com.arshadshah.nimaz.domain.model

/**
 * The Qur'an's thematic layer — what a surah is about, what a passage is about, and which
 * verses speak to a subject.
 *
 * None of it is scripture. It is the apparatus a printed mushaf carries in its margins and an
 * introduction, and it arrives with the content artifact like everything else
 * (arshad-shah/nimaz-data, schemaVersion 24). A device that has not yet been handed a
 * schemaVersion 24 artifact has the tables and no rows, so every model here is reached through
 * a nullable or a possibly-empty list and no screen treats absence as an error.
 *
 * ## The HTML dialect
 *
 * [SurahOverviewSection.body] and [QuranTopic.description] carry markup, and it is deliberately
 * a *tiny* dialect, normalised at import so nothing has to be parsed generously here:
 *
 * ```
 * <p>…</p>  <strong>…</strong>  <em>…</em>
 * <a href="quran:2:153-251">…</a>   <a href="topic:61">…</a>
 * ```
 *
 * Four tags, and the two link schemes address screens this app has. `ThematicHtml` in the
 * presentation layer is the only thing that reads them.
 */

/** A surah's long-form background, and the sections the source divides it into. */
data class SurahOverview(
    val surahNumber: Int,
    val summary: String,
    val sections: List<SurahOverviewSection>,
)

/**
 * One section of that background.
 *
 * [heading] is the source's own wording — 65 spellings across 114 surahs — and is what the
 * reader sees. [group] is that heading folded onto a handful of stable buckets at import, and
 * is what the screen orders and icons by, so a new spelling of "Theme and Subject Matter" is
 * still a section that lands in the right place.
 */
data class SurahOverviewSection(
    val position: Int,
    val heading: String,
    val group: SurahOverviewGroup,
    val body: String,
)

enum class SurahOverviewGroup(val wire: String) {
    /** Why the surah is called what it is called. */
    NAME("name"),

    /** When it was revealed, and into what. */
    REVELATION("revelation"),

    /** What it is about — the longest section, and usually the one worth opening first. */
    THEME("theme"),

    /** The events around it. */
    BACKGROUND("background"),

    /** An editorial note that precedes the first heading. Two surahs have one: 113 and 114. */
    NOTE("note"),

    OTHER("other");

    companion object {
        fun fromWire(wire: String): SurahOverviewGroup =
            entries.firstOrNull { it.wire == wire } ?: OTHER
    }
}

/**
 * A run of consecutive ayahs the mushaf's own outline treats as one passage.
 *
 * 1,049 of them tile all 114 surahs — every verse but 2:134 and 40:61 falls inside exactly one,
 * which is what lets the reader answer "what is this passage about" for wherever it happens to
 * be, rather than only at a surah boundary.
 */
data class AyahTheme(
    val surahNumber: Int,
    val ayahFrom: Int,
    val ayahTo: Int,
    val theme: String,
    val ayahCount: Int,
) {
    val isSingleAyah: Boolean get() = ayahFrom == ayahTo

    /** "2:8–16", or "2:25" when the passage is one verse. */
    val reference: String
        get() = if (isSingleAyah) "$surahNumber:$ayahFrom" else "$surahNumber:$ayahFrom–$ayahTo"

    fun contains(ayahNumber: Int): Boolean = ayahNumber in ayahFrom..ayahTo
}

/**
 * A subject the Qur'an speaks about.
 *
 * The three parent ids are three *different* hierarchies, not one with fallbacks — see
 * [TopicTree]. A topic can sit in all three under three different parents, and collapsing them
 * would be choosing which of three editors was right.
 */
data class QuranTopic(
    val id: Int,
    val name: String,
    val arabicName: String,
    val description: String,
    val wikiLink: String,
    val ayahCount: Int,
    val parentId: Int?,
    val thematicParentId: Int?,
    val ontologyParentId: Int?,
    val isThematic: Boolean,
    val isOntology: Boolean,
    val relatedTopicIds: List<Int>,
) {
    val hasArabicName: Boolean get() = arabicName.isNotBlank()
    val hasDescription: Boolean get() = description.isNotBlank()

    fun parentIn(tree: TopicTree): Int? = when (tree) {
        TopicTree.THEMATIC -> thematicParentId
        TopicTree.ONTOLOGY -> ontologyParentId
        TopicTree.INDEX -> parentId
    }

    fun belongsTo(tree: TopicTree): Boolean = when (tree) {
        TopicTree.THEMATIC -> isThematic
        TopicTree.ONTOLOGY -> isOntology
        TopicTree.INDEX -> true
    }
}

/**
 * Which of the three hierarchies a topic browser is walking.
 *
 * [wire] is matched in SQL, so these strings are a contract with `QuranDao` and must not drift.
 */
enum class TopicTree(val wire: String) {
    /** Doctrine, Stories, The Unseen — the curated thematic outline. 695 topics. */
    THEMATIC("thematic"),

    /** Location, Living Creation, Event, … — kinds of thing. 284 topics. */
    ONTOLOGY("ontology"),

    /** The shape a printed concordance has: "Musa" > "parting of the Red Sea". */
    INDEX("index");

    companion object {
        val DEFAULT = THEMATIC

        /** Resolves a route argument. An unknown value opens the default tree, never nothing. */
        fun fromWire(wire: String?): TopicTree =
            entries.firstOrNull { it.wire == wire } ?: DEFAULT
    }
}

/** One verse cited under a topic. */
data class TopicCitation(
    val ayahId: Int,
    val surahNumber: Int,
    val ayahNumber: Int,
) {
    val reference: String get() = "$surahNumber:$ayahNumber"
}

/**
 * A topic with its citations resolved, and the neighbours a reader can move to from it.
 *
 * [breadcrumb] is the path from the tree's root down to (but not including) the topic itself,
 * so a topic five levels into the ontology says where it sits instead of appearing free-floating.
 */
data class TopicDetail(
    val topic: QuranTopic,
    val tree: TopicTree,
    val breadcrumb: List<QuranTopic>,
    val children: List<QuranTopic>,
    val related: List<QuranTopic>,
    val citations: List<TopicCitation>,
)
