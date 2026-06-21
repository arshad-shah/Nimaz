package com.arshadshah.nimaz.presentation.screens.qaida

import androidx.compose.ui.res.stringResource
import com.arshadshah.nimaz.R
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.domain.model.QaidaLetter
import com.arshadshah.nimaz.presentation.components.organisms.QaidaLetterBoard
import com.arshadshah.nimaz.presentation.components.organisms.QaidaLetterDetailSheet
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.qaida_arabic_letters)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            QaidaLetterBoard(
                letters = letters,
                heardLetterIds = emptySet(),
                onLetterClick = { selected = it },
            )
        }

        selected?.let { letter ->
            ModalBottomSheet(onDismissRequest = { selected = null }) {
                QaidaLetterDetailSheet(
                    letter = letter,
                    onPlay = viewModel::playLetter,
                )
            }
        }
    }
}
