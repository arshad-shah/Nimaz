package com.arshadshah.nimaz.presentation.screens.tasbih

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * A pluggable look for the [TasbihBeads] strand. The strand mechanics (hidden
 * loop, gap, one-bead crossing) stay fixed; a design supplies the **palette**,
 * the **proportions**, and how a single bead is **drawn**. Add a new bead style
 * by declaring another [BeadDesign] in [BeadDesigns] and passing it to
 * [TasbihBeads] — no changes to the strand logic needed.
 *
 * @param wood   the three radial-gradient stops of a resting bead
 * @param gold   the stops the loose bead warms to mid-crossing
 * @param imame  the stops of the leader bead (lap marker)
 * @param drawBead how one bead is painted at a centre/radius with given stops
 */
class BeadDesign(
    val cord: Color,
    val wood: List<Color>,
    val gold: List<Color>,
    val imame: List<Color>,
    val beadFraction: Float = 0.072f, // bead radius / min side
    val pack: Float = 2.04f,          // bunch spacing, in bead radii
    val gapBeads: Float = 3.0f,       // gap width, in pack-spaces
    val drawBead: DrawScope.(center: Offset, r: Float, colors: List<Color>) -> Unit = DrawScope::roundBead,
)

/** Default bead painter: a softly lit round bead with a faint rim. */
fun DrawScope.roundBead(center: Offset, r: Float, colors: List<Color>) {
    val highlight = center + Offset(-r * 0.32f, -r * 0.32f)
    drawCircle(
        brush = Brush.radialGradient(colors, center = highlight, radius = r * 1.35f),
        radius = r,
        center = center
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.10f),
        radius = r,
        center = center,
        style = Stroke(width = r * 0.05f)
    )
}

/** Registry of available bead designs. Add new looks here. */
object BeadDesigns {
    val Wooden = BeadDesign(
        cord = Color(0xFF5A4226),
        wood = listOf(Color(0xFFC8893B), Color(0xFF8A4F1E), Color(0xFF5A3212)),
        gold = listOf(Color(0xFFFBE38A), Color(0xFFEAB308), Color(0xFFA87908)),
        imame = listOf(Color(0xFF3A7D5C), Color(0xFF1F5A3C), Color(0xFF123C28)),
    )

    /** The design used by default when bead mode is on. */
    val Default = Wooden
}
