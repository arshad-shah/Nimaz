package com.arshadshah.nimaz.data.local.help

import android.content.Context
import com.arshadshah.nimaz.data.local.database.dao.HelpDao
import com.arshadshah.nimaz.data.local.database.entity.HelpItemEntity
import com.arshadshah.nimaz.data.local.database.entity.HelpStepEntity
import com.arshadshah.nimaz.data.local.database.entity.HelpStringEntity
import com.arshadshah.nimaz.data.local.database.entity.HelpTopicEntity
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** Reads a bundled asset's text. Abstracted so the seeder is unit-testable without Android. */
interface HelpAssetReader {
    fun read(path: String): String
}

@Singleton
class AndroidHelpAssetReader @Inject constructor(
    @ApplicationContext private val context: Context
) : HelpAssetReader {
    override fun read(path: String): String =
        context.assets.open(path).bufferedReader().use { it.readText() }
}

/**
 * Populates the help_* tables from the bundled assets/help/help.json.
 *
 * Idempotent and content-version aware: it re-seeds only when the tables are
 * empty or when help.json's contentVersion is newer than what was last stored.
 * Reaching content this way (rather than via the prepopulated DB asset) means
 * both fresh installs and existing users get help content on update.
 */
@Singleton
class HelpContentSeeder @Inject constructor(
    private val dao: HelpDao,
    private val versionStore: HelpContentVersionStore,
    private val assetReader: HelpAssetReader
) {
    private val mutex = Mutex()

    suspend fun seedIfNeeded() = mutex.withLock {
        val root = helpJson.decodeFromString(
            HelpJsonRoot.serializer(), assetReader.read("help/help.json")
        )
        val stored = versionStore.get()
        val populated = dao.topicCount() > 0
        if (populated && stored >= root.contentVersion) return@withLock
        seed(root)
        versionStore.set(root.contentVersion)
    }

    private suspend fun seed(root: HelpJsonRoot) {
        val topics = mutableListOf<HelpTopicEntity>()
        val items = mutableListOf<HelpItemEntity>()
        val steps = mutableListOf<HelpStepEntity>()
        val strings = mutableListOf<HelpStringEntity>()

        fun addStrings(
            ownerType: String,
            ownerId: String,
            field: String,
            map: Map<String, String>
        ) {
            map.forEach { (lang, value) ->
                strings += HelpStringEntity(ownerType, ownerId, field, lang, value)
            }
        }

        root.topics.forEach { t ->
            topics += HelpTopicEntity(t.id, t.order, t.icon, t.color)
            addStrings("TOPIC", t.id, "title", t.title)
            addStrings("TOPIC", t.id, "subtitle", t.subtitle)
            t.items.forEach { item ->
                val type = if (item.type.equals("guide", true)) "GUIDE" else "QUESTION"
                items += HelpItemEntity(
                    item.id,
                    t.id,
                    type,
                    item.order,
                    item.icon,
                    item.estimatedMinutes
                )
                addStrings("ITEM", item.id, "question", item.question)
                addStrings("ITEM", item.id, "answer", item.answer)
                addStrings("ITEM", item.id, "title", item.title)
                item.steps.forEach { s ->
                    val pathLabels = if (s.pathLabels.isEmpty()) null
                    else helpJson.encodeToString(s.pathLabels)
                    steps += HelpStepEntity(s.id, item.id, s.order, s.deeplink, pathLabels)
                    addStrings("STEP", s.id, "title", s.title)
                    addStrings("STEP", s.id, "body", s.body)
                }
            }
        }

        dao.clearStrings(); dao.clearSteps(); dao.clearItems(); dao.clearTopics()
        dao.insertTopics(topics); dao.insertItems(items)
        dao.insertSteps(steps); dao.insertStrings(strings)
    }
}

/** Thin abstraction over the DataStore version key so the seeder is unit-testable. */
interface HelpContentVersionStore {
    suspend fun get(): Int
    suspend fun set(version: Int)
}

@Singleton
class DataStoreHelpContentVersionStore @Inject constructor(
    private val prefs: PreferencesDataStore
) : HelpContentVersionStore {
    override suspend fun get(): Int = prefs.helpContentVersion.first()
    override suspend fun set(version: Int) = prefs.setHelpContentVersion(version)
}
