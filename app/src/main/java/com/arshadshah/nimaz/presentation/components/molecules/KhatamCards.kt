package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.Khatam
import com.arshadshah.nimaz.domain.model.KhatamInsights
import com.arshadshah.nimaz.domain.model.KhatamPace
import com.arshadshah.nimaz.domain.model.KhatamStatus
import com.arshadshah.nimaz.presentation.components.atoms.KhatamAccent
import com.arshadshah.nimaz.presentation.components.atoms.KhatamProgressBar
import com.arshadshah.nimaz.presentation.components.atoms.KhatamProgressRing
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonType
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardLevel
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.atoms.NimazLabelChip
import com.arshadshah.nimaz.presentation.components.atoms.paceColor
import com.arshadshah.nimaz.presentation.components.atoms.paceLabel
import com.arshadshah.nimaz.presentation.components.atoms.rememberKhatamAccent
import androidx.compose.ui.tooling.preview.Preview
import com.arshadshah.nimaz.presentation.theme.NimazSpacing
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * The prominent "this is the khatam you're reading" card.
 *
 * Consolidates three previously separate implementations — `ActiveKhatamCard` on the
 * list, `ProgressOverview` on the detail screen and `KhatamProgressCard` on the Quran
 * home tab — so all three surfaces show identical numbers in an identical layout.
 *
 * @param showName false on the detail screen, where the title already names the khatam.
 * @param onContinue null hides the continue button (e.g. for a completed khatam).
 */
@Composable
fun KhatamHeroCard(
    khatam: Khatam,
    insights: KhatamInsights,
    modifier: Modifier = Modifier,
    accent: KhatamAccent = rememberKhatamAccent(),
    showName: Boolean = true,
    showActiveBadge: Boolean = true,
    continueLabel: String? = null,
    onContinue: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val isComplete = khatam.status == KhatamStatus.COMPLETED

    NimazCard(
        modifier = modifier.fillMaxWidth(),
        style = NimazCardStyle.ELEVATED,
        onClick = onClick,
        tone = NimazTone.NEUTRAL,
    ) {
        Column(Modifier.padding(NimazSpacing.Large)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showActiveBadge) {
                    NimazLabelChip(
                        text = stringResource(
                            if (isComplete) R.string.khatam_status_completed_badge
                            else R.string.khatam_active
                        ),
                        highlighted = true,
                    )
                } else {
                    Spacer(Modifier.width(0.dp))
                }
                Text(
                    text = paceLabel(insights.paceStatus),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = paceColor(insights.paceStatus),
                )
            }

            if (showName) {
                Spacer(Modifier.height(NimazSpacing.Small))
                Text(
                    text = khatam.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(NimazSpacing.Medium))

            Row(verticalAlignment = Alignment.CenterVertically) {
                KhatamProgressRing(
                    progress = khatam.progressPercent,
                    size = 56.dp,
                    strokeWidth = 6.dp,
                    accent = accent,
                    isComplete = isComplete,
                    textStyle = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.width(NimazSpacing.Medium))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            R.string.khatam_of_ayahs_read,
                            khatam.totalAyahsRead,
                            Khatam.TOTAL_QURAN_AYAHS
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(NimazSpacing.ExtraSmall))
                    KhatamProgressBar(
                        progress = khatam.progressPercent,
                        accent = accent,
                        isComplete = isComplete,
                    )
                    Spacer(Modifier.height(NimazSpacing.ExtraSmall))
                    Text(
                        text = heroSubtitle(khatam, insights),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (onContinue != null && continueLabel != null && !isComplete) {
                Spacer(Modifier.height(NimazSpacing.Medium))
                NimazButton(
                    text = continueLabel,
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth(),
                    type = NimazButtonType.PILL,
                    leadingIcon = Icons.Default.PlayArrow,
                    fullWidth = true,
                )
            }
        }
    }
}

/**
 * "Juz 8 · ~44 days left" — the one-line status under the hero's progress bar.
 */
@Composable
private fun heroSubtitle(khatam: Khatam, insights: KhatamInsights): String {
    val parts = mutableListOf<String>()
    if (khatam.status != KhatamStatus.COMPLETED) {
        parts += stringResource(R.string.khatam_juz_position, insights.currentJuz)
    }
    insights.estimatedDaysRemaining?.let { days ->
        parts += pluralStringResource(R.plurals.khatam_days_remaining, days, days)
    }
    if (parts.isEmpty()) {
        parts += pluralStringResource(
            R.plurals.khatam_ayahs_read_plural,
            khatam.totalAyahsRead,
            khatam.totalAyahsRead
        )
    }
    return parts.joinToString(" · ")
}

