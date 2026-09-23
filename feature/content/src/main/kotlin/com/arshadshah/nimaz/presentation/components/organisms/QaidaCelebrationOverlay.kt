package com.arshadshah.nimaz.presentation.components.organisms

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.feature.content.R as ContentR
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.QaidaStarRow
import com.arshadshah.nimaz.presentation.theme.NimazSpacing
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * The festive lesson-complete moment.
 *
 * Uses a dedicated teal-and-gold illustration so the completion state feels like
 * a real reward while staying consistent with Nimaz's dark surfaces and Qaida
 * palette. The hero is rendered with ContentScale.Fit so the approved square
 * artwork is never cropped or stretched; Android only scales it down for the
 * device. The earned stars remain dynamic and sit over the lower edge of the
 * artwork, followed by the completion copy, unlock chip, and Map / Next actions.
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
        enter = fadeIn() + scaleIn(initialScale = 0.9f),
        exit = fadeOut() + scaleOut(targetScale = 0.9f),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.68f)),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(horizontal = NimazSpacing.Large),
                shape = RoundedCornerShape(30.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 18.dp,
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = NimazSpacing.Large,
                        end = NimazSpacing.Large,
                        top = NimazSpacing.Large,
                        bottom = NimazSpacing.ExtraLarge,
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(NimazSpacing.Medium),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Image(
                            painter = painterResource(ContentR.drawable.qaida_lesson_complete_hero),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )

                        Surface(
                            modifier = Modifier.padding(bottom = 10.dp),
                            shape = RoundedCornerShape(percent = 50),
                            color = Color.Black.copy(alpha = 0.48f),
                            tonalElevation = 0.dp,
                        ) {
                            QaidaStarRow(
                                filled = stars.coerceIn(0, 3),
                                starSize = 24.dp,
                                modifier = Modifier.padding(
                                    horizontal = NimazSpacing.Medium,
                                    vertical = NimazSpacing.ExtraSmall,
                                ),
                            )
                        }
                    }

                    ArabicText(
                        text = stringResource(R.string.qaida_mashaallah),
                        size = ArabicTextSize.LARGE,
                        color = MaterialTheme.colorScheme.secondary,
                    )

                    Text(
                        text = stringResource(R.string.qaida_lesson_complete),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Text(
                        text = stringResource(R.string.qaida_lesson_learned, lessonTitle),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    if (unlockedTitle != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(percent = 50),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            tonalElevation = 2.dp,
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = NimazSpacing.Medium,
                                    vertical = NimazSpacing.Medium,
                                ),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                NimazIcon(
                                    imageVector = Icons.Filled.LockOpen,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.height(18.dp),
                                )
                                Text(
                                    text = stringResource(
                                        R.string.qaida_new_lesson_unlocked,
                                        unlockedTitle,
                                    ),
                                    modifier = Modifier.padding(start = NimazSpacing.Small),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
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
    name = "Celebration — 2 stars + unlock",
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
    name = "Celebration — 3 stars, no unlock",
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
    showBackground = true,
    widthDp = 412,
    heightDp = 720,
    name = "Celebration — Dark",
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
