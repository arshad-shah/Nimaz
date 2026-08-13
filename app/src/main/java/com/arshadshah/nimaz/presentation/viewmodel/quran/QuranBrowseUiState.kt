package com.arshadshah.nimaz.presentation.viewmodel.quran

import com.arshadshah.nimaz.domain.model.MushafPagination
import com.arshadshah.nimaz.domain.model.MushafScript
import com.arshadshah.nimaz.domain.model.QuranSearchQuery
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.presentation.viewmodel.UiError

/**
 * The merged browse surface: every surah, in order, under juz headers.
 *
 * [rows] is deliberately a flat `List<Surah>` rather than a list of groups. The screen prints a
 * juz header whenever [juzBySurah] changes between adjacent rows, which keeps the state a plain
 * list a test can assert on and avoids inventing a second surah type for the one screen —
 * `Surah` already carries every other field the row shows.
 */
data class QuranBrowseUiState(
    val query: String = "",
    val isLoading: Boolean = true,
    val rows: List<Surah> = emptyList(),
    /**
     * What the query resolved to when it named a *place* rather than filtered the list —
     * `juz 15`, `page 299`, a surah number. Drives the jump-to card above the list. Null for a
     * name search, which the list itself answers.
     */
    val jumpTarget: QuranSearchQuery? = null,
    /**
     * The page each surah opens on **in the active edition**. `Surah.startPage` is the Madani
     * column, so under a line-accurate edition it names a page that surah does not start on.
     */
    val startPages: Map<Int, Int> = emptyMap(),
    /**
     * Surah number → the juz it opens in, **in the active edition**.
     *
     * Not `Surah.juzStart`, which no longer exists: the `surahs` table has no juz column, so the
     * mapper used to fill that field with a literal 1 for all 114 rows. The juz is a property of
     * the pagination, so it is resolved from the opening page here.
     */
    val juzBySurah: Map<Int, Int> = emptyMap(),
    val pagination: MushafPagination = MushafPagination.fallback(MushafScript.DEFAULT),
    val error: UiError? = null,
)
