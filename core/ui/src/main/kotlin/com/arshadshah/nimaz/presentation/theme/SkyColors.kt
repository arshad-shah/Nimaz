package com.arshadshah.nimaz.presentation.theme

import androidx.compose.ui.graphics.Color

/** Central colour definitions for [com.arshadshah.nimaz.presentation.components.organisms.PrayerSkyScene] — sky/sun/moon/cloud/star art. */
object SkyColors {
    // ── Sun disc & corona ────────────────────────────────────────────────
    val SunDiskCoreDay = Color(0xFFFFF1A8)
    val SunDiskCoreWarm = Color(0xFFFFCF87)
    val SunDiskRimDay = NimazPalette.Yellow400
    val SunDiskRimWarm = NimazPalette.Orange500
    val SunCoronaDay = Color(0xFFFFF6C2)
    val SunCoronaWarm = Color(0xFFFFE0A0)

    // ── Night overlay & clouds ───────────────────────────────────────────
    val NightAurora = Color(0xFF7E8AD6)
    val CloudShadow = Color(0xFFAFAFAF)

    // ── Sky keyframe gradients (top → horizon per time of day) ────────────
    // Midnight (t = 0.00f / 1.00f)
    val MidnightZenith = Color(0xFF03060F)
    val MidnightUpper = Color(0xFF0A0F26)
    val MidnightMid = Color(0xFF141A38)
    val MidnightLower = Color(0xFF1B1F4A)
    val MidnightHorizon = Color(0xFF33285E)
    val MidnightCloud = Color(0xFF2A2F52)

    // Pre-dawn (t = 0.20f)
    val PreDawnZenith = Color(0xFF060A1C)
    val PreDawnUpper = Color(0xFF16204A)
    val PreDawnMid = Color(0xFF3B3270)
    val PreDawnLower = Color(0xFF8A4F6E)
    val PreDawnHorizon = Color(0xFFD08A5E)
    val PreDawnCloud = Color(0xFF7A5A72)

    // Sunrise (t = 0.28f)
    val SunriseZenith = Color(0xFF2B3A8C)
    val SunriseUpper = Color(0xFF7C6AB0)
    val SunriseMid = Color(0xFFE59AB0)
    val SunriseLower = Color(0xFFFBB778)
    val SunriseHorizon = Color(0xFFFFE0A3)
    val SunriseCloud = Color(0xFFFCE0CE)

    // Midday (t = 0.50f)
    val MiddayZenith = Color(0xFF0A2E7A)
    val MiddayUpper = Color(0xFF1E62D6)
    val MiddayMid = Color(0xFF4F9BF5)
    val MiddayLower = Color(0xFFBFE0FB)
    val MiddayHorizon = Color(0xFFEAF6FF)
    val MiddayCloud = Color(0xFFF2F7FF)

    // Afternoon (t = 0.67f)
    val AfternoonZenith = Color(0xFF15407F)
    val AfternoonUpper = Color(0xFF3E78C9)
    val AfternoonMid = Color(0xFF8FB6E8)
    val AfternoonLower = Color(0xFFF2D9A8)
    val AfternoonHorizon = Color(0xFFFBE3B0)
    val AfternoonCloud = Color(0xFFFBEBCF)

    // Sunset (t = 0.80f)
    val SunsetZenith = Color(0xFF241056)
    val SunsetUpper = Color(0xFF7A1E83)
    val SunsetMid = Color(0xFFD6356B)
    val SunsetLower = Color(0xFFF9733A)
    val SunsetHorizon = Color(0xFFFBD34D)
    val SunsetCloud = Color(0xFFF2B488)

    // Dusk (t = 0.87f)
    val DuskZenith = Color(0xFF04060F)
    val DuskUpper = Color(0xFF0E1330)
    val DuskMid = Color(0xFF241A45)

    // DuskLower shares MidnightHorizon (0xFF33285E)
    val DuskHorizon = Color(0xFF3A2A55)
    val DuskCloud = Color(0xFF3A3F66)

    // ── Moon ─────────────────────────────────────────────────────────────
    val MoonGlow = Color(0xFFC7D2FE)
    val MoonGlowTransparent = Color(0x00C7D2FE)
    val MoonDiscTop = Color(0xFF262C4C)
    val MoonDiscBottom = Color(0xFF10142C)
    val MoonLitHighlight = Color(0xFFE9EDF6)
    val MoonLitMid = Color(0xFFC5CCDE)
    val MoonLitEdge = Color(0xFFAAB2CC)
}
