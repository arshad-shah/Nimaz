package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.os.Build
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Visual weight of a [GlassPill]. All three are derived from a single `tint`
 * (white by default) so the pill stays legible over any sky gradient.
 *
 * - [Frosted] — the everyday legibility pill: a soft fill plus the lit glass edge.
 * - [Solid]   — heavier fill for titles / primary emphasis.
 * - [Ghost]   — no fill, only the lit edge; lets the sky show through the most.
 */
enum class GlassPillTone { Frosted, Solid, Ghost }

/** Compact label vs. the default. Controls padding, icon size and the icon gap. */
enum class GlassPillSize { Small, Medium }

/** A sensible default blur strength for the frosted-glass effect. */
val DefaultGlassBlur: Dp = 24.dp

/**
 * A shared handle that lets a glass surface blur whatever is drawn behind it —
 * the real "frosted pane" property, not just a translucent fill.
 *
 * Wiring is two-sided and dependency-free:
 * 1. mark the content to be blurred with [Modifier.glassBackdropSource] — it
 *    records that content into a [GraphicsLayer];
 * 2. pass the same backdrop to a [GlassPill] / [GlassIconButton], which samples
 *    the region under itself and draws it blurred beneath its fill.
 *
 * Real blur needs Android 12+ (API 31); on older devices the surface falls back
 * to its (slightly stronger) translucent fill, which stays legible.
 *
 * Create one with [rememberGlassBackdrop].
 */
@Stable
class GlassBackdrop internal constructor(internal val layer: GraphicsLayer) {
    /** Where the source content sits in the window, so the glass can align to it. */
    internal var sourcePositionInRoot by mutableStateOf(Offset.Zero)
}

/** Remembers a [GlassBackdrop] backed by a recording [GraphicsLayer]. */
@Composable
fun rememberGlassBackdrop(): GlassBackdrop {
    val layer = rememberGraphicsLayer()
    return remember(layer) { GlassBackdrop(layer) }
}

/**
 * Marks this content as the thing a glass surface blurs. Records the node's
 * drawing into [backdrop]'s layer (then draws it normally) and tracks its
 * position so overlaid glass can sample the right region.
 */
fun Modifier.glassBackdropSource(backdrop: GlassBackdrop): Modifier = this
    .onGloballyPositioned { backdrop.sourcePositionInRoot = it.positionInRoot() }
    .drawWithContent {
        backdrop.layer.record { this@drawWithContent.drawContent() }
        drawLayer(backdrop.layer)
    }

/**
 * A translucent "frosted glass" pill used to keep overlay text legible over the
 * living sky regardless of the time-of-day gradient behind it.
 *
 * The refinement over a plain translucent box is the **glass edge**: a hairline
 * border that fades from a bright top rim to a dim base, the way real frosted
 * glass catches light. Combined with a richer fill it reads as a lit pane rather
 * than a flat rectangle.
 *
 * Everything is derived from [tint] (white by default), so passing an accent
 * colour — gold for "soon", green for "done" — re-skins the whole pill coherently.
 *
 * @param leadingIcon  optional icon shown before the text, tinted to match it.
 * @param trailingIcon optional icon shown after the text (e.g. a chevron).
 * @param tone         visual weight — see [GlassPillTone].
 * @param size         compact vs. default metrics — see [GlassPillSize].
 * @param tint         base colour the fill, edge and content are derived from.
 * @param backdrop     when set (see [rememberGlassBackdrop]), the pill blurs the
 *                     content behind it for a true frosted-glass surface.
 * @param blurRadius   how strongly the backdrop is blurred; ignored without a
 *                     [backdrop]. Set to 0.dp for a flat translucent fill.
 * @param onClick      makes the whole pill tappable when provided.
 */
