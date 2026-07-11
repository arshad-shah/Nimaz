package com.arshadshah.nimaz.presentation.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.navigation.Route
import com.arshadshah.nimaz.domain.model.AiError
import com.arshadshah.nimaz.domain.model.AnswerConfidence
import com.arshadshah.nimaz.domain.model.Proof
import com.arshadshah.nimaz.domain.model.ProofSource
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.organisms.NimazSearchBar
import com.arshadshah.nimaz.presentation.viewmodel.AskEvent
import com.arshadshah.nimaz.presentation.viewmodel.AskPhase
import com.arshadshah.nimaz.presentation.viewmodel.AskUiState

/**
 * Adds the "Ask with Proof" experience to the Search screen's LazyColumn.
 * Gated on [AskUiState.aiEnabled]: when off it shows a subtle, dismissible hint;
 * when on it shows the ask input plus the answer / proofs / error states.
 */
fun LazyListScope.askSection(
    state: AskUiState,
    onEvent: (AskEvent) -> Unit,
    onNavigateToProof: (Route) -> Unit,
    onNavigateToSearchSettings: () -> Unit,
) {
    if (!state.aiEnabled) {
        if (!state.hintDismissed) {
            item(key = "ai_hint") {
                AskDisabledHint(
                    onOpenSettings = onNavigateToSearchSettings,
                    onDismiss = { onEvent(AskEvent.DismissHint) },
                )
            }
        }
        return
    }

    item(key = "ai_input") {
        NimazCard(style = NimazCardStyle.FILLED, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.ai_ask_a_question),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                NimazSearchBar(
                    query = state.question,
                    onQueryChange = { onEvent(AskEvent.UpdateQuestion(it)) },
                    placeholder = stringResource(R.string.ai_ask_placeholder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    showClearButton = state.question.isNotEmpty(),
                    onClear = { onEvent(AskEvent.Clear) },
                    onSearch = { onEvent(AskEvent.Submit) },
                )
            }
        }
    }

    when (val phase = state.phase) {
        AskPhase.Idle -> Unit

        AskPhase.Loading -> item(key = "ai_loading") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        is AskPhase.Answer -> {
            if (phase.answer.insufficientEvidence) {
                item(key = "ai_no_sources") { AskNoSourcesCard() }
            } else {
                item(key = "ai_answer") {
                    AnswerCard(
                        answer = phase.answer.answer,
                        confidence = phase.answer.confidence,
                    )
                }
                if (phase.proofs.isNotEmpty()) {
                    item(key = "ai_proof_header") {
                        Text(
                            text = stringResource(R.string.ai_proof_section),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    items(phase.proofs, key = { it.citationId }) { proof ->
                        ProofCard(proof = proof, onClick = { onNavigateToProof(proof.route) })
                    }
                }
            }
            item(key = "ai_footer") { AskFooter() }
        }

        is AskPhase.Error -> item(key = "ai_error") {
            AskErrorCard(error = phase.error)
        }
    }
}

@Composable
private fun AskDisabledHint(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    NimazCard(
        style = NimazCardStyle.FILLED,
        onClick = onOpenSettings,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.ai_enable_hint),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.dismiss),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AnswerCard(answer: String, confidence: AnswerConfidence) {
    NimazCard(style = NimazCardStyle.ELEVATED, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            ConfidenceIndicator(confidence)
            Text(
                text = answer,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun ConfidenceIndicator(confidence: AnswerConfidence) {
    val (label, color) = when (confidence) {
        AnswerConfidence.HIGH ->
            stringResource(R.string.ai_confidence_high) to MaterialTheme.colorScheme.primary

        AnswerConfidence.MEDIUM ->
            stringResource(R.string.ai_confidence_medium) to MaterialTheme.colorScheme.tertiary

        AnswerConfidence.LOW ->
            stringResource(R.string.ai_confidence_low) to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

@Composable
private fun ProofCard(proof: Proof, onClick: () -> Unit) {
    NimazCard(
        style = NimazCardStyle.OUTLINED,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = proof.source.icon(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = proof.meta,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Text(
                text = proof.displayText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun AskNoSourcesCard() {
    NimazCard(style = NimazCardStyle.FILLED, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.ai_no_sources_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.ai_no_sources_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun AskErrorCard(error: AiError) {
    val message = when (error) {
        is AiError.RateLimited -> {
            val retry = error.retryAfterSeconds
            if (retry != null) {
                stringResource(R.string.ai_error_rate_limited_retry, formatRetry(retry))
            } else {
                stringResource(R.string.ai_error_rate_limited)
            }
        }

        AiError.BudgetExceeded -> stringResource(R.string.ai_error_budget)
        AiError.Attestation -> stringResource(R.string.ai_error_attestation)
        AiError.Network -> stringResource(R.string.ai_error_network)
        is AiError.Invalid -> stringResource(R.string.ai_error_invalid)
        AiError.Unknown -> stringResource(R.string.ai_error_unknown)
    }
    NimazCard(style = NimazCardStyle.FILLED, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun AskFooter() {
    Text(
        text = stringResource(R.string.ai_footer_disclaimer),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    )
}

private fun ProofSource.icon(): ImageVector = when (this) {
    ProofSource.QURAN -> Icons.AutoMirrored.Filled.MenuBook
    ProofSource.HADITH -> Icons.Default.Mosque
    ProofSource.DUA -> Icons.Default.SelfImprovement
}

/** Turn retry seconds into a coarse "minutes"/"hours" figure for display. */
private fun formatRetry(seconds: Long): String {
    val minutes = (seconds + 59) / 60
    return if (minutes < 60) {
        "$minutes min"
    } else {
        "${(minutes + 59) / 60} h"
    }
}
