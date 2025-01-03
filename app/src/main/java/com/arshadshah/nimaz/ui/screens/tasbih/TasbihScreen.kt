package com.arshadshah.nimaz.ui.screens.tasbih

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_TASBIH
import com.arshadshah.nimaz.ui.components.tasbih.Counter
import com.arshadshah.nimaz.ui.components.tasbih.CustomCounter
import com.arshadshah.nimaz.ui.components.tasbih.TasbihRow
import com.arshadshah.nimaz.viewModel.TasbihViewModel
import kotlin.reflect.KFunction0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbihScreen(
    paddingValues: PaddingValues,
    tasbihId: String = "",
    tasbihArabic: String = "",
    tasbihEnglish: String = "",
    tasbihTranslitration: String = "",
    navController: NavHostController,
) {
    val context = LocalContext.current
    val viewModel = viewModel(
        key = AppConstants.TASBIH_VIEWMODEL_KEY,
        initializer = { TasbihViewModel(context) },
        viewModelStoreOwner = LocalContext.current as ComponentActivity
    )

    val rOrl = viewModel.orientationButtonState.collectAsState()
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
                    OutlinedIconButton(
                        modifier = Modifier
                            .testTag("backButton")
                            .padding(start = 8.dp),
                        onClick = {
                            navController.navigateUp()
                        }) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(id = R.drawable.back_icon),
                            contentDescription = "Back"
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
                CustomCounter(
                    tasbihId,
                    resetTasbih = resetButtonState,
                    count = counter,
                    tasbih = tasbihCreated,
                    rOrl = rOrl,
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
                    getTasbih = viewModel::getTasbih,
                )
            } else {
                Counter(
                    paddingValues,
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
            modifier = Modifier.size(
                24.dp
            ),
            painter = if (vibrationAllowed.value) painterResource(
                id = R.drawable.phone_vibration_off_icon
            )
            else painterResource(
                id = R.drawable.phone_vibration_on_icon
            ),
            contentDescription = "Vibration"
        )
    }
}