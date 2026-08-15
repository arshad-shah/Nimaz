package com.arshadshah.nimaz.presentation.viewmodel.quran

import com.arshadshah.nimaz.domain.model.AyahTheme
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.model.SurahOverview
import com.arshadshah.nimaz.presentation.viewmodel.UiError

/**
 * One surah's background and its passage outline — the two screens that carry the long stuff.
 *
 * Deliberately *not* [QuranViewModel]. That one loads the surah list, the bookmarks, the
 * favourites, the active khatam, the verse of the day and the audio session on construction,
 * which is the right shape for the Qur'an home and the reader and a lot to spin up to draw a
 * page of prose. These two screens want one surah, one query, and the size the reader sets
 * its translation in.
 */
data class SurahBackgroundState(
    val surah: Surah? = null,
    val overview: SurahOverview? = null,

    /**
     * The size the reader draws its translation at.
     *
     * Reused rather than given a dial of its own: this is Qur'an-adjacent long-form prose, the
     * same thing the reader's translation is, and two settings for one act of reading is one
     * too many. The reader's Qur'an settings remain the single place it is changed.
     */
    val proseFontSize: Float = DEFAULT_PROSE_FONT_SIZE,
    val isLoading: Boolean = true,
    /** Set when the background fails to load, so the screen can say so rather than look empty. */
    val error: UiError? = null,
)

data class SurahPassagesState(
    val surah: Surah? = null,
    val passages: List<AyahTheme> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = true,
    /** Set when the passages fail to load. */
    val error: UiError? = null,
) {
    /**
     * The outline, narrowed by the filter.
     *
     * A query matches the passage's subject, and — because this is a table of contents and the
     * thing a reader most often has in hand is a verse number — a number that falls inside the
     * passage's range. "153" finds "Patience and prayer" without the reader knowing that is
     * where it starts.
     */
    val visiblePassages: List<AyahTheme>
        get() {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) return passages
            val ayah = trimmed.toIntOrNull()
            return passages.filter { passage ->
                passage.theme.contains(trimmed, ignoreCase = true) ||
                        (ayah != null && passage.contains(ayah))
            }
        }

    val isFiltered: Boolean get() = query.isNotBlank()
}

/** The reader's starting prose size; the state that defaults to it now owns it. */
private const val DEFAULT_PROSE_FONT_SIZE = 16f

/**
 * The Qur'an's thematic layer for one surah: its long-form background and its passage outline.
 *
 * [isAvailable] separates "this install has no thematic content" from "this surah has none".
 * Only the first is possible in practice — every surah has an overview and at least one passage
 * — but an install that upgraded before the schemaVersion 24 artifact arrived has neither, and
 * the screen says so instead of showing two empty sections.
 */
data class SurahThematicUiState(
    val overview: SurahOverview? = null,
    val passages: List<AyahTheme> = emptyList(),

    /**
     * How many subjects this surah's verses are cited under.
     *
     * A count and not the list: the info screen labels one row with it, and loading a few
     * hundred topics to render a single integer is what the counting query exists to avoid.
     */
    val subjectCount: Int = 0,

    val isLoading: Boolean = true,
) {
    val isAvailable: Boolean
        get() = overview != null || passages.isNotEmpty() || subjectCount > 0
}
