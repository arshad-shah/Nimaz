package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.QaidaStarRow
import com.arshadshah.nimaz.presentation.theme.NimazCornerRadius
import com.arshadshah.nimaz.presentation.theme.NimazSpacing
import kotlinx.coroutines.delay

/**
 * The festive lesson-complete moment — the one intentionally celebratory screen.
 * Stars reveal one-by-one over a scrim; "ما شاء الله", what was learned, the
 * newly unlocked lesson, and Map / Next actions. Gold sparkles give the lift
 * while staying within the app's colour system.
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
        enter = fadeIn(),
        exit = fadeOut(),
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
                    .fillMaxWidth()
                    .padding(NimazSpacing.ExtraLarge),
                shape = RoundedCornerShape(NimazCornerRadius.ExtraLarge),
                color = MaterialTheme.colorScheme.secondaryContainer,
                tonalElevation = 6.dp,
            ) {
                Column(
                    modifier = Modifier.padding(NimazSpacing.ExtraLarge),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(NimazSpacing.Medium),
                ) {
                    Text(text = "✨", style = MaterialTheme.typography.displaySmall)
                    QaidaStarRow(filled = revealed, starSize = 44.dp)
                    ArabicText(
                        text = "ما شاء الله",
                        size = ArabicTextSize.LARGE,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        text = "Lesson complete!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        text = "You learned $lessonTitle.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    if (unlockedTitle != null) {
                        Surface(
                            shape = RoundedCornerShape(percent = 50),
                            color = MaterialTheme.colorScheme.secondary,
                        ) {
                            Text(
                                text = "🔓 New lesson: $unlockedTitle",
                                modifier = Modifier.padding(
                                    horizontal = NimazSpacing.Medium,
                                    vertical = NimazSpacing.Small,
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondary,
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = NimazSpacing.Small),
                        horizontalArrangement = Arrangement.spacedBy(NimazSpacing.Medium),
                    ) {
                        OutlinedButton(
                            onClick = onMap,
                            modifier = Modifier.weight(1f),
                        ) { Text("Map") }
                        Button(
                            onClick = onNext,
                            modifier = Modifier.weight(1f),
                        ) { Text("Next lesson") }
                    }
                }
            }
        }
    }
}
