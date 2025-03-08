package com.arshadshah.nimaz.ui.components.zakat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
import com.arshadshah.nimaz.ui.components.common.HeaderWithIcon

@Composable
fun ZakatPeriodCard(
    startDate: String,
    endDate: String,
    modifier: Modifier = Modifier
) {

    val showInfo = remember { mutableStateOf(false) }
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderWithIcon(
                    icon = Icons.Rounded.CalendarMonth,
                    title = "Zakat Period",
                    contentDescription = "Zakat period details",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                OutlinedIconButton(
                    onClick = { showInfo.value = !showInfo.value },
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = "Info",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Date Details Section
            DateDetailsSection(startDate, endDate)


            if (showInfo.value) {
                AlertDialogNimaz(
                    title = "Zakat Period",
                    contentDescription = "Zakat period details",
                    contentToShow = {
                        Text(
                            text = "Zakat is due on wealth that has been in your possession for a full lunar year. The lunar year is 354 days long. The Zakat period starts from the day you acquired the wealth and ends on the same day the following year.",
                        )
                    },
                    onDismissRequest = { showInfo.value = false },
                    onConfirm = { showInfo.value = false },
                    onDismiss = { showInfo.value = false },
                    showDismissButton = false,
                    confirmButtonText = "Got it"
                )
            }
        }
    }
}

@Composable
private fun DateDetailsSection(startDate: String, endDate: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DetailRow(
                icon = Icons.Rounded.CalendarMonth,
                label = "From",
                value = startDate
            )
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
            )
            DetailRow(
                icon = Icons.Rounded.CalendarMonth,
                label = "To",
                value = endDate
            )
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ZakatPeriodCardPreview() {
    MaterialTheme {
        ZakatPeriodCard(
            startDate = "01 Jan 2021",
            endDate = "01 Jan 2022"
        )
    }
}