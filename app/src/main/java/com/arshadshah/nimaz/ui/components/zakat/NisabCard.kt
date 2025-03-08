package com.arshadshah.nimaz.ui.components.zakat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.rounded.LocalAtm
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.services.NisabType
import com.arshadshah.nimaz.ui.components.common.HeaderWithIcon
import com.arshadshah.nimaz.viewModel.ZakatState
import java.util.Locale

@Composable
fun NisabCard(
    state: ZakatState,
    onNisabTypeChange: (NisabType) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeaderWithIcon(
                title = "Nisab Threshold",
                icon = Icons.AutoMirrored.Rounded.CompareArrows,
                contentDescription = "Nisab Threshold Icon",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )

            // Mode Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NisabType.entries.forEach { type ->
                    val isSelected = state.selectedNisabType == type
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNisabTypeChange(type) },
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = when (type) {
                                        NisabType.GOLD -> Icons.Rounded.LocalAtm
                                        NisabType.SILVER -> Icons.Rounded.Savings
                                    },
                                    contentDescription = type.name,
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxSize(),
                                    tint = when {
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            Text(
                                text = type.name.replaceFirstChar {
                                    if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }

            // Current Value Display
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = when (state.selectedNisabType) {
                                NisabType.GOLD -> Icons.Rounded.LocalAtm
                                NisabType.SILVER -> Icons.Rounded.Savings
                            },
                            contentDescription = null,
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxSize(),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Current Threshold",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${state.currencyInfo.symbol} ${
                                String.format(
                                    "%.2f",
                                    when (state.selectedNisabType) {
                                        NisabType.GOLD -> state.goldNisabThreshold
                                        NisabType.SILVER -> state.silverNisabThreshold
                                    }
                                )
                            }",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Live Rate",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}