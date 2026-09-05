# Prayer Times Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the Prayer Times screen in the app's shipped design language, and remove the
third prayer-tracking write surface so the screen only answers *when*.

**Architecture:** Two new design-system pieces land first and alone (`NimazSolarArc` in
`:core:ui` atoms, `NimazPrayerRow` in `:core:ui` molecules), each with tests, before any screen
touches them. Then the write path is deleted from `:feature:prayer`'s ViewModel. Then the screen
is rebuilt as `NimazScreenScaffold` + one `LazyColumn`. Docs land last.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), Hilt, Robolectric + Compose UI test,
JUnit4 + Truth, MockK, Gradle (JDK 21, compileSdk 36).

**Spec:** `docs/superpowers/specs/2026-09-05-prayer-times-redesign-design.md`

## Global Constraints

- **Branch:** `feat/prayer-times-redesign` (already created, spec committed at `ede15a38`).
  Do not push to `dev`.
- **No `Color(0xFF…)` literals.** Colours come from `MaterialTheme.colorScheme.*`,
  `NimazColors.*` or `NimazToneColors.foreground(tone)` (CLAUDE.md rule 7).
- **Interactive UI comes from the design system** — no `Text`/`Box`/`Surface` + `Modifier.clickable`
  standing in for a button, and no `.clickable` wrapped *around* a `NimazCard` (CLAUDE.md rule 8).
- **`:core:ui` owns `R.string.*`.** Presentation code imports `com.arshadshah.nimaz.core.ui.R`.
- **Every new string goes into all six locales**: `core/ui/src/main/res/values/strings.xml` plus
  `values-de`, `values-fr`, `values-id`, `values-ms`, `values-tr`. `lintDebug` fails on
  `MissingTranslation` and is a real PR gate via `fastlane`'s `test` lane.
- **No `Route` or `ScreenTags` change.** Nothing in this plan adds, removes or renames a
  destination.
- **Kotlin does not smart-cast a `val` from another module** — bind a local before a null check
  across a module boundary.
- Commit after every task. Conventional-commit subjects.

---

### Task 1: Solar arc geometry (pure Kotlin, no Compose)

The maths that makes the arc exact. Deliberately a plain function in `foundation/geometry/`
(alongside the existing `CompassDegrees`) so it is testable with no Compose, Robolectric or
screenshot machinery.

**Files:**
- Create: `core/ui/src/main/kotlin/com/arshadshah/nimaz/presentation/foundation/geometry/SolarArcGeometry.kt`
- Test: `core/ui/src/test/kotlin/com/arshadshah/nimaz/presentation/foundation/geometry/SolarArcGeometryTest.kt`

**Interfaces:**
- Consumes: nothing.
- Produces: `solarAltitude(t: Float, sunriseFraction: Float, sunsetFraction: Float): Float`,
  `drawnAltitude(t: Float, sunriseFraction: Float, sunsetFraction: Float): Float`,
  `const val NightCompression: Float`. Task 2 uses all three.

- [ ] **Step 1: Write the failing test**

Create `core/ui/src/test/kotlin/com/arshadshah/nimaz/presentation/foundation/geometry/SolarArcGeometryTest.kt`:

```kotlin
package com.arshadshah.nimaz.presentation.foundation.geometry

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The arc's shape is a closed form, not a hand-drawn curve, so it can be pinned exactly:
 * solar noon is midway between sunrise and sunset by definition, which makes a cosine through
 * both crossings the only curve that is 1 at the apex and 0 at both horizons.
 */
class SolarArcGeometryTest {

    // Dublin, early September.
    private val sunrise = 0.27f
    private val sunset = 0.80f
    private val dhuhr = (sunrise + sunset) / 2f

    @Test
    fun `the apex is exactly one at solar noon`() {
        assertThat(solarAltitude(dhuhr, sunrise, sunset)).isWithin(1e-4f).of(1f)
    }

    @Test
    fun `the curve is zero at both horizon crossings`() {
        assertThat(solarAltitude(sunrise, sunrise, sunset)).isWithin(1e-4f).of(0f)
        assertThat(solarAltitude(sunset, sunrise, sunset)).isWithin(1e-4f).of(0f)
    }

    @Test
    fun `midnight is below the horizon`() {
        assertThat(solarAltitude(0f, sunrise, sunset)).isLessThan(0f)
    }

    @Test
    fun `the curve is symmetric about solar noon`() {
        val before = solarAltitude(dhuhr - 0.1f, sunrise, sunset)
        val after = solarAltitude(dhuhr + 0.1f, sunrise, sunset)
        assertThat(before).isWithin(1e-4f).of(after)
    }

    /**
     * The real seasonal property. The apex is normalised to 1 in every season, so what a short
     * day changes is the *night*: Dublin in December troughs near -2.6, in June near -0.2.
     */
    @Test
    fun `a short winter day troughs far deeper than a long summer one`() {
        val summer = solarAltitude(0f, sunriseFraction = 0.20f, sunsetFraction = 0.90f)
        val winter = solarAltitude(0f, sunriseFraction = 0.35f, sunsetFraction = 0.70f)
        assertThat(winter).isLessThan(summer)
        assertThat(summer).isWithin(0.01f).of(-0.23f)
        assertThat(winter).isWithin(0.01f).of(-2.64f)
    }

    @Test
    fun `a full day of daylight never goes below the horizon`() {
        // Polar summer: the crossings meet at midnight.
        val values = (0..20).map { solarAltitude(it / 20f, 0f, 1f) }
        assertThat(values.min()).isAtLeast(-1e-4f)
    }

    @Test
    fun `the drawn curve never leaves minus one to one`() {
        // December's raw trough is -2.64; the drawn one must be clamped.
        val values = (0..100).map { drawnAltitude(it / 100f, 0.35f, 0.70f) }
        assertThat(values.min()).isAtLeast(-1f)
        assertThat(values.max()).isAtMost(1f)
    }

    @Test
    fun `night is compressed rather than drawn at full depth`() {
        val raw = solarAltitude(0f, sunrise, sunset)
        val drawn = drawnAltitude(0f, sunrise, sunset)
        assertThat(drawn).isWithin(1e-4f).of(raw * NightCompression)
    }

    @Test
    fun `daylight is not compressed`() {
        assertThat(drawnAltitude(dhuhr, sunrise, sunset)).isWithin(1e-4f).of(1f)
    }

    @Test
    fun `a sunset at or before sunrise gives a flat curve rather than throwing`() {
        assertThat(solarAltitude(0.5f, 0.6f, 0.6f)).isEqualTo(0f)
        assertThat(solarAltitude(0.5f, 0.8f, 0.2f)).isEqualTo(0f)
    }

    @Test
    fun `out of range fractions give a flat curve`() {
        assertThat(solarAltitude(0.5f, -0.2f, 0.8f)).isEqualTo(0f)
        assertThat(solarAltitude(0.5f, 0.2f, 1.4f)).isEqualTo(0f)
    }

    @Test
    fun `NaN and infinity give a flat curve rather than throwing`() {
        assertThat(solarAltitude(Float.NaN, sunrise, sunset)).isEqualTo(0f)
        assertThat(solarAltitude(0.5f, Float.NaN, sunset)).isEqualTo(0f)
        assertThat(solarAltitude(0.5f, sunrise, Float.POSITIVE_INFINITY)).isEqualTo(0f)
        assertThat(drawnAltitude(Float.NaN, sunrise, sunset)).isEqualTo(0f)
    }
}
```

- [ ] **Step 2: Run it and watch it fail**

```bash
./gradlew :core:ui:testDebugUnitTest --tests '*SolarArcGeometryTest*'
```

Expected: FAIL — `Unresolved reference: solarAltitude` (compilation error, not an assertion
failure).

- [ ] **Step 3: Write the implementation**

Create `core/ui/src/main/kotlin/com/arshadshah/nimaz/presentation/foundation/geometry/SolarArcGeometry.kt`:

