package com.arshadshah.nimaz.data.local.search

import androidx.sqlite.db.SimpleSQLiteQuery
import com.arshadshah.nimaz.domain.search.ArabicSearchNormaliser
import com.arshadshah.nimaz.domain.search.SearchMatch
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * What the index is actually asked, once it has said it is Ready — and what comes back.
 *
 * [ContentSearchIndex.hits] assembles a raw SQL string and a parallel argument list by hand,
 * because the `source` filter is optional and `SimpleSQLiteQuery` has no way to express that.
 * Hand-built pairs drift: an argument appended without its placeholder — or the other way round
 * — is `IllegalArgumentException` at bind time on the one code path that takes the filter, and
 * a filter bound in the wrong order silently searches for the right words in the wrong kind.
 * Neither shows up in a `Ready` status, so both are asserted on the query itself.
 *
 * Since the ranking change, `hits` asks twice: every match with its `matchinfo` blob, then the
 * keys of the best few. The ranking is asserted here too — it is the whole point of the change,
 * and "returns the right rows in the wrong order" passes every other test.
 *
 * The ways out that return an empty list without touching the database matter for the same
 * reason they always did: a search box holding one punctuation mark, and an install with no
 * index at all, must not reach a `MATCH` at all — `MATCH ''` throws.
 */
class ContentSearchIndexQueryTest {

    /** Records what the index asked for, and answers with whatever it was seeded with. */
    private class CapturingDao(
        private val meta: List<SearchIndexMetaRow>?,
        private val matches: List<SearchIndexMatchRow> = emptyList(),
        private val docs: List<SearchIndexDocRow> = emptyList(),
        private val failMatches: Boolean = false,
    ) : SearchIndexDao {
        var lastMatchQuery: SimpleSQLiteQuery? = null
        var lastDocQuery: SimpleSQLiteQuery? = null
        var matchQueries = 0

        override suspend fun rawMatches(query: SimpleSQLiteQuery): List<SearchIndexMatchRow> {
            matchQueries++
            lastMatchQuery = query
            if (failMatches) {
                throw android.database.sqlite.SQLiteException("malformed MATCH expression")
            }
            return matches
        }

        override suspend fun rawDocs(query: SimpleSQLiteQuery): List<SearchIndexDocRow> {
            lastDocQuery = query
            val wanted = bound(query).toSet()
            return docs.filter { it.docid in wanted }
        }

        override suspend fun rawMeta(query: SimpleSQLiteQuery): List<SearchIndexMetaRow> =
            meta ?: throw android.database.sqlite.SQLiteException("no such table: search_meta")

        /** The bound arguments of the match query, in the order the index appended them. */
        fun boundArgs(): List<Any?> = bound(lastMatchQuery!!)

        private fun bound(query: SimpleSQLiteQuery): List<Any?> {
            val binder = RecordingBinder()
            query.bindTo(binder)
            return binder.values
        }
    }

    private fun readyMeta() = listOf(
        SearchIndexMetaRow("fold_version", ArabicSearchNormaliser.FOLD_VERSION.toString()),
        SearchIndexMetaRow("documents", "150000"),
        SearchIndexMetaRow("flavour", "fts4"),
    )

    /**
     * A `matchinfo(…, 'pcx')` blob for a one-column index: per phrase, hits in this row, hits
     * in all rows, rows with a hit.
     */
    private fun info(vararg phrases: Triple<Int, Int, Int>): ByteArray {
        val buffer = ByteBuffer.allocate(4 * (2 + phrases.size * 3)).order(ByteOrder.nativeOrder())
        buffer.putInt(phrases.size).putInt(1)
        phrases.forEach { (here, all, rows) -> buffer.putInt(here).putInt(all).putInt(rows) }
        return buffer.array()
    }

    private fun match(docid: Long, vararg phrases: Triple<Int, Int, Int>) =
        SearchIndexMatchRow(docid, info(*phrases))

    private fun doc(docid: Long, ref: String, kind: String = "quran", source: String? = null) =
        SearchIndexDocRow(docid = docid, kind = kind, ref = ref, source = source)

