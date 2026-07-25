package com.arshadshah.nimaz.data.local.quran

import android.content.Context
import com.arshadshah.nimaz.data.local.database.dao.QuranDao
import com.arshadshah.nimaz.data.local.database.entity.MushafLayoutIndopak16Entity
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
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
 * Populates the 16-line IndoPak Quran data (sub-task 2/7 of #263):
 *  - the nullable `ayahs.text_indopak` column, and
 *  - the `mushaf_layout_indopak16` table (the line-accurate 548-page layout),
 * from the bundled assets `quran/ayahs_indopak.json` + `quran/mushaf_layout_indopak16.json`.
 *
 * ## Why seed at runtime instead of shipping it in the prepackaged DB
 * The prepackaged DB (`assets/database/nimaz_prepopulated.db`) is a ~147 MB Git-LFS blob
 * that Room copies with `createFromAsset` **only on a fresh install** — it is never
 * re-copied on upgrade. Regenerating it to embed the IndoPak data would both bloat the LFS
 * asset by tens of MB and still not reach existing installs. Seeding from versioned JSON
 * assets — exactly as [com.arshadshah.nimaz.data.local.dua.DuaContentSeeder],
 * [com.arshadshah.nimaz.data.local.help.HelpContentSeeder] and
 * [com.arshadshah.nimaz.data.local.qaida.QaidaContentSeeder] do — makes fresh installs and
 * upgrades converge on the same content, and the compressed APK cost is under ~1 MB.
 *
 * Idempotent and content-version aware: it re-seeds only when the layout table is empty or
 * when [INDOPAK_CONTENT_VERSION] is newer than what was last stored. The write is atomic
 * (see [QuranDao.replaceMushafIndopak16]) and touches only `text_indopak` + the layout
 * table, so nothing else in the Quran data is disturbed.
 */
@Singleton
class QuranIndopakSeeder @Inject constructor(
    private val dao: QuranDao,
    private val versionStore: IndopakContentVersionStore,
    private val assetReader: QuranAssetReader
) {
    private val mutex = Mutex()

    // Set once this process has confirmed the data is current, so every later page fetch
    // (getMushafPageLayout runs this on each call) short-circuits instead of retaking the
    // mutex for a DB count + DataStore read every time (#280 review).
    @Volatile
    private var seeded = false

    suspend fun seedIfNeeded() {
        if (seeded) return
        mutex.withLock {
            if (seeded) return@withLock
            val stored = versionStore.get()
            val populated = dao.countMushafLayoutIndopak16() > 0
            if (populated && stored >= INDOPAK_CONTENT_VERSION) {
                seeded = true
                return@withLock
            }
            seed()
            versionStore.set(INDOPAK_CONTENT_VERSION)
            seeded = true
        }
    }

    private suspend fun seed() {
        val ayahs = quranIndopakJson.decodeFromString(
            ListSerializer(IndopakAyahDto.serializer()),
            assetReader.read(AYAHS_ASSET)
        )
        val layout = quranIndopakJson.decodeFromString(
            ListSerializer(IndopakLayoutRowDto.serializer()),
            assetReader.read(LAYOUT_ASSET)
        )

        val ayahTexts = ayahs.associate { it.ayahId to it.textIndopak }
        val rows = layout.map {
            MushafLayoutIndopak16Entity(
                page = it.pageNumber,
                line = it.lineNumber,
                lineType = it.lineType,
                surahId = it.surahId,
                ayahId = it.ayahId,
                firstWordPosition = it.firstWordPosition,
                lastWordPosition = it.lastWordPosition
            )
        }
        dao.replaceMushafIndopak16(ayahTexts, rows)
    }

    companion object {
        const val AYAHS_ASSET = "quran/ayahs_indopak.json"
        const val LAYOUT_ASSET = "quran/mushaf_layout_indopak16.json"

        /**
         * Bump this whenever the bundled IndoPak assets change so existing installs re-seed
         * on update. `0` means "never seeded"; the first shipped version is `1`.
         */
        const val INDOPAK_CONTENT_VERSION = 1
    }
}

/** Thin abstraction over the DataStore version key so the seeder is unit-testable. */
interface IndopakContentVersionStore {
    suspend fun get(): Int
    suspend fun set(version: Int)
}

@Singleton
class DataStoreIndopakContentVersionStore @Inject constructor(
    private val prefs: PreferencesDataStore
) : IndopakContentVersionStore {
    override suspend fun get(): Int = prefs.indopakContentVersion.first()
    override suspend fun set(version: Int) = prefs.setIndopakContentVersion(version)
}