```kotlin
package com.arshadshah.nimaz.presentation.foundation.geometry

import kotlin.math.PI
import kotlin.math.cos

/**
 * The shape of the sun's day, from sunrise and sunset alone.
 *
 * Solar altitude is sinusoidal in the hour angle, and solar noon is midway between sunrise and
 * sunset *by definition*. So a cosine centred on that midpoint, scaled to pass through zero at
 * both crossings, is the exact curve — with no latitude, declination or date arithmetic.
 *
 * This is deliberately **suggestive, not simulated**: true Fajr and Isha depend on twilight
 * angle, and the real solar path changes shape with latitude in ways this does not model. It
 * draws a diagram of why the prayer times are when they are. It is not an ephemeris, and nothing
 * that needs an accurate altitude may call it.
 */

private const val TwoPi = (2.0 * PI).toFloat()

/** Below this the curve is degenerate (no daylight) and there is nothing to draw. */
private const val Epsilon = 1e-6f

/**
 * How far a below-horizon altitude is knocked back before it is drawn.
 *
 * A drawing decision, not astronomy. The apex is normalised to 1 in every season, so a short day
 * troughs *much* deeper than a long one — Dublin in December reaches -2.64 against June's -0.23.
 * Drawn at full depth a December night would be more than twice the visual weight of the day and
 * would leave the card, so night is compressed and then clamped to -1.
 */
const val NightCompression = 0.45f

/**
 * Normalised solar altitude at day-fraction [t] (0f = 00:00, 1f = 24:00).
 *
 * Returns 1f at solar noon, 0f at [sunriseFraction] and [sunsetFraction], and negative between
 * sunset and the next sunrise. Unbounded below — see [drawnAltitude] for the drawable form.
 *
 * Every degenerate input returns a flat zero curve rather than throwing: an arc must never be
 * the thing that crashes the screen.
 */
fun solarAltitude(t: Float, sunriseFraction: Float, sunsetFraction: Float): Float {
    if (!t.isFinite() || !sunriseFraction.isFinite() || !sunsetFraction.isFinite()) return 0f
    if (sunriseFraction < 0f || sunriseFraction > 1f) return 0f
    if (sunsetFraction < 0f || sunsetFraction > 1f) return 0f
    if (sunsetFraction <= sunriseFraction) return 0f

    val dhuhr = (sunriseFraction + sunsetFraction) / 2f
    // cos is even, so this is cos(2pi * halfDayLength) either way round.
    val c = cos(TwoPi * (sunriseFraction - dhuhr))
    val denominator = 1f - c
    if (denominator < Epsilon) return 0f

    val amplitude = 1f / denominator
    val offset = -c / denominator
    return amplitude * cos(TwoPi * (t - dhuhr)) + offset
}

/**
 * [solarAltitude] mapped into the `-1f..1f` band the arc is drawn in: daylight untouched, night
 * compressed by [NightCompression] and clamped.
 */
fun drawnAltitude(t: Float, sunriseFraction: Float, sunsetFraction: Float): Float {
    val raw = solarAltitude(t, sunriseFraction, sunsetFraction)
    return if (raw >= 0f) raw.coerceAtMost(1f) else (raw * NightCompression).coerceAtLeast(-1f)
}
```

- [ ] **Step 4: Run the tests and watch them pass**

```bash
./gradlew :core:ui:testDebugUnitTest --tests '*SolarArcGeometryTest*'
```

Expected: PASS, 12 tests.

If `a short winter day troughs far deeper` fails on the exact values, print the actual numbers
before changing the tolerance — the closed form is deterministic, so a mismatch means the formula
was mistyped, not that the expectation is too tight.

- [ ] **Step 5: Commit**

```bash
git add core/ui/src/main/kotlin/com/arshadshah/nimaz/presentation/foundation/geometry/SolarArcGeometry.kt \
        core/ui/src/test/kotlin/com/arshadshah/nimaz/presentation/foundation/geometry/SolarArcGeometryTest.kt
git commit -m "feat(core-ui): the solar arc's geometry, in closed form"
```

---

### Task 2: `NimazSolarArc`

The drawing. One `Canvas`, sampled from Task 1's curve.

**Files:**
- Create: `core/ui/src/main/kotlin/com/arshadshah/nimaz/presentation/components/atoms/NimazSolarArc.kt`
- Test: `core/ui/src/testDebug/kotlin/com/arshadshah/nimaz/presentation/components/atoms/NimazSolarArcTest.kt`

**Interfaces:**
- Consumes: `solarAltitude`, `drawnAltitude`, `NightCompression` (Task 1); `NimazTone`,
  `NimazToneColors.foreground(tone)`.
- Produces:
  - `data class NimazSolarNode(position: Float, label: String?, tone: NimazTone, contentDescription: String)`
  - `@Composable fun NimazSolarArc(nodes: List<NimazSolarNode>, sunriseFraction: Float, sunsetFraction: Float, contentDescription: String, modifier: Modifier = Modifier, sunPosition: Float? = null, litSpan: ClosedFloatingPointRange<Float>? = null, height: Dp = NimazSolarArcDefaults.Height)`
  - `object NimazSolarArcDefaults { val Height: Dp }`
  Task 6 uses all of it.

- [ ] **Step 1: Write the failing test**

Create `core/ui/src/testDebug/kotlin/com/arshadshah/nimaz/presentation/components/atoms/NimazSolarArcTest.kt`:

```kotlin
package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazSolarArcTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val nodes = listOf(
        NimazSolarNode(0.22f, "Fajr", NimazTone.MUTED, "Fajr at 05:12"),
        NimazSolarNode(0.27f, null, NimazTone.ACCENT, "Sunrise at 06:48"),
        NimazSolarNode(0.55f, "Dhuhr", NimazTone.PROMINENT, "Dhuhr at 13:22"),
        NimazSolarNode(0.72f, "Asr", NimazTone.WARNING, "Asr at 17:13"),
        NimazSolarNode(0.80f, null, NimazTone.WARNING, "Maghrib at 20:04"),
        NimazSolarNode(0.90f, "Isha", NimazTone.MUTED, "Isha at 21:38"),
    )

    private fun arc(
        sunPosition: Float? = null,
        litSpan: ClosedFloatingPointRange<Float>? = null,
    ): @Composable () -> Unit = {
        NimazSolarArc(
            nodes = nodes,
            sunriseFraction = 0.27f,
            sunsetFraction = 0.80f,
            contentDescription = "The sun's day: sunrise 06:48, sunset 20:04",
            sunPosition = sunPosition,
            litSpan = litSpan,
        )
    }

    @Test
    fun `the arc speaks as one node`() {
        composeRule.setThemedContent(arc())
        composeRule
            .onNodeWithContentDescription("The sun's day: sunrise 06:48, sunset 20:04")
            .assertIsDisplayed()
    }

    @Test
    fun `labelled nodes render their labels`() {
        composeRule.setThemedContent(arc())
        composeRule.onNodeWithText("Fajr").assertIsDisplayed()
        composeRule.onNodeWithText("Dhuhr").assertIsDisplayed()
        composeRule.onNodeWithText("Asr").assertIsDisplayed()
        composeRule.onNodeWithText("Isha").assertIsDisplayed()
    }

    @Test
    fun `a null sun position still renders - most days are not today`() {
        composeRule.setThemedContent(arc(sunPosition = null))
        composeRule.onNodeWithText("Dhuhr").assertIsDisplayed()
    }

    @Test
    fun `a sun position renders`() {
        composeRule.setThemedContent(arc(sunPosition = 0.62f))
        composeRule.onNodeWithText("Dhuhr").assertIsDisplayed()
    }

    @Test
    fun `a lit span renders`() {
        composeRule.setThemedContent(arc(sunPosition = 0.62f, litSpan = 0.55f..0.72f))
        composeRule.onNodeWithText("Asr").assertIsDisplayed()
    }

    @Test
    fun `an out of range sun position is coerced rather than thrown`() {
        composeRule.setThemedContent(arc(sunPosition = 4f))
        composeRule.onNodeWithText("Dhuhr").assertIsDisplayed()
    }

    @Test
    fun `a NaN sun position is coerced rather than thrown`() {
        composeRule.setThemedContent(arc(sunPosition = Float.NaN))
        composeRule.onNodeWithText("Dhuhr").assertIsDisplayed()
    }

    @Test
    fun `an inverted lit span is tolerated rather than thrown`() {
        composeRule.setThemedContent(arc(litSpan = 0.9f..0.1f))
        composeRule.onNodeWithText("Dhuhr").assertIsDisplayed()
    }

    @Test
    fun `an empty node list renders the bare curve`() {
        composeRule.setThemedContent {
            NimazSolarArc(
                nodes = emptyList(),
                sunriseFraction = 0.27f,
                sunsetFraction = 0.80f,
                contentDescription = "Bare arc",
            )
        }
        composeRule.onNodeWithContentDescription("Bare arc").assertIsDisplayed()
    }

    @Test
    fun `a degenerate day renders rather than throwing`() {
        composeRule.setThemedContent {
            NimazSolarArc(
                nodes = nodes,
                sunriseFraction = 0.8f,
                sunsetFraction = 0.2f,   // sunset before sunrise
                contentDescription = "Degenerate",
            )
        }
        composeRule.onNodeWithContentDescription("Degenerate").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Run it and watch it fail**

```bash
./gradlew :core:ui:testDebugUnitTest --tests '*NimazSolarArcTest*'
```

Expected: FAIL — `Unresolved reference: NimazSolarArc`.

- [ ] **Step 3: Write the implementation**

Create `core/ui/src/main/kotlin/com/arshadshah/nimaz/presentation/components/atoms/NimazSolarArc.kt`:

```kotlin
package com.arshadshah.nimaz.presentation.components.atoms

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.presentation.foundation.geometry.drawnAltitude
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.NimazToneColors
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * One marked point on a [NimazSolarArc].
 *
 * @param position day fraction in `0f..1f` — 0f is 00:00, 1f is 24:00.
 * @param label drawn above the point, or `null` for a bare dot (sunrise and Maghrib, whose times
 *   the card states anyway).
 * @param contentDescription required, not optional: the arc speaks as one sentence built from
 *   these, and an unnamed dot contributes nothing to it.
 */
