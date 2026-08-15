package com.arshadshah.nimaz.presentation.components.atoms

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.foundation.geometry.circlePath
import com.arshadshah.nimaz.presentation.foundation.geometry.diamondPath
import com.arshadshah.nimaz.presentation.foundation.geometry.scallopPath
import com.arshadshah.nimaz.presentation.theme.LocalIsDarkTheme
import com.arshadshah.nimaz.presentation.theme.LocalPatternStyle
import com.arshadshah.nimaz.presentation.theme.LocalShowIslamicPatterns
import com.arshadshah.nimaz.presentation.theme.NimazPalette
import com.arshadshah.nimaz.presentation.theme.NimazPatternStyle
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * Ornament opacity. Deliberately low — this sits behind body text on every screen
 * — but raised from the original barely-there values, which made the whole feature
 * read as broken: readers toggled it and saw no change. This is the ceiling before
 * the geometry starts competing with text.
 */
private const val PATTERN_ALPHA_LIGHT = 0.08f
private const val PATTERN_ALPHA_DARK = 0.07f

/** Lattice/field pitch in dp. */
private const val LATTICE_PITCH_DP = 30f
private const val FIELD_PITCH_DP = 34f

/**
 * The ornament colour: teal on light, gold on dark — matching how the Quran
 * surfaces already split ornament gold by theme.
 */
private val patternColor: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) {
        NimazPalette.Gold500.copy(alpha = PATTERN_ALPHA_DARK)
    } else {
        NimazPalette.Teal700.copy(alpha = PATTERN_ALPHA_LIGHT)
    }

/**
 * Wraps [content] on a page background carrying a decorative Islamic-geometry
 * ornament.
 *
 * **This is the single read site for [LocalShowIslamicPatterns].** The Appearance
 * setting was previously wired all the way from DataStore to a CompositionLocal
 * and then read by nothing at all, so the toggle did nothing. Screens must not
 * check the preference themselves — wrap in this and the preference is honoured
 * for free, which is what stops the dead-toggle bug recurring somewhere new.
 *
 * The geometry comes from the same builders as the Quran ornaments
 * ([com.arshadshah.nimaz.presentation.foundation.geometry.scallopPath], [com.arshadshah.nimaz.presentation.foundation.geometry.diamondPath]) so the ornament language cannot drift.
 *
 * Drawing is cached via [drawWithCache]: the [Path]s are rebuilt only when the
 * size, style or colour changes, never per frame.
 *
 * @param style defaults to [LocalPatternStyle]; pass explicitly to override for
 *   one screen (or to force a look in a preview).
 * @param enabled defaults to the user's preference.
 * @param alphaScale multiplies the base ornament opacity. Leave at 1 for real
 *   screens; the picker swatches pass a larger value so the geometry is legible at
 *   thumbnail size — at the on-screen 8% you cannot tell one style from another in
 *   an 88dp square. Preview/picker use only.
 */
@Composable
fun NimazPatternBackground(
    modifier: Modifier = Modifier,
    style: NimazPatternStyle = LocalPatternStyle.current,
    enabled: Boolean = LocalShowIslamicPatterns.current,
    surface: Color = MaterialTheme.colorScheme.background,
    alphaScale: Float = 1f,
    content: @Composable BoxScope.() -> Unit,
) {
    val base = patternColor
    val ornament = if (alphaScale == 1f) {
        base
    } else {
        base.copy(alpha = (base.alpha * alphaScale).coerceIn(0f, 1f))
    }
    val effectiveStyle = if (enabled) style else NimazPatternStyle.NONE

    Box(
        modifier = modifier
            .background(surface)
            .then(
                if (effectiveStyle == NimazPatternStyle.NONE) {
                    Modifier
                } else {
                    Modifier.drawWithCache {
                        val paths = buildPatternPaths(effectiveStyle, size, density = this.density)
                        val stroke = Stroke(width = 1.dp.toPx())
                        onDrawBehind {
                            paths.forEach { drawPath(it, color = ornament, style = stroke) }
                        }
                    }
                }
            ),
        content = content,
    )
}

/**
 * Builds the ornament geometry for [style] at [size].
 *
 * Kept as a pure function returning finished [Path]s so the caller can cache the
 * whole result; it must never be called from inside a draw lambda.
 */
private fun buildPatternPaths(
    style: NimazPatternStyle,
    size: Size,
    density: Float,
): List<Path> = when (style) {
    NimazPatternStyle.NONE -> emptyList()
    NimazPatternStyle.CORNER_MEDALLION -> cornerMedallion(size)
    NimazPatternStyle.LATTICE -> lattice(size, density)
    NimazPatternStyle.STAR_FIELD -> starField(size, density)
    NimazPatternStyle.ATELIER -> starField(size, density) + cornerMedallion(size)
}

/** A shamsa + concentric rings, centred just off the top-right corner. */
private fun cornerMedallion(size: Size): List<Path> {
    if (size.minDimension <= 0f) return emptyList()
    val r = size.minDimension * 0.62f
    val centre = Offset(size.width + r * 0.22f, -r * 0.22f)
    return listOf(
        scallopPath(centre, r, lobes = 16, anchor = 0.92f, control = 1.0f),
        circlePath(centre, r * 0.74f),
        circlePath(centre, r * 0.52f),
        scallopPath(centre, r * 0.36f, lobes = 12, anchor = 0.9f, control = 1.0f),
    )
}

