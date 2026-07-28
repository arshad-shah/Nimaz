package com.arshadshah.nimaz.data.corpus

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.data.local.database.NIMAZ_DATABASE_VERSION
import com.arshadshah.nimaz.data.local.database.NimazDatabase
import com.arshadshah.nimaz.data.local.dua.DuaAssetReader
import com.arshadshah.nimaz.data.local.dua.DuaContentSeeder
import com.arshadshah.nimaz.data.local.dua.DuaContentVersionStore
import com.arshadshah.nimaz.data.local.hadith.HadithAssetReader
import com.arshadshah.nimaz.data.local.hadith.HadithBackfillSeeder
import com.arshadshah.nimaz.data.local.hadith.HadithBackfillVersionStore
import com.arshadshah.nimaz.data.local.help.HelpAssetReader
import com.arshadshah.nimaz.data.local.help.HelpContentSeeder
import com.arshadshah.nimaz.data.local.help.HelpContentVersionStore
import com.arshadshah.nimaz.data.local.qaida.QaidaAssetReader
import com.arshadshah.nimaz.data.local.qaida.QaidaContentSeeder
import com.arshadshah.nimaz.data.local.qaida.QaidaContentVersionStore
import com.arshadshah.nimaz.data.local.quran.MushafContentVersionStore
import com.arshadshah.nimaz.data.local.quran.MushafLayoutSeeder
import com.arshadshah.nimaz.data.local.quran.QuranAssetReader
import com.arshadshah.nimaz.data.local.quran.QuranTranslationSeeder
import com.arshadshah.nimaz.data.local.quran.TranslationContentVersionStore
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
 * Manufactures the database a real device actually holds, and proves it is what the data
 * console's corpus claims.
 *
 * ## Why this exists
 *
 * `assets/database/nimaz_prepopulated.db` is stamped `user_version = 12`. The app is at 20.
 * The eight migrations in between, plus six content seeders, are what turn the shipped asset
 * into the data a user sees — 15 translations, 4 Mushaf editions, 379 repaired hadiths, and
 * the Help, Dua and Qaida content, none of which are in the asset.
 *
 * The `nz` console compiles its corpus from NDJSON sources and its whole claim to authority is
 * that the corpus *is* what devices hold. Sealing the v12 asset alone would make that claim
 * false by about 31 MB. The alternative — reimplementing the migrations and seeders in Python
 * inside `nz` — would be a second implementation of exactly the thing whose fidelity is the
 * question, and it could drift silently while every hash check still passed.
 *
 * So this runs the production code: the real [NimazDatabase.ALL_MIGRATIONS] objects, the real
 * seeders, the real bundled assets. Whatever they do to the bytes, including anything
 * surprising, is what the vault gets sealed from.
 *
 * ## Two jobs
 *
 * As a **test** it asserts the invariants that must hold for the corpus to be shippable.
 * As a **harness**, given `-Dnimaz.corpus.out=<path>`, it writes the resulting database out
 * and prints its sha256, which is then `nz vault seal`-ed. Re-running it on a later release
 * answers the question nothing currently asks: does the console's corpus still equal real
 * device state?
 */
@RunWith(RobolectricTestRunner::class)
class DeviceStateCorpusTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `device state is the asset plus every migration and every seeder`() = runTest {
        val db = openMigratedFromAsset()
        try {
            seedEverything(db)

            // The seeded tables are the point of the exercise: if any is empty, the corpus
            // would ship missing content that devices have, which is the failure this whole
            // harness exists to make impossible.
            val counts = seededRowCounts(db)
            for ((table, rows) in counts) {
                assertThat("$table=$rows").isNotEqualTo("$table=0")
            }

            // User tables must stay empty. They are part of the schema and none of the
            // corpus; a single row here would ship one user's bookmarks to everybody.
            for (table in USER_TABLES) {
                assertThat("$table=${rowCount(db, table)}").isEqualTo("$table=0")
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
     * Opens the shipped asset through Room exactly as the app does, so migrations 12 -> 20 run
     * as production `Migration` objects rather than as a description of them.
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

    /**
     * Runs all six seeders to completion.
     *
     * Two of them are lazy in production — [MushafLayoutSeeder] seeds per script on first use
     * and [QuranTranslationSeeder] per translation on first read — so a real device may hold
     * only the subset the user has opened. The corpus has to contain all of it, so every
     * enum entry is seeded here. That difference is exactly why existing installs cannot be
     * assumed to be at full v20 content, and why the seeders survive one more release before
     * being retired (see docs/DATA_RETIREMENT.md).
     */
    private suspend fun seedEverything(db: NimazDatabase) {
        val assets = AssetReader(context)

        HelpContentSeeder(db.helpDao(), MemoryVersion(), assets).seedIfNeeded()
        DuaContentSeeder(db.duaDao(), MemoryVersion(), assets).seedIfNeeded()
        QaidaContentSeeder(db.qaidaDao(), MemoryVersion(), assets).seedIfNeeded()
        HadithBackfillSeeder(db.hadithDao(), MemoryVersion(), assets).seedIfNeeded()

        val mushaf = MushafLayoutSeeder(db.quranDao(), MemoryKeyedVersion(), assets)
        for (script in MushafScript.entries) mushaf.seedIfNeeded(script)

        val translations = QuranTranslationSeeder(db.quranDao(), MemoryKeyedVersion(), assets)
        for (translation in QuranTranslation.entries) translations.seedIfNeeded(translation)
    }

    private fun seededRowCounts(db: NimazDatabase): Map<String, Int> =
        SEEDED_TABLES.associateWith { rowCount(db, it) }

    /**
     * Writes the corpus out for `nz vault seal`.
     *
     * VACUUM first: the file is about to become the identity of every artifact built from it,
     * and free-list state left over from seeding is noise that would differ run to run.
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

    private fun rowCount(db: NimazDatabase, table: String): Int =
        db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM `$table`").use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }

    private fun userVersion(db: NimazDatabase): Int =
        db.openHelper.readableDatabase.query("PRAGMA user_version").use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }

    private class AssetReader(private val context: Context) :
        QuranAssetReader, HelpAssetReader, DuaAssetReader, QaidaAssetReader, HadithAssetReader {
        override fun read(path: String): String =
            context.assets.open(path).bufferedReader().use { it.readText() }
    }

    /** The seeders' version gates are irrelevant here — everything must run, every time. */
    private class MemoryVersion :
        HelpContentVersionStore, DuaContentVersionStore, QaidaContentVersionStore,
        HadithBackfillVersionStore {
        private var value = 0
        override suspend fun get(): Int = value
        override suspend fun set(version: Int) {
            value = version
        }
    }

    private class MemoryKeyedVersion : MushafContentVersionStore, TranslationContentVersionStore {
        private val values = mutableMapOf<String, Int>()
        override suspend fun get(script: String): Int = values[script] ?: 0
        override suspend fun set(script: String, version: Int) {
            values[script] = version
        }
    }

    private companion object {
        const val OUT_PROPERTY = "nimaz.corpus.out"

        /** Tables filled by a seeder rather than by the shipped asset. */
        val SEEDED_TABLES = listOf(
            "translations",
            "mushaf_layout_lines",
            "mushaf_ayah_texts",
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

        /** Written by the app at runtime. Part of the schema, none of the corpus. */
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
