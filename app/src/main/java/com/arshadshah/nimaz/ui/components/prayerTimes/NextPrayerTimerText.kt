package com.arshadshah.nimaz.ui.components.prayerTimes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.arshadshah.nimaz.ui.components.common.placeholder.material.PlaceholderHighlight
import com.arshadshah.nimaz.ui.components.common.placeholder.material.placeholder
import com.arshadshah.nimaz.ui.components.common.placeholder.material.shimmer

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
                // Prayer Name
                AnimatedVisibility(
                    visible = !isLoading,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = prayerNameDisplay,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.placeholder(
                            visible = isLoading,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp),
                            highlight = PlaceholderHighlight.shimmer(
                                highlightColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                }

                // Next Prayer Time
                Text(
                    text = nextPrayerTimeDisplay,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                    modifier = Modifier.placeholder(
                        visible = isLoading,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp),
                        highlight = PlaceholderHighlight.shimmer(
                            highlightColor = MaterialTheme.colorScheme.surface
                        )
                    )
                )

                // Timer Badge
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = timerText,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .placeholder(
                                visible = isLoading,
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp),
                                highlight = PlaceholderHighlight.shimmer(
                                    highlightColor = MaterialTheme.colorScheme.surface
                                )
                            )
                    )
                }
            }
        }
}

@Preview(showBackground = true)
@Composable
fun NextPrayerTimerTextPreview() {
    MaterialTheme {
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