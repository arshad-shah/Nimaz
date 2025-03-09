package com.arshadshah.nimaz.ui.components.settings

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.theme.NimazTheme

data class ThemeOption(
    val themeName: String,
    val themeKey: String,
    val themeColor: Color,
    val themeTextColor: Color,
    var isSelected: Boolean,
    val description: String = "",
    @DrawableRes val icon: Int? = null,
    val supportingText: String? = null
)

@Composable
fun EnhancedThemeSelector(
    themeOptions: List<ThemeOption>,
    onThemeOptionSelected: (ThemeOption) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Theme"
) {
    var expandedCard by remember { mutableStateOf<String?>(null) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            themeOptions.forEach { themeOption ->
                ThemeListItem(
                    themeOption = themeOption,
                    isExpanded = expandedCard == themeOption.themeKey,
                    onItemClick = {
                        expandedCard = if (expandedCard == themeOption.themeKey) null
                        else themeOption.themeKey
                    },
                    onSelectClick = { onThemeOptionSelected(themeOption) }
                )
            }
        }
}

@Composable
private fun ThemeListItem(
    themeOption: ThemeOption,
    isExpanded: Boolean,
    onItemClick: () -> Unit,
    onSelectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val stripOpacity by animateFloatAsState(
        targetValue = if (themeOption.isSelected) 1f else 0.3f,
        animationSpec = tween(durationMillis = 200),
        label = "stripOpacity"
    )

    Surface(
        color = if (themeOption.isSelected)
            themeOption.themeColor.copy(alpha = 0.1f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onItemClick()
                    }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Theme Color Circle
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = themeOption.themeColor,
                    modifier = Modifier.size(56.dp)
                ) {
                }

                // Text Content
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = themeOption.themeName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (themeOption.isSelected) "Active" else "Tap to expand",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Status indicator
                if (themeOption.isSelected) {
                    Surface(
                        shape = CircleShape,
                        color = themeOption.themeColor,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.check_icon), // Replace with check icon
                            contentDescription = "Selected",
                            modifier = Modifier
                                .padding(8.dp)
                                .size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                } else {
                    // Navigation Arrow
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Expanded content with animation
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(animationSpec = tween(300)) +
                        expandVertically(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(200)) +
                        shrinkVertically(animationSpec = tween(200))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Description
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
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
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    // Select button
                    Button(
                        onClick = { onSelectClick() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !themeOption.isSelected,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themeOption.themeColor,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = if (themeOption.isSelected) "Current Theme" else "Select Theme",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = themeOption.themeTextColor
                        )
                    }
                }
            }

            // Animated color strip at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .size(4.dp)
                    .background(themeOption.themeColor.copy(alpha = stripOpacity))
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EnhancedThemeSelectorPreview() {
    NimazTheme {
        val themeOptions = listOf(
            ThemeOption(
                themeName = "Light",
                themeKey = "light",
                themeColor = Color(0xFFE0E0E0),
                themeTextColor = Color(0xFF000000),
                isSelected = false,
                description = "A light theme with a white background and dark text.",
                icon = R.drawable.rating_icon
            ),
            ThemeOption(
                themeName = "Dark",
                themeKey = "dark",
                themeColor = Color(0xFF121212),
                themeTextColor = Color(0xFFFFFFFF),
                isSelected = true,
                description = "A dark theme with a black background and light text.",
                icon = R.drawable.rating_icon
            ),
            ThemeOption(
                themeName = "System",
                themeKey = "system",
                themeColor = Color(0xFF2196F3),
                themeTextColor = Color(0xFFFFFFFF),
                isSelected = false,
                description = "A theme that follows the system settings.",
                icon = R.drawable.rating_icon
            )
        )

        EnhancedThemeSelector(
            themeOptions = themeOptions,
            onThemeOptionSelected = { }
        )
    }
}