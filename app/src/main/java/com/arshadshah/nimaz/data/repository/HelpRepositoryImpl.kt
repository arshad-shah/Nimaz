package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.data.local.database.dao.HelpDao
import com.arshadshah.nimaz.data.local.database.entity.HelpStringEntity
import com.arshadshah.nimaz.data.local.help.helpJson
import com.arshadshah.nimaz.domain.model.HelpGuideDetail
import com.arshadshah.nimaz.domain.model.HelpItem
import com.arshadshah.nimaz.domain.model.HelpSearchResult
import com.arshadshah.nimaz.domain.model.HelpStep
import com.arshadshah.nimaz.domain.model.HelpTopic
import com.arshadshah.nimaz.domain.model.HelpTopicDetail
import com.arshadshah.nimaz.domain.repository.HelpRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import javax.inject.Inject
import javax.inject.Singleton

private const val EN = "en"

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class HelpRepositoryImpl @Inject constructor(
    private val dao: HelpDao
) : HelpRepository {

    /** Resolves a field for owner in [lang], falling back to English, then "". */
    private fun List<HelpStringEntity>.resolve(
        ownerId: String, field: String, lang: String
    ): String {
        val forOwner = filter { it.ownerId == ownerId && it.fieldKey == field }
        return forOwner.firstOrNull { it.langCode == lang }?.value
            ?: forOwner.firstOrNull { it.langCode == EN }?.value
            ?: ""
    }

    private fun parsePathLabels(raw: String?): List<String> =
        if (raw.isNullOrBlank()) emptyList()
        else runCatching {
            helpJson.decodeFromString(ListSerializer(String.serializer()), raw)
        }.onFailure { CrashReporter.recordException(it) }.getOrDefault(emptyList())

    /**
     * Builds the DB-backed flow at collection time rather than at call time.
     *
     * This used to seed the bundled help content first; the content arrives in the artifact
     * since `HelpContentSeeder` was retired at versionCode 385 (`docs/retirement.yaml`). The
     * deferral stays because callers hold these flows without collecting them.
     */
    private fun <T> deferredFlow(block: () -> Flow<T>): Flow<T> =
        flow { emitAll(block()) }

    override fun getTopics(lang: String): Flow<List<HelpTopic>> = deferredFlow {
        combine(dao.getTopics(), dao.getAllItems()) { topics, allItems -> topics to allItems }
            .flatMapLatest { (topics, allItems) ->
                if (topics.isEmpty()) return@flatMapLatest flowOf(emptyList())
                dao.getStringsFor("TOPIC", topics.map { it.id }).map { strings ->
                    topics.map { t ->
                        HelpTopic(
                            id = t.id, iconKey = t.iconKey, colorKey = t.colorKey,
                            title = strings.resolve(t.id, "title", lang),
                            subtitle = strings.resolve(t.id, "subtitle", lang),
                            order = t.displayOrder,
                            itemCount = allItems.count { it.topicId == t.id }
                        )
                    }
                }
            }
    }

    override fun getTopicDetail(topicId: String, lang: String): Flow<HelpTopicDetail?> =
        deferredFlow {
            combine(
                dao.getTopics(),
                dao.getItemsForTopic(topicId),
                dao.getStringsFor("TOPIC", listOf(topicId))
            ) { topics, items, topicStrings -> Triple(topics, items, topicStrings) }
                .flatMapLatest { (topics, items, topicStrings) ->
                    val topicEntity = topics.firstOrNull { it.id == topicId }
                        ?: return@flatMapLatest flowOf(null)
                    dao.getStringsFor("ITEM", items.map { it.id }).map { itemStrings ->
                        val topic = HelpTopic(
                            id = topicEntity.id, iconKey = topicEntity.iconKey,
                            colorKey = topicEntity.colorKey,
                            title = topicStrings.resolve(topicId, "title", lang),
                            subtitle = topicStrings.resolve(topicId, "subtitle", lang),
                            order = topicEntity.displayOrder, itemCount = items.size
                        )
                        val questions = items.filter { it.type == "QUESTION" }.map {
                            HelpItem.HelpQuestion(
                                id = it.id, order = it.displayOrder,
                                question = itemStrings.resolve(it.id, "question", lang),
                                answer = itemStrings.resolve(it.id, "answer", lang)
                            )
                        }
                        val guides = items.filter { it.type == "GUIDE" }.map {
                            HelpItem.HelpGuide(
                                id = it.id, order = it.displayOrder, iconKey = it.iconKey,
                                title = itemStrings.resolve(it.id, "title", lang),
                                estimatedMinutes = it.estimatedMinutes, stepCount = 0
                            )
                        }
                        HelpTopicDetail(topic, questions, guides)
                    }
                }
        }

    override fun getGuide(guideId: String, lang: String): Flow<HelpGuideDetail?> = deferredFlow {
        combine(
            dao.getItem(guideId),
            dao.getStepsForItem(guideId),
            dao.getStringsFor("ITEM", listOf(guideId))
        ) { item, steps, itemStrings -> Triple(item, steps, itemStrings) }
            .flatMapLatest { (item, steps, itemStrings) ->
                if (item == null) return@flatMapLatest flowOf(null)
                dao.getStringsFor("STEP", steps.map { it.id }).map { stepStrings ->
                    HelpGuideDetail(
                        id = item.id,
                        title = itemStrings.resolve(item.id, "title", lang),
                        estimatedMinutes = item.estimatedMinutes,
                        steps = steps.map { s ->
                            HelpStep(
                                id = s.id, order = s.displayOrder,
                                title = stepStrings.resolve(s.id, "title", lang),
                                body = stepStrings.resolve(s.id, "body", lang),
                                deeplinkRoute = s.deeplinkRoute,
                                pathLabels = parsePathLabels(s.pathLabels)
                            )
                        }
                    )
                }
            }
    }

    override fun search(query: String, lang: String): Flow<List<HelpSearchResult>> = deferredFlow {
        if (query.isBlank()) return@deferredFlow flowOf(emptyList())
        combine(
            dao.searchStrings(lang, query),
            dao.searchStrings(EN, query),
            dao.getAllItems()
        ) { localized, english, items ->
            val itemById = items.associateBy { it.id }
            (localized + english)
                .filter { it.ownerType == "ITEM" && it.fieldKey in setOf("question", "title") }
                .distinctBy { it.ownerId }
                .mapNotNull { s ->
                    val item = itemById[s.ownerId] ?: return@mapNotNull null
                    HelpSearchResult(
                        topicId = item.topicId,
                        itemId = s.ownerId,
                        isGuide = item.type == "GUIDE",
                        title = s.value,
                        snippet = ""
                    )
                }
        }
    }
}
