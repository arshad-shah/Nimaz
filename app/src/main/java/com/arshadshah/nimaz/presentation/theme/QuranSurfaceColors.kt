package com.arshadshah.nimaz.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.arshadshah.nimaz.presentation.theme.QuranSurfaceColors.frameGold
import com.arshadshah.nimaz.presentation.theme.QuranSurfaceColors.medallionInk

/**
 * The single source of truth for the colours of the **Quran reading surfaces** —
 * the mushaf page, the tafseer study frame, their illuminated borders and the
 * shamsa page medallion.
 *
 * These surfaces used to hard-pin [NimazColors.Gold500] / `Primary800…950` /
 * `Color.White` and therefore had no light-mode identity at all. Every role
 * below now resolves per theme:
 *
 * - dark keeps the previous manuscript values (bright gold on a dark page);
 * - light re-uses the existing `surface` tokens for the tafseer frame, and darkens
 *   the gold so it stays legible on a pale page;
 * - the mushaf page itself no longer uses those tokens: it has its own [paper]
 *   register (see below), because it is imitating printed paper rather than
 *   presenting a card.
 *
 * Contrast note: [frameGold] is a border/ornament, so it only needs the 3:1
 * non-text ratio; [medallionInk] paints small numerals and therefore needs the
 * stricter text-grade ratio — hence the two different golds in light mode. The
 * [paper]/[paperLine]/[paperInk] trio is exempt from that 3:1 bar for [paperLine]:
 * it is a decorative hairline simulating a printed page's rule, not a component
 * boundary that needs to be found by contrast, so it is allowed to sit softer than
 * the ratio a real UI edge would owe.
 */
object QuranSurfaceColors {

    /** `true` when the running colour scheme is a dark one. */
    /**
     * Read from [LocalIsDarkTheme], which [NimazTheme] resolves from the user's
     * [ThemeMode] — not inferred from surface luminance, which misreads dynamic
     * colour schemes whose surface sits near the midpoint.
     */
    private val isDark: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalIsDarkTheme.current

    /** Outer illuminated border / ornament gold. */
    val frameGold: Color
        @Composable @ReadOnlyComposable
        get() = if (isDark) NimazPalette.Gold500 else NimazPalette.GoldDark

    /** Inner hairline border, echoing the cartouche's teal stroke. */
    val frameTeal: Color
        @Composable @ReadOnlyComposable
        get() = if (isDark) NimazColors.Primary700 else NimazColors.Primary600

    /** The page background the ayah text sits on. */
    val pageSurface: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.surface

    /**
     * The **paper register** — used only by the mushaf and 16-line reading modes.
     *
     * Held apart from [pageSurface] and the app's `surface` tokens on purpose: the
     * mushaf is imitating a printed page, and the rest of the Qur'an section is
     * moving to a flatter, cooler language. Reusing `surface` here would make the
     * page indistinguishable from every other card.
     *
     * Light values are a warm cream with a soft brown rule; dark values keep the
     * app's deep teal ground so the page does not glare at night.
     */
    val paper: Color
        @Composable @ReadOnlyComposable
        get() = if (isDark) Color(0xFF0B1D1B) else Color(0xFFFBF7EC)

    /** Hairline rules, the page frame and the cartouche stroke on [paper]. */
    val paperLine: Color
        @Composable @ReadOnlyComposable
        get() = if (isDark) Color(0xFF1F4340) else Color(0xFFE0D6BC)

    /** Arabic body text on [paper]. */
    val paperInk: Color
        @Composable @ReadOnlyComposable
        get() = if (isDark) Color(0xFFE8F1EF) else Color(0xFF1C1A14)

    /** Arabic body ink. */
    val ayahInk: Color
        @Composable @ReadOnlyComposable
        get() = if (isDark) {
            MaterialTheme.colorScheme.onBackground
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    /** Small numerals inside the shamsa medallion — needs text-grade contrast. */
    val medallionInk: Color
        @Composable @ReadOnlyComposable
        get() = if (isDark) NimazPalette.Gold500 else NimazPalette.Amber700
}
