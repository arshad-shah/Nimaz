package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.res.stringResource
import com.arshadshah.nimaz.R
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerRecord
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.color
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun NimazQadaPrayerItem(
    prayer: PrayerRecord,
    onMarkCompleted: () -> Unit,
    modifier: Modifier = Modifier,
    actionText: String = "Done"
) {
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")
    val formattedDate = try {
        Instant.ofEpochMilli(prayer.date)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(dateFormatter)
    } catch (e: Exception) {
        stringResource(R.string.unknown_date)
    }

    val prayerColor = prayer.prayerName.color()

    NimazCard(
        style = NimazCardStyle.FILLED,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
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

            NimazButton(
                text = actionText,
                onClick = onMarkCompleted,
                variant = NimazButtonVariant.TONAL,
                size = NimazButtonSize.SMALL,
                leadingIcon = Icons.Default.Check,
                accent = NimazColors.StatusColors.Prayed
            )
        }
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
