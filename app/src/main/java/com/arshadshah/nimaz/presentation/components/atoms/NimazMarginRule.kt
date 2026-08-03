package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The ruled margin a manuscript carries down its gutter.
 *
 * One hairline, drawn at a fixed distance from the row's start edge, with the row's marker
 * sitting on it. It is the device the subject tree and the passage outline share: in the tree
 * it carries indent depth, in the outline it separates the reference gutter from the prose, and
 * because it is literally the same line at the same weight the two lists read as one family
 * rather than two lists that happen to be in the same app.
 *
 * Drawn rather than laid out, so it costs no measure pass in a list that is 282 rows long.
 */
object NimazMarginRule {

    /** How far a tree row's children step in. Also the spacing between stacked rules. */
    val IndentStep: Dp = 20.dp

    /** The hairline's own width. Hairline means hairline — this is not a divider. */
    val Width: Dp = 1.dp

    /** The marker that sits on the rule. */
    val TickSize: Dp = 9.dp

    /**
     * Quiet enough to read as ruling rather than as a border, in both themes.
     *
     * Derived from the primary rather than the outline because the rule is part of the
     * thematic layer's voice, and an outline-coloured line reads as a container edge.
     */
    val color: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
}

/**
 * Draws [count] vertical hairlines behind this element, the first [start] from the start edge
 * and each subsequent one [step] further in.
 *
 * A tree row draws one rule per ancestor level, which is what makes an expanded branch read as
 * a continuous ruled gutter even though every row is an independent item in a lazy list — the
 * rule under a parent simply stops where its last child ends, because the next row at a
 * shallower depth draws one fewer.
 */
fun Modifier.nimazMarginRules(
    count: Int,
    color: Color,
    start: Dp,
    step: Dp = NimazMarginRule.IndentStep,
    width: Dp = NimazMarginRule.Width,
    rtl: Boolean = false,
): Modifier = if (count <= 0) this else drawBehind {
    val strokeWidth = width.toPx()
    repeat(count) { level ->
        val inset = start.toPx() + step.toPx() * level
        val x = if (rtl) size.width - inset else inset
        drawLine(
            color = color,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = strokeWidth,
        )
    }
}

/**
 * The marker that sits on the rule where a row meets it.
 *
 * Hollow by default — a punched hole in the ruling. [filled] fills it with [accent], which is
 * how the passage outline says "you are reading this one" without a second colour system.
 */
@Composable
fun NimazMarginTick(
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    accent: Color = MaterialTheme.colorScheme.secondary,
    ruleColor: Color = NimazMarginRule.color,
) {
    val fill = if (filled) accent else MaterialTheme.colorScheme.surface
    val stroke = if (filled) accent else ruleColor
    Box(
        modifier = modifier
            .size(NimazMarginRule.TickSize)
            .clip(CircleShape)
            .background(fill)
            .border(2.dp, stroke, CircleShape)
    )
}
