package com.arshadshah.nimaz.data.local.quran

import com.arshadshah.nimaz.data.local.database.dao.QuranDao
import com.arshadshah.nimaz.data.local.database.entity.TranslationEntity
import com.arshadshah.nimaz.domain.model.quran.catalogue.TranslationEdition
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One bundled translation asset.
 *
 * [texts] is **positional**: index `i` holds the text of global ayah id `i + 1`, all 6,236 of
 * them in mushaf order. Storing the ids alongside would roughly double the asset for no
 * information, and the pipeline's validators (`fetch_edition.py`) already guarantee the rows
 * are complete, unique, non-blank and ordered before the file is written — so the position
 * *is* the id. [QuranTranslationSeeder] re-checks the count on the way in, because an asset
 * that silently lost rows would otherwise shift every verse after the gap.
 */
@Serializable
data class TranslationAssetDto(
    val contentVersion: Int,
    val translatorId: String,
    val tanzilEdition: String = "",
    val sourceLastUpdate: String = "",
    val texts: List<String>
)

internal val quranTranslationJson = Json { ignoreUnknownKeys = true }

/**
 * Seeds one bundled translation into the `translations` table, on first use of that edition.
 *
 * ## Why translations are seeded rather than shipped in the DB
 * Same reason as every other seeder: `assets/database/nimaz_prepopulated.db` is a ~147 MB
 * Git-LFS blob that `createFromAsset` copies **only on a fresh install**. A translation added
 * to it would never reach anyone who already has the app. Seeding from a versioned asset makes
 * fresh installs and upgrades converge, for ~0.3 MB compressed per translation.
 *
 * ## Scoping
 * Everything is per translation: the populated check, the content-version key
 * (`translation.<id>`) and the in-process "already confirmed" flag. Seeding one edition never
 * touches another's rows — including `sahih_international`, which ships inside the
 * prepopulated DB and has no asset at all.
 *
 * This is not an [com.arshadshah.nimaz.data.local.seeding.AssetContentSeeder] subclass for the
 * same reason the layout seeder isn't: that base class models one seeder owning one content
 * key, and this one owns a key per edition.
 */
@Singleton
class QuranTranslationSeeder @Inject constructor(
    private val dao: QuranDao,
    private val versionStore: com.arshadshah.nimaz.data.local.seeding.ContentVersionStore,
    private val assetReader: QuranAssetReader
) {
    private val mutex = Mutex()

    // Per edition, so a reader switching translations doesn't re-check an edition already
    // confirmed current. Translation reads happen on every page/surah fetch.
    private val seeded = ConcurrentHashMap<String, Boolean>()

    /**
     * Seeds [edition] if its rows are missing or stale. A no-op for an edition with no bundled
     * asset — today that is `sahih_international`, whose rows come from the prepopulated DB.
     */
    suspend fun seedIfNeeded(edition: TranslationEdition) {
        if (seeded[edition.id] == true) return
        val binding = QuranContentAssets.translations[edition.id] ?: return
        mutex.withLock {
            if (seeded[edition.id] == true) return@withLock
            val stored = versionStore.get(contentKey(edition.id))
            val populated = dao.countTranslations(edition.id) > 0
            if (populated && stored >= binding.contentVersion) {
                seeded[edition.id] = true
                return@withLock
            }
            seed(edition.id, binding.assetPath)
            versionStore.set(contentKey(edition.id), binding.contentVersion)
            seeded[edition.id] = true
        }
    }

    private suspend fun seed(translatorId: String, assetPath: String) {
        val dto = quranTranslationJson.decodeFromString(
            TranslationAssetDto.serializer(),
            assetReader.read(assetPath)
        )
        require(dto.texts.size == TOTAL_AYAHS) {
            // Positional ids only work if the asset is complete. A short file would shift
            // every verse after the gap onto the wrong ayah — wrong, and silently so.
            "translation asset $assetPath has ${dto.texts.size} verses, expected $TOTAL_AYAHS"
        }
        require(dto.translatorId == translatorId) {
            // Guards a mis-copied asset: the file names the edition it holds, and it must be
            // the edition we are about to write it under.
            "translation asset $assetPath declares '${dto.translatorId}', expected '$translatorId'"
        }
        val rows = dto.texts.mapIndexed { index, text ->
            TranslationEntity(
                ayahId = index + 1,
                text = text,
                translatorId = translatorId
            )
        }
        dao.replaceTranslations(translatorId, rows)
    }

    companion object {
        const val TOTAL_AYAHS = 6236

        /** The [ContentVersionStore] key an edition's seeded version is stored under. */
        fun contentKey(translatorId: String): String = "translation.$translatorId"
    }
}
