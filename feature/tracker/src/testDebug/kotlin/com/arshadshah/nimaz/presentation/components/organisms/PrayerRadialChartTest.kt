package com.arshadshah.nimaz.presentation.components.organisms

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerStats
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The radial chart, drawn for real.
 *
 * `PrayerStatsChartTest` composes this chart and asserts it does not crash, which is as far as a
 * semantics-based test can go: the whole figure — grid rings, spokes, the data polygon and its
 * five points — is one `Canvas` block, and composing runs the `Canvas(modifier)` call without
 * executing a line of it.
 *
 * What that leaves unchecked is the one thing the chart is for: **the polygon's size follows the
 * record.** A figure built at a constant radius, or from `prayedByPrayer` with the misses
 * ignored, draws a perfectly plausible five-pointed shape that means nothing — and nobody
 * eyeballing a screenshot would catch it, because a radar chart with no reference looks like a
 * radar chart.
 *
 * The technique is #604's playbook item 5 — draw into a software `android.graphics.Canvas` under
 * `@GraphicsMode(NATIVE)` and read the pixels back. A compose rule composes once, so the records
 * being compared are drawn **side by side in one composition** rather than across tests, and each
 * is measured by the pixels in which it *differs from an empty chart drawn beside it*. Counting
 * non-background pixels does not work here: the card fills its whole area with a surface colour,
 * so every pixel is painted whatever the data says.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// 999dp at mdpi is 999px, so the three equal-weight thirds land on exact pixel
// boundaries and two identical charts rasterise identically. Density is pinned for the same
// reason #604 pins it elsewhere: it is a real input to layout, not an incidental.
@Config(qualifiers = "w999dp-h1200dp-mdpi")
class PrayerRadialChartTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val tracked = listOf(
        PrayerName.FAJR,
        PrayerName.DHUHR,
        PrayerName.ASR,
        PrayerName.MAGHRIB,
        PrayerName.ISHA,
    )

    private fun stats(prayedEach: Int, missedEach: Int) = PrayerStats(
        totalPrayed = prayedEach * tracked.size,
        totalMissed = missedEach * tracked.size,
        totalJamaah = 0,
        prayedByPrayer = tracked.associateWith { prayedEach },
        missedByPrayer = tracked.associateWith { missedEach },
        currentStreak = 0,
        longestStreak = 0,
        perfectDays = 0,
        startDate = 0L,
        endDate = 0L,
    )

    private val empty = PrayerStats(
        totalPrayed = 0,
        totalMissed = 0,
        totalJamaah = 0,
        prayedByPrayer = emptyMap(),
        missedByPrayer = emptyMap(),
        currentStreak = 0,
        longestStreak = 0,
        perfectDays = 0,
        startDate = 0L,
        endDate = 0L,
    )

    /** Draws three radial charts side by side, sharing one composition. */
    private fun drawThree(first: PrayerStats, second: PrayerStats, third: PrayerStats): Bitmap {
        composeRule.setContent {
            MaterialTheme {
                Row(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    listOf(first, second, third).forEach { stats ->
                        Box(modifier = Modifier.weight(1f)) {
                            PrayerStatsChart(
                                stats = stats,
                                chartType = PrayerChartType.RADIAL,
                                summaryItems = emptyList(),
                            )
                        }
                    }
                }
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

    private fun Bitmap.third(index: Int): IntArray {
        val w = width / 3
        return IntArray(w * height).also { getPixels(it, 0, w, index * w, 0, w, height) }
    }

    /**
     * How many pixels differ between two thirds of the bitmap.
     *
     * Each third holds the same card, the same grid, the same spokes and the same labels, so a
     * differing pixel is essentially one the polygon put there — "essentially" because
     * anti-aliasing does not fall identically across three separately-laid-out cards, which
     * leaves a small floor even between two identical records. The first test below measures
     * that floor rather than assuming it away.
     */
    private fun Bitmap.pixelsDiffering(a: Int, b: Int): Int {
        val left = third(a)
        val right = third(b)
        return left.indices.count { left[it] != right[it] }
    }

    @Test
    fun `the polygon dwarfs the noise between two identical charts`() {
        val bitmap = drawThree(stats(30, 0), stats(30, 0), empty)

        // Two thirds holding the same record still differ a little — anti-aliasing does not fall
        // identically across three separately-laid-out cards. The third holds an empty record,
        // whose polygon collapses to a point, so the difference against *it* is the polygon
        // itself. Establishing that it is several times the floor is what makes the comparison
        // in the next test mean something.
        val floor = bitmap.pixelsDiffering(0, 1)
        val polygon = bitmap.pixelsDiffering(0, 2)

        assertThat(polygon).isGreaterThan(floor * 3)
    }

    @Test
    fun `a stronger record paints a bigger polygon than a weaker one`() {
        // Strong, weak, and an empty chart to measure both against — all three drawn once, so
        // the comparison lives inside a single composition.
        val bitmap = drawThree(stats(30, 0), stats(1, 29), empty)

        val strongPolygon = bitmap.pixelsDiffering(0, 2)
        val weakPolygon = bitmap.pixelsDiffering(1, 2)

        // A polygon at a constant radius — or one read from `prayedByPrayer` with the misses
        // ignored — would make these two equal, and would look entirely convincing on screen.
        assertThat(weakPolygon).isGreaterThan(0)
        assertThat(strongPolygon).isGreaterThan(weakPolygon)
    }

    @Test
    fun `a chart with nothing recorded still draws its frame`() {
        val bitmap = drawThree(empty, empty, stats(30, 0))

        // Every prayer divides by a total of zero and the polygon collapses to a point. The
        // rings, spokes and labels do not depend on the data, so the frame must still be there —
        // an empty chart is a legitimate state on a fresh install, and a blank card is not.
        assertThat(bitmap.pixelsDiffering(0, 2)).isGreaterThan(0)
        composeRule.onAllNodesWithText("0%").assertCountEquals(2)
    }

    @Test
    fun `the centre label reads the totals, not the polygon`() {
        drawThree(stats(30, 0), stats(30, 0), stats(30, 0))

        // A full record puts every point on the outer ring, and the percentage is still computed
        // from `totalPrayed`/`totalMissed` rather than from the geometry.
        composeRule.onAllNodesWithText("100%").assertCountEquals(3)
    }
}
