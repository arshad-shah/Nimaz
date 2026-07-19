package com.arshadshah.nimaz.presentation.screens.search

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.arshadshah.nimaz.domain.model.AiError
import com.arshadshah.nimaz.domain.model.AnswerConfidence
import com.arshadshah.nimaz.presentation.components.atoms.NimazBanner
import com.arshadshah.nimaz.presentation.components.atoms.NimazBannerVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.theme.NimazColors

/**
 * The "Ask with Proof" hero cards for the Search screen: thinking, the
 * answer itself, errors, and the AI-off discovery card.
 *
 * The question is entered through the screen's single shared search bar, and
 * — since the v2 redesign — the cited proof verses are NOT rendered here:
 * [AskAnswerCard] carries only the answer + confidence + trust note, and
 * `SearchScreen` merges the proofs into the one filterable results list as
 * regular result cards marked "Cited".
 */

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

/** Thin teal→purple strip along the card's top edge — marks AI surfaces. */
@Composable
private fun AiAccentStrip() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary,
                    ),
                ),
            ),
    )
}

@Composable
internal fun AskLoadingCard() {
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
        Column {
            AiAccentStrip()
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
                // Skeleton of the incoming answer.
                listOf(0.92f, 0.98f, 0.7f).forEach { widthFraction ->
                    Box(
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .fillMaxWidth(widthFraction)
                            .height(11.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
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
}

/**
 * The AI answer alone — badge + "Answer" + confidence chip, the answer text,
 * and the trust note. The cited verses live in the results list below, so the
 * card deliberately carries no proof section.
 */
@Composable
internal fun AskAnswerCard(
    answer: String,
    confidence: AnswerConfidence,
) {
    NimazCard(
        style = NimazCardStyle.ELEVATED,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
    ) {
        Column {
            AiAccentStrip()
            Column(modifier = Modifier.padding(16.dp)) {
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
                    ConfidenceChip(confidence)
                }

                Text(
                    text = answer,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 12.dp),
                )

                HorizontalDivider(
                    modifier = Modifier.padding(top = 14.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                Row(modifier = Modifier.padding(top = 12.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(top = 1.dp)
                            .size(14.dp),
                    )
                    Text(
                        text = stringResource(R.string.ai_trust_note),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfidenceChip(confidence: AnswerConfidence) {
    // High reads as trust (teal), medium as caution (amber), low as muted.
    val (label, color) = when (confidence) {
        AnswerConfidence.HIGH ->
            stringResource(R.string.ai_confidence_high) to MaterialTheme.colorScheme.primary

        AnswerConfidence.MEDIUM ->
            stringResource(R.string.ai_confidence_medium) to NimazColors.Warning

        AnswerConfidence.LOW ->
            stringResource(R.string.ai_confidence_low) to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(start = 5.dp),
        )
    }
}

/**
 * Discovery card shown on Global Search while AI answers are off: what the
 * feature does, the privacy line, and enable / not-now actions.
 */
@Composable
internal fun AskDiscoveryCard(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    NimazCard(style = NimazCardStyle.ELEVATED, modifier = Modifier.fillMaxWidth()) {
        Column {
            AiAccentStrip()
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AiBadge()
                    Text(
                        text = stringResource(R.string.ai_discover_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.ai_discover_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
                NimazCard(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(),
                    style = NimazCardStyle.OUTLINED,
                    shape = RoundedCornerShape(11.dp),
                    elevation = 0.dp,
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(top = 1.dp)
                                .size(14.dp),
                        )
                        Text(
                            text = stringResource(R.string.ai_discover_privacy),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
                Row(
                    modifier = Modifier.padding(top = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NimazButton(
                        text = stringResource(R.string.ai_discover_enable),
                        onClick = onOpenSettings,
                        variant = NimazButtonVariant.FILLED,
                        leadingIcon = Icons.Default.AutoAwesome,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    NimazButton(
                        text = stringResource(R.string.ai_discover_dismiss),
                        onClick = onDismiss,
                        variant = NimazButtonVariant.TEXT,
                    )
                }
            }
        }
    }
}

/**
 * Ask failures rendered with the design-system [NimazBanner]: expected pauses
 * (daily limit, shared budget) as the amber WARNING variant, transient/technical
 * failures as the ERROR variant with a retry action where retrying can help.
 */
@Composable
internal fun AskErrorCard(error: AiError, onRetry: () -> Unit) {
    val icon: ImageVector
    val title: String
    val body: String
    when (error) {
        is AiError.RateLimited -> {
            icon = Icons.Default.Schedule
            title = stringResource(R.string.ai_error_rate_limited_title)
            val retry = error.retryAfterSeconds
            body = if (retry != null) {
                stringResource(R.string.ai_error_rate_limited_retry, formatRetry(retry))
            } else {
                stringResource(R.string.ai_error_rate_limited)
            }
        }

        AiError.BudgetExceeded -> {
            icon = Icons.Outlined.PauseCircle
            title = stringResource(R.string.ai_error_budget_title)
            body = stringResource(R.string.ai_error_budget)
        }

        AiError.Network -> {
            icon = Icons.Default.WifiOff
            title = stringResource(R.string.ai_error_network_title)
            body = stringResource(R.string.ai_error_network)
        }

        AiError.Unverified -> {
            icon = Icons.Outlined.Shield
            title = stringResource(R.string.ai_error_unverified_title)
            body = stringResource(R.string.ai_error_unverified)
        }

        is AiError.Invalid -> {
            icon = Icons.Outlined.Info
            title = stringResource(R.string.ai_error_invalid_title)
            body = stringResource(R.string.ai_error_invalid)
        }

        AiError.Unknown -> {
            icon = Icons.Outlined.Info
            title = stringResource(R.string.ai_error_unknown_title)
            body = stringResource(R.string.ai_error_unknown)
        }
    }
    // Retrying can only help for transient failures — not for daily/budget caps.
    val retryable = error is AiError.Network || error is AiError.Unknown
    // Hitting a usage cap is an expected pause, not a failure — warn, don't alarm.
    val variant = when (error) {
        is AiError.RateLimited, AiError.BudgetExceeded -> NimazBannerVariant.WARNING
        else -> NimazBannerVariant.ERROR
    }
    NimazBanner(
        message = body,
        variant = variant,
        icon = icon,
        title = title,
        actionLabel = if (retryable) stringResource(R.string.ai_try_again) else null,
        onAction = if (retryable) onRetry else null,
        modifier = Modifier.fillMaxWidth(),
    )
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
