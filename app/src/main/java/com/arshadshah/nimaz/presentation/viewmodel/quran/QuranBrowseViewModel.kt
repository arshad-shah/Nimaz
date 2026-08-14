package com.arshadshah.nimaz.presentation.viewmodel.quran

import androidx.lifecycle.ViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.core.monitoring.catchAndReport
import com.arshadshah.nimaz.core.monitoring.launchSafely
import com.arshadshah.nimaz.domain.model.MushafPagination
import com.arshadshah.nimaz.domain.model.MushafScript
import com.arshadshah.nimaz.domain.model.QuranSearchQuery
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.repository.settings.QuranPreferences
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import com.arshadshah.nimaz.presentation.viewmodel.UiError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Browse: one list of every surah in juz order, and one field that also answers "juz 15" and
 * "page 299".
 *
 * The three tabs this replaces — Surah, Juz, Page — were three indexes of the same book, and a
 * reader had to decide which one their question belonged to before they could ask it. The
 * question is always "where is this"; [QuranSearchQuery] works out which kind of answer it is.
 */
@HiltViewModel
class QuranBrowseViewModel @Inject constructor(
    private val quranUseCases: QuranUseCases,
    private val quranSettings: QuranPreferences,
    private val telemetry: Telemetry,
) : ViewModel() {

    private val _state = MutableStateFlow(QuranBrowseUiState())
    val state: StateFlow<QuranBrowseUiState> = _state.asStateFlow()

    /** Every surah, unfiltered, so a cleared query does not re-query the database. */
    private var allSurahs: List<Surah> = emptyList()

    init {
        observeSurahs()
        observePagination()
    }

    fun onEvent(event: QuranBrowseEvent) {
        when (event) {
            is QuranBrowseEvent.QueryChanged -> applyQuery(event.text)
            QuranBrowseEvent.ClearQuery -> applyQuery("")
        }
    }

    private fun observeSurahs() {
        launchSafely(telemetry, AppAnalytics.Feature.QURAN, "browse_load_surahs") {
            quranUseCases.getSurahList()
                .catchAndReport(telemetry, AppAnalytics.Feature.QURAN, "browse_surahs") { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = UiError(
                                message = R.string.quran_browse_load_failed,
                                details = error.message,
                            ),
                        )
                    }
                }
                .collect { surahs ->
                    allSurahs = surahs.sortedBy { it.number }
                    _state.update { it.recompute(allSurahs, it.query) }
                }
        }
    }

    /**
     * The active edition's page mapping, re-derived when the script preference changes, so the
     * page on each row is the page that surah opens on in the mushaf the reader is reading.
     */
    private fun observePagination() {
        launchSafely(telemetry, AppAnalytics.Feature.QURAN, "browse_pagination") {
            quranSettings.quranMushafScript
                .map { MushafScript.fromName(it) }
                .distinctUntilChanged()
                .map { quranUseCases.getMushafPagination(it) }
                .collect { pagination ->
                    _state.update {
                        it.copy(pagination = pagination).recompute(allSurahs, it.query)
                    }
                }
        }
    }

    private fun applyQuery(raw: String) {
        _state.update { it.recompute(allSurahs, raw) }
        if (raw.isNotBlank()) {
            telemetry.search(AppAnalytics.Feature.QURAN, raw.trim().length)
        }
    }

    /**
     * Re-derives everything the query and the edition decide together: the visible rows, the
     * jump card, and the page each surah opens on.
     *
     * One function because the three genuinely move together — a script change re-pages every
     * row, and a page query is answered *from* those pages — and splitting them is how the two
     * halves fall out of step.
     */
    private fun QuranBrowseUiState.recompute(
        surahs: List<Surah>,
        raw: String,
    ): QuranBrowseUiState {
        val pages = startPages(surahs, pagination)
        val juz = juzSpans(surahs, pages, pagination)
        val parsed = QuranSearchQuery.parse(raw)
        return copy(
            query = raw,
            isLoading = false,
            error = null,
            startPages = pages,
            juzSpans = juz,
            rows = filterRows(surahs, parsed, pages, juz),
            // A name search is answered by the list itself; only a query naming a place gets
            // a jump card, and an empty field names nothing.
            jumpTarget = parsed.takeIf {
                it !is QuranSearchQuery.Empty && it !is QuranSearchQuery.Name
            },
        )
    }

    /**
     * What the list shows for a query.
     *
     * A juz query narrows to every surah *printed in* that juz, not only the ones that open in
     * it — `juz 2` is entirely inside Al-Baqarah, so the start-only version answered it with an
     * empty list. A page query narrows to the surah printed on that page, which is more useful
     * than an empty list beneath a jump card. A name matches the English name, the
     * transliteration and the Arabic, because a reader typing "Kahf" and a reader typing
     * "الكهف" are asking the same thing.
     */
    private fun QuranBrowseUiState.filterRows(
        surahs: List<Surah>,
        query: QuranSearchQuery,
        pages: Map<Int, Int>,
        juz: Map<Int, IntRange>,
    ): List<Surah> = when (query) {
        QuranSearchQuery.Empty -> surahs
        is QuranSearchQuery.Juz -> surahs.filter { juz[it.number]?.contains(query.number) == true }
        is QuranSearchQuery.SurahNumber -> surahs.filter { it.number == query.number }
        is QuranSearchQuery.Page -> {
            val spans = pageSpans(surahs, pages, pagination)
            surahs.filter { query.number in (spans[it.number] ?: IntRange.EMPTY) }
        }

        is QuranSearchQuery.Name -> surahs.filter { surah ->
            surah.nameEnglish.contains(query.text, ignoreCase = true) ||
                surah.nameTransliteration.contains(query.text, ignoreCase = true) ||
                surah.nameArabic.contains(query.text)
        }
    }

    /**
     * Surah number → the page it opens on in [pagination].
     *
     * Resolved from the surah's first ayah rather than read off `Surah.startPage`, which is the
     * Madani column and names the wrong page under any other edition (#325). The cumulative
     * ayah count is how the ayah id of a surah's first verse is known without a second query.
     */
    private fun startPages(surahs: List<Surah>, pagination: MushafPagination): Map<Int, Int> {
        if (surahs.isEmpty()) return emptyMap()
        var firstAyahId = 1
        return surahs.associate { surah ->
            val page = pagination.pageForAyah(firstAyahId) ?: surah.startPage
            firstAyahId += surah.ayahCount
            surah.number to page
        }
    }

    /**
     * Surah number → the pages it is printed on, first..last.
     *
     * A surah's last page is the page before the *next surah in this list* opens — by position,
     * not by `number + 1`, so the span is right whatever subset of surahs it is given. An-Nas
     * runs to the edition's last page. `coerceAtLeast` because several of the short surahs share
     * an opening page with their neighbour, which would otherwise give a backwards range.
     */
    private fun pageSpans(
        surahs: List<Surah>,
        pages: Map<Int, Int>,
        pagination: MushafPagination,
    ): Map<Int, IntRange> = surahs.mapIndexed { index, surah ->
        val start = pages[surah.number] ?: surah.startPage
        val nextStart = surahs.getOrNull(index + 1)
            ?.let { pages[it.number] ?: it.startPage }
        val end = (nextStart?.let { it - 1 } ?: pagination.totalPages).coerceAtLeast(start)
        surah.number to start..end
    }.toMap()

    /**
     * Surah number → the juz it opens in .. the juz it ends in.
     *
     * Derived from [pageSpans], so it moves with the edition exactly the way the page on each
     * row does — no second pass over the ayah table.
     */
    private fun juzSpans(
        surahs: List<Surah>,
        pages: Map<Int, Int>,
        pagination: MushafPagination,
    ): Map<Int, IntRange> = pageSpans(surahs, pages, pagination).mapValues { (_, span) ->
        val firstJuz = pagination.juzForPage(span.first)
        firstJuz..pagination.juzForPage(span.last).coerceAtLeast(firstJuz)
    }
}
