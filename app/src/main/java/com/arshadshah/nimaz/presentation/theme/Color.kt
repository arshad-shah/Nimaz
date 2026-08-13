package com.arshadshah.nimaz.presentation.theme

import com.arshadshah.nimaz.presentation.theme.NimazPalette
import com.arshadshah.nimaz.presentation.theme.NimazPalette as P

/**
 * Tier 2 of the Nimaz colour system — **semantic tokens**.
 *
 * Every value here is a reference into the raw [NimazPalette]; this layer carries
 * *meaning* (primary, success, "prayed", "ramadan") rather than literal hues, so a
 * hue can be retuned in one place. Feature screens read these tokens (or
 * `MaterialTheme.colorScheme.*`), never raw palette constants or `Color(0xFF…)`.
 */
object NimazColors {
    // ── Primary — Teal ────────────────────────────────────────────────────────
    val Primary50 = P.Teal50
    val Primary100 = P.Teal100
    val Primary200 = P.Teal200
    val Primary400 = P.Teal400
    val Primary = P.Teal500              // Primary 500
    val Primary600 = P.Teal600
    val Primary700 = P.Teal700
    val PrimaryDark = Primary700          // Legacy alias
    val Primary800 = P.Teal800
    val Primary900 = P.Teal900
    val Primary950 = P.Teal950

    // Material role aliases
    val PrimaryLight = Primary400
    val PrimaryContainer = Primary100
    val OnPrimary = P.White
    val OnPrimaryContainer = Primary950

    // ── Secondary — Gold / Amber ──────────────────────────────────────────────
    val Gold400 = P.Yellow400
    val Gold500 = P.Gold500
    val Secondary = Gold500
    val SecondaryLight = Gold400
    val SecondaryDark = P.AmberDeep
    val SecondaryContainer = P.AmberSoft

    /** Darker amber-gold accent (e.g. icon tints/labels on gold surfaces). */
    val GoldDark = P.GoldDark

    // ── Semantic / categorical accents used across feature screens ────────────
    val Success = P.Green500
    val Warning = P.Amber500
    val Info = P.Blue500
    val InfoSoft = P.BlueSoft
    val Emerald = P.Emerald500
    val Sky = P.Sky500
    val Purple = P.Purple500
    val Pink = P.Pink500
    val Amber = P.Amber400
    val OrangeDark = P.Orange600
    val IndigoLight = P.Indigo400
    val Gray300 = P.Gray300

    // Onboarding illustration background (dark teal gradient).
    val OnboardingBgTop = P.OnboardingBgTop
    val OnboardingBgBottom = P.OnboardingBgBottom
    val OnSecondary = P.Black
    val OnSecondaryContainer = P.BrownDeep

    // ── Neutral ramp (dark-theme optimised) ───────────────────────────────────
    val Neutral0 = P.White
    val Neutral50 = P.Stone50
    val Neutral100 = P.Stone100
    val Neutral200 = P.Stone200
    val Neutral300 = P.Stone300
    val Neutral400 = P.Stone400
    val Neutral500 = P.Stone500
    val Neutral600 = P.Stone600
    val Neutral700 = P.Stone700
    val Neutral800 = P.Stone800
    val Neutral900 = P.Stone900
    val Neutral950 = P.Stone950

    // ── Tertiary — Deep Purple (accents) ──────────────────────────────────────
    val Tertiary = P.DeepPurple
    val TertiaryContainer = P.PurpleSoft
    val OnTertiary = P.White
    val OnTertiaryContainer = P.PurpleDeep

    // ── Background & Surface — Light ──────────────────────────────────────────
    val BackgroundLight = P.GrayBg
    val SurfaceLight = P.White
    val SurfaceVariantLight = P.Gray100
    val OnBackgroundLight = P.Ink
    val OnSurfaceLight = P.Ink
    val OnSurfaceVariantLight = P.Gray700

    // ── Background & Surface — Dark ───────────────────────────────────────────
    val BackgroundDark = P.Stone950
    val SurfaceDark = P.Stone900
    val SurfaceVariantDark = P.Stone800
    val OnBackgroundDark = P.Gray200
    val OnSurfaceDark = P.Gray200
    val OnSurfaceVariantDark = P.Gray500

