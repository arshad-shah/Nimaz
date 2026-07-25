package com.arshadshah.nimaz.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Tier 1 of the Nimaz colour system — the **raw palette**.
 *
 * This is the *single source of truth* for every literal colour value in the app.
 * Nothing else should contain a `Color(0xFF…)` literal: semantic tokens
 * ([NimazColors]) and feature/art colour groups all reference constants defined
 * here. Colours are grouped by hue family and named `Family + shade`, roughly
 * following the Tailwind scale (50 = lightest … 950 = darkest).
 *
 * Values are preserved exactly as they were historically — this object only
 * removes duplication and gives every hue one canonical name; it does not change
 * any pixel on screen. A small number of values keep their Material-palette
 * origin (prefixed `Mat…`) where the app deliberately uses a Material tone next
 * to a Tailwind one.
 */
object NimazPalette {

    // ── Neutrals ──────────────────────────────────────────────────────────────
    val White = Color(0xFFFFFFFF)
    val Black = Color(0xFF000000)

    // Stone ramp — the app's true neutral scale (warm gray).
    val Stone50 = Color(0xFFFAFAF9)
    val Stone100 = Color(0xFFF5F5F4)
    val Stone200 = Color(0xFFE7E5E4)
    val Stone300 = Color(0xFFD6D3D1)
    val Stone400 = Color(0xFFA8A29E)
    val Stone500 = Color(0xFF78716C)
    val Stone600 = Color(0xFF57534E)
    val Stone700 = Color(0xFF44403C)
    val Stone800 = Color(0xFF292524)
    val Stone900 = Color(0xFF1C1917)
    val Stone950 = Color(0xFF0C0A09)

    // Cool/Material grays used by surfaces, outlines and disabled states.
    val GrayBg = Color(0xFFFAFAFA)       // light background
    val Gray100 = Color(0xFFF5F5F5)      // light surface-variant
    val Gray200 = Color(0xFFE0E0E0)      // outline / dark on-surface text
    val Gray300 = Color(0xFFD4D4D4)
    val Gray400 = Color(0xFFBDBDBD)
    val Gray500 = Color(0xFFB0B0B0)      // dark on-surface-variant
    val Gray600 = Color(0xFF9E9E9E)
    val Gray700 = Color(0xFF757575)      // light on-surface-variant
    val Ink = Color(0xFF1C1C1C)          // light on-background/on-surface text

    // ── Teal (primary) ────────────────────────────────────────────────────────
    val Teal50 = Color(0xFFF0FDFA)
    val Teal100 = Color(0xFFCCFBF1)
    val Teal200 = Color(0xFF99F6E4)
    val Teal400 = Color(0xFF2DD4BF)
    val Teal500 = Color(0xFF14B8A6)
    val Teal600 = Color(0xFF0D9488)
    val Teal700 = Color(0xFF0F766E)
    val Teal800 = Color(0xFF115E59)
    val Teal900 = Color(0xFF134E4A)
    val Teal950 = Color(0xFF042F2E)

    // ── Amber / Gold (secondary) ──────────────────────────────────────────────
    val Yellow400 = Color(0xFFFACC15)
    val Gold500 = Color(0xFFEAB308)      // brand gold accent
    val GoldDark = Color(0xFFCA8A04)     // darker gold for tints on gold surfaces
    val GoldPure = Color(0xFFFFD700)     // zakat gold
    val Silver = Color(0xFFC0C0C0)       // zakat silver
    val Amber300 = Color(0xFFFCD34D)
    val Amber400 = Color(0xFFFBBF24)
    val Amber500 = Color(0xFFF59E0B)
    val Amber600 = Color(0xFFD97706)
    val Amber700 = Color(0xFFB45309)
    val Amber800 = Color(0xFF92400E)
    val AmberDeep = Color(0xFFFFA000)    // secondary-dark (Material)
    val AmberSoft = Color(0xFFFFECB3)    // secondary-container (Material)
    val MatAmber = Color(0xFFFFC107)     // Material amber — bookmarks/milestones/pending

    // ── Green / Emerald ───────────────────────────────────────────────────────
    val Green500 = Color(0xFF22C55E)
    val Green600 = Color(0xFF16A34A)
    val Emerald300 = Color(0xFF34D399)
    val Emerald500 = Color(0xFF10B981)
    val Emerald600 = Color(0xFF059669)
    val MatGreen = Color(0xFF4CAF50)     // Material green — prayed/fasted/complete/cash
    val LightGreen = Color(0xFF8BC34A)   // Material light green — hadith "hasan" grade
    val GreenDeep = Color(0xFF2E7D32)    // Material green 800 — jumuah accent

