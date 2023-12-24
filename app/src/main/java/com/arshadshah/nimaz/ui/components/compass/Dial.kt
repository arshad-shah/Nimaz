package com.arshadshah.nimaz.ui.components.compass

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun Dial(
    state: State<Double>,
    imageToDisplay: Painter,
    errorMessage: String,
    isLoading: Boolean,
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var data by remember { mutableStateOf<SensorData?>(null) }

    DisposableEffect(Unit) {
        val dataManager = SensorDataManager(context)
        dataManager.init(context)

        val job = scope.launch {
            dataManager.data
                .receiveAsFlow()
                .onEach { data = it }
                .collect {
                    // do nothing
                }
        }

        onDispose {
            dataManager.cancel()
            job.cancel()
        }
    }

    if (isLoading) {
        DialUI(0.0, data, imageToDisplay)
    } else if (errorMessage != "") {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
        {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.titleMedium,
                color = Color.Red,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
    } else {
        DialUI(state.value, data, imageToDisplay)
    }
}

@RequiresApi(Build.VERSION_CODES.S)
@SuppressLint("UnrememberedMutableState")
@Composable
fun DialUI(bearing: Double, data: SensorData?, imageToDisplay: Painter) {
    val yaw by derivedStateOf { (data?.yaw ?: 0f) }
    val pitch by derivedStateOf { (data?.pitch ?: 0f) }
    val roll by derivedStateOf { (data?.roll ?: 0f) }
    val degree = ((Math.toDegrees(yaw.toDouble()) + 360).toFloat() % 360)

    val currentAngle = 0f
    val rotateAnim = remember { Animatable(currentAngle) }
    val target = (bearing - degree).toFloat()
    val context = LocalContext.current
    val vibrator = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager

    val pointingToQibla = abs(target) < 5f

    val directionToTurn = when {
        abs(target) < 5f -> "You are facing the Qibla"
        target > 0f -> "Turn Right"
        else -> "Turn Left"
    }

    //if the user is facing the qibla, vibrate the phone and show a message to the user and stop the vibration after 1 second and stop animating the dial until the user turns away from the qibla
    LaunchedEffect(key1 = target, key2 = pointingToQibla) {
        if (abs(target) < 5f) {
            //animate to 0f
            rotateAnim.animateTo(0f, tween(100))
            //stop the animation
            rotateAnim.stop()
        } else {
            //start the animation
            rotateAnim.animateTo(target, tween(200))
        }

        if (pointingToQibla) {
            //create a single shot vibration
            vibrator.vibrate(
                CombinedVibration.createParallel(
                    VibrationEffect.createOneShot(
                        100,
                        255
                    )
                )
            )
        } else {
            vibrator.cancel()
        }
    }
    DialComponent(directionToTurn, pointingToQibla, imageToDisplay, rotateAnim)
}