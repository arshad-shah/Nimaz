package com.arshadshah.nimaz.data.local.content

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.data.local.database.NimazDatabase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The seeder that carries corpus corrections to installs the prepackaged asset never reaches.
 *
 * The properties pinned here are the ones whose failure would be silent on a device: applying
 * the wrong rows of a shared table, applying twice, or touching data the user owns.
 */
@RunWith(RobolectricTestRunner::class)
class ContentPatchSeederTest {

    private lateinit var db: NimazDatabase
    private val version = FakeVersionStore()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NimazDatabase::class.java
        ).allowMainThreadQueries().build()
        // translations.ayah_id -> ayahs.id -> surahs.id. The patch is applied with foreign
        // keys live, exactly as on a device, so the parents have to exist.
        exec(
            "INSERT INTO surahs (id, number, name_arabic, name_english, name_transliteration, " +
                "revelation_type, verses_count, order_revealed, start_page) " +
                "VALUES (1,1,'ا','A','A','Meccan',7,5,1)"
        )
        // Since schemaVersion 22 a verse row is its place in the mushaf; its text is a row
        // in mushaf_ayah_texts, which is also the table a patch now inserts into.
        exec(
            "INSERT INTO ayahs (id, surah_id, number_in_surah, number_global, juz, hizb, page) " +
                "VALUES (1,1,1,1,1,1,1)"
        )
    }

    @After
    fun tearDown() = db.close()

    private fun seeder(patch: String?) =
        ContentPatchSeeder(db, version, FakeAssetReader(patch))

    private fun exec(sql: String) = db.openHelper.writableDatabase.execSQL(sql)

    private fun query(sql: String): List<String> =
        db.openHelper.readableDatabase.query(sql).use { c ->
            buildList { while (c.moveToNext()) add(c.getString(0)) }
        }

    @Test
    fun `applies an update to the row its key names`() = runTest {
        exec("INSERT INTO translations (id, ayah_id, translator_id, text) VALUES (1, 1, 'en.sahih', 'before')")

        val result = seeder(
            patch(
                """{"collection":"tr.en_sahih","table":"translations",
                    "key":{"ayah_id":1,"translator_id":"en.sahih"},
                    "set":{"text":"after"}}"""
            )
        ).seedIfNeeded()

        assertThat(result).isEqualTo(ContentPatchResult.Applied(version = 2, ops = 1))
        assertThat(query("SELECT text FROM translations")).containsExactly("after")
    }

    @Test
    fun `a shared table is not rewritten wholesale by one edition's fix`() = runTest {
        // translations holds fifteen editions keyed by translator_id. A patch keyed only on
        // ayah_id would overwrite all fifteen with one edition's text — the exact bug the
        // emitter's verify step caught, pinned here on the consuming side too.
        exec("INSERT INTO translations (id, ayah_id, translator_id, text) VALUES (1, 1, 'en.sahih', 'english')")
        exec("INSERT INTO translations (id, ayah_id, translator_id, text) VALUES (2, 1, 'bn.bengali', 'bengali')")

        seeder(
            patch(
                """{"collection":"tr.en_sahih","table":"translations",
                    "key":{"ayah_id":1,"translator_id":"en.sahih"},
                    "set":{"text":"corrected"}}"""
            )
        ).seedIfNeeded()

        assertThat(query("SELECT text FROM translations ORDER BY translator_id"))
            .containsExactly("bengali", "corrected").inOrder()
    }

    @Test
    fun `applying twice leaves the same rows`() = runTest {
        exec("INSERT INTO translations (id, ayah_id, translator_id, text) VALUES (1, 1, 'en.sahih', 'before')")
        val body = patch(
            """{"collection":"tr.en_sahih","table":"translations",
                "key":{"ayah_id":1,"translator_id":"en.sahih"},"set":{"text":"after"}}"""
        )

        seeder(body).seedIfNeeded()
        val afterFirst = query("SELECT text FROM translations")
        // A second seeder, same patch: the version gate short-circuits it.
        val second = seeder(body).seedIfNeeded()

        assertThat(second).isEqualTo(ContentPatchResult.AlreadyApplied(2))
        assertThat(query("SELECT text FROM translations")).isEqualTo(afterFirst)
    }

    @Test
    fun `refuses a patch naming a user table and applies none of it`() = runTest {
        exec("INSERT INTO translations (id, ayah_id, translator_id, text) VALUES (1, 1, 'en.sahih', 'untouched')")

        val result = seeder(
            """{"format":1,"patchVersion":2,"baseline":"a","target":"b",
                "tables":["translations","quran_bookmarks"],
                "update":[{"table":"translations","key":{"ayah_id":1,"translator_id":"en.sahih"},
                           "set":{"text":"should not land"}}],
                "insert":[],"delete":[]}"""
        ).seedIfNeeded()

        assertThat(result).isEqualTo(
            ContentPatchResult.RefusedUserTable(listOf("quran_bookmarks"))
        )
        // The legitimate op is refused along with the rest: a patch that reached a user table
        // is a broken emitter, and partially trusting it is worse than trusting none of it.
        assertThat(query("SELECT text FROM translations")).containsExactly("untouched")
        assertThat(version.stored).isEqualTo(0)
    }

    @Test
    fun `no bundled patch is not an error`() = runTest {
        assertThat(seeder(null).seedIfNeeded()).isEqualTo(ContentPatchResult.NoPatch)
    }

    @Test
    fun `refuses a format it does not understand`() = runTest {
        val result = seeder(
            """{"format":99,"patchVersion":2,"tables":[],"update":[],"insert":[],"delete":[]}"""
        ).seedIfNeeded()
        assertThat(result).isEqualTo(ContentPatchResult.Unsupported(99))
        assertThat(version.stored).isEqualTo(0)
    }

    @Test
    fun `an older patch than the one recorded is skipped`() = runTest {
        version.stored = 5
        val result = seeder(
            patch(
                """{"table":"translations","key":{"ayah_id":1},"set":{"text":"x"}}"""
            )
        ).seedIfNeeded()
        assertThat(result).isEqualTo(ContentPatchResult.AlreadyApplied(2))
    }

    private fun patch(update: String) = """
        {"format":1,"patchVersion":2,"baseline":"a","target":"b",
         "tables":["translations"],"update":[$update],"insert":[],"delete":[]}
    """.trimIndent()

    private class FakeAssetReader(private val body: String?) : ContentPatchAssetReader {
        override fun read(path: String): String? = body
    }

    private class FakeVersionStore(var stored: Int = 0) : ContentPatchVersionStore {
        override suspend fun get(): Int = stored
        override suspend fun set(version: Int) {
            stored = version
        }
    }
}