    // ── The surface-container ladder ──────────────────────────────────────────
    //
    // Material 3 generates these from the primary hue when a scheme leaves them out, and the app
    // left every one of them out. The result was a teal-and-lavender cast on anything that used
    // them — the segmented-control track, the calendar header, chip beds — while the surfaces the
    // scheme *did* define stayed a clean white. Two families of grey in one app, one of them
    // nobody chose.
    //
    // Defined here as plain neutrals off the same ramp as `SurfaceVariant`, so a container is a
    // step of lightness rather than a step of hue.
    val SurfaceContainerLowestLight = P.White
    val SurfaceContainerLowLight = P.Stone50
    val SurfaceContainerLight = P.Stone100
    val SurfaceContainerHighLight = P.Gray100
    val SurfaceContainerHighestLight = P.Stone200
    val SurfaceBrightLight = P.White
    val SurfaceDimLight = P.Stone200

    val SurfaceContainerLowestDark = P.Stone950
    val SurfaceContainerLowDark = P.Stone900
    val SurfaceContainerDark = P.Stone800
    val SurfaceContainerHighDark = P.Stone700
    val SurfaceContainerHighestDark = P.Stone600
    val SurfaceBrightDark = P.Stone700
    val SurfaceDimDark = P.Stone950

    // ── Error ─────────────────────────────────────────────────────────────────
    val Error = P.ErrorRed
    val ErrorLight = P.ErrorPink
    val ErrorContainer = P.ErrorContainer
    val OnError = P.White
    val OnErrorContainer = P.OnErrorContainer

    // ── Outline ───────────────────────────────────────────────────────────────
    val OutlineLight = P.Gray200
    val OutlineDark = P.Stone800

    /**
     * The subtle hairline — dividers, chip borders, the quiet edge of an outlined badge.
     *
     * Used 49 times across the app and never defined, so every one of those hairlines was
     * drawing Material 3's generated baseline, which is tinted off the primary hue. It is the
     * same omission that made surfaces mint and containers lavender.
     */
    val OutlineVariantLight = P.Stone200
    val OutlineVariantDark = P.Stone800

    // ── Inverse — the tooltip / snackbar surface, which flips against the theme ────
    val InverseSurfaceLight = P.Stone900
    val InverseOnSurfaceLight = P.Gray100
    val InversePrimaryLight = Primary400

    val InverseSurfaceDark = P.Gray100
    val InverseOnSurfaceDark = P.Ink
    val InversePrimaryDark = Primary700

    // ── Prayer-time accents ───────────────────────────────────────────────────
    object PrayerColors {
        val Fajr = P.Indigo500
        val Sunrise = P.Amber500
        val Dhuhr = P.Gold500
        val Asr = P.Orange500
        val Maghrib = P.Red500
        val Isha = P.Violet500
    }

    // ── Prayer-tracking status ────────────────────────────────────────────────
    object StatusColors {
        val Prayed = P.MatGreen
        val Missed = P.MatRed
        val Pending = P.MatAmber
        val Qada = P.MatPurple
        val Jamaah = P.MatBlue
        val Active = P.MatGreen          // green for active state
        val Partial = P.MatOrange400     // some prayers prayed
    }

    // ── Fasting ───────────────────────────────────────────────────────────────
    object FastingColors {
        val Fasted = P.MatGreen
        val NotFasted = P.Gray400
        val Makeup = P.MatOrange
        val Exempted = P.Gray600
        val Ramadan = P.MatPurple
    }

    // ── Quran ─────────────────────────────────────────────────────────────────
    object QuranColors {
        val Meccan = P.Brown700
        val Medinan = P.MatTeal700
        val BookmarkPrimary = P.MatAmber

        /**
         * Teal "mushaf" gradient + accents shared by the Quran banners and cards
         * (Verse-of-the-day, Continue-reading, Surah/Juz banners). Centralises the
         * 115E59→042F2E gradient, 0F766E border and gold accent that were hardcoded
         * (and copy-pasted) across each of them.
         */
        val BannerGradient = listOf(Primary800, Primary950)
        val BannerBorder = Primary700
        val BannerAccent = Gold500
    }

