package com.arshadshah.nimaz.ui.components.settings

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Theme option data class.
 */
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

/**
 * Enhanced theme selector with expandable cards.
 *
 * Design System Alignment:
 * - Surface with surfaceVariant @ 0.5 alpha (or theme tinted when selected)
 * - 16dp corners
 * - 12dp padding
 * - Icon container: 44dp with 10dp corners
 * - Status indicator: 28dp with 8dp corners
 */
@Composable
fun EnhancedThemeSelector(
    themeOptions: List<ThemeOption>,
    onThemeOptionSelected: (ThemeOption) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedCard by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
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

/**
 * Individual theme list item with expandable details.
 */
@Composable
private fun ThemeListItem(
    themeOption: ThemeOption,
    isExpanded: Boolean,
    onItemClick: () -> Unit,
    onSelectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        color = if (themeOption.isSelected) {
            themeOption.themeColor.copy(alpha = 0.15f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onItemClick()
            }
    ) {
        Column {
            // Main Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Theme Color Container
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = themeOption.themeColor,
                    modifier = Modifier.size(44.dp)
                ) {
                    // Empty - just shows the color
                }

                // Text Content
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = themeOption.themeName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (themeOption.isSelected) "Active theme" else "Tap to preview",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Status Indicator
                AnimatedContent(
                    targetState = themeOption.isSelected,
                    transitionSpec = {
                        fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut()
                    },
                    label = "status_indicator"
                ) { isSelected ->
                    if (isSelected) {
                        // Checkmark for selected
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = themeOption.themeColor,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Selected",
                                    modifier = Modifier.size(16.dp),
                                    tint = themeOption.themeTextColor
                                )
                            }
                        }
                    } else {
                        // Expand/collapse arrow
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isExpanded)
                                        Icons.Rounded.KeyboardArrowUp
                                    else
                                        Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Expanded Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(animationSpec = tween(200)) +
                        expandVertically(animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(150)) +
                        shrinkVertically(animationSpec = tween(150))
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = 12.dp,
                        end = 12.dp,
                        bottom = 12.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Description
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    // Select Button
                    Button(
                        onClick = onSelectClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        enabled = !themeOption.isSelected,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themeOption.themeColor,
                            contentColor = themeOption.themeTextColor,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(
                            text = if (themeOption.isSelected) "Current Theme" else "Apply Theme",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// PREVIEWS
// =============================================================================

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun EnhancedThemeSelectorPreview() {
    MaterialTheme {
        val themeOptions = listOf(
            ThemeOption(
                themeName = "Forest Green",
                themeKey = "default",
                themeColor = Color(0xFF2E7D32),
                themeTextColor = Color.White,
                isSelected = true,
                description = "A calming forest green theme inspired by nature's tranquility"
            ),
            ThemeOption(
                themeName = "Raisin Black",
                themeKey = "raisin_black",
                themeColor = Color(0xFF2D2D3A),
                themeTextColor = Color.White,
                isSelected = false,
                description = "An elegant darker theme with sophisticated raisin black tones"
            ),
            ThemeOption(
                themeName = "Burgundy",
                themeKey = "dark_red",
                themeColor = Color(0xFF800020),
                themeTextColor = Color.White,
                isSelected = false,
                description = "Rich burgundy tones for a classic and timeless appearance"
            ),
            ThemeOption(
                themeName = "System",
                themeKey = "system",
                themeColor = Color(0xFF6750A4),
                themeTextColor = Color.White,
                isSelected = false,
                description = "Automatically matches your system's theme preferences"
            )
        )

        EnhancedThemeSelector(
            themeOptions = themeOptions,
            onThemeOptionSelected = {},
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EnhancedThemeSelectorPreview_Dark() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(color = MaterialTheme.colorScheme.background) {
            val themeOptions = listOf(
                ThemeOption(
                    themeName = "Forest Green",
                    themeKey = "default",
                    themeColor = Color(0xFF81C784),
                    themeTextColor = Color.Black,
                    isSelected = false,
                    description = "A calming forest green theme"
                ),
                ThemeOption(
                    themeName = "Burgundy",
                    themeKey = "dark_red",
                    themeColor = Color(0xFFCF6679),
                    themeTextColor = Color.Black,
                    isSelected = true,
                    description = "Rich burgundy tones"
                )
            )

            EnhancedThemeSelector(
                themeOptions = themeOptions,
                onThemeOptionSelected = {},
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}