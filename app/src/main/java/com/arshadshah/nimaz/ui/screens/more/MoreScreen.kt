package com.arshadshah.nimaz.ui.screens.more

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.navigation.BottomNavigationBar

@Composable
fun MoreScreen(
    onNavigateToTasbihScreen: (String, String, String, String) -> Unit,
    onNavigateToNames: () -> Unit,
    onNavigateToListOfTasbeeh: () -> Unit,
    onNavigateToShadah: () -> Unit,
    onNavigateToZakat: () -> Unit,
    onNavigateToPrayerTracker: () -> Unit,
    onNavigateToCalender: () -> Unit,
    onNavigateToQibla: () -> Unit,
    onNavigateToTasbihListScreen: () -> Unit,
    onNavigateToHadithShelf: () -> Unit,
    navController: NavHostController,
) {
    val features = listOf(
        FeatureGroup(
            title = "Worship",
            features = listOf(
                FeatureItem(
                    title = "Tasbih",
                    description = "Digital counter for dhikr",
                    icon = R.drawable.counter_icon,
                    onClick = { onNavigateToTasbihScreen(" ", " ", " ", " ") }
                ),
                FeatureItem(
                    title = "Qibla",
                    description = "Find prayer direction",
                    icon = R.drawable.qibla,
                    onClick = onNavigateToQibla
                )
            )
        ),
        FeatureGroup(
            title = "Knowledge",
            features = listOf(
                FeatureItem(
                    title = "Names of Allah",
                    description = "Learn the 99 names",
                    icon = R.drawable.names_of_allah,
                    onClick = onNavigateToNames
                ),
                FeatureItem(
                    title = "Duas",
                    description = "Daily supplications",
                    icon = R.drawable.dua,
                    onClick = onNavigateToListOfTasbeeh
                ),
                FeatureItem(
                    title = "Hadith Shelf",
                    description = "Collection of hadiths",
                    icon = R.drawable.bookshelf_icon,
                    onClick = onNavigateToHadithShelf
                )
            )
        ),
        FeatureGroup(
            title = "Tools",
            features = listOf(
                FeatureItem(
                    title = "Trackers",
                    description = "Monitor your prayers",
                    icon = R.drawable.tracker_icon,
                    onClick = onNavigateToPrayerTracker
                ),
                FeatureItem(
                    title = "Calendar",
                    description = "Islamic calendar",
                    icon = R.drawable.calendar_icon,
                    onClick = onNavigateToCalender
                ),
                FeatureItem(
                    title = "Shahadah",
                    description = "Declaration of faith",
                    icon = R.drawable.shahadah,
                    onClick = onNavigateToShadah
                )
            )
        )
    )

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController)
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(AppConstants.TEST_TAG_MORE),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                features.forEach { group ->
                    item {
                        FeatureGroupCard(group)
                    }
                }
            }
        }
    }
}

private data class FeatureGroup(
    val title: String,
    val features: List<FeatureItem>
)

private data class FeatureItem(
    val title: String,
    val description: String,
    val icon: Int,
    val onClick: () -> Unit
)

@Composable
private fun FeatureGroupCard(
    group: FeatureGroup,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Section
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = group.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Features List
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                group.features.forEach { feature ->
                    FeatureItem(feature)
                }
            }
        }
    }
}

@Composable
private fun FeatureItem(
    feature: FeatureItem,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = feature.onClick)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Container
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Image(
                    painter = painterResource(feature.icon),
                    contentDescription = feature.title,
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxSize()
                )
            }

            // Text Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = feature.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = feature.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Navigation Arrow
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.angle_small_right_icon),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(16.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}