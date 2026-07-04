package com.arshadshah.nimaz.presentation.components.molecules

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.BISMILLAH_TEXT
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.components.atoms.toArabicNumber
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode
import kotlin.math.cos
import kotlin.math.sin

/* ------------------------------------------------------------------
 * Theme tokens — the cartouche is a fixed gold-on-teal "manuscript"
 * ornament, so it pins to the brand ramps rather than the running
 * colorScheme (mirrors how MushafSurahHeader pins its own palette).
 * ------------------------------------------------------------------ */
private val CartoucheGold = NimazColors.Gold500
private val CartoucheTeal = NimazColors.Primary400
private val CartoucheMedallionFill = NimazColors.Primary950
private val CartouchePanelGradient = listOf(NimazColors.Primary800, NimazColors.Primary950)

/**
 * Surah header in the style of a manuscript unwan cartouche:
 * an ogee-pointed panel with a gold outer stroke and teal echo stroke,
 * a 12-lobe shamsa number medallion anchored on the left tip,
 * and a bud finial on the right tip. Ayah count and revelation type
 * render as small badges under the surah name.
 *
 * Reuses [ArabicText] for the surah name, [NimazBadge] for the badges,
 * [toArabicNumber] for optional Arabic-Indic numerals and the
 * [NimazColors] brand ramps for its palette.
 *
 * @param surah the surah to render a header for
 */
@Composable
fun SurahHeaderCartouche(
    surah: Surah,
    modifier: Modifier = Modifier,
    height: Dp = 96.dp,
    useArabicIndicNumerals: Boolean = false,
    // Basmala precedes every surah except Al-Fatiha (1, it opens with it) and At-Tawbah (9).
    showBismillah: Boolean = surah.number != 1 && surah.number != 9,
) {
    SurahHeaderCartouche(
        number = surah.number,
        englishName = surah.nameEnglish,
        arabicName = surah.nameArabic,
        ayahCount = surah.ayahCount,
        revelationType = surah.revelationType.displayLabel(),
        modifier = modifier,
        height = height,
        useArabicIndicNumerals = useArabicIndicNumerals,
        showBismillah = showBismillah,
    )
}

/**
 * Decorative surah header with explicit parameters.
 *
 * @param revelationType free-text label rendered in the gold badge (e.g. "Meccan")
 */
@Composable
fun SurahHeaderCartouche(
    number: Int,
    englishName: String,
    arabicName: String,
    ayahCount: Int,
    revelationType: String,
    modifier: Modifier = Modifier,
    height: Dp = 96.dp,
    useArabicIndicNumerals: Boolean = false,
    showBismillah: Boolean = false,
    gold: Color = CartoucheGold,
    teal: Color = CartoucheTeal,
    medallionFill: Color = CartoucheMedallionFill,
    panelGradient: List<Color> = CartouchePanelGradient,
    titleColor: Color = Color.White,
) {
    val medR = height * 0.40f              // medallion radius
    val medCx = medR + 6.dp                // medallion centre x
    val label = if (useArabicIndicNumerals) toArabicNumber(number) else number.toString()

    Column(modifier = modifier.fillMaxWidth()) {
    Box(modifier = Modifier.fillMaxWidth().height(height)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cy = h / 2f
            val t = h * 0.60f              // ogee tip extension
            val medRp = medR.toPx()
            val medCxp = medCx.toPx()

            // panel — its left tip hides behind the medallion
            val panel = cartouchePath(
                x0 = medCxp - medRp * 0.3f, y0 = 4.dp.toPx(),
                x1 = w - 14.dp.toPx(), y1 = h - 4.dp.toPx(), t = t,
            )
            drawPath(panel, Brush.verticalGradient(panelGradient))
            drawPath(panel, gold, style = Stroke(1.4.dp.toPx(), join = StrokeJoin.Round))

            // inner teal echo stroke
            drawPath(
                cartouchePath(
                    x0 = medCxp - medRp * 0.3f + 6.dp.toPx(), y0 = 9.dp.toPx(),
                    x1 = w - 20.dp.toPx(), y1 = h - 9.dp.toPx(), t = t * 0.86f,
                ),
                teal.copy(alpha = 0.55f),
                style = Stroke(0.9.dp.toPx()),
            )

            // right-tip finial: stem + gold bud
            drawLine(
                gold,
                Offset(w - 14.dp.toPx(), cy), Offset(w - 7.dp.toPx(), cy),
                strokeWidth = 1.2.dp.toPx(),
            )
            drawPath(diamondPath(Offset(w - 5.dp.toPx(), cy), 3.5.dp.toPx()), gold)

            // shamsa number medallion on the left tip
            val medC = Offset(medCxp, cy)
            val med = scallopPath(medC, medRp, lobes = 12, anchor = 0.86f, control = 1.17f)
            drawPath(med, teal.copy(alpha = 0.15f), style = Stroke(5.dp.toPx(), join = StrokeJoin.Round)) // soft glow
            drawPath(med, medallionFill)
            drawPath(med, gold, style = Stroke(1.4.dp.toPx(), join = StrokeJoin.Round))
            drawPath(circlePath(medC, medRp * 0.68f), teal, style = Stroke(0.9.dp.toPx()))
        }

        // number over the medallion
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = medCx - medR)
                .size(medR * 2),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, color = teal, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        // name + badges, Arabic name at the end
        Row(
            Modifier
                .fillMaxSize()
                .padding(start = medCx + medR + 14.dp, end = height * 0.72f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(englishName, color = titleColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Row(
                    Modifier.padding(top = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    NimazBadge(
                        text = revelationType,
                        size = NimazBadgeSize.SMALL,
                        backgroundColor = gold,
                        outlined = true,
                    )
                    NimazBadge(
                        text = "$ayahCount Ayahs",
                        size = NimazBadgeSize.SMALL,
                        backgroundColor = teal,
                        outlined = true,
                    )
                }
            }
            ArabicText(
                text = arabicName,
                size = ArabicTextSize.LARGE,
                color = gold,
                fontWeight = FontWeight.Bold,
            )
        }
    }

        if (showBismillah) {
            Spacer(Modifier.height(16.dp))
            BismillahRow(color = gold)
        }
    }
}

