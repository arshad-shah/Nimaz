package com.arshadshah.nimaz.presentation.components.molecules

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeEmphasis
import androidx.annotation.FloatRange
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.lerp
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode
import com.arshadshah.nimaz.presentation.theme.ZakatSurfaceColors

/** One figure in the hero's tile row. */
data class ZakatHeroStat(
    val value: String,
    val label: String,
    /** Paints [value] in the nisab gold — use for the nisab threshold only. */
    val accented: Boolean = false,
)

/** The status pill on the plinth. [met] drives gold vs neutral. */
data class ZakatHeroStatus(
    val text: String,
    val met: Boolean,
)

/** Vertical overlap of the tile row onto the plinth. */
private val TileOverlap = 22.dp

/** Extra plinth bottom padding so the tiles overlap dead space, not the subtitle. */
private val PlinthBottomPadding = 34.dp

/**
 * The shared summary hero for the zakat surfaces — a deep-teal **plinth** carrying
 * the headline amount, with a row of neutral **stat tiles** riding up over its
 * lower edge.
 *
 * Replaces the two hand-rolled gold-gradient cards (`ZakatResultSummaryCard` and
 * `TotalPaidSummaryCard`) that were byte-for-byte duplicates of each other, each
 * painting a `Brush.linearGradient` into a [NimazTone.TRANSPARENT] card with ink
 * pinned to `Neutral900`. Colour policy now lives in [ZakatSurfaceColors].
 *
 * Separation between the amount and the figures is carried by **elevation and
 * colour, not rules** — there is deliberately no divider here, unlike the older
 * `StatsSummaryCard` stat row. The overlap follows `HomeHero`, which lifts a card
 * over its backdrop the same way.
 *
 * @param label small eyebrow above the amount.
 * @param amount the headline figure, already formatted for the current locale.
 * @param subtitle one line under the amount.
 * @param status optional pill on the plinth's top-right.
 * @param stats up to three figures; pass empty for a plinth-only hero.
 * @param muteAmount renders [amount] at reduced emphasis — for a zero/ineligible
 *   state, where a full-strength figure would overstate a number that is not owed.
 * @param collapseProgress 0f full, 1f collapsed — for a hero pinned above a scrolling form.
 *   The tiles and the subtitle fold away and the amount shrinks; **the amount never
 *   disappears**, because on the calculator it is the one figure the whole task is about, and
 *   losing sight of it mid-entry is what a scrolling hero got wrong.
 */
