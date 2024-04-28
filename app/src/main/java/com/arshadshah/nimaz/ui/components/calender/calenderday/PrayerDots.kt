package com.arshadshah.nimaz.ui.components.calender.calenderday

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.data.local.models.LocalPrayersTracker

@Composable
fun PrayerDots(todaysTracker: LocalPrayersTracker?) {
    Row(
        modifier = Modifier
            .padding(2.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        PrayerDot(isPrayed = todaysTracker?.fajr == true)
        PrayerDot(isPrayed = todaysTracker?.dhuhr == true)
        PrayerDot(isPrayed = todaysTracker?.asr == true)
        PrayerDot(isPrayed = todaysTracker?.maghrib == true)
        PrayerDot(isPrayed = todaysTracker?.isha == true)
    }
}