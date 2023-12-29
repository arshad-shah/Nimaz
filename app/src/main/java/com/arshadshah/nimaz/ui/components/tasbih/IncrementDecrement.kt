package com.arshadshah.nimaz.ui.components.tasbih

import androidx.activity.ComponentActivity
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.viewModel.TasbihViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun IncrementDecrement(
    count: MutableState<Int>,
    lap: MutableState<Int>,
    lapCountCounter: MutableState<Int>,
    objective: MutableState<String>,
) {
    val context = LocalContext.current
    val viewModel = viewModel(
        key = AppConstants.TASBIH_VIEWMODEL_KEY,
        initializer = { TasbihViewModel(context) },
        viewModelStoreOwner = LocalContext.current as ComponentActivity
    )
    val rOrl = remember {
        viewModel.orientationButtonState
    }.collectAsState()

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
                    lap = lap,
                    lapCountCounter = lapCountCounter,
                    objective = objective,
                )

                Spacer(modifier = Modifier.width(16.dp))
                IncrementButton(
                    count = count,
                    lap = lap,
                    lapCountCounter = lapCountCounter,
                    objective = objective,
                    context = context
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
                    context = context
                )
                Spacer(modifier = Modifier.width(16.dp))
                Decrementbutton(
                    count = count,
                    lap = lap,
                    lapCountCounter = lapCountCounter,
                    objective = objective,
                )

            }
        }
    }
}