data class NimazSolarNode(
    val position: Float,
    val label: String? = null,
    val tone: NimazTone = NimazTone.ACCENT,
    val contentDescription: String,
)

object NimazSolarArcDefaults {
    /** Tall enough for the day limb, the night troughs and one row of labels. */
    val Height: Dp = 108.dp
}

/** Above this scale six labels collide in the width available, so they drop out. */
private const val LabelDropOutFontScale = 1.5f

/** How many samples the curve is drawn from. 96 is a point every 15 minutes. */
private const val CurveSamples = 96

private val CurveStroke = 3.dp
private val NodeRadius = 3.6f
private val SunRadius = 5.5f

/**
 * The sun's day as a curve, with the prayers marked where the sun actually puts them.
 *
 * Not a chart *about* the prayer times — a picture of *why they are when they are*. Dhuhr is the
 * apex because solar noon is the apex; sunrise and Maghrib are the horizon crossings; Fajr and
 * Isha sit below the line. See
 * [com.arshadshah.nimaz.presentation.foundation.geometry.solarAltitude] for the geometry, and for
 * why this is a diagram rather than a simulation.
 *
 * @param sunPosition where the sun is now, as a day fraction. `null` draws no sun — the correct
 *   rendering for any day that is not today, and most days a reader looks at are not today.
 * @param litSpan the prayer window the reader is currently inside, brightened along the curve.
 *   This is what makes a separate window band unnecessary.
 */
@Composable
fun NimazSolarArc(
    nodes: List<NimazSolarNode>,
    sunriseFraction: Float,
    sunsetFraction: Float,
    contentDescription: String,
    modifier: Modifier = Modifier,
    sunPosition: Float? = null,
    litSpan: ClosedFloatingPointRange<Float>? = null,
    height: Dp = NimazSolarArcDefaults.Height,
) {
    val dayColor = NimazToneColors.foreground(NimazTone.ACCENT)
    val duskColor = NimazToneColors.foreground(NimazTone.WARNING)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val horizonColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    val fontScale = LocalDensity.current.fontScale
    val showLabels = fontScale <= LabelDropOutFontScale
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = 9.sp,
        fontWeight = FontWeight.SemiBold,
        color = labelColor,
        textAlign = TextAlign.Center,
    )

    val safeSun = sunPosition?.takeIf { it.isFinite() }?.coerceIn(0f, 1f)
    val safeSpan = litSpan
        ?.takeIf { it.start.isFinite() && it.endInclusive.isFinite() }
        ?.let { minOf(it.start, it.endInclusive)..maxOf(it.start, it.endInclusive) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            // One spoken sentence for the whole drawing. Six dots and four labels would
            // otherwise read as ten unlabelled nodes.
            .clearAndSetSemantics { this.contentDescription = contentDescription }
    ) {
        val horizonY = size.height * 0.62f
        val dayHeight = horizonY - (if (showLabels) size.height * 0.14f else 0f)
        val nightHeight = size.height - horizonY

        fun pointAt(t: Float): Offset {
            val h = drawnAltitude(t, sunriseFraction, sunsetFraction)
            val y = if (h >= 0f) horizonY - h * dayHeight else horizonY - h * nightHeight
            return Offset(t * size.width, y)
        }

        // The horizon: the line that makes Fajr and Isha legible as "before dawn" and
        // "after dusk" rather than as two dots that fell off the curve.
        drawLine(
            color = horizonColor,
            start = Offset(0f, horizonY),
            end = Offset(size.width, horizonY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx())),
        )

        val stroke = Stroke(width = CurveStroke.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)

        // Night and day are two paths, so night can be dashed and muted while day is solid and
        // gradient-filled. Sampling both from the same function keeps them continuous.
        val nightPath = Path()
        val dayPath = Path()
        var nightStarted = false
        var dayStarted = false
        for (i in 0..CurveSamples) {
            val t = i / CurveSamples.toFloat()
            val p = pointAt(t)
            val isDay = drawnAltitude(t, sunriseFraction, sunsetFraction) >= 0f
            if (isDay) {
                if (dayStarted) dayPath.lineTo(p.x, p.y) else { dayPath.moveTo(p.x, p.y); dayStarted = true }
                nightStarted = false
            } else {
                if (nightStarted) nightPath.lineTo(p.x, p.y) else { nightPath.moveTo(p.x, p.y); nightStarted = true }
                dayStarted = false
            }
        }

        drawPath(
            path = nightPath,
            color = trackColor,
            style = Stroke(
                width = CurveStroke.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
            ),
        )
        drawPath(
            path = dayPath,
            brush = Brush.horizontalGradient(listOf(dayColor, duskColor)),
            style = stroke,
        )

        // The current window, brightened over the top of the day limb.
        if (safeSpan != null) {
            val spanPath = Path()
            var started = false
            var i = 0
            while (i <= CurveSamples) {
                val t = i / CurveSamples.toFloat()
                if (t in safeSpan) {
                    val p = pointAt(t)
                    if (started) spanPath.lineTo(p.x, p.y) else { spanPath.moveTo(p.x, p.y); started = true }
                }
                i++
            }
            drawPath(path = spanPath, color = duskColor, style = stroke)
        }

        nodes.forEach { node ->
            val t = node.position.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: return@forEach
            val p = pointAt(t)
            drawCircle(color = toneColorFor(node.tone, dayColor, duskColor, trackColor), radius = NodeRadius.dp.toPx(), center = p)
            if (showLabels && node.label != null) {
                val measured = measurer.measure(node.label, labelStyle)
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(
                        x = (p.x - measured.size.width / 2f).coerceIn(0f, size.width - measured.size.width),
                        y = p.y - measured.size.height - 6.dp.toPx(),
                    ),
                )
            }
        }

        if (safeSun != null) {
            val p = pointAt(safeSun)
            // A soft halo, then the disc: the halo is what stops the sun reading as a seventh
            // prayer marker.
            drawCircle(color = duskColor.copy(alpha = 0.25f), radius = SunRadius.dp.toPx() * 2.2f, center = p)
            drawCircle(color = duskColor, radius = SunRadius.dp.toPx(), center = p)
        }
    }
}

/**
 * Tones resolved outside the draw scope would need a composable context inside it, so the three
 * colours the arc actually uses are passed in and mapped here.
 */
private fun toneColorFor(tone: NimazTone, day: Color, dusk: Color, muted: Color): Color = when (tone) {
    NimazTone.ACCENT, NimazTone.PROMINENT -> day
    NimazTone.WARNING, NimazTone.ERROR -> dusk
    else -> muted
}

