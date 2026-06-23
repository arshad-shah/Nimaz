package com.arshadshah.nimaz.presentation.screens.help

import androidx.compose.ui.res.stringResource
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.domain.model.HelpGuideDetail
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazLoadingState
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.HelpEvent
import com.arshadshah.nimaz.presentation.viewmodel.HelpViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpGuideScreen(
    guideId: String,
    onNavigateBack: () -> Unit,
    onDeepLink: (String) -> Unit,
    viewModel: HelpViewModel = hiltViewModel()
) {
    val state by viewModel.guideState.collectAsStateWithLifecycle()

    LaunchedEffect(guideId) { viewModel.onEvent(HelpEvent.LoadGuide(guideId)) }

    val guide = state.guide

    Scaffold(
        topBar = { NimazBackTopAppBar(title = stringResource(R.string.help_guide_title), onBackClick = onNavigateBack) }
    ) { padding ->
        when {
            state.isLoading && guide == null -> {
                NimazLoadingState(modifier = Modifier.padding(padding))
            }

            guide == null -> {
                Box(Modifier
                    .fillMaxSize()
                    .padding(padding), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.help_guide_unavailable),
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { HelpGuideHero(guide) }
                    item {
                        HelpStepTimeline(
                            steps = guide.steps,
                            onPathChipClick = { step -> step.deeplinkRoute?.let(onDeepLink) }
                        )
                    }
                    item { HelpGuideDone() }
                }
            }
        }
    }
}

@Composable
private fun HelpGuideHero(guide: HelpGuideDetail) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HelpIconBox(iconKey = "tune", tint = MaterialTheme.colorScheme.primary, boxSize = 52.dp)
        Text(
            text = guide.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        val steps = guide.steps.size
        val mins = guide.estimatedMinutes
        val meta = buildString {
            append("$steps step").append(if (steps == 1) "" else "s")
            if (mins != null) append(" · about $mins min")
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(100))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                .padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            NimazIcon(
                imageVector = Icons.Filled.Timelapse,
                contentDescription = null,
                variant = NimazIconVariant.PRIMARY,
                iconSize = 14.dp
            )
            Text(
                text = meta,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun HelpGuideDone() {
    val green = NimazColors.Success
    NimazCard(style = NimazCardStyle.OUTLINED) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            NimazIcon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = green,
                iconSize = 22.dp
            )
            Column {
                Text(
                    text = stringResource(R.string.help_thats_it),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.help_still_stuck),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