    // ── Zakat ─────────────────────────────────────────────────────────────────
    object ZakatColors {
        val Gold = P.GoldPure
        val Silver = P.Silver
        val Cash = P.MatGreen
        val Investment = P.MatBlue

        /** Darker amber-gold accent used on zakat calculator/history surfaces. */
        val GoldAccent = P.GoldDark
    }

    // ── Hadith book-card gradients (per collection) ───────────────────────────
    object HadithCollectionColors {
        val Bukhari = listOf(P.Green500, P.Green600)
        val Muslim = listOf(P.Blue500, P.Blue600)
        val Tirmidhi = listOf(P.Purple500, P.Purple600)
        val Nasai = listOf(P.Orange500, P.Orange600)
        val AbuDawud = listOf(P.Pink500, P.Pink600)
        val IbnMajah = listOf(P.Teal500, P.Teal600)
        val Default = IbnMajah
    }

    // ── Tasbih ────────────────────────────────────────────────────────────────
    object TasbihColors {
        val Complete = P.MatGreen
        val Milestone = P.MatAmber
    }

    // ── Tajweed — distinct colour per sub-rule, matching printed-mushaf coding ─
    // Each rule has a light and a dark (OLED-brighter) tone; pairs are consumed
    // by TajweedParser.
    object TajweedColors {
        // ── Light theme ── all ≥ 4.5:1 vs the reader background (#FAFAFA);
        // verified by scripts/check_tajweed_contrast.py.
        val GhunnahLight = P.Emerald700        // was Emerald600 (3.61:1)
        val IkhfaLight = P.Teal700             // was Teal600 (3.59:1)
        val IkhfaShafawiLight = P.Cyan700
        val IdghamGhunnahLight = P.Amber800    // was Amber600 (3.05:1)
        val IdghamNoGhunnahLight = P.Brown700  // freed Amber800 for IdghamGhunnah
        val IdghamShafawiLight = P.Amber700
        val IdghamMutajanisaynLight = P.Orange700
        val IdghamMutaqaribayLight = P.Orange800  // was Orange600 (3.41:1)
        val IdghamMutamathilaynLight = P.Amber900  // dm — derived (#291)
        val WaqfLight = P.Stone600                 // wq — stop signs (#291)
        val TafkhimLight = P.GreenDeep             // tk — heavy (raa/lam, #291)
        val TarqiqLight = P.Purple600              // tq — light (raa/lam, #291)
        // Qalqalah — blue family, kubra deeper than sughra
        val QalqalahSughraLight = P.Blue600
        val QalqalahKubraLight = P.Blue700
        // Madd — one warm hue family (rose→red→pink), distinct lightness so the
        // six sub-rules read as related. See TajweedParser KDoc for beat counts.
        val MaddNormalLight = P.Rose600        // mn — natural, 2
        val MaddMunfasilLight = P.Pink700      // mf — jaiz munfasil (was Pink600 4.40:1)
        val MaddMuttasilLight = P.Red600       // mt — wajib muttasil, 4/5
        val MaddAaridLight = P.Rose800         // ma — 'aarid lis-sukun, 2/4/6
        val MaddLinLight = P.Fuchsia700        // ml — lin (freed Pink700 for munfasil)
        val MaddNecessaryLight = P.Red700      // my — lazim, 6
        val IqlabLight = P.Violet600
        val LamShamsiyyahLight = P.Indigo600
        val SilentLight = P.Slate500
        val HamzaWaslLight = P.Slate600        // was Slate400 (2.46:1) — 22% of all spans

