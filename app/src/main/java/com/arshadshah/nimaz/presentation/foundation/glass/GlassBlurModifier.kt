package com.arshadshah.nimaz.presentation.foundation.glass

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.arshadshah.nimaz.presentation.components.atoms.GlassBackdrop

/**
 * Draws the slice of [backdrop] sitting directly behind this node, blurred and
 * clipped to [shape], beneath the node's own content. Needs API 31+; below that
 * it is a no-op and the translucent fill carries the legibility on its own.
 */
@Composable
fun Modifier.glassBlur(
    backdrop: GlassBackdrop,
    blurRadius: Dp,
    shape: Shape,
): Modifier {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return this
    val blurPx = with(LocalDensity.current) { blurRadius.toPx() }
    val glassLayer = rememberGraphicsLayer()
    var positionInRoot by remember { mutableStateOf(Offset.Zero) }
    return this
        .onGloballyPositioned { positionInRoot = it.positionInRoot() }
        .drawWithContent {
            val offset = positionInRoot - backdrop.sourcePositionInRoot
            glassLayer.renderEffect = BlurEffect(blurPx, blurPx, TileMode.Clamp)
            glassLayer.record {
                translate(left = -offset.x, top = -offset.y) {
                    drawLayer(backdrop.layer)
                }
            }
            val outline = shape.createOutline(size, layoutDirection, this)
            clipPath(Path().apply { addOutline(outline) }) {
                drawLayer(glassLayer)
            }
            drawContent()
        }
}