    // ---- The query that is built ----

    @Test
    fun `a search binds the folded expression, the kind and the candidate cap, in that order`() =
        runTest {
            val dao = CapturingDao(readyMeta())
            val index = ContentSearchIndex(dao)

            index.hits("light", kind = SearchKind.QURAN, limit = 25)

            assertThat(dao.boundArgs())
                .containsExactly(
                    ArabicSearchNormaliser.matchExpression("light"),
                    "quran",
                    ContentSearchIndex.MAX_CANDIDATES.toLong(),
                )
                .inOrder()
            assertThat(dao.lastMatchQuery!!.sql).contains("d.kind = ?")
            assertThat(dao.lastMatchQuery!!.sql).doesNotContain("d.source = ?")
            assertThat(dao.lastMatchQuery!!.sql).contains("matchinfo(search_index, 'pcx')")
        }

    @Test
    fun `the kind filter is a subquery, never a join`() = runTest {
        // Measured on the data-v10 artifact: the IN form costs milliseconds, a JOIN on
        // search_docs 1.8 s for a word with 76 hits, because SQLite drives a join from the
        // documents side and asks the contentless index about one docid at a time.
        val dao = CapturingDao(readyMeta())
        ContentSearchIndex(dao).hits("light", kind = SearchKind.QURAN)

        val sql = dao.lastMatchQuery!!.sql
        assertThat(sql).contains("docid IN (SELECT d.docid FROM search_docs d WHERE")
        assertThat(sql.uppercase()).doesNotContain("JOIN")
    }

    @Test
    fun `a source filter adds both its placeholder and its argument`() = runTest {
        val dao = CapturingDao(readyMeta())
        val index = ContentSearchIndex(dao)

        index.hits("prayer", kind = SearchKind.HADITH, source = "bukhari", limit = 10)

        // Placeholder and argument have to arrive together and in step — the cap is bound
        // last, after whichever filters were appended.
        assertThat(dao.lastMatchQuery!!.sql).contains("d.source = ?")
        assertThat(dao.boundArgs())
            .containsExactly(
                ArabicSearchNormaliser.matchExpression("prayer"),
                "hadith",
                "bukhari",
                ContentSearchIndex.MAX_CANDIDATES.toLong(),
            )
            .inOrder()
    }

    @Test
    fun `the match mode reaches the expression`() = runTest {
        val dao = CapturingDao(readyMeta())
        ContentSearchIndex(dao).hits(
            "patience hardship",
            kind = SearchKind.HADITH,
            match = SearchMatch.ALL_WORDS,
        )

        assertThat(dao.boundArgs().first()).isEqualTo(
            ArabicSearchNormaliser.matchExpression("patience hardship", SearchMatch.ALL_WORDS)
        )
    }

    @Test
    fun `the kind bound is the wire name the artifact indexed under`() = runTest {
        val dao = CapturingDao(readyMeta())
        val index = ContentSearchIndex(dao)

        // The enum name and the wire name differ for every hyphenated kind; the artifact only
        // knows the latter.
        index.hits("x", kind = SearchKind.HADITH_BOOK)

        assertThat(dao.boundArgs()).contains("hadith-book")
    }

    // ---- The ranking ----

    @Test
    fun `matches come back best first, not in index order`() = runTest {
        val dao = CapturingDao(
            readyMeta(),
            // Index order: 1, 2, 3. Doc 3 mentions the word most, doc 1 least.
            matches = listOf(
                match(1, Triple(1, 10, 5)),
                match(2, Triple(2, 10, 5)),
                match(3, Triple(4, 10, 5)),
            ),
            docs = listOf(doc(1, "2:255"), doc(2, "24:35"), doc(3, "112:1")),
        )

        assertThat(ContentSearchIndex(dao).refs("light", kind = SearchKind.QURAN))
            .containsExactly("112:1", "24:35", "2:255").inOrder()
    }

