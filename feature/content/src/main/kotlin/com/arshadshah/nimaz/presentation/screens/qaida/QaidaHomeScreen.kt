package com.arshadshah.nimaz.presentation.screens.qaida

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.arshadshah.nimaz.presentation.theme.AmiriFontFamily
import com.arshadshah.nimaz.presentation.theme.QaidaMedallionState
import com.arshadshah.nimaz.presentation.theme.rememberQaidaPalette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.feature.content.R as FeatureR
import com.arshadshah.nimaz.domain.model.LessonStatus
import com.arshadshah.nimaz.presentation.components.atoms.*
import com.arshadshah.nimaz.presentation.components.molecules.NimazConfirmDialog
import com.arshadshah.nimaz.presentation.components.molecules.*
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.content.QaidaReaderEvent
import com.arshadshah.nimaz.presentation.viewmodel.content.QaidaReaderViewModel

/** Course, personal review queue and downloaded audio all use the same on-device history. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QaidaHomeScreen(
    onNavigateBack: () -> Unit,
    onOpenLesson: (Int) -> Unit,
    onOpenLetters: () -> Unit,
    viewModel: QaidaReaderViewModel = hiltViewModel(),
) {
    val course by viewModel.courseProgress.collectAsStateWithLifecycle()
    val due by viewModel.dueLessons.collectAsStateWithLifecycle()
    val today by viewModel.todayCount.collectAsStateWithLifecycle()
    val cacheBytes by viewModel.cacheBytes.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    var chapter by rememberSaveable { mutableIntStateOf(-1) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var clearAudio by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        viewModel.refreshReview(); viewModel.refreshCacheSize()
        onPauseOrDispose { }
    }
    LaunchedEffect(tab) { viewModel.refreshCacheSize(); viewModel.refreshReview(); listState.scrollToItem(0) }
    if (settingsOpen) {
        QaidaSettingsScreen(onNavigateBack = { settingsOpen = false }, viewModel = viewModel)
        return
    }
    NimazScreenScaffold(
        topBar = {
            NimazBackTopAppBar(title = stringResource(R.string.qaida), onBackClick = onNavigateBack,
                actions = {
                    NimazIconButton(Icons.Default.Translate, onOpenLetters,
                        contentDescription = stringResource(R.string.qaida_letter_explorer))
                    NimazIconButton(Icons.Default.Settings, { settingsOpen = true },
                        contentDescription = stringResource(FeatureR.string.qaida_settings))
                })
        },
        bottomBar = {
            NimazSecondaryNavigation(
                destinations = listOf(
                    NimazSecondaryDestination(stringResource(FeatureR.string.qaida_tab_journey), Icons.Default.Route),
                    NimazSecondaryDestination(stringResource(FeatureR.string.qaida_tab_review), Icons.Default.AutoStories),
                    NimazSecondaryDestination(stringResource(FeatureR.string.qaida_tab_downloads), Icons.Default.Headphones)),
                selectedIndex = tab, onSelect = { tab = it })
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), state = listState,
            contentPadding = PaddingValues(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)) {
            when (tab) {
                0 -> {
                    item {
                        val cp = course
                        QaidaCourseHeader(titleArabic = "رِحْلَتِي مَعَ القاعدة",
                            titleEnglish = stringResource(R.string.qaida_journey_title),
                            lessonIndex = ((cp?.completedLessons ?: 0) + 1).coerceAtMost(cp?.totalLessons?.coerceAtLeast(1) ?: 1),
                            totalLessons = cp?.totalLessons ?: 0, totalStars = cp?.totalStars ?: 0,
                            overallFraction = cp?.overallFraction ?: 0f,
                            continueLabel = cp?.lessons?.firstOrNull { it.lesson.id == cp.nextLessonId }?.lesson?.titleEnglish,
                            onContinue = { cp?.nextLessonId?.let(onOpenLesson) }, showContinue = false)
                    }
                    item {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(FeatureR.string.qaida_chapters_title), style = MaterialTheme.typography.titleLarge.copy(fontFamily = AmiriFontFamily))
                            NimazButton(stringResource(FeatureR.string.qaida_see_all), { chapter = if (chapter == 3) -1 else 3 },
                                variant = NimazButtonVariant.TEXT, size = NimazButtonSize.SMALL)
                        }
                    }
                    val groups = listOf(1..5 to FeatureR.string.qaida_chapter_letters,
                        6..12 to FeatureR.string.qaida_chapter_build, 13..17 to FeatureR.string.qaida_chapter_confidence)
                    groups.forEachIndexed { groupIndex, (range, title) ->
                        item {
                            NimazCard(Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                                colors = NimazCardDefaults.colors(
                                    container = when (groupIndex) { 0 -> MaterialTheme.colorScheme.primaryContainer; 1 -> MaterialTheme.colorScheme.secondaryContainer; else -> MaterialTheme.colorScheme.tertiaryContainer },
                                    content = when (groupIndex) { 0 -> MaterialTheme.colorScheme.onPrimaryContainer; 1 -> MaterialTheme.colorScheme.onSecondaryContainer; else -> MaterialTheme.colorScheme.onTertiaryContainer }),
                                onClick = { chapter = if (chapter == groupIndex) -1 else groupIndex }) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    NimazIcon(listOf(Icons.Default.MenuBook, Icons.Default.ViewInAr, Icons.Default.Mosque)[groupIndex],
                                        contentDescription = null, iconSize = 32.dp, tint = MaterialTheme.colorScheme.primary)
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(stringResource(title), style = MaterialTheme.typography.titleMedium.copy(fontFamily = AmiriFontFamily))
                                        Text(stringResource(listOf(FeatureR.string.qaida_chapter_letters_hint,
                                            FeatureR.string.qaida_chapter_build_hint, FeatureR.string.qaida_chapter_confidence_hint)[groupIndex]),
                                            style = MaterialTheme.typography.bodySmall)
                                    }
                                    NimazIcon(if (chapter == groupIndex || chapter == 3) Icons.Default.ExpandLess else Icons.Default.ChevronRight, contentDescription = null)
                                }
                            }
                        }
                        if (chapter == groupIndex || chapter == 3)
                        items(course?.lessons.orEmpty().filter { it.lesson.lessonNumber in range }, key = { it.lesson.id }) { state ->
                            val announcement = if (state.status == LessonStatus.COMPLETED)
                                pluralStringResource(R.plurals.qaida_a11y_lesson_complete_format, state.stars,
                                    state.lesson.lessonNumber, state.lesson.titleEnglish, state.stars)
                            else stringResource(if (state.status == LessonStatus.LOCKED)
                                R.string.qaida_a11y_lesson_locked_format else R.string.qaida_a11y_lesson_current_format,
                                state.lesson.lessonNumber, state.lesson.titleEnglish)
                            NimazCard(modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().semantics { contentDescription = announcement },
                                tone = if (state.lesson.id == course?.nextLessonId) NimazTone.ACCENT else NimazTone.NEUTRAL,
                                enabled = state.status != LessonStatus.LOCKED,
                                onClick = { onOpenLesson(state.lesson.id) }) {
                                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(stringResource(FeatureR.string.qaida_lesson_row, state.lesson.lessonNumber, state.lesson.titleEnglish),
                                        style = MaterialTheme.typography.titleMedium)
                                    Text(if (state.status == LessonStatus.LOCKED) stringResource(FeatureR.string.qaida_locked_hint)
                                        else stringResource(FeatureR.string.qaida_practice_count, state.completedCells, state.totalCells),
                                        style = MaterialTheme.typography.bodySmall)
                                    NimazProgressTrack(state.completionFraction, Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }
                    item {
                        Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(stringResource(FeatureR.string.qaida_path_title), style = MaterialTheme.typography.titleMedium.copy(fontFamily = AmiriFontFamily))
                            val lessons = course?.lessons.orEmpty()
                            val current = lessons.indexOfFirst { it.lesson.id == course?.nextLessonId }.coerceAtLeast(0)
                            val start = (current - 1).coerceAtLeast(0).coerceAtMost((lessons.size - 4).coerceAtLeast(0))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                lessons.drop(start).take(4).forEach { state ->
                                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        val pathLabel = stringResource(FeatureR.string.qaida_lesson_row, state.lesson.lessonNumber, state.lesson.titleEnglish)
                                        val lockedHint = stringResource(FeatureR.string.qaida_locked_hint)
                                        QaidaMedallion(label = state.lesson.lessonNumber.toString(),
                                            state = when (state.status) {
                                                LessonStatus.COMPLETED -> QaidaMedallionState.DONE
                                                LessonStatus.LOCKED -> QaidaMedallionState.LOCKED
                                                else -> QaidaMedallionState.CURRENT
                                            },
                                            contentDescription = if (state.status == LessonStatus.LOCKED) "$pathLabel. $lockedHint" else pathLabel,
                                            palette = rememberQaidaPalette(), size = 48.dp,
                                            onClick = { onOpenLesson(state.lesson.id) })
                                        Text(state.lesson.titleEnglish, style = MaterialTheme.typography.labelSmall,
                                            maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                            course?.let { cp -> cp.lessons.firstOrNull { it.lesson.id == cp.nextLessonId }?.let { state ->
                                NimazButton(stringResource(R.string.qaida_continue_format, state.lesson.titleEnglish),
                                    { onOpenLesson(state.lesson.id) }, fullWidth = true)
                            } }
                            Text(stringResource(FeatureR.string.qaida_today_count, today), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                }
                1 -> {
                    item {
                        Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Image(painterResource(if (due.isEmpty()) FeatureR.drawable.qaida_empty_art else FeatureR.drawable.qaida_review_art), null, Modifier.fillMaxWidth().height(180.dp))
                            Text(stringResource(FeatureR.string.qaida_review_title), style = MaterialTheme.typography.headlineSmall)
                            Text(stringResource(FeatureR.string.qaida_review_description))
                            if (due.isEmpty()) NimazBanner(maxTitleLines = Int.MAX_VALUE, 
                                title = stringResource(FeatureR.string.qaida_review_empty),
                                variant = NimazBannerVariant.INFO, icon = Icons.Default.CheckCircle)
                            course?.nextLessonId?.let { nextId ->
                                if (due.isEmpty()) NimazButton(stringResource(FeatureR.string.qaida_continue_practice),
                                    { onOpenLesson(nextId) }, fullWidth = true)
                            }
                        }
                    }
                    items(course?.lessons.orEmpty().filter { it.lesson.id in due }, key = { it.lesson.id }) { state ->
                        NimazCard(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), onClick = { onOpenLesson(state.lesson.id) }) {
                            Text(state.lesson.titleEnglish, Modifier.padding(24.dp), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                else -> item {
                    NimazCard(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
                        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(stringResource(FeatureR.string.qaida_downloads_title), style = MaterialTheme.typography.headlineSmall)
                            Text(stringResource(FeatureR.string.qaida_downloads_description))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                NimazIcon(Icons.Default.Headphones, contentDescription = null,
                                    iconSize = 32.dp, tint = MaterialTheme.colorScheme.primary)
                                Text(stringResource(FeatureR.string.qaida_cache_size, "%.1f".format(cacheBytes / 1048576.0)),
                                    modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            }
                            if (cacheBytes == 0L) NimazBanner(maxTitleLines = Int.MAX_VALUE, 
                                title = stringResource(FeatureR.string.qaida_downloads_empty), variant = NimazBannerVariant.INFO)
                            NimazButton(stringResource(FeatureR.string.qaida_clear_audio), { clearAudio = true },
                                enabled = cacheBytes > 0, variant = NimazButtonVariant.OUTLINED, fullWidth = true)
                        }
                    }
                }
            }
        }
    }
    if (clearAudio) NimazConfirmDialog(
        title = stringResource(FeatureR.string.qaida_clear_audio),
        message = stringResource(FeatureR.string.qaida_clear_audio_message),
        confirmText = stringResource(FeatureR.string.qaida_clear_audio),
        cancelText = stringResource(R.string.cancel), titleIcon = Icons.Default.RestartAlt, isDestructive = true,
        onConfirm = {
            viewModel.onEvent(QaidaReaderEvent.ClearAudio)
            clearAudio = false
        }, onDismiss = { clearAudio = false })
}
