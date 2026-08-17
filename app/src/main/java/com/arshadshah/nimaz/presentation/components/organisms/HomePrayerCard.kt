package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButtonSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcons
import com.arshadshah.nimaz.presentation.components.atoms.PrayerStatusBadge
import com.arshadshah.nimaz.presentation.components.atoms.clockTimeText
import com.arshadshah.nimaz.presentation.foundation.tokens.getPrayerColor
import com.arshadshah.nimaz.presentation.model.PrayerTimeDisplay
import com.arshadshah.nimaz.presentation.model.isDone
import com.arshadshah.nimaz.presentation.model.toDisplayStatus

/**
 * Prayer section for the home compact layout.
 *
 * Shows a section header (title + done-count + settings icon) above a card that contains
 * expandable prayer rows with an inline status picker and an "Open the tracker" button.
 */
@Composable
fun HomePrayerCard(
    prayers: List<PrayerTimeDisplay>,
    onSettingsClick: () -> Unit,
    onTrackerClick: () -> Unit,
    onTogglePrayer: (PrayerType) -> Unit,
    onSetPrayerStatus: (PrayerType, PrayerStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    val doneCount = prayers.count { it.prayerStatus.toDisplayStatus(it.isPassed).isDone() }

    var expandedId by remember { mutableStateOf<PrayerType?>(null) }

    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.home_todays_prayers).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.07.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.home_prayers_done_of_five, doneCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 4.dp),
            )
            NimazIconButton(
                icon = Icons.Default.Tune,
                onClick = onSettingsClick,
                contentDescription = stringResource(R.string.home_prayer_settings_cd),
                size = NimazIconButtonSize.SMALL,
            )
        }

        NimazCard(
            style = NimazCardStyle.ELEVATED,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                prayers.forEachIndexed { index, prayer ->
                    val isExpanded = expandedId == prayer.type
                    val canExpand = prayer.type != PrayerType.SUNRISE && prayer.isPassed

                    if (index > 0) {
                        HorizontalDivider(color = dividerColor)
                    }

                    PrayerRow(
                        prayer = prayer,
                        isExpanded = isExpanded,
                        canExpand = canExpand,
                        onToggleExpand = { expandedId = if (isExpanded) null else prayer.type },
                    )

                    if (isExpanded && canExpand) {
                        StatusPicker(
                            prayer = prayer,
                            onSetPrayerStatus = onSetPrayerStatus,
                        )
                    }
                }

                HorizontalDivider(color = dividerColor)

                NimazButton(
                    text = stringResource(R.string.home_open_tracker),
                    onClick = onTrackerClick,
                    variant = NimazButtonVariant.TEXT,
                    size = NimazButtonSize.SMALL,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun PrayerRow(
    prayer: PrayerTimeDisplay,
    isExpanded: Boolean,
    canExpand: Boolean,
    onToggleExpand: () -> Unit,
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        label = "chevron_rotate",
    )
    val bgColor = if (prayer.isNext) MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
                  else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .then(if (canExpand) Modifier.clickable(onClick = onToggleExpand) else Modifier)
            .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(getPrayerColor(prayer.type))
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = prayer.name,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.widthIn(min = 62.dp),
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = clockTimeText(prayer.timeAt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        PrayerStatusBadge(
            status = prayer.prayerStatus.toDisplayStatus(prayer.isPassed),
            size = NimazBadgeSize.SMALL,
        )
        if (canExpand) {
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = NimazIcons.Forward,
                contentDescription = null,
                modifier = Modifier
                    .size(16.dp)
                    .rotate(chevronRotation),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusPicker(
    prayer: PrayerTimeDisplay,
    onSetPrayerStatus: (PrayerType, PrayerStatus) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 15.dp)
            .padding(top = 4.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            StatusChip(
                label = stringResource(R.string.on_time),
                status = PrayerStatus.PRAYED,
                current = prayer.prayerStatus,
                selectedColor = Color(0xFF3B8E3F),
                selectedBg = Color(0xFF4CAF50).copy(alpha = 0.1f),
                modifier = Modifier.weight(1f),
                onClick = { onSetPrayerStatus(prayer.type, PrayerStatus.PRAYED) },
            )
            StatusChip(
                label = stringResource(R.string.late),
                status = PrayerStatus.LATE,
                current = prayer.prayerStatus,
                selectedColor = Color(0xFF1976D2),
                selectedBg = Color(0xFF2196F3).copy(alpha = 0.1f),
                modifier = Modifier.weight(1f),
                onClick = { onSetPrayerStatus(prayer.type, PrayerStatus.LATE) },
            )
            StatusChip(
                label = stringResource(R.string.missed),
                status = PrayerStatus.MISSED,
                current = prayer.prayerStatus,
                selectedColor = Color(0xFFD3392C),
                selectedBg = Color(0xFFF44336).copy(alpha = 0.1f),
                modifier = Modifier.weight(1f),
                onClick = { onSetPrayerStatus(prayer.type, PrayerStatus.MISSED) },
            )
            StatusChip(
                label = stringResource(R.string.made_up),
                status = PrayerStatus.QADA,
                current = prayer.prayerStatus,
                selectedColor = Color(0xFF8E24AA),
                selectedBg = Color(0xFF9C27B0).copy(alpha = 0.1f),
                modifier = Modifier.weight(1f),
                onClick = { onSetPrayerStatus(prayer.type, PrayerStatus.QADA) },
            )
        }
        if (prayer.prayerStatus == PrayerStatus.NOT_PRAYED) {
            Text(
                text = stringResource(R.string.prayer_not_recorded_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    status: PrayerStatus,
    current: PrayerStatus,
    selectedColor: Color,
    selectedBg: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val isSelected = current == status
    val bgColor = if (isSelected) selectedBg else MaterialTheme.colorScheme.surface
    val textColor = if (isSelected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (isSelected) selectedColor
                      else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            ),
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
