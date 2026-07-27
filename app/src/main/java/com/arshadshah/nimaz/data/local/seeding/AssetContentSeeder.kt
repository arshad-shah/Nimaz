package com.arshadshah.nimaz.data.local.seeding

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The shape shared by the content seeders that **replace a content table wholesale** from a
 * bundled, versioned JSON asset: parse, compare versions, replace atomically, record the
 * version — serialised by a mutex and short-circuited once this process has confirmed the
 * content is current.
 *
 * ## Why seeding exists at all
 * The prepopulated DB is only copied onto a device by `createFromAsset` on a **fresh install**,
 * and migrations only create empty tables. Content shipped in an update would therefore never
 * reach existing users. Seeding from a versioned asset makes fresh installs and upgrades
 * converge on the same content.
 *
 * ## What this does *not* cover
 * [com.arshadshah.nimaz.data.local.hadith.HadithBackfillSeeder] deliberately stays outside this
 * base class. It is not a replace-shaped seeder: it applies keyed per-row repairs, never
 * deletes, and gates on the *presence of gaps* (`emptyArabicCount() > 0`) rather than the
 * absence of rows. Forcing it into [replace] would change what it does, and its fast-path
 * would have to invert. It shares the mutex/version idea and nothing else.
 *
 * Mushaf layouts also stay separate ([com.arshadshah.nimaz.data.local.quran.QuranLayoutSeeder]):
 * they are seeded **per edition**, so the populated check, the version key and the "already
 * confirmed" flag are all keyed by layout id rather than being per-seeder singletons.
 *
 * @param T the parsed asset root.
 */
abstract class AssetContentSeeder<T> {

    /**
     * The [ContentVersionStore] key this seeder's version is recorded under. Must be unique —
     * two seeders sharing a key would each mark the other current and skip their own seed.
     */
    protected abstract val contentKey: String

    /** Path of the bundled asset, relative to `assets/`. */
    protected abstract val assetPath: String

    /** Reads the bundled asset's text. */
    protected abstract fun readAsset(path: String): String

    /** Parses the asset. Called before the version compare — see [versionOf]. */
    protected abstract fun parse(json: String): T

    /**
     * The content version carried by the parsed asset.
     *
     * This is a function of the parsed root rather than an abstract `val` on purpose: the
     * Dua/Help/Qaida assets are JSON objects with a `contentVersion` field, so their version
     * genuinely is not knowable without parsing first. A seeder whose asset has no version
     * field can return a constant here.
     */
    protected abstract fun versionOf(parsed: T): Int

    /** Whether the target tables already hold content. */
    protected abstract suspend fun isPopulated(): Boolean

    /** Atomically replaces the content tables. Implementations must be `@Transaction`. */
    protected abstract suspend fun replace(parsed: T)

    protected abstract val versionStore: ContentVersionStore

    private val mutex = Mutex()

    // Set once this process has confirmed the content is current, so repeated reads
    // short-circuit instead of retaking the mutex for a COUNT(*) and a DataStore read.
    @Volatile
    private var seeded = false

    /**
     * Seeds when the tables are empty or the bundled version is newer than the stored one.
     * Idempotent and safe to call on every read.
     */
    suspend fun seedIfNeeded() {
        if (seeded) return
        mutex.withLock {
            if (seeded) return@withLock
            val parsed = parse(readAsset(assetPath))
            val bundledVersion = versionOf(parsed)
            val stored = versionStore.get(contentKey)
            if (isPopulated() && stored >= bundledVersion) {
                seeded = true
                return@withLock
            }
            replace(parsed)
            versionStore.set(contentKey, bundledVersion)
            seeded = true
        }
    }
}
