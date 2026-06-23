package com.arshadshah.nimaz.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Central home for small, bespoke **decorative** colours that don't belong to the
 * semantic ramps in [NimazPalette] — single-use art accents owned by one component.
 * Grouped by owner so component files never hold raw `Color(0xFF…)` literals.
 *
 * (Large art sets live in their own files: [SkyColors], [BeadColors], [GlassColors].)
 */

/** Decorative gradient/accent variants for `NimazCard`. */
object CardArtColors {
    val IndigoGradientStart = Color(0xFF5C6BC0)
    val IndigoGradientEnd = Color(0xFF9FA8DA)
    val AmberPrimary = Color(0xFFFFB74D)
    val AmberSecondary = Color(0xFFFFE0B2)
}

/** Qibla compass needle facets and capsule/dial backgrounds. */
object CompassArtColors {
    val NeedleTop = Color(0xFF23252B)
    val NeedleSide = Color(0xFF101115)
    val NeedleFront = Color(0xFF181A1F)
    val GoldCapsuleBackground = Color(0xFF1A160B)
    val DialBackground = Color(0xFF120F0A)
}

/** Asma-ul-Husna names medallion gradient. */
object NamesArtColors {
    val MedallionGradientStart = Color(0xFF9575FF)
}

/** Tafseer word-highlight fallback tint when no rule colour is supplied. */
object HighlightArtColors {
    val FallbackYellow = Color(0xFFFDE68A)
}

/** Illuminated onboarding artwork (mihrab niche, emblems, calligraphic tints). */
object OnboardingArtColors {
    val GoldDeep = Color(0xFFB8860B)
    val Cream = Color(0xFFF5E6B8)
    val TextSoft = Color(0xFFCFE3DF)
    val Niche = Color(0xFF0C2F2C)
    val TealTop = Color(0xFF14463F)
}

/** Loose one-off decorative accents, each owned by a single component. */
object MiscArtColors {
    val TopBarBlue = Color(0xFF3E86C9)         // HomeDynamicTopBar
    val MutedTextGray = Color(0xFF737373)      // QuranInfoAtoms label
    val PageIndicatorGold = Color(0xFFE0B057)  // NimazPageIndicator active dot
}
