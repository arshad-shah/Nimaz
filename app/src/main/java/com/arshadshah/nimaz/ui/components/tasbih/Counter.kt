package com.arshadshah.nimaz.ui.components.tasbih

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.reflect.KFunction0
import kotlin.reflect.KFunction1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Counter(
    increment: KFunction0<Unit>,
    resetTasbih: State<Boolean>,
    count: State<Int>,
    decrement: KFunction0<Unit>,
    objective: State<Int>,
    setCounter: KFunction1<Int, Unit>,
    setObjective: KFunction1<Int, Unit>,
    setLap: KFunction1<Int, Unit>,
    setLapCounter: KFunction1<Int, Unit>,
    lap: State<Int>,
    lapCounter: State<Int>,
    resetTasbihState: KFunction0<Unit>,
    vibrationAllowed: State<Boolean>,
) {
    val showObjectiveDialog = remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(key1 = count.value, key2 = objective.value, key3 = lap.value) {
        context.getSharedPreferences("tasbih", Context.MODE_PRIVATE).edit().apply {
            putInt("count", count.value)
            putString("objective", objective.value.toString())
            putInt("lap", lap.value)
            putInt("lapCountCounter", lapCounter.value)
            apply()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Stats Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatCard(
                    title = "Loop",
                    value = lap.value.toString(),
                    color = MaterialTheme.colorScheme.primaryContainer
                )
                StatCard(
                    title = "Target",
                    value = if (objective.value > 0) objective.value.toString() else "-",
                    color = MaterialTheme.colorScheme.tertiaryContainer
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Counter Circle
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable {
                        if (objective.value > 0 && count.value + 1 > objective.value) {
                            setLap(lap.value + 1)
                            setCounter(0)
                            setLapCounter(lapCounter.value + 1)
                            if (vibrationAllowed.value) {
                                performHapticFeedback(context, true)
                            }
                        } else {
                            increment()
                            if (vibrationAllowed.value) {
                                performHapticFeedback(context, true)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = count.value.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        fontSize = 72.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (objective.value > 0) {
                        Text(
                            text = "${((count.value.toFloat() / objective.value) * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Control Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(
                    onClick = { decrement() },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Remove,
                        contentDescription = "Decrement",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                IconButton(
                    onClick = { showObjectiveDialog.value = true },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Set Target",
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }

                IconButton(
                    onClick = { resetTasbihState() },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Reset",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }

    // Dialogs
    if (resetTasbih.value) {
        AlertDialog(
            onDismissRequest = { resetTasbihState() },
            title = { Text("Reset Counter") },
            text = { Text("Are you sure you want to reset the counter?") },
            confirmButton = {
                TextButton(onClick = {
                    setCounter(0)
                    setLap(0)
                    setLapCounter(0)
                    resetTasbihState()
                }) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { resetTasbihState() }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showObjectiveDialog.value) {
        AlertDialog(
            onDismissRequest = { showObjectiveDialog.value = false },
            title = { Text("Set Target") },
            text = {
                OutlinedTextField(
                    value = if (objective.value == 0) "" else objective.value.toString(),
                    onValueChange = {
                        if (it.isNotEmpty()) setObjective(it.toIntOrNull() ?: 0)
                        else setObjective(0)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    label = { Text("Target Count") }
                )
            },
            confirmButton = {
                TextButton(onClick = { showObjectiveDialog.value = false }) {
                    Text("Set")
                }
            },
            dismissButton = {
                TextButton(onClick = { showObjectiveDialog.value = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    color: Color
) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .height(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = color
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun performHapticFeedback(context: Context, vibrationAllowed: Boolean) {
    if (!vibrationAllowed) return

    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
}