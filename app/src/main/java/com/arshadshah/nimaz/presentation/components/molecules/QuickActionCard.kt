package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.ContainedIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconContainerShape
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.theme.NimazColors
import androidx.compose.ui.tooling.preview.Preview
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Quick action item for home screen grid.
 */
@Composable
fun QuickActionCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    badge: String? = null
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box {
                ContainedIcon(
                    imageVector = icon,
                    size = NimazIconSize.LARGE,
                    containerShape = NimazIconContainerShape.ROUNDED_SQUARE,
                    backgroundColor = iconColor.copy(alpha = 0.15f),
                    iconColor = iconColor
                )
                if (badge != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.error)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onError,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Compact horizontal quick action item.
 */
@Composable
fun HorizontalQuickAction(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    subtitle: String? = null
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContainedIcon(
                imageVector = icon,
                size = NimazIconSize.MEDIUM,
                containerShape = NimazIconContainerShape.ROUNDED_SQUARE,
                backgroundColor = iconColor.copy(alpha = 0.15f),
                iconColor = iconColor
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Gradient quick action card for featured items.
 */
@Composable
fun GradientQuickAction(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(gradientColors))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Quick actions grid for home screen.
 */
@Composable
fun QuickActionsGrid(
    modifier: Modifier = Modifier,
    onQuranClick: () -> Unit,
    onHadithClick: () -> Unit,
    onDuaClick: () -> Unit,
    onTasbihClick: () -> Unit,
    onQiblaClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onFastingClick: () -> Unit,
    onZakatClick: () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                title = "Quran",
                icon = Icons.Default.MenuBook,
                iconColor = NimazColors.QuranColors.Meccan,
                onClick = onQuranClick,
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                title = "Hadith",
                icon = Icons.Default.Book,
                iconColor = NimazColors.QuranColors.Medinan,
                onClick = onHadithClick,
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                title = "Duas",
                icon = Icons.Default.Book,
                iconColor = MaterialTheme.colorScheme.secondary,
                onClick = onDuaClick,
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                title = "Tasbih",
                icon = Icons.Default.RadioButtonChecked,
                iconColor = NimazColors.TasbihColors.Counter,
                onClick = onTasbihClick,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                title = "Qibla",
                icon = Icons.Default.Explore,
                iconColor = MaterialTheme.colorScheme.primary,
                onClick = onQiblaClick,
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                title = "Calendar",
                icon = Icons.Default.CalendarMonth,
                iconColor = MaterialTheme.colorScheme.tertiary,
                onClick = onCalendarClick,
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                title = "Fasting",
                icon = Icons.Default.Restaurant,
                iconColor = NimazColors.FastingColors.Fasted,
                onClick = onFastingClick,
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                title = "Zakat",
                icon = Icons.Default.Calculate,
                iconColor = NimazColors.ZakatColors.Gold,
                onClick = onZakatClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Mini quick action for space-constrained layouts.
 */
@Composable
fun MiniQuickAction(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 48.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(iconColor.copy(alpha = 0.15f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Preview(showBackground = true, name = "Quick Action Card")
@Composable
private fun QuickActionCardPreview() {
    NimazTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                title = "Quran",
                icon = Icons.Default.MenuBook,
                iconColor = NimazColors.QuranColors.Meccan,
                onClick = {},
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                title = "Hadith",
                icon = Icons.Default.Book,
                iconColor = NimazColors.QuranColors.Medinan,
                subtitle = "Daily",
                onClick = {},
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Preview(showBackground = true, name = "Quick Action Card with Badge")
@Composable
private fun QuickActionCardWithBadgePreview() {
    NimazTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                title = "Tasbih",
                icon = Icons.Default.RadioButtonChecked,
                iconColor = NimazColors.TasbihColors.Counter,
                badge = "3",
                onClick = {},
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                title = "Qibla",
                icon = Icons.Default.Explore,
                onClick = {},
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Preview(showBackground = true, name = "Horizontal Quick Action")
@Composable
private fun HorizontalQuickActionPreview() {
    NimazTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HorizontalQuickAction(
                title = "Quran",
                icon = Icons.Default.MenuBook,
                iconColor = NimazColors.QuranColors.Meccan,
                subtitle = "Continue reading",
                onClick = {}
            )
            HorizontalQuickAction(
                title = "Calendar",
                icon = Icons.Default.CalendarMonth,
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Gradient Quick Action")
@Composable
private fun GradientQuickActionPreview() {
    NimazTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GradientQuickAction(
                title = "Start Fasting",
                icon = Icons.Default.Restaurant,
                subtitle = "Track your Ramadan fasts",
                gradientColors = listOf(
                    NimazColors.FastingColors.Fasted,
                    NimazColors.FastingColors.Fasted.copy(alpha = 0.7f)
                ),
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Quick Actions Grid")
@Composable
private fun QuickActionsGridPreview() {
    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            QuickActionsGrid(
                onQuranClick = {},
                onHadithClick = {},
                onDuaClick = {},
                onTasbihClick = {},
                onQiblaClick = {},
                onCalendarClick = {},
                onFastingClick = {},
                onZakatClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Mini Quick Actions")
@Composable
private fun MiniQuickActionPreview() {
    NimazTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MiniQuickAction(
                icon = Icons.Default.MenuBook,
                iconColor = NimazColors.QuranColors.Meccan,
                onClick = {}
            )
            MiniQuickAction(
                icon = Icons.Default.Explore,
                onClick = {}
            )
            MiniQuickAction(
                icon = Icons.Default.CalendarMonth,
                onClick = {}
            )
        }
    }
}