    // ── Red ───────────────────────────────────────────────────────────────────
    val Red300 = Color(0xFFFCA5A5)
    val Red400 = Color(0xFFF87171)
    val Red500 = Color(0xFFEF4444)
    val Red600 = Color(0xFFDC2626)
    val Red700 = Color(0xFFB91C1C)
    val MatRed = Color(0xFFF44336)       // Material red — missed
    val ErrorRed = Color(0xFFB00020)
    val ErrorPink = Color(0xFFCF6679)    // dark-theme error
    val ErrorContainer = Color(0xFFFDE7E9)
    val OnErrorContainer = Color(0xFF370B0E)

    // ── Orange ────────────────────────────────────────────────────────────────
    val Orange300 = Color(0xFFFDBA74)
    val Orange400 = Color(0xFFFB923C)
    val Orange500 = Color(0xFFF97316)
    val Orange600 = Color(0xFFEA580C)
    val Orange700 = Color(0xFFC2410C)
    val MatOrange = Color(0xFFFF9800)    // Material orange — makeup/late
    val MatOrange400 = Color(0xFFFFA726) // partial completion

    // ── Blue / Sky ────────────────────────────────────────────────────────────
    val Blue300 = Color(0xFF93C5FD)
    val Blue400 = Color(0xFF60A5FA)
    val Blue500 = Color(0xFF3B82F6)
    val Blue600 = Color(0xFF2563EB)
    val Blue700 = Color(0xFF1D4ED8)
    val BlueSoft = Color(0xFF5B8DEF)     // info-soft
    val Sky500 = Color(0xFF0EA5E9)
    val MatBlue = Color(0xFF2196F3)      // Material blue — jamaah/investment

    // ── Indigo ────────────────────────────────────────────────────────────────
    val Indigo400 = Color(0xFF818CF8)
    val Indigo500 = Color(0xFF6366F1)
    val Indigo600 = Color(0xFF4F46E5)

    // ── Violet / Purple ───────────────────────────────────────────────────────
    val Violet400 = Color(0xFFA78BFA)
    val Violet500 = Color(0xFF8B5CF6)
    val Violet600 = Color(0xFF7C3AED)
    val Purple500 = Color(0xFFA855F7)
    val Purple600 = Color(0xFF9333EA)
    val DeepPurple = Color(0xFF7C4DFF)   // tertiary
    val PurpleSoft = Color(0xFFE8DAFF)   // tertiary-container
    val PurpleDeep = Color(0xFF2E1065)   // on-tertiary-container
    val MatPurple = Color(0xFF9C27B0)    // Material purple — ramadan/qada

    // ── Pink / Rose ───────────────────────────────────────────────────────────
    val Pink300 = Color(0xFFF9A8D4)
    val Pink400 = Color(0xFFF472B6)
    val Pink500 = Color(0xFFEC4899)
    val Pink600 = Color(0xFFDB2777)
    val Pink700 = Color(0xFFBE185D)
    val Rose300 = Color(0xFFFDA4AF)
    val Rose400 = Color(0xFFFB7185)
    val Rose600 = Color(0xFFE11D48)
    val Rose800 = Color(0xFF9F1239)

    // ── Cyan ──────────────────────────────────────────────────────────────────
    val Cyan400 = Color(0xFF22D3EE)
    val Cyan700 = Color(0xFF0E7490)
    val MatTeal700 = Color(0xFF00796B)   // Material teal — medinan badge

    // ── Brown ─────────────────────────────────────────────────────────────────
    val Brown700 = Color(0xFF795548)     // meccan badge
    val BrownDeep = Color(0xFF3E2723)    // on-secondary-container

    // ── Slate ─────────────────────────────────────────────────────────────────
    val Slate300 = Color(0xFFCBD5E1)
    val Slate400 = Color(0xFF94A3B8)
    val Slate500 = Color(0xFF64748B)

    // ── Onboarding illustration backdrop (deep teal) ──────────────────────────
    val OnboardingBgTop = Color(0xFF061A1C)
    val OnboardingBgBottom = Color(0xFF0A2A2A)
}
