package com.arshadshah.nimaz.data.local.database

import com.arshadshah.nimaz.data.local.user.NIMAZ_USER_DATABASE_VERSION
import com.arshadshah.nimaz.data.local.user.NimazUserDatabase
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

/**
 * Pins Room's identity hash for both databases.
 *
 * ## Why a hash rather than a diff
 *
 * Room writes an `identityHash` into every exported schema and into the shipped database's
 * `room_master_table`. On open it compares them, and a mismatch throws
 * `IllegalStateException: Room cannot verify the data integrity` — **a hard crash on launch, for
 * every install**, not a silent or destructive migration. There is no
 * `fallbackToDestructiveMigration` anywhere in this project to soften it.
 *
 * The hash is computed over the sorted `createSql` of the entities, views and FTS tables plus the
 * setup queries. It contains no module name, no Gradle path and no KSP argument, so moving these
 * files between modules *cannot* change it — which is exactly why pinning the value is a useful
 * regression test and "diff the exported JSON byte-for-byte" is not:
 *
 *  - the byte diff is **noisier** than the property that matters (`formatVersion` and JSON key
 *    ordering can move with a Room upgrade while the hash is untouched), and
 *  - it has a real hole: a `room.schemaLocation` typo means the file is never regenerated, and a
 *    **stale file diffs clean**. That is a false green on the one check that matters.
 *
 * What could genuinely move the hash during a file move: an accidental entity edit while
 * resolving imports, or `@TypeConverters` resolving to a different converter set once converters
 * live in another module. Neither is what a byte diff tests. Both are what this catches.
 *
 * ## Why this test carries more weight than usual
 *
 * All fourteen migration tests are **instrumented**, and `android_instrumented_tests.yml` runs
 * per PR but the epic's artifact diffing is per milestone. This is the cheapest per-PR evidence
 * that a change to this module did not break every existing install, and it runs in
 * milliseconds without an emulator.
 */
class ExportedSchemaIdentityTest {

    private companion object {
        /**
         * The shipped v25 identity hash. Changing this constant is a deliberate act: it means
         * every device that already has the database will crash on open unless a migration to a
         * new version ships alongside.
         */
        const val CONTENT_V25_IDENTITY_HASH = "a16f41c537275480893fb6c31c4def37"
        const val USER_V1_IDENTITY_HASH = "e1d5df802dac215616583997c8ce3bdb"

        /** 36 `@Entity` plus the `AyahWithText` `@DatabaseView`. */
        const val CONTENT_ENTITY_COUNT = 36
        const val CONTENT_VIEW_COUNT = 1
        const val USER_ENTITY_COUNT = 15

        /**
         * `18.json` was never exported. Pre-existing — it predates the module split and is
         * deliberately not "fixed" here, because writing a schema file after the fact would mean
         * inventing one. Named explicitly so that a *second* gap fails this test rather than
         * widening a vague tolerance.
         */
        val KNOWN_MISSING_SCHEMA_VERSIONS = setOf(19)
    }

    // CWD for a module's unit tests is the module directory, and `schemas/` is a sibling of
    // `src/`. It moved here from `app/schemas` with the `room.schemaLocation` arg that writes it.
    private val schemaRoot = File("schemas")

    private fun schemaFor(databaseClass: Class<*>, version: Int): File =
        File(schemaRoot, "${databaseClass.canonicalName}/$version.json")

    // kotlinx.serialization rather than org.json: this is a plain JVM unit test, and Android's
    // org.json is a stub on that classpath — the same trap `ArabicSearchNormaliserTest` in
    // `:core:domain` documents. Parsing with it would throw, or worse, quietly return defaults.
    private fun schemaJson(databaseClass: Class<*>, version: Int): JsonObject {
        val file = schemaFor(databaseClass, version)
        assertWithMessage(
            "no exported schema at ${file.path} — has `room.schemaLocation` stopped pointing " +
                "at core/database/schemas? An un-exported schema is not a build failure, it is " +
                "a missing file that MigrationTestHelper only discovers on a device."
        ).that(file.isFile).isTrue()
        return Json.parseToJsonElement(file.readText()).jsonObject
            .getValue("database").jsonObject
    }

