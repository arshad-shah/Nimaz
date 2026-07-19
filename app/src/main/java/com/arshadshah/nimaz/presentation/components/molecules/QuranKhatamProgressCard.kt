package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
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
 * Shows the same numbers as the shared [KhatamHeroCard] — progress ring, "N of M ayahs
 * read", juz, days left and the pace verdict — but in the compact row form
 * ([KhatamCompactRow]), because on the Quran home tab khatam sits below the
 * continue-reading card and still has to be visible in the first screenful.
 *
 * When there is no active khatam the slot collapses to a one-row prompt rather than a
 * full-height empty state, so an inactive khatam no longer costs as much space as an
 * active one.
 */
@Composable
internal fun KhatamProgressCard(
    activeKhatam: Khatam?,
    insights: KhatamInsights?,
    completedCount: Int,
    onClickActive: (Long) -> Unit,
    onClickStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (activeKhatam != null) {
        KhatamCompactRow(
            khatam = activeKhatam,
            insights = insights ?: KhatamInsights(),
            modifier = modifier,
            onClick = { onClickActive(activeKhatam.id) },
        )
    } else {
        KhatamStartPromptRow(
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
            actionLabel = stringResource(R.string.khatam_start_new),
            onAction = onClickStart,
            modifier = modifier,
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