@Composable
fun GlassPill(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelLarge,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    tone: GlassPillTone = GlassPillTone.Frosted,
    size: GlassPillSize = GlassPillSize.Medium,
    tint: Color = Color.White,
    backdrop: GlassBackdrop? = null,
    blurRadius: Dp = DefaultGlassBlur,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(percent = 100)

    val (hPad, vPad, iconSize, gap) = when (size) {
        GlassPillSize.Small -> PillMetrics(10.dp, 4.dp, 14.dp, 4.dp)
        GlassPillSize.Medium -> PillMetrics(14.dp, 7.dp, 18.dp, 6.dp)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(gap),
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .glassSurface(tone = tone, tint = tint, shape = shape, backdrop = backdrop, blurRadius = blurRadius)
            .padding(horizontal = hPad, vertical = vPad),
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(iconSize),
            )
        }
        Text(text = text, style = style, color = tint)
        if (trailingIcon != null) {
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

private data class PillMetrics(
    val hPad: Dp,
    val vPad: Dp,
    val iconSize: Dp,
    val gap: Dp,
)

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
private fun Modifier.glassSurface(
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

/**
 * Draws the slice of [backdrop] sitting directly behind this node, blurred and
 * clipped to [shape], beneath the node's own content. Needs API 31+; below that
 * it is a no-op and the translucent fill carries the legibility on its own.
 */
@Composable
private fun Modifier.glassBlur(
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

/**
 * The circular, icon-only sibling of [GlassPill] — same frosted fill and lit
 * edge, sized as a tap target. Use it for overlay actions that sit on the
 * living sky, e.g. the settings button in the home top bar.
 *
 * Because everything derives from [tint], a caller can cross-fade the whole
 * button from white (over sky) to a surface colour (over a solid bar) just by
 * lerping the tint — no separate scrim needed.
 *
 * @param tone visual weight — see [GlassPillTone].
 * @param size [GlassPillSize.Small] (36dp) or [GlassPillSize.Medium] (44dp) target.
 * @param tint base colour the fill, edge and icon are derived from.
 * @param backdrop   when set, blurs the content behind the button (see [rememberGlassBackdrop]).
 * @param blurRadius backdrop blur strength; ignored without a [backdrop].
 */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: GlassPillTone = GlassPillTone.Frosted,
    size: GlassPillSize = GlassPillSize.Medium,
    tint: Color = Color.White,
    backdrop: GlassBackdrop? = null,
    blurRadius: Dp = DefaultGlassBlur,
) {
    val (target, iconSize) = when (size) {
        GlassPillSize.Small -> 36.dp to 18.dp
        GlassPillSize.Medium -> 44.dp to 22.dp
    }
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(target)
            .glassSurface(tone = tone, tint = tint, shape = CircleShape, backdrop = backdrop, blurRadius = blurRadius),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}

// ---------------------------------------------------------------------------
// Previews — rendered over representative sky gradients (a white pill on a
// white canvas is invisible), so the frosted effect actually reads.
// ---------------------------------------------------------------------------

/** A stand-in for the real time-of-day sky so previews show true contrast. */
@Composable
private fun SkyPreview(
    colors: List<Color>,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(width = 320.dp, height = 200.dp)
            .background(Brush.verticalGradient(colors)),
        contentAlignment = Alignment.Center,
    ) { content() }
}

private val MiddaySky = listOf(
    Color(0xFF0A2E7A), Color(0xFF1E62D6), Color(0xFF4F9BF5), Color(0xFFBFE0FB)
)
private val DuskSky = listOf(
    Color(0xFF2B3A8C), Color(0xFF7C6AB0), Color(0xFFE59AB0), Color(0xFFFBB778)
)
private val NightSky = listOf(
    Color(0xFF03060F), Color(0xFF0A0F26), Color(0xFF141A38), Color(0xFF33285E)
)
private val DawnSky = listOf(
    Color(0xFF16204A), Color(0xFF3B3270), Color(0xFF8A4F6E), Color(0xFFD08A5E)
)

private val pillShadow = Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 1f), 4f)

@Preview(name = "Tones · midday", showBackground = true)
@Composable
private fun GlassPill_Tones() {
    NimazTheme {
        SkyPreview(MiddaySky) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassPill("Frosted", tone = GlassPillTone.Frosted)
                GlassPill("Solid", tone = GlassPillTone.Solid)
                GlassPill("Ghost", tone = GlassPillTone.Ghost)
            }
        }
    }
}

