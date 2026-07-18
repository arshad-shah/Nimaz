package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.Khatam
import com.arshadshah.nimaz.domain.model.KhatamInsights
import com.arshadshah.nimaz.domain.model.KhatamPace
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * The Quran tab's entry point into Khatam.
 *
 * Was a third bespoke hero layout with its own hand-rolled gradient bar and stat columns;
 * it now delegates to the shared [KhatamHeroCard] so Home, the Khatam list and the Khatam
 * detail screen cannot drift apart.
 */
@Composable
internal fun KhatamProgressCard(
    activeKhatam: Khatam?,
    insights: KhatamInsights?,
    completedCount: Int,
    onClickActive: (Long) -> Unit,
    onClickStart: () -> Unit,
    modifier: Modifier = Modifier,
    continueLabel: String? = null,
    onContinue: (() -> Unit)? = null,
) {
    if (activeKhatam != null) {
        KhatamHeroCard(
            khatam = activeKhatam,
            insights = insights ?: KhatamInsights(),
            modifier = modifier,
            continueLabel = continueLabel,
            onContinue = onContinue,
            onClick = { onClickActive(activeKhatam.id) },
        )
    } else {
        NimazEmptyState(
            title = stringResource(R.string.khatam_no_started),
            message = if (completedCount > 0) {
                pluralStringResource(
                    R.plurals.khatam_completed_lifetime,
                    completedCount,
                    completedCount,
                )
            } else {
                stringResource(R.string.khatam_start_journey)
            },
            icon = Icons.AutoMirrored.Filled.MenuBook,
            iconTint = MaterialTheme.colorScheme.primary,
            actionLabel = stringResource(R.string.khatam_start_new),
            onAction = onClickStart,
            modifier = modifier.fillMaxWidth(),
        )
    }
}

// ---- Previews ----

private val previewCardInsights = KhatamInsights(
    daysActive = 21,
    averagePace = 111f,
    currentStreak = 7,
    juzCompleted = 8,
    remainingAyahs = 3896,
    estimatedDaysRemaining = 44,
    paceStatus = KhatamPace.ON_TRACK,
)

@Composable
private fun KhatamProgressCardShowcase() {
    Column(Modifier.padding(16.dp)) {
        KhatamProgressCard(
            activeKhatam = Khatam(
                id = 1,
                name = "Ramadan 1447",
                isActive = true,
                totalAyahsRead = 2340,
                startedAt = System.currentTimeMillis(),
            ),
            insights = previewCardInsights,
            completedCount = 1,
            onClickActive = {},
            onClickStart = {},
            continueLabel = "Continue · Surah 8, 12",
            onContinue = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 380, name = "Khatam Progress Card — Light")
@Composable
private fun KhatamProgressCardLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { KhatamProgressCardShowcase() }
}

@Preview(showBackground = true, widthDp = 380, name = "Khatam Progress Card — Dark")
@Composable
private fun KhatamProgressCardDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { KhatamProgressCardShowcase() }
}

@Preview(showBackground = true, widthDp = 380, name = "Khatam Progress Card — Empty")
@Composable
private fun KhatamProgressCardEmptyPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        Column(Modifier.padding(16.dp)) {
            KhatamProgressCard(
                activeKhatam = null,
                insights = null,
                completedCount = 0,
                onClickActive = {},
                onClickStart = {},
            )
        }
    }
}
