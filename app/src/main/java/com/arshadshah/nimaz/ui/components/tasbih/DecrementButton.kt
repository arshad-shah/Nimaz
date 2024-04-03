package com.arshadshah.nimaz.ui.components.tasbih

import android.os.VibrationEffect
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants.TASBIH_VIEWMODEL_KEY
import com.arshadshah.nimaz.viewModel.TasbihViewModel
import kotlin.reflect.KFunction0

@Composable
fun Decrementbutton(
    count: State<Int>,
    decrement: KFunction0<Unit>,
    vibrationAllowed: State<Boolean>,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    ElevatedButton(
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        contentPadding = PaddingValues(24.dp),
        onClick = {
                if (vibrationAllowed.value) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                decrement()
                onClick()
        }) {
        Icon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(id = R.drawable.minus_icon),
            contentDescription = "Delete"
        )
    }

}