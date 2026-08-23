package com.arshadshah.nimaz.data.local.database

import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * What each migration does to a database that is not quite the shape it expects.
 *
 * The instrumented suite already walks the chain end to end and checks it lands on the current
 * schema. What is asserted here is the other half, and it is the half that has actually broken
 * installs: every one of these steps runs against a database whose shape depends on *which
 * artifact the device happened to fetch*, not on which version it is upgrading from. Room runs
 * the migrations even after `createFromAsset`, so a statement that assumes a table is present
 * throws "no such table" on the first launch of a **fresh install** — a crash before the app has
 * drawn anything, on the devices least able to report it. Idempotence is the contract, and the
 * only way to test a contract about running twice is to run twice.
 *
 * These run on the JVM against a real SQLite file, so they are part of every pull request's
 * check rather than of the emulator lane.
 */
@RunWith(RobolectricTestRunner::class)
class NimazDatabaseMigrationTest {

    private lateinit var file: File
    private lateinit var helper: SupportSQLiteOpenHelper
    private lateinit var db: SupportSQLiteDatabase

    @Before
    fun setUp() {
        file = File.createTempFile("nimaz-migration", ".db").also { it.delete() }
        helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(
                ApplicationProvider.getApplicationContext<Context>()
            )
                .name(file.absolutePath)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, old: Int, new: Int) = Unit
                })
                .build()
        )
        db = helper.writableDatabase
    }

    @After
    fun tearDown() {
        helper.close()
        file.delete()
    }

    // ---- The `updatedAt` columns (v9 → v11) ----

    @Test
    fun `the missing v10 step adds the column a v9 database never got`() {
        db.execSQL("CREATE TABLE quran_favorites (ayahId INTEGER PRIMARY KEY)")

        migrate(NimazDatabase.MIGRATION_9_10)

        // Registered late: a device at v9 crashed on launch with "A migration from 9 to 10 was
        // required but not found" until this step existed.
        assertThat(columns("quran_favorites")).contains("updatedAt")
    }

    @Test
    fun `running the v10 and v11 steps in sequence does not add the column twice`() {
        db.execSQL("CREATE TABLE quran_favorites (ayahId INTEGER PRIMARY KEY)")
        db.execSQL("CREATE TABLE tasbih_presets (id INTEGER PRIMARY KEY)")
        db.execSQL("CREATE TABLE tasbih_sessions (id INTEGER PRIMARY KEY)")
        db.execSQL("CREATE TABLE khatam_ayahs (id INTEGER PRIMARY KEY)")
        db.execSQL("CREATE TABLE khatam_daily_log (id INTEGER PRIMARY KEY)")
        db.execSQL("CREATE TABLE zakat_history (id INTEGER PRIMARY KEY)")

        migrate(NimazDatabase.MIGRATION_9_10)
        // `quran_favorites.updatedAt` already exists by now. An unguarded ALTER here is
        // "duplicate column name" — a failed migration, which Room turns into a crash.
        migrate(NimazDatabase.MIGRATION_10_11)

        assertThat(columns("quran_favorites").count { it == "updatedAt" }).isEqualTo(1)
        assertThat(columns("zakat_history")).contains("updatedAt")
        assertThat(columns("khatam_daily_log")).contains("updatedAt")
    }

    @Test
    fun `the v11 step run twice is a no-op the second time`() {
        db.execSQL("CREATE TABLE quran_favorites (ayahId INTEGER PRIMARY KEY)")
        db.execSQL("CREATE TABLE tasbih_presets (id INTEGER PRIMARY KEY)")
        db.execSQL("CREATE TABLE tasbih_sessions (id INTEGER PRIMARY KEY)")
        db.execSQL("CREATE TABLE khatam_ayahs (id INTEGER PRIMARY KEY)")
        db.execSQL("CREATE TABLE khatam_daily_log (id INTEGER PRIMARY KEY)")
        db.execSQL("CREATE TABLE zakat_history (id INTEGER PRIMARY KEY)")

        migrate(NimazDatabase.MIGRATION_10_11)
        migrate(NimazDatabase.MIGRATION_10_11)

        assertThat(columns("tasbih_presets").count { it == "updatedAt" }).isEqualTo(1)
    }

    // ---- Repairing an artifact that arrived in an older shape (v12 → v13) ----

    @Test
    fun `the legacy repair leaves a current artifact alone rather than failing on it`() {
        // Five of the `updatedAt` tables and both tafseer annotation tables belong to the user's
        // database since schemaVersion 23, so a current artifact has none of them. An unguarded
        // `ALTER TABLE` here is a crash on the first launch of a fresh install.
        db.execSQL("CREATE TABLE surahs (id INTEGER PRIMARY KEY, name TEXT)")

        migrate(NimazDatabase.MIGRATION_12_13)

        assertThat(tables()).contains("surahs")
    }

    @Test
    fun `the legacy repair adds the columns a stale artifact is missing`() {
        db.execSQL("CREATE TABLE quran_favorites (ayahId INTEGER PRIMARY KEY)")
        db.execSQL("CREATE TABLE tasbih_presets (id INTEGER PRIMARY KEY)")

        migrate(NimazDatabase.MIGRATION_12_13)

        assertThat(columns("quran_favorites")).contains("updatedAt")
        assertThat(columns("tasbih_presets")).contains("updatedAt")
    }

    @Test
    fun `the legacy repair renames the tafseer indices to the names Room expects`() {
        db.execSQL(
            "CREATE TABLE tafseer_texts (id INTEGER PRIMARY KEY, ayah_id INTEGER, tafseer_id TEXT)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX `index_tafseer_texts_ayah_tafseer` " +
                "ON `tafseer_texts` (`ayah_id`, `tafseer_id`)"
        )

        migrate(NimazDatabase.MIGRATION_12_13)

        // The generator named these "*_ayah_tafseer"; Room validates against
        // "*_ayah_id_tafseer_id" and refuses to open the database when they disagree.
        assertThat(indices()).contains("index_tafseer_texts_ayah_id_tafseer_id")
        assertThat(indices()).doesNotContain("index_tafseer_texts_ayah_tafseer")
    }

    @Test
    fun `the legacy repair can be run twice`() {
        db.execSQL("CREATE TABLE quran_favorites (ayahId INTEGER PRIMARY KEY)")
        db.execSQL("CREATE TABLE tafseer_notes (id INTEGER PRIMARY KEY, ayah_id INTEGER, tafseer_id TEXT)")

        migrate(NimazDatabase.MIGRATION_12_13)
        migrate(NimazDatabase.MIGRATION_12_13)

        assertThat(columns("quran_favorites").count { it == "updatedAt" }).isEqualTo(1)
        assertThat(indices()).contains("index_tafseer_notes_ayah_id_tafseer_id")
    }

    // ---- Repairing data (v11 → v12, v15 → v16) ----

    @Test
    fun `a surah's start page is taken from the verses it actually holds`() {
        db.execSQL("CREATE TABLE surahs (id INTEGER PRIMARY KEY, start_page INTEGER)")
        db.execSQL("CREATE TABLE ayahs (id INTEGER PRIMARY KEY, surah_id INTEGER, page INTEGER)")
        db.execSQL("INSERT INTO surahs (id, start_page) VALUES (1, 99), (2, 99)")
        db.execSQL(
            "INSERT INTO ayahs (id, surah_id, page) VALUES (1, 1, 1), (2, 1, 2), (3, 2, 2), (4, 2, 3)"
        )

        migrate(NimazDatabase.MIGRATION_11_12)

        // The shipped values came from a different Mushaf edition and disagreed with the page
        // the reader actually opens on.
        assertThat(longsOf("SELECT start_page FROM surahs ORDER BY id"))
            .containsExactly(1L, 2L).inOrder()
    }

    @Test
    fun `the counting presets are filed under the category they belong to`() {
        db.execSQL("CREATE TABLE tasbih_presets (id INTEGER PRIMARY KEY, name TEXT)")
        db.execSQL(
            "INSERT INTO tasbih_presets (id, name) VALUES " +
                "(1, 'SubhanAllah'), (2, 'Astaghfirullah'), " +
                "(3, 'Allahumma bika amsayna'), (4, 'Something nobody shipped')"
        )

        migrate(NimazDatabase.MIGRATION_15_16)

        assertThat(stringOf("SELECT category FROM tasbih_presets WHERE id = 1"))
            .isEqualTo("after_prayer")
        assertThat(stringOf("SELECT category FROM tasbih_presets WHERE id = 2")).isEqualTo("daily")
        assertThat(stringOf("SELECT category FROM tasbih_presets WHERE id = 3"))
            .isEqualTo("evening")
        // A preset the app never shipped is left uncategorised rather than guessed at.
        assertThat(stringOf("SELECT category FROM tasbih_presets WHERE id = 4")).isNull()
    }

    // ---- Duplicated translations (v18 → v19) ----

    @Test
    fun `duplicated verses collapse to the row that was written first`() {
        db.execSQL(
            "CREATE TABLE translations (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "ayah_id INTEGER NOT NULL, translator_id TEXT NOT NULL, text TEXT NOT NULL)"
        )
        db.execSQL(
            "INSERT INTO translations (ayah_id, translator_id, text) VALUES " +
                "(1, 'en.sahih', 'first'), (1, 'en.sahih', 'second'), " +
                "(1, 'en.pickthall', 'other'), (2, 'en.sahih', 'unrelated')"
        )

        migrate(NimazDatabase.MIGRATION_18_19)

        // A re-seed that inserted without deleting doubled every verse; the surviving row is the
        // lowest id per (ayah, translator).
        assertThat(stringsOf("SELECT text FROM translations ORDER BY id"))
            .containsExactly("first", "other", "unrelated").inOrder()
    }

    @Test
    fun `the index added makes a second seeding impossible rather than merely undone`() {
        db.execSQL(
            "CREATE TABLE translations (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "ayah_id INTEGER NOT NULL, translator_id TEXT NOT NULL, text TEXT NOT NULL)"
        )
        db.execSQL("INSERT INTO translations (ayah_id, translator_id, text) VALUES (1, 'en.sahih', 'first')")

        migrate(NimazDatabase.MIGRATION_18_19)

        val duplicate = runCatching {
            db.execSQL("INSERT INTO translations (ayah_id, translator_id, text) VALUES (1, 'en.sahih', 'again')")
        }
        assertThat(duplicate.isFailure).isTrue()
        assertThat(indices()).contains("index_translations_ayah_id_translator_id")
    }

    // ---- The mushaf layout, generalised (v17 → v18 → v20) ----

    @Test
    fun `the single-edition layout table is replaced by the keyed one`() {
        db.execSQL("CREATE TABLE ayahs (id INTEGER PRIMARY KEY, text_uthmani TEXT)")

        migrate(NimazDatabase.MIGRATION_17_18)
        assertThat(tables()).contains("mushaf_layout_indopak16")
        assertThat(columns("ayahs")).contains("text_indopak")

        migrate(NimazDatabase.MIGRATION_19_20)

        // An edition becomes data rather than schema. The dropped table held only derived
        // content, so nothing of the user's goes with it.
        assertThat(tables()).doesNotContain("mushaf_layout_indopak16")
        assertThat(tables()).containsAtLeast("mushaf_layout_lines", "mushaf_ayah_texts")
    }

    @Test
    fun `the inert indopak column is emptied rather than dropped`() {
        db.execSQL("CREATE TABLE ayahs (id INTEGER PRIMARY KEY, text_indopak TEXT)")
        db.execSQL("INSERT INTO ayahs (id, text_indopak) VALUES (1, 'some text'), (2, NULL)")

        migrate(NimazDatabase.MIGRATION_19_20)

        // Dropping a column in SQLite rebuilds a 6,236-row table for no functional gain.
        assertThat(columns("ayahs")).contains("text_indopak")
        assertThat(longOf("SELECT COUNT(*) FROM ayahs WHERE text_indopak IS NOT NULL"))
            .isEqualTo(0)
    }

    // ---- Content tables created empty (v23 → v24) ----

    @Test
    fun `the thematic tables are created empty and can be created again`() {
        migrate(NimazDatabase.MIGRATION_23_24)

        assertThat(tables()).containsAtLeast(
            "surah_overviews",
            "surah_overview_sections",
            "ayah_themes",
            "quran_topics",
            "quran_topic_ayahs",
        )

        db.execSQL(
            "INSERT INTO quran_topics (topic_id, name, arabic_name, description, wiki_link, " +
                "is_thematic, is_ontology, ayah_count, related_topic_ids) " +
                "VALUES (1, 'Mercy', 'رحمة', '', '', 1, 0, 3, '[]')"
        )
        // A fresh install off a schemaVersion 24 artifact already has these tables with rows in
        // them, and still runs this migration.
        migrate(NimazDatabase.MIGRATION_23_24)

        assertThat(longOf("SELECT COUNT(*) FROM quran_topics")).isEqualTo(1)
    }

    // ---- Divisions derived on the device (v24 → v25) ----

    @Test
    fun `a verse learns its rukū and rub from the ranges already on the device`() {
        seedForV25()

        migrate(NimazDatabase.MIGRATION_24_25)

        // Without this the device reads four nulls and renders no markers at all, in the window
        // between updating and the matching artifact landing.
        assertThat(longsOf("SELECT ruku_number FROM ayahs ORDER BY id"))
            .containsExactly(1L, 1L, 2L).inOrder()
        assertThat(longsOf("SELECT ruku_end_ayah_id FROM ayahs ORDER BY id"))
            .containsExactly(2L, 2L, 3L).inOrder()
        assertThat(longsOf("SELECT rub_number FROM ayahs ORDER BY id"))
            .containsExactly(1L, 1L, 2L).inOrder()
    }

    @Test
    fun `the rukū number restarts within each surah`() {
        seedForV25()
        // `rukus.number` counts 1..556 across the whole Quran, and no Mushaf has ever printed
        // that — the reader wants the number within the surah.
        db.execSQL("INSERT INTO ayahs (id, surah_id) VALUES (4, 2)")
        db.execSQL("INSERT INTO rukus (number, surah_id, start_ayah_id, end_ayah_id) VALUES (3, 2, 4, 4)")

        migrate(NimazDatabase.MIGRATION_24_25)

        assertThat(longOf("SELECT ruku_number FROM ayahs WHERE id = 4")).isEqualTo(1)
    }

    @Test
    fun `the v25 step runs again on a database that already arrived with the columns`() {
        seedForV25()

        migrate(NimazDatabase.MIGRATION_24_25)
        migrate(NimazDatabase.MIGRATION_24_25)

        assertThat(columns("ayahs").count { it == "ruku_number" }).isEqualTo(1)
        assertThat(longsOf("SELECT ruku_number FROM ayahs ORDER BY id"))
            .containsExactly(1L, 1L, 2L).inOrder()
    }

    @Test
    fun `the ayah projection is recreated as a view`() {
        seedForV25()

        migrate(NimazDatabase.MIGRATION_24_25)

        // DROP-then-CREATE rather than `CREATE VIEW IF NOT EXISTS`: SQLite stores the defining
        // statement verbatim and Room compares the whole string, so the guard would itself be
        // the mismatch that makes the database unopenable.
        assertThat(stringOf("SELECT sql FROM sqlite_master WHERE type='view' AND name='ayah_with_text'"))
            .doesNotContain("IF NOT EXISTS")
    }

    /** The shape a schemaVersion 24 database is in, with two surahs' worth of divisions. */
    private fun seedForV25() {
        db.execSQL("CREATE TABLE ayahs (id INTEGER PRIMARY KEY, surah_id INTEGER NOT NULL)")
        db.execSQL(
            "CREATE TABLE rukus (number INTEGER PRIMARY KEY, surah_id INTEGER NOT NULL, " +
                "start_ayah_id INTEGER NOT NULL, end_ayah_id INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE hizb_quarters (number INTEGER PRIMARY KEY, " +
                "start_ayah_id INTEGER NOT NULL, end_ayah_id INTEGER NOT NULL)"
        )
        db.execSQL("INSERT INTO ayahs (id, surah_id) VALUES (1, 1), (2, 1), (3, 1)")
        db.execSQL(
            "INSERT INTO rukus (number, surah_id, start_ayah_id, end_ayah_id) " +
                "VALUES (1, 1, 1, 2), (2, 1, 3, 3)"
        )
        db.execSQL(
            "INSERT INTO hizb_quarters (number, start_ayah_id, end_ayah_id) VALUES (1, 1, 2), (2, 3, 3)"
        )
    }

    // ---- Tables that arrive empty (v7 → v8, v8 → v9, v13 → v14, v14 → v15) ----

    @Test
    fun `the khatam tables are created, and creating them again keeps their rows`() {
        migrate(NimazDatabase.MIGRATION_7_8)

        assertThat(tables()).containsAtLeast("khatams", "khatam_ayahs", "khatam_daily_log")
        db.execSQL(
            "INSERT INTO khatams (name, created_at, updated_at) VALUES ('Ramadan', 1, 1)"
        )

        // Room runs every migration after `createFromAsset` too, so each of these also runs on
        // a fresh install whose artifact already brought the tables.
        migrate(NimazDatabase.MIGRATION_7_8)

        assertThat(longOf("SELECT COUNT(*) FROM khatams")).isEqualTo(1)
    }

    @Test
    fun `the names and prophets tables are created and are safe to create again`() {
        migrate(NimazDatabase.MIGRATION_8_9)
        migrate(NimazDatabase.MIGRATION_8_9)

        assertThat(tables()).containsAtLeast(
            "asma_ul_husna",
            "asma_ul_husna_bookmarks",
            "asma_un_nabi",
            "asma_un_nabi_bookmarks",
            "prophets",
            "prophet_bookmarks",
        )
    }

    @Test
    fun `the help tables are created and are safe to create again`() {
        migrate(NimazDatabase.MIGRATION_13_14)
        migrate(NimazDatabase.MIGRATION_13_14)

        assertThat(tables()).containsAtLeast("help_topic", "help_item", "help_step", "help_string")
        assertThat(indices()).containsAtLeast(
            "index_help_item_topic_id",
            "index_help_step_item_id",
            "index_help_string_lang_code",
        )
    }

    @Test
    fun `the qaida tables are created and the shipped lessons are not disturbed`() {
        migrate(NimazDatabase.MIGRATION_14_15)

        assertThat(tables()).containsAtLeast(
            "qaida_lessons",
            "qaida_letters",
            "qaida_lines",
            "qaida_cells",
            "qaida_lesson_progress",
            "qaida_cell_progress",
        )
        db.execSQL(
            "INSERT INTO qaida_lesson_progress (lesson_id, status, stars, completed_cells, " +
                "total_cells, updated_at) VALUES (1, 'IN_PROGRESS', 2, 3, 10, 1)"
        )

        // On a fresh install the four content tables arrive full, and the two progress tables
        // are the user's — neither may be emptied by a second run.
        migrate(NimazDatabase.MIGRATION_14_15)

        assertThat(longOf("SELECT COUNT(*) FROM qaida_lesson_progress")).isEqualTo(1)
    }

    // ---- Commentary folded into blocks (v20 → v21) ----

    @Test
    fun `consecutive verses sharing one commentary become one block`() {
        seedTafseerTexts(
            Triple(1, 81, "on 81-83") to "ibn_kathir",
            Triple(1, 82, "on 81-83") to "ibn_kathir",
            Triple(1, 83, "on 81-83") to "ibn_kathir",
        )

        migrate(NimazDatabase.MIGRATION_20_21)

        // The passage used to be duplicated under every ayah it covers, so the reader showed
        // the same text three times with nothing to say it was one passage.
        assertThat(blocks()).containsExactly("ibn_kathir 1:81-83 on 81-83")
    }

    @Test
    fun `a gap in the verses starts a new block rather than widening one`() {
        seedTafseerTexts(
            Triple(1, 1, "same words") to "ibn_kathir",
            Triple(1, 2, "same words") to "ibn_kathir",
            // 3 is missing.
            Triple(1, 4, "same words") to "ibn_kathir",
        )

        migrate(NimazDatabase.MIGRATION_20_21)

        // Grouping on the text alone would bridge the gap and claim commentary on a verse that
        // has none.
        assertThat(blocks())
            .containsExactly("ibn_kathir 1:1-2 same words", "ibn_kathir 1:4-4 same words")
    }

    @Test
    fun `two commentaries on the same verses stay apart`() {
        seedTafseerTexts(
            Triple(1, 1, "one view") to "ibn_kathir",
            Triple(1, 1, "another view") to "maarif",
        )

        migrate(NimazDatabase.MIGRATION_20_21)

        assertThat(blocks())
            .containsExactly("ibn_kathir 1:1-1 one view", "maarif 1:1-1 another view")
    }

    @Test
    fun `the fold runs only into an empty table, and takes the old one away with it`() {
        seedTafseerTexts(Triple(1, 1, "words") to "ibn_kathir")

        migrate(NimazDatabase.MIGRATION_20_21)

        // Dropping the source is what makes the second run a no-op — a database holding both
        // shapes must not end up with the same commentary twice.
        assertThat(tables()).doesNotContain("tafseer_texts")
        migrate(NimazDatabase.MIGRATION_20_21)
        assertThat(blocks()).hasSize(1)
    }

    @Test
    fun `a database with no commentary at all still gets the table`() {
        migrate(NimazDatabase.MIGRATION_20_21)

        assertThat(tables()).contains("tafseer_blocks")
        assertThat(longOf("SELECT COUNT(*) FROM tafseer_blocks")).isEqualTo(0)
    }

    // ---- A verse's row becomes its place in the mushaf (v21 → v22) ----

    @Test
    fun `the divisions are derived from the columns that described them per verse`() {
        seedForV22()

        migrate(NimazDatabase.MIGRATION_21_22)

        // None of this can be fetched: `createFromAsset` re-copies only on a fresh install and
        // a content patch cannot create a table its baseline never had, so anything not
        // derivable here would simply be absent for existing users.
        assertThat(rowsOf("SELECT number, start_ayah_id, end_ayah_id FROM juzs ORDER BY number"))
            .containsExactly("1|1|3", "2|4|4").inOrder()
        assertThat(rowsOf("SELECT number, start_ayah_id, end_ayah_id FROM pages ORDER BY number"))
            .containsExactly("1|1|2", "2|3|4").inOrder()
        assertThat(
            rowsOf("SELECT number, juz_number, start_ayah_id, end_ayah_id FROM hizb_quarters ORDER BY number")
        ).containsExactly("1|1|1|3", "2|2|4|4").inOrder()
    }

    @Test
    fun `the uthmani text a verse carried becomes a text source`() {
        seedForV22()

        migrate(NimazDatabase.MIGRATION_21_22)

        assertThat(rowsOf("SELECT ayah_id, text FROM mushaf_ayah_texts WHERE text_source = 'UTHMANI' ORDER BY ayah_id"))
            .containsExactly("1|verse one", "2|verse two", "3|verse three", "4|verse four")
            .inOrder()
    }

    @Test
    fun `the marked verses become the sajda table, numbered in order`() {
        seedForV22()

        migrate(NimazDatabase.MIGRATION_21_22)

        assertThat(rowsOf("SELECT ayah_id, sequence, kind FROM sajdas ORDER BY ayah_id"))
            .containsExactly("2|1|obligatory", "4|2|recommended").inOrder()
    }

    @Test
    fun `the verse row is rebuilt without the columns that moved, keeping the rest`() {
        seedForV22()

        migrate(NimazDatabase.MIGRATION_21_22)

        // SQLite cannot drop a column in a table this old, so the columns go by rebuilding it.
        assertThat(columns("ayahs")).containsAtLeast("id", "surah_id", "juz", "hizb", "page")
        assertThat(columns("ayahs")).containsNoneOf("text_uthmani", "text_arabic", "sajda")
        assertThat(longOf("SELECT COUNT(*) FROM ayahs")).isEqualTo(4)
        assertThat(rowsOf("SELECT id, transliteration FROM ayahs ORDER BY id LIMIT 1"))
            .containsExactly("1|bismillah")
    }

    /** A schemaVersion 21 `ayahs` table, with the two tables this step writes into. */
    private fun seedForV22() {
        db.execSQL("CREATE TABLE surahs (id INTEGER PRIMARY KEY)")
        db.execSQL(
            "CREATE TABLE ayahs (id INTEGER PRIMARY KEY, surah_id INTEGER NOT NULL, " +
                "number_in_surah INTEGER NOT NULL, number_global INTEGER NOT NULL, " +
                "juz INTEGER NOT NULL, hizb INTEGER NOT NULL, page INTEGER NOT NULL, " +
                "transliteration TEXT, text_tajweed TEXT, text_uthmani TEXT, " +
                "sajda INTEGER NOT NULL DEFAULT 0, sajda_type TEXT)"
        )
        // `mushaf_ayah_texts` arrived in the step before this one.
        db.execSQL(
            "CREATE TABLE mushaf_ayah_texts (text_source TEXT NOT NULL, ayah_id INTEGER NOT NULL, " +
                "text TEXT NOT NULL, PRIMARY KEY(text_source, ayah_id))"
        )
        db.execSQL("INSERT INTO surahs (id) VALUES (1)")
        db.execSQL(
            "INSERT INTO ayahs (id, surah_id, number_in_surah, number_global, juz, hizb, page, " +
                "transliteration, text_uthmani, sajda, sajda_type) VALUES " +
                "(1, 1, 1, 1, 1, 1, 1, 'bismillah', 'verse one', 0, NULL), " +
                "(2, 1, 2, 2, 1, 1, 1, NULL, 'verse two', 1, 'obligatory'), " +
                "(3, 1, 3, 3, 1, 1, 2, NULL, 'verse three', 0, NULL), " +
                "(4, 1, 4, 4, 2, 2, 2, NULL, 'verse four', 1, NULL)"
        )
    }

    private fun seedTafseerTexts(vararg rows: Pair<Triple<Int, Int, String>, String>) {
        db.execSQL(
            "CREATE TABLE tafseer_texts (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "tafseer_id TEXT NOT NULL, surah_number INTEGER NOT NULL, " +
                "ayah_number INTEGER NOT NULL, ayah_id INTEGER NOT NULL, text TEXT NOT NULL)"
        )
        rows.forEachIndexed { index, (key, tafseer) ->
            val (surah, ayah, text) = key
            db.execSQL(
                "INSERT INTO tafseer_texts (tafseer_id, surah_number, ayah_number, ayah_id, text) " +
                    "VALUES (?, ?, ?, ?, ?)",
                arrayOf<Any>(tafseer, surah, ayah, index + 1, text),
            )
        }
    }

    private fun blocks(): List<String> = rowsOf(
        "SELECT tafseer_id || ' ' || surah_number || ':' || ayah_start || '-' || ayah_end " +
            "|| ' ' || text FROM tafseer_blocks ORDER BY tafseer_id, ayah_start"
    )

    // ---- The remaining steps ----

    @Test
    fun `a hadith gains a place for its chain of narration, once`() {
        db.execSQL("CREATE TABLE hadiths (id INTEGER PRIMARY KEY, text_arabic TEXT)")

        migrate(NimazDatabase.MIGRATION_16_17)
        migrate(NimazDatabase.MIGRATION_16_17)

        // Nullable, because when it is absent the reader derives the isnād from the Arabic.
        assertThat(columns("hadiths").count { it == "narrator_chain" }).isEqualTo(1)
    }

    @Test
    fun `the step that moved the user's tables leaves them exactly where they were`() {
        db.execSQL("CREATE TABLE quran_bookmarks (id INTEGER PRIMARY KEY, ayahId INTEGER)")
        db.execSQL("INSERT INTO quran_bookmarks (id, ayahId) VALUES (1, 262)")

        migrate(NimazDatabase.MIGRATION_22_23)

        // Deliberately empty. Room ignores tables it does not declare, and a bug in the copy out
        // of here is only survivable while the original rows are still on disk.
        assertThat(tables()).contains("quran_bookmarks")
        assertThat(longOf("SELECT COUNT(*) FROM quran_bookmarks")).isEqualTo(1)
    }

    @Test
    fun `the prepackaged callback repairs a freshly copied artifact`() {
        db.execSQL("CREATE TABLE quran_favorites (ayahId INTEGER PRIMARY KEY)")
        db.execSQL("CREATE TABLE tafseer_texts (id INTEGER PRIMARY KEY, ayah_id INTEGER, tafseer_id TEXT)")
        db.execSQL(
            "CREATE UNIQUE INDEX `index_tafseer_texts_ayah_tafseer` " +
                "ON `tafseer_texts` (`ayah_id`, `tafseer_id`)"
        )

        // The same repair as MIGRATION_12_13, down the other of the two paths: right after the
        // asset is copied and *before* Room validates its schema.
        NimazDatabase.PREPACKAGED_CALLBACK.onOpenPrepackagedDatabase(db)

        assertThat(columns("quran_favorites")).contains("updatedAt")
        assertThat(indices()).contains("index_tafseer_texts_ayah_id_tafseer_id")
    }

    @Test
    fun `the prepackaged callback does not fall over on a current artifact`() {
        // The shape a fresh install actually gets: none of the tables the repair names, because
        // they belong to the user's database since schemaVersion 23.
        db.execSQL("CREATE TABLE surahs (id INTEGER PRIMARY KEY)")

        NimazDatabase.PREPACKAGED_CALLBACK.onOpenPrepackagedDatabase(db)

        assertThat(tables()).contains("surahs")
    }

    @Test
    fun `every registered step appears in the chain exactly once`() {
        val steps = NimazDatabase.ALL_MIGRATIONS.map { it.startVersion to it.endVersion }

        // `DatabaseModule` registers this array and `MigrationChainTest` replays it, so a
        // migration that is written but not listed is registered nowhere. It has happened: the
        // chain failed with "A migration from 7 to 20 was required but not found" while the app
        // itself was fine, because the two kept separate hand-maintained copies.
        assertThat(steps).containsNoDuplicates()
        assertThat(steps.map { it.first }.sorted())
            .isEqualTo((7 until NIMAZ_DATABASE_VERSION).toList())
        steps.forEach { (start, end) -> assertThat(end).isEqualTo(start + 1) }
    }

    // ---- Reading the schema back ----

    private fun migrate(migration: Migration) = migration.migrate(db)

    private fun columns(table: String): List<String> =
        db.query("PRAGMA table_info(`$table`)").use { cursor ->
            val index = cursor.getColumnIndex("name")
            buildList { while (cursor.moveToNext()) add(cursor.getString(index)) }
        }

    private fun tables(): List<String> =
        stringsOf("SELECT name FROM sqlite_master WHERE type='table'")

    private fun indices(): List<String> =
        stringsOf("SELECT name FROM sqlite_master WHERE type='index'")

    /** Every row of [sql], columns joined by `|`, so a multi-column shape reads as one string. */
    private fun rowsOf(sql: String): List<String> = db.query(sql).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add((0 until cursor.columnCount).joinToString("|") { cursor.getString(it) })
            }
        }
    }

    private fun stringsOf(sql: String): List<String> = db.query(sql).use { cursor ->
        buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
    }

    private fun longsOf(sql: String): List<Long> = db.query(sql).use { cursor ->
        buildList { while (cursor.moveToNext()) add(cursor.getLong(0)) }
    }

    private fun stringOf(sql: String): String? = db.query(sql).use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }

    private fun longOf(sql: String): Long = db.query(sql).use { cursor ->
        cursor.moveToFirst()
        cursor.getLong(0)
    }
}
