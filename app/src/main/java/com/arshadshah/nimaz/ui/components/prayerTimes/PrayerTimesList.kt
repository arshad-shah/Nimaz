package com.arshadshah.nimaz.ui.components.prayerTimes


import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.components.common.placeholder.material.PlaceholderHighlight
import com.arshadshah.nimaz.ui.components.common.placeholder.material.placeholder
import com.arshadshah.nimaz.ui.components.common.placeholder.material.shimmer
import com.arshadshah.nimaz.viewModel.PrayerTimesViewModel
import es.dmoral.toasty.Toasty
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.util.Locale


@Composable
fun PrayerTimesList() {
    val context = LocalContext.current

    val viewModel = viewModel(
        key = AppConstants.PRAYER_TIMES_VIEWMODEL_KEY,
        initializer = { PrayerTimesViewModel() },
        viewModelStoreOwner = LocalContext.current as ComponentActivity
    )

    val currentPrayerName = remember {
        viewModel.currentPrayerName
    }.collectAsState()

    val fajrTime = remember {
        viewModel.fajrTime
    }.collectAsState()

    val sunriseTime = remember {
        viewModel.sunriseTime
    }.collectAsState()

    val dhuhrTime = remember {
        viewModel.dhuhrTime
    }.collectAsState()

    val asrTime = remember {
        viewModel.asrTime
    }.collectAsState()

    val maghribTime = remember {
        viewModel.maghribTime
    }.collectAsState()

    val ishaTime = remember {
        viewModel.ishaTime
    }.collectAsState()

    val isLoading = remember {
        viewModel.isLoading
    }.collectAsState()

    val isError = remember {
        viewModel.error
    }.collectAsState()

    val nextPrayerName = remember {
        viewModel.nextPrayerName
    }.collectAsState()

    val nextPrayerTime = remember {
        viewModel.nextPrayerTime
    }.collectAsState()

    if (isError.value.isNotBlank()) {
        Toasty.error(context, isError.value).show()
    } else if (isLoading.value) {
        PrayerTimesListUI(
            prayerTimesMap = mapOf(
                "Fajr" to fajrTime.value,
                "Sunrise" to sunriseTime.value,
                "Dhuhr" to dhuhrTime.value,
                "Asr" to asrTime.value,
                "Maghrib" to maghribTime.value,
                "Isha" to ishaTime.value,
            ),
            name = nextPrayerName.value,
            loading = true,
            currentPrayerName = currentPrayerName.value,
        )
    } else {
        val timeToNextPrayerLong =
            nextPrayerTime.value.atZone(java.time.ZoneId.systemDefault())
                ?.toInstant()
                ?.toEpochMilli()
        val currentTime =
            LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toInstant()
                .toEpochMilli()

        val difference = timeToNextPrayerLong?.minus(currentTime)
        viewModel.handleEvent(
            LocalContext.current,
            PrayerTimesViewModel.PrayerTimesEvent.Start(difference!!)
        )

        val mapOfPrayerTimes = mapOf(
            "Fajr" to fajrTime.value!!,
            "Sunrise" to sunriseTime.value!!,
            "Dhuhr" to dhuhrTime.value!!,
            "Asr" to asrTime.value!!,
            "Maghrib" to maghribTime.value!!,
            "Isha" to ishaTime.value!!,
        )
        PrayerTimesListUI(
            loading = false,
            currentPrayerName = currentPrayerName.value.first()
                .uppercaseChar() + currentPrayerName.value.substring(1),
            name = nextPrayerName.value.first()
                .uppercaseChar() + nextPrayerName.value.substring(1),
            prayerTimesMap = mapOfPrayerTimes,
        )
    }

}

@Composable
fun PrayerTimesListUI(
    prayerTimesMap: Map<String, LocalDateTime?>,
    loading: Boolean,
    currentPrayerName: String,
    name: String,
) {
    val today = LocalDate.now()
    val todayHijri = HijrahDate.from(today)
    val ramadanStart = HijrahDate.of(todayHijri[ChronoField.YEAR], 9, 1)
    val ramadanEnd = HijrahDate.of(todayHijri[ChronoField.YEAR], 9, 29)
    val isRamadan = todayHijri.isAfter(ramadanStart) && todayHijri.isBefore(ramadanEnd)
    Card(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
        ),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 8.dp)
    ) {
        Column(
            modifier = Modifier.scrollable(
                orientation = Orientation.Vertical,
                enabled = true,
                state = rememberScrollState()
            )
        ) {
            //iterate over the map
            for ((key, value) in prayerTimesMap) {
                //if the element is first then dont add a divider else add a divider on top
                if (key != prayerTimesMap.keys.first()) {
                    Divider(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                    )
                }
                val isBoldText = if (isRamadan) {
                    key == "Fajr" || key == "Maghrib"
                } else {
                    false
                }
                PrayerTimesRow(
                    prayerName = key,
                    prayerTime = value,
                    isHighlighted = currentPrayerName == key,
                    shouldShowTimer = name == key,
                    loading = loading,
                    isBoldText = isBoldText,
                )
            }
        }
    }
}

//the row for the prayer times
@Composable
fun PrayerTimesRow(
    prayerName: String,
    prayerTime: LocalDateTime?,
    isHighlighted: Boolean,
    loading: Boolean,
    isBoldText: Boolean,
    shouldShowTimer: Boolean,
) {
    val viewModel = viewModel(
        key = AppConstants.PRAYER_TIMES_VIEWMODEL_KEY,
        initializer = { PrayerTimesViewModel() },
        viewModelStoreOwner = LocalContext.current as ComponentActivity
    )
    val countDownTime = remember { viewModel.timer }.collectAsState()
    //format the date to time based on device format
    //get the device trime format
    val deviceTimeFormat = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
    //if the device time format is 24 hour then use the 24 hour format
    val formatter = if (deviceTimeFormat) {
        DateTimeFormatter.ofPattern("HH:mm")
    } else {
        DateTimeFormatter.ofPattern("hh:mm a")
    }
    val sentenceCase =
        prayerName.lowercase(Locale.ROOT)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = if (isHighlighted) {
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clip(
                    RoundedCornerShape(
                        topStart = 8.dp,
                        topEnd = 8.dp,
                        bottomStart = 8.dp,
                        bottomEnd = 8.dp
                    )
                )
        } else {
            Modifier
                .fillMaxWidth()
        }
    ) {
        Text(
            text = sentenceCase,
            modifier = Modifier
                .padding(16.dp)
                .placeholder(
                    visible = loading,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(4.dp),
                    highlight = PlaceholderHighlight.shimmer(
                        highlightColor = Color.White,
                    )
                ),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = if (isBoldText) FontWeight.ExtraBold else MaterialTheme.typography.titleLarge.fontWeight
        )
        if (shouldShowTimer) {
            Text(
                modifier = Modifier
                    .padding(16.dp)
                    .placeholder(
                        visible = loading,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(4.dp),
                        highlight = PlaceholderHighlight.shimmer(
                            highlightColor = Color.White,
                        )
                    ),
                text = " -${countDownTime.value.hours} : ${countDownTime.value.minutes} : ${countDownTime.value.seconds}",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleSmall
            )
        }
        Text(
            text = prayerTime!!.format(formatter),
            modifier = Modifier
                .padding(16.dp)
                .placeholder(
                    visible = loading,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(4.dp),
                    highlight = PlaceholderHighlight.shimmer(
                        highlightColor = Color.White,
                    )
                ),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = if (isBoldText) FontWeight.ExtraBold else MaterialTheme.typography.titleLarge.fontWeight
        )
    }
}