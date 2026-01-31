package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.ContainedIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconContainerShape
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.tooling.preview.Preview
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Location list item for saved locations.
 */
@Composable
fun LocationItem(
    locationName: String,
    country: String,
    modifier: Modifier = Modifier,
    region: String? = null,
    latitude: Double? = null,
    longitude: Double? = null,
    isSelected: Boolean = false,
    isFavorite: Boolean = false,
    isCurrentLocation: Boolean = false,
    onClick: () -> Unit,
    onFavoriteClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Location icon
            ContainedIcon(
                imageVector = if (isCurrentLocation) Icons.Default.MyLocation else Icons.Default.LocationOn,
                size = NimazIconSize.MEDIUM,
                containerShape = NimazIconContainerShape.ROUNDED_SQUARE,
                backgroundColor = if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                iconColor = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Location details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = locationName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isCurrentLocation) {
                        Spacer(modifier = Modifier.width(8.dp))
                        NimazBadge(
                            text = "Current",
                            backgroundColor = MaterialTheme.colorScheme.primary,
                            textColor = MaterialTheme.colorScheme.onPrimary,
                            size = NimazBadgeSize.SMALL
                        )
                    }
                }

                Text(
                    text = buildString {
                        if (region != null) {
                            append(region)
                            append(", ")
                        }
                        append(country)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (latitude != null && longitude != null) {
                    Text(
                        text = "${String.format("%.4f", latitude)}, ${String.format("%.4f", longitude)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // Selected indicator
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Favorite star
            if (onFavoriteClick != null) {
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (isFavorite) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            // More options menu
            if (onDeleteClick != null && !isCurrentLocation) {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(40.dp)
                ) {
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
        }
    }
}

/**
 * Compact location selection item.
 */
@Composable
fun CompactLocationItem(
    locationName: String,
    country: String,
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
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = locationName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                text = country,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Current location indicator card.
 */
@Composable
fun CurrentLocationCard(
    locationName: String,
    country: String,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onRefreshClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Current Location",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isLoading) "Detecting..." else locationName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (!isLoading) {
                    Text(
                        text = country,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (onRefreshClick != null) {
                IconButton(onClick = onRefreshClick) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Refresh location",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}


@Preview(showBackground = true, name = "Location Item")
@Composable
private fun LocationItemPreview() {
    NimazTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LocationItem(
                locationName = "Dublin",
                country = "Ireland",
                region = "Leinster",
                latitude = 53.3498,
                longitude = -6.2603,
                onClick = {},
                onFavoriteClick = {},
                onDeleteClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Location Item Selected")
@Composable
private fun LocationItemSelectedPreview() {
    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            LocationItem(
                locationName = "Mecca",
                country = "Saudi Arabia",
                isSelected = true,
                isFavorite = true,
                onClick = {},
                onFavoriteClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Location Item Current")
@Composable
private fun LocationItemCurrentPreview() {
    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            LocationItem(
                locationName = "Current Location",
                country = "Ireland",
                isCurrentLocation = true,
                isSelected = true,
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Compact Location Item")
@Composable
private fun CompactLocationItemPreview() {
    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            CompactLocationItem(
                locationName = "London",
                country = "United Kingdom",
                isSelected = false,
                onClick = {}
            )
            CompactLocationItem(
                locationName = "Paris",
                country = "France",
                isSelected = true,
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Current Location Card")
@Composable
private fun CurrentLocationCardPreview() {
    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            CurrentLocationCard(
                locationName = "Dublin",
                country = "Ireland",
                onRefreshClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Current Location Card Loading")
@Composable
private fun CurrentLocationCardLoadingPreview() {
    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            CurrentLocationCard(
                locationName = "",
                country = "",
                isLoading = true,
                onRefreshClick = {}
            )
        }
    }
}