@Preview(name = "Leading icons · dusk", showBackground = true)
@Composable
private fun GlassPill_LeadingIcons() {
    NimazTheme {
        SkyPreview(DuskSky) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassPill("Manchester, UK", leadingIcon = Icons.Filled.Place)
                GlassPill("Maghrib", leadingIcon = Icons.Filled.WbSunny, tone = GlassPillTone.Solid)
                GlassPill("3 reminders", leadingIcon = Icons.Filled.Notifications, tone = GlassPillTone.Ghost)
            }
        }
    }
}

@Preview(name = "Accent tints · night", showBackground = true)
@Composable
private fun GlassPill_Tints() {
    NimazTheme {
        SkyPreview(NightSky) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassPill(
                    "Completed",
                    leadingIcon = Icons.Filled.Check,
                    tone = GlassPillTone.Solid,
                    tint = Color(0xFF7FE3A4),
                )
                GlassPill(
                    "Starts soon",
                    leadingIcon = Icons.Filled.WbSunny,
                    tint = Color(0xFFFFD27D),
                )
                GlassPill("Details", trailingIcon = Icons.Filled.KeyboardArrowDown)
            }
        }
    }
}

@Preview(name = "Sizes · dawn", showBackground = true)
@Composable
private fun GlassPill_Sizes() {
    NimazTheme {
        SkyPreview(DawnSky) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassPill("Medium", leadingIcon = Icons.Filled.Place, size = GlassPillSize.Medium)
                GlassPill("Small", leadingIcon = Icons.Filled.Place, size = GlassPillSize.Small)
            }
        }
    }
}

@Preview(name = "Icon buttons · midday", showBackground = true)
@Composable
private fun GlassIconButton_Tones() {
    NimazTheme {
        SkyPreview(MiddaySky) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassIconButton(Icons.Filled.Settings, "Settings", onClick = {})
                GlassIconButton(Icons.Filled.Search, "Search", onClick = {}, tone = GlassPillTone.Solid)
                GlassIconButton(Icons.Filled.Notifications, "Alerts", onClick = {}, tone = GlassPillTone.Ghost)
                GlassIconButton(Icons.Filled.Place, "Location", onClick = {}, size = GlassPillSize.Small)
            }
        }
    }
}

@Preview(name = "Backdrop blur · frosted glass", showBackground = true)
@Composable
private fun GlassPill_BackdropBlur() {
    NimazTheme {
        val backdrop = rememberGlassBackdrop()
        val dots = listOf(
            Color(0xFFFFB000), Color(0xFFFF5C5C), Color(0xFF4ADE80),
            Color(0xFF38BDF8), Color(0xFFC084FC), Color(0xFFFF8FA3),
        )
        Box(
            modifier = Modifier.size(width = 320.dp, height = 200.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Sharp, high-frequency content so the blur is unmistakable.
            Row(
                modifier = Modifier
                    .size(width = 320.dp, height = 200.dp)
                    .glassBackdropSource(backdrop)
                    .background(Brush.verticalGradient(MiddaySky)),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                dots.forEach { c ->
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(c, CircleShape),
                    )
                }
            }
            GlassPill(
                text = "Frosted glass",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                leadingIcon = Icons.Filled.WbSunny,
                tone = GlassPillTone.Solid,
                backdrop = backdrop,
            )
        }
    }
}

@Preview(name = "In context · title + status", showBackground = true)
@Composable
private fun GlassPill_InContext() {
    NimazTheme {
        SkyPreview(DawnSky) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassPill(
                    text = "5:42 AM",
                    style = MaterialTheme.typography.titleLarge.copy(
                        shadow = pillShadow,
                        fontWeight = FontWeight.Bold,
                    ),
                    leadingIcon = Icons.Filled.WbSunny,
                    tone = GlassPillTone.Solid,
                )
                GlassPill(
                    text = "Fajr begins",
                    style = MaterialTheme.typography.labelMedium.copy(shadow = pillShadow),
                )
            }
        }
    }
}
