package com.arshadshah.nimaz.presentation.viewmodel.quran

import com.arshadshah.nimaz.domain.model.TopicTree

sealed interface QuranTopicsEvent {
    /** The browser is on screen and wants its catalogue. Idempotent — safe to re-send. */
    data object OpenBrowser : QuranTopicsEvent

    /** Show another hierarchy. Every tab is counted at load, so this is a pure state change. */
    data class SelectTree(val tree: TopicTree) : QuranTopicsEvent

    /** How the index is listed: alphabetically, or by how much of the Qur'an each entry has. */
    data class SetIndexSort(val sort: TopicIndexSort) : QuranTopicsEvent

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
