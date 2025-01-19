package com.arshadshah.nimaz.ui.components.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter

@Composable
fun CompactLocationTopBar(
    locationName: String,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        shape = RoundedCornerShape(16.dp),
        // Increased elevation for better depth perception
        tonalElevation = 6.dp,
        // Using surface container highest for better contrast with content
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp), // Increased padding for better spacing
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Location Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp), // Increased spacing
                modifier = Modifier.weight(1f)
            ) {
                // Location Icon Container with higher contrast background
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    // Using primary for better contrast
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp) // Increased size for better visibility
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            // Using onPrimary for guaranteed contrast
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Location Info
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AnimatedContent(
                        targetState = if (isLoading) "Loading..." else locationName,
                        transitionSpec = {
                            (slideInVertically { height -> height } + fadeIn())
                                .togetherWith(slideOutVertically { height -> -height } + fadeOut())
                        },
                        label = "location"
                    ) { text ->
                        Text(
                            text = text,
                            // Using larger text style for better readability
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            // Using on surface for maximum contrast
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Date Chip with improved contrast
            Surface(
                // Using secondary container for better distinction
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.current_date_icon),
                        contentDescription = "Calendar",
                        // Using onSecondaryContainer for guaranteed contrast
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(16.dp)
                    )

                    val currentDate = remember {
                        LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM"))
                    }

                    val hijriDate = remember {
                        HijrahDate.from(LocalDate.now())
                            .format(DateTimeFormatter.ofPattern("dd MMM"))
                    }

                    Text(
                        text = "$currentDate • $hijriDate",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        // Using onSecondaryContainer for guaranteed contrast
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CompactLocationTopBarPreview() {
    MaterialTheme {
        CompactLocationTopBar(
            locationName = "London, United Kingdom",
            isLoading = false
        )
    }
}