package com.arshadshah.nimaz.presentation.screens.qaida

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.molecules.NimazConfirmDialog
import com.arshadshah.nimaz.presentation.components.molecules.NimazDropdownMenu
import com.arshadshah.nimaz.presentation.components.molecules.NimazDropdownRow
import com.arshadshah.nimaz.presentation.components.molecules.QaidaCourseHeader
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.components.organisms.QaidaCoursePath
import com.arshadshah.nimaz.presentation.viewmodel.content.QaidaReaderEvent
import com.arshadshah.nimaz.presentation.viewmodel.content.QaidaReaderViewModel

/**
 * The Qaida course map: a pinned header (title, progress, continue) above the
 * scrolling winding trail of lessons. Reachable from the More menu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QaidaHomeScreen(
    onNavigateBack: () -> Unit,
    onOpenLesson: (Int) -> Unit,
    onOpenLetters: () -> Unit,
    viewModel: QaidaReaderViewModel = hiltViewModel(),
) {
    val progress by viewModel.courseProgress.collectAsStateWithLifecycle()
    val cp = progress

    var menuExpanded by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    NimazScreenScaffold(
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.qaida),
                onBackClick = onNavigateBack,
                actions = {
                    IconButton(onClick = onOpenLetters) {
                        NimazIcon(
                            Icons.Filled.Translate,
                            contentDescription = stringResource(R.string.qaida_letter_explorer)
                        )
                    }
                    IconButton(onClick = { menuExpanded = true }) {
                        NimazIcon(
                            Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.more)
                        )
                    }
                    NimazDropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        NimazDropdownRow(
                            text = stringResource(R.string.qaida_reset_journey),
                            leadingIcon = Icons.Filled.RestartAlt,
                            destructive = true,
                            onClick = {
                                menuExpanded = false
                                showResetDialog = true
                            },
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            QaidaCourseHeader(
                titleArabic = "رِحْلَتِي مَعَ القاعدة",
                titleEnglish = stringResource(R.string.qaida_journey_title),
                lessonIndex = ((cp?.completedLessons ?: 0) + 1).coerceAtMost(cp?.totalLessons ?: 1),
                totalLessons = cp?.totalLessons ?: 0,
                totalStars = cp?.totalStars ?: 0,
                overallFraction = cp?.overallFraction ?: 0f,
                continueLabel = cp?.nextLessonId?.let { id ->
                    cp.lessons.firstOrNull { it.lesson.id == id }?.lesson?.titleEnglish
                },
                onContinue = { cp?.nextLessonId?.let(onOpenLesson) },
            )
            Box(modifier = Modifier.weight(1f)) {
                QaidaCoursePath(
                    lessons = cp?.lessons.orEmpty(),
                    currentLessonId = cp?.nextLessonId,
                    onLessonClick = onOpenLesson,
                )
            }
        }
    }

    if (showResetDialog) {
        NimazConfirmDialog(
            title = stringResource(R.string.qaida_reset_title),
            message = stringResource(R.string.qaida_reset_message),
            confirmText = stringResource(R.string.reset),
            cancelText = stringResource(R.string.cancel),
            titleIcon = Icons.Filled.RestartAlt,
            isDestructive = true,
            onConfirm = { viewModel.onEvent(QaidaReaderEvent.ResetJourney) },
            onDismiss = { showResetDialog = false },
        )
    }
}
