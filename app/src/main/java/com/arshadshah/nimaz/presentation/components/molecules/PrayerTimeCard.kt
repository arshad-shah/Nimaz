package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Text
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.getArabicPrayerName
import com.arshadshah.nimaz.presentation.components.atoms.getPrayerColor
import com.arshadshah.nimaz.presentation.components.atoms.getPrayerIcon
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.viewmodel.PrayerTimeDisplay

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
    modifier: Modifier = Modifier
) {
    val prayerColor = getPrayerColor(prayer.type)
    val isPrayed = prayer.prayerStatus == PrayerStatus.PRAYED
    val isSunrise = prayer.type == PrayerType.SUNRISE

    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (prayer.isPassed && !isActive) 0.6f else 1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        // Active prayers get a coloured left accent bar (drawn via the Row
        // below) plus a soft outline; no border on inactive cards so the list
        // reads quietly until the next prayer pops.
        border = if (isActive) {
            BorderStroke(1.dp, prayerColor.copy(alpha = 0.4f))
        } else null,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left accent bar — only on the active prayer. 4dp wide, full
            // card height, prayer-coloured. The single biggest readability
            // win for spotting the next prayer at a glance.
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(72.dp)
                    .background(if (isActive) prayerColor else Color.Transparent)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Prayer Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            prayerColor.copy(
                                alpha = if (prayer.isPassed && !isActive) 0.12f else 0.2f
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getPrayerIcon(prayer.type),
                        contentDescription = null,
                        tint = prayerColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

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
                        if (isActive) {
                            Spacer(modifier = Modifier.width(8.dp))
                            NextPill(accent = prayerColor)
                        }
                    }
                    ArabicText(
                        text = getArabicPrayerName(prayer.type),
                        size = ArabicTextSize.SMALL,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Prayer Time
                Text(
                    text = prayer.time,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.width(14.dp))

                if (!isSunrise) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onToggle)
                            .then(
                                if (isPrayed) {
                                    Modifier.background(MaterialTheme.colorScheme.primary)
                                } else {
                                    Modifier.border(
                                        2.dp,
                                        MaterialTheme.colorScheme.outline,
                                        CircleShape
                                    )
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isPrayed) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(R.string.prayed),
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

/**
 * Small "NEXT" pill rendered next to the active prayer's name. Uses the
 * prayer's accent colour at 20% opacity so it reads as part of the same
 * visual family as the left accent bar and the icon container.
 */
@Composable
private fun NextPill(accent: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(accent.copy(alpha = 0.22f))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            text = "NEXT",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 0.8.sp
            ),
            color = accent
        )
    }
}

private val sampleAsr = PrayerTimeDisplay(
    PrayerType.ASR, "Asr", "4:30 PM",
    isPassed = false, isCurrent = true, isNext = true
)

private val sampleFajr = PrayerTimeDisplay(
    PrayerType.FAJR, "Fajr", "5:23 AM",
    isPassed = true, isCurrent = false, isNext = false,
    prayerStatus = PrayerStatus.PRAYED
)

private val sampleMaghrib = PrayerTimeDisplay(
    PrayerType.MAGHRIB, "Maghrib", "6:12 PM",
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
