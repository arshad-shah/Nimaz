package com.arshadshah.nimaz.presentation.components.atoms

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * Custom prayer icons that show the sun at its stage over a horizon line, so
 * each daytime prayer reads differently:
 *  - Sunrise / Maghrib: a half-disc sitting on the horizon (east vs. west).
 *  - Dhuhr: a full sun high and centred (the apex).
 *  - Asr: a full sun offset lower-right (descending).
 *  - Fajr / Isha: a crescent (Isha adds a star).
 *
 * Built from SVG path strings via [addPathNodes] (geometry only —
 * [androidx.compose.material3.Icon] applies the tint at the call site). Built
 * once via [lazy].
 */

private val ink = SolidColor(Color.Black)
private const val HORIZON = "M2.5 17 L21.5 17"

private fun prayerIcon(name: String, content: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply(content).build()

private fun ImageVector.Builder.fillPath(pathData: String) {
    addPath(pathData = addPathNodes(pathData), fill = ink)
}

private fun ImageVector.Builder.strokePath(
    pathData: String,
    width: Float = 1.5f,
    alpha: Float = 1f
) {
    addPath(
        pathData = addPathNodes(pathData),
        stroke = ink,
        strokeAlpha = alpha,
        strokeLineWidth = width,
        strokeLineCap = StrokeCap.Round,
    )
}

val PrayerIconFajr: ImageVector by lazy {
    prayerIcon("PrayerFajr") {
        strokePath(HORIZON, width = 1.7f)
        fillPath("M13.4 8.6 A4.6 4.6 0 1 1 8.8 4 A3.5 3.5 0 0 0 13.4 8.6 Z")
    }
}

val PrayerIconSunrise: ImageVector by lazy {
    prayerIcon("PrayerSunrise") {
        strokePath("M8 6.6 L8 8.4 M2.4 12 L3.9 12.8 M13.6 12 L12.1 12.8")
        fillPath("M3.2 17 A4.8 4.8 0 0 1 12.8 17 Z")
        strokePath(HORIZON, width = 1.7f)
    }
}

val PrayerIconDhuhr: ImageVector by lazy {
    prayerIcon("PrayerDhuhr") {
        strokePath(HORIZON, width = 1.7f, alpha = 0.55f)
        fillPath("M16.4 9 A4.4 4.4 0 1 1 7.6 9 A4.4 4.4 0 1 1 16.4 9 Z")
        strokePath(
            "M12 1.3 L12 2.9 M6.3 9 L4.7 9 M17.7 9 L19.3 9 " +
                    "M7.96 4.96 L6.83 3.83 M16.04 4.96 L17.17 3.83 " +
                    "M7.96 13.04 L6.83 14.17 M16.04 13.04 L17.17 14.17",
        )
    }
}

val PrayerIconAsr: ImageVector by lazy {
    prayerIcon("PrayerAsr") {
        strokePath(HORIZON, width = 1.7f, alpha = 0.7f)
        fillPath("M19.1 10.8 A4.1 4.1 0 1 1 10.9 10.8 A4.1 4.1 0 1 1 19.1 10.8 Z")
        strokePath(
            "M15 3.9 L15 5.4 M21.4 10.8 L19.9 10.8 " +
                    "M18.95 6.85 L20 5.8 M18.95 14.75 L20 15.8 M9.6 10.8 L8.1 10.8",
        )
    }
}

val PrayerIconMaghrib: ImageVector by lazy {
    prayerIcon("PrayerMaghrib") {
        strokePath("M16 6.6 L16 8.4 M10.4 12 L11.9 12.8 M21.6 12 L20.1 12.8")
        fillPath("M11.2 17 A4.8 4.8 0 0 1 20.8 17 Z")
        strokePath(HORIZON, width = 1.7f)
    }
}

val PrayerIconIsha: ImageVector by lazy {
    prayerIcon("PrayerIsha") {
        fillPath("M16.8 13 A6.6 6.6 0 1 1 9.2 5.4 A5.1 5.1 0 0 0 16.8 13 Z")
        fillPath("M5.4 4.6 L6.05 6.1 L7.55 6.75 L6.05 7.4 L5.4 8.9 L4.75 7.4 L2.75 7.4 L4.25 6.75 Z")
    }
}


// ==================== PREVIEWS ====================

/**
 * Shows every prayer-stage icon (sun-over-horizon / crescent geometry) tinted
 * with the theme primary so the per-stage differences are visible.
 */
@Composable
private fun PrayerStageIconsShowcase() {
    val icons = listOf(
        "Fajr" to PrayerIconFajr,
        "Sunrise" to PrayerIconSunrise,
        "Dhuhr" to PrayerIconDhuhr,
        "Asr" to PrayerIconAsr,
        "Maghrib" to PrayerIconMaghrib,
        "Isha" to PrayerIconIsha,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        icons.forEach { (label, icon) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NimazIcon(
                    imageVector = icon,
                    contentDescription = label,
                    size = NimazIconSize.EXTRA_LARGE,
                    variant = NimazIconVariant.PRIMARY
                )
                Text(text = label, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Preview(showBackground = true, name = "Prayer Stage Icons — Light")
@Composable
private fun PrayerStageIconsLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        PrayerStageIconsShowcase()
    }
}

@Preview(
    showBackground = true, name = "Prayer Stage Icons — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun PrayerStageIconsDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        PrayerStageIconsShowcase()
    }
}
