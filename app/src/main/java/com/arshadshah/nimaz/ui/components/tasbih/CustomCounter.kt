package com.arshadshah.nimaz.ui.components.tasbih

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.data.local.models.LocalTasbih
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
import es.dmoral.toasty.Toasty
import kotlin.reflect.KFunction0
import kotlin.reflect.KFunction1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomCounter(
    tasbihId: String,
    increment: KFunction0<Unit>,
    updateTasbih: KFunction1<LocalTasbih, Unit>,
    resetTasbih: State<Boolean>,
    count: State<Int>,
    decrement: KFunction0<Unit>,
    tasbih: State<LocalTasbih>,
    objective: State<Int>,
    setCounter: KFunction1<Int, Unit>,
    setObjective: KFunction1<Int, Unit>,
    setLap: KFunction1<Int, Unit>,
    setLapCounter: KFunction1<Int, Unit>,
    lap: State<Int>,
    lapCounter: State<Int>,
    resetTasbihState: KFunction0<Unit>,
    rOrl: State<Boolean>,
    vibrationAllowed: State<Boolean>,
    getTasbih: KFunction1<Int, Unit>,
) {
    val showObjectiveDialog = remember { mutableStateOf(false) }

    val context = LocalContext.current

    //when we firrst launch the composable, we want to set the count to the tasbih count
    LaunchedEffect(key1 = tasbih.value.id) {
        getTasbih(tasbih.value.id)
        setLapCounter(context.getSharedPreferences("tasbih", 0).getInt("lapCountCounter-${tasbih.value.id}", 0))
        setLap(context.getSharedPreferences("tasbih", 0).getInt("lap-${tasbih.value.id}", 0))
        setCounter(context.getSharedPreferences("tasbih", 0).getInt("count-${tasbih.value.id}", 0))
        setObjective(
            context.getSharedPreferences("tasbih", 0)
                .getString("objective-${tasbih.value.id}", tasbih.value.goal.toString())!!.toInt()
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
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
            lapCountCounter = lapCounter,
            objective = objective,
            increment = increment,
            rOrl = rOrl,
            decrement = decrement,
            vibrationAllowed = vibrationAllowed,
            onClick = {
                updateTasbih(
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
                Toasty.success(context, "Tasbih Updated").show()
            }
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
                resetTasbihState()
            },
            onConfirm = {
                setCounter(0)
                setObjective(tasbih.value.goal)
                setLap(1)
                setLapCounter(0)
            },
            onDismiss = {
                resetTasbihState()
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
                    value = objective.value.toString(),
                    onValueChange = {
                                    if (it.isNotEmpty()) {
                                        setObjective(it.toInt())
                                    }else{
                                        setObjective(0)
                                    }
                    },
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
                            showObjectiveDialog.value = false
                        })
                )
            },
            onDismissRequest = {
                showObjectiveDialog.value = false
            },
            onConfirm = {
                showObjectiveDialog.value = false
            },
            onDismiss = {
                showObjectiveDialog.value = false
            })
    }
}