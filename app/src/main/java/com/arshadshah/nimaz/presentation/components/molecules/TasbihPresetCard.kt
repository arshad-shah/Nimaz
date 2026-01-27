package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.ContainedIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconContainerShape
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.theme.NimazColors
import androidx.compose.ui.tooling.preview.Preview

/**
 * Tasbih preset card for selecting dhikr.
 */
@Composable
fun TasbihPresetCard(
    name: String,
    arabicText: String,
    targetCount: Int,
    modifier: Modifier = Modifier,
    transliteration: String? = null,
    translation: String? = null,
    category: String? = null,
    isDefault: Boolean = false,
    totalTimesUsed: Int? = null,
    onClick: () -> Unit,
    onEditClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon/indicator
            ContainedIcon(
                imageVector = Icons.Default.Star,
                size = NimazIconSize.MEDIUM,
                containerShape = NimazIconContainerShape.ROUNDED_SQUARE,
                backgroundColor = NimazColors.TasbihColors.Counter.copy(alpha = 0.15f),
                iconColor = NimazColors.TasbihColors.Counter
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isDefault) {
                        Spacer(modifier = Modifier.width(8.dp))
                        NimazBadge(
                            text = "Default",
                            backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                            textColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            size = NimazBadgeSize.SMALL
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                ArabicText(
                    text = arabicText,
                    size = ArabicTextSize.SMALL,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )

                if (transliteration != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = transliteration,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Target: $targetCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (category != null) {
                        Text(
                            text = "  |  $category",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (totalTimesUsed != null && totalTimesUsed > 0) {
                        Text(
                            text = "  |  Used $totalTimesUsed times",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Menu or arrow
            if (onEditClick != null || onDeleteClick != null) {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (onEditClick != null) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                showMenu = false
                                onEditClick()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null)
                            }
                        )
                    }
                    if (onDeleteClick != null && !isDefault) {
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDeleteClick()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Compact preset item for selection lists.
 */
@Composable
fun CompactPresetItem(
    name: String,
    arabicText: String,
    targetCount: Int,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = arabicText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "x$targetCount",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Featured preset card for home screen quick access.
 */
@Composable
fun FeaturedPresetCard(
    name: String,
    arabicText: String,
    targetCount: Int,
    modifier: Modifier = Modifier,
    lastUsed: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = NimazColors.TasbihColors.Counter.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Quick Tasbih",
                style = MaterialTheme.typography.labelMedium,
                color = NimazColors.TasbihColors.Counter.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NimazColors.TasbihColors.Counter
            )
            Spacer(modifier = Modifier.height(8.dp))
            ArabicText(
                text = arabicText,
                size = ArabicTextSize.MEDIUM,
                color = NimazColors.TasbihColors.Counter
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Target: $targetCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = NimazColors.TasbihColors.Counter.copy(alpha = 0.7f)
                )
                if (lastUsed != null) {
                    Text(
                        text = "Last: $lastUsed",
                        style = MaterialTheme.typography.labelSmall,
                        color = NimazColors.TasbihColors.Counter.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/**
 * Category section header with count.
 */
@Composable
fun PresetCategoryHeader(
    categoryName: String,
    presetCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = categoryName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "$presetCount presets",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true, name = "Tasbih Preset Card")
@Composable
private fun TasbihPresetCardPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            TasbihPresetCard(
                name = "SubhanAllah",
                arabicText = "سُبْحَانَ اللَّهِ",
                targetCount = 33,
                transliteration = "SubhanAllah",
                translation = "Glory be to Allah",
                category = "After Prayer",
                onClick = {},
                onEditClick = {},
                onDeleteClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Tasbih Preset Card Default")
@Composable
private fun TasbihPresetCardDefaultPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            TasbihPresetCard(
                name = "Alhamdulillah",
                arabicText = "الْحَمْدُ لِلَّهِ",
                targetCount = 33,
                isDefault = true,
                totalTimesUsed = 145,
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Compact Preset Item")
@Composable
private fun CompactPresetItemPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            CompactPresetItem(
                name = "Allahu Akbar",
                arabicText = "اللَّهُ أَكْبَرُ",
                targetCount = 34,
                isSelected = false,
                onClick = {}
            )
            CompactPresetItem(
                name = "SubhanAllah",
                arabicText = "سُبْحَانَ اللَّهِ",
                targetCount = 33,
                isSelected = true,
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Featured Preset Card")
@Composable
private fun FeaturedPresetCardPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            FeaturedPresetCard(
                name = "SubhanAllah",
                arabicText = "سُبْحَانَ اللَّهِ",
                targetCount = 33,
                lastUsed = "Today",
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Preset Category Header")
@Composable
private fun PresetCategoryHeaderPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            PresetCategoryHeader(
                categoryName = "After Prayer",
                presetCount = 5
            )
        }
    }
}
