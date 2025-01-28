package com.arshadshah.nimaz.ui.components.intro

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {

        val showDialog = remember { mutableStateOf(false) }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = { showDialog.value = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.time_calculation),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = "Calculation Method",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = getMethods().getValue(calculationSettingsState.calculationMethod),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        SettingsSelectionDialog(
            title = "Select Calculation Method",
            options = getMethods(),
            selectedOption = calculationSettingsState.calculationMethod,
            onOptionSelected = {
                viewModel.handleEvent(
                    IntroductionViewModel.IntroEvent.UpdateCalculationMethod(
                        it
                    )
                )
            },
            onDismiss = { showDialog.value = false },
            showDialog = showDialog.value
        )
    }
}