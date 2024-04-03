package com.arshadshah.nimaz.ui.components.tasbih

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.reflect.KFunction0

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun IncrementDecrement(
    count: State<Int>,
    lap: State<Int>,
    lapCountCounter: State<Int>,
    objective: State<Int>,
    increment: KFunction0<Unit>,
    decrement: KFunction0<Unit>,
    vibrationAllowed: State<Boolean>,
    onClick: () -> Unit,
    rOrl: State<Boolean>,
) {
    AnimatedContent(
        transitionSpec = {
            ContentTransform(
                fadeIn() +
                        slideInHorizontally(
                            animationSpec = tween(500)
                        ),
                fadeOut() + slideOutHorizontally(
                    animationSpec = tween(500)
                )
            )
        },
        targetState = rOrl.value, label = ""
    ) { rorl ->
        if (rorl) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Decrementbutton(
                    count = count,
                    decrement = decrement,
                    vibrationAllowed = vibrationAllowed,
                    onClick = onClick
                )

                Spacer(modifier = Modifier.width(16.dp))
                IncrementButton(
                    count = count,
                    lap = lap,
                    lapCountCounter = lapCountCounter,
                    objective = objective,
                    increment = increment,
                    vibrationAllowed = vibrationAllowed,
                    onClick = onClick
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IncrementButton(
                    count = count,
                    lap = lap,
                    lapCountCounter = lapCountCounter,
                    objective = objective,
                    increment = increment,
                    vibrationAllowed = vibrationAllowed,
                    onClick = onClick,
                )
                Spacer(modifier = Modifier.width(16.dp))
                Decrementbutton(
                    count = count,
                    decrement = decrement,
                    vibrationAllowed = vibrationAllowed,
                    onClick = onClick,
                )

            }
        }
    }
}