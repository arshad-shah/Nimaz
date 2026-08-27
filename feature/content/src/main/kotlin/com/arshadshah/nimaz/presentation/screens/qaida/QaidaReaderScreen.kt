package com.arshadshah.nimaz.presentation.screens.qaida

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.LessonStatus
import com.arshadshah.nimaz.domain.model.QaidaCourseProgress
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.organisms.QaidaCelebrationOverlay
import com.arshadshah.nimaz.presentation.components.organisms.QaidaLessonLines
import com.arshadshah.nimaz.presentation.viewmodel.content.QaidaReaderEvent
import com.arshadshah.nimaz.presentation.viewmodel.content.QaidaReaderViewModel

/**
 * The lesson reader: tap-to-hear cell tiles laid out by line, a transliteration
 * toggle, and the festive celebration overlay when the lesson completes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QaidaReaderScreen(
    lessonId: Int,
    onNavigateBack: () -> Unit,
    viewModel: QaidaReaderViewModel = hiltViewModel(),
) {
    LaunchedEffect(lessonId) { viewModel.onEvent(QaidaReaderEvent.SelectLesson(lessonId)) }

    val content by viewModel.lessonContent.collectAsStateWithLifecycle()
    val playing by viewModel.playingCell.collectAsStateWithLifecycle()
    val lessonProgress by viewModel.lessonProgress.collectAsStateWithLifecycle()
    val course by viewModel.courseProgress.collectAsStateWithLifecycle()
    val completedCellIds by viewModel.completedCellIds.collectAsStateWithLifecycle()
    val openLessonId by viewModel.selectedLessonId.collectAsStateWithLifecycle()

    var showTransliteration by rememberSaveable { mutableStateOf(true) }
    var showCelebration by remember { mutableStateOf(false) }

    // Only celebrate on a *fresh* completion during this visit — not when the
    // child re-opens an already-finished lesson (which should just show the
    // lesson so they can practise it again). [openedComplete] captures whether
    // the lesson was already done on entry; it resets when the lesson changes.
    val status = lessonProgress?.status
    var openedComplete by remember(lessonId) { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(status, lessonId) {
        if (openedComplete == null && status != null) {
            openedComplete = status == LessonStatus.COMPLETED
        }
        if (openedComplete == false && status == LessonStatus.COMPLETED) {
            showCelebration = true
        }
    }

    val unlockedTitle = course?.let { c ->
        c.nextLessonId?.let { id -> c.lessons.firstOrNull { it.lesson.id == id }?.lesson?.titleEnglish }
    }

    NimazScreenScaffold(
        // Opts out of the app ornament: the reader owns a plain, high-contrast backdrop.
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        content?.lesson?.titleEnglish ?: stringResource(R.string.qaida_lesson)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        NimazIcon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showTransliteration = !showTransliteration }) {
                        NimazIcon(
                            imageVector = if (showTransliteration) Icons.Filled.Visibility
                            else Icons.Filled.VisibilityOff,
                            contentDescription = stringResource(R.string.qaida_toggle_transliteration),
                        )
                    }
                },
            )
        },
        bottomBar = {
            QaidaLessonNav(
                course = course,
                openLessonId = openLessonId,
                onPrevious = { viewModel.onEvent(QaidaReaderEvent.PreviousLesson) },
                onResume = { viewModel.onEvent(QaidaReaderEvent.Resume) },
                onNext = { viewModel.onEvent(QaidaReaderEvent.NextLesson) },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            content?.let { c ->
                QaidaLessonLines(
                    content = c,
                    playingCellId = playing?.id,
                    showTransliteration = showTransliteration,
                    onCellTap = { viewModel.onEvent(QaidaReaderEvent.CellTapped(it)) },
                    onPlayLine = { viewModel.onEvent(QaidaReaderEvent.PlayLine(it)) },
                    completedCellIds = completedCellIds,
                )
            }

            QaidaCelebrationOverlay(
                visible = showCelebration,
                stars = lessonProgress?.stars ?: 0,
                lessonTitle = content?.lesson?.titleEnglish ?: "",
                unlockedTitle = unlockedTitle,
                onNext = {
                    showCelebration = false
                    viewModel.onEvent(QaidaReaderEvent.NextLesson)
                },
                onMap = onNavigateBack,
            )
        }
    }
}

/**
 * Walk the course from inside a lesson: back one, forward one, or straight to the lesson the
 * learner is actually up to.
 *
 * Forward used to exist only on the celebration overlay, so a child who opened lesson 3 to
 * practise had to return to the map to reach lesson 4 — and there was no way back to lesson 2
 * at all. "Continue" appears only once the open lesson is *not* the one the course points at,
 * which is exactly when browsing backwards has left somewhere to return to; the next lesson is
 * gated on [LessonStatus.LOCKED] here as well as in the ViewModel, so a locked lesson reads as
 * unavailable rather than as a button that does nothing.
 */
@Composable
private fun QaidaLessonNav(
    course: QaidaCourseProgress?,
    openLessonId: Int?,
    onPrevious: () -> Unit,
    onResume: () -> Unit,
    onNext: () -> Unit,
) {
    val lessons = course?.lessons.orEmpty()
    val index = lessons.indexOfFirst { it.lesson.id == openLessonId }
    if (index < 0) return

    val hasPrevious = index > 0
    val hasNext = index < lessons.lastIndex &&
            lessons[index + 1].status != LessonStatus.LOCKED
    val resumeId = course?.nextLessonId ?: lessons.firstOrNull()?.lesson?.id
    val canResume = resumeId != null && resumeId != openLessonId

    Surface(
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NimazButton(
                text = stringResource(R.string.qaida_previous_lesson),
                onClick = onPrevious,
                variant = NimazButtonVariant.OUTLINED,
                size = NimazButtonSize.MEDIUM,
                leadingIcon = Icons.AutoMirrored.Filled.ArrowBack,
                enabled = hasPrevious,
            )
            if (canResume) {
                NimazButton(
                    text = stringResource(R.string.qaida_resume_lesson),
                    onClick = onResume,
                    variant = NimazButtonVariant.TEXT,
                    size = NimazButtonSize.MEDIUM,
                    leadingIcon = Icons.Filled.PlayArrow,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Box(modifier = Modifier.weight(1f))
            }
            NimazButton(
                text = stringResource(R.string.qaida_next_lesson),
                onClick = onNext,
                variant = NimazButtonVariant.FILLED,
                size = NimazButtonSize.MEDIUM,
                enabled = hasNext,
            )
        }
    }
}
