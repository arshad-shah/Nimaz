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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.arshadshah.nimaz.presentation.components.atoms.DiamondFloret
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeEmphasis
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.components.atoms.cartouchePath
import com.arshadshah.nimaz.presentation.components.atoms.circlePath
import com.arshadshah.nimaz.presentation.components.atoms.diamondPath
import com.arshadshah.nimaz.presentation.components.atoms.scallopPath
import com.arshadshah.nimaz.presentation.components.atoms.toArabicNumber
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.QuranSurfaceColors
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/* ------------------------------------------------------------------
 * Theme tokens — the cartouche is a fixed gold-on-teal "manuscript"
 * ornament, so its teal panel pins to the brand ramps rather than the
 * running colorScheme. Its gold, however, routes through
 * [QuranSurfaceColors.frameGold] so the ornament stays legible when the page
 * behind it is light.
 * ------------------------------------------------------------------ */
private val CartoucheTeal = NimazColors.Primary400
private val CartoucheMedallionFill = NimazColors.Primary950
private val CartouchePanelGradient = listOf(NimazColors.Primary800, NimazColors.Primary950)

/**
 * Surah header in the style of a manuscript unwan cartouche:
 * an ogee-pointed panel with a gold outer stroke and teal echo stroke,
 * a 12-lobe shamsa number medallion anchored on the left tip,
 * and a bud finial on the right tip. Revelation type renders as
 * a small badge under the surah name.
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
    revelationType: String,
    modifier: Modifier = Modifier,
    height: Dp = 96.dp,
    useArabicIndicNumerals: Boolean = false,
    showBismillah: Boolean = false,
    gold: Color = QuranSurfaceColors.frameGold,
    teal: Color = CartoucheTeal,
    medallionFill: Color = CartoucheMedallionFill,
    panelGradient: List<Color> = CartouchePanelGradient,
    titleColor: Color = Color.White,
) {
    val medR = height * 0.40f              // medallion radius
    val medCx = medR + 6.dp                // medallion centre x
    val label = if (useArabicIndicNumerals) toArabicNumber(number) else number.toString()

    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(height)) {
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
                drawPath(
                    med,
                    teal.copy(alpha = 0.15f),
                    style = Stroke(5.dp.toPx(), join = StrokeJoin.Round)
                ) // soft glow
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

            // name + revelation badge, Arabic name at the end
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(start = medCx + medR + 14.dp, end = height * 0.72f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        englishName,
                        color = titleColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        Modifier.padding(top = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        NimazBadge(
                            text = revelationType,
                            size = NimazBadgeSize.SMALL,
                            colors = NimazBadgeDefaults.feature(
                                color = gold,
                                emphasis = NimazBadgeEmphasis.OUTLINED,
                            ),
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
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
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

private fun RevelationType.displayLabel(): String = when (this) {
    RevelationType.MECCAN -> "Meccan"
    RevelationType.MEDINAN -> "Medinan"
}

/* ------------------------------------------------------------------
 * Geometry lives in atoms/QuranOrnamentGeometry.kt (shared with
 * ShamsaMedallion and the rest of the Quran ornament language).
 * ------------------------------------------------------------------ */

/* ------------------------------------------------------------------
 * Previews
 * ------------------------------------------------------------------ */

@Preview(
    showBackground = true,
    backgroundColor = 0xFF0A0A08,
    widthDp = 420,
    name = "Cartouche — surahs"
)
@Composable
private fun SurahHeaderCartouchePreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        Column {
            SurahHeaderCartouche(
                number = 1,
                englishName = "Al-Fatiha",
                arabicName = "الفاتحة",
                revelationType = "Meccan",
            )
            SurahHeaderCartouche(
                number = 2,
                englishName = "Al-Baqarah",
                arabicName = "البقرة",
                revelationType = "Medinan",
            )
            SurahHeaderCartouche(
                number = 114,
                englishName = "An-Nas",
                arabicName = "الناس",
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
            revelationType = "Medinan",
            showBismillah = true,
        )
    }
}

/**
 * The plaque itself stays a saturated teal manuscript object in light mode
 * (decision: it is an illuminated header, not a themed card) — only its gold
 * ornament darkens so it survives sitting on a pale page.
 */
@Preview(showBackground = true, widthDp = 420, name = "Cartouche — Light theme page")
@Composable
private fun SurahHeaderCartoucheLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            SurahHeaderCartouche(surah = sampleSurahFatihah)
            SurahHeaderCartouche(surah = sampleSurahBaqarah, useArabicIndicNumerals = true)
        }
    }
}

// Sample data shared by header/page previews.
internal val sampleSurahFatihah = Surah(
    number = 1,
    nameArabic = "الفاتحة",
    nameEnglish = "Al-Fatihah",
    nameTransliteration = "The Opening",
    revelationType = RevelationType.MECCAN,
    ayahCount = 7,
    orderInMushaf = 1
)

internal val sampleSurahBaqarah = Surah(
    number = 2,
    nameArabic = "البقرة",
    nameEnglish = "Al-Baqarah",
    nameTransliteration = "The Cow",
    revelationType = RevelationType.MEDINAN,
    ayahCount = 286,
    orderInMushaf = 2
)
