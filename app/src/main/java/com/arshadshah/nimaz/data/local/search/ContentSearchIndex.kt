package com.arshadshah.nimaz.data.local.search

import android.util.Log
import androidx.sqlite.db.SimpleSQLiteQuery
import com.arshadshah.nimaz.domain.search.ArabicSearchNormaliser
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
     * The keys of everything matching [query], for one [kind].
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
    ): List<String> = hits(query, kind, source, limit).map { it.ref }

    suspend fun hits(
        query: String,
        kind: SearchKind,
        source: String? = null,
        limit: Int = DEFAULT_LIMIT,
    ): List<SearchIndexRefRow> {
        if (status() !is Status.Ready) return emptyList()
        val expression = ArabicSearchNormaliser.matchExpression(query)
        // MATCH throws on an empty expression rather than matching nothing, and a query
        // of nothing but punctuation is a perfectly ordinary thing for a search box to
        // be holding halfway through being typed.
        if (expression.isEmpty()) return emptyList()

        val args = mutableListOf<Any>(expression)
        val filters = StringBuilder()
        args += kind.wire
        filters.append(" AND d.kind = ?")
        if (source != null) {
            args += source
            filters.append(" AND d.source = ?")
        }
        args += limit

        return runCatching {
            dao.rawRefs(
                SimpleSQLiteQuery(
                    // The subquery is not a style choice. Measured on the real artifact,
                    // the equivalent `JOIN search_index i ON d.docid = i.docid … MATCH`
                    // takes 595 ms where this takes 0.6 ms, for the same rows: the join
                    // lets SQLite drive from `search_docs` and ask the index about one
                    // docid at a time, which a contentless index cannot answer without
                    // walking it. Materialising the match list first is the only order
                    // that works here.
                    """
                    SELECT d.kind AS kind, d.ref AS ref, d.source AS source
                    FROM search_docs d
                    WHERE d.docid IN (
                        SELECT docid FROM search_index WHERE search_index MATCH ?
                    )$filters
                    LIMIT ?
                    """.trimIndent(),
                    args.toTypedArray(),
                )
            )
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
         * Bounded because the caller ranks in memory. 400 candidates per kind per term is
         * far more than any result list shows and still a single index walk.
         */
        const val DEFAULT_LIMIT = 400
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
    ASMA_UL_HUSNA("asma-ul-husna"),
    ASMA_UN_NABI("asma-un-nabi"),
    PROPHET("prophet"),
    EVENT("event"),
    TASBIH("tasbih"),
}
