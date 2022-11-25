package com.arshadshah.nimaz.ui.components.ui.prayerTimes

import android.text.format.DateUtils
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.theme.NimazTheme
import kotlinx.coroutines.delay

@Composable
fun LocationTimeContainerUI(location: String) {
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
                text = location,
                modifier = Modifier
                    .padding(8.dp)
                    .weight(0.45f),
                textAlign = TextAlign.Center
            )
            //vertical divider line
            Divider(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp), color = MaterialTheme.colorScheme.outline
            )
            //only allow 50% of the width for the time text
            TimeText(
                modifier = Modifier
                    .padding(8.dp)
                    .weight(0.45f)
            )
        }
    }
}


@Composable
//TimeText with seconds
fun TimeText(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val (time, setTime) = remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(true) {
        while (true) {
            setTime(System.currentTimeMillis())
            delay(DateUtils.SECOND_IN_MILLIS)
        }
    }
    Text(
        modifier = modifier,
        text = DateUtils.formatDateTime(context, time, DateUtils.FORMAT_SHOW_TIME),
        textAlign = TextAlign.Center
    )
}


@Preview
@Composable
fun LocationTimeContainerUIPreview() {
    NimazTheme {
        LocationTimeContainerUI("London")
    }
}
