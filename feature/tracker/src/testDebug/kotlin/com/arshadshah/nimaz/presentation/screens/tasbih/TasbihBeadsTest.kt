package com.arshadshah.nimaz.presentation.screens.tasbih

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The bead strand, drawn for real and read back off a bitmap.
 *
 * Beads mode is one of the two ways the tasbih counts, and it is **entirely** `Canvas` geometry:
 * no text, no semantics, nothing an `onNodeWith…` finder can reach. Composing the tree runs the
 * `Canvas(modifier)` call and none of `drawStrand`, so every line of the strand — the arc-length
 * table, the two bunches, the loose bead crossing the gap — was unexecuted by any test anywhere.
 *
 * What that hides is not cosmetic. The strand runs **top-right → bottom-left** by default and
 * mirrors for a left-handed user, which is a setting with no other visible confirmation: get the
 * mirror wrong and the beads advance the wrong way for exactly the users who asked for it. The
 * design registry is the same shape of problem — six materials, one `byKey` lookup, and a
 * fallback that silently returns wood for anything it does not recognise.
 *
 * The technique is #604's playbook item 5: draw the `ComposeView` into a **software**
 * `android.graphics.Canvas`, which makes Compose's `RenderNodeLayer` invoke the draw block
 * directly instead of replaying a render node. `@GraphicsMode(NATIVE)` because the assertions are
 * about pixels — under the legacy shadow canvas every draw is a no-op and the bitmap comes back
 * blank whether the strand worked or not. One draw per test, so a comparison of two renders has
 * to be two tests feeding one helper.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp")
class TasbihBeadsTest {

    // The v1 rule rather than the shared `createComponentComposeRule()`: this needs the
    // activity's own view to draw into a bitmap, which the shared fixture's rule type does not
    // expose.
    @Suppress("DEPRECATION")
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun draw(content: @Composable () -> Unit): Bitmap {
        composeRule.setContent {
            MaterialTheme {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) { content() }
            }
        }
        composeRule.waitForIdle()

