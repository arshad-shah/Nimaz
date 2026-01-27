package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.DuaArabicText
import com.arshadshah.nimaz.presentation.components.molecules.DuaListItem
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Data class representing a Dua for the reader.
 */
data class DuaReaderData(
    val id: String,
    val title: String,
    val titleArabic: String?,
    val arabicText: String,
    val transliteration: String?,
    val translation: String,
    val reference: String?,
    val benefits: String?,
    val occasion: String?,
    val repeatCount: Int = 1,
    val isFavorite: Boolean = false,
    val categoryName: String? = null
)

/**
 * Complete Dua reader with full text, counter, and audio controls.
 */
@Composable
fun DuaReader(
    dua: DuaReaderData,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    showTransliteration: Boolean = true,
    arabicTextSize: ArabicTextSize = ArabicTextSize.LARGE,
    isPlaying: Boolean = false,
    hasPrevious: Boolean = false,
    hasNext: Boolean = false,
    onPreviousClick: () -> Unit = {},
    onNextClick: () -> Unit = {},
    onFavoriteClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onCopyClick: () -> Unit = {},
    onPlayClick: () -> Unit = {},
    onStopClick: () -> Unit = {}
) {
    var currentCount by remember { mutableIntStateOf(0) }
    var showBenefits by remember { mutableStateOf(false) }

    val progress by animateFloatAsState(
        targetValue = if (dua.repeatCount > 0) currentCount.toFloat() / dua.repeatCount else 0f,
        label = "counter_progress"
    )

    Box(modifier = modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (dua.categoryName != null) {
                            Text(
                                text = dua.categoryName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        Text(
                            text = dua.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        if (dua.titleArabic != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            ArabicText(
                                text = dua.titleArabic,
                                size = ArabicTextSize.MEDIUM,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (dua.occasion != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = dua.occasion,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onCopyClick) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onShareClick) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = if (isPlaying) onStopClick else onPlayClick) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Stop audio" else "Play audio",
                            tint = if (isPlaying) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    IconButton(onClick = onFavoriteClick) {
                        Icon(
                            imageVector = if (dua.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (dua.isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (dua.isFavorite) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                // Arabic text
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    DuaArabicText(
                        text = dua.arabicText,
                        size = arabicTextSize,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    )
                }

                // Transliteration
                if (showTransliteration && dua.transliteration != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Transliteration",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = dua.transliteration,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                }

                // Translation
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Translation",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = dua.translation,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.5
                        )
                    }
                }

                // Benefits (expandable)
                if (dua.benefits != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        onClick = { showBenefits = !showBenefits }
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Benefits & Virtues",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = if (showBenefits) "▲" else "▼",
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            AnimatedVisibility(
                                visible = showBenefits,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Text(
                                    text = dua.benefits,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(
                                        start = 16.dp,
                                        end = 16.dp,
                                        bottom = 16.dp
                                    )
                                )
                            }
                        }
                    }
                }

                // Reference
                if (dua.reference != null) {
                    Text(
                        text = dua.reference,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }

                // Counter section
                if (dua.repeatCount > 1) {
                    DuaCounter(
                        currentCount = currentCount,
                        targetCount = dua.repeatCount,
                        progress = progress,
                        onIncrement = { if (currentCount < dua.repeatCount) currentCount++ },
                        onDecrement = { if (currentCount > 0) currentCount-- },
                        onReset = { currentCount = 0 },
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Navigation controls
        DuaNavigationBar(
            hasPrevious = hasPrevious,
            hasNext = hasNext,
            onPreviousClick = onPreviousClick,
            onNextClick = onNextClick,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/**
 * Counter component for Dua repetitions.
 */
@Composable
private fun DuaCounter(
    currentCount: Int,
    targetCount: Int,
    progress: Float,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Repeat Counter",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Circular progress with count
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = if (currentCount >= targetCount) {
                        NimazColors.TasbihColors.Complete
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 8.dp
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$currentCount",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "/ $targetCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Counter controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDecrement,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrement"
                    )
                }

                FloatingActionButton(
                    onClick = onIncrement,
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increment",
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(
                    onClick = onReset,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = "↻",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            if (currentCount >= targetCount) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Completed!",
                    style = MaterialTheme.typography.labelLarge,
                    color = NimazColors.TasbihColors.Complete,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Navigation bar for Dua reader.
 */
@Composable
private fun DuaNavigationBar(
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPreviousClick,
                enabled = hasPrevious
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                    contentDescription = "Previous dua",
                    tint = if (hasPrevious) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    }
                )
            }

            Text(
                text = "Swipe or tap to navigate",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            IconButton(
                onClick = onNextClick,
                enabled = hasNext
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                    contentDescription = "Next dua",
                    tint = if (hasNext) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    }
                )
            }
        }
    }
}

/**
 * Dua list view for browsing categories.
 */
@Composable
fun DuaListReader(
    duas: List<DuaReaderData>,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onDuaClick: (DuaReaderData) -> Unit = {},
    onFavoriteClick: (DuaReaderData) -> Unit = {}
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (duas.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No duas found",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = duas,
                    key = { it.id }
                ) { dua ->
                    DuaListItem(
                        duaTitle = dua.title,
                        arabicText = dua.arabicText,
                        translation = dua.translation.take(150) + if (dua.translation.length > 150) "..." else "",
                        source = dua.reference,
                        isFavorite = dua.isFavorite,
                        onDuaClick = { onDuaClick(dua) },
                        onFavoriteClick = { onFavoriteClick(dua) }
                    )
                }
            }
        }
    }
}

// Previews
@Preview(showBackground = true)
@Composable
private fun DuaReaderPreview() {
    NimazTheme {
        DuaReader(
            dua = DuaReaderData(
                id = "1",
                title = "Dua Before Eating",
                titleArabic = "دعاء قبل الطعام",
                arabicText = "بِسْمِ اللهِ",
                transliteration = "Bismillah",
                translation = "In the name of Allah",
                reference = "Bukhari",
                benefits = "Mentioning the name of Allah before eating protects the food from evil",
                occasion = "Before eating any meal",
                repeatCount = 3,
                categoryName = "Food & Drink"
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DuaCounterPreview() {
    NimazTheme {
        DuaCounter(
            currentCount = 5,
            targetCount = 10,
            progress = 0.5f,
            onIncrement = {},
            onDecrement = {},
            onReset = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
