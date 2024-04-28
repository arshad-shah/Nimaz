package com.arshadshah.nimaz.ui.components.tasbih

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import kotlin.reflect.KFunction0

@Composable
fun IncrementButton(
    count: State<Int>,
    lap: State<Int>,
    lapCountCounter: State<Int>,
    objective: State<Int>,
    increment: KFunction0<Unit>,
    vibrationAllowed: State<Boolean>,
    onClick: () -> Unit,
) {

    val haptic = LocalHapticFeedback.current

    ElevatedButton(
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = ButtonDefaults.elevatedButtonElevation(
            defaultElevation = 4.dp
        ),
        contentPadding = PaddingValues(48.dp),
        onClick = {
            if (vibrationAllowed.value) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            increment()
            onClick()
        }) {
        Icon(
            modifier = Modifier.size(48.dp),
            painter = painterResource(id = R.drawable.plus_icon),
            contentDescription = "Add"
        )
    }
}