    @Test
    fun `a hit on a rare word outranks a hit on a common one`() = runTest {
        // Two phrases (an OR-ed expression): one in 5 rows of the index, one in 50,000.
        val dao = CapturingDao(
            readyMeta(),
            matches = listOf(
                match(1, Triple(0, 10, 5), Triple(1, 90_000, 50_000)),
                match(2, Triple(1, 10, 5), Triple(0, 90_000, 50_000)),
            ),
            docs = listOf(doc(1, "common"), doc(2, "rare")),
        )

        assertThat(ContentSearchIndex(dao).refs("x", kind = SearchKind.QURAN))
            .containsExactly("rare", "common").inOrder()
    }

    @Test
    fun `the limit keeps the best, and only they are looked up`() = runTest {
        val dao = CapturingDao(
            readyMeta(),
            matches = (1L..10L).map { match(it, Triple(it.toInt(), 100, 50)) },
            docs = (1L..10L).map { doc(it, "ref$it") },
        )

        val refs = ContentSearchIndex(dao).refs("x", kind = SearchKind.QURAN, limit = 3)

        assertThat(refs).containsExactly("ref10", "ref9", "ref8").inOrder()
        assertThat(dao.lastDocQuery!!.sql).contains("IN (?,?,?)")
    }

    @Test
    fun `nothing matched means no second query`() = runTest {
        val dao = CapturingDao(readyMeta())

        assertThat(ContentSearchIndex(dao).hits("light", kind = SearchKind.QURAN)).isEmpty()
        assertThat(dao.lastDocQuery).isNull()
    }

    // ---- The ways out that never reach the index ----

    @Test
    fun `a query with nothing searchable in it never reaches a MATCH`() = runTest {
        val dao = CapturingDao(readyMeta())
        val index = ContentSearchIndex(dao)

        // Halfway through typing, a search box holds exactly this. `MATCH ''` throws.
        assertThat(index.hits("  ,. ", kind = SearchKind.QURAN)).isEmpty()
        assertThat(index.refs("", kind = SearchKind.QURAN)).isEmpty()
        assertThat(dao.matchQueries).isEqualTo(0)
    }

    @Test
    fun `an install with no index answers empty without querying it`() = runTest {
        val dao = CapturingDao(meta = null, matches = listOf(match(1, Triple(1, 1, 1))))
        val index = ContentSearchIndex(dao)

        assertThat(index.hits("light", kind = SearchKind.QURAN)).isEmpty()
        assertThat(dao.matchQueries).isEqualTo(0)
    }

    @Test
    fun `an index this build cannot read is not queried either`() = runTest {
        val dao = CapturingDao(
            listOf(SearchIndexMetaRow("fold_version", "-1")),
            matches = listOf(match(1, Triple(1, 1, 1))),
        )
        val index = ContentSearchIndex(dao)

        assertThat(index.hits("light", kind = SearchKind.QURAN)).isEmpty()
        assertThat(dao.matchQueries).isEqualTo(0)
    }

    @Test
    fun `a query the index rejects empties the results rather than the screen`() = runTest {
        val dao = CapturingDao(readyMeta(), failMatches = true)
        val index = ContentSearchIndex(dao)

        // A term the FTS parser will not take is the realistic failure — the search screen
        // must come back empty, not fall over.
        assertThat(index.hits("light", kind = SearchKind.QURAN)).isEmpty()
        assertThat(dao.matchQueries).isEqualTo(1)
    }

    /** Collects whatever `SimpleSQLiteQuery` binds, in index order. */
    private class RecordingBinder : androidx.sqlite.db.SupportSQLiteProgram {
        private val slots = mutableMapOf<Int, Any?>()

        val values: List<Any?> get() = slots.toSortedMap().values.toList()

        override fun bindNull(index: Int) {
            slots[index] = null
        }

        override fun bindLong(index: Int, value: Long) {
            slots[index] = value
        }

        override fun bindDouble(index: Int, value: Double) {
            slots[index] = value
        }

        override fun bindString(index: Int, value: String) {
            slots[index] = value
        }

        override fun bindBlob(index: Int, value: ByteArray) {
            slots[index] = value
        }

        override fun clearBindings() {
            slots.clear()
        }

        override fun close() = Unit
    }
}
