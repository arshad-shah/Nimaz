package com.arshadshah.nimaz.presentation.theme

import androidx.compose.ui.graphics.Color

/** Central colour definitions for [com.arshadshah.nimaz.presentation.components.atoms.GlassPill]
 *  glass-morphism auroras and tints. */
object GlassColors {
    // The aurora presets reflect the sky, so they reuse the PrayerSkyScene
    // atmospheric palette ([SkyColors]) rather than re-declaring the same hexes.

    // Midday sky aurora — deep blue → bright blue → sky → pale haze.
    val MiddayBlueDeep = SkyColors.MiddayZenith
    val MiddayBlueMid = SkyColors.MiddayUpper
    val MiddayBlueBright = SkyColors.MiddayMid
    val MiddayBluePale = SkyColors.MiddayLower

    // Dusk sky aurora — indigo → mauve → rose → peach.
    val DuskIndigo = SkyColors.SunriseZenith
    val DuskMauve = SkyColors.SunriseUpper
    val DuskRose = SkyColors.SunriseMid
    val DuskPeach = SkyColors.SunriseLower

    // Night sky aurora — near-black → midnight → deep indigo → violet.
    val NightBlack = SkyColors.MidnightZenith
    val NightMidnight = SkyColors.MidnightUpper
    val NightIndigo = SkyColors.MidnightMid
    val NightViolet = SkyColors.MidnightHorizon

    // Dawn sky aurora — navy → violet → plum → amber.
    val DawnNavy = SkyColors.PreDawnUpper
    val DawnViolet = SkyColors.PreDawnMid
    val DawnPlum = SkyColors.PreDawnLower
    val DawnAmber = SkyColors.PreDawnHorizon

    // Accent tints used to re-skin a pill coherently.
    val TintMintGreen = Color(0xFF7FE3A4)
    val TintGold = Color(0xFFFFD27D)

    // Backdrop-blur preview dots — high-frequency content behind the frosted glass.
    val DotAmber = Color(0xFFFFB000)
    val DotRed = Color(0xFFFF5C5C)
    val DotGreen = Color(0xFF4ADE80)
    val DotSky = Color(0xFF38BDF8)
    val DotPurple = Color(0xFFC084FC)
    val DotPink = Color(0xFFFF8FA3)
}