        val root: View = composeRule.activity
            .findViewById<ViewGroup>(android.R.id.content)
            .getChildAt(0)
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        return bitmap
    }

    private fun strand(
        count: Int = 7,
        targetCount: Int = 33,
        design: BeadDesign = BeadDesigns.Wood,
        leftHanded: Boolean = false,
        onIncrement: () -> Unit = {},
    ): Bitmap = draw {
        TasbihBeads(
            count = count,
            onIncrement = onIncrement,
            targetCount = targetCount,
            design = design,
            leftHanded = leftHanded,
            modifier = Modifier.fillMaxSize(),
        )
    }

    /** Pixels in the given half that are not the black backdrop — i.e. strand actually painted. */
    private fun Bitmap.inkInHalf(rightHalf: Boolean): Int {
        val half = width / 2
        val left = if (rightHalf) half else 0
        val pixels = IntArray(half * height)
        getPixels(pixels, 0, half, left, 0, half, height)
        return pixels.count { it != android.graphics.Color.BLACK }
    }

    private fun Bitmap.ink(): Int = inkInHalf(false) + inkInHalf(true)

    /** Mean colour of everything painted, as (r, g, b). Ignores the backdrop. */
    private fun Bitmap.paintedColour(): Triple<Double, Double, Double> {
        val pixels = IntArray(width * height)
        getPixels(pixels, 0, width, 0, 0, width, height)
        val painted = pixels.filter { it != android.graphics.Color.BLACK }
        check(painted.isNotEmpty()) { "nothing was painted" }
        return Triple(
            painted.map { android.graphics.Color.red(it).toDouble() }.average(),
            painted.map { android.graphics.Color.green(it).toDouble() }.average(),
            painted.map { android.graphics.Color.blue(it).toDouble() }.average(),
        )
    }

    @Test
    fun `the strand paints beads across the whole surface`() {
        val bitmap = strand()

        // Edge to edge is the design: the loop is hidden and the visible strand runs off both
        // sides, so a render confined to one half means the geometry collapsed.
        assertThat(bitmap.inkInHalf(rightHalf = false)).isGreaterThan(0)
        assertThat(bitmap.inkInHalf(rightHalf = true)).isGreaterThan(0)
    }

    @Test
    fun `the counted bunch hangs toward the bottom-left for a right-handed strand`() {
        val bitmap = strand()

        // p0 sits at 82% of the width and p2 at 16%, so the strand leans left overall. This is
        // the reference the mirrored case below is measured against.
        assertThat(bitmap.inkInHalf(rightHalf = false))
            .isGreaterThan(bitmap.inkInHalf(rightHalf = true))
    }

    @Test
    fun `a left-handed strand leans the other way`() {
        val bitmap = strand(leftHanded = true)

        // The mirror is the whole of what `tasbihLeftHanded` does to the strand, and nothing else
        // on the screen reports which way the beads advance.
        assertThat(bitmap.inkInHalf(rightHalf = true))
            .isGreaterThan(bitmap.inkInHalf(rightHalf = false))
    }

    @Test
    fun `wood beads are painted in wood colours`() {
        val (r, g, b) = strand(design = BeadDesigns.Wood).paintedColour()

        // Warm: red dominates. The check that matters is against jade below — a `drawBead` that
        // ignored the design's stops would paint every material identically.
        assertThat(r).isGreaterThan(b)
        assertThat(r).isGreaterThan(g)
    }

    @Test
    fun `jade beads are painted in jade colours`() {
        val (r, g, _) = strand(design = BeadDesigns.Jade).paintedColour()

        assertThat(g).isGreaterThan(r)
    }

    @Test
    fun `an unknown design key falls back to the default rather than failing`() {
        // A persisted key from a design that has since been removed must not leave the counter
        // blank — `byKey` is the only thing standing between that and an empty screen.
        assertThat(BeadDesigns.byKey("no-such-material")).isSameInstanceAs(BeadDesigns.Default)
        assertThat(BeadDesigns.byKey(null)).isSameInstanceAs(BeadDesigns.Default)
        assertThat(BeadDesigns.byKey("jade")).isSameInstanceAs(BeadDesigns.Jade)
        assertThat(BeadDesigns.all.map { it.key }).containsNoDuplicates()
    }

    @Test
    fun `a tap on the strand counts one bead`() {
        var increments = 0
        strand(onIncrement = { increments++ })

        composeRule.onRoot().performClick()
        composeRule.waitForIdle()

        // A press with no movement is a tap, and one gesture is one bead — this is the beads-mode
        // equivalent of the classic circle's tap, and it writes to Room the same way.
        assertThat(increments).isEqualTo(1)
    }

    @Test
    fun `a flick across the gap counts one bead`() {
        var increments = 0
        strand(onIncrement = { increments++ })

        // The strand runs top-right → bottom-left, so a drag that direction is a bead crossing
        // the gap. One gesture is one bead however far it travels — a flick that counted by
        // distance would run the tasbih up by ten on a fast swipe.
        composeRule.onRoot().performTouchInput {
            swipeLeft(startX = right - 1f, endX = left + 1f, durationMillis = 200)
        }
        composeRule.waitForIdle()

        assertThat(increments).isEqualTo(1)
    }

    @Test
    fun `a drag that does not cross the gap settles back without counting`() {
        var increments = 0
        strand(onIncrement = { increments++ })

        // A finger that moves and changes its mind must not count. The crossing is tracked
        // synchronously as the gesture runs precisely so a fast flick is decided correctly —
        // which means a short one has to be decided correctly too.
        composeRule.onRoot().performTouchInput {
            val start = center
            down(start)
            moveTo(start + androidx.compose.ui.geometry.Offset(-6f, 6f))
            up()
        }
        composeRule.waitForIdle()

        assertThat(increments).isEqualTo(0)
    }

    @Test
    fun `a target of zero still draws a strand`() {
        // `beadCount` coerces to at least one; without that the modulo in the imame test divides
        // by zero and the counter crashes on a preset somebody saved with no target.
        assertThat(strand(count = 0, targetCount = 0).ink()).isGreaterThan(0)
    }
}
