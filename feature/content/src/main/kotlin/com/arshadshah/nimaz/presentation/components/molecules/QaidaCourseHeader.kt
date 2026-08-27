package com.arshadshah.nimaz.presentation.components.molecules

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
 * Pinned header for the Qaida course map: the Arabic journey title, an overall
 * progress bar, "Lesson X of N" with the running star total, and a "Continue"
 * button that resumes the next lesson. Lives on the app surface so it adapts to
 * light/dark; gold (secondary) marks the stars and progress accent.
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
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = NimazSpacing.Large, vertical = NimazSpacing.Medium),
        verticalArrangement = Arrangement.spacedBy(NimazSpacing.Small),
    ) {
        ArabicText(
            text = titleArabic,
            size = ArabicTextSize.MEDIUM,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = titleEnglish,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        LinearProgressIndicator(
            progress = { overallFraction.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = NimazSpacing.ExtraSmall),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.qaida_lesson_progress, lessonIndex, totalLessons),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                NimazIcon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    iconSize = 18.dp,
                )
                Text(
                    text = " $totalStars",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        if (continueLabel != null) {
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
