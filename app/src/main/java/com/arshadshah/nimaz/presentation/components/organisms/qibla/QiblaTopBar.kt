package com.arshadshah.nimaz.presentation.components.organisms.qibla

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.qibla.QiblaLocationLabel
import com.arshadshah.nimaz.presentation.components.organisms.NimazPillTabs
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Qibla top bar — intentionally identical across the Compass and AR modes so
 * switching modes never shifts the chrome. Shows the active location (name +
 * coordinates) on the left and the Compass / AR mode toggle on the right. There
 * is no back button or screen title: the screen is reached directly from the
 * bottom navigation.
 */
@Composable
fun QiblaTopBar(
    locationName: String?,
    latitude: Double?,
    longitude: Double?,
    fallbackTitle: String,
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QiblaLocationLabel(
                    locationName = locationName,
                    latitude = latitude,
                    longitude = longitude,
                    fallbackTitle = fallbackTitle,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                NimazPillTabs(
                    tabs = tabs,
                    selectedIndex = selectedIndex,
                    onTabSelect = onTabSelect
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Qibla Top Bar")
@Composable
private fun QiblaTopBarPreview() {
    NimazTheme {
        QiblaTopBar(
            locationName = "London, UK",
            latitude = 51.5074,
            longitude = -0.1278,
            fallbackTitle = "Qibla Compass",
            tabs = listOf("Compass", "AR"),
            selectedIndex = 0,
            onTabSelect = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Qibla Top Bar - AR selected")
@Composable
private fun QiblaTopBarArPreview() {
    NimazTheme {
        QiblaTopBar(
            locationName = "Jakarta, Indonesia",
            latitude = -6.2088,
            longitude = 106.8456,
            fallbackTitle = "Qibla Compass",
            tabs = listOf("Compass", "AR"),
            selectedIndex = 1,
            onTabSelect = {}
        )
    }
}
