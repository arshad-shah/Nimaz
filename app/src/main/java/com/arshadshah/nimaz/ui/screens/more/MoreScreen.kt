package com.arshadshah.nimaz.ui.screens.more

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
        FeatureItem(
            title = "Tasbih",
            icon = R.drawable.counter_icon,
            color = MaterialTheme.colorScheme.primaryContainer,
            onClick = { onNavigateToTasbihScreen(" ", " ", " ", " ") }
        ),
        FeatureItem(
            title = "Tasbih List",
            icon = R.drawable.tasbih,
            color = MaterialTheme.colorScheme.secondaryContainer,
            onClick = onNavigateToTasbihListScreen
        ),
        FeatureItem(
            title = "Qibla",
            icon = R.drawable.qibla,
            color = MaterialTheme.colorScheme.tertiaryContainer,
            onClick = onNavigateToQibla
        ),
        FeatureItem(
            title = "Names of Allah",
            icon = R.drawable.names_of_allah,
            color = MaterialTheme.colorScheme.primaryContainer,
            onClick = onNavigateToNames
        ),
        FeatureItem(
            title = "Duas",
            icon = R.drawable.dua,
            color = MaterialTheme.colorScheme.secondaryContainer,
            onClick = onNavigateToListOfTasbeeh
        ),
        FeatureItem(
            title = "Hadith Shelf",
            icon = R.drawable.bookshelf_icon,
            color = MaterialTheme.colorScheme.tertiaryContainer,
            onClick = onNavigateToHadithShelf
        ),
        FeatureItem(
            title = "Trackers",
            icon = R.drawable.tracker_icon,
            color = MaterialTheme.colorScheme.primaryContainer,
            onClick = onNavigateToPrayerTracker
        ),
        FeatureItem(
            title = "Calendar",
            icon = R.drawable.calendar_icon,
            color = MaterialTheme.colorScheme.secondaryContainer,
            onClick = onNavigateToCalender
        ),
        FeatureItem(
            title = "Shahadah",
            icon = R.drawable.shahadah,
            color = MaterialTheme.colorScheme.tertiaryContainer,
            onClick = onNavigateToShadah
        )
    )
    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                navController
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(it)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(AppConstants.TEST_TAG_MORE)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(features.size) { index ->
                    FeatureCard(features[index])
                }
            }
        }
    }
}

private data class FeatureItem(
    val title: String,
    val icon: Int,
    val color: Color,
    val onClick: () -> Unit
)

@Composable
private fun FeatureCard(
    feature: FeatureItem,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = feature.color,
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(
                onClick = feature.onClick
            ),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                modifier = Modifier.size(56.dp)
            ) {
                Image(
                    painter = painterResource(feature.icon),
                    contentDescription = feature.title,
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxSize(),
                )
            }

            Text(
                text = feature.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Icon(
                painter = painterResource(R.drawable.angle_small_right_icon),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}