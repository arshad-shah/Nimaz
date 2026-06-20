package com.arshadshah.nimaz.data.local.qaida

import android.content.Context
import com.arshadshah.nimaz.data.local.database.dao.QaidaDao
import com.arshadshah.nimaz.data.local.database.entity.QaidaCellEntity
import com.arshadshah.nimaz.data.local.database.entity.QaidaLessonEntity
import com.arshadshah.nimaz.data.local.database.entity.QaidaLetterEntity
import com.arshadshah.nimaz.data.local.database.entity.QaidaLineEntity
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** Reads a bundled asset's text. Abstracted so the seeder is unit-testable without Android. */
interface QaidaAssetReader {
    fun read(path: String): String
}

@Singleton
class AndroidQaidaAssetReader @Inject constructor(
    @ApplicationContext private val context: Context
) : QaidaAssetReader {
    override fun read(path: String): String =
        context.assets.open(path).bufferedReader().use { it.readText() }
}

/**
 * Populates the Qaida content tables (qaida_lessons / qaida_letters /
 * qaida_lines / qaida_cells) from the bundled assets/qaida/qaida_content.json.
 *
 * The Qaida content historically lived only inside the prepopulated DB asset,
 * which Room copies via createFromAsset *only on a fresh install*. On an app
 * update the on-device database already exists, so the prepopulated DB is never
 * re-copied and MIGRATION_14_15 only creates the (empty) Qaida tables — it does
 * not seed them. Upgrading users therefore saw an empty Qaida.
 *
 * Seeding from a versioned JSON asset at runtime — exactly as
 * [com.arshadshah.nimaz.data.local.dua.DuaContentSeeder] and
 * [com.arshadshah.nimaz.data.local.help.HelpContentSeeder] do — fixes that:
 * both fresh installs and upgrades converge on the bundled content.
 *
 * Idempotent and content-version aware: it re-seeds only when the tables are
 * empty or when qaida_content.json's contentVersion is newer than what was last
 * stored. The replace is atomic and touches only the content tables; the
 * progress tables (qaida_lesson_progress / qaida_cell_progress) have no foreign
 * key into the content tables, so the user's learning progress is preserved.
 */
@Singleton
class QaidaContentSeeder @Inject constructor(
    private val dao: QaidaDao,
    private val versionStore: QaidaContentVersionStore,
    private val assetReader: QaidaAssetReader
) {
    private val mutex = Mutex()

    suspend fun seedIfNeeded() = mutex.withLock {
        val root = qaidaJson.decodeFromString(
            QaidaJsonRoot.serializer(), assetReader.read("qaida/qaida_content.json")
        )
        val stored = versionStore.get()
        val populated = dao.lessonCount() > 0
        if (populated && stored >= root.contentVersion) return@withLock
        seed(root)
        versionStore.set(root.contentVersion)
    }

    private suspend fun seed(root: QaidaJsonRoot) {
        val lessons = root.lessons.map {
            QaidaLessonEntity(
                id = it.id,
                lessonNumber = it.lessonNumber,
                titleEnglish = it.titleEnglish,
                titleArabic = it.titleArabic,
                titleTransliteration = it.titleTransliteration,
                description = it.description,
                // Stored JSON-encoded to match the prepopulated-DB convention
                // (QaidaRepositoryImpl parses it back with parseJsonArray).
                conceptTags = qaidaJson.encodeToString(it.conceptTags),
                icon = it.icon,
                displayOrder = it.displayOrder
            )
        }
        val letters = root.letters.map {
            QaidaLetterEntity(
                id = it.id,
                letterArabic = it.letterArabic,
                nameArabic = it.nameArabic,
                nameTransliteration = it.nameTransliteration,
                isolatedForm = it.isolatedForm,
                initialForm = it.initialForm,
                medialForm = it.medialForm,
                finalForm = it.finalForm,
                isConnecting = it.isConnecting,
                makhrajArea = it.makhrajArea,
                makhrajDetail = it.makhrajDetail,
                phoneticHint = it.phoneticHint,
                audioKey = it.audioKey,
                displayOrder = it.displayOrder
            )
        }
        val lines = root.lines.map {
            QaidaLineEntity(
                id = it.id,
                lessonId = it.lessonId,
                lineNumber = it.lineNumber,
                lineType = it.lineType,
                instructionEnglish = it.instructionEnglish,
                instructionArabic = it.instructionArabic,
                displayOrder = it.displayOrder
            )
        }
        val cells = root.cells.map {
            QaidaCellEntity(
                id = it.id,
                lineId = it.lineId,
                lessonId = it.lessonId,
                position = it.position,
                textArabic = it.textArabic,
                transliteration = it.transliteration,
                tokenType = it.tokenType,
                audioKey = it.audioKey,
                highlightGroup = it.highlightGroup,
                letterId = it.letterId,
                notes = it.notes
            )
        }
        dao.replaceAllContent(lessons, letters, lines, cells)
    }
}

/** Thin abstraction over the DataStore version key so the seeder is unit-testable. */
interface QaidaContentVersionStore {
    suspend fun get(): Int
    suspend fun set(version: Int)
}

@Singleton
class DataStoreQaidaContentVersionStore @Inject constructor(
    private val prefs: PreferencesDataStore
) : QaidaContentVersionStore {
    override suspend fun get(): Int = prefs.qaidaContentVersion.first()
    override suspend fun set(version: Int) = prefs.setQaidaContentVersion(version)
}
