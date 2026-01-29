package com.arshadshah.nimaz.presentation.screens.hadith

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import android.content.Intent
import android.widget.Toast
import com.arshadshah.nimaz.domain.model.Hadith
import com.arshadshah.nimaz.domain.model.HadithGrade
import com.arshadshah.nimaz.presentation.components.atoms.HadithArabicText
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.HadithEvent
import com.arshadshah.nimaz.presentation.viewmodel.HadithViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithReaderScreen(
    bookId: String,
    chapterId: String,
    onNavigateBack: () -> Unit,
    viewModel: HadithViewModel = hiltViewModel()
) {
    val state by viewModel.readerState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    LaunchedEffect(chapterId, bookId) {
        // If bookId is empty and chapterId doesn't contain "_", it's a hadithId from search
        if (bookId.isEmpty() && !chapterId.contains("_")) {
            viewModel.onEvent(HadithEvent.LoadHadithById(chapterId))
        } else {
            viewModel.onEvent(HadithEvent.LoadChapter(chapterId))
        }
    }

    val currentHadith = state.hadiths.getOrNull(state.currentHadithIndex)
    val hasPrevious = state.currentHadithIndex > 0
    val hasNext = state.currentHadithIndex < state.hadiths.size - 1

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = state.chapter?.nameEnglish ?: "Loading...",
                subtitle = state.chapter?.let {
                    "Chapter ${it.chapterNumber}"
                },
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            currentHadith?.let { hadith ->
                BottomActionBar(
                    isBookmarked = hadith.isBookmarked,
                    onBookmarkClick = {
                        viewModel.onEvent(
                            HadithEvent.ToggleBookmark(
                                hadithId = hadith.id,
                                bookId = bookId,
                                hadithNumber = hadith.hadithNumber
                            )
                        )
                    },
                    onShareClick = {
                        val shareText = buildString {
                            appendLine(hadith.textArabic)
                            appendLine()
                            appendLine(hadith.textEnglish)
                            hadith.narratorName?.let {
                                appendLine()
                                appendLine("Narrated by: $it")
                            }
                            hadith.reference?.let {
                                appendLine()
                                appendLine(it)
                            }
                        }
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Hadith"))
                    },
                    onCopyClick = {
                        val text = buildString {
                            appendLine(hadith.textArabic)
                            appendLine()
                            appendLine(hadith.textEnglish)
                            hadith.narratorName?.let {
                                appendLine()
                                appendLine("Narrated by: $it")
                            }
                            hadith.reference?.let {
                                appendLine()
                                appendLine(it)
                            }
                        }
                        clipboardManager.setText(AnnotatedString(text))
                    }
                )
            }
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else if (currentHadith != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Navigation bar
                HadithNavigationBar(
                    currentNumber = currentHadith.hadithNumber,
                    totalCount = state.hadiths.size,
                    hasPrevious = hasPrevious,
                    hasNext = hasNext,
                    onPrevious = {
                        viewModel.onEvent(
                            HadithEvent.NavigateToHadith(state.currentHadithIndex - 1)
                        )
                    },
                    onNext = {
                        viewModel.onEvent(
                            HadithEvent.NavigateToHadith(state.currentHadithIndex + 1)
                        )
                    }
                )

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    // Grade badge
                    currentHadith.grade?.let { grade ->
                        GradeBadge(grade = grade)
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // Hadith card
                    HadithContentCard(
                        hadith = currentHadith,
                        showArabic = state.showArabic,
                        fontSize = state.fontSize,
                        arabicFontSize = state.arabicFontSize
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Chain of narration
                    currentHadith.narratorChain?.let { chain ->
                        if (chain.isNotBlank()) {
                            ChainOfNarrationSection(chain = chain)
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hadith found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HadithNavigationBar(
    currentNumber: Int,
    totalCount: Int,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .drawBehind {
                drawLine(
                    color = borderColor,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous button
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (hasPrevious) MaterialTheme.colorScheme.surfaceContainerHighest
                    else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f)
                )
                .clickable(enabled = hasPrevious, onClick = onPrevious)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (hasPrevious) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
            Text(
                text = "Previous",
                style = MaterialTheme.typography.labelMedium,
                color = if (hasPrevious) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
        }

        // Hadith number badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "#$currentNumber",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )
            Text(
                text = "of $totalCount",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Next button
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (hasNext) MaterialTheme.colorScheme.surfaceContainerHighest
                    else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f)
                )
                .clickable(enabled = hasNext, onClick = onNext)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Next",
                style = MaterialTheme.typography.labelMedium,
                color = if (hasNext) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (hasNext) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun GradeBadge(
    grade: HadithGrade,
    modifier: Modifier = Modifier
) {
    val (text, color, icon) = when (grade) {
        HadithGrade.SAHIH -> Triple(
            grade.displayName(),
            NimazColors.StatusColors.Prayed,
            Icons.Default.CheckCircle
        )
        HadithGrade.HASAN -> Triple(
            grade.displayName(),
            NimazColors.StatusColors.Late,
            Icons.Default.CheckCircle
        )
        HadithGrade.DAIF -> Triple(
            grade.displayName(),
            NimazColors.StatusColors.Missed,
            Icons.Default.Warning
        )
        HadithGrade.MAWDU -> Triple(
            grade.displayName(),
            Color.Gray,
            Icons.Default.Warning
        )
        HadithGrade.UNKNOWN -> Triple(
            grade.displayName(),
            Color.Gray,
            Icons.Default.Warning
        )
    }

    Row(
        modifier = modifier
            .background(
                color.copy(alpha = 0.15f),
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = color
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun HadithContentCard(
    hadith: Hadith,
    showArabic: Boolean,
    fontSize: Float,
    arabicFontSize: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        // Arabic section with gradient background
        if (showArabic) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceContainerHighest,
                                MaterialTheme.colorScheme.surfaceContainer
                            )
                        )
                    )
                    .drawBehind {
                        drawLine(
                            color = Color.White.copy(alpha = 0.08f),
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    .padding(horizontal = 20.dp, vertical = 25.dp)
            ) {
                HadithArabicText(
                    text = hadith.textArabic,
                    customFontSize = arabicFontSize,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // English section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 25.dp)
        ) {
            // Narrator badge
            if (!hadith.narratorName.isNullOrEmpty()) {
                Text(
                    text = "Narrated by ${hadith.narratorName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHighest,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
                Spacer(modifier = Modifier.height(15.dp))
            }

            // English translation
            Text(
                text = hadith.textEnglish,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * 1.8f).sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Reference
            hadith.reference?.let { ref ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = ref,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ChainOfNarrationSection(
    chain: String,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }

    // Parse chain - split by common delimiters
    val narrators = remember(chain) {
        chain.split("->", "←", "\n", " عن ")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(20.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Chain of Narration (Isnad)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (isExpanded) "Hide" else "Show",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { isExpanded = !isExpanded }
                    .padding(4.dp)
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier.padding(top = 15.dp)
            ) {
                if (narrators.size > 1) {
                    // Render as timeline
                    val lineColor = MaterialTheme.colorScheme.outlineVariant
                    val dotColor = MaterialTheme.colorScheme.outlineVariant
                    val bgColor = MaterialTheme.colorScheme.surface

                    Column(
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .drawBehind {
                                drawLine(
                                    color = lineColor,
                                    start = Offset(0f, 0f),
                                    end = Offset(0f, size.height),
                                    strokeWidth = 2.dp.toPx()
                                )
                            }
                    ) {
                        narrators.forEach { narrator ->
                            Row(
                                modifier = Modifier.padding(
                                    start = 15.dp,
                                    top = 10.dp,
                                    bottom = 10.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Dot on the timeline
                                Box(
                                    modifier = Modifier
                                        .offset(x = (-21).dp)
                                        .size(12.dp)
                                        .background(bgColor, CircleShape)
                                        .border(2.dp, dotColor, CircleShape)
                                )
                                Text(
                                    text = narrator,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.offset(x = (-15).dp)
                                )
                            }
                        }
                    }
                } else {
                    // Single block of text for the chain
                    Text(
                        text = chain,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomActionBar(
    isBookmarked: Boolean,
    onBookmarkClick: () -> Unit,
    onShareClick: () -> Unit,
    onCopyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        MaterialTheme.colorScheme.surface
                    ),
                    startY = 0f,
                    endY = 80f
                )
            )
            .padding(horizontal = 20.dp, vertical = 15.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(16.dp)
                )
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ActionButton(
                icon = {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = if (isBookmarked) "Saved" else "Save",
                isActive = isBookmarked,
                onClick = onBookmarkClick,
                modifier = Modifier.weight(1f)
            )

            ActionButton(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = "Share",
                onClick = onShareClick,
                modifier = Modifier.weight(1f)
            )

            ActionButton(
                icon = {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = "Copy",
                onClick = onCopyClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ActionButton(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false
) {
    val activeColor = NimazColors.StatusColors.Late // Gold-like color for active state
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier,
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides
                        if (isActive) activeColor else inactiveColor
            ) {
                icon()
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = if (isActive) activeColor else inactiveColor
        )
    }
}
