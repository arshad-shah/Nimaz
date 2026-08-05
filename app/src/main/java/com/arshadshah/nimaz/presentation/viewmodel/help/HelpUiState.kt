package com.arshadshah.nimaz.presentation.viewmodel.help

import com.arshadshah.nimaz.domain.model.HelpGuideDetail
import com.arshadshah.nimaz.domain.model.HelpSearchResult
import com.arshadshah.nimaz.domain.model.HelpTopic
import com.arshadshah.nimaz.domain.model.HelpTopicDetail

data class HelpHomeUiState(
    val topics: List<HelpTopic> = emptyList(),
    val query: String = "",
    val results: List<HelpSearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)

data class HelpTopicUiState(
    val detail: HelpTopicDetail? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

data class HelpGuideUiState(
    val guide: HelpGuideDetail? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)
