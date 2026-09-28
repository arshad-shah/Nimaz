package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.JuzProgressInfo
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.foundation.tokens.KhatamAccent
import com.arshadshah.nimaz.presentation.foundation.tokens.rememberKhatamAccent
import com.arshadshah.nimaz.presentation.theme.NimazSpacing
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/** Compact ordered juz grid. The existing journey API is shared by all callers. */
@Composable
fun KhatamJourneyTrail(
    juzProgress: List<JuzProgressInfo>,
    modifier: Modifier = Modifier,
    accent: KhatamAccent = rememberKhatamAccent(),
    onJuzClick: ((Int) -> Unit)? = null,
) {
    if (juzProgress.isEmpty()) return
    val ordered = juzProgress.sortedBy { it.juzNumber }
    val current = ordered.firstOrNull { !it.isComplete }?.juzNumber
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val columns = (maxWidth.value / (56f * fontScale.coerceAtLeast(1f))).toInt().coerceIn(1, 5)
        Column(verticalArrangement = Arrangement.spacedBy(NimazSpacing.Small)) {
            ordered.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(NimazSpacing.Small)) {
                    row.forEach { juz ->
                        val isCurrent = juz.juzNumber == current
                        val description = stringResource(
                            when {
                                juz.isComplete -> R.string.khatam_a11y_juz_complete
                                isCurrent -> R.string.khatam_a11y_juz_current
                                else -> R.string.khatam_a11y_juz_locked
                            }, juz.juzNumber,
                        )
                        val shape = RoundedCornerShape(NimazSpacing.Small)
                        val fill = when {
                            juz.isComplete -> accent.complete
                            isCurrent -> MaterialTheme.colorScheme.primaryContainer
                            else -> accent.muted
                        }
                        val ink = when {
                            juz.isComplete -> accent.onComplete
                            isCurrent -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> accent.onMuted
                        }
                        NimazCard(
                            modifier = Modifier.weight(1f),
                            onClick = onJuzClick?.let { { it(juz.juzNumber) } },
                            shape = shape,
                        ) {
                            Box(
                                Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                    .background(fill, shape)
                                    .then(if (isCurrent) Modifier.border(2.dp, accent.progress, shape) else Modifier)
                                    .clearAndSetSemantics { contentDescription = description }
                                    .padding(NimazSpacing.Small),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(juz.juzNumber.toString(), color = ink, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

// ---- Previews ----

/** 30 juz with [completed] of them finished, for previewing the trail. */
private fun previewJuz(completed: Int): List<JuzProgressInfo> =
    (1..30).map { n ->
        val total = 200
        JuzProgressInfo(
            juzNumber = n,
            totalAyahs = total,
            readAyahs = when {
                n <= completed -> total
                n == completed + 1 -> total / 3
                else -> 0
            },
        )
    }

@Preview(showBackground = true, widthDp = 360, name = "Khatam Trail — Light")
@Composable
private fun KhatamJourneyTrailLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        KhatamJourneyTrail(juzProgress = previewJuz(8), modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true, widthDp = 360, name = "Khatam Trail — Dark")
@Composable
private fun KhatamJourneyTrailDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        KhatamJourneyTrail(juzProgress = previewJuz(8), modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true, widthDp = 360, name = "Khatam Trail — Just started")
@Composable
private fun KhatamJourneyTrailStartPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        KhatamJourneyTrail(juzProgress = previewJuz(0), modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true, widthDp = 360, name = "Khatam Trail — Complete")
@Composable
private fun KhatamJourneyTrailCompletePreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        KhatamJourneyTrail(juzProgress = previewJuz(30), modifier = Modifier.padding(16.dp))
    }
}
