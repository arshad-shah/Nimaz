package com.arshadshah.nimaz.presentation.screens.hadith

import android.annotation.SuppressLint
import android.content.ClipData
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazPager
import com.arshadshah.nimaz.presentation.components.atoms.rememberNimazPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.share.ContentShareManager
import com.arshadshah.nimaz.core.share.Shareables
import com.arshadshah.nimaz.domain.model.Hadith
import com.arshadshah.nimaz.presentation.components.atoms.HadithArabicText
import com.arshadshah.nimaz.presentation.components.atoms.NimazLabelChip
import com.arshadshah.nimaz.presentation.components.atoms.NimazPillActionButton
import com.arshadshah.nimaz.presentation.components.molecules.NimazReaderBottomBar
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.AdaptiveSpacing
import com.arshadshah.nimaz.presentation.viewmodel.HadithEvent
import com.arshadshah.nimaz.presentation.viewmodel.HadithReaderUiState
import com.arshadshah.nimaz.presentation.viewmodel.HadithViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithReaderScreen(
    bookId: String,
    chapterId: String,
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    viewModel: HadithViewModel = hiltViewModel()
) {
    val state by viewModel.readerState.collectAsState()
    val hadiths = state.hadiths
    val scope = rememberCoroutineScope()

    val pagerState = rememberNimazPagerState(
        initialPage = state.currentHadithIndex.coerceAtLeast(0),
        pageCount = { hadiths.size }
    )

    LaunchedEffect(chapterId, bookId) {
        // If bookId is empty and chapterId isn't a "book_chapter" id, it's a hadithId from search.
        if (bookId.isEmpty() && !chapterId.contains("_")) {
            viewModel.onEvent(HadithEvent.LoadHadithById(chapterId))
        } else {
            viewModel.onEvent(HadithEvent.LoadChapter(chapterId))
        }
    }

    LaunchedEffect(state.currentHadithIndex, hadiths.size) {
        if (hadiths.isNotEmpty()) {
            pagerState.scrollToPage(state.currentHadithIndex.coerceIn(0, hadiths.lastIndex))
        }
    }

    val currentHadith = hadiths.getOrNull(pagerState.currentPage)

    Scaffold(
        topBar = {
            NimazBackTopAppBar(
                title = state.chapter?.nameEnglish ?: stringResource(R.string.loading),
                subtitle = state.chapter?.let {
                    stringResource(R.string.hadith_chapter_format, it.chapterNumber)
                },
                onBackClick = onNavigateBack,
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        NimazIcon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.hadith_settings),
                            variant = NimazIconVariant.MUTED
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading && hadiths.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                hadiths.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.no_hadith_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        NimazPager(
                            state = pagerState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            key = { hadiths[it].id }
                        ) { page ->
                            HadithPage(hadith = hadiths[page], state = state)
                        }

                        currentHadith?.let { hadith ->
                            HadithReaderBottomBar(
                                hadith = hadith,
                                viewModel = viewModel,
                                currentPage = pagerState.currentPage,
                                pageCount = hadiths.size,
                                onPrev = {
                                    scope.launch {
                                        pagerState.animateScrollToPage(
                                            (pagerState.currentPage - 1).coerceAtLeast(0)
                                        )
                                    }
                                },
                                onNext = {
                                    scope.launch {
                                        pagerState.animateScrollToPage(
                                            (pagerState.currentPage + 1).coerceAtMost(hadiths.lastIndex)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HadithPage(
    hadith: Hadith,
    state: HadithReaderUiState
) {
    val grade = if (state.showGrade) hadithGradeDisplay(hadith.grade) else null

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val minColumnHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = AdaptiveSpacing.maxReadableWidth())
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .heightIn(min = minColumnHeight),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (grade != null || !hadith.narratorName.isNullOrEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    grade?.let { HadithGradeChip(label = it.label, color = it.color) }
                    hadith.narratorName?.trim()?.takeIf { it.isNotBlank() }?.let { narrator ->
                        // The dataset's narrator field already includes the
                        // "Narrated by …" prefix; only add it when it's missing.
                        val narratorText = if (narrator.startsWith("narrat", ignoreCase = true)) {
                            narrator
                        } else {
                            stringResource(R.string.hadith_narrated_by_format, narrator)
                        }
                        NimazLabelChip(text = narratorText, highlighted = true)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            if (state.showArabic) {
                HadithArabicText(
                    text = hadith.textArabic,
                    customFontSize = state.arabicFontSize,
                    fontFamily = state.arabicFontFamily,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (state.showTranslation) {
                if (state.showArabic) {
                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
                Text(
                    text = hadith.textEnglish,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = state.fontSize.sp,
                        lineHeight = (state.fontSize * 1.6f).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            if (!hadith.reference.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                NimazLabelChip(text = hadith.reference, icon = Icons.Default.Book)
            }

            if (state.showChain && !hadith.narratorChain.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(20.dp))
                ChainOfNarrationSection(
                    chain = hadith.narratorChain,
                    arabicFontFamily = state.arabicFontFamily
                )
            }
        }
    }
}

/** Collapsible, frosted chain-of-narration (isnād) section. Collapsed by default. */
@Composable
private fun ChainOfNarrationSection(
    chain: String,
    arabicFontFamily: FontFamily,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (isExpanded) 180f else 0f, label = "isnad_chevron")

    val narrators = remember(chain) {
        chain.split("->", "←", "\n", " عن ")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                RoundedCornerShape(14.dp)
            )
            .clickable { isExpanded = !isExpanded }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.hadith_isnad),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            NimazIcon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                variant = NimazIconVariant.PRIMARY,
                modifier = Modifier.rotate(rotation)
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(top = 15.dp)) {
                if (narrators.size > 1) {
                    val lineColor = MaterialTheme.colorScheme.outlineVariant
                    val dotColor = MaterialTheme.colorScheme.primary
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
                                modifier = Modifier.padding(start = 15.dp, top = 10.dp, bottom = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .offset(x = (-21).dp)
                                        .size(12.dp)
                                        .background(bgColor, CircleShape)
                                        .border(2.dp, dotColor, CircleShape)
                                )
                                Text(
                                    text = narrator,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = arabicFontFamily,
                                        textDirection = TextDirection.Rtl
                                    ),
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.offset(x = (-15).dp)
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = chain,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = arabicFontFamily,
                            textDirection = TextDirection.Rtl,
                            lineHeight = 26.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * The consolidated bottom bar for the current hadith: Bookmark · Share · Copy,
 * flanked by prev/next chevrons + page dots. Reuses [NimazReaderBottomBar].
 */
@SuppressLint("LocalContextGetResourceValueCall")
@Composable
private fun HadithReaderBottomBar(
    hadith: Hadith,
    viewModel: HadithViewModel,
    currentPage: Int,
    pageCount: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val clipboardScope = rememberCoroutineScope()
    val bookmarkFlow = remember(hadith.id) { viewModel.isHadithBookmarked(hadith.id) }
    val isBookmarked by bookmarkFlow.collectAsState(initial = hadith.isBookmarked)

    val copiedMsg = stringResource(R.string.hadith_copied)
    // Resolve the template in composable scope (stringResource can't be called from the
    // non-composable text builder below), then format it per narrator name.
    val narratedByTemplate = stringResource(R.string.hadith_narrated_by_format)
    val narratedByFmt = { name: String -> String.format(narratedByTemplate, name) }

    fun buildHadithText(): String = buildString {
        appendLine(hadith.textArabic)
        appendLine()
        appendLine(hadith.textEnglish)
        hadith.narratorName?.takeIf { it.isNotBlank() }?.let {
            appendLine()
            appendLine(narratedByFmt(it))
        }
        hadith.reference?.takeIf { it.isNotBlank() }?.let {
            appendLine()
            appendLine(it)
        }
    }

    NimazReaderBottomBar(
        currentPage = currentPage,
        pageCount = pageCount,
        onPrev = onPrev,
        onNext = onNext,
        prevContentDescription = stringResource(R.string.previous),
        nextContentDescription = stringResource(R.string.next)
    ) {
        NimazPillActionButton(
            icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
            contentDescription = stringResource(R.string.cd_bookmark),
            onClick = {
                viewModel.onEvent(
                    HadithEvent.ToggleBookmark(
                        hadithId = hadith.id,
                        bookId = hadith.bookId,
                        hadithNumber = hadith.hadithNumber
                    )
                )
            },
            active = isBookmarked,
            activeColor = NimazColors.Gold500
        )
        NimazPillActionButton(
            icon = Icons.Default.Share,
            contentDescription = stringResource(R.string.cd_share),
            onClick = {
                clipboardScope.launch {
                    ContentShareManager.shareBranded(
                        context,
                        Shareables.hadith(context, hadith)
                    )
                }
            }
        )
        NimazPillActionButton(
            icon = Icons.Default.ContentCopy,
            contentDescription = stringResource(R.string.cd_copy),
            onClick = {
                clipboardScope.launch {
                    clipboard.setClipEntry(
                        ClipEntry(ClipData.newPlainText("Hadith", buildHadithText()))
                    )
                    Toast.makeText(context, copiedMsg, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}