        // ── Dark theme (brighter for OLED readability) ──
        val GhunnahDark = P.Emerald300
        val IkhfaDark = P.Teal400
        val IkhfaShafawiDark = P.Cyan400
        val IdghamGhunnahDark = P.Amber400
        val IdghamNoGhunnahDark = P.Amber500
        val IdghamShafawiDark = P.Amber300
        val IdghamMutajanisaynDark = P.Orange400
        val IdghamMutaqaribayDark = P.Orange300
        val IdghamMutamathilaynDark = P.Amber200   // dm
        val WaqfDark = P.Stone400                  // wq
        val TafkhimDark = P.Green500               // tk
        val TarqiqDark = P.Purple400               // tq
        val QalqalahSughraDark = P.Blue400
        val QalqalahKubraDark = P.Blue300
        val MaddNormalDark = P.Rose400         // mn
        val MaddMunfasilDark = P.Pink400       // mf
        val MaddMuttasilDark = P.Red400        // mt
        val MaddAaridDark = P.Rose300          // ma
        val MaddLinDark = P.Pink300            // ml
        val MaddNecessaryDark = P.Red300       // my
        val IqlabDark = P.Violet400
        val LamShamsiyyahDark = P.Indigo400
        val SilentDark = P.Slate400
        val HamzaWaslDark = P.Slate300
    }
}

// Light Theme Colors
val md_theme_light_primary = NimazColors.Primary
val md_theme_light_onPrimary = NimazColors.OnPrimary
val md_theme_light_primaryContainer = NimazColors.PrimaryContainer
val md_theme_light_onPrimaryContainer = NimazColors.OnPrimaryContainer
val md_theme_light_secondary = NimazColors.Secondary
val md_theme_light_onSecondary = NimazColors.OnSecondary
val md_theme_light_secondaryContainer = NimazColors.SecondaryContainer
val md_theme_light_onSecondaryContainer = NimazColors.OnSecondaryContainer
val md_theme_light_tertiary = NimazColors.Tertiary
val md_theme_light_onTertiary = NimazColors.OnTertiary
val md_theme_light_tertiaryContainer = NimazColors.TertiaryContainer
val md_theme_light_onTertiaryContainer = NimazColors.OnTertiaryContainer
val md_theme_light_error = NimazColors.Error
val md_theme_light_errorContainer = NimazColors.ErrorContainer
val md_theme_light_onError = NimazColors.OnError
val md_theme_light_onErrorContainer = NimazColors.OnErrorContainer
val md_theme_light_background = NimazColors.BackgroundLight
val md_theme_light_onBackground = NimazColors.OnBackgroundLight
val md_theme_light_surface = NimazColors.SurfaceLight
val md_theme_light_onSurface = NimazColors.OnSurfaceLight
val md_theme_light_surfaceVariant = NimazColors.SurfaceVariantLight
val md_theme_light_onSurfaceVariant = NimazColors.OnSurfaceVariantLight
val md_theme_light_outline = NimazColors.OutlineLight

// Dark Theme Colors
val md_theme_dark_primary = NimazColors.PrimaryLight
val md_theme_dark_onPrimary = NimazColors.OnPrimaryContainer
val md_theme_dark_primaryContainer = NimazColors.PrimaryDark
val md_theme_dark_onPrimaryContainer = NimazColors.PrimaryContainer
val md_theme_dark_secondary = NimazColors.SecondaryLight
val md_theme_dark_onSecondary = NimazColors.OnSecondaryContainer
val md_theme_dark_secondaryContainer = NimazColors.SecondaryDark
val md_theme_dark_onSecondaryContainer = NimazColors.SecondaryContainer
val md_theme_dark_tertiary = NimazColors.Tertiary
val md_theme_dark_onTertiary = NimazColors.OnTertiary
val md_theme_dark_tertiaryContainer = NimazColors.OnTertiaryContainer
val md_theme_dark_onTertiaryContainer = NimazColors.TertiaryContainer
val md_theme_dark_error = NimazColors.ErrorLight
val md_theme_dark_errorContainer = NimazColors.OnErrorContainer
val md_theme_dark_onError = NimazColors.OnError
val md_theme_dark_onErrorContainer = NimazColors.ErrorContainer
val md_theme_dark_background = NimazColors.BackgroundDark
val md_theme_dark_onBackground = NimazColors.OnBackgroundDark
val md_theme_dark_surface = NimazColors.SurfaceDark
val md_theme_dark_onSurface = NimazColors.OnSurfaceDark
val md_theme_dark_surfaceVariant = NimazColors.SurfaceVariantDark
val md_theme_dark_onSurfaceVariant = NimazColors.OnSurfaceVariantDark
val md_theme_dark_outline = NimazColors.OutlineDark