    @Test
    fun `the content database's identity hash is unchanged`() {
        val database = schemaJson(NimazDatabase::class.java, NIMAZ_DATABASE_VERSION)

        assertThat(database.getValue("version").jsonPrimitive.int)
            .isEqualTo(NIMAZ_DATABASE_VERSION)
        assertWithMessage(
            "The v$NIMAZ_DATABASE_VERSION identity hash changed. Room compares this against " +
                "room_master_table on open, so every existing install would throw " +
                "\"Room cannot verify the data integrity\" on launch. If an entity genuinely " +
                "changed, that needs a version bump and a migration, not a new constant here."
        ).that(database.getValue("identityHash").jsonPrimitive.content)
            .isEqualTo(CONTENT_V25_IDENTITY_HASH)
    }

    @Test
    fun `the user database's identity hash is unchanged`() {
        val database = schemaJson(NimazUserDatabase::class.java, NIMAZ_USER_DATABASE_VERSION)

        assertThat(database.getValue("version").jsonPrimitive.int)
            .isEqualTo(NIMAZ_USER_DATABASE_VERSION)
        assertThat(database.getValue("identityHash").jsonPrimitive.content)
            .isEqualTo(USER_V1_IDENTITY_HASH)
    }

    @Test
    fun `both databases still declare every entity and view they did`() {
        // A floor, in the spirit of #553's scan floors: an entity dropped while resolving imports
        // would move the hash, but this says *which* count moved, which is the faster diagnosis.
        val content = schemaJson(NimazDatabase::class.java, NIMAZ_DATABASE_VERSION)
        assertThat(content.getValue("entities").jsonArray.size).isEqualTo(CONTENT_ENTITY_COUNT)
        assertThat(content.getValue("views").jsonArray.size).isEqualTo(CONTENT_VIEW_COUNT)

        val user = schemaJson(NimazUserDatabase::class.java, NIMAZ_USER_DATABASE_VERSION)
        assertThat(user.getValue("entities").jsonArray.size).isEqualTo(USER_ENTITY_COUNT)
        assertThat(user["views"]?.jsonArray?.size ?: 0).isEqualTo(0)
    }

    @Test
    fun `every migration in ALL_MIGRATIONS has an exported schema to land on`() {
        // MigrationTestHelper opens `<version>.json` for the version a migration targets. A
        // migration whose target schema was never exported fails on a device, from a build that
        // compiled cleanly — so the gap is worth finding here.
        val targets = NimazDatabase.ALL_MIGRATIONS.map { it.endVersion }.toSortedSet()
        assertThat(targets).isNotEmpty()

        val missing = targets.filterNot { schemaFor(NimazDatabase::class.java, it).isFile }

        assertWithMessage(
            "These migration targets have no exported schema: $missing. " +
                "Known and accepted: $KNOWN_MISSING_SCHEMA_VERSIONS."
        ).that(missing.toSet()).isEqualTo(KNOWN_MISSING_SCHEMA_VERSIONS)
    }

    @Test
    fun `the migration chain is contiguous and ends at the current version`() {
        val migrations = NimazDatabase.ALL_MIGRATIONS.sortedBy { it.startVersion }

        migrations.zipWithNext { earlier, later ->
            assertWithMessage(
                "gap in the migration chain: ${earlier.startVersion}->${earlier.endVersion} " +
                    "is followed by ${later.startVersion}->${later.endVersion}"
            ).that(later.startVersion).isEqualTo(earlier.endVersion)
        }
        assertThat(migrations.last().endVersion).isEqualTo(NIMAZ_DATABASE_VERSION)
    }
}
