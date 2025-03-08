package com.arshadshah.nimaz.ui.components.settings


import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.components.common.HeaderWithIcon
import com.arshadshah.nimaz.ui.theme.NimazTheme


@Composable
fun SettingsAppearanceSection(
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    currentTheme: String,
    themeOptions: List<ThemeOption>,
    onThemeSelect: (ThemeOption) -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
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
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeaderWithIcon(
                title = "Appearance",
                icon = Icons.Default.ColorLens,
                contentDescription = "Theme Icon",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )

            // Dark Mode Section
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(56.dp)
                        ) {
                            AnimatedContent(
                                targetState = isDarkMode,
                                label = "theme_icon",
                                transitionSpec = {
                                    fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut()
                                }
                            ) { dark ->
                                Icon(
                                    painter = painterResource(
                                        id = if (dark) R.drawable.dark_icon else R.drawable.light_icon
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxSize(),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (isDarkMode) "Dark Mode" else "Light Mode",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isDarkMode) "Optimized for low light" else "Classic bright theme",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = onDarkModeChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }

            // Theme Selection Section
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CompactThemeSelector(
                        themeOptions = themeOptions,
                        onThemeOptionSelected = onThemeSelect
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun SettingsAppearanceSectionPreview() {
    NimazTheme {
        SettingsAppearanceSection(
            isDarkMode = true,
            onDarkModeChange = {},
            currentTheme = "SYSTEM",
            themeOptions = listOf(
                ThemeOption(
                    themeName = "Light",
                    themeKey = "LIGHT",
                    themeColor = Color(0xFFF8F9FA),
                    isSelected = false
                ),
                ThemeOption(
                    themeName = "Dark",
                    themeKey = "DARK",
                    themeColor = Color(0xFF202124),
                    isSelected = true
                ),
                ThemeOption(
                    themeName = "System",
                    themeKey = "SYSTEM",
                    themeColor = Color(0xFF607D8B),
                    isSelected = false
                )
            ),
            onThemeSelect = {}
        )
    }
}