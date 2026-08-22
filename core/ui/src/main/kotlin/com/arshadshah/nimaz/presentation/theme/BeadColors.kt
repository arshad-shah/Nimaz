package com.arshadshah.nimaz.presentation.theme

import androidx.compose.ui.graphics.Color

/** Central colour definitions for tasbih bead materials ([com.arshadshah.nimaz.presentation.screens.tasbih.BeadDesign], [com.arshadshah.nimaz.presentation.screens.tasbih.TasbihBeads]). */
object BeadColors {
    // Gold — the active (crossing) bead palette. (Mid stop is NimazPalette.Gold500.)
    val GoldHighlight = Color(0xFFFBE38A)
    val GoldShadow = Color(0xFFA87908)

    // Jade — the imame (lap marker) palette.
    val JadeHighlight = Color(0xFF3A7D5C)
    val JadeMid = Color(0xFF1F5A3C)
    val JadeShadow = Color(0xFF123C28)

    // Wood material.
    val WoodCord = Color(0xFF5A4226)
    val WoodHighlight = Color(0xFFC8893B)
    val WoodMid = Color(0xFF8A4F1E)
    val WoodShadow = Color(0xFF5A3212)

    // Marble material.
    val MarbleCord = Color(0xFF5C7682)
    val MarbleHighlight = Color(0xFFEAF2F5)
    val MarbleMid = Color(0xFF8FB0BE)
    val MarbleShadow = Color(0xFF51707E)

    // Amethyst material.
    val AmethystCord = Color(0xFF4A3360)
    val AmethystHighlight = Color(0xFFD9B6F0)
    val AmethystMid = Color(0xFF8E54B8)
    val AmethystShadow = Color(0xFF5A2E80)

    // Onyx material.
    val OnyxCord = Color(0xFF3A3A44)
    val OnyxHighlight = Color(0xFF9A9AA6)
    val OnyxMid = Color(0xFF4C4C58)
    val OnyxShadow = Color(0xFF1C1C24)

    // Pearl material.
    val PearlCord = Color(0xFFB8AE92)
    val PearlHighlight = Color(0xFFFFFDF8)
    val PearlMid = Color(0xFFE7DEC8)
    val PearlShadow = Color(0xFFC8BC9C)

    // Jade material (the bead body, distinct from the jade imame above).
    val JadeCord = Color(0xFF2C5240)
    val JadeRestHighlight = Color(0xFFBFE6CC)
    val JadeRestMid = Color(0xFF4FA776)
    val JadeRestShadow = Color(0xFF2C6E49)

    // Bead-tray background.
    val TrayBackground = Color(0xFF0B100E)
}
