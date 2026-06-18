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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.domain.model.HelpTopic
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionTitle
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.HelpEvent
import com.arshadshah.nimaz.presentation.viewmodel.HelpViewModel
import androidx.compose.runtime.LaunchedEffect

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

    Scaffold(
        topBar = {
            NimazBackTopAppBar(
                title = detail?.topic?.title ?: "Help",
                onBackClick = onNavigateBack
            )
        }
    ) { padding ->
        when {
            state.isLoading && detail == null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            detail == null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(
                        text = "This topic is unavailable.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    item { HelpTopicHero(detail.topic) }

                    if (detail.questions.isNotEmpty()) {
                        item { NimazSectionTitle(text = "Common questions") }
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
                        item { NimazSectionTitle(text = "Step-by-step") }
                        items(detail.guides) { guide ->
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
