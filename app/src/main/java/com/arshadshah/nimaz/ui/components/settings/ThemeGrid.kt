package com.arshadshah.nimaz.ui.components.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.theme.NimazTheme

data class ThemeOption(
    val themeName: String,
    val themeKey: String,
    val themeColor: Color,
    var isSelected: Boolean,
    val description: String = "" // Added description field
)

@Composable
fun ThemeSelector(
    themeOptions: List<ThemeOption>,
    onThemeOptionSelected: (ThemeOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    themeOptions.forEach { themeOption ->
                        EnhancedThemeItem(
                            themeOption = themeOption,
                            onSelected = { onThemeOptionSelected(themeOption) }
                        )
                    }
                }

                // Current theme info
                themeOptions.find { it.isSelected }?.let { selectedTheme ->
                    ThemeInfo(themeOption = selectedTheme)
                }
            }
        }
    }
}

@Composable
private fun EnhancedThemeItem(
    themeOption: ThemeOption,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (themeOption.isSelected)
            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        else
            Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "border color"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .border(
                    width = 3.dp,
                    color = borderColor,
                    shape = CircleShape
                )
                .padding(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(themeOption.themeColor)
                    .clickable(onClick = onSelected),
                contentAlignment = Alignment.Center
            ) {
                if (themeOption.themeKey == "SYSTEM") {
                    Icon(
                        painter = painterResource(id = R.drawable.system_icon),
                        contentDescription = "System Theme",
                        tint = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = themeOption.themeName,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (themeOption.isSelected) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (themeOption.isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ThemeInfo(
    themeOption: ThemeOption,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Column {
                Text(
                    text = themeOption.themeName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                if (themeOption.description.isNotEmpty()) {
                    Text(
                        text = themeOption.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EnhancedThemeSelectorPreview() {
    NimazTheme {
        ThemeSelector(
            themeOptions = listOf(
                ThemeOption(
                    themeName = "Light",
                    themeKey = "LIGHT",
                    themeColor = Color(0xFFF8F9FA),
                    isSelected = true,
                    description = "Perfect for daytime use with clean, bright visuals"
                ),
                ThemeOption(
                    themeName = "Dark",
                    themeKey = "DARK",
                    themeColor = Color(0xFF202124),
                    isSelected = false,
                    description = "Easy on the eyes in low-light conditions"
                ),
                ThemeOption(
                    themeName = "System",
                    themeKey = "SYSTEM",
                    themeColor = Color(0xFF607D8B),
                    isSelected = false,
                    description = "Automatically matches your system theme"
                ),
                ThemeOption(
                    themeName = "Blue",
                    themeKey = "BLUE",
                    themeColor = Color(0xFF1976D2),
                    isSelected = false,
                    description = "Calming blue theme for a soothing experience"
                )
            ),
            onThemeOptionSelected = {}
        )
    }
}