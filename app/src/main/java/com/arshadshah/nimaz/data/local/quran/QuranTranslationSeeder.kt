package com.arshadshah.nimaz.data.local.quran

import com.arshadshah.nimaz.data.local.database.dao.QuranDao
import com.arshadshah.nimaz.data.local.database.entity.TranslationEntity
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
import com.arshadshah.nimaz.domain.model.QuranTranslation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Populates the `translations` table from the bundled
 * `assets/quran/translations/<id>.json` files, one translation at a time.
 *
 * ## Why seed at runtime
 * Same reason as [QuranIndopakSeeder]: the prepackaged DB
 * (`assets/database/nimaz_prepopulated.db`) is a ~147 MB Git-LFS blob that Room copies with
 * `createFromAsset` **only on a fresh install**, so translations baked into it would never
 * reach anyone upgrading. Shipping them as versioned JSON assets makes fresh installs and
 * upgrades converge on the same content.
 *
 * ## Why lazily, per translation
 * The catalogue is 15 translations × 6,236 verses. Seeding all of them eagerly would write
 * ~94k rows and tens of MB into the DB for content almost every user will never open. So a
 * translation is seeded the first time it is actually *selected* — [seedIfNeeded] is called
 * on the read path, mirroring how [QuranIndopakSeeder] is driven by the first IndoPak page
 * fetch. A single translation is ~6k rows and seeds in well under a second.
 *
 * Idempotent and content-version aware: it re-seeds a translation only when that
 * translation's rows are missing or when the asset's `contentVersion` is newer than the
 * version last stored for it. The write is atomic per translation and scoped to that
 * `translator_id` (see [QuranDao.replaceTranslation]), so seeding one never disturbs
 * another — or any user data.
 */
@Singleton
class QuranTranslationSeeder @Inject constructor(
    private val dao: QuranDao,
    private val versionStore: TranslationContentVersionStore,
    private val assetReader: QuranAssetReader
) {
    private val mutex = Mutex()

    // Translations confirmed current in this process, so repeat reads (every page fetch asks)
    // short-circuit instead of retaking the mutex for a COUNT + DataStore read each time.
    private val seeded = mutableSetOf<String>()

    /**
     * Ensures [translation]'s verses are present and current. Safe to call on every read;
     * the common case is a set lookup.
     */
    suspend fun seedIfNeeded(translation: QuranTranslation) {
        if (translation.id in seeded) return
        mutex.withLock {
            if (translation.id in seeded) return@withLock
            val asset = readAsset(translation)
            val stored = versionStore.get(translation.id)
            val populated = dao.countTranslationsFor(translation.id) == EXPECTED_AYAH_COUNT
            if (populated && stored >= asset.contentVersion) {
                seeded += translation.id
                return@withLock
            }
            seed(translation, asset)
            versionStore.set(translation.id, asset.contentVersion)
            seeded += translation.id
        }
    }

    private fun readAsset(translation: QuranTranslation): TranslationAssetDto {
        val dto = quranTranslationJson.decodeFromString(
            TranslationAssetDto.serializer(),
            assetReader.read(assetPath(translation))
        )
        require(dto.texts.size == EXPECTED_AYAH_COUNT) {
            "translation ${translation.id}: asset has ${dto.texts.size} verses, " +
                    "expected $EXPECTED_AYAH_COUNT"
        }
        require(dto.translationId == translation.id) {
            "translation ${translation.id}: asset declares id '${dto.translationId}'"
        }
        return dto
    }

    private suspend fun seed(translation: QuranTranslation, asset: TranslationAssetDto) {
        // index i -> global ayah id i + 1; see TranslationAssetDto.
        val rows = asset.texts.mapIndexed { index, text ->
            TranslationEntity(
                ayahId = index + 1,
                text = text,
                translatorId = translation.id
            )
        }
        dao.replaceTranslation(translation.id, rows)
    }

    companion object {
        const val EXPECTED_AYAH_COUNT = 6236

        fun assetPath(translation: QuranTranslation): String =
            "quran/translations/${translation.id}.json"
    }
}

/** Thin abstraction over the DataStore key so the seeder is unit-testable. */
interface TranslationContentVersionStore {
    suspend fun get(translationId: String): Int
    suspend fun set(translationId: String, version: Int)
}

/**
 * Stores per-translation seeded versions in one DataStore string-set, as `"<id>:<version>"`
 * entries. A set keeps the whole catalogue in a single preference key instead of minting a
 * new key per translation, so adding a translation needs no DataStore change at all.
 */
@Singleton
class DataStoreTranslationContentVersionStore @Inject constructor(
    private val prefs: PreferencesDataStore
) : TranslationContentVersionStore {

    override suspend fun get(translationId: String): Int =
        prefs.quranTranslationVersions.first()
            .firstOrNull { it.substringBefore(SEPARATOR) == translationId }
            ?.substringAfter(SEPARATOR)
            ?.toIntOrNull()
            ?: 0

    override suspend fun set(translationId: String, version: Int) {
        val current = prefs.quranTranslationVersions.first()
        val without = current.filterNot { it.substringBefore(SEPARATOR) == translationId }
        prefs.setQuranTranslationVersions(without.toSet() + "$translationId$SEPARATOR$version")
    }

    private companion object {
        const val SEPARATOR = ':'
    }
}
