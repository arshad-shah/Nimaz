package com.arshadshah.nimaz.data.local.quran

import android.content.Context
import com.arshadshah.nimaz.data.local.database.dao.QuranDao
import com.arshadshah.nimaz.data.local.database.entity.MushafAyahTextEntity
import com.arshadshah.nimaz.data.local.database.entity.MushafLayoutLineEntity
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
import com.arshadshah.nimaz.domain.model.MushafScript
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
import javax.inject.Inject
import javax.inject.Singleton

/** Reads a bundled asset's text. Abstracted so the seeders are unit-testable without Android. */
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
 * Populates a line-accurate Mushaf edition — its glyph text (`mushaf_ayah_texts`) and its
 * printed line breaks (`mushaf_layout_lines`) — from the bundled
 * `assets/quran/mushaf/` assets.
 *
 * ## Why seed at runtime instead of shipping it in the prepackaged DB
 * The prepackaged DB (`assets/database/nimaz_prepopulated.db`) is a ~147 MB Git-LFS blob
 * that Room copies with `createFromAsset` **only on a fresh install** — it is never
 * re-copied on upgrade. Baking editions into it would both bloat the LFS asset and still not
 * reach existing installs. Seeding from versioned JSON assets — exactly as
 * [com.arshadshah.nimaz.data.local.dua.DuaContentSeeder],
 * [com.arshadshah.nimaz.data.local.help.HelpContentSeeder] and
 * [com.arshadshah.nimaz.data.local.qaida.QaidaContentSeeder] do — makes fresh installs and
 * upgrades converge on the same content, at a compressed APK cost well under a megabyte per
 * edition.
 *
 * ## Why per edition
 * Each edition is ~14k layout rows plus, for a new text source, 6,236 glyph rows. Seeding
 * every edition eagerly would write several times that for content most users never open, so
 * an edition is seeded the first time it is actually displayed. Editions that share a text
 * source ([MushafScript.textSource]) rewrite identical glyph rows, which is cheap and keeps
 * each seed self-contained.
 *
 * Idempotent and content-version aware: it re-seeds an edition only when its rows are
 * missing or when [CONTENT_VERSION] is newer than the version last stored for it. The write
 * is atomic (see [QuranDao.replaceMushafLayout]) and scoped by script + text source, so
 * nothing else in the Quran data is disturbed.
 */
@Singleton
class MushafLayoutSeeder @Inject constructor(
    private val dao: QuranDao,
    private val versionStore: MushafContentVersionStore,
    private val assetReader: QuranAssetReader
) {
    private val mutex = Mutex()

    // Editions confirmed current in this process, so every later page fetch (the reader asks
    // on each one) short-circuits instead of retaking the mutex for a COUNT + DataStore read.
    private val seeded = mutableSetOf<MushafScript>()

    /**
     * Ensures [script]'s text and layout are present and current. A no-op for ayah-flow
     * editions such as [MushafScript.MADANI], which have no stored layout at all.
     */
    suspend fun seedIfNeeded(script: MushafScript) {
        if (!script.isLineAccurate || script in seeded) return
        val textSource = script.textSource ?: return
        mutex.withLock {
            if (script in seeded) return@withLock
            val stored = versionStore.get(script.name)
            val populated = dao.countLayoutLines(script.name) > 0 &&
                    dao.countAyahTexts(textSource) == EXPECTED_AYAH_COUNT
            if (populated && stored >= CONTENT_VERSION) {
                seeded += script
                return@withLock
            }
            seed(script, textSource)
            versionStore.set(script.name, CONTENT_VERSION)
            seeded += script
        }
    }

    private suspend fun seed(script: MushafScript, textSource: String) {
        val texts = mushafLayoutJson.decodeFromString(
            ListSerializer(MushafAyahTextDto.serializer()),
            assetReader.read(textAsset(textSource))
        )
        val layout = mushafLayoutJson.decodeFromString(
            ListSerializer(MushafLayoutRowDto.serializer()),
            assetReader.read(layoutAsset(script))
        )
        require(texts.size == EXPECTED_AYAH_COUNT) {
            "mushaf text source $textSource: ${texts.size} ayahs, expected $EXPECTED_AYAH_COUNT"
        }

        val textRows = texts.map {
            MushafAyahTextEntity(textSource = textSource, ayahId = it.ayahId, text = it.text)
        }
        val layoutRows = layout.map {
            MushafLayoutLineEntity(
                script = script.name,
                page = it.pageNumber,
                line = it.lineNumber,
                lineType = it.lineType,
                surahId = it.surahId,
                ayahId = it.ayahId,
                firstWordPosition = it.firstWordPosition,
                lastWordPosition = it.lastWordPosition
            )
        }
        dao.replaceMushafLayout(script.name, textSource, textRows, layoutRows)
    }

    companion object {
        const val EXPECTED_AYAH_COUNT = 6236

        /**
         * Bump whenever the bundled mushaf assets change so existing installs re-seed on
         * update. `0` means "never seeded"; the first version shipped under the generalised
         * (script-keyed) schema is `1`.
         */
        const val CONTENT_VERSION = 1

        fun textAsset(textSource: String): String =
            "quran/mushaf/${textSource.lowercase()}_text.json"

        fun layoutAsset(script: MushafScript): String =
            "quran/mushaf/${script.name.lowercase()}_layout.json"
    }
}

/** Thin abstraction over the DataStore key so the seeder is unit-testable. */
interface MushafContentVersionStore {
    suspend fun get(script: String): Int
    suspend fun set(script: String, version: Int)
}

/**
 * Stores per-edition seeded versions in one DataStore string-set, as `"<script>:<version>"`
 * entries — the same shape [DataStoreTranslationContentVersionStore] uses, so adding an
 * edition needs no DataStore change.
 */
@Singleton
class DataStoreMushafContentVersionStore @Inject constructor(
    private val prefs: PreferencesDataStore
) : MushafContentVersionStore {

    override suspend fun get(script: String): Int =
        prefs.quranMushafVersions.first()
            .firstOrNull { it.substringBefore(SEPARATOR) == script }
            ?.substringAfter(SEPARATOR)
            ?.toIntOrNull()
            ?: 0

    override suspend fun set(script: String, version: Int) {
        val current = prefs.quranMushafVersions.first()
        val without = current.filterNot { it.substringBefore(SEPARATOR) == script }
        prefs.setQuranMushafVersions(without.toSet() + "$script$SEPARATOR$version")
    }

    private companion object {
        const val SEPARATOR = ':'
    }
}
