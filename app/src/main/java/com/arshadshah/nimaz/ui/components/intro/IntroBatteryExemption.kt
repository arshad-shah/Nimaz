package com.arshadshah.nimaz.ui.components.intro

import android.annotation.SuppressLint
import android.content.Context
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.components.common.PermissionItem
import com.arshadshah.nimaz.ui.components.settings.state.rememberPreferenceBooleanSettingState
import com.arshadshah.nimaz.viewModel.IntroductionViewModel


@SuppressLint("BatteryLife")
@Composable
fun IntroBatteryExemption(
    viewModel: IntroductionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    // Read battery state directly from system
    val isExempt = remember {
        mutableStateOf(powerManager.isIgnoringBatteryOptimizations(context.packageName))
    }

    // Create preference state for UI
    val state = rememberPreferenceBooleanSettingState(
        AppConstants.BATTERY_OPTIMIZATION,
        isExempt.value
    )

    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle

    // Update ViewModel when battery state changes
    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val currentExemptStatus =
                    powerManager.isIgnoringBatteryOptimizations(context.packageName)
                isExempt.value = currentExemptStatus
                viewModel.handleEvent(
                    IntroductionViewModel.IntroEvent.UpdateBatteryOptimization(
                        currentExemptStatus
                    )
                )
            }
        }

        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
    PermissionItem(
        title = "Battery Optimization",
        description = "Exempt Nimaz from battery optimization to receive Adhan notifications on time",
        icon = painterResource(id = R.drawable.battery),
        isGranted = isExempt.value,
        onPermissionChange = { enabled ->
            viewModel.handleEvent(IntroductionViewModel.IntroEvent.HandleBatteryOptimization(enabled))
        },
    )
}