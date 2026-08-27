package com.arshadshah.nimaz.presentation.components.atoms

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * A swatch and a label — one row of a legend.
 *
 * The swatch is [NimazStatusDot], so a legend can explain a ring as well as a disc. It drew its
 * own circle before, which meant a grid marking "recorded as not happening" with a ring had no
 * way to say so underneath.
 *
 * @param color swatch colour, matching the indicator it explains.
 * @param label what the swatch means.
 * @param style must match the indicator style used in the thing being explained.
 * @param dotSize swatch diameter. Kept as a raw [Dp] for the callers that already pass one.
 */
@Composable
fun NimazLegendItem(
    color: Color,
    label: String,
    modifier: Modifier = Modifier,
    dotSize: Dp = 8.dp,
    style: NimazStatusDotStyle = NimazStatusDotStyle.FILLED,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NimazStatusDot(
            color = color,
            style = style,
            diameter = dotSize,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ==================== PREVIEWS ====================

@Composable
private fun NimazLegendItemShowcase() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            NimazLegendItem(color = NimazColors.Success, label = "Fasted")
            NimazLegendItem(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                label = "Not fasting",
                style = NimazStatusDotStyle.OUTLINED,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            NimazLegendItem(color = NimazColors.FastingColors.Exempted, label = "Exempt")
            NimazLegendItem(color = NimazColors.FastingColors.Makeup, label = "Owed")
        }
    }
}

@Preview(showBackground = true, widthDp = 320, name = "NimazLegendItem — Light")
@Composable
private fun NimazLegendItemLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { NimazLegendItemShowcase() }
}

@Preview(
    showBackground = true, widthDp = 320, name = "NimazLegendItem — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun NimazLegendItemDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { NimazLegendItemShowcase() }
}
