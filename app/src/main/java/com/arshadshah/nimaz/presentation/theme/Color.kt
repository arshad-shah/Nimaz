package com.arshadshah.nimaz.presentation.theme

import androidx.compose.ui.graphics.Color

object NimazColors {
    // Primary - Teal (Updated to match prototype)
    val Primary50 = Color(0xFFF0FDFA)
    val Primary100 = Color(0xFFCCFBF1)
    val Primary200 = Color(0xFF99F6E4)
    val Primary400 = Color(0xFF2DD4BF)
    val Primary = Color(0xFF14B8A6)  // Primary 500
    val Primary600 = Color(0xFF0D9488)
    val Primary700 = Color(0xFF0F766E)
    val PrimaryDark = Primary700  // Legacy alias
    val Primary800 = Color(0xFF115E59)
    val Primary900 = Color(0xFF134E4A)
    val Primary950 = Color(0xFF042F2E)

    // Legacy compatibility
    val PrimaryLight = Primary400
    val PrimaryContainer = Primary100
    val OnPrimary = Color(0xFFFFFFFF)
    val OnPrimaryContainer = Primary950

    // Secondary - Gold/Amber (Updated)
    val Gold400 = Color(0xFFFACC15)
    val Gold500 = Color(0xFFEAB308)
    val Secondary = Gold500
    val SecondaryLight = Gold400
    val SecondaryDark = Color(0xFFFFA000)
    val SecondaryContainer = Color(0xFFFFECB3)
    val OnSecondary = Color(0xFF000000)
    val OnSecondaryContainer = Color(0xFF3E2723)

    // Neutral Colors (Dark theme optimized)
    val Neutral0 = Color(0xFFFFFFFF)
    val Neutral50 = Color(0xFFFAFAF9)
    val Neutral100 = Color(0xFFF5F5F4)
    val Neutral200 = Color(0xFFE7E5E4)
    val Neutral300 = Color(0xFFD6D3D1)
    val Neutral400 = Color(0xFFA8A29E)
    val Neutral500 = Color(0xFF78716C)
    val Neutral600 = Color(0xFF57534E)
    val Neutral700 = Color(0xFF44403C)
    val Neutral800 = Color(0xFF292524)
    val Neutral900 = Color(0xFF1C1917)
    val Neutral950 = Color(0xFF0C0A09)

    // Tertiary - Deep Purple (for accents)
    val Tertiary = Color(0xFF7C4DFF)
    val TertiaryContainer = Color(0xFFE8DAFF)
    val OnTertiary = Color(0xFFFFFFFF)
    val OnTertiaryContainer = Color(0xFF2E1065)

    // Background & Surface - Light Theme
    val BackgroundLight = Color(0xFFFAFAFA)
    val SurfaceLight = Color(0xFFFFFFFF)
    val SurfaceVariantLight = Color(0xFFF5F5F5)
    val OnBackgroundLight = Color(0xFF1C1C1C)
    val OnSurfaceLight = Color(0xFF1C1C1C)
    val OnSurfaceVariantLight = Color(0xFF757575)

    // Background & Surface - Dark Theme
    val BackgroundDark = Color(0xFF0C0A09)
    val SurfaceDark = Color(0xFF1C1917)
    val SurfaceVariantDark = Color(0xFF292524)
    val OnBackgroundDark = Color(0xFFE0E0E0)
    val OnSurfaceDark = Color(0xFFE0E0E0)
    val OnSurfaceVariantDark = Color(0xFFB0B0B0)

    // Error
    val Error = Color(0xFFB00020)
    val ErrorLight = Color(0xFFCF6679)
    val ErrorContainer = Color(0xFFFDE7E9)
    val OnError = Color(0xFFFFFFFF)
    val OnErrorContainer = Color(0xFF370B0E)

    // Outline
    val OutlineLight = Color(0xFFE0E0E0)
    val OutlineDark = Color(0xFF292524)

    // Prayer Time Colors (Updated to match prototype)
    object PrayerColors {
        val Fajr = Color(0xFF6366F1)           // Indigo
        val FajrGradientEnd = Color(0xFF9FA8DA)
        val Sunrise = Color(0xFFF59E0B)         // Amber
        val SunriseGradientEnd = Color(0xFFFFE0B2)
        val Dhuhr = Color(0xFFEAB308)           // Yellow
        val DhuhrGradientEnd = Color(0xFFFFF9C4)
        val Asr = Color(0xFFF97316)             // Orange
        val AsrGradientEnd = Color(0xFFFFCCBC)
        val Maghrib = Color(0xFFEF4444)         // Red
        val MaghribGradientEnd = Color(0xFFFFCDD2)
        val Isha = Color(0xFF8B5CF6)            // Purple
        val IshaGradientEnd = Color(0xFFC5CAE9)
    }

    // Status Colors
    object StatusColors {
        val Prayed = Color(0xFF4CAF50)
        val Missed = Color(0xFFF44336)
        val Pending = Color(0xFFFFC107)
        val Qada = Color(0xFF9C27B0)
        val Jamaah = Color(0xFF2196F3)
        val Late = Color(0xFFFF9800)         // Orange for late prayers
        val Active = Color(0xFF4CAF50)        // Green for active state
        val Partial = Color(0xFFFFA726)       // Orange for partial completion (some prayers prayed)
    }

    // Fasting Colors
    object FastingColors {
        val Fasted = Color(0xFF4CAF50)
        val NotFasted = Color(0xFFBDBDBD)
        val Makeup = Color(0xFFFF9800)
        val Exempted = Color(0xFF9E9E9E)
        val Ramadan = Color(0xFF9C27B0)        // Purple for Ramadan fasts
        val Voluntary = Color(0xFF2196F3)      // Blue for voluntary fasts
    }

    // Quran Colors
    object QuranColors {
        val Meccan = Color(0xFF795548)
        val Medinan = Color(0xFF00796B)
        val SajdaAyah = Color(0xFFE91E63)
        val BookmarkPrimary = Color(0xFFFFC107)
        val BookmarkSecondary = Color(0xFF00BCD4)
    }

    // Zakat Colors
    object ZakatColors {
        val Gold = Color(0xFFFFD700)
        val Silver = Color(0xFFC0C0C0)
        val Cash = Color(0xFF4CAF50)
        val Investment = Color(0xFF2196F3)
    }

    // Tasbih Colors
    object TasbihColors {
        val Counter = Color(0xFF009688)
        val Complete = Color(0xFF4CAF50)
        val Milestone = Color(0xFFFFC107)
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
