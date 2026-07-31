package com.arshadshah.nimaz.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arshadshah.nimaz.data.local.search.ContentSearchIndex
import com.arshadshah.nimaz.data.local.search.SearchKind
import com.arshadshah.nimaz.domain.search.ArabicSearchNormaliser
import com.arshadshah.nimaz.support.NimazDbRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The search index, end to end, against a real SQLite (#330).
 *
 * The index is built by arshad-shah/nimaz-data and arrives inside the artifact, so this
 * stands up the same three tables by hand and fills them through
 * [ArabicSearchNormaliser] — which is the point: if the Kotlin folding and the Python
 * folding ever disagree, `ArabicSearchNormaliserTest` catches it against the committed
 * fixtures, and this catches whether a *correctly folded* index is actually queryable
 * the way [ContentSearchIndex] queries it.
 *
 * It runs on a device rather than on the JVM because the thing under test is Android's
 * SQLite: whether FTS4 exists, what `unicode61` does to Arabic, and whether the
 * contentless table can be joined the way the production query joins it.
 */
@RunWith(AndroidJUnit4::class)
class ContentSearchIndexTest {

    @get:Rule
    val dbRule = NimazDbRule()

    private val index get() = ContentSearchIndex(dbRule.db.searchIndexDao())

    /** Verses as the corpus actually stores them: fully vocalised, ALEF WASLA and all. */
    private val ayahs = mapOf(
        1 to "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
        2 to "ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَٰلَمِينَ",
        3 to "ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
    )

    /** The artifact's own DDL, run through the support database rather than the DAO —
     * `@RawQuery` is for reading, and a CREATE that only runs when a cursor happens to be
     * stepped is the kind of test setup that fails for the wrong reason. */
    private fun createIndex(foldVersion: Int = ArabicSearchNormaliser.FOLD_VERSION) {
        exec(
            """
            CREATE TABLE search_docs (
                docid INTEGER PRIMARY KEY, kind TEXT NOT NULL, ref TEXT NOT NULL, source TEXT
            )
            """.trimIndent()
        )
        exec("CREATE VIRTUAL TABLE search_index USING fts4(body, content=\"\", tokenize=unicode61)")
        exec("CREATE TABLE search_meta (key TEXT PRIMARY KEY, value TEXT NOT NULL)")
        exec("INSERT INTO search_meta VALUES ('fold_version', '$foldVersion')")
        exec("INSERT INTO search_meta VALUES ('flavour', 'fts4')")
        exec("INSERT INTO search_meta VALUES ('documents', '${ayahs.size + 1}')")

        ayahs.forEach { (id, text) ->
            exec("INSERT INTO search_docs VALUES ($id, 'quran', '$id', 'SIMPLE')")
            exec(
                "INSERT INTO search_index (docid, body) VALUES ($id, ?)",
                ArabicSearchNormaliser.fold(text),
            )
        }
        // One translation row, so the `source` filter has something to exclude.
        exec("INSERT INTO search_docs VALUES (99, 'translation', '2', 'sahih_international')")
        exec(
            "INSERT INTO search_index (docid, body) VALUES (99, ?)",
            ArabicSearchNormaliser.fold("All praise is due to Allah, Lord of the worlds"),
        )
    }

    private fun exec(sql: String, vararg args: Any) {
        dbRule.db.openHelper.writableDatabase.execSQL(sql, arrayOf(*args))
    }

    @Test
    fun anArabicWordNoSubstringSearchCouldFindIsFound() = runTest {
        createIndex()
        // The stored text contains none of these as a substring — that is the bug.
        ayahs.values.forEach { assertThat(it).doesNotContain("الرحمن") }

        val refs = index.refs("الرحمن", SearchKind.QURAN)
        assertThat(refs).containsExactly("1", "3")
    }

    @Test
    fun theDivineNameIsFoundThroughAlefWasla() = runTest {
        createIndex()
        assertThat(index.refs("الله", SearchKind.QURAN)).containsExactly("1")
    }

    @Test
    fun severalWordsMatchAsAPhraseAndNotAsAnyWord() = runTest {
        createIndex()
        // "الحمد لله" is adjacent in ayah 2 only. If this were OR-ed it would also return
        // ayah 1, which carries لله but not الحمد — and the phrase bonus would rank noise.
        assertThat(index.refs("الحمد لله", SearchKind.QURAN)).containsExactly("2")
    }

    @Test
    fun aKindNarrowsAndSoDoesASource() = runTest {
        createIndex()
        assertThat(index.refs("praise", SearchKind.TRANSLATION)).containsExactly("2")
        assertThat(index.refs("praise", SearchKind.TRANSLATION, source = "bn_bengali")).isEmpty()
        assertThat(index.refs("praise", SearchKind.QURAN)).isEmpty()
    }

    @Test
    fun anIndexFoldedByADifferentVersionIsRefusedRatherThanUsed() = runTest {
        createIndex(foldVersion = ArabicSearchNormaliser.FOLD_VERSION + 1)
        // Present, non-empty, correctly shaped — and folded by rules this app does not
        // speak. Using it would return fewer results with nothing reporting why.
        assertThat(index.status()).isInstanceOf(ContentSearchIndex.Status.Mismatched::class.java)
        assertThat(index.isAvailable()).isFalse()
        assertThat(index.refs("الرحمن", SearchKind.QURAN)).isEmpty()
    }

    @Test
    fun anArtifactWithoutAnIndexReportsItRatherThanThrowing() = runTest {
        // Every install made before the index shipped. `createFromAsset` copies once, so
        // those phones keep the artifact they have and the repositories fall back.
        assertThat(index.status()).isEqualTo(ContentSearchIndex.Status.Absent)
        assertThat(index.refs("الرحمن", SearchKind.QURAN)).isEmpty()
    }

    @Test
    fun aQueryOfNothingButPunctuationDoesNotReachMatch() = runTest {
        createIndex()
        // MATCH throws on an empty expression, and a search box holds exactly this
        // halfway through being typed.
        assertThat(index.refs("  ... ", SearchKind.QURAN)).isEmpty()
    }
}
