package com.arshadshah.nimaz.data.local.search

import android.util.Log
import androidx.sqlite.db.SimpleSQLiteQuery
import com.arshadshah.nimaz.domain.search.ArabicSearchNormaliser
import com.arshadshah.nimaz.domain.search.SearchMatch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The shipped search index, or the honest absence of one.
 *
 * Arabic search matched nothing at all before this (#330): the corpus is vocalised and
 * three quarters of it starts with ALEF WASLA, so no `LIKE '%…%'` could ever match a
 * typed query. The fix is a folded FTS index compiled into the artifact by
 * arshad-shah/nimaz-data, and this is the only thing in the app that reads it.
 *
 * ## Why availability is a question at all
 *
 * `createFromAsset` copies the artifact **once**, on first install. An existing install
 * keeps the file it already has and moves forward through Room migrations and content
 * patches, neither of which can add a table. So a phone that installed before the index
 * shipped will not have one, and asking it to build one — 152,551 documents — is the
 * on-device work this design exists to avoid.
 *
 * Callers therefore ask [isAvailable] and fall back to the `LIKE` queries when it is
 * false. That is not a worse search than before; it is exactly the search from before,
 * which for Latin scripts worked. Fresh installs get the real thing.
 *
 * ## Why the fold version is checked
 *
 * The index stores each body folded by the data repository's `arabic.py`, and
 * [ArabicSearchNormaliser] folds the query. If those two disagree the index is present,
 * non-empty, correctly shaped — and matches nothing, which is indistinguishable from
 * "no results" from the outside. The artifact stamps `search_meta.fold_version`; a
 * mismatch here means the app is newer or older than the artifact it is holding, and
 * refusing the index is the only outcome that does not lie to the user.
 */
@Singleton
class ContentSearchIndex @Inject constructor(
    private val dao: SearchIndexDao,
) {

    sealed interface Status {
        /** Present, and folded the way this app folds. */
        data class Ready(val documents: Int, val flavour: String) : Status

        /** No index in this artifact — an install that predates it. */
        data object Absent : Status

        /** Present, but built by a different folding than this app speaks. */
        data class Mismatched(val stamped: String?) : Status
    }

    private val mutex = Mutex()

    @Volatile
    private var cached: Status? = null

    /** Probed once per process; the answer is a property of the file on disk. */
    suspend fun status(): Status = cached ?: mutex.withLock {
        cached ?: probe().also { cached = it }
    }

    suspend fun isAvailable(): Boolean = status() is Status.Ready

    /**
     * The keys of everything matching [query], for one [kind], **best match first**.
     *
     * Returns an empty list when there is no index, when the query has no searchable
     * content, or when nothing matched — the caller cannot distinguish those and must
     * not need to, which is why [isAvailable] is asked separately before falling back.
     */
    suspend fun refs(
        query: String,
        kind: SearchKind,
        source: String? = null,
        limit: Int = DEFAULT_LIMIT,
        match: SearchMatch = SearchMatch.AUTO,
    ): List<String> = hits(query, kind, source, limit, match).map { it.ref }

    /**
     * Every match for [query] in one [kind], ranked, and the top [limit] of them.
     *
     * ## Why rank here
     *
     * This used to return the first [limit] matches in **index order** — book order. For a
     * common word that was the whole problem: "prayer" has 4,817 hadith hits, the first 400
     * are all Bukhari, and the other five books never reached the screen however relevant
     * they were. Everything above this ranked those 400 by how many query words they held,
     * which for a one-word search is a tie, so the list *was* book order.
     *
     * Now every match is scored first and capped after. The score is TF-IDF from FTS4's
     * `matchinfo(…, 'pcx')`: for each phrase in the expression, how often it occurs in
     * this document (dampened, so ten mentions are not ten times one) times how rare it is
     * across the whole index. A verse that says "patience" three times outranks one that
     * says it once, and a match on a rare word outranks a match on a common one. The
     * artifact is built with `matchinfo=fts3`, which keeps no document lengths, so this is
     * not BM25 — a long hadith is not penalised for its length. That would need the
     * nimaz-data build to drop the option, and is the obvious next step.
     *
     * ## Why two queries, and why this shape
     *
     * The first query asks the FTS table directly, filtering by kind through
     * `docid IN (SELECT … FROM search_docs …)`. Measured on the data-v10 artifact, that
     * costs 3–47 ms for the common-word worst case on a desktop, against 2–18 ms for the old
     * capped query. A `JOIN` on `search_docs` instead is the shape to avoid: SQLite drives
     * it from the documents side and asks the contentless index about one docid at a time,
     * which took 1.8 s for "patience" in hadith — 76 hits — in the same measurement.
     * The second query looks the top [limit]
     * docids up in `search_docs` by primary key.
     *
     * [MAX_CANDIDATES] bounds the first query for words so common that ranking them is
     * not worth the time; past it, the extra matches are dropped in index order, as before.
     */
    suspend fun hits(
        query: String,
        kind: SearchKind,
        source: String? = null,
        limit: Int = DEFAULT_LIMIT,
        match: SearchMatch = SearchMatch.AUTO,
    ): List<SearchIndexRefRow> {
        val ready = status() as? Status.Ready ?: return emptyList()
        val expression = ArabicSearchNormaliser.matchExpression(query, match)
        // MATCH throws on an empty expression rather than matching nothing, and a query
        // of nothing but punctuation is a perfectly ordinary thing for a search box to
        // be holding halfway through being typed.
        if (expression.isEmpty()) return emptyList()

        val args = mutableListOf<Any>(expression)
        val filters = StringBuilder()
        args += kind.wire
        filters.append("d.kind = ?")
        if (source != null) {
            args += source
            filters.append(" AND d.source = ?")
        }
        args += MAX_CANDIDATES

        return runCatching {
            val matches = dao.rawMatches(
                SimpleSQLiteQuery(
                    """
                    SELECT docid AS docid, matchinfo(search_index, 'pcx') AS info
                    FROM search_index
                    WHERE search_index MATCH ?
                    AND docid IN (SELECT d.docid FROM search_docs d WHERE $filters)
                    LIMIT ?
                    """.trimIndent(),
                    args.toTypedArray(),
                )
            )
            if (matches.isEmpty()) return@runCatching emptyList<SearchIndexRefRow>()

            val documents = ready.documents.coerceAtLeast(1)
            val top = matches
                .map { it.docid to MatchScore.of(it.info, documents) }
                // Ties by docid, so the order is stable from one search to the next.
                .sortedWith(compareByDescending<Pair<Long, Double>> { it.second }.thenBy { it.first })
                .take(limit)
                .map { it.first }

            val placeholders = top.joinToString(",") { "?" }
            val byDocid = dao.rawDocs(
                SimpleSQLiteQuery(
                    "SELECT docid AS docid, kind AS kind, ref AS ref, source AS source " +
                        "FROM search_docs WHERE docid IN ($placeholders)",
                    top.toTypedArray(),
                )
            ).associateBy { it.docid }
            top.mapNotNull { docid ->
                byDocid[docid]?.let { SearchIndexRefRow(kind = it.kind, ref = it.ref, source = it.source) }
            }
        }.getOrElse { error ->
            // A malformed MATCH expression is the realistic failure — a user typing a
            // quote, say — and it must not take the search screen down with it.
            Log.w(TAG, "search index query failed for kind=${kind.wire}", error)
            emptyList()
        }
    }

    private suspend fun probe(): Status = runCatching {
        val meta = dao.rawMeta(SimpleSQLiteQuery("SELECT key, value FROM search_meta"))
            .associate { it.key to it.value }
        val stamped = meta["fold_version"]
        if (stamped != ArabicSearchNormaliser.FOLD_VERSION.toString()) {
            Log.w(
                TAG,
                "search index folds at version $stamped, this app folds at " +
                    "${ArabicSearchNormaliser.FOLD_VERSION} — not using it",
            )
            return@runCatching Status.Mismatched(stamped)
        }
        Status.Ready(
            documents = meta["documents"]?.toIntOrNull() ?: 0,
            flavour = meta["flavour"].orEmpty(),
        )
    }.getOrElse {
        // "no such table: search_meta" on every install made before the index shipped.
        // Expected, not exceptional.
        Status.Absent
    }

    companion object {
        private const val TAG = "ContentSearchIndex"

        /**
         * How many ranked hits a caller gets. The caller merges several passes in memory, and
         * 400 per kind per pass is far more than any result list shows.
         */
        const val DEFAULT_LIMIT = 400

        /**
         * How many matches are scored before the best [DEFAULT_LIMIT] are kept. Every word
         * but the very commonest stays under it ("prayer" in hadith: 4,817; "الله" in the
         * Qur'an: 3,146); "allah" in the translations (16,626) does not, and is too common
         * to rank usefully on its own — the phrase and every-word passes do that work.
         */
        const val MAX_CANDIDATES = 6000
    }
}

/**
 * The kinds the artifact indexes under. These strings are a contract with
 * arshad-shah/nimaz-data — they are the `searchable.kind` values in `collection.yaml` —
 * so they are spelled once, here.
 */
enum class SearchKind(val wire: String) {
    QURAN("quran"),
    TRANSLATION("translation"),
    SURAH("surah"),
    HADITH("hadith"),
    HADITH_BOOK("hadith-book"),
    DUA("dua"),
    DUA_CATEGORY("dua-category"),
    TAFSEER("tafseer"),

    /**
     * The Qur'an's thematic layer (nimaz-data schemaVersion 24). A `theme` ref is
     * `surah:ayah_from` — the passage's key — and a `topic` ref is the topic id.
     */
    THEME("theme"),
    TOPIC("topic"),
    ASMA_UL_HUSNA("asma-ul-husna"),
    ASMA_UN_NABI("asma-un-nabi"),
    PROPHET("prophet"),
    EVENT("event"),
    TASBIH("tasbih"),
}