// ==================== PREVIEWS ====================

@Composable
private fun ShowcaseLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun NimazSolarArcShowcase() {
    val september = listOf(
        NimazSolarNode(0.22f, "Fajr", NimazTone.MUTED, "Fajr at 05:12"),
        NimazSolarNode(0.27f, null, NimazTone.ACCENT, "Sunrise at 06:48"),
        NimazSolarNode(0.55f, "Dhuhr", NimazTone.PROMINENT, "Dhuhr at 13:22"),
        NimazSolarNode(0.72f, "Asr", NimazTone.WARNING, "Asr at 17:13"),
        NimazSolarNode(0.80f, null, NimazTone.WARNING, "Maghrib at 20:04"),
        NimazSolarNode(0.90f, "Isha", NimazTone.MUTED, "Isha at 21:38"),
    )
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ShowcaseLabel("Today — inside Dhuhr's window")
        NimazSolarArc(september, 0.27f, 0.80f, "The sun's day", sunPosition = 0.62f, litSpan = 0.55f..0.72f)

        ShowcaseLabel("Another day — no sun")
        NimazSolarArc(september, 0.27f, 0.80f, "The sun's day")

        ShowcaseLabel("December — a short day and a deep night")
        NimazSolarArc(september, 0.35f, 0.70f, "The sun's day", sunPosition = 0.5f)
    }
}

@Preview(showBackground = true, widthDp = 360, name = "NimazSolarArc — Light")
@Composable
private fun NimazSolarArcLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { NimazSolarArcShowcase() }
}

@Preview(
    showBackground = true, widthDp = 360, name = "NimazSolarArc — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
@Composable
private fun NimazSolarArcDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { NimazSolarArcShowcase() }
}
```

- [ ] **Step 4: Run the tests and watch them pass**

```bash
./gradlew :core:ui:testDebugUnitTest --tests '*NimazSolarArcTest*'
```

Expected: PASS, 10 tests.

Note on the label tests: `drawText` inside a `Canvas` does **not** create a semantics node, so
`onNodeWithText("Fajr")` will fail. If it does, that is the expected outcome of this design —
the labels are painted, and the arc deliberately speaks as one node. **Fix the test, not the
component:** replace the four `onNodeWithText` assertions in
`labelled nodes render their labels` with a single assertion that the arc renders at all, and
rename it `the arc renders with a full set of nodes`. Do not add per-label semantics: that would
reintroduce the ten-unlabelled-nodes problem the `clearAndSetSemantics` exists to prevent.

- [ ] **Step 5: Verify the previews compile and the module's own gates pass**

```bash
./gradlew :core:ui:check
```

Expected: PASS — this also runs `moduleBoundary` and `:core:ui`'s lint.

- [ ] **Step 6: Commit**

```bash
git add core/ui/src/main/kotlin/com/arshadshah/nimaz/presentation/components/atoms/NimazSolarArc.kt \
        core/ui/src/testDebug/kotlin/com/arshadshah/nimaz/presentation/components/atoms/NimazSolarArcTest.kt
