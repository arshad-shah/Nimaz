package com.arshadshah.nimaz.presentation.viewmodel.quran

import com.arshadshah.nimaz.domain.model.QuranTopic
import com.arshadshah.nimaz.domain.model.TopicTree
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranTopicsEvent.RebaseTo.Companion.ROOT

sealed interface QuranTopicsEvent {
    /** The browser is on screen and wants its current level. Idempotent — safe to re-send. */
    data object OpenBrowser : QuranTopicsEvent

    data class SelectTree(val tree: TopicTree) : QuranTopicsEvent

    /** Open or close a node's children in place. */
    data class Toggle(val topic: QuranTopic) : QuranTopicsEvent

    /** Re-root the tree on [topic], pushing it onto the crumb bar. */
    data class Focus(val topic: QuranTopic) : QuranTopicsEvent

    /**
     * Re-root at a crumb. [index] is a position in [TopicBrowseState.focus];
     * [ROOT] goes back to the whole tree.
     */
    data class RebaseTo(val index: Int) : QuranTopicsEvent {
        companion object {
            const val ROOT = -1
        }
    }

    /**
     * What the system back gesture does: close the innermost open node, or — with nothing open
     * — step back out of one focus. The screen pops only when neither is left.
     */
    data object Back : QuranTopicsEvent

    data class Search(val query: String) : QuranTopicsEvent
    data object ClearSearch : QuranTopicsEvent

    /** The subjects one surah speaks about. Idempotent — safe to re-send for the same surah. */
    data class LoadSurahSubjects(val surahNumber: Int) : QuranTopicsEvent

    /** Filter the loaded surah subjects. In memory; no query is run. */
    data class FilterSurahSubjects(val query: String) : QuranTopicsEvent

    data object ClearSurahSubjectsFilter : QuranTopicsEvent

    /**
     * One subject's detail. [fromSurah] is the surah the reader came from, whose citations are
     * pinned to the top — null when they came from somewhere with no surah in hand.
     */
    data class LoadDetail(
        val topicId: Int,
        val tree: TopicTree,
        val fromSurah: Int? = null,
    ) : QuranTopicsEvent
}
