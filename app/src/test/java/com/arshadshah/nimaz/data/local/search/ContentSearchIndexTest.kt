package com.arshadshah.nimaz.data.local.search

import androidx.sqlite.db.SimpleSQLiteQuery
import com.arshadshah.nimaz.domain.search.ArabicSearchNormaliser
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * The three states behind "Arabic search returns nothing".
 *
 * The FTS index ships inside the content artifact. When it is absent or stamped with a fold
 * version this build does not speak, search falls back to `LIKE` — and `LIKE` matches no Arabic
 * at all: الله appears in 1,746 verses and returns zero rows. An empty result list reads as "no
 * results", so nobody reports it, which is why #472/#473 had to make the artifact install
 * *visible*.
 *
 * This is the state machine that decides which of those a device is in, and it had no tests.
 */
class ContentSearchIndexTest {

    /** A stand-in for the artifact's `search_meta` table. */
    private class FakeDao(
        private var meta: List<SearchIndexMetaRow>?,
        private val refs: List<SearchIndexRefRow> = emptyList(),
    ) : SearchIndexDao {
        val metaReads = AtomicInteger(0)

        override suspend fun rawRefs(query: SimpleSQLiteQuery): List<SearchIndexRefRow> = refs

        override suspend fun rawMeta(query: SimpleSQLiteQuery): List<SearchIndexMetaRow> {
            metaReads.incrementAndGet()
            // Room throws "no such table: search_meta" on an artifact that predates the index.
            return meta ?: throw android.database.sqlite.SQLiteException("no such table: search_meta")
        }
    }

    private fun meta(
        foldVersion: String = ArabicSearchNormaliser.FOLD_VERSION.toString(),
        documents: String = "150000",
        flavour: String = "fts4",
    ) = listOf(
        SearchIndexMetaRow("fold_version", foldVersion),
        SearchIndexMetaRow("documents", documents),
        SearchIndexMetaRow("flavour", flavour),
    )

    @Test
    fun `a matching fold version is Ready`() = runTest {
        val index = ContentSearchIndex(FakeDao(meta()))

        val status = index.status()

        assertThat(status).isInstanceOf(ContentSearchIndex.Status.Ready::class.java)
        assertThat((status as ContentSearchIndex.Status.Ready).documents).isEqualTo(150_000)
        assertThat(status.flavour).isEqualTo("fts4")
        assertThat(index.isAvailable()).isTrue()
    }

    /**
     * The state every install that predates the index is in — and the one an install stuck on a
     * deferred content replace stays in indefinitely (#473).
     */
    @Test
    fun `a missing search_meta table is Absent, not an error`() = runTest {
        val index = ContentSearchIndex(FakeDao(meta = null))

        assertThat(index.status()).isEqualTo(ContentSearchIndex.Status.Absent)
        assertThat(index.isAvailable()).isFalse()
    }

    /**
     * An index folded by a different normaliser version would match the wrong things, so it is
     * refused rather than used. Refusing is the safe answer and also a silent one — hence the
     * telemetry in #472.
     */
    @Test
    fun `a fold version this build does not speak is Mismatched`() = runTest {
        val index = ContentSearchIndex(FakeDao(meta(foldVersion = "-1")))

        val status = index.status()

        assertThat(status).isInstanceOf(ContentSearchIndex.Status.Mismatched::class.java)
        assertThat((status as ContentSearchIndex.Status.Mismatched).stamped).isEqualTo("-1")
        assertThat(index.isAvailable()).isFalse()
    }

    @Test
    fun `an absent fold version is Mismatched rather than assumed compatible`() = runTest {
        val index = ContentSearchIndex(FakeDao(listOf(SearchIndexMetaRow("documents", "10"))))

        assertThat(index.status()).isInstanceOf(ContentSearchIndex.Status.Mismatched::class.java)
        assertThat(index.isAvailable()).isFalse()
    }

    /**
     * Probed once per process — the answer is a property of the file on disk, and the file does
     * not change under a running app. `ContentArtifactInstaller` replaces it *before* Room opens
     * it, so a swap always comes with a fresh process.
     */
    @Test
    fun `the status is probed once and cached`() = runTest {
        val dao = FakeDao(meta())
        val index = ContentSearchIndex(dao)

        repeat(5) { index.status() }
        index.isAvailable()

        assertThat(dao.metaReads.get()).isEqualTo(1)
    }

    @Test
    fun `an absent index is cached too, not retried on every search`() = runTest {
        val dao = FakeDao(meta = null)
        val index = ContentSearchIndex(dao)

        repeat(5) { index.status() }

        assertThat(dao.metaReads.get()).isEqualTo(1)
    }

    @Test
    fun `a malformed document count degrades to zero rather than failing`() = runTest {
        val index = ContentSearchIndex(FakeDao(meta(documents = "not-a-number")))

        val status = index.status()

        assertThat(status).isInstanceOf(ContentSearchIndex.Status.Ready::class.java)
        assertThat((status as ContentSearchIndex.Status.Ready).documents).isEqualTo(0)
    }
}
