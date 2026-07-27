package com.arshadshah.nimaz.data.local.dua

import android.content.Context
import com.arshadshah.nimaz.data.local.database.dao.DuaDao
import com.arshadshah.nimaz.data.local.database.entity.DuaCategoryEntity
import com.arshadshah.nimaz.data.local.database.entity.DuaEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import com.arshadshah.nimaz.data.local.seeding.AssetContentSeeder
import com.arshadshah.nimaz.data.local.seeding.ContentVersionStore
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
    override val versionStore: ContentVersionStore,
    private val assetReader: DuaAssetReader
) : AssetContentSeeder<DuaJsonRoot>() {

    override val contentKey = CONTENT_KEY
    override val assetPath = ASSET_PATH

    override fun readAsset(path: String): String = assetReader.read(path)

    override fun parse(json: String): DuaJsonRoot =
        duaJson.decodeFromString(DuaJsonRoot.serializer(), json)

    override fun versionOf(parsed: DuaJsonRoot): Int = parsed.contentVersion

    override suspend fun isPopulated(): Boolean = dao.categoryCount() > 0

    override suspend fun replace(parsed: DuaJsonRoot) = seed(parsed)

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

    companion object {
        /** Unique [ContentVersionStore] key for this seeder's content. */
        const val CONTENT_KEY = "dua"
        const val ASSET_PATH = "duas/duas.json"
    }

}