/**
 * A khatam as a row in the list.
 *
 * Status reads from the ring colour plus the trailing chip rather than a coloured edge
 * rail — with a list of these, per-row rails read as clutter.
 */
@Composable
fun KhatamRowCard(
    khatam: Khatam,
    modifier: Modifier = Modifier,
    accent: KhatamAccent = rememberKhatamAccent(),
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    val isComplete = khatam.status == KhatamStatus.COMPLETED
    val isArchived = khatam.status == KhatamStatus.ABANDONED

    NimazCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        // surfaceContainerLow sits too close to `background` to read as a card.
        tone = NimazTone.NEUTRAL,
        style = NimazCardStyle.ELEVATED,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(NimazSpacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KhatamProgressRing(
                progress = khatam.progressPercent,
                size = 44.dp,
                strokeWidth = 5.dp,
                accent = accent,
                isComplete = isComplete,
                textStyle = MaterialTheme.typography.labelSmall,
            )
            Spacer(Modifier.width(NimazSpacing.Medium))
            Column(Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = khatam.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(NimazSpacing.Small))
                    when {
                        isComplete -> NimazLabelChip(
                            text = stringResource(R.string.khatam_status_completed_badge),
                            highlighted = true,
                        )

                        khatam.isActive -> NimazLabelChip(
                            text = stringResource(R.string.khatam_status_active_badge),
                            highlighted = true,
                        )

                        isArchived -> NimazLabelChip(
                            text = stringResource(R.string.khatam_archived),
                        )
                    }
                }
                if (subtitle != null) {
                    Spacer(Modifier.height(NimazSpacing.ExtraSmall))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!isComplete) {
                    Spacer(Modifier.height(NimazSpacing.ExtraSmall))
                    KhatamProgressBar(
                        progress = khatam.progressPercent,
                        height = 5.dp,
                        accent = accent,
                    )
                }
            }
        }
    }
}

// ---- Previews ----

private fun previewKhatam(
    name: String = "Ramadan 1447",
    read: Int = 2340,
    status: KhatamStatus = KhatamStatus.ACTIVE,
    active: Boolean = true,
) = Khatam(
    id = 1,
    name = name,
    status = status,
    isActive = active,
    dailyTarget = 20,
    totalAyahsRead = read,
    startedAt = System.currentTimeMillis() - 21L * 24 * 60 * 60 * 1000,
    completedAt = if (status == KhatamStatus.COMPLETED) System.currentTimeMillis() else null,
)

private val previewInsights = KhatamInsights(
    daysActive = 21,
    averagePace = 111f,
    currentStreak = 7,
    longestStreak = 12,
    juzCompleted = 8,
    remainingAyahs = 3896,
    estimatedDaysRemaining = 44,
    paceStatus = KhatamPace.ON_TRACK,
)

@Composable
private fun KhatamCardsShowcase() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        KhatamHeroCard(
            khatam = previewKhatam(),
            insights = previewInsights,
            continueLabel = "Continue · Surah 8, 12",
            onContinue = {},
        )
        KhatamRowCard(
            khatam = previewKhatam(),
            subtitle = "2,340 ayahs read · Started 12 Feb 2026",
            onClick = {},
        )
        KhatamRowCard(
            khatam = previewKhatam(name = "Second reading", read = 748, active = false),
            subtitle = "748 ayahs read · Started 2 Jul 2026",
            onClick = {},
        )
        KhatamRowCard(
            khatam = previewKhatam(
                name = "Ramadan 1446",
                read = Khatam.TOTAL_QURAN_AYAHS,
                status = KhatamStatus.COMPLETED,
                active = false,
            ),
            subtitle = "6,236 ayahs read · Finished 9 Apr 2025",
            onClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 380, name = "Khatam Cards — Light")
@Composable
private fun KhatamCardsLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { KhatamCardsShowcase() }
}

@Preview(showBackground = true, widthDp = 380, name = "Khatam Cards — Dark")
@Composable
private fun KhatamCardsDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { KhatamCardsShowcase() }
}

@Preview(showBackground = true, widthDp = 380, name = "Khatam Hero — Behind pace")
@Composable
private fun KhatamHeroBehindPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        KhatamHeroCard(
            khatam = previewKhatam(read = 420),
            insights = previewInsights.copy(
                averagePace = 20f,
                juzCompleted = 2,
                estimatedDaysRemaining = 290,
                paceStatus = KhatamPace.BEHIND,
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}
