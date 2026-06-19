package com.arshadshah.nimaz.presentation.screens.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * Hand-drawn Islamic artwork for the onboarding flow ("Illuminated" theme). All
 * graphics are Compose [Canvas] vectors — no image assets — so they stay crisp
 * at any size and recolour for free. Imported and composed by OnboardingScreen.
 *
 * The palette is a fixed branded deep-teal + gold (it does not follow the
 * Material light/dark scheme): the intro should feel the same for everyone.
 */

val IllumGold = Color(0xFFEAB308)
val IllumGoldDeep = Color(0xFFB8860B)
val IllumCream = Color(0xFFF5E6B8)
val IllumTextSoft = Color(0xFFCFE3DF)
private val IllumNiche = Color(0xFF0C2F2C)
private val IllumTealTop = Color(0xFF14463F)
private val IllumTealMid = Color(0xFF0A2A2A)
private val IllumTealDeep = Color(0xFF061A1C)

/** The deep-teal field behind every onboarding page. */
val illuminatedBackground: Brush = Brush.verticalGradient(
    0.0f to IllumTealTop,
    0.45f to IllumTealMid,
    1.0f to IllumTealDeep
)

/** Which emblem fills the mihrab niche on a given page. SHIELD draws on its own. */
enum class OnboardingEmblem { MOSQUE, PRAYER_TIMES, QURAN, SHIELD }

/**
 * A khatam (rub-el-hizb) geometric band, faded into the background. Drawn as a
 * repeated tile of an overlapping square + diamond — the classic Islamic motif —
 * across the top of each page.
 */
@Composable
fun KhatamBand(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val tile = 34.dp.toPx()
        val stroke = 1.dp.toPx()
        var y = 0f
        while (y < size.height) {
            var x = 0f
            while (x < size.width) {
                val fade = 1f - (y / size.height).coerceIn(0f, 1f) // brighter at top
                val c = IllumGold.copy(alpha = 0.5f * fade)
                val h = tile / 2f
                // diamond
                drawPath(
                    Path().apply {
                        moveTo(x + h, y + tile * 0.12f)
                        lineTo(x + tile * 0.88f, y + h)
                        lineTo(x + h, y + tile * 0.88f)
                        lineTo(x + tile * 0.12f, y + h)
                        close()
                    },
                    color = c, style = Stroke(width = stroke)
                )
                // square
                drawRect(
                    color = c,
                    topLeft = Offset(x + tile * 0.24f, y + tile * 0.24f),
                    size = Size(tile * 0.52f, tile * 0.52f),
                    style = Stroke(width = stroke)
                )
                x += tile
            }
            y += tile
        }
    }
}

/**
 * The mihrab arch with its per-page emblem (or, for [OnboardingEmblem.SHIELD], a
 * shield enclosing a khatam star). Art is authored in a 116×150 box and scaled
 * to fit [modifier]'s size, centred.
 */
