package com.arshadshah.nimaz.ui.components.settings

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R

data class ThemeOption(
    val themeName: String,
    val themeKey: String,
    val themeColor: Color,
    var isSelected: Boolean,
    val description: String = "",
    @DrawableRes val icon: Int? = null,
    val supportingText: String? = null // Added for additional context
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompactThemeSelector(
    themeOptions: List<ThemeOption>,
    onThemeOptionSelected: (ThemeOption) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedCard by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Choose your theme",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 2
        ) {
            themeOptions.forEach { themeOption ->
                CompactThemeCard(
                    themeOption = themeOption,
                    isExpanded = expandedCard == themeOption.themeKey,
                    onCardClick = {
                        expandedCard = if (expandedCard == themeOption.themeKey) null
                        else themeOption.themeKey
                    },
                    onSelectClick = { onThemeOptionSelected(themeOption) },
                    modifier = Modifier.weight(1f, fill = true)
                )
            }
        }
    }
}

@Composable
private fun CompactThemeCard(
    themeOption: ThemeOption,
    isExpanded: Boolean,
    onCardClick: () -> Unit,
    onSelectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.animateContentSize(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (themeOption.isSelected)
                MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        onClick = onCardClick
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Theme Preview Circle
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(themeOption.themeColor)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    themeOption.icon?.let { iconRes ->
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = themeOption.themeName,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (themeOption.isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            if (isExpanded) {
                Text(
                    text = themeOption.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                themeOption.supportingText?.let { supportingText ->
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalButton(
                    onClick = onSelectClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !themeOption.isSelected,
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Text(
                        text = if (themeOption.isSelected) "Current" else "Select",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CompactThemeSelectorPreview() {
    MaterialTheme {
        CompactThemeSelector(
            themeOptions = listOf(
                ThemeOption(
                    themeName = "Light",
                    themeKey = "LIGHT",
                    themeColor = Color(0xFFF8F9FA),
                    isSelected = true,
                    description = "Perfect for daytime use",
                    supportingText = "Optimized for readability"
                ),
                ThemeOption(
                    themeName = "Dark",
                    themeKey = "DARK",
                    themeColor = Color(0xFF202124),
                    isSelected = false,
                    description = "Easy on the eyes",
                    supportingText = "Reduces eye strain"
                ),
                ThemeOption(
                    themeName = "System",
                    themeKey = "SYSTEM",
                    themeColor = Color(0xFF607D8B),
                    isSelected = false,
                    description = "Matches system theme",
                    icon = R.drawable.system_icon
                ),
                ThemeOption(
                    themeName = "Blue",
                    themeKey = "BLUE",
                    themeColor = Color(0xFF1976D2),
                    isSelected = false,
                    description = "Calming blue theme"
                )
            ),
            onThemeOptionSelected = {}
        )
    }
}