package com.arshadshah.nimaz.testing

import android.graphics.Bitmap
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
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.ext.junit.rules.ActivityScenarioRule

/**
 * Draws a composable for real and hands back the pixels.
 *
 * **Why this exists.** `:core:ui` is the module that *owns* the app's drawn surfaces — the
 * fractured shamsa on every failure screen, the qibla dial, the manuscript pattern behind the
 * Quran reader, the celebration burst. Every one of them is `Canvas`/`drawBehind` geometry, and
 * composing the tree runs the `Canvas(modifier)` call while its `DrawScope` lambda never
 * executes: a `when` that fell through to the wrong lobe count, or a radius fraction typed as
 * `0.48f` instead of `0.84f`, compiles, previews and asserts clean against the semantics tree.
 *
 * Asking the `ComposeView` to draw itself into a **software** `android.graphics.Canvas` is what
 * makes those lines run: Compose's `RenderNodeLayer` invokes the draw block directly rather than
 * replaying a render node when the canvas it is handed is not hardware-accelerated. The caller
 * must be annotated `@GraphicsMode(GraphicsMode.Mode.NATIVE)` — under Robolectric's legacy shadow
 * canvas every draw is a no-op and the bitmap comes back uniformly blank whether the geometry
 * worked or not.
 *
 * `captureToImage()` is the route this deliberately does not take: it goes through `PixelCopy` on
 * a real window and hangs under Robolectric (#604).
 *
 * **One draw per test.** `setContent` may only be called once on a rule, so a before/after
 * comparison has to be two tests, or one composition that puts both variants on screen at once.
 */
typealias ActivityRule = AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity>

/**
 * Composes [content] over a black field and draws the result into a bitmap.
 *
 * [advanceMillis] moves a **pinned** clock on before the draw. An infinite transition sits at its
 * initial value until the clock turns, so a shimmer or a rotation drawn at t=0 is indistinguishable
 * from one that never animates — set `mainClock.autoAdvance = false` and advance to the point in
 * the cycle the assertion is about.
 */
fun ActivityRule.drawToBitmap(
    advanceMillis: Long = 0,
    content: @Composable () -> Unit,
): Bitmap {
    setContent {
        MaterialTheme {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) { content() }
        }
    }
    waitForIdle()
    if (advanceMillis > 0) {
        mainClock.advanceTimeBy(advanceMillis)
        waitForIdle()
    }

    val root: View = activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
    val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
    root.draw(android.graphics.Canvas(bitmap))
    return bitmap
}

/** Every pixel of the bitmap, row-major. */
fun Bitmap.allPixels(): IntArray =
    IntArray(width * height).also { getPixels(it, 0, width, 0, 0, width, height) }

/** The pixels of one rectangle, row-major. */
fun Bitmap.region(left: Int, top: Int, w: Int, h: Int): IntArray =
    IntArray(w * h).also { getPixels(it, 0, w, left, top, w, h) }

/** Pixels that are not the black backdrop — i.e. paint the drawing actually laid down. */
fun IntArray.ink(): Int = count { it != android.graphics.Color.BLACK }

/** How many distinct colours appear — a flat fill has one, a gradient has many. */
fun IntArray.distinctColours(): Int = toSet().size

/** Mean luminance, for comparing how strongly two regions were painted. */
fun IntArray.brightness(): Double = map {
    (android.graphics.Color.red(it) + android.graphics.Color.green(it) +
        android.graphics.Color.blue(it)) / 3.0
}.average()
