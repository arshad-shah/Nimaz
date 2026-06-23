package com.arshadshah.nimaz.presentation.screens.qaida

import androidx.compose.ui.res.stringResource
import com.arshadshah.nimaz.R
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.domain.model.LessonStatus
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.organisms.QaidaCelebrationOverlay
import com.arshadshah.nimaz.presentation.components.organisms.QaidaLessonLines
import com.arshadshah.nimaz.presentation.viewmodel.QaidaReaderEvent
import com.arshadshah.nimaz.presentation.viewmodel.QaidaReaderViewModel

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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(content?.lesson?.titleEnglish ?: "Lesson") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        NimazIcon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
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
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
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