@Composable
fun ZakatSummaryHero(
    label: String,
    amount: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    status: ZakatHeroStatus? = null,
    stats: List<ZakatHeroStat> = emptyList(),
    muteAmount: Boolean = false,
    @FloatRange(from = 0.0, to = 1.0) collapseProgress: Float = 0f,
) {
    val collapse = collapseProgress.coerceIn(0f, 1f)
    // Interpolated rather than switched between two layouts: a hard swap at the threshold makes
    // the whole form jump by the tile row's height, which reads as a glitch rather than a
    // collapse. `showStats` still gates the tiles entirely, so at rest they cost nothing.
    val showStats = stats.isNotEmpty() && collapse < 1f
    // One announcement for the whole plinth: TalkBack should read "Zakat due,
    // $1,284.50, above nisab" as a phrase, not as four unrelated fragments.
    val plinthDescription = if (status != null) {
        stringResource(R.string.zakat_a11y_plinth_status_format, label, amount, status.text)
    } else {
        stringResource(R.string.zakat_a11y_stat_format, label, amount)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        NimazCard(
            modifier = Modifier.fillMaxWidth(),
            style = NimazCardStyle.GRADIENT,
            shape = RoundedCornerShape(18.dp),
            gradient = ZakatSurfaceColors.plinthGradient,
            colors = NimazCardDefaults.colors(
                container = Color.Transparent,
                content = ZakatSurfaceColors.plinthInk,
                border = ZakatSurfaceColors.plinthBorder,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 18.dp,
                        end = 18.dp,
                        top = lerp(18.dp, 12.dp, collapse),
                        // Only reserve dead space when tiles will actually cover it.
                        bottom = if (!showStats) lerp(18.dp, 12.dp, collapse)
                        else lerp(PlinthBottomPadding, 18.dp, collapse),
                    )
                    .clearAndSetSemantics { contentDescription = plinthDescription }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = label.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        color = ZakatSurfaceColors.plinthAccent,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    status?.let {
                        NimazBadge(
                            text = it.text,
                            tone = if (it.met) NimazTone.ACCENT else NimazTone.NEUTRAL,
                            emphasis = NimazBadgeEmphasis.OUTLINED,
                            size = NimazBadgeSize.SMALL,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(lerp(10.dp, 2.dp, collapse)))

                val displaySmall = MaterialTheme.typography.displaySmall
                val titleLarge = MaterialTheme.typography.titleLarge
                Text(
                    text = amount,
                    style = displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = lerp(displaySmall.fontSize, titleLarge.fontSize, collapse),
                        lineHeight = lerp(40.sp, 28.sp, collapse),
                    ),
                    color = if (muteAmount) {
                        ZakatSurfaceColors.plinthInk.copy(alpha = 0.42f)
                    } else {
                        ZakatSurfaceColors.plinthInk
                    },
                )

                // The subtitle is the first thing to go: it explains how the figure is derived,
                // which is worth reading once and not worth the height on every scroll.
                if (collapse < 1f) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = ZakatSurfaceColors.plinthInkMuted.copy(alpha = 1f - collapse),
                    )
                }
            }
        }

        if (showStats) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = -TileOverlap)
                    .alpha(1f - collapse)
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                stats.forEach { stat ->
                    StatTile(stat = stat, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    stat: ZakatHeroStat,
    modifier: Modifier = Modifier,
) {
    NimazCard(
        modifier = modifier,
        style = NimazCardStyle.ELEVATED,
        tone = NimazTone.NEUTRAL,
        shape = RoundedCornerShape(12.dp),
        elevation = 3.dp,
    ) {
        val statDescription =
            stringResource(R.string.zakat_a11y_stat_format, stat.label, stat.value)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 6.dp)
                .clearAndSetSemantics {
                    contentDescription = statDescription
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stat.value,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = if (stat.accented) {
                    ZakatSurfaceColors.nisabInk
                } else {
                    ZakatSurfaceColors.tileInk
                },
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stat.label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                color = ZakatSurfaceColors.tileLabelInk,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

// ==================== PREVIEWS ====================

@Composable
private fun ZakatSummaryHeroShowcase() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ZakatSummaryHero(
            label = "Zakat Due",
            amount = "$1,284.50",
            subtitle = "2.5% of eligible wealth",
            status = ZakatHeroStatus(text = "Above nisab", met = true),
            stats = listOf(
                ZakatHeroStat("$51,380", "Net"),
                ZakatHeroStat("$5,847", "Nisab · Gold", accented = true),
                ZakatHeroStat("2.5%", "Rate"),
            ),
        )
        ZakatSummaryHero(
            label = "Zakat Due",
            amount = "$0.00",
            subtitle = "No zakat is due below the nisab threshold",
            status = ZakatHeroStatus(text = "Below nisab", met = false),
            muteAmount = true,
            stats = listOf(
                ZakatHeroStat("$0.00", "Net"),
                ZakatHeroStat("$5,847", "Nisab · Gold", accented = true),
                ZakatHeroStat("2.5%", "Rate"),
            ),
        )
        ZakatSummaryHero(
            label = "Total Paid",
            amount = "$4,120.00",
            subtitle = "3 calculations recorded",
        )
    }
}

@Preview(showBackground = true, name = "Zakat hero — Light")
@Composable
private fun ZakatSummaryHeroLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { ZakatSummaryHeroShowcase() }
}

@Preview(
    showBackground = true, name = "Zakat hero — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun ZakatSummaryHeroDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { ZakatSummaryHeroShowcase() }
}

/** Linear interpolation for a font size, which `Dp.lerp` cannot do. */
private fun lerp(start: TextUnit, stop: TextUnit, fraction: Float): TextUnit =
    (start.value + (stop.value - start.value) * fraction).sp
