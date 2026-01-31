package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerRecord
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun NimazQadaPrayerItem(
    prayer: PrayerRecord,
    onMarkCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")
    val formattedDate = try {
        Instant.ofEpochMilli(prayer.date)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(dateFormatter)
    } catch (e: Exception) {
        "Unknown date"
    }

    val prayerColor = getPrayerColor(prayer.prayerName)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(prayerColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(prayerColor)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = prayer.prayerName.displayName(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                onClick = onMarkCompleted,
                shape = RoundedCornerShape(8.dp),
                color = NimazColors.StatusColors.Prayed.copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = NimazColors.StatusColors.Prayed,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Done",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = NimazColors.StatusColors.Prayed
                    )
                }
            }
        }
    }
}

private fun getPrayerColor(prayerName: PrayerName): Color {
    return when (prayerName) {
        PrayerName.FAJR -> NimazColors.PrayerColors.Fajr
        PrayerName.SUNRISE -> NimazColors.PrayerColors.Sunrise
        PrayerName.DHUHR -> NimazColors.PrayerColors.Dhuhr
        PrayerName.ASR -> NimazColors.PrayerColors.Asr
        PrayerName.MAGHRIB -> NimazColors.PrayerColors.Maghrib
        PrayerName.ISHA -> NimazColors.PrayerColors.Isha
    }
}

@Preview(showBackground = true, widthDp = 400, name = "NimazQadaPrayerItem")
@Composable
private fun NimazQadaPrayerItemPreview() {
    NimazTheme {
        NimazQadaPrayerItem(
            prayer = PrayerRecord(
                id = 1,
                date = System.currentTimeMillis(),
                prayerName = PrayerName.FAJR,
                status = PrayerStatus.MISSED,
                prayedAt = null,
                scheduledTime = System.currentTimeMillis(),
                isJamaah = false,
                isQadaFor = null,
                note = null,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ),
            onMarkCompleted = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
