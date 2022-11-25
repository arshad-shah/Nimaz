package com.arshadshah.nimaz.ui.components.ui.prayerTimes

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import com.arshadshah.nimaz.data.remote.models.CountDownTime
import com.arshadshah.nimaz.data.remote.viewModel.CountTimeViewModel

@Composable
fun CurrentNextPrayerContainerUI(
    currentPrayerName: String,
    state: LiveData<CountDownTime>
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(0.dp, 8.dp)
            .height(IntrinsicSize.Max)
            .shadow(5.dp, shape = MaterialTheme.shapes.medium, clip = true)
    ) {
        //align items to center

        Row(horizontalArrangement = Arrangement.Center) {
            //only allow 50% of the width for the location text
            Text(
                text = currentPrayerName, modifier = Modifier
                    .padding(8.dp)
                    .weight(0.35f),
                textAlign = TextAlign.Center
            )
            //vertical divider line
            Divider(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp), color = MaterialTheme.colorScheme.outline
            )
            //only allow 50% of the width for the time text
            CounDownTimerText(
                modifier = Modifier
                    .padding(8.dp)
                    .weight(0.55f),
                state = state
            )
        }
    }
}

@Composable
fun CounDownTimerText(
    modifier: Modifier,
    state: LiveData<CountDownTime>
) {
    var countDownTime by remember { mutableStateOf(CountDownTime(0, 0, 0)) }
    LaunchedEffect(key1 = state) {
        state.observeForever {
            countDownTime = it
        }
    }
    Text(
        text = getTimerString(countDownTime.hours.toInt(), countDownTime.minutes.toInt(), countDownTime.seconds.toInt()),
        modifier = modifier,
        textAlign = TextAlign.Center
    )
}
fun getTimerString(hours: Int, minutes: Int, seconds: Int): String {
    return when {
        hours > 1 && minutes != 0 -> {
            "$hours hrs $minutes mins"
        }
        hours > 1 && minutes == 0 -> {
            "$hours hrs"
        }
        hours == 1 && minutes in 2..59 -> {
            "$hours hrs $minutes mins"
        }
        hours == 1 && minutes < 2 && minutes > 0 -> {
            "$hours hrs $minutes mins"
        }
        hours == 1 && minutes == 0 -> {
            "$hours hr"
        }
        hours == 0 && minutes in 2..59 -> {
            "$minutes mins $seconds secs"
        }
        hours == 0 && minutes < 2 && minutes > 0 -> {
            "$minutes mins $seconds seconds"
        }
        hours == 0 && minutes == 0 -> {
            "$seconds seconds"
        }
        //if there is less than 1 minute left
        else -> {
            "$seconds seconds"
        }
    }
}
