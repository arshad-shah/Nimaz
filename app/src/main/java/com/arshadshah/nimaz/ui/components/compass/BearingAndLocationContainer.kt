package com.arshadshah.nimaz.ui.components.compass

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.components.common.CustomText
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import es.dmoral.toasty.Toasty
import kotlin.math.roundToInt

@Composable
fun BearingAndLocationContainer(
    state: State<Double>,
    isLoading: State<Boolean>,
    errorMessage: State<String>,
) {

    //get the context
    val context = LocalContext.current
    //get the shared preferences
    val sharedPref = PrivateSharedPreferences(context)

    if (isLoading.value) {
        //show a loading indicator
        BearingAndLocationContainerUI(location = "Loading...", heading = "Loading...")
    } else if (errorMessage.value != "") {
        BearingAndLocationContainerUI(location = "Error", heading = "Error")
        Toasty.error(LocalContext.current, errorMessage.value).show()
    } else {
        //get the location
        val location = sharedPref.getData(AppConstants.LOCATION_INPUT, "")
        //round the bearing to 2 decimal places
        val bearing = state.value.roundToInt()
        val compassDirection = bearingToCompassDirection(state.value.toFloat())
        val heading = "$bearing° $compassDirection"
        Log.d(AppConstants.QIBLA_COMPASS_SCREEN_TAG, "BearingAndLocationContainer: $heading")
        //show the bearing and location
        BearingAndLocationContainerUI(location, heading)
    }
}


//a function that turns a bearing into actual compass direction with a heading
fun bearingToCompassDirection(bearing: Float): String {
    val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW", "N")
    val index = ((bearing + 22.5f) / 45f).toInt()
    return directions[index % 8]
}


@Composable
fun BearingAndLocationContainerUI(location: String, heading: String) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
        ),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier
            .padding(8.dp)
            .height(IntrinsicSize.Max)
    ) {
        //align items to center

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            //only allow 50% of the width for the location text
            CustomText(
                modifier = Modifier
                    .weight(0.5f)
                    .padding(8.dp),
                heading = "Location", text = location
            )
            //vertical divider line
            Divider(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp),
                color = MaterialTheme.colorScheme.outline
            )
            CustomText(
                modifier = Modifier
                    .weight(0.5f)
                    .padding(8.dp),
                heading = "Heading", text = heading
            )
        }
    }
}