git commit -m "feat(core-ui): NimazSolarArc — the sun's day, with the prayers on it"
```

---

### Task 3: `NimazPrayerRow`

The row form of a prayer. `PrayerTimeCard` stays exactly as it is for `HomeScreen`.

**Files:**
- Create: `core/ui/src/main/kotlin/com/arshadshah/nimaz/presentation/components/molecules/NimazPrayerRow.kt`
- Test: `core/ui/src/testDebug/kotlin/com/arshadshah/nimaz/presentation/components/molecules/NimazPrayerRowTest.kt`

**Interfaces:**
- Consumes: `NimazIcon`, `NimazIconSize`, `getPrayerIcon(PrayerType?)`, `getPrayerColor(PrayerType?)`,
  `getArabicPrayerName(PrayerType?)`, `ArabicText`.
- Produces: `@Composable fun NimazPrayerRow(type: PrayerType, name: String, time: String, modifier: Modifier = Modifier, qualifier: String? = null, isPassed: Boolean = false, isNext: Boolean = false, showArabic: Boolean = true)`. Task 6 uses it.

- [ ] **Step 1: Write the failing test**

Create `core/ui/src/testDebug/kotlin/com/arshadshah/nimaz/presentation/components/molecules/NimazPrayerRowTest.kt`:

```kotlin
package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazPrayerRowTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `the row names the prayer and states its time`() {
        composeRule.setThemedContent {
            NimazPrayerRow(type = PrayerType.DHUHR, name = "Dhuhr", time = "13:22")
        }
        composeRule.onNodeWithText("Dhuhr").assertIsDisplayed()
        composeRule.onNodeWithText("13:22").assertIsDisplayed()
    }

    /**
     * The whole point of this molecule. Prayer Times is a reference screen: a row that looks
     * tappable would promise logging the screen no longer does.
     */
    @Test
    fun `the row is never clickable`() {
        composeRule.setThemedContent {
            NimazPrayerRow(type = PrayerType.ASR, name = "Asr", time = "17:13")
        }
        composeRule.onNodeWithText("Asr").assertHasNoClickAction()
    }

    @Test
    fun `a qualifier renders beside the name`() {
        composeRule.setThemedContent {
            NimazPrayerRow(
                type = PrayerType.DHUHR, name = "Dhuhr", time = "13:20", qualifier = "Jumu'ah",
            )
        }
        composeRule.onNodeWithText("Jumu'ah").assertIsDisplayed()
    }

    @Test
    fun `a passed prayer still states its name and time`() {
        composeRule.setThemedContent {
            NimazPrayerRow(type = PrayerType.FAJR, name = "Fajr", time = "05:12", isPassed = true)
        }
        composeRule.onNodeWithText("Fajr").assertIsDisplayed()
        composeRule.onNodeWithText("05:12").assertIsDisplayed()
    }

    @Test
    fun `the next prayer still states its name and time`() {
        composeRule.setThemedContent {
            NimazPrayerRow(type = PrayerType.ASR, name = "Asr", time = "17:13", isNext = true)
        }
        composeRule.onNodeWithText("Asr").assertIsDisplayed()
        composeRule.onNodeWithText("17:13").assertIsDisplayed()
    }

    @Test
    fun `arabic can be suppressed`() {
        composeRule.setThemedContent {
            NimazPrayerRow(
                type = PrayerType.SUNRISE, name = "Sunrise", time = "06:48", showArabic = false,
            )
        }
        composeRule.onNodeWithText("Sunrise").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Run it and watch it fail**

```bash
./gradlew :core:ui:testDebugUnitTest --tests '*NimazPrayerRowTest*'
```

Expected: FAIL — `Unresolved reference: NimazPrayerRow`.

- [ ] **Step 3: Write the implementation**

Create `core/ui/src/main/kotlin/com/arshadshah/nimaz/presentation/components/molecules/NimazPrayerRow.kt`:

```kotlin
package com.arshadshah.nimaz.presentation.components.molecules

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.foundation.tokens.getArabicPrayerName
import com.arshadshah.nimaz.presentation.foundation.tokens.getPrayerColor
import com.arshadshah.nimaz.presentation.foundation.tokens.getPrayerIcon
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/** How far a prayer whose time has passed is knocked back. */
private const val PassedAlpha = 0.5f

/**
 * One prayer as a row inside a shared card — the reference form.
 *
 * Deliberately **not** [PrayerTimeCard], which is a card *per* prayer carrying a tracking
 * checkbox and is what `HomeScreen`'s two-column layout is built around. This row has no
 * `onClick` and no toggle **by design**: Prayer Times answers *when*, and the prayer tracker
 * answers what the reader did about it. A row that looked tappable would promise logging this
 * screen no longer performs.
 *
 * @param qualifier a short note beside the name — "Jumu'ah" on a Friday Dhuhr.
 * @param isNext tints the icon and weights the time; it does not make the row interactive.
 */
@Composable
fun NimazPrayerRow(
    type: PrayerType,
    name: String,
    time: String,
    modifier: Modifier = Modifier,
    qualifier: String? = null,
    isPassed: Boolean = false,
    isNext: Boolean = false,
    showArabic: Boolean = true,
) {
    val prayerColor = getPrayerColor(type)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (isPassed && !isNext) PassedAlpha else 1f)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isNext) prayerColor.copy(alpha = 0.20f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center,
        ) {
            NimazIcon(
                imageVector = getPrayerIcon(type),
                contentDescription = null,
                size = NimazIconSize.SMALL,
                tint = if (isNext) prayerColor else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (qualifier != null) {
                    Text(
                        text = qualifier,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (showArabic) {
                ArabicText(
                    text = getArabicPrayerName(type),
                    size = ArabicTextSize.SMALL,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text(
            text = time,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isNext) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isNext) prayerColor else MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ==================== PREVIEWS ====================

@Composable
private fun NimazPrayerRowShowcase() {
    Column(modifier = Modifier.padding(16.dp)) {
        NimazPrayerRow(PrayerType.FAJR, "Fajr", "05:12", isPassed = true)
        NimazPrayerRow(PrayerType.SUNRISE, "Sunrise", "06:48", isPassed = true, showArabic = false)
        NimazPrayerRow(PrayerType.DHUHR, "Dhuhr", "13:20", qualifier = "Jumu'ah", isPassed = true)
        NimazPrayerRow(PrayerType.ASR, "Asr", "17:13", isNext = true)
        NimazPrayerRow(PrayerType.MAGHRIB, "Maghrib", "20:04")
        NimazPrayerRow(PrayerType.ISHA, "Isha", "21:38")
    }
}

@Preview(showBackground = true, widthDp = 360, name = "NimazPrayerRow — Light")
@Composable
private fun NimazPrayerRowLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { NimazPrayerRowShowcase() }
}

@Preview(
    showBackground = true, widthDp = 360, name = "NimazPrayerRow — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
@Composable
private fun NimazPrayerRowDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { NimazPrayerRowShowcase() }
}
```

If `NimazIcon` has no `tint` parameter, or `ArabicText` has no `color` / `size` parameter with
these names, **read the actual signatures** in
`core/ui/.../components/atoms/NimazIcon.kt` and `ArabicText.kt` and adapt — do not add
parameters to those atoms for this row.

- [ ] **Step 4: Run the tests and watch them pass**

```bash
./gradlew :core:ui:testDebugUnitTest --tests '*NimazPrayerRowTest*'
```

Expected: PASS, 6 tests.

- [ ] **Step 5: Commit**

```bash
git add core/ui/src/main/kotlin/com/arshadshah/nimaz/presentation/components/molecules/NimazPrayerRow.kt \
        core/ui/src/testDebug/kotlin/com/arshadshah/nimaz/presentation/components/molecules/NimazPrayerRowTest.kt
git commit -m "feat(core-ui): NimazPrayerRow — the reference form of a prayer"
```

---

### Task 4: A trailing action slot on `PrayerSkyScene`

So the "Today" pill can sit in the glass bar instead of being positioned by hand at
`statusBarTop + 60.dp`.

**Files:**
- Modify: `core/ui/src/main/kotlin/com/arshadshah/nimaz/presentation/components/organisms/PrayerSkyScene.kt` (signature at `:228`)
- Test: `core/ui/src/testDebug/kotlin/com/arshadshah/nimaz/presentation/components/organisms/PrayerSkySceneActionTest.kt` (create)

**Interfaces:**
- Consumes: nothing new.
- Produces: `PrayerSkyScene(..., trailingAction: (@Composable () -> Unit)? = null)` — a new
  **last, defaulted** parameter, so every existing call site is unaffected. Task 6 passes it.

- [ ] **Step 1: Write the failing test**

Create `core/ui/src/testDebug/kotlin/com/arshadshah/nimaz/presentation/components/organisms/PrayerSkySceneActionTest.kt`:

```kotlin
package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PrayerSkySceneActionTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `a trailing action renders in the glass bar`() {
        composeRule.setThemedContent {
            PrayerSkyScene(
                timeOfDay = 0.5f,
                timeLabel = "14:32",
                statusLabel = "Asr in 2h 41m",
                locationName = "Dublin",
                onBack = {},
                onSettings = {},
                cloudsEnabled = false,
                trailingAction = { Text("Today") },
            )
        }
        composeRule.onNodeWithText("Today").assertIsDisplayed()
    }

    @Test
    fun `no trailing action is the default and renders nothing extra`() {
        composeRule.setThemedContent {
            PrayerSkyScene(
                timeOfDay = 0.5f,
                timeLabel = "14:32",
                statusLabel = "Asr in 2h 41m",
                cloudsEnabled = false,
            )
        }
        composeRule.onNodeWithText("14:32").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Run it and watch it fail**

```bash
./gradlew :core:ui:testDebugUnitTest --tests '*PrayerSkySceneActionTest*'
```

Expected: FAIL — `Cannot find a parameter with this name: trailingAction`.

- [ ] **Step 3: Add the parameter**

In `PrayerSkyScene.kt`, add as the **last** parameter of the signature at `:228`:

```kotlin
    onSettings: (() -> Unit)? = null,
    /**
     * An optional third pill in the glass bar, after back and settings.
     *
     * Prayer Times' "Today" shortcut used to be a `NimazBadge` positioned by hand at
     * `statusBarTop + 60.dp` with a comment about dodging these very actions. A slot costs
     * nothing and removes the magic offset.
     */
    trailingAction: (@Composable () -> Unit)? = null,
```

Then render it inside the glass top-bar `Row` — find the row that already holds the back and
settings pills (guarded by `showTopBar`) and add, after the settings pill:

```kotlin
                if (trailingAction != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    trailingAction()
                }
```

Import `androidx.compose.foundation.layout.Spacer` and `androidx.compose.foundation.layout.width`
if they are not already imported.

Note `showTopBar` is currently `locationName != null && onBack != null && onSettings != null`.
Leave that condition alone — the trailing action is an addition to a bar that exists, not a
reason to create one.

- [ ] **Step 4: Run the tests and watch them pass**

```bash
./gradlew :core:ui:testDebugUnitTest --tests '*PrayerSkyScene*'
```

Expected: PASS. Existing `PrayerSkyScene` tests must also still pass — the parameter is defaulted,
so no call site changed.

- [ ] **Step 5: Confirm no call site broke**

```bash
./gradlew :core:ui:compileDebugKotlin :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add core/ui/src/main/kotlin/com/arshadshah/nimaz/presentation/components/organisms/PrayerSkyScene.kt \
        core/ui/src/testDebug/kotlin/com/arshadshah/nimaz/presentation/components/organisms/PrayerSkySceneActionTest.kt
git commit -m "feat(core-ui): PrayerSkyScene takes a trailing glass-bar action"
```

---

### Task 5: Prayer Times stops writing

The reframe. Spec §1.

**Files:**
- Modify: `feature/prayer/src/main/kotlin/com/arshadshah/nimaz/presentation/viewmodel/prayer/PrayerTimesEvent.kt`
- Modify: `feature/prayer/src/main/kotlin/com/arshadshah/nimaz/presentation/viewmodel/prayer/PrayerTimesViewModel.kt` (`:50`, `:118`, `:236-238`, `:262`, `:276-296`)
- Modify: `feature/prayer/src/main/kotlin/com/arshadshah/nimaz/presentation/screens/prayer/PrayerTimesScreen.kt` (remove the `onToggle` wiring only — the screen is rebuilt in Task 6)
- Delete: `feature/prayer/src/test/kotlin/com/arshadshah/nimaz/presentation/viewmodel/prayer/PrayerTimesTrackingTest.kt`
- Modify: `feature/prayer/src/test/kotlin/com/arshadshah/nimaz/presentation/viewmodel/prayer/PrayerTimesViewModelTest.kt`

**Interfaces:**
- Consumes: nothing new.
- Produces: `PrayerTimesEvent` with exactly four members — `PreviousDay`, `NextDay`, `GoToToday`,
  `SelectDate(date)`. Task 6 dispatches only these.

- [ ] **Step 1: Write the failing regression guard**

This is the test that keeps the reframe true when someone later adds an event. Add to
`PrayerTimesViewModelTest.kt` — match the file's existing fixture setup rather than inventing one;
read its `@Before` and reuse the same `PrayerUseCases` mock and `RecordingTelemetry`:

```kotlin
    /**
     * Prayer Times answers *when*. The tracker answers what the reader did about it.
     *
     * This screen used to be the third place in the app a prayer could be written, with a binary
     * PRAYED/NOT_PRAYED vocabulary the tracker redesign retired — so tapping a row here silently
     * downgraded a LATE prayer, and a double-tap turned an assertion into an absence.
     *
     * Asserted over *every* event rather than over the removed one, so adding a writing event
     * later fails here rather than shipping.
     */
    @Test
    fun `no event writes a prayer record`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(PrayerTimesEvent.NextDay)
        viewModel.onEvent(PrayerTimesEvent.PreviousDay)
        viewModel.onEvent(PrayerTimesEvent.GoToToday)
        viewModel.onEvent(PrayerTimesEvent.SelectDate(LocalDate.of(2026, 9, 12)))
        advanceUntilIdle()

        coVerify(exactly = 0) {
            prayerUseCases.updatePrayerStatus(any(), any(), any(), any(), any())
        }
        assertThat(telemetry.events.none { it.name == "prayer_tracked" }).isTrue()
    }
