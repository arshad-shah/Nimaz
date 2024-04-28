package com.arshadshah.nimaz.ui.components.prayerTimes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.components.common.CustomText
import com.arshadshah.nimaz.viewModel.SettingsViewModel
import java.util.Locale
import kotlin.reflect.KFunction1

@Composable
fun LocationTimeContainer(
    locationState: State<String>,
    currentPrayerName: State<String>,
    handleEvent: KFunction1<SettingsViewModel.SettingsEvent, Unit>,
    isLoading: State<Boolean>,
) {
    if (isLoading.value) {
        ContainerUI(
            currentPrayerNameSentenceCase = "Loading...",
            location = locationState,
            handleEvent = handleEvent
        )
    } else {
        val currentPrayerNameSentenceCase = currentPrayerName.value
            .substring(0, 1)
            .uppercase(Locale.ROOT) + currentPrayerName.value
            .substring(1).lowercase(Locale.ROOT)

        ContainerUI(
            currentPrayerNameSentenceCase = currentPrayerNameSentenceCase,
            location = locationState,
            handleEvent = handleEvent
        )
    }
}

@Composable
fun ContainerUI(
    currentPrayerNameSentenceCase: String,
    location: State<String>,
    handleEvent: KFunction1<SettingsViewModel.SettingsEvent, Unit>,
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .padding(4.dp)
            .height(IntrinsicSize.Max)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CustomText(
                modifier = Modifier
                    .weight(0.5f)
                    .padding(8.dp)
                    .clickable {
                        handleEvent(SettingsViewModel.SettingsEvent.LoadLocation(context))
                    },
                heading = "Location", text = location.value
            )
            //vertical divider line
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
            )
            CustomText(
                modifier = Modifier
                    .weight(0.5f)
                    .padding(8.dp),
                heading = "Current Prayer",
                //fix the name to be sentence case,
                text = currentPrayerNameSentenceCase
            )
        }
    }
}