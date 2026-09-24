package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.ui.res.painterResource
import com.arshadshah.nimaz.feature.content.R as FeatureR
import com.arshadshah.nimaz.presentation.components.atoms.NimazProgressTrack
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonType
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.theme.NimazSpacing
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * Illustrated course welcome with theme-backed progress and an optional resume action.
 * Reuses the bundled serif face; no feature-specific palette is introduced.
 */
@Composable
fun QaidaCourseHeader(
    titleArabic: String,
    titleEnglish: String,
    lessonIndex: Int,
    totalLessons: Int,
    totalStars: Int,
    overallFraction: Float,
    continueLabel: String?,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    showContinue: Boolean = true,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = NimazSpacing.Large, vertical = NimazSpacing.Small),
        verticalArrangement = Arrangement.spacedBy(NimazSpacing.Small),
    ) {
        Text(titleEnglish, style = MaterialTheme.typography.headlineMedium.copy(
            fontFamily = com.arshadshah.nimaz.presentation.theme.AmiriFontFamily),
            color = MaterialTheme.colorScheme.onSurface)
        Text(stringResource(FeatureR.string.qaida_gentle_intro), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Image(painterResource(FeatureR.drawable.qaida_journey_book), contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(140.dp))
        com.arshadshah.nimaz.presentation.components.atoms.NimazCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(FeatureR.string.qaida_daily_title), style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = com.arshadshah.nimaz.presentation.theme.AmiriFontFamily))
                NimazProgressTrack(progress = overallFraction.coerceIn(0f, 1f), modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.qaida_lesson_progress, lessonIndex, totalLessons),
                        style = MaterialTheme.typography.bodySmall)
                    Text(java.text.NumberFormat.getPercentInstance().format(overallFraction.coerceIn(0f, 1f)),
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (continueLabel != null && showContinue) {
            NimazButton(
                text = stringResource(R.string.qaida_continue_format, continueLabel),
                onClick = onContinue,
                modifier = Modifier
                    .padding(top = NimazSpacing.ExtraSmall)
                    .testTag("qaida_continue"),
                variant = NimazButtonVariant.FILLED,
                type = NimazButtonType.PILL,
                leadingIcon = Icons.Filled.PlayArrow,
                fullWidth = true,
            )
        }
    }
}


// ==================== PREVIEWS ====================

@Composable
private fun QaidaCourseHeaderShowcase() {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        QaidaCourseHeader(
            titleArabic = "القاعدة النورانية",
            titleEnglish = "Noorani Qaida",
            lessonIndex = 4,
            totalLessons = 17,
            totalStars = 9,
            overallFraction = 0.35f,
            continueLabel = "Lesson 4",
            onContinue = {},
        )
    }
}

@Preview(showBackground = true, name = "Qaida Course Header — Light")
@Composable
private fun QaidaCourseHeaderLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        QaidaCourseHeaderShowcase()
    }
}

@Preview(showBackground = true, name = "Qaida Course Header — Dark")
@Composable
private fun QaidaCourseHeaderDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        QaidaCourseHeaderShowcase()
    }
}
