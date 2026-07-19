package com.arshadshah.nimaz.presentation.screens.qaida

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.QaidaLetter
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.molecules.NimazBottomSheet
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.components.organisms.QaidaLetterBoard
import com.arshadshah.nimaz.presentation.components.organisms.QaidaLetterDetailSheet
import com.arshadshah.nimaz.presentation.viewmodel.QaidaReaderEvent
import com.arshadshah.nimaz.presentation.viewmodel.QaidaReaderViewModel

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
    val letters by viewModel.letters.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<QaidaLetter?>(null) }

    NimazScreenScaffold(
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.qaida_arabic_letters),
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
                scrollable = false,
                contentPadding = PaddingValues(0.dp)
            ) {
                QaidaLetterDetailSheet(
                    letter = letter,
                    onPlay = { viewModel.onEvent(QaidaReaderEvent.PlayLetter(it)) },
                )
            }
        }
    }
}
