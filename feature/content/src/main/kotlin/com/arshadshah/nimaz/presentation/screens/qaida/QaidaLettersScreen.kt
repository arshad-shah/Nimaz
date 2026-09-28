package com.arshadshah.nimaz.presentation.screens.qaida

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButton
import com.arshadshah.nimaz.feature.content.R as FeatureR
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.QaidaLetter
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.molecules.NimazBottomSheet
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.components.organisms.QaidaLetterBoard
import com.arshadshah.nimaz.presentation.components.organisms.QaidaLetterDetailSheet
import com.arshadshah.nimaz.presentation.viewmodel.content.QaidaReaderEvent
import com.arshadshah.nimaz.presentation.viewmodel.content.QaidaReaderViewModel

/**
 * The letter explorer: an alphabet board; tapping a letter opens its detail in
 * a bottom sheet (forms, name, phonetic hint, makhraj, play).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QaidaLettersScreen(
    onNavigateBack: () -> Unit,
    viewModel: QaidaReaderViewModel = hiltViewModel(),
) {
    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.onEvent(QaidaReaderEvent.SelectLesson(1)) }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { viewModel.onEvent(QaidaReaderEvent.StopAudio) } }
    val download by viewModel.download.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        onPauseOrDispose { viewModel.onEvent(QaidaReaderEvent.StopAudio) }
    }
    val letters by viewModel.letters.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<QaidaLetter?>(null) }

    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    if (settingsOpen) {
        QaidaSettingsScreen(onNavigateBack = { settingsOpen = false }, viewModel = viewModel)
        return
    }
    NimazScreenScaffold(
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.qaida),
                subtitle = stringResource(R.string.qaida_arabic_letters),
                actions = { NimazIconButton(Icons.Default.Settings, {
                    viewModel.onEvent(QaidaReaderEvent.StopAudio); settingsOpen = true
                }, contentDescription = stringResource(FeatureR.string.qaida_settings)) },
                onBackClick = onNavigateBack,
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            QaidaLetterBoard(
                letters = letters,
                heardLetterIds = emptySet(),
                onLetterClick = { selected = it },
            )
        }

        selected?.let { letter ->
            NimazBottomSheet(
                onDismissRequest = { selected = null },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                scrollable = true,
                contentPadding = PaddingValues(0.dp)
            ) {
                QaidaLetterDetailSheet(
                    letter = letter,
                    audioAvailable = download.ready,
                    onPlay = { viewModel.onEvent(QaidaReaderEvent.PlayLetter(it)) },
                )
            }
        }
    }
}
