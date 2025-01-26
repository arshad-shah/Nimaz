package com.arshadshah.nimaz.ui.components.prayerTimes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.components.common.placeholder.material.PlaceholderHighlight
import com.arshadshah.nimaz.ui.components.common.placeholder.material.placeholder
import com.arshadshah.nimaz.ui.components.common.placeholder.material.shimmer
import com.arshadshah.nimaz.ui.theme.NimazTheme

@Composable
fun NextPrayerTimerText(
    prayerNameDisplay: String,
    nextPrayerTimeDisplay: String,
    timerText: String,
    isLoading: Boolean = false,
    horizontalPosition: Alignment.Horizontal = Alignment.CenterHorizontally
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(6.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = horizontalPosition,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = prayerNameDisplay,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.placeholder(
                    visible = isLoading,
                    highlight = PlaceholderHighlight.shimmer()
                )
            )

            Text(
                text = nextPrayerTimeDisplay,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                modifier = Modifier.placeholder(
                    visible = isLoading,
                    highlight = PlaceholderHighlight.shimmer()
                )
            )

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                shape = MaterialTheme.shapes.large,
            ) {
                Text(
                    text = timerText,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .placeholder(
                            visible = isLoading,
                            highlight = PlaceholderHighlight.shimmer()
                        )
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NextPrayerTimerTextPreview() {
    NimazTheme {
        Column {
            NextPrayerTimerText(
                prayerNameDisplay = "Asr",
                nextPrayerTimeDisplay = "3:30 PM",
                timerText = "2h 15m remaining",
                isLoading = false
            )
        }
    }
}