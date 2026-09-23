package com.arshadshah.nimaz.presentation.components.molecules

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.NimazSolarArc
import com.arshadshah.nimaz.presentation.components.atoms.NimazSolarArcDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazSolarNode
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * One prayer (or sunrise) on a [NimazPrayerDayArc].
 *
 * @param position day fraction in `0f..1f`.
 * @param isHorizon sunrise and Maghrib: the sun's horizon crossings. They are drawn as bare dots —
 *   the arc already says where they are — and their times go in the row under the arc.
 * @param highlighted the time just moved; see [NimazSolarNode.highlighted].
 */
data class NimazPrayerDayPoint(
    val position: Float,
    val name: String,
    val time: String,
    val isHorizon: Boolean = false,
    val tone: NimazTone = NimazTone.MUTED,
    val highlighted: Boolean = false,
)

/**
 * A day's prayers on the sun's path, with sunrise and sunset under it — the Prayer Times screen's
 * day card and the prayer settings' live preview are this one component.
 *
 * With [showTimes] each prayer carries its time on the arc, so the drawing *is* the timetable (the
 * settings preview, where there is no list below). Without it the arc carries names only, for a
 * screen that lists the times underneath.
 *
 * @param sunPosition / @param litSpan as on [NimazSolarArc] — today only.
 */
@Composable
fun NimazPrayerDayArc(
    points: List<NimazPrayerDayPoint>,
    sunriseFraction: Float,
    sunsetFraction: Float,
    contentDescription: String,
    modifier: Modifier = Modifier,
    showTimes: Boolean = true,
    sunPosition: Float? = null,
    litSpan: ClosedFloatingPointRange<Float>? = null,
) {
    val sunrise = points.firstOrNull { it.isHorizon && it.position <= 0.5f }
    val sunset = points.lastOrNull { it.isHorizon && it.position > 0.5f }
    Column(modifier = modifier.fillMaxWidth()) {
        NimazSolarArc(
            nodes = points.map { point ->
                NimazSolarNode(
                    position = point.position,
                    label = point.name.takeUnless { point.isHorizon },
                    tone = point.tone,
                    contentDescription = "${point.name} ${point.time}",
                    time = point.time.takeIf { showTimes && !point.isHorizon },
                    highlighted = point.highlighted,
                )
            },
            sunriseFraction = sunriseFraction,
            sunsetFraction = sunsetFraction,
            contentDescription = contentDescription,
            sunPosition = sunPosition,
            litSpan = litSpan,
            height = if (showTimes) NimazSolarArcDefaults.HeightWithTimes else NimazSolarArcDefaults.Height,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            HorizonTime(sunrise)
            HorizonTime(sunset)
        }
    }
}

@Composable
private fun HorizonTime(point: NimazPrayerDayPoint?) {
    if (point == null) return
    Text(
        text = point.time,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = if (point.highlighted) FontWeight.SemiBold else null,
        color = if (point.highlighted) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// ==================== PREVIEWS ====================

private val previewDay = listOf(
    NimazPrayerDayPoint(0.218f, "Fajr", "5:13"),
    NimazPrayerDayPoint(0.300f, "Sunrise", "7:12", isHorizon = true, tone = NimazTone.ACCENT),
    NimazPrayerDayPoint(0.553f, "Dhuhr", "1:17", tone = NimazTone.PROMINENT),
    NimazPrayerDayPoint(0.689f, "Asr", "4:32", tone = NimazTone.WARNING, highlighted = true),
    NimazPrayerDayPoint(0.806f, "Maghrib", "7:21", isHorizon = true, tone = NimazTone.WARNING),
    NimazPrayerDayPoint(0.884f, "Isha", "9:13"),
)

@Composable
private fun NimazPrayerDayArcShowcase() {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        NimazPrayerDayArc(previewDay, 0.300f, 0.806f, "Today's prayers")
        NimazPrayerDayArc(previewDay, 0.300f, 0.806f, "Today's prayers", showTimes = false, sunPosition = 0.6f)
    }
}

@Preview(showBackground = true, widthDp = 360, name = "NimazPrayerDayArc — Light")
@Composable
private fun NimazPrayerDayArcLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { NimazPrayerDayArcShowcase() }
}

@Preview(
    showBackground = true, widthDp = 360, name = "NimazPrayerDayArc — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
@Composable
private fun NimazPrayerDayArcDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { NimazPrayerDayArcShowcase() }
}
