package com.arshadshah.nimaz.ui.screens.tasbih

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_TASBIH
import com.arshadshah.nimaz.ui.components.tasbih.CompactCustomCounter
import com.arshadshah.nimaz.ui.components.tasbih.Counter
import com.arshadshah.nimaz.viewModel.TasbihViewModel
import kotlin.reflect.KFunction0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbihScreen(
    tasbihId: String = "",
    tasbihArabic: String = "",
    tasbihEnglish: String = "",
    tasbihTranslitration: String = "",
    navController: NavHostController,
    viewModel: TasbihViewModel = hiltViewModel()
) {
    LaunchedEffect(
        key1 = tasbihId,
        block = {
            if (tasbihId.isNotBlank()) {
                viewModel.getTasbihById(tasbihId.toInt())
            }
        }
    )

    val vibrationAllowed = viewModel.vibrationButtonState.collectAsState()
    val resetButtonState = viewModel.resetButtonState.collectAsState()

    val counter = viewModel.counter.collectAsState()
    val tasbihCreated = viewModel.tasbihCreated.collectAsState()
    val objective = viewModel.objective.collectAsState()
    val lap = viewModel.lap.collectAsState()
    val lapCounter = viewModel.lapCounter.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text("Tasbih", style = MaterialTheme.typography.titleLarge)
            },
                navigationIcon = {
                    OutlinedIconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
                actions = {
                    TopBarActionsTasbih(
                        vibrationAllowed,
                        viewModel::toggleVibration,
                    )
                }
            )
        },
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .testTag(TEST_TAG_TASBIH),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,

            ) {

            if (tasbihArabic.isNotBlank() && tasbihEnglish.isNotBlank() && tasbihTranslitration.isNotBlank() && tasbihId.isNotBlank()) {
                CompactCustomCounter(
                    tasbihId,
                    resetTasbih = resetButtonState,
                    count = counter,
                    tasbih = tasbihCreated,
                    lap = lap,
                    lapCounter = lapCounter,
                    objective = objective,
                    vibrationAllowed = vibrationAllowed,
                    increment = viewModel::incrementCounter,
                    decrement = viewModel::decrementCounter,
                    updateTasbih = viewModel::updateTasbih,
                    setCounter = viewModel::setCounter,
                    setObjective = viewModel::setObjective,
                    setLap = viewModel::setLap,
                    setLapCounter = viewModel::setLapCounter,
                    resetTasbihState = viewModel::resetTasbih,
                )
            } else {
                Counter(
                    resetTasbih = resetButtonState,
                    count = counter,
                    lap = lap,
                    lapCounter = lapCounter,
                    objective = objective,
                    vibrationAllowed = vibrationAllowed,
                    increment = viewModel::incrementCounter,
                    decrement = viewModel::decrementCounter,
                    setCounter = viewModel::setCounter,
                    setObjective = viewModel::setObjective,
                    setLap = viewModel::setLap,
                    setLapCounter = viewModel::setLapCounter,
                    resetTasbihState = viewModel::resetTasbih,
                )
            }
        }
    }
}

@Composable
fun TopBarActionsTasbih(
    vibrationAllowed: State<Boolean>,
    updateVibrationButtonState: KFunction0<Unit>,
) {
    //vibration toggle button for tasbih to provide feedback
    IconButton(onClick = {
        updateVibrationButtonState()
    }) {
        Icon(
            imageVector = if (vibrationAllowed.value) {
                Icons.Rounded.Vibration
            } else {
                ImageVector.vectorResource(id = R.drawable.phone_vibration_on_icon)
            },
            contentDescription = if (vibrationAllowed.value) {
                "Disable vibration feedback"
            } else {
                "Enable vibration feedback"
            },
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}