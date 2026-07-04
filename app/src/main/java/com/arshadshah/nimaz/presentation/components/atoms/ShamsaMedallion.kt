package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * A 12-lobe shamsa medallion with a number at its centre — the same ornament
 * the surah-header cartouche anchors on its left tip, extracted so the surah
 * list, grids and header all share one mark.
 *
 * Drawn with [scallopPath] + [circlePath] so the geometry matches the header
 * exactly: a gold scalloped rim (over a faint gold wash) and a teal inner ring.
 *
 * @param number surah / juz / page number shown at the centre
 * @param useArabicIndicNumerals render the number as Arabic-Indic digits (٠-٩)
 */
@Composable
fun ShamsaMedallion(
    number: Int,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    gold: Color = NimazColors.Gold500,
    teal: Color = NimazColors.Primary,
    numberColor: Color = teal,
    useArabicIndicNumerals: Boolean = false,
    numberStyle: TextStyle = MaterialTheme.typography.titleSmall,
) {
    val label = if (useArabicIndicNumerals) toArabicNumber(number) else number.toString()
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(this.size.width / 2f, this.size.height / 2f)
            // Lobes bulge to ~1.17·r, so inset r a touch to keep the apexes inside the box.
            val r = this.size.minDimension / 2f / 1.18f
            val med = scallopPath(c, r, lobes = 12, anchor = 0.86f, control = 1.17f)
            drawPath(med, gold.copy(alpha = 0.08f))
            drawPath(med, gold, style = Stroke(1.4.dp.toPx(), join = StrokeJoin.Round))
            drawPath(circlePath(c, r * 0.66f), teal.copy(alpha = 0.8f), style = Stroke(0.9.dp.toPx()))
        }
        Text(
            text = label,
            color = numberColor,
            style = numberStyle,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * A small solid diamond — the cartouche's bud/finial mark, reused as a floret
 * on the Basmala line and in the mushaf-frame ornamental dividers.
 */
@Composable
fun DiamondFloret(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 7.dp,
    alpha: Float = 0.8f,
) {
    Canvas(modifier.size(size)) {
        drawPath(
            diamondPath(
                Offset(this.size.width / 2f, this.size.height / 2f),
                this.size.minDimension / 2f,
            ),
            color.copy(alpha = alpha),
        )
    }
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true, name = "Shamsa Medallion")
@Composable
private fun ShamsaMedallionPreview() {
    NimazTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShamsaMedallion(number = 1)
            ShamsaMedallion(number = 36)
            ShamsaMedallion(number = 114)
            ShamsaMedallion(number = 30, useArabicIndicNumerals = true, size = 48.dp)
        }
    }
}
