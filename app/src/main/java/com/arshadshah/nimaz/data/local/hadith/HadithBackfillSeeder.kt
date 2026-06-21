package com.arshadshah.nimaz.data.local.hadith

import android.content.Context
import com.arshadshah.nimaz.data.local.database.dao.HadithDao
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

val hadithFillsJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

@Serializable
data class HadithFillsRoot(
    val contentVersion: Int = 1,
    val fills: List<HadithFillDto> = emptyList()
)

/**
 * One repaired hadith. [id] is the stable global primary key of the `hadiths`
 * row (it matches the prepopulated DB and the source JSON), so backfilling is a
 * direct keyed UPDATE that never depends on the per-book numbering schemes.
 */
@Serializable
data class HadithFillDto(
    val id: Int,
    val reference: String = "",
    val textArabic: String,
    val textEnglish: String = "",
    val narrator: String = "",
    // Optional curated chain of narration (isnād). When provided it is stored and
    // overrides the chain the reader otherwise derives from [textArabic].
    val narratorChain: String = ""
)

/** Reads a bundled asset's text. Abstracted so the seeder is unit-testable without Android. */
interface HadithAssetReader {
    fun read(path: String): String
}

@Singleton
class AndroidHadithAssetReader @Inject constructor(
    @ApplicationContext private val context: Context
) : HadithAssetReader {
    override fun read(path: String): String =
        context.assets.open(path).bufferedReader().use { it.readText() }
}

/**
 * Backfills chains of narration (text_arabic / text_english / narrator) for the
 * hadiths that shipped EMPTY in the prepopulated database.
 *
 * Why this exists: the prepopulated DB asset is only copied onto a device on a
 * fresh install, so correcting the asset alone would never reach users who
 * already have the app. This seeder reads the bundled assets/hadith/
 * hadith_fills.json and applies the fixes at runtime, so both fresh installs and
 * existing users converge on the same complete data — the same approach the app
 * already uses for Help, Dua and Qaida content.
 *
 * Idempotent and content-version aware: it re-applies only when the bundled
 * contentVersion is newer than what was last stored, or when empty chains are
 * still present in the database.
 */
@Singleton
class HadithBackfillSeeder @Inject constructor(
    private val dao: HadithDao,
    private val versionStore: HadithBackfillVersionStore,
    private val assetReader: HadithAssetReader
) {
    private val mutex = Mutex()

    suspend fun seedIfNeeded() = mutex.withLock {
        applyTextFills()
        deriveMissingChains()
    }

    /**
     * Applies the bundled text / narrator / curated-chain repairs, version-gated
     * exactly as before.
     */
    private suspend fun applyTextFills() {
        val stored = versionStore.get()
        val hasGaps = dao.emptyArabicCount() > 0
        // Fast path: nothing stale and no empty text left -> avoid parsing.
        if (!hasGaps && stored > 0) return

        val root = hadithFillsJson.decodeFromString(
            HadithFillsRoot.serializer(), assetReader.read("hadith/hadith_fills.json")
        )
        if (!hasGaps && stored >= root.contentVersion) return

        root.fills.forEach { fill ->
            if (fill.textArabic.isNotBlank()) {
                dao.backfillHadith(
                    id = fill.id,
                    textArabic = fill.textArabic,
                    textEnglish = fill.textEnglish,
                    narrator = fill.narrator
                )
            }
            // A curated isnād is applied independently of the text fill so it can
            // be shipped on its own and never gets clobbered by a blank.
            if (fill.narratorChain.isNotBlank()) {
                dao.updateNarratorChain(fill.id, fill.narratorChain)
            }
        }
        versionStore.set(root.contentVersion)
    }

    /**
     * Persists each hadith's chain of narration, derived from its (authentic)
     * Arabic isnād via [IsnadParser]. This runs once — it drains the rows whose
     * `narrator_chain` is still NULL (added empty by MIGRATION_16_17 for both
     * fresh installs and existing users), stamping "" where no chain could be
     * derived so they are not re-scanned. Runs after [applyTextFills] so chains
     * parse from any freshly-repaired Arabic. CPU-bound, so kept off the caller's
     * dispatcher.
     */
    private suspend fun deriveMissingChains() {
        if (dao.countMissingChains() == 0) return
        withContext(Dispatchers.Default) {
            while (true) {
                val batch = dao.getHadithsMissingChain(CHAIN_BATCH)
                if (batch.isEmpty()) break
                val updates = batch.map { it.id to (IsnadParser.parse(it.textArabic) ?: "") }
                dao.setNarratorChains(updates)
            }
        }
    }

    private companion object {
        const val CHAIN_BATCH = 500
    }
}

/** Thin abstraction over the DataStore version key so the seeder is unit-testable. */
interface HadithBackfillVersionStore {
    suspend fun get(): Int
    suspend fun set(version: Int)
}

@Singleton
class DataStoreHadithBackfillVersionStore @Inject constructor(
    private val prefs: PreferencesDataStore
) : HadithBackfillVersionStore {
    override suspend fun get(): Int = prefs.hadithBackfillVersion.first()
    override suspend fun set(version: Int) = prefs.setHadithBackfillVersion(version)
}
