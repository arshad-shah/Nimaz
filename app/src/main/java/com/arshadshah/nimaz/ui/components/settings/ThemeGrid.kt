package com.arshadshah.nimaz.ui.components.settings

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.components.common.HeaderWithIcon
import com.arshadshah.nimaz.ui.theme.NimazTheme

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
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        HeaderWithIcon(
            title = "Choose your theme",
            icon = Icons.Default.Colorize,
            contentDescription = "Theme selection section",
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
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
        modifier = modifier
            .border(
                width = 2.dp,
                color = if (themeOption.isSelected)
                    themeOption.themeColor.copy(alpha = 0.5f)
                else Color.Transparent,
                shape = MaterialTheme.shapes.small
            )
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (themeOption.isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.small,
        onClick = onCardClick
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .border(
                            width = 2.dp,
                            color = themeOption.themeColor.copy(alpha = 0.1f),
                            shape = CircleShape
                        ),
                    shape = CircleShape,
                    color = themeOption.themeColor
                ) {
                    themeOption.icon?.let { iconRes ->
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxSize()
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = themeOption.themeName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = if (themeOption.isSelected)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface
                    )

                    if (!isExpanded) {
                        Surface(
                            color = if (themeOption.isSelected)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (themeOption.isSelected) "Active" else "Tap to expand",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (themeOption.isSelected)
                                    MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            if (isExpanded) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = themeOption.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        themeOption.supportingText?.let { supportingText ->
                            Text(
                                text = supportingText,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Button(
                    onClick = onSelectClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !themeOption.isSelected,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    Text(
                        text = if (themeOption.isSelected) "Current Theme" else "Select Theme",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CompactThemeSelectorPreview() {
    NimazTheme {
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