/** A repeating diamond lattice with a bud at each vertex. */
private fun lattice(size: Size, density: Float): List<Path> {
    val pitch = LATTICE_PITCH_DP * density
    if (pitch <= 0f || size.width <= 0f) return emptyList()
    val paths = mutableListOf<Path>()
    var y = 0f
    while (y < size.height + pitch) {
        var x = 0f
        while (x < size.width + pitch) {
            paths += diamondPath(Offset(x, y), pitch * 0.5f)
            x += pitch
        }
        y += pitch
    }
    return paths
}

/** Sparse buds on a staggered grid — texture without line work. */
private fun starField(size: Size, density: Float): List<Path> {
    val pitch = FIELD_PITCH_DP * density
    if (pitch <= 0f || size.width <= 0f) return emptyList()
    val paths = mutableListOf<Path>()
    var row = 0
    var y = pitch * 0.5f
    while (y < size.height + pitch) {
        val offsetX = if (row % 2 == 0) 0f else pitch * 0.5f
        var x = offsetX + pitch * 0.5f
        while (x < size.width + pitch) {
            paths += diamondPath(Offset(x, y), pitch * 0.075f)
            x += pitch
        }
        y += pitch
        row++
    }
    return paths
}

// ==================== PREVIEWS ====================

/**
 * One style at full screen size, with the content that actually stresses it: a
 * card, and body text sitting directly on the ornament.
 *
 * Preview-only sample text is intentionally literal here rather than pulled from
 * `strings.xml` — a preview is developer-facing scaffolding, never shipped UI, and
 * routing it through resources would add untranslatable entries to every locale.
 */
@Composable
private fun PatternSample(style: NimazPatternStyle) {
    NimazPatternBackground(
        modifier = Modifier.fillMaxSize(),
        style = style,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = style.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            NimazCard(style = NimazCardStyle.ELEVATED, tone = NimazTone.NEUTRAL) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "A card on the page",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = "Cards must still read as lifted off the background.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = "Body text directly on the patterned background — this is " +
                        "the legibility case that matters. If the ornament competes " +
                        "with these words at a glance, the alpha is too high.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private const val PREVIEW_W = 360
private const val PREVIEW_H = 320

@Preview(name = "1 · None — Light", widthDp = PREVIEW_W, heightDp = PREVIEW_H)
@Composable
private fun PatternNoneLight() =
    NimazTheme(themeMode = ThemeMode.LIGHT) { PatternSample(NimazPatternStyle.NONE) }

@Preview(name = "2 · Corner medallion — Light", widthDp = PREVIEW_W, heightDp = PREVIEW_H)
@Composable
private fun PatternCornerLight() =
    NimazTheme(themeMode = ThemeMode.LIGHT) { PatternSample(NimazPatternStyle.CORNER_MEDALLION) }

@Preview(name = "3 · Lattice — Light", widthDp = PREVIEW_W, heightDp = PREVIEW_H)
@Composable
private fun PatternLatticeLight() =
    NimazTheme(themeMode = ThemeMode.LIGHT) { PatternSample(NimazPatternStyle.LATTICE) }

@Preview(name = "4 · Star field — Light", widthDp = PREVIEW_W, heightDp = PREVIEW_H)
@Composable
private fun PatternStarFieldLight() =
    NimazTheme(themeMode = ThemeMode.LIGHT) { PatternSample(NimazPatternStyle.STAR_FIELD) }

@Preview(name = "5 · Atelier — Light", widthDp = PREVIEW_W, heightDp = PREVIEW_H)
@Composable
private fun PatternAtelierLight() =
    NimazTheme(themeMode = ThemeMode.LIGHT) { PatternSample(NimazPatternStyle.ATELIER) }

private const val NIGHT = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL

@Preview(name = "1 · None — Dark", widthDp = PREVIEW_W, heightDp = PREVIEW_H, uiMode = NIGHT)
@Composable
private fun PatternNoneDark() =
    NimazTheme(themeMode = ThemeMode.DARK) { PatternSample(NimazPatternStyle.NONE) }

@Preview(
    name = "2 · Corner medallion — Dark",
    widthDp = PREVIEW_W,
    heightDp = PREVIEW_H,
    uiMode = NIGHT
)
@Composable
private fun PatternCornerDark() =
    NimazTheme(themeMode = ThemeMode.DARK) { PatternSample(NimazPatternStyle.CORNER_MEDALLION) }

@Preview(name = "3 · Lattice — Dark", widthDp = PREVIEW_W, heightDp = PREVIEW_H, uiMode = NIGHT)
@Composable
private fun PatternLatticeDark() =
    NimazTheme(themeMode = ThemeMode.DARK) { PatternSample(NimazPatternStyle.LATTICE) }

@Preview(name = "4 · Star field — Dark", widthDp = PREVIEW_W, heightDp = PREVIEW_H, uiMode = NIGHT)
@Composable
private fun PatternStarFieldDark() =
    NimazTheme(themeMode = ThemeMode.DARK) { PatternSample(NimazPatternStyle.STAR_FIELD) }

@Preview(name = "5 · Atelier — Dark", widthDp = PREVIEW_W, heightDp = PREVIEW_H, uiMode = NIGHT)
@Composable
private fun PatternAtelierDark() =
    NimazTheme(themeMode = ThemeMode.DARK) { PatternSample(NimazPatternStyle.ATELIER) }

/** Verifies the user preference actually suppresses the ornament. */
@Preview(name = "6 · Preference off", widthDp = PREVIEW_W, heightDp = PREVIEW_H)
@Composable
private fun PatternDisabledPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        CompositionLocalProvider(LocalShowIslamicPatterns provides false) {
            PatternSample(NimazPatternStyle.ATELIER)
        }
    }
}
