package com.arshadshah.nimaz.presentation.screens.qaida

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.text.style.TextAlign
import com.arshadshah.nimaz.presentation.theme.AmiriFontFamily
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.arshadshah.nimaz.domain.model.LessonStatus
import com.arshadshah.nimaz.feature.content.R
import com.arshadshah.nimaz.core.ui.R as CoreR
import com.arshadshah.nimaz.presentation.components.atoms.*
import com.arshadshah.nimaz.presentation.components.molecules.NimazBanner
import com.arshadshah.nimaz.presentation.components.molecules.NimazBannerVariant
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.content.QaidaReaderEvent
import com.arshadshah.nimaz.presentation.viewmodel.content.QaidaReaderViewModel

/** A lesson introduction, focused learning, self-check, overview and a gentle completion page. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QaidaReaderScreen(
    lessonId: Int,
    onNavigateBack: () -> Unit,
    viewModel: QaidaReaderViewModel = hiltViewModel(),
) {
    LaunchedEffect(lessonId) { viewModel.onEvent(QaidaReaderEvent.SelectLesson(lessonId)) }
    DisposableEffect(Unit) { onDispose { viewModel.onEvent(QaidaReaderEvent.StopAudio) } }
    val selectedId by viewModel.selectedLessonId.collectAsStateWithLifecycle()
    val course by viewModel.courseProgress.collectAsStateWithLifecycle()
    val activeId = selectedId ?: lessonId
    LifecycleResumeEffect(Unit) {
        onPauseOrDispose { viewModel.onEvent(QaidaReaderEvent.StopAudio) }
    }
    val content by viewModel.lessonContent.collectAsStateWithLifecycle()
    val download by viewModel.download.collectAsStateWithLifecycle()
    val audio by viewModel.audioState.collectAsStateWithLifecycle()
    val sessionHeard by viewModel.sessionHeard.collectAsStateWithLifecycle()
    val completed by viewModel.completedCellIds.collectAsStateWithLifecycle()
    val c = content?.takeIf { it.lesson.id == activeId }
    val all = c?.lines?.flatMap { it.cells }.orEmpty()
    var page by rememberSaveable(activeId) { mutableIntStateOf(0) } // intro, learn, completion
    var mode by rememberSaveable(activeId) { mutableIntStateOf(0) } // listen, self-check, overview
    var index by rememberSaveable(activeId) { mutableIntStateOf(-1) }
    var reviewIds by rememberSaveable(activeId) { mutableStateOf(listOf<Int>()) }
    var practised by rememberSaveable(activeId) { mutableStateOf(listOf<Int>()) }
    var reveal by rememberSaveable(activeId, index) { mutableStateOf(false) }
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val showHint = settings.showTransliteration
    val slow = settings.slowPlayback
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    val cells = if (reviewIds.isEmpty()) all else all.filter { it.id in reviewIds }
    val cell = cells.getOrNull(index.coerceAtLeast(0))
    val listState = rememberLazyListState()
    LaunchedEffect(page, mode, index, activeId) { listState.scrollToItem(0) }
    LaunchedEffect(all) {
        if (index < 0 && all.isNotEmpty()) index = all.indexOfFirst { it.id == viewModel.resumeCell(activeId) }.coerceAtLeast(0)
    }
    LaunchedEffect(cell?.id) { cell?.let(viewModel::savePosition); reveal = false; viewModel.onEvent(QaidaReaderEvent.StopAudio) }
    fun finish() { viewModel.onEvent(QaidaReaderEvent.StopAudio); page = 2 }
    fun next() { if (index < cells.lastIndex) index++ else finish() }
    fun back() { if (page == 0) onNavigateBack() else { viewModel.onEvent(QaidaReaderEvent.StopAudio); page = 0 } }
    if (settingsOpen) {
        QaidaSettingsScreen(onNavigateBack = { settingsOpen = false }, viewModel = viewModel, onJourneyReset = onNavigateBack)
        return
    }
    BackHandler(page != 0) { back() }
    NimazScreenScaffold(
        topBar = {
            NimazBackTopAppBar(title = stringResource(CoreR.string.qaida),
                subtitle = c?.let { stringResource(CoreR.string.qaida_lesson_progress, it.lesson.lessonNumber, course?.totalLessons ?: 0) },
                onBackClick = { if (page == 2) onNavigateBack() else back() },
                actions = {
                    NimazIconButton(Icons.Default.Settings, {
                        viewModel.onEvent(QaidaReaderEvent.StopAudio); settingsOpen = true
                    }, contentDescription = stringResource(R.string.qaida_settings))
                })
        }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), state = listState, contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (c == null) item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Image(painterResource(R.drawable.qaida_journey_book), null, Modifier.fillMaxWidth().height(170.dp))
                    NimazBanner(title = stringResource(R.string.qaida_getting_ready),
                        variant = NimazBannerVariant.INFO, isLoading = true)
                }
            }
            else when (page) {
                0 -> item {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Image(painterResource(R.drawable.qaida_journey_book), null, Modifier.fillMaxWidth().height(190.dp))
                        Text(stringResource(R.string.qaida_lesson_intro), style = MaterialTheme.typography.headlineMedium.copy(fontFamily = AmiriFontFamily))
                        ArabicText(c.lesson.titleArabic, modifier = Modifier.fillMaxWidth(), size = ArabicTextSize.LARGE)
                        Text(c.lesson.description, style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(R.string.qaida_teacher_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (all.isEmpty()) NimazBanner(title = stringResource(R.string.qaida_empty_lesson),
                            variant = NimazBannerVariant.INFO)
                        else {
                            NimazButton(stringResource(if (completed.isEmpty()) R.string.qaida_begin_lesson else R.string.qaida_continue_practice),
                                { reviewIds = emptyList(); page = 1 }, fullWidth = true)
                            if (viewModel.dueCells(activeId).isNotEmpty()) NimazButton(stringResource(R.string.qaida_review_lesson), {
                                reviewIds = viewModel.dueCells(activeId).toList(); index = 0; mode = 1; page = 1
                            }, variant = NimazButtonVariant.OUTLINED, fullWidth = true)
                        }
                    }
                }
                1 -> {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(c.lesson.titleEnglish, style = MaterialTheme.typography.headlineLarge.copy(fontFamily = AmiriFontFamily))
                            Text(c.lesson.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(stringResource(R.string.qaida_cell_position, (index + 1).coerceAtLeast(1), cells.size),
                                style = MaterialTheme.typography.labelLarge)
                            NimazProgressTrack(if (cells.isEmpty()) 0f else (index + 1).toFloat() / cells.size, Modifier.fillMaxWidth())
                            SecondaryTabRow(selectedTabIndex = when (mode) { 3 -> 1; 1 -> 2; else -> 0 },
                                containerColor = MaterialTheme.colorScheme.background) {
                                listOf(R.string.qaida_listen, R.string.qaida_repeat_tab, R.string.qaida_practise).forEachIndexed { tab, label ->
                                    Tab(selected = mode == listOf(0, 3, 1)[tab], onClick = {
                                        mode = listOf(0, 3, 1)[tab]; viewModel.onEvent(QaidaReaderEvent.StopAudio)
                                    }, text = { Text(stringResource(label)) }, icon = {
                                        NimazIcon(listOf(Icons.Default.Headphones, Icons.Default.Repeat, Icons.Default.Edit)[tab], contentDescription = null)
                                    })
                                }
                            }
                        }
                    }
                    if (mode == 2) items(cells, key = { it.id }) { item ->
                        NimazCard(Modifier.fillMaxWidth(), onClick = { index = cells.indexOf(item); mode = 0 }) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                HarakatArabicText(item.textArabic, item.highlightGroup)
                                if (showHint) Text(item.transliteration)
                                if (item.id in completed) Text(stringResource(R.string.qaida_continue_practice), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    } else if (cell != null) {
                        item {
                            NimazCard(Modifier.fillMaxWidth()) {
                                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    HarakatArabicText(cell.textArabic, cell.highlightGroup, size = ArabicTextSize.DISPLAY,
                                        modifier = Modifier.fillMaxWidth())
                                    if ((mode != 1 && showHint) || (mode == 1 && reveal)) {
                                        Text(cell.transliteration, style = MaterialTheme.typography.headlineMedium.copy(fontFamily = AmiriFontFamily), color = MaterialTheme.colorScheme.primary)
                                        c.lines.firstOrNull { line -> line.cells.any { it.id == cell.id } }?.line?.instructionEnglish
                                            ?.takeIf { it.isNotBlank() }?.let {
                                                Text(it, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        cell.notes?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                                    }
                                    if (mode == 1 && !reveal) {
                                        Text(stringResource(R.string.qaida_read_prompt))
                                        NimazButton(stringResource(R.string.qaida_reveal), { reveal = true }, fullWidth = true)
                                    } else {
                                        NimazIconButton(if (audio.isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow, {
                                            viewModel.onEvent(if (audio.isPlaying) QaidaReaderEvent.StopAudio
                                                else if (mode == 3) QaidaReaderEvent.RepeatCell(cell, 3) else QaidaReaderEvent.CellTapped(cell))
                                        }, modifier = Modifier.size(80.dp), style = NimazIconButtonStyle.FILLED,
                                            size = NimazIconButtonSize.EXTRA_LARGE, enabled = download.ready,
                                            contentDescription = stringResource(if (audio.isPlaying) R.string.qaida_stop_sound else R.string.qaida_play_sound))
                                        if (download.ready) {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                NimazButton(stringResource(if (slow) R.string.qaida_normal_sound else R.string.qaida_slow_sound), { viewModel.onEvent(QaidaReaderEvent.SetSlow(!slow)) },
                                                    modifier = Modifier.weight(1f), variant = NimazButtonVariant.QUIET, size = NimazButtonSize.SMALL,
                                                    leadingIcon = Icons.Default.Speed)
                                                NimazButton(stringResource(R.string.qaida_repeat_sound), { viewModel.onEvent(QaidaReaderEvent.RepeatCell(cell, 3)) },
                                                    modifier = Modifier.weight(1f), variant = NimazButtonVariant.QUIET, size = NimazButtonSize.SMALL,
                                                    leadingIcon = Icons.Default.Repeat)
                                            }
                                            if (slow) Text(stringResource(R.string.qaida_slow_hint), style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                        if (mode != 1) item {
                            val start = (index - 1).coerceAtLeast(0).coerceAtMost((cells.size - 3).coerceAtLeast(0))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                cells.drop(start).take(3).forEach { related ->
                                    NimazCard(Modifier.weight(1f), style = NimazCardStyle.OUTLINED,
                                        selected = related.id == cell.id, tone = if (related.id == cell.id) NimazTone.ACCENT else NimazTone.NEUTRAL,
                                        onClick = { index = cells.indexOf(related) }) {
                                        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            HarakatArabicText(related.textArabic, related.highlightGroup, size = ArabicTextSize.MEDIUM)
                                            if (showHint) Text(related.transliteration, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (mode != 1) NimazButton(stringResource(R.string.qaida_ready_practise), { mode = 1 }, fullWidth = true)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    NimazButton(stringResource(R.string.qaida_all_cells), { mode = 2 }, variant = NimazButtonVariant.TEXT)
                                    if (mode != 1) NimazButton(stringResource(if (showHint) R.string.qaida_hide_hint else R.string.qaida_show_hint),
                                    { viewModel.onEvent(QaidaReaderEvent.SetTransliteration(!showHint)) }, variant = NimazButtonVariant.TEXT)
                                }
                                if (mode == 1 && reveal) {
                                    Text(stringResource(R.string.qaida_self_check), style = MaterialTheme.typography.bodySmall)
                                    NimazButton(stringResource(R.string.qaida_confident), {
                                        viewModel.onEvent(QaidaReaderEvent.PractisedCell(cell, true)); practised = (practised + cell.id).distinct(); next()
                                    }, fullWidth = true)
                                    NimazButton(stringResource(R.string.qaida_again), {
                                        viewModel.onEvent(QaidaReaderEvent.PractisedCell(cell, false)); practised = (practised + cell.id).distinct(); next()
                                    }, variant = NimazButtonVariant.OUTLINED, fullWidth = true)
                                }
                                NimazButton(stringResource(R.string.qaida_previous_sound), { index-- }, enabled = index > 0,
                                    variant = NimazButtonVariant.TEXT, fullWidth = true)
                                NimazButton(stringResource(if (index == cells.lastIndex) R.string.qaida_finish_session else R.string.qaida_next_sound), { next() },
                                    variant = NimazButtonVariant.TEXT, fullWidth = true)
                            }
                        }
                    }
                }
                else -> item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Image(painterResource(R.drawable.qaida_reward_book), null, Modifier.fillMaxWidth().height(200.dp))
                        Text(stringResource(R.string.qaida_reward_title), style = MaterialTheme.typography.headlineMedium.copy(fontFamily = AmiriFontFamily))
                        Text(stringResource(R.string.qaida_session_count, (practised + sessionHeard).distinct().size), style = MaterialTheme.typography.titleMedium)
                        NimazCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                                val checks = listOf(
                                    (sessionHeard.isNotEmpty()) to if (sessionHeard.isNotEmpty()) R.string.qaida_listened_done else R.string.qaida_listened_pending,
                                    (practised.isNotEmpty()) to if (practised.isNotEmpty()) R.string.qaida_practised_done else R.string.qaida_practised_pending,
                                    false to R.string.qaida_review_ready)
                                checks.forEachIndexed { i, (done, label) ->
                                    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        NimazIcon(if (done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = null, tint = MaterialTheme.colorScheme.primary, iconSize = 28.dp)
                                        Text(stringResource(label), style = MaterialTheme.typography.bodyMedium)
                                    }
                                    if (i < checks.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                        NimazCard(Modifier.fillMaxWidth(), tone = NimazTone.ACCENT) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                NimazIcon(Icons.Default.Spa, contentDescription = null, iconSize = 36.dp)
                                Column {
                                    Text(stringResource(R.string.qaida_keep_growing), style = MaterialTheme.typography.titleLarge.copy(fontFamily = AmiriFontFamily))
                                    Text(stringResource(R.string.qaida_keep_growing_hint), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        NimazButton(stringResource(R.string.qaida_quick_review), {
                            reviewIds = (practised + sessionHeard).distinct(); index = 0; mode = 1; page = 1
                        }, fullWidth = true)
                        NimazButton(stringResource(R.string.qaida_back_journey), onNavigateBack, variant = NimazButtonVariant.QUIET, fullWidth = true)
                        Text(stringResource(R.string.qaida_footer), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        val lessons = course?.lessons.orEmpty()
                        val current = lessons.indexOfFirst { it.lesson.id == activeId }
                        val following = lessons.getOrNull(current + 1)?.takeIf { current >= 0 && it.status != LessonStatus.LOCKED }
                        if (following != null) NimazButton(stringResource(CoreR.string.qaida_next_lesson),
                            { viewModel.onEvent(QaidaReaderEvent.NextLesson) }, fullWidth = true)
                    }
                }
            }
            if (c != null && page != 2) item {
                NimazCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Image(painterResource(R.drawable.qaida_audio_art), null,
                            Modifier.fillMaxWidth().height(100.dp))
                        NimazBanner(title = stringResource(when {
                            download.loading -> R.string.qaida_download_progress
                            download.ready -> R.string.qaida_audio_ready
                            download.failed -> R.string.qaida_audio_failed
                            else -> R.string.qaida_audio_unavailable
                        }, download.completed, download.total),
                            variant = if (download.failed) NimazBannerVariant.ERROR else NimazBannerVariant.INFO,
                            isLoading = download.loading)
                        if (download.loading) NimazProgressTrack(if (download.total == 0) 0f else download.completed.toFloat() / download.total, Modifier.fillMaxWidth())
                        if (audio.error != null) NimazBanner(title = stringResource(R.string.qaida_playback_failed),
                            variant = NimazBannerVariant.ERROR)
                        if (!download.loading && !download.ready) NimazButton(stringResource(R.string.qaida_retry_audio),
                            { viewModel.onEvent(QaidaReaderEvent.RetryAudio) }, variant = NimazButtonVariant.TEXT, fullWidth = true)
                    }
                }
            }
        }
    }
}
