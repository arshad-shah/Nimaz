package com.arshadshah.nimaz.data.local.search

import androidx.sqlite.db.SimpleSQLiteQuery
import com.arshadshah.nimaz.domain.search.ArabicSearchNormaliser
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * What the index is actually asked, once it has said it is Ready.
 *
 * [ContentSearchIndex.hits] assembles a raw SQL string and a parallel argument list by hand,
 * because the `source` filter is optional and `SimpleSQLiteQuery` has no way to express that.
 * Hand-built pairs drift: an argument appended without its placeholder — or the other way round
 * — is `IllegalArgumentException` at bind time on the one code path that takes the filter, and
 * a filter bound in the wrong order silently searches for the right words in the wrong kind.
 * Neither shows up in a `Ready` status, so both are asserted on the query itself.
 *
 * The two ways out that return an empty list without touching the database matter for the same
 * reason: a search box holding one punctuation mark, and an install with no index at all, must
 * not reach a `MATCH` at all — `MATCH ''` throws.
 */
class ContentSearchIndexQueryTest {

    /** Records what the index asked for, and answers with whatever it was seeded with. */
    private class CapturingDao(
        private val meta: List<SearchIndexMetaRow>?,
        private val refs: List<SearchIndexRefRow> = emptyList(),
        private val failRefs: Boolean = false,
    ) : SearchIndexDao {
        var lastRefQuery: SimpleSQLiteQuery? = null
        var refQueries = 0

        override suspend fun rawRefs(query: SimpleSQLiteQuery): List<SearchIndexRefRow> {
            refQueries++
            lastRefQuery = query
            if (failRefs) throw android.database.sqlite.SQLiteException("malformed MATCH expression")
            return refs
        }

        override suspend fun rawMeta(query: SimpleSQLiteQuery): List<SearchIndexMetaRow> =
            meta ?: throw android.database.sqlite.SQLiteException("no such table: search_meta")

        /** The bound arguments, in the order the index appended them. */
        fun boundArgs(): List<Any?> {
            val binder = RecordingBinder()
            lastRefQuery!!.bindTo(binder)
            return binder.values
        }
    }

    private fun readyMeta() = listOf(
        SearchIndexMetaRow("fold_version", ArabicSearchNormaliser.FOLD_VERSION.toString()),
        SearchIndexMetaRow("documents", "150000"),
        SearchIndexMetaRow("flavour", "fts4"),
    )

    private fun hit(ref: String, kind: String = "quran") =
        SearchIndexRefRow(kind = kind, ref = ref, source = null)

    // ---- The query that is built ----

    @Test
    fun `a search binds the folded expression, the kind and the limit, in that order`() = runTest {
        val dao = CapturingDao(readyMeta(), refs = listOf(hit("2:255")))
        val index = ContentSearchIndex(dao)

        index.hits("light", kind = SearchKind.QURAN, limit = 25)

        assertThat(dao.boundArgs())
            .containsExactly(ArabicSearchNormaliser.matchExpression("light"), "quran", 25L)
            .inOrder()
        assertThat(dao.lastRefQuery!!.sql).contains("d.kind = ?")
        assertThat(dao.lastRefQuery!!.sql).doesNotContain("d.source = ?")
    }

    @Test
    fun `a source filter adds both its placeholder and its argument`() = runTest {
        val dao = CapturingDao(readyMeta(), refs = listOf(hit("bukhari:1", kind = "hadith")))
        val index = ContentSearchIndex(dao)

        index.hits("prayer", kind = SearchKind.HADITH, source = "bukhari", limit = 10)

        // Placeholder and argument have to arrive together and in step — the limit is bound
        // last, after whichever filters were appended.
        assertThat(dao.lastRefQuery!!.sql).contains("d.source = ?")
        assertThat(dao.boundArgs())
            .containsExactly(
                ArabicSearchNormaliser.matchExpression("prayer"),
                "hadith",
                "bukhari",
                10L,
            )
            .inOrder()
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

    @Test
    fun `the default limit is used when the caller does not choose one`() = runTest {
        val dao = CapturingDao(readyMeta())
        val index = ContentSearchIndex(dao)

        index.hits("light", kind = SearchKind.QURAN)

        assertThat(dao.boundArgs().last())
            .isEqualTo(ContentSearchIndex.DEFAULT_LIMIT.toLong())
    }

    @Test
    fun `refs are the hits' keys, in the order the index returned them`() = runTest {
        val dao = CapturingDao(
            readyMeta(),
            refs = listOf(hit("2:255"), hit("24:35"), hit("112:1")),
        )
        val index = ContentSearchIndex(dao)

        assertThat(index.refs("light", kind = SearchKind.QURAN))
            .containsExactly("2:255", "24:35", "112:1").inOrder()
    }

    // ---- The ways out that never reach the index ----

    @Test
    fun `a query with nothing searchable in it never reaches a MATCH`() = runTest {
        val dao = CapturingDao(readyMeta())
        val index = ContentSearchIndex(dao)

        // Halfway through typing, a search box holds exactly this. `MATCH ''` throws.
        assertThat(index.hits("  ,. ", kind = SearchKind.QURAN)).isEmpty()
        assertThat(index.refs("", kind = SearchKind.QURAN)).isEmpty()
        assertThat(dao.refQueries).isEqualTo(0)
    }

    @Test
    fun `an install with no index answers empty without querying it`() = runTest {
        val dao = CapturingDao(meta = null, refs = listOf(hit("2:255")))
        val index = ContentSearchIndex(dao)

        assertThat(index.hits("light", kind = SearchKind.QURAN)).isEmpty()
        assertThat(dao.refQueries).isEqualTo(0)
    }

    @Test
    fun `an index this build cannot read is not queried either`() = runTest {
        val dao = CapturingDao(
            listOf(SearchIndexMetaRow("fold_version", "-1")),
            refs = listOf(hit("2:255")),
        )
        val index = ContentSearchIndex(dao)

        assertThat(index.hits("light", kind = SearchKind.QURAN)).isEmpty()
        assertThat(dao.refQueries).isEqualTo(0)
    }

    @Test
    fun `a query the index rejects empties the results rather than the screen`() = runTest {
        val dao = CapturingDao(readyMeta(), failRefs = true)
        val index = ContentSearchIndex(dao)

        // A term the FTS parser will not take is the realistic failure — the search screen
        // must come back empty, not fall over.
        assertThat(index.hits("light", kind = SearchKind.QURAN)).isEmpty()
        assertThat(dao.refQueries).isEqualTo(1)
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
