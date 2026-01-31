package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Get the color associated with a prayer type.
 */
fun getPrayerColor(prayerType: PrayerType): Color {
    return when (prayerType) {
        PrayerType.FAJR -> NimazColors.PrayerColors.Fajr
        PrayerType.SUNRISE -> NimazColors.PrayerColors.Sunrise
        PrayerType.DHUHR -> NimazColors.PrayerColors.Dhuhr
        PrayerType.ASR -> NimazColors.PrayerColors.Asr
        PrayerType.MAGHRIB -> NimazColors.PrayerColors.Maghrib
        PrayerType.ISHA -> NimazColors.PrayerColors.Isha
    }
}

/**
 * Get the gradient end color for a prayer type.
 */
fun getPrayerGradientEnd(prayerType: PrayerType): Color {
    return when (prayerType) {
        PrayerType.FAJR -> NimazColors.PrayerColors.FajrGradientEnd
        PrayerType.SUNRISE -> NimazColors.PrayerColors.SunriseGradientEnd
        PrayerType.DHUHR -> NimazColors.PrayerColors.DhuhrGradientEnd
        PrayerType.ASR -> NimazColors.PrayerColors.AsrGradientEnd
        PrayerType.MAGHRIB -> NimazColors.PrayerColors.MaghribGradientEnd
        PrayerType.ISHA -> NimazColors.PrayerColors.IshaGradientEnd
    }
}

/**
 * Get the gradient brush for a prayer type.
 */
fun getPrayerGradient(prayerType: PrayerType): Brush {
    return Brush.linearGradient(
        colors = listOf(
            getPrayerColor(prayerType),
            getPrayerGradientEnd(prayerType)
        )
    )
}

/**
 * Vertical bar indicator with prayer color.
 */
@Composable
fun PrayerColorBar(
    prayerType: PrayerType,
    modifier: Modifier = Modifier,
    width: Dp = 4.dp,
    height: Dp = 24.dp,
    useGradient: Boolean = true,
    shape: Shape = RoundedCornerShape(2.dp)
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(shape)
            .background(
                if (useGradient) {
                    getPrayerGradient(prayerType)
                } else {
                    Brush.linearGradient(listOf(getPrayerColor(prayerType), getPrayerColor(prayerType)))
                }
            )
    )
}

/**
 * Status indicator with color (Prayed, Missed, etc.).
 */
@Composable
fun StatusIndicator(
    status: PrayerStatus,
    modifier: Modifier = Modifier,
    size: Dp = 12.dp,
    showLabel: Boolean = false
) {
    val color = when (status) {
        PrayerStatus.PRAYED -> NimazColors.StatusColors.Prayed
        PrayerStatus.MISSED -> NimazColors.StatusColors.Missed
        PrayerStatus.PENDING -> NimazColors.StatusColors.Pending
        PrayerStatus.QADA -> NimazColors.StatusColors.Qada
        PrayerStatus.JAMAAH -> NimazColors.StatusColors.Jamaah
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color)
        )
        if (showLabel) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = status.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = color
            )
        }
    }
}

/**
 * Prayer status enum for indicators.
 */
enum class PrayerStatus(val displayName: String) {
    PRAYED("Prayed"),
    MISSED("Missed"),
    PENDING("Pending"),
    QADA("Qada"),
    JAMAAH("Jama'ah")
}

@Preview(showBackground = true, name = "Prayer Color Bars")
@Composable
private fun PrayerColorBarPreview() {
    NimazTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PrayerType.entries.take(5).forEach { prayerType ->
                PrayerColorBar(prayerType = prayerType, height = 48.dp)
            }
        }
    }
}

@Preview(showBackground = true, name = "Status Indicators")
@Composable
private fun StatusIndicatorPreview() {
    NimazTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PrayerStatus.entries.forEach { status ->
                StatusIndicator(status = status, showLabel = true)
            }
        }
    }
}

