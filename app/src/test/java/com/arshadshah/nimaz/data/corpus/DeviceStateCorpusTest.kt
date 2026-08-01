package com.arshadshah.nimaz.data.corpus

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.data.local.database.NIMAZ_DATABASE_VERSION
import com.arshadshah.nimaz.data.local.database.NimazDatabase
import com.arshadshah.nimaz.domain.model.MushafScript
import com.arshadshah.nimaz.domain.model.QuranTranslation
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.security.MessageDigest

/**
 * Proves the shipped artifact is the database a real device actually holds.
 *
 * ## Why this exists
 *
 * This used to manufacture device state: the v12 asset, plus the migrations, plus six content
 * seeders that between them carried 15 translations, 4 Mushaf editions, 379 repaired hadiths
 * and the Help, Dua and Qaida content — about 31 MB the asset did not have. The seeders were
 * the difference between what shipped and what a user saw, so the corpus could not be sealed
 * without running them.
 *
 * That difference is now zero. The artifact fetched from `arshad-shah/nimaz-data` carries all
 * of it at [NIMAZ_DATABASE_VERSION], the seeders had become no-ops, and all six were retired at
 * versionCode 385 (`docs/retirement.yaml`). So what this asserts has inverted: not "the seeders
 * fill these tables" but **"nothing needs to fill them"**.
 *
 * That makes it the standing guard on the artifact itself. A content collection dropped
 * upstream, a `data.lock.json` rolled back to a tag that predates a table, an importer that
 * silently emitted zero rows — each one lands here as a named empty table rather than as an
 * empty screen on a device.
 *
 * ## What it asserts
 *
 * - every content table is non-empty, named individually so a failure says which one;
 * - no hadith shipped with blank Arabic — the gap the retired `HadithBackfillSeeder` existed to
 *   repair, asserted directly now that nothing repairs it at runtime;
 * - each line-accurate edition's stored layout agrees with the [MushafScript] catalogue on
 *   lines per page, which is the one invariant only the app can state (the data repo declares
 *   its own mirror of these numbers, so a drift between the two is exactly what goes unnoticed);
 * - no user table is present at all — since schemaVersion 23 they live in `NimazUserDatabase`,
 *   so the corpus cannot carry a row of somebody's bookmarks even by accident;
 * - the schema is at [NIMAZ_DATABASE_VERSION].
 *
 * As a **harness**, given `-Dnimaz.corpus.out=<path>`, it also writes the resulting database
 * out and prints its sha256, which is what `nz vault seal` consumes when the corpus needs
 * re-baselining against real device state.
 */
