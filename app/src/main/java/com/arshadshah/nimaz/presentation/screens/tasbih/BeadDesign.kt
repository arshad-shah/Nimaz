package com.arshadshah.nimaz.presentation.screens.tasbih

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.arshadshah.nimaz.presentation.screens.tasbih.BeadDesigns.Default
import com.arshadshah.nimaz.presentation.screens.tasbih.BeadDesigns.all
import com.arshadshah.nimaz.presentation.theme.BeadColors
import com.arshadshah.nimaz.presentation.theme.NimazPalette

/**
 * A pluggable look for the [TasbihBeads] strand. The strand mechanics (hidden
 * loop, gap, one-bead crossing) stay fixed; a design supplies the **palette**,
 * the **proportions**, and how a single bead is **drawn**. Add a new bead style
 * by declaring another [BeadDesign] in [BeadDesigns] (and listing it in
 * [BeadDesigns.all]) — no changes to the strand logic needed.
 *
 * @param key    stable identifier, persisted as the user's chosen design
 * @param label  human-readable name shown in the design picker
 * @param resting the three radial-gradient stops of a resting bead
 * @param active the stops the loose bead warms to mid-crossing (gold for all)
 * @param imame  the stops of the leader bead (lap marker)
 * @param drawBead how one bead is painted at a centre/radius with given stops
 */
class BeadDesign(
    val key: String,
    val label: String,
    val cord: Color,
    val resting: List<Color>,
    val active: List<Color> = BeadDesigns.GoldActive,
    val imame: List<Color> = BeadDesigns.JadeImame,
    val beadFraction: Float = 0.060f, // bead radius / min side
    val pack: Float = 2.06f,          // bunch spacing, in bead radii
    val gapBeads: Float = 5.0f,       // gap width, in pack-spaces (wide — the active bead travels it)
    val drawBead: DrawScope.(center: Offset, r: Float, colors: List<Color>) -> Unit = DrawScope::roundBead,
)

/**
 * Default bead painter. The body is filled with the design's **signature** colour
 * (colors[1]) so the material reads clearly, then shaded toward colors[2] at the
 * rim and lit with a small colors[0] specular highlight — so switching designs is
 * unmistakable, not just a faint highlight change.
 */
fun DrawScope.roundBead(center: Offset, r: Float, colors: List<Color>) {
    // Body = signature colour.
    drawCircle(color = colors[1], radius = r, center = center)
    // Edge shading toward the darkest stop.
    drawCircle(
        brush = Brush.radialGradient(
            0.45f to Color.Transparent,
            1f to colors[2].copy(alpha = 0.6f),
            center = center,
            radius = r
        ),
        radius = r,
        center = center
    )
    // Specular highlight (lightest stop), upper-left.
    val highlight = center + Offset(-r * 0.30f, -r * 0.30f)
    drawCircle(
        brush = Brush.radialGradient(
            0f to colors[0],
            1f to Color.Transparent,
            center = highlight,
            radius = r * 0.85f
        ),
        radius = r,
        center = center
    )
    // Faint rim.
    drawCircle(
        color = Color.White.copy(alpha = 0.08f),
        radius = r,
        center = center,
        style = Stroke(width = r * 0.05f)
    )
}

/** Registry of available bead designs. Add new looks here and to [all]. */
object BeadDesigns {
    /** The loose crossing bead always warms to gold, whatever the material. */
    val GoldActive = listOf(BeadColors.GoldHighlight, NimazPalette.Gold500, BeadColors.GoldShadow)

    /** The imame (lap marker) is jade-green across designs. */
    val JadeImame = listOf(BeadColors.JadeHighlight, BeadColors.JadeMid, BeadColors.JadeShadow)

    val Wood = BeadDesign(
        key = "wood",
        label = "Wood",
        cord = BeadColors.WoodCord,
        resting = listOf(BeadColors.WoodHighlight, BeadColors.WoodMid, BeadColors.WoodShadow),
    )

    val Marble = BeadDesign(
        key = "marble",
        label = "Marble",
        cord = BeadColors.MarbleCord,
        resting = listOf(BeadColors.MarbleHighlight, BeadColors.MarbleMid, BeadColors.MarbleShadow),
    )

    val Amethyst = BeadDesign(
        key = "amethyst",
        label = "Amethyst",
        cord = BeadColors.AmethystCord,
        resting = listOf(BeadColors.AmethystHighlight, BeadColors.AmethystMid, BeadColors.AmethystShadow),
    )

    val Onyx = BeadDesign(
        key = "onyx",
        label = "Onyx",
        cord = BeadColors.OnyxCord,
        resting = listOf(BeadColors.OnyxHighlight, BeadColors.OnyxMid, BeadColors.OnyxShadow),
    )

    val Pearl = BeadDesign(
        key = "pearl",
        label = "Pearl",
        cord = BeadColors.PearlCord,
        resting = listOf(BeadColors.PearlHighlight, BeadColors.PearlMid, BeadColors.PearlShadow),
    )

    val Jade = BeadDesign(
        key = "jade",
        label = "Jade",
        cord = BeadColors.JadeCord,
        resting = listOf(BeadColors.JadeRestHighlight, BeadColors.JadeRestMid, BeadColors.JadeRestShadow),
        // jade beads would hide a jade imame — mark the lap in gold instead
        imame = GoldActive,
    )

    /** Ordered list backing the bead-design picker. */
    val all = listOf(Wood, Marble, Amethyst, Onyx, Pearl, Jade)

    /** The design used by default when bead mode is on. */
    val Default = Wood

    /** Resolve a persisted [key] back to its design (falling back to [Default]). */
    fun byKey(key: String?): BeadDesign = all.firstOrNull { it.key == key } ?: Default
}
