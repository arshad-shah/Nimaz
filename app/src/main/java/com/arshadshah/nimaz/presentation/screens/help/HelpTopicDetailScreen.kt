package com.arshadshah.nimaz.presentation.screens.help

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.HelpTopic
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionTitle
import com.arshadshah.nimaz.presentation.components.molecules.NimazErrorDefaults
import com.arshadshah.nimaz.presentation.components.molecules.NimazErrorState
import com.arshadshah.nimaz.presentation.components.molecules.NimazLoadingState
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.help.HelpEvent
import com.arshadshah.nimaz.presentation.viewmodel.help.HelpViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpTopicDetailScreen(
    topicId: String,
    onNavigateBack: () -> Unit,
    onOpenGuide: (String) -> Unit,
    viewModel: HelpViewModel = hiltViewModel()
) {
    val state by viewModel.topicState.collectAsStateWithLifecycle()

    LaunchedEffect(topicId) { viewModel.onEvent(HelpEvent.LoadTopic(topicId)) }

    val detail = state.detail

    NimazScreenScaffold(
        topBar = {
            NimazBackTopAppBar(
                title = detail?.topic?.title ?: stringResource(R.string.help_title),
                onBackClick = onNavigateBack
            )
        }
    ) { padding ->
        val error = state.error
        when {
            state.isLoading && detail == null -> {
                NimazLoadingState(modifier = Modifier.padding(padding))
            }

            // Before the null-detail branch: a failed load also leaves `detail` null, and
            // "this topic isn't available" is the wrong thing to say about one that is.
            error != null -> NimazErrorState(
                title = stringResource(error.message),
                message = stringResource(R.string.help_load_failed_body),
                kind = error.kind,
                details = error.details,
                primaryAction = NimazErrorDefaults.retry(
                    onRetry = { viewModel.onEvent(HelpEvent.Retry) },
                    label = stringResource(R.string.try_again),
                ),
                modifier = Modifier.padding(padding),
            )

            detail == null -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding), contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.help_topic_unavailable),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    item { HelpTopicHero(detail.topic) }

                    if (detail.questions.isNotEmpty()) {
                        item { NimazSectionTitle(text = stringResource(R.string.help_common_questions)) }
                        item {
                            NimazCard(style = NimazCardStyle.OUTLINED) {
                                detail.questions.forEachIndexed { index, q ->
                                    HelpQuestionRow(question = q)
                                    if (index != detail.questions.lastIndex) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    }
                                }
                            }
                        }
                    }

                    if (detail.guides.isNotEmpty()) {
                        item { NimazSectionTitle(text = stringResource(R.string.help_step_by_step)) }
                        items(detail.guides, key = { it.id }) { guide ->
                            HelpGuideRow(
                                guide = guide,
                                tint = helpColor(detail.topic.colorKey),
                                onClick = { onOpenGuide(guide.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HelpTopicHero(topic: HelpTopic) {
    val tint = helpColor(topic.colorKey)
    NimazCard(style = NimazCardStyle.OUTLINED) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            HelpIconBox(topic.iconKey, tint, boxSize = 52.dp)
            Column {
                Text(
                    text = topic.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (topic.subtitle.isNotBlank()) {
                    Text(
                        text = topic.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