@RunWith(RobolectricTestRunner::class)
class DeviceStateCorpusTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `device state is the artifact plus every migration, with nothing left to seed`() =
        runTest {
            val db = openMigratedFromAsset()
            try {
                // If any of these is empty the artifact would ship missing content, which is
                // the failure this harness exists to make impossible.
                val counts = contentRowCounts(db)
                for ((table, rows) in counts) {
                    assertThat("$table=$rows").isNotEqualTo("$table=0")
                }

                assertThat(scalar(db, "SELECT COUNT(*) FROM hadiths WHERE TRIM(text_arabic) = ''"))
                    .isEqualTo(0)

                assertThat(translatorIdsInArtifact(db))
                    .containsExactlyElementsIn(QuranTranslation.entries.map { it.id })

                for (script in MushafScript.entries.filter { it.isLineAccurate }) {
                    val widest = scalar(
                        db,
                        "SELECT COALESCE(MAX(line), 0) FROM mushaf_layout_lines " +
                            "WHERE script = '${script.name}'"
                    )
                    assertThat("${script.name} max line=$widest")
                        .isEqualTo("${script.name} max line=${script.linesPerPage}")
                }

                // User tables must not be here at all. They used to be part of this schema and
                // empty — the assertion was that no row leaked, because a single one would ship
                // somebody's bookmarks to everybody. Since schemaVersion 23 the stronger
                // statement holds: they are in the user's own database.
                for (table in USER_TABLES) {
                    assertThat("$table exists=${hasTable(db, table)}")
                        .isEqualTo("$table exists=false")
                }

                assertThat(userVersion(db)).isEqualTo(NIMAZ_DATABASE_VERSION)

                // Absent or blank means "just assert" — the harness role is opt-in, so the
                // suite stays a fast regression check on CI and only writes 147 MB on demand.
                System.getProperty(OUT_PROPERTY)
                    ?.takeIf { it.isNotBlank() }
                    ?.let { export(db, File(it), counts) }
            } finally {
                db.close()
            }
        }

    /**
     * Opens the shipped artifact through Room exactly as the app does, so the migration chain
     * runs as production `Migration` objects rather than as a description of them.
     */
    private fun openMigratedFromAsset(): NimazDatabase {
        val target = File(context.cacheDir, "device-state-corpus.db")
        target.delete()
        return Room.databaseBuilder(context, NimazDatabase::class.java, target.absolutePath)
            .createFromAsset("database/nimaz_prepopulated.db", NimazDatabase.PREPACKAGED_CALLBACK)
            .addMigrations(*NimazDatabase.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()
            .also { it.openHelper.writableDatabase }
    }

    private fun contentRowCounts(db: NimazDatabase): Map<String, Int> =
        CONTENT_TABLES.associateWith { rowCount(db, it) }

    private fun translatorIdsInArtifact(db: NimazDatabase): List<String> =
        db.openHelper.readableDatabase
            .query("SELECT DISTINCT translator_id FROM translations")
            .use { cursor ->
                buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
            }

    /**
     * Writes the corpus out for `nz vault seal`.
     *
     * VACUUM first: the file is about to become the identity of every artifact built from it,
     * and free-list state is noise that would differ run to run.
     */
    private fun export(db: NimazDatabase, out: File, counts: Map<String, Int>) {
        db.openHelper.writableDatabase.execSQL("VACUUM")
        db.close()
        val source = File(context.cacheDir, "device-state-corpus.db")
        out.parentFile?.mkdirs()
        source.copyTo(out, overwrite = true)

        val digest = MessageDigest.getInstance("SHA-256")
        out.inputStream().use { stream ->
            val buffer = ByteArray(1 shl 20)
            while (true) {
                val read = stream.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        println("corpus  ${out.absolutePath}")
        println("bytes   ${out.length()}")
        println("sha256  ${digest.digest().joinToString("") { "%02x".format(it) }}")
        counts.toSortedMap().forEach { (table, rows) -> println("  $table  $rows") }
    }

    private fun hasTable(db: NimazDatabase, table: String): Boolean =
        db.openHelper.readableDatabase
            .query("SELECT 1 FROM sqlite_master WHERE type='table' AND name = ?", arrayOf(table))
            .use { it.moveToFirst() }

    private fun rowCount(db: NimazDatabase, table: String): Int =
        scalar(db, "SELECT COUNT(*) FROM `$table`")

    private fun scalar(db: NimazDatabase, sql: String): Int =
        db.openHelper.readableDatabase.query(sql).use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }

    private fun userVersion(db: NimazDatabase): Int = scalar(db, "PRAGMA user_version")

    private companion object {
        const val OUT_PROPERTY = "nimaz.corpus.out"

        /**
         * Content tables that a seeder used to fill and the artifact now carries whole. Kept as
         * an explicit list rather than derived from the schema so that a table dropped from the
         * artifact fails here by name instead of quietly leaving the set.
         */
        val CONTENT_TABLES = listOf(
            "translations",
            "mushaf_layout_lines",
            "mushaf_ayah_texts",
            "hadiths",
            "help_topic",
            "help_item",
            "help_step",
            "help_string",
            "duas",
            "dua_categories",
            "qaida_lessons",
            "qaida_letters",
            "qaida_cells",
            "qaida_lines",
        )

        /**
         * Written by the app at runtime, and since schemaVersion 23 not in this database at
         * all — they live in `NimazUserDatabase`. Kept as a list because "absent" is the
         * assertion now.
         */
        val USER_TABLES = listOf(
            "quran_bookmarks", "quran_favorites", "reading_progress", "khatams",
            "khatam_ayahs", "khatam_daily_log", "prayer_records", "fast_records",
            "makeup_fasts", "tasbih_sessions", "zakat_history", "locations",
            "hadith_bookmarks", "dua_bookmarks", "dua_progress", "prophet_bookmarks",
            "asma_ul_husna_bookmarks", "asma_un_nabi_bookmarks", "tafseer_notes",
            "tafseer_highlights",
        )
    }
}
