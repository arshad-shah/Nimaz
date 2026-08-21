package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.HelpGuideDetail
import com.arshadshah.nimaz.domain.model.HelpSearchResult
import com.arshadshah.nimaz.domain.model.HelpTopic
import com.arshadshah.nimaz.domain.model.HelpTopicDetail
import com.arshadshah.nimaz.domain.repository.HelpRepository
import kotlinx.coroutines.flow.Flow

data class HelpUseCases(
    val getTopics: GetHelpTopicsUseCase,
    val getTopicDetail: GetHelpTopicDetailUseCase,
    val getGuide: GetHelpGuideUseCase,
    val search: SearchHelpUseCase
)

class GetHelpTopicsUseCase(private val repo: HelpRepository) {
    operator fun invoke(lang: String): Flow<List<HelpTopic>> = repo.getTopics(lang)
}

class GetHelpTopicDetailUseCase(private val repo: HelpRepository) {
    operator fun invoke(topicId: String, lang: String): Flow<HelpTopicDetail?> =
        repo.getTopicDetail(topicId, lang)
}

class GetHelpGuideUseCase(private val repo: HelpRepository) {
    operator fun invoke(guideId: String, lang: String): Flow<HelpGuideDetail?> =
        repo.getGuide(guideId, lang)
}

class SearchHelpUseCase(private val repo: HelpRepository) {
    operator fun invoke(query: String, lang: String): Flow<List<HelpSearchResult>> =
        repo.search(query, lang)
}
