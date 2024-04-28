package com.arshadshah.nimaz.ui.components.prayerTimes


import android.content.Context
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_ASR
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_DHUHR
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_FAJR
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_ISHA
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_MAGHRIB
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_SUNRISE
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
import kotlin.reflect.KFunction2


@Composable
fun PrayerTimesList(
    prayerTimesState: State<PrayerTimesViewModel.PrayerTimesState>,
    error: State<String?>,
    isLoading: State<Boolean>,
    handleEvents: KFunction2<Context, PrayerTimesViewModel.PrayerTimesEvent, Unit>
) {

    val context = LocalContext.current

    val currentPrayerName = prayerTimesState.value.currentPrayerName

    val fajrTime = prayerTimesState.value.fajrTime

    val sunriseTime = prayerTimesState.value.sunriseTime

    val dhuhrTime = prayerTimesState.value.dhuhrTime

    val asrTime = prayerTimesState.value.asrTime

    val maghribTime = prayerTimesState.value.maghribTime

    val ishaTime = prayerTimesState.value.ishaTime

    val nextPrayerName = prayerTimesState.value.nextPrayerName

    val nextPrayerTime = prayerTimesState.value.nextPrayerTime

    val timer = prayerTimesState.value.countDownTime

    val mapOfPrayerTimes = mapOf(
        PRAYER_NAME_FAJR to fajrTime,
        PRAYER_NAME_SUNRISE to sunriseTime,
        PRAYER_NAME_DHUHR to dhuhrTime,
        PRAYER_NAME_ASR to asrTime,
        PRAYER_NAME_MAGHRIB to maghribTime,
        PRAYER_NAME_ISHA to ishaTime,
    )

    if (error.value?.isNotBlank() == true) {
        Toasty.error(context, error.value!!).show()
    } else if (isLoading.value) {
        PrayerTimesListUI(
            prayerTimesMap = mapOfPrayerTimes,
            loading = true,
            currentPrayerName = currentPrayerName,
        )
    } else {
        val timeToNextPrayerLong =
            nextPrayerTime.atZone(java.time.ZoneId.systemDefault())
                ?.toInstant()
                ?.toEpochMilli()
        val currentTime =
            LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toInstant()
                .toEpochMilli()

        val difference = timeToNextPrayerLong?.minus(currentTime)
        handleEvents(
            context,
            PrayerTimesViewModel.PrayerTimesEvent.Start(difference!!)
        )
        PrayerTimesListUI(
            loading = false,
            currentPrayerName = currentPrayerName,
            prayerTimesMap = mapOfPrayerTimes,
        )
    }

}

@Composable
fun PrayerTimesListUI(
    prayerTimesMap: Map<String, LocalDateTime?>,
    loading: Boolean,
    currentPrayerName: String,
) {
    val today = LocalDate.now()
    val todayHijri = HijrahDate.from(today)
    val ramadanStart = HijrahDate.of(todayHijri[ChronoField.YEAR], 9, 1)
    val ramadanEnd = HijrahDate.of(todayHijri[ChronoField.YEAR], 9, 29)
    val isRamadan = todayHijri.isAfter(ramadanStart) && todayHijri.isBefore(ramadanEnd)
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        //iterate over the map
        for ((key, value) in prayerTimesMap) {
            val radii = calculatePrayerTimeRadii(
                index = prayerTimesMap.keys.indexOf(key),
                totalItems = prayerTimesMap.size,
                orientation = Orientation.Vertical,
            )
            PrayerTimesRow(
                prayerName = key,
                prayerTime = value,
                isHighlighted = currentPrayerName == key,
                loading = loading,
                isRamadan = isRamadan,
                radii = radii,
            )
        }
    }
}

data class PrayerTimeRadii(
    val topLeft: Float,
    val topRight: Float,
    val bottomLeft: Float,
    val bottomRight: Float,
)

fun calculatePrayerTimeRadii(
    index: Int,
    totalItems: Int,
    orientation: Orientation,
): PrayerTimeRadii {
    val isSingleItem = totalItems == 1
    val isLastItem = index == totalItems - 1
    val isFirstItem = index == 0

    return when (orientation) {
        Orientation.Horizontal -> {
            if (isSingleItem) {
                PrayerTimeRadii(4f, 4f, 4f, 4f)
            } else if (isFirstItem) {
                PrayerTimeRadii(16f, 16f, 4f, 4f)
            } else if (isLastItem) {
                PrayerTimeRadii(4f, 4f, 16f, 16f)
            } else {
                PrayerTimeRadii(4f, 4f, 4f, 4f)
            }
        }

        Orientation.Vertical -> {
            if (isSingleItem) {
                PrayerTimeRadii(4f, 4f, 4f, 4f)
            } else if (isFirstItem) {
                PrayerTimeRadii(16f, 16f, 4f, 4f)
            } else if (isLastItem) {
                PrayerTimeRadii(4f, 4f, 16f, 16f)
            } else {
                PrayerTimeRadii(4f, 4f, 4f, 4f)
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
    isRamadan: Boolean,
    radii: PrayerTimeRadii,
) {
    //format the date to time based on device format
    //get the device trime format
    val deviceTimeFormat = DateFormat.is24HourFormat(LocalContext.current)
    //if the device time format is 24 hour then use the 24 hour format
    val formatter = if (deviceTimeFormat) {
        DateTimeFormatter.ofPattern("HH:mm")
    } else {
        DateTimeFormatter.ofPattern("hh:mm a")
    }
    val sentenceCase =
        prayerName.lowercase(Locale.ROOT)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

    val isBold = when {
        isRamadan && prayerName == PRAYER_NAME_FAJR -> true
        isRamadan && prayerName == PRAYER_NAME_MAGHRIB -> true
        else -> false
    }
    Card(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        shape = RoundedCornerShape(
            topStart = radii.topLeft.dp,
            topEnd = radii.topRight.dp,
            bottomStart = radii.bottomLeft.dp,
            bottomEnd = radii.bottomRight.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp, horizontal = 8.dp)
    ) {
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
            } else if (isBold) {
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f))
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
                fontWeight = if (isBold) FontWeight.ExtraBold else MaterialTheme.typography.titleLarge.fontWeight
            )
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
                fontWeight = if (isBold) FontWeight.ExtraBold else MaterialTheme.typography.titleLarge.fontWeight
            )
        }
    }
}