/**
 * The Basmala as a plain centred line, flanked by two small diamond florets
 * that reuse the cartouche's bud-finial mark — the modern printed-mushaf
 * treatment (see [SurahHeaderCartouche]).
 */
@Composable
private fun BismillahRow(color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DiamondFloret(color = color)
        ArabicText(
            text = BISMILLAH_TEXT,
            size = ArabicTextSize.LARGE,
            color = color,
        )
        DiamondFloret(color = color)
    }
}

/** A small solid diamond — the same mark as the cartouche's right-tip bud. */
@Composable
private fun DiamondFloret(color: Color, modifier: Modifier = Modifier, size: Dp = 7.dp) {
    Canvas(modifier.size(size)) {
        drawPath(
            diamondPath(Offset(this.size.width / 2f, this.size.height / 2f), this.size.minDimension / 2f),
            color.copy(alpha = 0.8f),
        )
    }
}

private fun RevelationType.displayLabel(): String = when (this) {
    RevelationType.MECCAN -> "Meccan"
    RevelationType.MEDINAN -> "Medinan"
}

/* ------------------------------------------------------------------
 * Geometry
 * ------------------------------------------------------------------ */

private fun polar(center: Offset, radius: Float, degrees: Float): Offset {
    val rad = Math.toRadians(degrees.toDouble())
    return Offset(
        center.x + radius * cos(rad).toFloat(),
        center.y + radius * sin(rad).toFloat(),
    )
}

/** Ring of outward lobes; lobe apex lands ~ on radius [r]. */
private fun scallopPath(c: Offset, r: Float, lobes: Int, anchor: Float, control: Float): Path {
    val step = 360f / lobes
    return Path().apply {
        val start = polar(c, r * anchor, -90f)
        moveTo(start.x, start.y)
        for (i in 0 until lobes) {
            val cp = polar(c, r * control, -90f + (i + 0.5f) * step)
            val p = polar(c, r * anchor, -90f + (i + 1f) * step)
            quadraticTo(cp.x, cp.y, p.x, p.y)
        }
        close()
    }
}

/** Ogee-pointed panel between x0..x1, y0..y1; [t] is the tip extension. */
private fun cartouchePath(x0: Float, y0: Float, x1: Float, y1: Float, t: Float): Path {
    val cy = (y0 + y1) / 2f
    val bl = x0 + t
    val br = x1 - t
    val bow = (y1 - y0) * 0.30f
    return Path().apply {
        moveTo(bl, y0)
        lineTo(br, y0)
        cubicTo(br + t * 0.55f, y0, x1 - t * 0.25f, cy - bow, x1, cy)
        cubicTo(x1 - t * 0.25f, cy + bow, br + t * 0.55f, y1, br, y1)
        lineTo(bl, y1)
        cubicTo(bl - t * 0.55f, y1, x0 + t * 0.25f, cy + bow, x0, cy)
        cubicTo(x0 + t * 0.25f, cy - bow, bl - t * 0.55f, y0, bl, y0)
        close()
    }
}

private fun diamondPath(c: Offset, r: Float): Path = Path().apply {
    moveTo(c.x, c.y - r); lineTo(c.x + r, c.y)
    lineTo(c.x, c.y + r); lineTo(c.x - r, c.y)
    close()
}

private fun circlePath(c: Offset, r: Float): Path = Path().apply {
    addOval(Rect(c.x - r, c.y - r, c.x + r, c.y + r))
}

/* ------------------------------------------------------------------
 * Previews
 * ------------------------------------------------------------------ */

@Preview(showBackground = true, backgroundColor = 0xFF0A0A08, widthDp = 420, name = "Cartouche — surahs")
@Composable
private fun SurahHeaderCartouchePreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        Column {
            SurahHeaderCartouche(
                number = 1,
                englishName = "Al-Fatiha",
                arabicName = "الفاتحة",
                ayahCount = 7,
                revelationType = "Meccan",
            )
            SurahHeaderCartouche(
                number = 2,
                englishName = "Al-Baqarah",
                arabicName = "البقرة",
                ayahCount = 286,
                revelationType = "Medinan",
            )
            SurahHeaderCartouche(
                number = 114,
                englishName = "An-Nas",
                arabicName = "الناس",
                ayahCount = 6,
                revelationType = "Meccan",
                useArabicIndicNumerals = true,
            )
        }
    }
}

@Preview(
    showBackground = true, backgroundColor = 0xFF0A0A08, widthDp = 420,
    name = "Cartouche — from Surah model",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
@Composable
private fun SurahHeaderCartoucheModelPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            // Fatiha (1): showBismillah defaults false — opens the mushaf itself.
            SurahHeaderCartouche(surah = sampleSurahFatihah)
            // Baqarah (2): showBismillah defaults true — Basmala renders below.
            SurahHeaderCartouche(surah = sampleSurahBaqarah, useArabicIndicNumerals = true)
        }
    }
}

@Preview(
    showBackground = true, backgroundColor = 0xFF0A0A08, widthDp = 420,
    name = "Cartouche — Bismillah band",
)
@Composable
private fun SurahHeaderCartoucheBismillahPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        SurahHeaderCartouche(
            number = 2,
            englishName = "Al-Baqarah",
            arabicName = "البقرة",
            ayahCount = 286,
            revelationType = "Medinan",
            showBismillah = true,
        )
    }
}
