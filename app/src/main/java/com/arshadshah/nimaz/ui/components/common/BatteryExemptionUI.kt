package com.arshadshah.nimaz.ui.components.common

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.components.settings.SettingsSwitch
import com.arshadshah.nimaz.ui.components.settings.state.rememberPreferenceBooleanSettingState

@SuppressLint("BatteryLife")
@Composable
fun BatteryExemptionUI(isBatteryExempt: Boolean, onBatteryExemptChange: (Boolean) -> Unit) {
    val context = LocalContext.current

    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle

    //battery optimization exemption
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val isChecked =
        remember { mutableStateOf(powerManager.isIgnoringBatteryOptimizations(context.packageName)) }

    //the state of the switch
    val state = rememberPreferenceBooleanSettingState(
        AppConstants.BATTERY_OPTIMIZATION,
        isChecked.value
    )

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    onBatteryExemptChange(
                        powerManager.isIgnoringBatteryOptimizations(
                            context.packageName
                        )
                    )
                    state.value = powerManager.isIgnoringBatteryOptimizations(context.packageName)
                    isChecked.value =
                        powerManager.isIgnoringBatteryOptimizations(context.packageName)
                }

                else -> {

                }
            }
        }

        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }


    SettingsSwitch(
        modifier = Modifier.testTag("BatteryExemptionSwitch"),
        state = state,
        onCheckedChange = {
            if (it) {
                val intent = Intent()
                intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                intent.data = Uri.parse("package:" + context.packageName)
                context.startActivity(intent)
            } else {
                //navigate to the battery optimization settings
                val intent = Intent()
                intent.action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                context.startActivity(intent)
            }
        },
        title = {
            Text(
                text = "Battery Optimization",
            )
        },
        subtitle = {
            //if the permission is granted, show a checkmark and text saying "Allowed"
            if (isBatteryExempt) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 4.dp),
                        painter = painterResource(id = R.drawable.checkbox_icon),
                        contentDescription = "Battery Exemption Allowed"
                    )
                    Text(text = "Exempt")
                }
            } else {
                //if the permission is not granted, show a notification icon and text saying "Not Allowed"
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 4.dp),
                        painter = painterResource(id = R.drawable.cross_circle_icon),
                        contentDescription = "Battery Exemption Not Allowed"
                    )
                    Text(text = "Optimizing")
                }
            }
        },
        icon = {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(id = R.drawable.battery),
                contentDescription = "Battery Optimization"
            )
        }
    )

    if (!isBatteryExempt) {
        BannerSmall(
            message = "Exempt Nimaz from battery optimization to receive Adhan notifications on time",
            showFor = BannerDuration.FOREVER.value
        )
    }
}