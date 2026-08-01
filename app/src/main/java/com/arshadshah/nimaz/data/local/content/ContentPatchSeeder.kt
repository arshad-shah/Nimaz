package com.arshadshah.nimaz.data.local.content

import android.content.Context
import com.arshadshah.nimaz.data.local.database.NimazDatabase
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import java.util.zip.GZIPInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Applies the content patch that carries corrections to installs the artifact never reaches.
 *
 * Room's `createFromAsset` copies the prepackaged database onto a device **only on a fresh
 * install**. Correcting the artifact therefore reaches new installs and nobody else, which is
 * why every content fix in this app so far has needed its own bespoke seeder — one per table,
 * hand-written, with nothing checking that the fix matched what the artifact was supposed to
 * contain. `HadithBackfillSeeder`, which carried 379 repaired hadiths until it was retired at
 * versionCode 385 (`docs/retirement.yaml`), was the last such seeder and the shape this
 * generalises.
 *
 * The patch is a build output, not hand-authored. `nz patch emit` diffs the published baseline
 * artifact against the current one; `nz patch verify` then applies the result to the baseline
 * and asserts every collection's content hash equals the target's. A patch that does not
 * reconstruct the artifact cannot be published, so what arrives here is not a plausible fix —
 * it is the difference, proved.
 *
 * Three properties make applying it safe:
 *
 * 1. **It cannot touch user data.** Ops are only emitted for declared content collections;
 *    bookmarks, khatam progress and prayer records have no collection, so no op can name them.
 *    [USER_TABLES] re-asserts that here rather than trusting it, because the cost of being
 *    wrong is a user's data and the cost of the check is a set lookup.
 * 2. **It is cumulative from the baseline**, so which version a device upgrades from does not
 *    matter. Every op is an idempotent keyed write and re-applying is harmless.
 * 3. **It is version-gated**, so the common case costs one DataStore read.
 */
@Singleton
class ContentPatchSeeder @Inject constructor(
    private val database: NimazDatabase,
    private val versionStore: ContentPatchVersionStore,
    private val assetReader: ContentPatchAssetReader,
) {
    private val mutex = Mutex()

    /**
     * Applies the bundled patch if it is newer than what was last recorded.
     *
     * A missing asset is not an error: a release with nothing to correct ships no patch.
     */
    suspend fun seedIfNeeded(): ContentPatchResult = mutex.withLock {
        val raw = assetReader.read(ASSET_PATH) ?: return ContentPatchResult.NoPatch
        val patch = json.decodeFromString<ContentPatch>(raw)

        if (patch.format != SUPPORTED_FORMAT) {
            return ContentPatchResult.Unsupported(patch.format)
        }
        if (patch.patchVersion <= versionStore.get()) {
            return ContentPatchResult.AlreadyApplied(patch.patchVersion)
        }

        val forbidden = patch.tables.filter { it in USER_TABLES }
        if (forbidden.isNotEmpty()) {
            // Refuse the whole patch rather than the offending ops. A patch that reached a
            // user table is a broken emitter, and partially trusting it is worse than
            // trusting none of it.
            return ContentPatchResult.RefusedUserTable(forbidden)
        }

        val applied = database.runInTransaction<Int> { apply(patch) }
        versionStore.set(patch.patchVersion)
        ContentPatchResult.Applied(patch.patchVersion, applied)
    }

    private fun apply(patch: ContentPatch): Int {
        val db = database.openHelper.writableDatabase
        var count = 0
        patch.delete.forEach { op ->
            val (where, args) = op.key.toWhere()
            db.execSQL("DELETE FROM `${op.table}` WHERE $where", args)
            count++
        }
        patch.insert.forEach { op ->
            val cols = op.row.keys.toList()
            val names = cols.joinToString(", ") { "`$it`" }
            val holes = cols.joinToString(", ") { "?" }
            db.execSQL(
                "INSERT OR REPLACE INTO `${op.table}` ($names) VALUES ($holes)",
                cols.map { op.row.getValue(it).toBindable() }.toTypedArray(),
            )
            count++
        }
        patch.update.forEach { op ->
            val cols = op.set.keys.toList()
            val assigns = cols.joinToString(", ") { "`$it` = ?" }
            val (where, args) = op.key.toWhere()
            db.execSQL(
                "UPDATE `${op.table}` SET $assigns WHERE $where",
                (cols.map { op.set.getValue(it).toBindable() } + args).toTypedArray(),
            )
            count++
        }
        return count
    }

    /**
     * Builds the WHERE clause from the op's own key.
     *
     * The key is a column→value map rather than a positional tuple, and it carries more than
     * the collection's natural key: a shared table splits many editions by a discriminator, so
     * `translations` holds fifteen rows per ayah and `WHERE ayah_id = ?` would rewrite all
     * fifteen with one edition's text. The emitter puts the discriminator in the key; this
     * binds whatever it was given and nothing more.
     */
    private fun Map<String, JsonElement>.toWhere(): Pair<String, Array<Any?>> {
        require(isNotEmpty()) { "patch op has no key — refusing to match every row" }
        val cols = keys.sorted()
        return cols.joinToString(" AND ") { "`$it` = ?" } to
            cols.map { getValue(it).toBindable() }.toTypedArray()
    }

    private fun JsonElement.toBindable(): Any? {
        if (this is JsonNull) return null
        val primitive = this as? JsonPrimitive
            ?: throw IllegalArgumentException("patch values must be primitives, got $this")
        if (primitive.isString) return primitive.content
        return primitive.content.toLongOrNull()
            ?: primitive.content.toDoubleOrNull()
            ?: when (primitive.content) {
                "true" -> 1L
                "false" -> 0L
                else -> primitive.content
            }
    }

    companion object {
        /**
         * No `.gz` — AGP strips the extension and decompresses `.gz` assets at packaging
         * time, so a file staged as `content-patch.json.gz` arrives as `content-patch.json`.
         * The reader sniffs for the gzip magic bytes rather than trusting either name.
         */
        const val ASSET_PATH = "database/content-patch.json"
        const val SUPPORTED_FORMAT = 1

        private val json = Json { ignoreUnknownKeys = true; isLenient = true }

        /**
         * Tables the app writes to at runtime. Mirrors `user_tables` in the data console's
         * `console.yaml`; kept here so the refusal does not depend on the emitter being right.
         */
        val USER_TABLES = setOf(
            "reading_progress", "quran_bookmarks", "quran_favorites", "hadith_bookmarks",
            "dua_bookmarks", "dua_progress", "prayer_records", "fast_records", "makeup_fasts",
            "khatams", "khatam_ayahs", "khatam_daily_log", "tasbih_sessions", "zakat_history",
            "tafseer_highlights", "tafseer_notes", "locations", "asma_ul_husna_bookmarks",
            "asma_un_nabi_bookmarks", "prophet_bookmarks", "qaida_lesson_progress",
            "qaida_cell_progress",
        )
    }
}

