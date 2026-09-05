package com.arshadshah.nimaz.presentation.components.molecules

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.foundation.tokens.getArabicPrayerName
import com.arshadshah.nimaz.presentation.foundation.tokens.getPrayerColor
import com.arshadshah.nimaz.presentation.foundation.tokens.getPrayerIcon
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/** How far a prayer whose time has passed is knocked back. */
private const val PassedAlpha = 0.5f

/** The tint strength of the next prayer's icon well. */
private const val NextWellAlpha = 0.20f

/**
 * One prayer as a row inside a shared card — the reference form.
 *
 * Deliberately **not** [PrayerTimeCard], which is a card *per* prayer carrying a tracking
 * checkbox, and which `HomeScreen`'s two-column layout is built around. The two coexist because
 * they do different jobs; neither is a restyle of the other.
 *
 * This row has no `onClick` and no toggle **by design**. Prayer Times answers *when*; the prayer
 * tracker answers what the reader did about it. A row that looked tappable would promise logging
 * this screen no longer performs — and the logging it used to perform wrote a binary
 * `PRAYED`/`NOT_PRAYED`, which silently downgraded a prayer the reader had recorded as `LATE` on
 * the tracker, and turned an assertion into an absence on a second tap.
 *
 * @param qualifier a short note beside the name — "Jumu'ah" on a Friday Dhuhr.
 * @param isNext tints the icon well and weights the time. It does **not** make the row
 *   interactive.
 * @param showArabic sunrise is not a salat, so it carries no Arabic name.
 */
@Composable
fun NimazPrayerRow(
    type: PrayerType,
    name: String,
    time: String,
    modifier: Modifier = Modifier,
    qualifier: String? = null,
    isPassed: Boolean = false,
    isNext: Boolean = false,
    showArabic: Boolean = true,
) {
    val prayerColor = getPrayerColor(type)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (isPassed && !isNext) PassedAlpha else 1f)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isNext) {
                        prayerColor.copy(alpha = NextWellAlpha)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            NimazIcon(
                imageVector = getPrayerIcon(type),
                contentDescription = null,
                size = NimazIconSize.SMALL,
                tint = if (isNext) prayerColor else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (qualifier != null) {
                    Text(
                        text = qualifier,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (showArabic) {
                // ArabicText centres by default, which is right for a reader page and wrong in a
                // left-aligned row — it would float the Arabic away from the name above it.
                ArabicText(
                    text = getArabicPrayerName(type),
                    size = ArabicTextSize.SMALL,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                )
            }
        }

        Text(
            text = time,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isNext) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isNext) prayerColor else MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ==================== PREVIEWS ====================

@Composable
private fun NimazPrayerRowShowcase() {
    Column(modifier = Modifier.padding(16.dp)) {
        NimazPrayerRow(PrayerType.FAJR, "Fajr", "05:12", isPassed = true)
        NimazPrayerRow(PrayerType.SUNRISE, "Sunrise", "06:48", isPassed = true, showArabic = false)
        NimazPrayerRow(PrayerType.DHUHR, "Dhuhr", "13:20", qualifier = "Jumu'ah", isPassed = true)
        NimazPrayerRow(PrayerType.ASR, "Asr", "17:13", isNext = true)
        NimazPrayerRow(PrayerType.MAGHRIB, "Maghrib", "20:04")
        NimazPrayerRow(PrayerType.ISHA, "Isha", "21:38")
    }
}

@Preview(showBackground = true, widthDp = 360, name = "NimazPrayerRow — Light")
@Composable
private fun NimazPrayerRowLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { NimazPrayerRowShowcase() }
}

@Preview(
    showBackground = true, widthDp = 360, name = "NimazPrayerRow — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
@Composable
private fun NimazPrayerRowDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { NimazPrayerRowShowcase() }
}