@Composable
fun OnboardingEmblem(
    kind: OnboardingEmblem,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val vbw = 116f
        val vbh = 150f
        val scale = min(size.width / vbw, size.height / vbh)
        val ox = (size.width - vbw * scale) / 2f
        val oy = (size.height - vbh * scale) / 2f
        fun p(x: Float, y: Float) = Offset(ox + x * scale, oy + y * scale)
        fun sw(w: Float) = w * scale
        fun poly(vararg pts: Pair<Float, Float>) = Path().apply {
            pts.forEachIndexed { i, (x, y) ->
                val o = p(x, y)
                if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y)
            }
            close()
        }
        fun line(ax: Float, ay: Float, bx: Float, by: Float, w: Float, color: Color = IllumGold) =
            drawLine(color, p(ax, ay), p(bx, by), strokeWidth = sw(w), cap = androidx.compose.ui.graphics.StrokeCap.Round)

        if (kind == OnboardingEmblem.SHIELD) {
            // Shield outline
            drawPath(
                Path().apply {
                    val a = p(58f, 18f); moveTo(a.x, a.y)
                    p(90f, 30f).let { lineTo(it.x, it.y) }
                    p(90f, 58f).let { lineTo(it.x, it.y) }
                    val q1 = p(90f, 90f); val e1 = p(58f, 104f); quadraticTo(q1.x, q1.y, e1.x, e1.y)
                    val q2 = p(26f, 90f); val e2 = p(26f, 58f); quadraticTo(q2.x, q2.y, e2.x, e2.y)
                    p(26f, 30f).let { lineTo(it.x, it.y) }
                    close()
                },
                color = IllumGold, style = Stroke(width = sw(2.5f))
            )
            // Khatam star: octagon + square
            drawPath(
                poly(58f to 44f, 69f to 49f, 74f to 60f, 69f to 71f, 58f to 76f, 47f to 71f, 42f to 60f, 47f to 49f),
                color = IllumGold, style = Stroke(width = sw(1.6f))
            )
            drawRect(
                color = IllumGold,
                topLeft = p(47f, 49f),
                size = Size(sw(22f), sw(22f)),
                style = Stroke(width = sw(1.6f))
            )
            return@Canvas
        }

        // ---- Mihrab arch (outline + niche fill) ----
        val nichePath = Path().apply {
            val a = p(22f, 150f); moveTo(a.x, a.y)
            p(22f, 62f).let { lineTo(it.x, it.y) }
            val q1 = p(22f, 22f); val e1 = p(58f, 22f); quadraticTo(q1.x, q1.y, e1.x, e1.y)
            val q2 = p(94f, 22f); val e2 = p(94f, 62f); quadraticTo(q2.x, q2.y, e2.x, e2.y)
            p(94f, 150f).let { lineTo(it.x, it.y) }
            close()
        }
        drawPath(nichePath, color = IllumNiche.copy(alpha = 0.6f))

        val archPath = Path().apply {
            val a = p(12f, 150f); moveTo(a.x, a.y)
            p(12f, 60f).let { lineTo(it.x, it.y) }
            val q1 = p(12f, 12f); val e1 = p(58f, 12f); quadraticTo(q1.x, q1.y, e1.x, e1.y)
            val q2 = p(104f, 12f); val e2 = p(104f, 60f); quadraticTo(q2.x, q2.y, e2.x, e2.y)
            p(104f, 150f).let { lineTo(it.x, it.y) }
        }
        drawPath(
            archPath,
            brush = Brush.verticalGradient(listOf(IllumGold, IllumGoldDeep)),
            style = Stroke(width = sw(2.5f))
        )

        when (kind) {
            OnboardingEmblem.MOSQUE -> {
                drawCircle(IllumGold, radius = sw(15f), center = p(58f, 74f))                 // dome
                drawRect(IllumGold, topLeft = p(44f, 74f), size = Size(sw(30f), sw(34f)))      // body
                drawPath(poly(58f to 52f, 62f to 62f, 54f to 62f), color = IllumGold)          // finial
                drawRoundRect(IllumGold, topLeft = p(30f, 64f), size = Size(sw(6f), sw(44f)), cornerRadius = CornerRadius(sw(3f))) // minaret L
                drawRoundRect(IllumGold, topLeft = p(80f, 64f), size = Size(sw(6f), sw(44f)), cornerRadius = CornerRadius(sw(3f))) // minaret R
                drawCircle(IllumGold, radius = sw(4f), center = p(33f, 63f))
                drawCircle(IllumGold, radius = sw(4f), center = p(83f, 63f))
                drawRoundRect(IllumNiche, topLeft = p(51f, 92f), size = Size(sw(14f), sw(16f)), cornerRadius = CornerRadius(sw(7f))) // door
            }
            OnboardingEmblem.PRAYER_TIMES -> {
                line(30f, 96f, 86f, 96f, 2f)                                                   // horizon
                line(58f, 58f, 58f, 48f, 2f)                                                   // rays
                line(44f, 64f, 38f, 56f, 2f)
                line(72f, 64f, 78f, 56f, 2f)
                line(35f, 80f, 26f, 78f, 2f)
                line(81f, 80f, 90f, 78f, 2f)
                drawPath(                                                                      // rising sun
                    Path().apply {
                        val a = p(44f, 96f); moveTo(a.x, a.y)
                        arcTo(Rect(p(44f, 82f).x, p(44f, 82f).y, p(72f, 110f).x, p(72f, 110f).y), 180f, 180f, false)
                        close()
                    },
                    color = IllumGold
                )
                drawCircle(IllumCream, radius = sw(9f), center = p(80f, 44f))                  // crescent
                drawCircle(IllumNiche, radius = sw(7f), center = p(83.5f, 44f))
            }
            OnboardingEmblem.QURAN -> {
                line(58f, 40f, 58f, 30f, 1.5f)                                                 // noor rays
                line(44f, 44f, 39f, 36f, 1.5f)
                line(72f, 44f, 77f, 36f, 1.5f)
                drawPath(poly(58f to 96f, 30f to 88f, 30f to 64f, 58f to 72f), color = IllumGold)     // left page
                drawPath(poly(58f to 96f, 86f to 88f, 86f to 64f, 58f to 72f), color = IllumGoldDeep) // right page
                line(36f, 74f, 52f, 78f, 1.4f, IllumNiche)                                     // text lines
                line(36f, 80f, 52f, 84f, 1.4f, IllumNiche)
                line(64f, 78f, 80f, 74f, 1.4f, IllumNiche)
                line(64f, 84f, 80f, 80f, 1.4f, IllumNiche)
            }
            OnboardingEmblem.SHIELD -> Unit // handled above
        }
    }
}

// Previews

@Preview(name = "Emblems", widthDp = 360, heightDp = 220)
@Composable
private fun OnboardingEmblemsPreview() {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .background(illuminatedBackground)
            .size(360.dp, 220.dp)
    ) {
        OnboardingEmblem(OnboardingEmblem.MOSQUE, Modifier.size(90.dp, 220.dp))
        OnboardingEmblem(OnboardingEmblem.PRAYER_TIMES, Modifier.size(90.dp, 220.dp))
        OnboardingEmblem(OnboardingEmblem.QURAN, Modifier.size(90.dp, 220.dp))
        OnboardingEmblem(OnboardingEmblem.SHIELD, Modifier.size(90.dp, 220.dp))
    }
}

@Preview(name = "Khatam Band", widthDp = 360, heightDp = 70)
@Composable
private fun KhatamBandPreview() {
    KhatamBand(
        modifier = Modifier
            .background(IllumTealDeep)
            .size(360.dp, 70.dp)
    )
}