```

Adapt `createViewModel()`, `prayerUseCases` and `telemetry` to the names the file already uses.
If `RecordingTelemetry` exposes recorded events under a different property than `events`, read
`core/monitoring`'s `RecordingTelemetry` and use the real one.

- [ ] **Step 2: Run it and watch it fail**

```bash
./gradlew :feature:prayer:testDebugUnitTest --tests '*PrayerTimesViewModelTest*'
```

Expected: PASS already (none of those four events write today) — **this one is a guard, not a
red test.** That is correct and expected for a regression guard. Confirm it is genuinely load-
bearing by temporarily adding `viewModel.onEvent(PrayerTimesEvent.TogglePrayer(PrayerType.ASR))`
to the test body, re-running, seeing it FAIL, then removing that line again.

- [ ] **Step 3: Delete the write path**

In `PrayerTimesEvent.kt`, remove the `TogglePrayer` member and the now-unused `PrayerType` import:

```kotlin
package com.arshadshah.nimaz.presentation.viewmodel.prayer

import java.time.LocalDate

sealed interface PrayerTimesEvent {
    data object PreviousDay : PrayerTimesEvent
    data object NextDay : PrayerTimesEvent
    data object GoToToday : PrayerTimesEvent
    data class SelectDate(val date: LocalDate) : PrayerTimesEvent
}
```

In `PrayerTimesViewModel.kt`, delete:
- `:118` — the `is PrayerTimesEvent.TogglePrayer -> togglePrayer(event.type)` branch
- `:276-296` — the whole `togglePrayer` function
- `:50` — the `statuses` field
- `:236-238` — the `statusJob` / `observe_statuses` launch and the `statuses = records.associate…`
  assignment, plus the `statusJob` declaration
- `:262` — the `prayerStatus = statuses[PrayerName.valueOf(pt.type.name)]` assignment (leave the
  surrounding `PrayerTimeDisplay` construction; the field keeps its default)

Then remove every import left unused — likely `PrayerName`, `PrayerStatus`, and `java.time.Instant`
if nothing else uses it. The Kotlin compiler warns rather than errors on unused imports, so check
them by eye.

**Do not remove `PrayerTimeDisplay.prayerStatus` itself.** `HomeScreen` populates it.

- [ ] **Step 4: Delete the obsolete test file and the screen's toggle wiring**

```bash
git rm feature/prayer/src/test/kotlin/com/arshadshah/nimaz/presentation/viewmodel/prayer/PrayerTimesTrackingTest.kt
```

In `PrayerTimesScreen.kt`, remove the three toggle-related tests' subject: `DayList`'s `onToggle`
parameter and `showToggle` argument, and `PrayerTimeCard`'s `onClick` / `onToggle` / `showToggle`
arguments. Pass `onClick = {}`, `onToggle = {}`, `showToggle = false` for now — Task 6 replaces
this composable wholesale, and this step exists only to keep the branch compiling.

Delete these three tests from `PrayerTimesScreenTest.kt`:
`tapping a prayer row toggles that prayer`, `a future day offers no tracking toggles`,
`today's rows do offer them`.

- [ ] **Step 5: Run the module's tests**

```bash
./gradlew :feature:prayer:testDebugUnitTest
```

Expected: PASS. If `PrayerTimesViewModelTest` has a test asserting that toggling *sunrise* does
nothing, delete it too — its subject is gone.

- [ ] **Step 6: Confirm the whole app still compiles**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL. This runs KSP, so it also validates Hilt wiring.

- [ ] **Step 7: Commit**

```bash
git add -A feature/prayer
git commit -m "refactor(prayer): Prayer Times answers when, not what you did

Removes the third prayer-tracking write surface. It wrote a binary
PRAYED/NOT_PRAYED, so tapping a row silently downgraded a prayer logged as
LATE on the tracker, and a double-tap turned an assertion into an absence.

With the toggle gone the statuses map and its Room subscription are dead too,
which also stops this screen recomposing on every tracker write."
```

---

### Task 6: The screen, rebuilt

**Files:**
- Rewrite: `feature/prayer/src/main/kotlin/com/arshadshah/nimaz/presentation/screens/prayer/PrayerTimesScreen.kt`
- Modify: `core/ui/src/main/res/values/strings.xml` and all five translations
- Rewrite: `feature/prayer/src/test/kotlin/com/arshadshah/nimaz/presentation/screens/prayer/PrayerTimesScreenTest.kt`

**Interfaces:**
- Consumes: `NimazSolarArc` / `NimazSolarNode` / `NimazSolarArcDefaults` (Task 2),
  `NimazPrayerRow` (Task 3), `PrayerSkyScene(trailingAction = …)` (Task 4), the four-member
  `PrayerTimesEvent` (Task 5), plus existing `NimazScreenScaffold`, `NimazDayRail`,
  `NimazDayRailItem`, `NimazSectionHeader`, `NimazCard`, `NimazIconButton`, `NimazBottomSheet`,
  `NimazCalendar`, `NimazDivider`, `NimazBadge`.
- Produces: `PrayerTimesScreen(onNavigateBack: () -> Unit, onNavigateToSettings: () -> Unit, viewModel: PrayerTimesViewModel = hiltViewModel())` — **signature unchanged**, so `PrayerGraph.kt`
  needs no edit.

- [ ] **Step 1: Add the strings, in all six locales**

Add to `core/ui/src/main/res/values/strings.xml`:

```xml
<string name="prayer_arc_cd">The sun\'s day: sunrise %1$s, sunset %2$s</string>
<string name="prayer_window_lede">You are in the window of</string>
<string name="prayer_window_until">until %1$s</string>
<string name="prayer_window_started">started %1$s</string>
<string name="prayer_daylight_lede">The day\'s arc</string>
<string name="prayer_daylight_amount">%1$s of daylight</string>
<string name="prayer_daylight_shorter">%1$s shorter</string>
<string name="prayer_daylight_longer">%1$s longer</string>
<string name="prayer_jumuah">Jumu\'ah</string>
<string name="cd_pick_month">Pick a month</string>
```

Then add a translated entry for each of the ten in `values-de`, `values-fr`, `values-id`,
`values-ms` and `values-tr`. **Do not skip a locale** — `lintDebug` fails on `MissingTranslation`
and it is a PR gate.

- [ ] **Step 2: Write the screen**

