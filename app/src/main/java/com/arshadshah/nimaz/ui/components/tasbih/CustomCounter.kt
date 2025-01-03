package com.arshadshah.nimaz.ui.components.tasbih

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
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
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.data.local.models.LocalTasbih
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont
import es.dmoral.toasty.Toasty
import kotlin.reflect.KFunction0
import kotlin.reflect.KFunction1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomCounter(
    tasbihId: String,
    increment: () -> Unit,
    updateTasbih: (LocalTasbih) -> Unit,
    resetTasbih: State<Boolean>,
    count: State<Int>,
    decrement: () -> Unit,
    tasbih: State<LocalTasbih>,
    objective: State<Int>,
    setCounter: (Int) -> Unit,
    setObjective: (Int) -> Unit,
    setLap: (Int) -> Unit,
    setLapCounter: (Int) -> Unit,
    lap: State<Int>,
    lapCounter: State<Int>,
    resetTasbihState: () -> Unit,
    rOrl: State<Boolean>,
    vibrationAllowed: State<Boolean>,
    getTasbih: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val showObjectiveDialog = remember { mutableStateOf(false) }
    val context = LocalContext.current
    Log.d("tasbihId", tasbih.value.toString())
    LaunchedEffect(Unit) {
        setObjective(tasbih.value.goal)
        setCounter(tasbih.value.count)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        LaunchedEffect(count.value) {
            updateTasbih(tasbih.value.copy(count = count.value))
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TasbihHeader(tasbih = tasbih.value)
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

            CounterCircle(
                count = count.value,
                objective = objective.value,
                onClick = {
                    if (objective.value > 0 && count.value + 1 > objective.value) {
                        setLap(lap.value + 1)
                        setCounter(0)
                        setLapCounter(lapCounter.value + 1)
                        if (vibrationAllowed.value) {
                            performHapticFeedback(context)
                        }
                    } else {
                        increment()
                        if (vibrationAllowed.value) {
                            performHapticFeedback(context)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            ControlButtons(
                onDecrement = decrement,
                onSetObjective = { showObjectiveDialog.value = true },
                onReset = resetTasbihState
            )
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
private fun CounterCircle(
    count: Int,
    objective: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(280.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.displayLarge,
                fontSize = 72.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (objective > 0) {
                Text(
                    text = "${((count.toFloat() / objective) * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun ControlButtons(
    onDecrement: () -> Unit,
    onSetObjective: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        CircleIconButton(
            icon = Icons.Rounded.Remove,
            contentDescription = "Decrement",
            backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            onClick = onDecrement
        )

        CircleIconButton(
            icon = Icons.Rounded.Edit,
            contentDescription = "Set Target",
            backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            onClick = onSetObjective
        )

        CircleIconButton(
            icon = Icons.Rounded.Refresh,
            contentDescription = "Reset",
            backgroundColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            onClick = onReset
        )
    }
}

@Composable
private fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    backgroundColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(backgroundColor)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor
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
        colors = CardDefaults.cardColors(containerColor = color)
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

@Composable
private fun ResetDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset Counter") },
        text = { Text("Are you sure you want to reset the counter?") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Reset") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ObjectiveDialog(
    currentObjective: Int,
    onObjectiveSet: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Target") },
        text = {
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
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Set") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun TasbihHeader(tasbih: LocalTasbih) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text(
                text = tasbih.arabicName,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = utmaniQuranFont,
                    fontWeight = FontWeight.SemiBold
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp)
            )
        }

        Text(
            text = tasbih.englishName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = tasbih.translationName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

private fun performHapticFeedback(context: Context) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
}