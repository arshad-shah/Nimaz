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
 * juz header whenever the *opening* juz in [juzSpans] changes between adjacent rows, which keeps
 * the state a plain list a test can assert on and avoids inventing a second surah type for the
 * one screen — `Surah` already carries every other field the row shows.
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
     * Surah number → **every** juz it occupies, **in the active edition**.
     *
     * A span, not a single number, because most of the long surahs cross a juz boundary and a
     * few cross several: Al-Baqarah runs from juz 1 into juz 3. Filing each surah under the one
     * juz it *opens* in made the list claim that Al-Baqarah is juz 1 — and made juz 2 vanish
     * from the index entirely, since no surah begins in it. The row now names its range and a
     * `juz 2` query finds the surah that is printed there.
     *
     * Not `Surah.juzStart`, which no longer exists: the `surahs` table has no juz column, so the
     * mapper used to fill that field with a literal 1 for all 114 rows. The juz is a property of
     * the pagination, so it is resolved from the surah's page range here.
     */
    val juzSpans: Map<Int, IntRange> = emptyMap(),
    val pagination: MushafPagination = MushafPagination.fallback(MushafScript.DEFAULT),
    val error: UiError? = null,
)
