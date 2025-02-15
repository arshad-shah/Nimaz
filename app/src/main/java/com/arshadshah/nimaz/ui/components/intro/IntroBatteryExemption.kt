package com.arshadshah.nimaz.ui.components.intro

import android.annotation.SuppressLint
import android.content.Context
import android.os.PowerManager
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.components.common.BannerDuration
import com.arshadshah.nimaz.ui.components.common.BannerSmall
import com.arshadshah.nimaz.ui.components.common.BannerVariant
import com.arshadshah.nimaz.ui.components.settings.state.rememberPreferenceBooleanSettingState
import com.arshadshah.nimaz.viewModel.IntroductionViewModel
@SuppressLint("BatteryLife")
@Composable
fun IntroBatteryExemption(
    viewModel: IntroductionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val lifecycle = LocalLifecycleOwner.current.lifecycle

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
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Section
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.battery),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Battery Optimization",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = if (isExempt.value) "Optimizations disabled" else "Optimizations enabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Switch(
                        checked = isExempt.value,
                        onCheckedChange = { enabled ->
                            viewModel.handleEvent(
                                IntroductionViewModel.IntroEvent.HandleBatteryOptimization(enabled)
                            )
                        },
                    )
                }
            }

            BannerSmall(
                message = if (isExempt.value)
                    "Battery optimization disabled"
                else
                    "Enable to ensure timely prayer notifications",
                variant = if (isExempt.value)
                    BannerVariant.Success
                else
                    BannerVariant.Warning,
                showFor = BannerDuration.FOREVER.value
            )

            // Features Section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BatteryFeature(
                        icon = R.drawable.adhan,
                        title = "Reliable Notifications",
                        description = "Ensure Adhan notifications arrive on time"
                    )
                    BatteryFeature(
                        icon = R.drawable.time_calculation,
                        title = "Background Updates",
                        description = "Allow prayer time calculations in background"
                    )
                    BatteryFeature(
                        icon = R.drawable.tracker_icon,
                        title = "Accurate Tracking",
                        description = "Maintain precise prayer tracking functionality"
                    )
                }
            }
        }
    }
}

@Composable
private fun BatteryFeature(
    icon: Int,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(48.dp)
        ) {
            Image(
                painter = painterResource(id = icon),
                contentDescription = null,
                modifier = Modifier
                    .padding(12.dp)
                    .size(24.dp),
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}