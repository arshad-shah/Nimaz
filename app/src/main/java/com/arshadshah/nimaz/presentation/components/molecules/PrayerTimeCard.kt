package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazCheckbox
import com.arshadshah.nimaz.presentation.components.atoms.NimazCheckboxSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazCheckboxType
import com.arshadshah.nimaz.presentation.components.atoms.NimazCheckboxVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconContainerShape
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconType
import com.arshadshah.nimaz.presentation.components.atoms.getArabicPrayerName
import com.arshadshah.nimaz.presentation.components.atoms.getPrayerColor
import com.arshadshah.nimaz.presentation.components.atoms.getPrayerIcon
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.viewmodel.home.PrayerTimeDisplay
import kotlin.time.Instant
import com.arshadshah.nimaz.presentation.components.atoms.clockTimeText

/**
 * A single prayer's row card: icon + name (English / Arabic) + time +
 * prayed-toggle. Active (next-up) prayers get a primaryContainer background;
 * passed prayers fade. Sunrise has no toggle since it isn't a salat.
 */
@Composable
fun PrayerTimeCard(
    prayer: PrayerTimeDisplay,
    isActive: Boolean,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    showToggle: Boolean = true
) {
    val prayerColor = getPrayerColor(prayer.type)
    val isPrayed = prayer.prayerStatus == PrayerStatus.PRAYED
    val isSunrise = prayer.type == PrayerType.SUNRISE || !showToggle

    NimazCard(
        style = NimazCardStyle.FILLED,
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (prayer.isPassed && !isActive) 0.6f else 1f),
        shape = RoundedCornerShape(16.dp),
        selected = isActive,
        // Active prayers get a coloured left accent bar (drawn via the Row
        // below) plus a soft outline; the same prayer-tinted border on both
        // states keeps the list reading quietly until the next prayer pops.
        colors = NimazCardDefaults.selectable(
            border = prayerColor.copy(alpha = 0.4f),
            activeBorder = prayerColor.copy(alpha = 0.4f)
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Prayer Icon
                NimazIcon(
                    imageVector = getPrayerIcon(prayer.type),
                    contentDescription = null,
                    type = NimazIconType.CONTAINED,
                    containerShape = NimazIconContainerShape.ROUNDED_SQUARE,
                    tint = MaterialTheme.colorScheme.onSurface,
                    containerColor = prayerColor.copy(
                        alpha = if (prayer.isPassed && !isActive) 0.12f else 0.2f
                    ),
                    containerSize = 44.dp,
                    iconSize = 26.dp,
                    cornerRadius = 12.dp,
                )

                Spacer(modifier = Modifier.width(14.dp))

                // Prayer Info (name + Arabic + optional NEXT badge)
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = prayer.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    ArabicText(
                        text = getArabicPrayerName(prayer.type),
                        size = ArabicTextSize.SMALL,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Prayer Time
                Text(
                    text = clockTimeText(prayer.timeAt),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.width(14.dp))

                if (!isSunrise) {
                    NimazCheckbox(
                        checked = isPrayed,
                        onCheckedChange = { onToggle() },
                        variant = NimazCheckboxVariant.SUCCESS,
                        size = NimazCheckboxSize.LARGE,
                        type = NimazCheckboxType.CIRCLE,
                        contentDescription = stringResource(R.string.prayed)
                    )
                } else {
                    Spacer(modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

private val sampleAsr = PrayerTimeDisplay(
    PrayerType.ASR, "Asr", previewInstant(16, 30),
    isPassed = false, isCurrent = true, isNext = true
)

private val sampleFajr = PrayerTimeDisplay(
    PrayerType.FAJR, "Fajr", previewInstant(5, 23),
    isPassed = true, isCurrent = false, isNext = false,
    prayerStatus = PrayerStatus.PRAYED
)

private val sampleMaghrib = PrayerTimeDisplay(
    PrayerType.MAGHRIB, "Maghrib", previewInstant(18, 12),
    isPassed = false, isCurrent = false, isNext = false
)

private val sampleSunrise = PrayerTimeDisplay(
    PrayerType.SUNRISE, "Sunrise", previewInstant(18, 12),
    isPassed = false, isCurrent = false, isNext = false
)

@Preview(showBackground = true, widthDp = 400, name = "Active")
@Composable
private fun PrayerTimeCard_Active_Preview() {
    NimazTheme {
        PrayerTimeCard(
            prayer = sampleAsr,
            isActive = true,
            onClick = {},
            onToggle = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Completed")
@Composable
private fun PrayerTimeCard_Completed_Preview() {
    NimazTheme {
        PrayerTimeCard(
            prayer = sampleFajr,
            isActive = false,
            onClick = {},
            onToggle = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Upcoming")
@Composable
private fun PrayerTimeCard_Upcoming_Preview() {
    NimazTheme {
        PrayerTimeCard(
            prayer = sampleMaghrib,
            isActive = false,
            onClick = {},
            onToggle = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}


@Preview(showBackground = true, widthDp = 400, name = "Sunrise")
@Composable
private fun PrayerTimeCard_sunrise_Preview() {
    NimazTheme {
        PrayerTimeCard(
            prayer = sampleSunrise,
            isActive = false,
            onClick = {},
            onToggle = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

/** Fixed wall-clock instants for previews, so sample rows read like a real day. */
private fun previewInstant(hour: Int, minute: Int): Instant =
    Instant.fromEpochMilliseconds(
        java.time.LocalDate.now().atTime(hour, minute)
            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
