package com.arshadshah.nimaz.presentation.components.organisms

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.QaidaCelebrationBurst
import com.arshadshah.nimaz.presentation.components.atoms.QaidaStarRow
import com.arshadshah.nimaz.presentation.theme.NimazSpacing
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import kotlinx.coroutines.delay

/**
 * The festive lesson-complete moment — the one intentionally celebratory screen.
 *
 * Built in the [com.arshadshah.nimaz.presentation.components.molecules.NimazDialog]
 * design language (28dp rounded surface, tonal elevation, the shared spacing/
 * radius tokens) but as a bespoke, fully Canvas-driven celebration: the
 * [QaidaCelebrationBurst] hero (rotating gold sunburst + pulsing halo +
 * twinkling sparkles + an eight-point Islamic star) sits behind the three earned
 * stars, which reveal one-by-one. No emoji, no images — everything is icons and
 * Canvas art. "ما شاء الله", what was learned, the newly-unlocked lesson, and
 * Map / Next actions follow. The card pops in over a dimmed scrim.
 */
@Composable
fun QaidaCelebrationOverlay(
    visible: Boolean,
    stars: Int,
    lessonTitle: String,
    unlockedTitle: String?,
    onNext: () -> Unit,
    onMap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.85f),
        exit = fadeOut() + scaleOut(targetScale = 0.85f),
        modifier = modifier,
    ) {
        var revealed by remember { mutableIntStateOf(0) }
        LaunchedEffect(visible, stars) {
            revealed = 0
            if (visible) {
                repeat(stars) {
                    delay(350)
                    revealed += 1
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(NimazSpacing.ExtraLarge),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
            ) {
                Column(
                    modifier = Modifier.padding(NimazSpacing.ExtraLarge),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(NimazSpacing.Medium),
                ) {
                    // Hero: the Canvas celebration burst with the earned stars
                    // revealing on top of it.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        QaidaCelebrationBurst(modifier = Modifier.fillMaxSize())
                        QaidaStarRow(filled = revealed, starSize = 44.dp)
                    }

                    ArabicText(
                        text = stringResource(R.string.qaida_mashaallah),
                        size = ArabicTextSize.LARGE,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        text = stringResource(R.string.qaida_lesson_complete),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.qaida_lesson_learned, lessonTitle),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (unlockedTitle != null) {
                        Surface(
                            shape = RoundedCornerShape(percent = 50),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = NimazSpacing.Medium,
                                    vertical = NimazSpacing.Small,
                                ),
                                horizontalArrangement = Arrangement.spacedBy(NimazSpacing.Small),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                NimazIcon(
                                    imageVector = Icons.Filled.LockOpen,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.height(16.dp),
                                )
                                Text(
                                    text = stringResource(
                                        R.string.qaida_new_lesson_unlocked,
                                        unlockedTitle
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = NimazSpacing.Small),
                        horizontalArrangement = Arrangement.spacedBy(NimazSpacing.Medium),
                    ) {
                        NimazButton(
                            text = stringResource(R.string.qaida_map),
                            onClick = onMap,
                            modifier = Modifier.weight(1f),
                            variant = NimazButtonVariant.OUTLINED,
                        )
                        NimazButton(
                            text = stringResource(R.string.qaida_next_lesson),
                            onClick = onNext,
                            modifier = Modifier.weight(1f),
                            variant = NimazButtonVariant.FILLED,
                        )
                    }
                }
            }
        }
    }
}

@Preview(
    showBackground = true,
    widthDp = 412,
    heightDp = 720,
    name = "Celebration — 2 stars + unlock"
)
@Composable
private fun QaidaCelebrationOverlayPreview() {
    NimazTheme {
        QaidaCelebrationOverlay(
            visible = true,
            stars = 2,
            lessonTitle = "The Letters",
            unlockedTitle = "Joined Letters",
            onNext = {},
            onMap = {},
        )
    }
}

@Preview(
    showBackground = true,
    widthDp = 412,
    heightDp = 720,
    name = "Celebration — 3 stars, no unlock"
)
@Composable
private fun QaidaCelebrationOverlayFullPreview() {
    NimazTheme {
        QaidaCelebrationOverlay(
            visible = true,
            stars = 3,
            lessonTitle = "Joined Letters",
            unlockedTitle = null,
            onNext = {},
            onMap = {},
        )
    }
}

@Preview(
    showBackground = true, widthDp = 412, heightDp = 720, name = "Celebration — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun QaidaCelebrationOverlayDarkPreview() {
    NimazTheme {
        QaidaCelebrationOverlay(
            visible = true,
            stars = 1,
            lessonTitle = "The Letters",
            unlockedTitle = "Joined Letters",
            onNext = {},
            onMap = {},
        )
    }
}
