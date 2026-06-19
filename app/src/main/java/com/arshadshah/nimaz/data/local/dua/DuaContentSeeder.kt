package com.arshadshah.nimaz.data.local.dua

import android.content.Context
import com.arshadshah.nimaz.data.local.database.dao.DuaDao
import com.arshadshah.nimaz.data.local.database.entity.DuaCategoryEntity
import com.arshadshah.nimaz.data.local.database.entity.DuaEntity
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** Reads a bundled asset's text. Abstracted so the seeder is unit-testable without Android. */
interface DuaAssetReader {
    fun read(path: String): String
}

@Singleton
class AndroidDuaAssetReader @Inject constructor(
    @ApplicationContext private val context: Context
) : DuaAssetReader {
    override fun read(path: String): String =
        context.assets.open(path).bufferedReader().use { it.readText() }
}

/**
 * Populates the dua_categories and duas tables from the bundled
 * assets/duas/duas.json.
 *
 * The dua content historically lived only inside the prepopulated DB asset,
 * which Room copies via createFromAsset *only on a fresh install*. That meant
 * expanding the dua dataset never reached existing users on update — their
 * on-device database already existed, so the new content was never copied and
 * (since the dua tables carried no schema change) no migration touched them.
 *
 * Seeding from a versioned JSON asset at runtime — exactly as
 * [com.arshadshah.nimaz.data.local.help.HelpContentSeeder] does — fixes that:
 * both fresh installs and upgrades converge on the bundled content.
 *
 * Idempotent and content-version aware: it re-seeds only when the tables are
 * empty or when duas.json's contentVersion is newer than what was last stored.
 * The replace is atomic and touches only the content tables; dua_bookmarks and
 * dua_progress are not foreign-key linked to duas, so user data is preserved.
 */
@Singleton
class DuaContentSeeder @Inject constructor(
    private val dao: DuaDao,
    private val versionStore: DuaContentVersionStore,
    private val assetReader: DuaAssetReader
) {
    private val mutex = Mutex()

    suspend fun seedIfNeeded() = mutex.withLock {
        val root = duaJson.decodeFromString(
            DuaJsonRoot.serializer(), assetReader.read("duas/duas.json")
        )
        val stored = versionStore.get()
        val populated = dao.categoryCount() > 0
        if (populated && stored >= root.contentVersion) return@withLock
        seed(root)
        versionStore.set(root.contentVersion)
    }

    private suspend fun seed(root: DuaJsonRoot) {
        val categories = root.categories.map {
            DuaCategoryEntity(
                id = it.id,
                nameEnglish = it.nameEnglish,
                nameArabic = it.nameArabic,
                icon = it.icon,
                displayOrder = it.displayOrder,
                duaCount = it.duaCount
            )
        }
        val duas = root.duas.map {
            DuaEntity(
                id = it.id,
                categoryId = it.categoryId,
                titleEnglish = it.titleEnglish,
                titleArabic = it.titleArabic,
                textArabic = it.textArabic,
                transliteration = it.transliteration,
                translation = it.translation,
                source = it.source,
                virtue = it.virtue,
                repeatCount = it.repeatCount,
                audioFile = it.audioFile,
                displayOrder = it.displayOrder
            )
        }
        dao.replaceAllContent(categories, duas)
    }
}

/** Thin abstraction over the DataStore version key so the seeder is unit-testable. */
interface DuaContentVersionStore {
    suspend fun get(): Int
    suspend fun set(version: Int)
}

@Singleton
class DataStoreDuaContentVersionStore @Inject constructor(
    private val prefs: PreferencesDataStore
) : DuaContentVersionStore {
    override suspend fun get(): Int = prefs.duaContentVersion.first()
    override suspend fun set(version: Int) = prefs.setDuaContentVersion(version)
}
