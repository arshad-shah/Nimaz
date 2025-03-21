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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
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
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats Card
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatItem(
                                title = "Loop",
                                value = lap.value.toString(),
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                            StatItem(
                                title = "Target",
                                value = if (objective.value > 0) objective.value.toString() else "-",
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }

            // Counter Card
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier
                            .size(200.dp)
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
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = count.value.toString(),
                                style = MaterialTheme.typography.displayLarge,
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ActionButton(
                            icon = Icons.Rounded.Remove,
                            label = "Decrement",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            onClick = decrement
                        )

                        ActionButton(
                            icon = Icons.Rounded.Edit,
                            label = "Set Target",
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            onClick = { showObjectiveDialog.value = true }
                        )

                        ActionButton(
                            icon = Icons.Rounded.Refresh,
                            label = "Reset",
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            onClick = resetTasbihState
                        )
                    }
                }
            }
        }
    }

    if (resetTasbih.value) {
        ResetDialog(
            onConfirm = {
                setCounter(0)
                setLap(0)
                setLapCounter(0)
                resetTasbihState()
            },
            onDismiss = resetTasbihState
        )
    }

    if (showObjectiveDialog.value) {
        ObjectiveDialog(
            currentObjective = objective.value,
            onObjectiveSet = setObjective,
            onDismiss = { showObjectiveDialog.value = false }
        )
    }
}

@Composable
private fun StatItem(
    title: String,
    value: String,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .size(64.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = containerColor
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.padding(16.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun ObjectiveDialog(
    currentObjective: Int,
    onObjectiveSet: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialogNimaz(
        title = "Set Target",
        contentDescription = "Set the target count for the tasbih",
        contentToShow = {
            Column {
                OutlinedTextField(
                    value = if (currentObjective == 0) "" else currentObjective.toString(),
                    onValueChange = { value ->
                        if (value.isNotEmpty()) onObjectiveSet(value.toIntOrNull() ?: 0)
                        else onObjectiveSet(0)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    label = { Text("Target Count") }
                )
            }
        },
        contentHeight = 100.dp,
        cardContent = true,
        onDismissRequest = onDismiss,
        onConfirm = onDismiss,
        confirmButtonText = "Set",
        onDismiss = onDismiss,
        dismissButtonText = "Cancel",
    )
}

@Composable
private fun ResetDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialogNimaz(
        title = "Reset Counter",
        contentDescription = "Are you sure you want to reset the counter?",
        contentToShow = {
            Column {
                Text("Are you sure you want to reset the counter?")
            }
        },
        contentHeight = 100.dp,
        cardContent = true,
        onDismissRequest = onDismiss,
        onConfirm = {
            onConfirm()
        },
        confirmButtonText = "Reset",
        onDismiss = onDismiss,
        dismissButtonText = "Cancel",
    )
}

private fun performHapticFeedback(context: Context, vibrationAllowed: Boolean) {
    if (!vibrationAllowed) return
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
}