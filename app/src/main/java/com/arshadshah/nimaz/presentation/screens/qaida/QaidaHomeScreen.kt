package com.arshadshah.nimaz.presentation.screens.qaida

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.arshadshah.nimaz.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.presentation.components.molecules.NimazConfirmDialog
import com.arshadshah.nimaz.presentation.components.molecules.QaidaCourseHeader
import com.arshadshah.nimaz.presentation.components.organisms.QaidaCoursePath
import com.arshadshah.nimaz.presentation.viewmodel.QaidaReaderViewModel

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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Qaida") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenLetters) {
                        Icon(Icons.Filled.Translate, contentDescription = "Letter explorer")
                    }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more))
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.qaida_reset_journey)) },
                            leadingIcon = {
                                Icon(Icons.Filled.RestartAlt, contentDescription = null)
                            },
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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            QaidaCourseHeader(
                titleArabic = "رِحْلَتِي مَعَ القاعدة",
                titleEnglish = "My Qaida Journey",
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
            onConfirm = { viewModel.resetJourney() },
            onDismiss = { showResetDialog = false },
        )
    }
}