Rewrite `PrayerTimesScreen.kt`. The composition, top to bottom inside one `LazyColumn`:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTimesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: PrayerTimesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val today = state.selectedDate.takeIf { state.isToday } ?: LocalDate.now()
    val selectedDate = state.selectedDate ?: today
    val sky = rememberPrayerSky(state, today, selectedDate)
    var showMonthSheet by remember { mutableStateOf(false) }

    NimazScreenScaffold { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // 1. The sky. Full bleed, so it takes no horizontal padding and is the one item
            //    that scrolls off the top.
            item {
                PrayerSkyScene(
                    timeOfDay = sky.timeOfDay,
                    timeLabel = sky.timeLabel,
                    statusLabel = sky.statusLabel,
                    moonFraction = state.moonFraction,
                    sunriseFraction = state.sunriseFraction,
                    sunsetFraction = state.sunsetFraction,
                    shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                    locationName = if (state.isUsingFallbackLocation) {
                        stringResource(R.string.location_using_default)
                    } else state.locationName,
                    onBack = onNavigateBack,
                    onSettings = onNavigateToSettings,
                    trailingAction = if (!state.isToday) {
                        {
                            NimazBadge(
                                text = stringResource(R.string.today),
                                size = NimazBadgeSize.LARGE,
                                colors = NimazBadgeDefaults.colors(
                                    tone = NimazTone.ACCENT,
                                    emphasis = NimazBadgeEmphasis.FILLED,
                                ),
                                onClick = { viewModel.onEvent(PrayerTimesEvent.GoToToday) },
                            )
                        }
                    } else null,
                    modifier = Modifier.fillMaxWidth().height(260.dp + statusBarTop()),
                )
            }

            // 2. The week rail plus a jump to any month.
            item {
                DayRailRow(
                    selectedDate = selectedDate,
                    today = today,
                    onSelect = { viewModel.onEvent(PrayerTimesEvent.SelectDate(it)) },
                    onJump = { showMonthSheet = true },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }

            // 3. The solar day card.
            item {
                SolarDayCard(
                    state = state,
                    prayers = sky.prayers,
                    isToday = state.isToday,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }

            // 4 + 5. The prayers, as rows in one card.
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    NimazSectionHeader(
                        title = selectedDate.formatWeekdayDayMonth(),
                        trailingText = state.daylight,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    NimazCard(style = NimazCardStyle.FILLED, shape = RoundedCornerShape(20.dp)) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                            sky.prayers.forEachIndexed { index, prayer ->
                                if (index > 0) NimazDivider()
                                NimazPrayerRow(
                                    type = prayer.type,
                                    name = prayer.type.displayName,
                                    time = prayer.timeAt?.let { clockTimeText(it) } ?: "--:--",
                                    qualifier = jumuahQualifier(prayer.type, selectedDate),
                                    isPassed = prayer.isPassed,
                                    isNext = prayer.isNext && state.isToday,
                                    showArabic = prayer.type != PrayerType.SUNRISE,
                                )
                            }
                        }
                    }
                }
            }

            // 6. About this day.
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    NimazSectionHeader(
                        title = stringResource(R.string.prayer_about_this_day),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DayInfoCard(
                        sunrise = state.sunriseAt?.let { clockTimeText(it) } ?: "--:--",
                        sunset = state.sunsetAt?.let { clockTimeText(it) } ?: "--:--",
                        daylight = state.daylight,
                        method = state.methodLabel,
                    )
                }
            }
        }
    }

    if (showMonthSheet) { /* unchanged from the current file — see below */ }
}
```

Notes for the implementer:

- **`prayer_about_this_day`** is an eleventh string; add it in Step 1's list in all six locales
  if you did not already.
- **Keep the existing month sheet block verbatim** from the current file (the
  `NimazBottomSheet` + `NimazCalendar` with `displayedMonth`). It works and is not part of this
  redesign.
- **Keep `rememberPrayerSky`** as it is, except: its `statusLabel` for a non-today date should
  become just the relative label (`relativeLabel(selectedDate)`), since the card now owns the
  sunrise/sunset pair. And **delete `daysFromToday()`** — `relativeLabel()` already does the job
  with `pluralStringResource`. Note `relativeLabel` currently has no `diff == 0L` branch; add one
  returning `stringResource(R.string.today)`.
- **`statusBarTop()`** is a tiny private helper wrapping the existing
  `WindowInsets.statusBars.asPaddingValues().calculateTopPadding()` expression.
- **Keep the horizontal swipe paging** by putting the `pointerInput` /
  `detectHorizontalDragGestures` modifier on the `LazyColumn` itself, dispatching
  `PreviousDay` / `NextDay` at the existing 64.dp threshold.
- **`DayList` and `DayNavBar` are deleted.** So is the `layout {}` overlap trick and its comment.

`SolarDayCard`, the new private composable, in the same file:

```kotlin
@Composable
private fun SolarDayCard(
    state: PrayerTimesUiState,
    prayers: List<PrayerTimeDisplay>,
    isToday: Boolean,
    modifier: Modifier = Modifier,
) {
    val now by rememberNow(TickResolution.MINUTES)
    val current = prayers.lastOrNull { it.isPassed }
    val next = prayers.firstOrNull { it.isNext }

    val nodes = prayers.mapNotNull { prayer ->
        val at = prayer.timeAt ?: return@mapNotNull null
        NimazSolarNode(
            position = at.dayFraction(),
            label = if (prayer.type == PrayerType.SUNRISE || prayer.type == PrayerType.MAGHRIB) {
                null
            } else prayer.type.displayName,
            tone = if (prayer.isNext) NimazTone.WARNING else NimazTone.MUTED,
            contentDescription = "${prayer.type.displayName} ${clockTimeText(at)}",
        )
    }

    NimazCard(
        modifier = modifier,
        style = NimazCardStyle.FILLED,
        shape = RoundedCornerShape(20.dp),
        tone = NimazTone.ACCENT,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (isToday && current != null) {
                Text(
                    text = stringResource(R.string.prayer_window_lede),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = current.type.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            } else {
                Text(
                    text = stringResource(R.string.prayer_daylight_lede),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.prayer_daylight_amount, state.daylight),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            NimazSolarArc(
                nodes = nodes,
                sunriseFraction = state.sunriseFraction,
                sunsetFraction = state.sunsetFraction,
                contentDescription = stringResource(
                    R.string.prayer_arc_cd,
                    state.sunriseAt?.let { clockTimeText(it) } ?: "--:--",
                    state.sunsetAt?.let { clockTimeText(it) } ?: "--:--",
                ),
                sunPosition = if (isToday) now.dayFraction() else null,
                litSpan = if (isToday && current?.timeAt != null && next?.timeAt != null) {
                    current.timeAt.dayFraction()..next.timeAt.dayFraction()
                } else null,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
```

`dayFraction()` is a small private extension converting an instant to a fraction of its local day:

```kotlin
/** An instant as a fraction of its own local day — 0f at midnight, 1f at the next. */
private fun kotlin.time.Instant.dayFraction(): Float {
    val local = java.time.Instant.ofEpochMilli(toEpochMilliseconds())
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDateTime()
    return (local.hour * 60 + local.minute) / 1440f
}
```

**Kotlin will not smart-cast `current.timeAt` across the module boundary** (`PrayerTimeDisplay`
lives in `:core:ui`). Bind locals before the range:

```kotlin
                litSpan = if (isToday) {
                    val from = current?.timeAt
                    val to = next?.timeAt
                    if (from != null && to != null) from.dayFraction()..to.dayFraction() else null
                } else null,
```

Use that form, not the one above it.

`DayRailRow`:

```kotlin
@Composable
private fun DayRailRow(
    selectedDate: LocalDate,
    today: LocalDate,
    onSelect: (LocalDate) -> Unit,
    onJump: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Seven days centred on the selection, so paging keeps the chosen day in the middle.
    val days = remember(selectedDate) { (-3..3).map { selectedDate.plusDays(it.toLong()) } }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        NimazDayRail(
            days = days.map { date ->
                NimazDayRailItem(
                    weekdayLabel = date.dayOfWeek
                        .getDisplayName(java.time.format.TextStyle.NARROW, Locale.getDefault()),
                    dayLabel = date.dayOfMonth.toString(),
                    isToday = date == today,
                    contentDescription = date.formatWeekdayDayMonth(),
                )
            },
            selectedIndex = days.indexOf(selectedDate).takeIf { it >= 0 },
            onSelect = { onSelect(days[it]) },
            modifier = Modifier.weight(1f),
        )
        NimazIconButton(
            onClick = onJump,
            icon = Icons.Default.CalendarMonth,
            contentDescription = stringResource(R.string.cd_pick_month),
        )
    }
}
```

`DayInfoCard` and `InfoRow` are **kept verbatim** from the current file.

- [ ] **Step 3: Compile**

```bash
./gradlew :feature:prayer:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL. Fix signature mismatches against the real atoms — `NimazIconButton`,
`NimazCard`, `NimazBadgeDefaults` — by reading them, not by guessing.

- [ ] **Step 4: Rewrite the screen tests**

In `PrayerTimesScreenTest.kt`, keep the fixture helpers (`str`, `at`, `prayersFor`, `render`,
`show`) and rewrite the assertions. Replace the day-arrow tests with rail tests and add the two
new ones:

```kotlin
    @Test
    fun `the rail selects another day`() {
        show(state = baseState())
        // The rail centres on the selection, so index 4 is tomorrow.
        composeRule.onNodeWithText(tomorrow.dayOfMonth.toString()).performClick()
        composeRule.onNodeWithText(tomorrow.formatWeekdayDayMonth()).assertIsDisplayed()
    }

    @Test
    fun `the month button opens the picker and picking a day selects it`() {
        show(state = baseState())
        composeRule.onNodeWithContentDescription(str(R.string.cd_pick_month)).performClick()
        composeRule.onNodeWithText("15").performClick()
        composeRule.onNodeWithText("15").assertIsDisplayed()
    }

    @Test
    fun `on today the card states the window you are inside`() {
        show(state = baseState().copy(isToday = true))
        composeRule.onNodeWithText(str(R.string.prayer_window_lede)).assertIsDisplayed()
    }

    @Test
    fun `on another day the card states the daylight instead`() {
        show(state = baseState().copy(isToday = false, daylight = "12h 43m"))
        composeRule.onNodeWithText(str(R.string.prayer_daylight_lede)).assertIsDisplayed()
    }

    /** The reframe, asserted at the screen as well as the ViewModel. */
    @Test
    fun `no prayer row is clickable on any day`() {
        show(state = baseState())
        composeRule.onNodeWithText("Fajr").assertHasNoClickAction()
        composeRule.onNodeWithText("Asr").assertHasNoClickAction()
    }
```

Adapt `baseState()` / `tomorrow` to whatever the file's existing fixtures are called. Keep the
eight surviving tests: the six prayers list, the location header and its default flag, the "Today"
pill only off today, swipe paging both ways, sun-time placeholders, and the sky's back/settings
pills navigating.

- [ ] **Step 5: Run the module's tests**

```bash
./gradlew :feature:prayer:testDebugUnitTest
```

Expected: PASS.

- [ ] **Step 6: Run the full gate set for the touched modules**

```bash
./gradlew :core:ui:check :feature:prayer:check :app:compileDebugKotlin :app:testDebugUnitTest
```

Expected: all PASS.

- [ ] **Step 7: Run lint — it is a real gate and it is slow, so run it once, here**

```bash
./gradlew lintDebug
```

Expected: no lint **errors**. Two it catches that nothing else does:
`MissingTranslation` (a string missing from one of the five locales) and
`LocalContextGetResourceValueCall` (a `context.getString` inside a composable).

- [ ] **Step 8: Commit**

```bash
git add -A feature/prayer core/ui/src/main/res
git commit -m "feat(prayer): the Prayer Times screen, in the app's design language

NimazScreenScaffold and one LazyColumn, matching every other redesigned
screen. The living sky stays but scrolls; the day nav bar becomes a week rail
with a month jump; the six prayer cards become six rows in one card; and the
day card carries a 24-hour solar arc with the horizon and all six prayers
marked where the sun actually puts them.

The sky and the card stop saying the same sentence: the sky keeps the moment,
the card owns the window."
```

---

### Task 7: The docs

A change is not finished until the doc that owns the area is updated. Spec §6.

**Files:**
- Modify: `docs/NAVIGATION.md` (§2.3 mermaid, line ~186)
- Modify: `docs/TESTING.md` (line ~880)
- Modify: `docs/ARCHITECTURE.md` (§8 and §9)
- Modify: `docs/CLEAN_ARCHITECTURE_CHECKLIST.md`

- [ ] **Step 1: Correct the navigation map**

`docs/NAVIGATION.md:186` currently reads:

```
        PrayerTimes --> PrayerTracker & PrayerStats & QadaPrayers & MonthlyPrayerTimes
```

`PrayerGraph.kt` wires `PrayerTimesScreen` with exactly `onNavigateBack` and
`onNavigateToSettings`, so four of those five edges do not exist. Replace the line so the Prayer
subgraph shows the destinations that are actually wired. Read the surrounding subgraph first and
keep its style; do not delete `PrayerTracker`, `PrayerStats`, `QadaPrayers` or
`MonthlyPrayerTimes` from the diagram if other edges reach them.

- [ ] **Step 2: Delete the obsolete testing-doc row**

`docs/TESTING.md:880` is a table row for `PrayerTimesTrackingTest`, describing
"#359's third site, the one that made dashboards under-count". Delete the whole row — the file and
the behaviour are gone.

- [ ] **Step 3: Update ARCHITECTURE**

In §8, add `NimazSolarArc` and `NimazPrayerRow` alongside the existing `NimazButton` /
`NimazCard` / `NimazMenuDivider` / `NimazIcons` bullets, including the rule that
`PrayerTimeCard` is Home's card-per-prayer idiom and `NimazPrayerRow` the reference-row one, and
that neither is a substitute for the other.

In §9, record the **resolved** deviation: prayer tracking had three write surfaces, and the
Prayer Times one wrote a binary vocabulary the tracker redesign had retired. Note that #359 names
only two sites, and that this removes the third.

- [ ] **Step 4: Tick the checklist**

In `docs/CLEAN_ARCHITECTURE_CHECKLIST.md`, tick the rule-8 item for the
`.clickable`-inside-a-`NimazCard` pattern as it applied to `PrayerTimesScreen`'s `DayNavBar`
(now deleted). If the file has no "one write surface per fact" entry, add one, ticked, citing
this change.

- [ ] **Step 5: Run the docs gate**

```bash
python3 scripts/check_docs.py
```

Expected: PASS. It checks routes both directions, destination counts, `ScreenTags` coverage, doc
headers, index entries and every cross-doc link and anchor.

- [ ] **Step 6: Validate the mermaid you edited**

```bash
npm install --no-save mermaid jsdom && node scripts/check_mermaid.mjs
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add docs/
git commit -m "docs: the Prayer Times redesign, and two docs that were already wrong

NAVIGATION.md's map asserted four PrayerTimes edges PrayerGraph.kt never
wired, and TESTING.md documented a test this branch deletes."
```

---

## Final verification

Run the whole gate set once, from a clean state:

```bash
./gradlew :core:ui:check :feature:prayer:check
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest
./gradlew :app:jacocoTestReport --dry-run
./gradlew lintDebug
python3 scripts/check_docs.py
```

`--dry-run` on `jacocoTestReport` still configures the build and stores the configuration-cache
entry, so it catches a build-script serialisation mistake in seconds without needing the private
content-repo credential that the task itself would require.

**`assembleDebugAndroidTest` is not required** for this branch: no `Route` or `ScreenTags` entry
changed, and `PrayerTimesScreen`'s signature is unchanged, so `FeatureNavigationTest` cannot have
broken. Run it anyway if any step above ended up touching `core/navigation`.

---

## Self-review notes

**Spec coverage.** §1 → Task 5. §2 → Tasks 1–2. §3 → Task 3. §4 → Tasks 4 and 6. §5.1 → Tasks 1–2.
§5.2 → Task 6 Step 4. §5.3 → Task 5 Step 1. §6 → Task 7. §6.1 → Task 6 Step 1. §7 is explicitly
out of scope and has no task, correctly. §8's build sequence maps one-to-one onto Tasks 1–7.

**Known soft spots**, flagged rather than hidden:

1. **Task 2 Step 4 expects a test to need fixing.** `drawText` inside a `Canvas` creates no
   semantics node, so the label assertions will likely fail. The step says what to do and — more
   importantly — what *not* to do (add per-node semantics, which would undo the
   `clearAndSetSemantics` the accessibility design depends on).
2. **Task 6's code is a composition sketch, not a transcription.** Parameter names on
   `NimazIconButton`, `NimazCard`, `NimazBadgeDefaults` and `ArabicText` must be checked against
   the real atoms. Every place this matters says so.
3. **Task 5's regression guard is green from the start.** That is what a guard is. The step
   includes the temporary-failure check that proves it is load-bearing rather than vacuous.
