package com.arshadshah.nimaz.ui.components.prayerTimes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.components.common.placeholder.material.PlaceholderHighlight
import com.arshadshah.nimaz.ui.components.common.placeholder.material.placeholder
import com.arshadshah.nimaz.ui.components.common.placeholder.material.shimmer

@Composable
fun NextPrayerTimerText(
    modifier: Modifier = Modifier,
    prayerNameDisplay: String,
    nextPrayerTimeDisplay: String,
    timerText: String,
    isLoading: Boolean = false,
    horizontalPosition: Alignment.Horizontal = Alignment.CenterHorizontally
) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = horizontalPosition
    ) {
        Text(
            modifier = Modifier.placeholder(
                visible = isLoading,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(4.dp),
                highlight = PlaceholderHighlight.shimmer(
                    highlightColor = Color.White,
                )
            ),
            text = prayerNameDisplay,
            style = MaterialTheme.typography.displaySmall
        )
        Text(
            modifier = Modifier.placeholder(
                visible = isLoading,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(4.dp),
                highlight = PlaceholderHighlight.shimmer(
                    highlightColor = Color.White,
                )
            ),
            text = nextPrayerTimeDisplay,
            style = MaterialTheme.typography.titleLarge
        )
        Badge(
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ){
            Text(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .placeholder(
                        visible = isLoading,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(4.dp),
                        highlight = PlaceholderHighlight.shimmer(
                            highlightColor = Color.White,
                        )
                    ),
                text = timerText,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}