/** What a run did, so callers and tests can assert on it instead of on side effects. */
sealed interface ContentPatchResult {
    data object NoPatch : ContentPatchResult
    data class AlreadyApplied(val version: Int) : ContentPatchResult
    data class Applied(val version: Int, val ops: Int) : ContentPatchResult
    data class Unsupported(val format: Int) : ContentPatchResult
    data class RefusedUserTable(val tables: List<String>) : ContentPatchResult
}

@Serializable
data class ContentPatch(
    val format: Int = 0,
    @SerialName("patchVersion") val patchVersion: Int = 0,
    val baseline: String = "",
    val target: String = "",
    val tables: List<String> = emptyList(),
    val update: List<UpdateOp> = emptyList(),
    val insert: List<InsertOp> = emptyList(),
    val delete: List<DeleteOp> = emptyList(),
)

@Serializable
data class UpdateOp(
    val collection: String = "",
    val table: String,
    val key: Map<String, JsonElement>,
    val set: Map<String, JsonElement>,
)

@Serializable
data class InsertOp(
    val collection: String = "",
    val table: String,
    val key: Map<String, JsonElement> = emptyMap(),
    val row: Map<String, JsonElement>,
)

@Serializable
data class DeleteOp(
    val collection: String = "",
    val table: String,
    val key: Map<String, JsonElement>,
)

/** Reads the gzipped patch asset. Abstracted so the seeder is testable without Android. */
interface ContentPatchAssetReader {
    /** Returns null when the release ships no patch. */
    fun read(path: String): String?
}

@Singleton
class AndroidContentPatchAssetReader @Inject constructor(
    @ApplicationContext private val context: Context,
) : ContentPatchAssetReader {
    override fun read(path: String): String? = runCatching {
        context.assets.open(path).use { raw ->
            val stream = java.io.BufferedInputStream(raw)
            stream.mark(2)
            val magic = ByteArray(2).also { stream.read(it) }
            stream.reset()
            val gzipped = magic[0] == 0x1f.toByte() && magic[1] == 0x8b.toByte()
            if (gzipped) {
                GZIPInputStream(stream).bufferedReader().use { it.readText() }
            } else {
                stream.bufferedReader().use { it.readText() }
            }
        }
    }.getOrNull()
}

interface ContentPatchVersionStore {
    suspend fun get(): Int
    suspend fun set(version: Int)
}

@Singleton
class DataStoreContentPatchVersionStore @Inject constructor(
    private val prefs: PreferencesDataStore,
) : ContentPatchVersionStore {
    override suspend fun get(): Int = prefs.contentPatchVersion.first()

    override suspend fun set(version: Int) {
        prefs.setContentPatchVersion(version)
    }
}
