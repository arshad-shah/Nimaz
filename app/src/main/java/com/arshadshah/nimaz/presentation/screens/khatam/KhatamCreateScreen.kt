package com.arshadshah.nimaz.presentation.screens.khatam

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.Khatam
import com.arshadshah.nimaz.presentation.components.molecules.NimazNumberStepper
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.KhatamEvent
import com.arshadshah.nimaz.presentation.viewmodel.KhatamViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhatamCreateScreen(
    onNavigateBack: () -> Unit,
    viewModel: KhatamViewModel = hiltViewModel()
) {
    val state by viewModel.createState.collectAsState()

    // Navigate back after successful creation
    LaunchedEffect(state.isCreating) {
        if (!state.isCreating && state.name.isEmpty() && state.error == null) {
            // State was reset after creation
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.khatam_new),
                onBackClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Name field
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.khatam_name_label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = { viewModel.onEvent(KhatamEvent.UpdateName(it)) },
                        placeholder = { Text(stringResource(R.string.khatam_name_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        isError = state.error != null
                    )
                    state.error?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Daily target stepper
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    NimazNumberStepper(
                        label = stringResource(R.string.khatam_daily_target),
                        value = state.dailyTarget,
                        onValueChange = { viewModel.onEvent(KhatamEvent.UpdateDailyTarget(it)) },
                        formatValue = { "$it ayahs" },
                        minValue = 1,
                        maxValue = 200,
                        step = 5
                    )

                    // Estimated completion
                    val daysToComplete = if (state.dailyTarget > 0) {
                        (Khatam.TOTAL_QURAN_AYAHS.toFloat() / state.dailyTarget).toInt()
                    } else 0
                    Text(
                        text = stringResource(R.string.khatam_estimated_completion, daysToComplete),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 15.dp, vertical = 4.dp)
                    )
                }
            }

            // Notes field
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.khatam_notes_label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = state.notes,
                        onValueChange = { viewModel.onEvent(KhatamEvent.UpdateNotes(it)) },
                        placeholder = { Text(stringResource(R.string.khatam_notes_placeholder)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 4
                    )
                }
            }

            // Create button
            item {
                Button(
                    onClick = {
                        viewModel.onEvent(KhatamEvent.CreateKhatam)
                        onNavigateBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = state.name.isNotBlank() && !state.isCreating
                ) {
                    Text(
                        text = if (state.isCreating) stringResource(R.string.khatam_creating) else stringResource(R.string.khatam_start),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
