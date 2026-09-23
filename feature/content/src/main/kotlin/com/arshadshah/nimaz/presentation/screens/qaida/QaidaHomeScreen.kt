package com.arshadshah.nimaz.presentation.screens.qaida

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.feature.content.R as FeatureR
import com.arshadshah.nimaz.domain.model.LessonStatus
import com.arshadshah.nimaz.presentation.components.atoms.*
import com.arshadshah.nimaz.presentation.components.molecules.NimazConfirmDialog
import com.arshadshah.nimaz.presentation.components.molecules.QaidaCourseHeader
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
    val cacheBytes by viewModel.cacheBytes.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var reset by remember { mutableStateOf(false) }
    var clearAudio by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        viewModel.refreshReview(); viewModel.refreshCacheSize()
        onPauseOrDispose { }
    }
    LaunchedEffect(tab) { viewModel.refreshCacheSize(); viewModel.refreshReview() }
    NimazScreenScaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NimazBackTopAppBar(title = stringResource(R.string.qaida), onBackClick = onNavigateBack,
                actions = {
                    IconButton(onClick = onOpenLetters) {
                        NimazIcon(Icons.Default.Translate, stringResource(R.string.qaida_letter_explorer))
                    }
                    IconButton(onClick = { reset = true }) {
                        NimazIcon(Icons.Default.RestartAlt, stringResource(R.string.qaida_reset_journey))
                    }
                })
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                NimazSegmentedControl(
                    options = listOf(FeatureR.string.qaida_tab_journey, FeatureR.string.qaida_tab_review,
                        FeatureR.string.qaida_tab_downloads).map { NimazSegmentedOption(stringResource(it)) },
                    selectedIndex = tab, onSelect = { tab = it }, purpose = NimazSegmentedPurpose.VIEW,
                    modifier = Modifier.padding(horizontal = 20.dp))
            }
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
                            onContinue = { cp?.nextLessonId?.let(onOpenLesson) })
                    }
                    val groups = listOf(1..5 to FeatureR.string.qaida_chapter_letters,
                        6..12 to FeatureR.string.qaida_chapter_build, 13..17 to FeatureR.string.qaida_chapter_confidence)
                    groups.forEach { (range, title) ->
                        item {
                            Column(Modifier.padding(horizontal = 24.dp)) {
                                Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
                                Text(stringResource(FeatureR.string.qaida_chapter_range, range.first, range.last),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        items(course?.lessons.orEmpty().filter { it.lesson.lessonNumber in range }, key = { it.lesson.id }) { state ->
                            val announcement = stringResource(if (state.status == LessonStatus.LOCKED)
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
                }
                1 -> {
                    item {
                        Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Image(painterResource(FeatureR.drawable.qaida_reward_book), null, Modifier.fillMaxWidth().height(180.dp))
                            Text(stringResource(FeatureR.string.qaida_review_title), style = MaterialTheme.typography.headlineSmall)
                            Text(stringResource(FeatureR.string.qaida_review_description))
                            if (due.isEmpty()) Text(stringResource(FeatureR.string.qaida_review_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            Text(stringResource(FeatureR.string.qaida_cache_size, "%.1f".format(cacheBytes / 1048576.0)))
                            NimazButton(stringResource(FeatureR.string.qaida_clear_audio), { clearAudio = true },
                                enabled = cacheBytes > 0, variant = NimazButtonVariant.OUTLINED, fullWidth = true)
                        }
                    }
                }
            }
        }
    }
    if (reset || clearAudio) NimazConfirmDialog(
        title = stringResource(if (reset) R.string.qaida_reset_title else FeatureR.string.qaida_clear_audio),
        message = stringResource(if (reset) R.string.qaida_reset_message else FeatureR.string.qaida_clear_audio_message),
        confirmText = stringResource(if (reset) R.string.reset else FeatureR.string.qaida_clear_audio),
        cancelText = stringResource(R.string.cancel), titleIcon = Icons.Default.RestartAlt, isDestructive = true,
        onConfirm = {
            viewModel.onEvent(if (reset) QaidaReaderEvent.ResetJourney else QaidaReaderEvent.ClearAudio)
            reset = false; clearAudio = false
        }, onDismiss = { reset = false; clearAudio = false })
}
