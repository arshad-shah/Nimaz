package com.arshadshah.nimaz.presentation.screens.help

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.HelpSearchResult
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcons
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionTitle
import com.arshadshah.nimaz.presentation.components.molecules.NimazErrorDefaults
import com.arshadshah.nimaz.presentation.components.molecules.NimazErrorState
import com.arshadshah.nimaz.presentation.components.molecules.NimazErrorVariant
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.components.organisms.NimazSearchBar
import com.arshadshah.nimaz.presentation.viewmodel.help.HelpEvent
import com.arshadshah.nimaz.presentation.viewmodel.help.HelpViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTopic: (String) -> Unit,
    onContact: () -> Unit,
    viewModel: HelpViewModel = hiltViewModel()
) {
    val state by viewModel.homeState.collectAsStateWithLifecycle()

    NimazScreenScaffold(
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.help_title),
                onBackClick = onNavigateBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            item {
                NimazSearchBar(
                    query = state.query,
                    onQueryChange = { viewModel.onEvent(HelpEvent.Search(it)) },
                    placeholder = stringResource(R.string.help_search_hint),
                    onClear = { viewModel.onEvent(HelpEvent.Search("")) }
                )
            }

            if (state.isSearching) {
                if (state.results.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.help_no_results),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                } else {
                    items(state.results, key = { "${it.topicId}:${it.itemId}" }) { result ->
                        HelpResultRow(
                            result = result,
                            onClick = { onNavigateToTopic(result.topicId) })
                    }
                }
            } else if (state.error != null) {
                // SECTION, not FULLSCREEN, and inside the list so the search bar above it
                // stays usable: search reads a different code path, and a reader whose
                // topic list failed can still find a topic by name.
                item {
                    val error = state.error!!
                    NimazErrorState(
                        title = stringResource(error.message),
                        message = stringResource(R.string.help_load_failed_body),
                        kind = error.kind,
                        details = error.details,
                        variant = NimazErrorVariant.SECTION,
                        primaryAction = NimazErrorDefaults.retry(
                            onRetry = { viewModel.onEvent(HelpEvent.Retry) },
                            label = stringResource(R.string.try_again),
                        ),
                    )
                }
            } else {
                item {
                    NimazSectionTitle(
                        text = stringResource(R.string.help_browse_topics),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(state.topics.chunked(2), key = { row -> row.first().id }) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                        row.forEach { topic ->
                            HelpTopicTile(
                                topic = topic,
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateToTopic(topic.id) }
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                item { HelpContactCard(onClick = onContact) }
            }
        }
    }
}

@Composable
private fun HelpResultRow(result: HelpSearchResult, onClick: () -> Unit) {
    NimazCard(style = NimazCardStyle.OUTLINED, onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = result.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            NimazIcon(
                imageVector = NimazIcons.Forward,
                contentDescription = null,
                variant = NimazIconVariant.MUTED,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun HelpContactCard(onClick: () -> Unit) {
    val tint = MaterialTheme.colorScheme.primary
    NimazCard(
        modifier = Modifier.padding(top = 4.dp),
        style = NimazCardStyle.OUTLINED,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                NimazIcon(
                    imageVector = Icons.Filled.MailOutline,
                    contentDescription = null,
                    tint = tint,
                    iconSize = 21.dp
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.help_still_need),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.help_email_us),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            NimazIcon(
                imageVector = NimazIcons.Forward,
                contentDescription = null,
                tint = tint
            )
        }
    }
}
