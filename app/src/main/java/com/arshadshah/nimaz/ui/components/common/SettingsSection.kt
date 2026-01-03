package com.arshadshah.nimaz.ui.components.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.collections.forEach

@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
    items: List<SettingsSectionItem>,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            HeaderWithIcon(
                icon = icon,
                title = title,
                contentDescription = "Settings section",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )

            // Settings Items
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items.forEach { item ->
                    Surface(
                        onClick = item.onClick,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SettingsOption(
                            icon = ImageVector.vectorResource(item.icon),
                            title = item.title,
                            description = item.subtitle ?: "",
                            onClick = item.onClick,
                            modifier = Modifier,
                        )
//                        Row(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(12.dp),
//                            horizontalArrangement = Arrangement.spacedBy(12.dp),
//                            verticalAlignment = Alignment.CenterVertically
//                        ) {
//                            // Icon Container
//                            Surface(
//                                shape = RoundedCornerShape(10.dp),
//                                color = MaterialTheme.colorScheme.secondaryContainer,
//                                modifier = Modifier.size(40.dp)
//                            ) {
//                                Box(
//                                    modifier = Modifier.fillMaxSize(),
//                                    contentAlignment = Alignment.Center
//                                ) {
//                                    Icon(
//                                        painter = painterResource(id = item.icon),
//                                        contentDescription = null,
//                                        modifier = Modifier.size(20.dp),
//                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
//                                    )
//                                }
//                            }
//
//                            // Content
//                            Column(
//                                modifier = Modifier.weight(1f),
//                                verticalArrangement = Arrangement.spacedBy(2.dp)
//                            ) {
//                                Text(
//                                    text = item.title,
//                                    style = MaterialTheme.typography.bodyLarge,
//                                    color = MaterialTheme.colorScheme.onSurface
//                                )
//                                item.subtitle?.let {
//                                    Text(
//                                        text = it,
//                                        style = MaterialTheme.typography.bodySmall,
//                                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                                    )
//                                }
//                            }
//
//                            // Action or Arrow
//                            item.action?.invoke() ?: run {
//                                ArrowRight()
//                            }
//                        }
                    }
                }
            }
        }
    }
}

data class SettingsSectionItem(
    val title: String,
    val subtitle: String? = null,
    @DrawableRes val icon: Int,
    val onClick: () -> Unit,
    val action: (@Composable () -> Unit)? = null
)

@Preview
@Composable
fun SettingsSectionPreview() {
    SettingsSection(
        title = "Appearance",
        icon = ImageVector.vectorResource(id = android.R.drawable.ic_menu_manage),
        items = listOf(
            SettingsSectionItem(
                title = "Theme",
                subtitle = "Light, Dark, or System Default",
                icon = android.R.drawable.ic_menu_day,
                onClick = {}
            ),
            SettingsSectionItem(
                title = "Font Size",
                subtitle = "Adjust the font size",
                icon = android.R.drawable.ic_menu_zoom,
                onClick = {}
            )
        ),
        modifier = Modifier.padding(16.dp)
    )
}