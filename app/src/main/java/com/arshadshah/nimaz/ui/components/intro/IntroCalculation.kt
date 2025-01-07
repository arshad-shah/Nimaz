package com.arshadshah.nimaz.ui.components.intro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants.getMethods
import com.arshadshah.nimaz.ui.components.common.SettingsSelectionDialog
import com.arshadshah.nimaz.viewModel.IntroductionViewModel

@Composable
fun IntroCalculation(
    viewModel: IntroductionViewModel = hiltViewModel()
) {
    val calculationSettingsState by viewModel.calculationSettingsState.collectAsState()

    ElevatedCard(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
    ) {
        // Auto Parameters Toggle
        Surface(
            onClick = {
                viewModel.handleEvent(
                    IntroductionViewModel.IntroEvent.ToggleAutoCalculation(
                        !calculationSettingsState.isAutoCalculation
                    )
                )
            },
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.time_calculation),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = if (calculationSettingsState.isAutoCalculation) "Auto" else "Manual",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Experimental",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = calculationSettingsState.isAutoCalculation,
                    onCheckedChange = {
                        viewModel.handleEvent(
                            IntroductionViewModel.IntroEvent.ToggleAutoCalculation(it)
                        )
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
    }

    AnimatedVisibility(
        visible = !calculationSettingsState.isAutoCalculation,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        CalculationMethodItem(
            calculationMethod = calculationSettingsState.calculationMethod,
            onCalculationMethodChange = { method ->
                viewModel.handleEvent(
                    IntroductionViewModel.IntroEvent.UpdateCalculationMethod(method)
                )
            }
        )
    }
}


@Composable
private fun CalculationMethodItem(
    calculationMethod: String,
    onCalculationMethodChange: (String) -> Unit
) {

    val showDialog = remember { mutableStateOf(false) }
    val methods = remember { getMethods() }
    Surface(
        onClick = { showDialog.value = true },
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.time_calculation),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Calculation Method",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = calculationMethod,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    SettingsSelectionDialog(
        title = "Select Calculation Method",
        options = methods,
        selectedOption = calculationMethod,
        onOptionSelected = onCalculationMethodChange,
        onDismiss = { showDialog.value = false },
        showDialog = showDialog.value
    )
}
