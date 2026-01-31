package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButtonSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButtonStyle
import androidx.compose.ui.tooling.preview.Preview
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Full dua display card with Arabic, translation, transliteration, and actions.
 */
@Composable
fun DuaCard(
    duaTitle: String,
    arabicText: String,
    translation: String,
    modifier: Modifier = Modifier,
    transliteration: String? = null,
    showTransliteration: Boolean = false,
    source: String? = null,
    benefit: String? = null,
    repetitions: Int? = null,
    currentCount: Int = 0,
    isFavorite: Boolean = false,
    onFavoriteClick: (() -> Unit)? = null,
    onShareClick: (() -> Unit)? = null,
    onCopyClick: (() -> Unit)? = null,
    onPlayClick: (() -> Unit)? = null,
    onCountIncrement: (() -> Unit)? = null,
    onCountDecrement: (() -> Unit)? = null,
    onCountReset: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = duaTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // Actions
                Row {
                    if (onPlayClick != null) {
                        IconButton(onClick = onPlayClick, modifier = Modifier.size(40.dp)) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (onCopyClick != null) {
                        IconButton(onClick = onCopyClick, modifier = Modifier.size(40.dp)) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (onShareClick != null) {
                        IconButton(onClick = onShareClick, modifier = Modifier.size(40.dp)) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (onFavoriteClick != null) {
                        IconButton(onClick = onFavoriteClick, modifier = Modifier.size(40.dp)) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                                tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Arabic text
            ArabicText(
                text = arabicText,
                size = ArabicTextSize.LARGE,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )

            // Transliteration
            if (showTransliteration && transliteration != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = transliteration,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Translation
            Text(
                text = translation,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )

            // Source
            if (source != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Source: $source",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Benefit
            if (benefit != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Benefit",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = benefit,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            // Counter (if dua has recommended repetitions)
            if (repetitions != null && repetitions > 0 && onCountIncrement != null) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                DuaCounter(
                    currentCount = currentCount,
                    targetCount = repetitions,
                    onIncrement = onCountIncrement,
                    onDecrement = onCountDecrement,
                    onReset = onCountReset
                )
            }
        }
    }
}

/**
 * Dua counter section.
 */
@Composable
private fun DuaCounter(
    currentCount: Int,
    targetCount: Int,
    onIncrement: () -> Unit,
    onDecrement: (() -> Unit)? = null,
    onReset: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Recitation Counter",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Decrement button
            if (onDecrement != null) {
                NimazIconButton(
                    icon = Icons.Default.Remove,
                    onClick = onDecrement,
                    style = NimazIconButtonStyle.OUTLINED,
                    size = NimazIconButtonSize.MEDIUM,
                    enabled = currentCount > 0
                )
                Spacer(modifier = Modifier.width(16.dp))
            }

            // Count display
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$currentCount",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (currentCount >= targetCount) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = "of $targetCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Increment button
            NimazIconButton(
                icon = Icons.Default.Add,
                onClick = onIncrement,
                style = NimazIconButtonStyle.FILLED,
                size = NimazIconButtonSize.MEDIUM
            )
        }

        // Reset button
        if (onReset != null && currentCount > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            IconButton(onClick = onReset) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Reset",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Reset",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Completion indicator
        if (currentCount >= targetCount) {
            Spacer(modifier = Modifier.height(8.dp))
            NimazBadge(
                text = "Completed!",
                backgroundColor = MaterialTheme.colorScheme.primary,
                textColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Preview(showBackground = true, name = "Dua Card")
@Composable
private fun DuaCardPreview() {
    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            DuaCard(
                duaTitle = "Dua for entering the mosque",
                arabicText = "اللَّهُمَّ افْتَحْ لِي أَبْوَابَ رَحْمَتِكَ",
                translation = "O Allah, open for me the gates of Your mercy.",
                transliteration = "Allahumma iftah li abwaba rahmatik",
                showTransliteration = true,
                source = "Sahih Muslim",
                repetitions = 3,
                onFavoriteClick = {},
                onShareClick = {},
                onCopyClick = {},
                onPlayClick = {},
                onCountIncrement = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Dua Card with Counter")
@Composable
private fun DuaCardWithCounterPreview() {
    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            DuaCard(
                duaTitle = "Istighfar",
                arabicText = "أَسْتَغْفِرُ اللَّهَ",
                translation = "I seek forgiveness from Allah.",
                benefit = "The Prophet ﷺ said: \"By Allah, I seek forgiveness from Allah and turn to Him in repentance more than seventy times a day.\"",
                repetitions = 100,
                currentCount = 45,
                isFavorite = true,
                onFavoriteClick = {},
                onCountIncrement = {},
                onCountDecrement = {},
                onCountReset = {}
            )
        }
    }
}
