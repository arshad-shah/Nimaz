package com.arshadshah.nimaz.domain.repository

import com.arshadshah.nimaz.domain.model.HelpGuideDetail
import com.arshadshah.nimaz.domain.model.HelpSearchResult
import com.arshadshah.nimaz.domain.model.HelpTopic
import com.arshadshah.nimaz.domain.model.HelpTopicDetail
import kotlinx.coroutines.flow.Flow

interface HelpRepository {
    fun getTopics(lang: String): Flow<List<HelpTopic>>
    fun getTopicDetail(topicId: String, lang: String): Flow<HelpTopicDetail?>
    fun getGuide(guideId: String, lang: String): Flow<HelpGuideDetail?>
    fun search(query: String, lang: String): Flow<List<HelpSearchResult>>
}
