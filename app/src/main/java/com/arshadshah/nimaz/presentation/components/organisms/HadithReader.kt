package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.components.atoms.getHadithGradeBadgeColors
import com.arshadshah.nimaz.presentation.components.molecules.HadithCard
import com.arshadshah.nimaz.presentation.components.molecules.HadithListItem

/**
 * Data class representing a Hadith for the reader.
 */
data class HadithReaderData(
    val id: String,
    val hadithNumber: Int,
    val arabicText: String,
    val englishText: String,
    val narratorChain: String?,
    val grade: String?,
    val bookName: String,
    val chapterName: String,
    val collectionName: String = "",
    val reference: String?,
    val isBookmarked: Boolean = false
)

/**
 * Complete Hadith reader with full text and navigation.
 */
@Composable
fun HadithReader(
    hadith: HadithReaderData,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    showArabic: Boolean = true,
    arabicTextSize: ArabicTextSize = ArabicTextSize.MEDIUM,
    hasPrevious: Boolean = false,
    hasNext: Boolean = false,
    onPreviousClick: () -> Unit = {},
    onNextClick: () -> Unit = {},
    onBookmarkClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onCopyClick: () -> Unit = {}
) {
    var showNarratorChain by remember { mutableStateOf(false) }

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
                        Text(
                            text = hadith.bookName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = hadith.chapterName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Hadith #${hadith.hadithNumber}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )

                            if (hadith.grade != null) {
                                Spacer(modifier = Modifier.width(12.dp))
                                val (bgColor, textColor) = getHadithGradeBadgeColors(hadith.grade)
                                NimazBadge(
                                    text = hadith.grade,
                                    backgroundColor = bgColor,
                                    textColor = textColor,
                                    size = NimazBadgeSize.MEDIUM
                                )
                            }
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
                    IconButton(onClick = onBookmarkClick) {
                        Icon(
                            imageVector = if (hadith.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (hadith.isBookmarked) "Remove bookmark" else "Add bookmark",
                            tint = if (hadith.isBookmarked) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                // Narrator chain (expandable)
                if (hadith.narratorChain != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Narrator Chain (Isnad)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                IconButton(
                                    onClick = { showNarratorChain = !showNarratorChain },
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Icon(
                                        imageVector = if (showNarratorChain) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = if (showNarratorChain) "Collapse" else "Expand",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            AnimatedVisibility(
                                visible = showNarratorChain,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Text(
                                    text = hadith.narratorChain,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(
                                        start = 12.dp,
                                        end = 12.dp,
                                        bottom = 12.dp
                                    )
                                )
                            }
                        }
                    }
                }

                // Arabic text
                if (showArabic) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        ArabicText(
                            text = hadith.arabicText,
                            size = arabicTextSize,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }

                // English translation
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
                            text = hadith.englishText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.5
                        )
                    }
                }

                // Reference
                if (hadith.reference != null) {
                    Text(
                        text = hadith.reference,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Navigation controls
        HadithNavigationBar(
            hasPrevious = hasPrevious,
            hasNext = hasNext,
            onPreviousClick = onPreviousClick,
            onNextClick = onNextClick,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/**
 * Navigation bar for Hadith reader.
 */
@Composable
private fun HadithNavigationBar(
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
                    contentDescription = "Previous hadith",
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
                    contentDescription = "Next hadith",
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
 * Hadith list view for browsing collections.
 */
@Composable
fun HadithListReader(
    hadiths: List<HadithReaderData>,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onHadithClick: (HadithReaderData) -> Unit = {},
    onBookmarkClick: (HadithReaderData) -> Unit = {}
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (hadiths.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No hadiths found",
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
                    items = hadiths,
                    key = { it.id }
                ) { hadith ->
                    HadithListItem(
                        hadithNumber = hadith.hadithNumber,
                        arabicText = hadith.arabicText,
                        translationText = hadith.englishText.take(150) + if (hadith.englishText.length > 150) "..." else "",
                        collectionName = hadith.collectionName.ifEmpty { hadith.bookName },
                        grade = hadith.grade,
                        narrator = hadith.narratorChain?.take(50),
                        isBookmarked = hadith.isBookmarked,
                        onHadithClick = { onHadithClick(hadith) },
                        onBookmarkClick = { onBookmarkClick(hadith) }
                    )
                }
            }
        }
    }
}

/**
 * Chapter view with hadiths grouped by chapter.
 */
@Composable
fun HadithChapterReader(
    chapterName: String,
    chapterNumber: Int,
    hadiths: List<HadithReaderData>,
    modifier: Modifier = Modifier,
    bookName: String? = null,
    onHadithClick: (HadithReaderData) -> Unit = {},
    onBookmarkClick: (HadithReaderData) -> Unit = {}
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Chapter header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (bookName != null) {
                        Text(
                            text = bookName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "Chapter $chapterNumber",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = chapterName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${hadiths.size} Hadiths",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Hadiths
        items(
            items = hadiths,
            key = { it.id }
        ) { hadith ->
            HadithCard(
                hadithNumber = hadith.hadithNumber,
                arabicText = hadith.arabicText,
                translation = hadith.englishText,
                collectionName = hadith.collectionName.ifEmpty { hadith.bookName },
                bookName = hadith.chapterName,
                narratorChain = hadith.narratorChain,
                grade = hadith.grade,
                reference = hadith.reference,
                isBookmarked = hadith.isBookmarked,
                onBookmarkClick = { onBookmarkClick(hadith) }
            )
        }
    }
}
