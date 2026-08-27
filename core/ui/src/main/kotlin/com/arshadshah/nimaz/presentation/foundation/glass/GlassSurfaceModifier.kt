package com.arshadshah.nimaz.presentation.foundation.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.GlassBackdrop
import com.arshadshah.nimaz.presentation.components.atoms.GlassIconButton
import com.arshadshah.nimaz.presentation.components.atoms.GlassPill
import com.arshadshah.nimaz.presentation.components.atoms.GlassPillTone

/**
 * The shared glass treatment: an optional backdrop blur, a [tone]-weighted fill,
 * and the lit edge — a hairline border that fades from a bright top rim to a dim
 * base. Both [GlassPill] and [GlassIconButton] derive their look from here so the
 * family stays visually identical across shapes.
 *
 * The fill alphas are deliberately substantial: a glass pill must stay legible
 * even with no [backdrop] (e.g. on Android < 12, where the blur is skipped).
 */
@Composable
fun Modifier.glassSurface(
    tone: GlassPillTone,
    tint: Color,
    shape: Shape,
    backdrop: GlassBackdrop?,
    blurRadius: Dp,
): Modifier {
    val fillAlpha = when (tone) {
        GlassPillTone.Frosted -> 0.26f
        GlassPillTone.Solid -> 0.42f
        GlassPillTone.Ghost -> 0.10f
    }
    val edgeTopAlpha = when (tone) {
        GlassPillTone.Frosted -> 0.55f
        GlassPillTone.Solid -> 0.65f
        GlassPillTone.Ghost -> 0.45f
    }
    val edgeBottomAlpha = 0.12f
    return this
        .then(
            if (backdrop != null && blurRadius > 0.dp) {
                Modifier.glassBlur(backdrop = backdrop, blurRadius = blurRadius, shape = shape)
            } else {
                Modifier
            }
        )
        .background(color = tint.copy(alpha = fillAlpha), shape = shape)
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                listOf(
                    tint.copy(alpha = edgeTopAlpha),
                    tint.copy(alpha = edgeBottomAlpha),
                )
            ),
            shape = shape,
        )
}


