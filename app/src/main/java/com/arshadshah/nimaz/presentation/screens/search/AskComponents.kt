package com.arshadshah.nimaz.presentation.screens.search

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.viewmodel.AskPhase
import com.arshadshah.nimaz.presentation.viewmodel.AskUiState

/**
 * Adds the "Ask with Proof" experience to the Search screen's LazyColumn.
 *
 * The question is entered through the screen's single shared search bar — this
 * section renders only the *output* of an ask. Gated on [AskUiState.aiEnabled]:
 * when off it shows a subtle, dismissible hint; when on it renders one hero
 * card per phase — thinking, answer + proof verses, or a friendly error with
 * retry. Proof cards are real records from the local library that deep-link
 * into the readers.
 */
fun LazyListScope.askSection(
    state: AskUiState,
    onNavigateToProof: (Route) -> Unit,
    onNavigateToSearchSettings: () -> Unit,
    onDismissHint: () -> Unit,
    onRetry: () -> Unit,
) {
    if (!state.aiEnabled) {
        if (!state.hintDismissed) {
            item(key = "ai_hint") {
                AskDisabledHint(
                    onOpenSettings = onNavigateToSearchSettings,
                    onDismiss = onDismissHint,
                )
            }
        }
        return
    }

    when (val phase = state.phase) {
        AskPhase.Idle -> Unit

        AskPhase.Loading -> {
            item(key = "ai_card") { AskLoadingCard() }
        }

        is AskPhase.Answer -> {
            item(key = "ai_card") {
                AskAnswerCard(
                    answer = phase.answer,
                    confidence = phase.confidence,
                    proofs = phase.proofs,
                    onNavigateToProof = onNavigateToProof,
                )
            }
        }

        is AskPhase.Error -> {
            item(key = "ai_card") { AskErrorCard(error = phase.error, onRetry = onRetry) }
        }
    }
}

/** Sparkle avatar on a soft theme-derived gradient — the AI section's marker. */
@Composable
private fun AiBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.tertiaryContainer,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun AskHeaderRow(trailing: @Composable () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AiBadge()
        Text(
            text = stringResource(R.string.ai_answer_section),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f),
        )
        trailing()
    }
}

@Composable
private fun AskLoadingCard() {
    // Gentle breathing on the badge while the answer is generated.
    val transition = rememberInfiniteTransition(label = "ai_loading")
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ai_loading_pulse",
    )
    NimazCard(style = NimazCardStyle.ELEVATED, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AiBadge(modifier = Modifier.alpha(pulse))
                Text(
                    text = stringResource(R.string.ai_thinking),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(2.dp)),
            )
        }
    }
}

@Composable
private fun AskAnswerCard(
    answer: String,
    confidence: AnswerConfidence,
    proofs: List<Proof>,
    onNavigateToProof: (Route) -> Unit,
) {
    NimazCard(
        style = NimazCardStyle.ELEVATED,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            AskHeaderRow(trailing = { ConfidenceChip(confidence) })

            Text(
                text = answer,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 12.dp),
            )

            if (proofs.isEmpty()) {
                Text(
                    text = stringResource(R.string.ai_no_citations),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            } else {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                Text(
                    text = stringResource(R.string.ai_proof_section),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    proofs.forEachIndexed { index, proof ->
                        ProofRow(
                            index = index + 1,
                            proof = proof,
                            onClick = { onNavigateToProof(proof.route) },
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.ai_footer_disclaimer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun ConfidenceChip(confidence: AnswerConfidence) {
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

/** One cited verse from the local library — tappable, deep-links to the reader. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProofRow(index: Int, proof: Proof, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = index.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = proof.source.icon(),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = proof.meta,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
                Text(
                    text = proof.displayText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
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
private fun AskErrorCard(error: AiError, onRetry: () -> Unit) {
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
        AiError.Network -> stringResource(R.string.ai_error_network)
        is AiError.Invalid -> stringResource(R.string.ai_error_invalid)
        AiError.Unknown -> stringResource(R.string.ai_error_unknown)
    }
    // Retrying can only help for transient failures — not for daily/budget caps.
    val retryable = error is AiError.Network || error is AiError.Unknown
    NimazCard(style = NimazCardStyle.FILLED, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            AskHeaderRow()
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
            if (retryable) {
                Spacer(modifier = Modifier.height(8.dp))
                NimazButton(
                    text = stringResource(R.string.ai_try_again),
                    onClick = onRetry,
                    variant = NimazButtonVariant.TEXT,
                    leadingIcon = Icons.Default.Refresh,
                )
            }
        }
    }
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
