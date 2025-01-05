package com.arshadshah.nimaz.ui.components.intro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.components.settings.SettingValueState
import com.arshadshah.nimaz.ui.components.settings.SettingsList
import com.arshadshah.nimaz.ui.components.settings.SettingsSwitch
import com.arshadshah.nimaz.viewModel.IntroductionViewModel

@Composable
fun IntroCalculation(
    viewModel: IntroductionViewModel = hiltViewModel()
) {
    val calcAuto = viewModel.isAutoCalculation.collectAsState()
    val calculationMethod = viewModel.calculationMethod.collectAsState()
    val methods = viewModel.availableCalculationMethods.collectAsState()

    ElevatedCard(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
    ) {
        SettingsSwitch(
            state = createBooleanState(calcAuto.value),
            title = {
                if (calcAuto.value) {
                    Text(text = "Auto Calculation")
                } else {
                    Text(text = "Manual Calculation")
                }
            },
            subtitle = {
                Text(text = "Auto angles are Experimental")
            },
            onCheckedChange = { enabled ->
                viewModel.handleEvent(
                    IntroductionViewModel.IntroEvent.ToggleAutoCalculation(enabled)
                )
            }
        )
    }

    AnimatedVisibility(
        visible = !calcAuto.value,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        ElevatedCard(
            modifier = Modifier
                .padding(4.dp)
                .fillMaxWidth()
        ) {
            SettingsList(
                title = "Calculation Method",
                subtitle = calculationMethod.value,
                description = "The method used to calculate the prayer times.",
                icon = {
                    Image(
                        modifier = Modifier.size(34.dp),
                        painter = painterResource(id = R.drawable.time_calculation),
                        contentDescription = "Calculation Method"
                    )
                },
                items = methods.value,
                valueState = createValueState(calculationMethod.value),
                onChange = { method ->
                    viewModel.handleEvent(
                        IntroductionViewModel.IntroEvent.UpdateCalculationMethod(method)
                    )
                },
                height = 300.dp
            )
        }
    }
}

// Add these at the bottom of your file
private fun createValueState(value: String) = object : SettingValueState<String> {
    override var value: String = value
    override fun reset() {}
}

private fun createBooleanState(value: Boolean) = object : SettingValueState<Boolean> {
    override var value: Boolean = value
    override fun reset() {}
}