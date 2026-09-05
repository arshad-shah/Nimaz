package com.arshadshah.nimaz.presentation.components.atoms

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.testing.drawToBitmap
import com.arshadshah.nimaz.testing.ink
import com.arshadshah.nimaz.testing.region
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The solar arc, drawn for real and read back off a bitmap.
 *
 * **Every assertion in `NimazSolarArcTest` passes against an arc that draws nothing.** The whole
 * component is one `Canvas` whose `DrawScope` lambda never runs when the tree is merely composed,
 * and `clearAndSetSemantics` collapses it to a single node — so the semantics tree is identical
 * whether the geometry is right, wrong, or absent. "Dhuhr sits at the apex" is a claim only a
 * draw pass can settle, and it is the claim the component exists to make.
 *
 * `@GraphicsMode(NATIVE)` because the assertions are about pixels: under the legacy shadow canvas
 * every draw is a no-op and the bitmap comes back blank whether the arc worked or not. Density is
 * pinned to mdpi so 1dp is 1px and the expected coordinates below are readable as dp.
 *
 * See `SoftwareCanvas.kt` for why this route rather than `captureToImage()` (#604).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-mdpi")
class NimazSolarArcArtTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val arcWidth = 300
    private val arcHeight = 108

    /** `horizonY` is 62% of the height — see `NimazSolarArc`. */
    private val horizonRow = (arcHeight * 0.62f).toInt()

    private val labelled = listOf(
        NimazSolarNode(0.10f, "Fajr", NimazTone.MUTED, "Fajr"),
        NimazSolarNode(0.30f, "Sunrise", NimazTone.ACCENT, "Sunrise"),
        NimazSolarNode(0.50f, "Dhuhr", NimazTone.PROMINENT, "Dhuhr"),
        NimazSolarNode(0.65f, "Asr", NimazTone.WARNING, "Asr"),
        NimazSolarNode(0.80f, "Maghrib", NimazTone.WARNING, "Maghrib"),
        NimazSolarNode(0.92f, "Isha", NimazTone.MUTED, "Isha"),
    )

    /** The first row carrying paint in a vertical strip — i.e. how high the curve reaches there. */
    private fun Bitmap.topInkRow(x: Int, w: Int, rows: Int): Int {
        val pixels = region(x, 0, w, rows)
        for (row in 0 until rows) {
            for (col in 0 until w) {
                if (pixels[row * w + col] != android.graphics.Color.BLACK) return row
            }
        }
        return rows
    }

    /**
     * The whole reason the geometry is a closed form rather than a hand-drawn parabola.
     *
     * Sunrise 0.40 and sunset 0.90 put solar noon at 0.65 — well right of the drawing's middle.
     * A symmetric curve would peak at 150px; this must peak at 195px. The test also pins that
     * the pre-dawn stretch is drawn *below* the horizon, which is what makes Fajr and Isha
     * legible as night rather than as two dots that fell off the curve.
     */
    @Test
    fun `the apex sits at solar noon, not at the middle of the drawing`() {
        val bitmap = composeRule.drawToBitmap {
            Box(Modifier.width(arcWidth.dp).height(arcHeight.dp)) {
                NimazSolarArc(
                    nodes = emptyList(),
                    sunriseFraction = 0.40f,
                    sunsetFraction = 0.90f,
                    contentDescription = "Asymmetric day",
                    height = arcHeight.dp,
                )
            }
        }

        // 0.65 of 300px = 195px: solar noon, and the highest the curve goes.
        val atSolarNoon = bitmap.topInkRow(x = 190, w = 10, rows = arcHeight)
        // 150px is the middle of the drawing, which a symmetric curve would peak at.
        val atMiddle = bitmap.topInkRow(x = 145, w = 10, rows = arcHeight)

        // The asymmetry is the whole claim: the curve is meaningfully higher at 0.65 than at 0.5.
        assertThat(atSolarNoon).isLessThan(atMiddle)
        assertThat(atSolarNoon).isLessThan(horizonRow)

        // Below the horizon, only the pre-dawn stretch carries paint — solar noon has none. Read
        // two rows under the line, because the dashed horizon itself spans the full width and is
        // ink in every column.
        val belowTop = horizonRow + 2
        val belowHeight = arcHeight - belowTop
        val beforeDawnBelow = bitmap.region(40, belowTop, 10, belowHeight).ink()
        val solarNoonBelow = bitmap.region(190, belowTop, 10, belowHeight).ink()

        assertThat(beforeDawnBelow).isGreaterThan(0)
        assertThat(solarNoonBelow).isEqualTo(0)
    }

    /**
     * The sun rides the curve on today and is absent on every other day — which is most days a
     * reader looks at. Both variants are drawn in one composition because `setContent` may only
     * be called once per rule, so a before/after has to share a bitmap.
     */
    @Test
    fun `the sun is drawn on today and absent on any other day`() {
        val bitmap = composeRule.drawToBitmap {
            Column {
                Box(Modifier.width(arcWidth.dp).height(arcHeight.dp)) {
                    NimazSolarArc(
                        nodes = emptyList(),
                        sunriseFraction = 0.25f,
                        sunsetFraction = 0.75f,
                        contentDescription = "Today",
                        sunPosition = 0.5f,
                        height = arcHeight.dp,
                    )
                }
                Box(Modifier.width(arcWidth.dp).height(arcHeight.dp)) {
                    NimazSolarArc(
                        nodes = emptyList(),
                        sunriseFraction = 0.25f,
                        sunsetFraction = 0.75f,
                        contentDescription = "Another day",
                        sunPosition = null,
                        height = arcHeight.dp,
                    )
                }
            }
        }

        // A 30px box centred on the apex, where the sun sits at t = 0.5.
        val withSun = bitmap.region(135, 5, 30, 30).ink()
        val withoutSun = bitmap.region(135, 5 + arcHeight, 30, 30).ink()

        assertThat(withSun).isGreaterThan(withoutSun)
    }

    /**
     * Six labels do not fit across 300dp at a large font scale, so they drop out and the drawing
     * degrades to a legible diagram rather than to overlapping text.
     *
     * A **degenerate** day (sunset before sunrise) is deliberate: it flattens the curve to the
     * horizon line, which removes the confound that dropping the labels also reclaims their band
     * and makes the curve taller. With a flat curve the two halves of this bitmap differ in
     * exactly one thing — whether the labels were painted.
     */
    @Test
    fun `labels drop out at a large font scale`() {
        val bitmap = composeRule.drawToBitmap {
            Column {
                Box(Modifier.width(arcWidth.dp).height(arcHeight.dp)) {
                    NimazSolarArc(
                        nodes = labelled,
                        sunriseFraction = 0.80f,
                        sunsetFraction = 0.20f,
                        contentDescription = "Normal scale",
                        height = arcHeight.dp,
                    )
                }
                CompositionLocalProvider(
                    LocalDensity provides Density(density = 1f, fontScale = 2f)
                ) {
                    Box(Modifier.width(arcWidth.dp).height(arcHeight.dp)) {
                        NimazSolarArc(
                            nodes = labelled,
                            sunriseFraction = 0.80f,
                            sunsetFraction = 0.20f,
                            contentDescription = "Large scale",
                            height = arcHeight.dp,
                        )
                    }
                }
            }
        }

        val normalScale = bitmap.region(0, 0, arcWidth, arcHeight).ink()
        val largeScale = bitmap.region(0, arcHeight, arcWidth, arcHeight).ink()

        // The curve, horizon and six dots are drawn in both. Only the labels differ.
        assertThat(largeScale).isGreaterThan(0)
        assertThat(normalScale).isGreaterThan(largeScale)
    }

    /** A bad node position must skip that node, not take the whole draw pass down with it. */
    @Test
    fun `a node with a NaN position is skipped rather than crashing the draw`() {
        val bitmap = composeRule.drawToBitmap {
            Box(Modifier.width(arcWidth.dp).height(arcHeight.dp)) {
                NimazSolarArc(
                    nodes = listOf(
                        NimazSolarNode(Float.NaN, "Broken", NimazTone.ERROR, "Broken"),
                        NimazSolarNode(0.5f, "Dhuhr", NimazTone.PROMINENT, "Dhuhr"),
                    ),
                    sunriseFraction = 0.25f,
                    sunsetFraction = 0.75f,
                    contentDescription = "With a bad node",
                    height = arcHeight.dp,
                )
            }
        }

        assertThat(bitmap.region(0, 0, arcWidth, arcHeight).ink()).isGreaterThan(0)
    }

    /** A span that lies outside the day draws no window rather than an empty path. */
    @Test
    fun `a lit span outside the day is drawn as nothing`() {
        val bitmap = composeRule.drawToBitmap {
            Box(Modifier.width(arcWidth.dp).height(arcHeight.dp)) {
                NimazSolarArc(
                    nodes = emptyList(),
                    sunriseFraction = 0.25f,
                    sunsetFraction = 0.75f,
                    contentDescription = "Span off the end",
                    litSpan = 2f..3f,
                    height = arcHeight.dp,
                )
            }
        }

        assertThat(bitmap.region(0, 0, arcWidth, arcHeight).ink()).isGreaterThan(0)
    }
}
