package com.arshadshah.nimaz.data.local.quran

import android.content.Context
import com.arshadshah.nimaz.data.local.database.dao.QuranDao
import com.arshadshah.nimaz.data.local.database.entity.MushafLayoutEntity
import com.arshadshah.nimaz.data.local.seeding.ContentVersionStore
import com.arshadshah.nimaz.domain.model.quran.catalogue.MushafLayoutEdition
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** Reads a bundled asset's text. Abstracted so the seeder is unit-testable without Android. */
interface QuranAssetReader {
    fun read(path: String): String
}

@Singleton
class AndroidQuranAssetReader @Inject constructor(
    @ApplicationContext private val context: Context
) : QuranAssetReader {
    override fun read(path: String): String =
        context.assets.open(path).bufferedReader().use { it.readText() }
}

/**
 * Populates a line-accurate Mushaf edition's data from its bundled JSON assets:
 *  - the edition's rows in the `mushaf_layouts` table (its printed page/line layout), and
 *  - where the edition needs one, the ayah text column its word positions index into
 *    (today only the nullable `ayahs.text_indopak`).
 *
 * Which assets an edition uses is declared in [QuranContentAssets.mushafLayouts], so this
 * seeder is edition-agnostic: adding a layout adds a catalogue entry and an asset binding,
 * not a seeder (ADR-001/002).
 *
 * ## Why seed at runtime instead of shipping it in the prepackaged DB
 * The prepackaged DB (`assets/database/nimaz_prepopulated.db`) is a ~147 MB Git-LFS blob
 * that Room copies with `createFromAsset` **only on a fresh install** — it is never
 * re-copied on upgrade. Regenerating it to embed layout data would both bloat the LFS
 * asset by tens of MB and still not reach existing installs. Seeding from versioned JSON
 * assets — exactly as [com.arshadshah.nimaz.data.local.dua.DuaContentSeeder],
 * [com.arshadshah.nimaz.data.local.help.HelpContentSeeder] and
 * [com.arshadshah.nimaz.data.local.qaida.QaidaContentSeeder] do — makes fresh installs and
 * upgrades converge on the same content, and the compressed APK cost is under ~1 MB.
 *
 * Idempotent and content-version aware per edition: an edition re-seeds only when its rows
 * are absent or when its bundled content version is newer than what was last stored. The
 * write is atomic (see [QuranDao.replaceMushafLayout]) and scoped to that edition's rows
 * plus `text_indopak`, so nothing else in the Quran data is disturbed.
 */
@Singleton
class QuranLayoutSeeder @Inject constructor(
    private val dao: QuranDao,
    private val versionStore: ContentVersionStore,
    private val assetReader: QuranAssetReader
) {
    private val mutex = Mutex()

    // Set per edition once this process has confirmed that edition's data is current, so
    // every later page fetch (getMushafPageLayout runs this on each call) short-circuits
    // instead of retaking the mutex for a DB count + DataStore read every time (#280 review).
    private val seeded = ConcurrentHashMap<String, Boolean>()

    /**
     * Seeds [edition] if its rows are missing or stale. A no-op for a flowed edition, which
     * paginates by the `ayahs.page` column and has no layout rows at all.
     */
    suspend fun seedIfNeeded(edition: MushafLayoutEdition) {
        if (!edition.hasLineLayout) return
        if (seeded[edition.id] == true) return
        val assets = QuranContentAssets.mushafLayouts[edition.id] ?: return
        mutex.withLock {
            if (seeded[edition.id] == true) return@withLock
            val stored = versionStore.get(contentKey(edition.id))
            val populated = dao.countMushafLayout(edition.id) > 0
            if (populated && stored >= assets.layout.contentVersion) {
                seeded[edition.id] = true
                return@withLock
            }
            seed(edition.id, assets)
            versionStore.set(contentKey(edition.id), assets.layout.contentVersion)
            seeded[edition.id] = true
        }
    }

    private suspend fun seed(layoutId: String, assets: QuranContentAssets.LayoutAssets) {
        // An edition that reads a text column the app already ships (text_uthmani) has no
        // text asset — only its layout rows are seeded.
        val ayahTexts = assets.ayahText?.let { binding ->
            quranIndopakJson.decodeFromString(
                ListSerializer(IndopakAyahDto.serializer()),
                assetReader.read(binding.assetPath)
            ).associate { it.ayahId to it.textIndopak }
        } ?: emptyMap()

        val layout = quranIndopakJson.decodeFromString(
            ListSerializer(IndopakLayoutRowDto.serializer()),
            assetReader.read(assets.layout.assetPath)
        )
        val rows = layout.map {
            MushafLayoutEntity(
                layoutId = layoutId,
                page = it.pageNumber,
                line = it.lineNumber,
                lineType = it.lineType,
                surahId = it.surahId,
                ayahId = it.ayahId,
                firstWordPosition = it.firstWordPosition,
                lastWordPosition = it.lastWordPosition
            )
        }
        dao.replaceMushafLayout(layoutId, ayahTexts, rows)
    }

    companion object {
        /** The [ContentVersionStore] key an edition's seeded content version is stored under. */
        fun contentKey(layoutId: String): String = "mushaf_layout.$layoutId"
    }
}
