package com.arshadshah.nimaz.ui.components.tasbih

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.local.models.LocalTasbih
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
import com.arshadshah.nimaz.viewModel.TasbihViewModel
import es.dmoral.toasty.Toasty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomCounter(
    paddingValues: PaddingValues,
    tasbihId: String,
) {
    val context = LocalContext.current
    val viewModel = viewModel(
        key = AppConstants.TASBIH_VIEWMODEL_KEY,
        initializer = { TasbihViewModel(context) },
        viewModelStoreOwner = LocalContext.current as ComponentActivity
    )

    viewModel.handleEvent(TasbihViewModel.TasbihEvent.GetTasbih(tasbihId.toInt()))

    val resetTasbih = remember {
        viewModel.resetButtonState
    }.collectAsState()

    val tasbih = remember {
        viewModel.tasbihCreated
    }.collectAsState()

    val error = remember { viewModel.tasbihError }.collectAsState()

    val count = remember {
        mutableStateOf(tasbih.value.count)
    }

    LaunchedEffect(key1 = count.value) {
        //update the tasbih
        viewModel.handleEvent(
            TasbihViewModel.TasbihEvent.UpdateTasbih(
                LocalTasbih(
                    id = tasbih.value.id,
                    date = tasbih.value.date,
                    arabicName = tasbih.value.arabicName,
                    englishName = tasbih.value.englishName,
                    translationName = tasbih.value.translationName,
                    goal = tasbih.value.goal,
                    count = count.value,
                )
            )
        )
    }

    val objective = remember {
        mutableStateOf(tasbih.value.goal.toString())
    }

    val showObjectiveDialog = remember { mutableStateOf(false) }

    //lap counter
    val lap = remember {
        mutableStateOf(
            context.getSharedPreferences("tasbih", 0).getInt("lap-${tasbih.value.id}", 0)
        )
    }
    val lapCountCounter = remember {
        mutableStateOf(
            context.getSharedPreferences("tasbih", 0)
                .getInt("lapCountCounter-${tasbih.value.id}", 0)
        )
    }

    //when we firrst launch the composable, we want to set the count to the tasbih count
    LaunchedEffect(key1 = tasbih.value.id) {
        count.value =
            context.getSharedPreferences("tasbih", 0).getInt("count-${tasbih.value.id}", 0)
        objective.value = context.getSharedPreferences("tasbih", 0)
            .getString("objective-${tasbih.value.id}", tasbih.value.goal.toString())!!
        lap.value = context.getSharedPreferences("tasbih", 0).getInt("lap-${tasbih.value.id}", 0)
        lapCountCounter.value = context.getSharedPreferences("tasbih", 0)
            .getInt("lapCountCounter-${tasbih.value.id}", 0)
    }

    //persist all the values in shared preferences if the activity is destroyed
    LaunchedEffect(key1 = count.value, key2 = objective.value, key3 = lap.value)
    {
        //save the count
        context.getSharedPreferences("tasbih", 0).edit()
            .putInt("count-${tasbih.value.id}", count.value).apply()
        //save the objective
        context.getSharedPreferences("tasbih", 0).edit()
            .putString("objective-${tasbih.value.id}", objective.value)
            .apply()
        //save the lap
        context.getSharedPreferences("tasbih", 0).edit()
            .putInt("lap-${tasbih.value.id}", lap.value).apply()
        //save the lap count counter
        context.getSharedPreferences("tasbih", 0).edit()
            .putInt("lapCountCounter-${tasbih.value.id}", lapCountCounter.value).apply()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(paddingValues),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        //lap text
        Text(
            modifier = Modifier
                .align(Alignment.CenterHorizontally),
            text = "Loop ${lap.value}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        //large count text
        Text(
            modifier = Modifier
                .align(Alignment.CenterHorizontally),
            text = count.value.toString(),
            style = MaterialTheme.typography.displayMedium,
            fontSize = 100.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            modifier = Modifier
                .align(Alignment.CenterHorizontally),
            text = tasbih.value.goal.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(32.dp))
        IncrementDecrement(
            count = count,
            lap = lap,
            lapCountCounter = lapCountCounter,
            objective = objective,
        )
    }

    if (resetTasbih.value) {
        AlertDialogNimaz(
            bottomDivider = false,
            topDivider = false,
            contentHeight = 100.dp,
            contentDescription = "Reset Counter",
            title = "Reset Counter",
            confirmButtonText = "Yes",
            dismissButtonText = "No, Cancel",
            contentToShow = {
                Text(
                    text = "Are you sure you want to reset the counter?",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(8.dp)
                )
            },
            onDismissRequest = {
                viewModel.handleEvent(TasbihViewModel.TasbihEvent.UpdateResetButtonState(false))
            },
            onConfirm = {
                count.value = 0
                lap.value = 1
                lapCountCounter.value = 0
                viewModel.handleEvent(
                    TasbihViewModel.TasbihEvent.UpdateResetButtonState(
                        false
                    )
                )
            },
            onDismiss = {
                viewModel.handleEvent(TasbihViewModel.TasbihEvent.UpdateResetButtonState(false))
            })
    }

    if (showObjectiveDialog.value) {
        AlertDialogNimaz(
            cardContent = false,
            bottomDivider = false,
            topDivider = false,
            contentHeight = 100.dp,
            contentDescription = "Set Tasbih Objective",
            title = "Set Tasbih Objective",
            contentToShow = {
                OutlinedTextField(
                    shape = MaterialTheme.shapes.extraLarge,
                    textStyle = MaterialTheme.typography.titleLarge,
                    value = objective.value,
                    onValueChange = { objective.value = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    label = {
                        Text(
                            text = "Objective",
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val isInt = objective.value.toIntOrNull()
                            if (isInt != null) {
                                if (objective.value != "" || isInt != 0) {
                                    showObjectiveDialog.value = false
                                } else {
                                    Toasty
                                        .error(
                                            context,
                                            "Objective must be greater than 0",
                                            Toasty.LENGTH_SHORT
                                        )
                                        .show()
                                }
                            } else {
                                Toasty
                                    .error(
                                        context,
                                        "Objective must be greater than 0",
                                        Toasty.LENGTH_SHORT
                                    )
                                    .show()
                            }
                        })
                )
            },
            onDismissRequest = {
                showObjectiveDialog.value = false
            },
            onConfirm = {
                val isInt = objective.value.toIntOrNull()
                if (isInt != null) {
                    if (objective.value != "" || isInt != 0) {
                        showObjectiveDialog.value = false
                    } else {
                        Toasty
                            .error(
                                context,
                                "Objective must be greater than 0",
                                Toasty.LENGTH_SHORT
                            )
                            .show()
                    }
                } else {
                    Toasty
                        .error(
                            context,
                            "Objective must be greater than 0",
                            Toasty.LENGTH_SHORT
                        )
                        .show()
                }
            },
            onDismiss = {
                showObjectiveDialog.value = false
            })
    }
}