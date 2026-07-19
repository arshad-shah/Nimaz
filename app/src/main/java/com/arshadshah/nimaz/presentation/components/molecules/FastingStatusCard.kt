package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconContainerShape
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconType
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Today's fasting status: icon + label + "fasting today" / "no fast today"
 * substatus. Used standalone in the tablet [TodayInfoCards] stack and as one
 * page of the mobile `TodayCarousel`.
 *
 * Pass [fillHeight] = true when the card should stretch to the parent's
 * height (carousel pages need this for visual consistency).
 */
@Composable
fun FastingStatusCard(
    fastingToday: Boolean,
    modifier: Modifier = Modifier,
    fillHeight: Boolean = false,
) {
    NimazCard(
        tone = NimazTone.NEUTRAL,
        style = NimazCardStyle.ELEVATED,
        modifier = modifier
            .fillMaxWidth()
            .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier),
    ) {
        val statusText = if (fastingToday) {
            stringResource(R.string.today_fasting)
        } else {
            stringResource(R.string.no_fast_today)
        }

        if (fillHeight) {
            // Carousel page (design B): "Fasting" header at the top, the state
            // big in the middle, and a logged/not-logged badge anchored at the
            // bottom — spread so the fixed-height card fills cleanly.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FastingIconChip()
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.fasting),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                FastingStateBadge(fastingToday = fastingToday)
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FastingIconChip()
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = stringResource(R.string.fasting),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FastingStateBadge(fastingToday: Boolean) {
    val bg = if (fastingToday) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val fg = if (fastingToday) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (fastingToday) {
            NimazIcon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = fg,
                iconSize = 14.dp
            )
            Spacer(modifier = Modifier.width(5.dp))
        }
        Text(
            text = stringResource(
                if (fastingToday) R.string.fasting_logged else R.string.fasting_not_logged
            ),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = fg
        )
    }
}

@Composable
private fun FastingIconChip() {
    NimazIcon(
        imageVector = Icons.Default.LightMode,
        contentDescription = null,
        type = NimazIconType.CONTAINED,
        containerShape = NimazIconContainerShape.ROUNDED_SQUARE,
        tint = MaterialTheme.colorScheme.secondary,
        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
        containerSize = 44.dp,
        iconSize = 22.dp,
        cornerRadius = 12.dp,
    )
}

@Preview(showBackground = true, widthDp = 400, name = "Fasting")
@Composable
private fun FastingStatusCard_Fasting_Preview() {
    NimazTheme {
        FastingStatusCard(
            fastingToday = true,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Not fasting")
@Composable
private fun FastingStatusCard_NotFasting_Preview() {
    NimazTheme {
        FastingStatusCard(
            fastingToday = false,
            modifier = Modifier.padding(16.dp)
        )
    }
}
