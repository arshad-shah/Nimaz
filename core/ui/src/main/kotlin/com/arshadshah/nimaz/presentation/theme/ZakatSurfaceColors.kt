package com.arshadshah.nimaz.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.arshadshah.nimaz.presentation.theme.ZakatSurfaceColors.nisabInk

/**
 * The single source of truth for the colours of the **Zakat summary surfaces** —
 * the calculator hero and the history total-paid card.
 *
 * Both cards previously hand-rolled a `GoldPure → GoldDark` gradient inside a
 * [NimazTone.TRANSPARENT] card and pinned their ink to [NimazColors.Neutral900],
 * so they rendered identically in light and dark and had no theme identity at
 * all. `GoldPure` (#FFD700) on a pale surface is exactly the contrast failure
 * [QuranSurfaceColors] was written to fix; this object mirrors that file's
 * approach for the zakat surfaces.
 *
 * Contrast note: the plinth is a fixed deep-teal gradient in both themes, so ink
 * *on the plinth* is theme-independent. Only roles that land on the page or on a
 * neutral tile resolve per theme — and [nisabInk] paints a small numeral, so it
 * takes the stricter text-grade gold (`Amber700`) on light rather than the
 * ornament-grade `GoldDark`.
 */
object ZakatSurfaceColors {

    /**
     * Read from [LocalIsDarkTheme], which [NimazTheme] resolves from the user's
     * [ThemeMode] — not inferred from surface luminance, which misreads dynamic
     * colour schemes whose surface sits near the midpoint.
     */
    private val isDark: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalIsDarkTheme.current

    /** The hero plinth gradient — shared with the Quran banners. */
    val plinthGradient: List<Color>
        @Composable @ReadOnlyComposable
        get() = NimazColors.QuranColors.BannerGradient

    /** Hairline around the plinth, matching the Quran banner border. */
    val plinthBorder: Color
        @Composable @ReadOnlyComposable
        get() = NimazColors.QuranColors.BannerBorder

    /** Eyebrow label and accent ink on the plinth. Fixed: the plinth is dark in both themes. */
    val plinthAccent: Color
        @Composable @ReadOnlyComposable
        get() = NimazPalette.Gold500

    /** The amount itself, on the plinth. */
    val plinthInk: Color
        @Composable @ReadOnlyComposable
        get() = Color.White

    /** Secondary/subtitle ink on the plinth. */
    val plinthInkMuted: Color
        @Composable @ReadOnlyComposable
        get() = Color.White.copy(alpha = 0.5f)

    /**
     * The nisab figure on a neutral stat tile — a small numeral, so it needs the
     * text-grade ratio: `Amber700` on light rather than the ornament gold.
     */
    val nisabInk: Color
        @Composable @ReadOnlyComposable
        get() = if (isDark) NimazPalette.Gold500 else NimazPalette.Amber700

    /** Value ink on a neutral stat tile. */
    val tileInk: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSurface

    /** Caption ink on a neutral stat tile. */
    val tileLabelInk: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSurfaceVariant
}
