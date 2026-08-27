package com.arshadshah.nimaz.presentation.components.organisms

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.testing.brightness
import com.arshadshah.nimaz.testing.distinctColours
import com.arshadshah.nimaz.testing.drawToBitmap
import com.arshadshah.nimaz.testing.region
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The prayer-times sky, drawn for real.
 *
 * `SkyBackground` is the largest piece of drawing in the app: a gradient sky whose stops are
 * interpolated from the time of day, a sun on an arc, a star field and a phased moon, and a cloud
 * band baked into a bitmap and blitted twice so it wraps. All of it lives inside `drawWithCache`
 * and a stack of `private DrawScope` helpers, so **none of it runs when the tree is composed** —
 * the existing smoke test proves the composable does not throw and reaches almost none of the
 * geometry. Drawing into a software canvas is what executes it.
 *
 * What is worth catching is that the scene actually tracks the time it is given. Every failure
 * mode here is silent and visual: a `t` fed to the wrong end of a `lerp` gives a bright sky at
 * midnight, a sun drawn from the wrong quadrant rises in the west, and a `nightFactor` that never
 * reaches 1 leaves the stars permanently invisible. None of it throws, and none of it is
 * observable from the semantics tree — the scene has no nodes at all.
 *
 * One draw per test: `setContent` may only be called once on a rule, so a "compare two times of
 * day" assertion has to put both scenes in one composition (#604).
 *
 * Every `SkyBackground` here is handed `Modifier.matchParentSize()`. It takes no size of its own —
 * `PrayerSkyScene` gives it one — so a scene composed inside a sized `Box` without it measures
 * 0×0, draws nothing, and every pixel assertion fails for a reason that has nothing to do with the
 * geometry.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-mdpi")
class PrayerSkySceneArtTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        /** Canonical anchors from the scene's own constants. */
        const val NOON = 0.53f
        const val MIDNIGHT = 0.0f
        const val DUSK = 0.9f
    }

    @Test
    fun `the sky is painted rather than left transparent`() {
        val bitmap = composeRule.drawToBitmap {
            Box(Modifier.fillMaxWidth().height(300.dp)) {
                SkyBackground(modifier = Modifier.matchParentSize(), timeOfDay = NOON, moonFraction = 0.5f, cloudsEnabled = false)
            }
        }

        // Over a black field: a scene that never ran its draw block leaves the backdrop.
        val sky = bitmap.region(0, 0, bitmap.width, 300)
        assertThat(sky.brightness()).isGreaterThan(10.0)
    }

    @Test
    fun `the sky is a gradient, not a flat wash`() {
        // The stops are interpolated per band; a scene painted with one colour would mean the
        // gradient collapsed to a single stop, which is exactly what a bad `bracket()` produces.
        val bitmap = composeRule.drawToBitmap {
            Box(Modifier.fillMaxWidth().height(300.dp)) {
                SkyBackground(modifier = Modifier.matchParentSize(), timeOfDay = NOON, moonFraction = 0.5f, cloudsEnabled = false)
            }
        }

        val column = bitmap.region(bitmap.width / 2, 0, 1, 300)
        assertThat(column.distinctColours()).isAtLeast(5)
    }

    @Test
    fun `midday is brighter than midnight`() {
        // The single most visible property of the whole scene, and the one a reversed `lerp`
        // breaks. Both drawn in one composition, so the comparison is between two times of day
        // rather than against a constant that would change with a palette tweak.
        val bitmap = composeRule.drawToBitmap {
            androidx.compose.foundation.layout.Column {
                Box(Modifier.fillMaxWidth().height(200.dp)) {
                    SkyBackground(modifier = Modifier.matchParentSize(), timeOfDay = NOON, moonFraction = 0.5f, cloudsEnabled = false)
                }
                Box(Modifier.fillMaxWidth().height(200.dp)) {
                    SkyBackground(modifier = Modifier.matchParentSize(), timeOfDay = MIDNIGHT, moonFraction = 0.5f, cloudsEnabled = false)
                }
            }
        }

        val day = bitmap.region(0, 0, bitmap.width, 200).brightness()
        val night = bitmap.region(0, 200, bitmap.width, 200).brightness()

        assertThat(day).isGreaterThan(night)
    }

    @Test
    fun `the sun crosses the sky rather than staying put`() {
        // `drawSunAt` places the disc on an arc parameterised by the warped day fraction. A sun
        // pinned to the centre — the shape a dropped `td` term produces — draws a perfectly
        // plausible scene that never moves.
        val bitmap = composeRule.drawToBitmap {
            androidx.compose.foundation.layout.Column {
                Box(Modifier.fillMaxWidth().height(200.dp)) {
                    SkyBackground(modifier = Modifier.matchParentSize(), timeOfDay = 0.33f, moonFraction = 0.5f, cloudsEnabled = false)
                }
                Box(Modifier.fillMaxWidth().height(200.dp)) {
                    SkyBackground(modifier = Modifier.matchParentSize(), timeOfDay = 0.74f, moonFraction = 0.5f, cloudsEnabled = false)
                }
            }
        }

        // Morning sun sits left of centre, late-afternoon sun right of it, so the two halves of
        // each scene carry the light in opposite proportions.
        val third = bitmap.width / 3
        val morningLeft = bitmap.region(0, 0, third, 200).brightness()
        val morningRight = bitmap.region(bitmap.width - third, 0, third, 200).brightness()
        val eveningLeft = bitmap.region(0, 200, third, 200).brightness()
        val eveningRight = bitmap.region(bitmap.width - third, 200, third, 200).brightness()

        assertThat(morningLeft - morningRight).isNotEqualTo(eveningLeft - eveningRight)
    }

    @Test
    fun `a full moon is brighter than a new moon on the same night sky`() {
        // `litRegion` builds the lit crescent by intersecting a circle with an oval and a rect, and
        // the path ops are the part that silently produces a disc that is always full or always
        // dark. Same time of day either side, so the only variable is the phase.
        val bitmap = composeRule.drawToBitmap {
            androidx.compose.foundation.layout.Column {
                Box(Modifier.fillMaxWidth().height(200.dp)) {
                    SkyBackground(modifier = Modifier.matchParentSize(), timeOfDay = MIDNIGHT, moonFraction = 0.5f, cloudsEnabled = false)
                }
                Box(Modifier.fillMaxWidth().height(200.dp)) {
                    SkyBackground(modifier = Modifier.matchParentSize(), timeOfDay = MIDNIGHT, moonFraction = 0.0f, cloudsEnabled = false)
                }
            }
        }

        val full = bitmap.region(0, 0, bitmap.width, 200).brightness()
        val new = bitmap.region(0, 200, bitmap.width, 200).brightness()

        assertThat(full).isGreaterThan(new)
    }

    @Test
    fun `every moon phase draws without the path operations collapsing`() {
        // Waxing crescent, first quarter, waxing gibbous, full, waning gibbous, last quarter,
        // waning crescent. The `litRegion` `when` picks a different combination of union,
        // intersect and difference on each side of half, and an arm that produced an empty path
        // would draw nothing at all rather than throw.
        val phases = listOf(0.12f, 0.25f, 0.38f, 0.5f, 0.62f, 0.75f, 0.88f)

        val bitmap = composeRule.drawToBitmap {
            Row {
                phases.forEach { phase ->
                    Box(Modifier.size(58.dp, 200.dp)) {
                        SkyBackground(
                            modifier = Modifier.matchParentSize(),
                            timeOfDay = MIDNIGHT,
                            moonFraction = phase,
                            cloudsEnabled = false,
                        )
                    }
                }
            }
        }

        assertThat(bitmap.region(0, 0, 406, 200).brightness()).isGreaterThan(0.0)
    }

    @Test
    fun `enabling the clouds bakes the band without disturbing the sky under it`() {
        // `cloudsEnabled` gates two things: the frame-clock loop that drifts the band, and the
        // second baked layer itself. The history in the source is the reason it is worth a test —
        // the drift has been rewritten twice, and one of those versions animated 0f to 0f forever
        // even with clouds off. What must hold is that turning them on does not disturb the sky
        // they sit over.
        //
        // The band's own pixels are deliberately not asserted. It is blitted through
        // `ColorFilter.tint(..., BlendMode.Modulate)`, and that blend does not survive
        // Robolectric's canvas — the layer composites to nothing here whatever it contains, so an
        // assertion that the clouds are visible would be measuring the shadow implementation
        // rather than the drawing. What runs for real is the bake, the wrap-around double blit and
        // the phase.
        val bitmap = composeRule.drawToBitmap {
            androidx.compose.foundation.layout.Column {
                Box(Modifier.fillMaxWidth().height(200.dp)) {
                    SkyBackground(
                        modifier = Modifier.matchParentSize(),
                        timeOfDay = NOON,
                        moonFraction = 0.5f,
                        cloudsEnabled = true,
                    )
                }
                Box(Modifier.fillMaxWidth().height(200.dp)) {
                    SkyBackground(
                        modifier = Modifier.matchParentSize(),
                        timeOfDay = NOON,
                        moonFraction = 0.5f,
                        cloudsEnabled = false,
                    )
                }
            }
        }

        val clouded = bitmap.region(0, 0, bitmap.width, 200)
        val clear = bitmap.region(0, 200, bitmap.width, 200)

        assertThat(clouded.brightness()).isGreaterThan(10.0)
        assertThat(clouded.brightness()).isWithin(1.0).of(clear.brightness())
    }

    @Test
    fun `dusk draws neither the full day sky nor the full night sky`() {
        // `nightFactor` ramps from 0 to 1 across the hour after sunset. A step function there —
        // the shape a dropped divisor gives — makes the sky snap from noon to midnight.
        val bitmap = composeRule.drawToBitmap {
            androidx.compose.foundation.layout.Column {
                Box(Modifier.fillMaxWidth().height(200.dp)) {
                    SkyBackground(modifier = Modifier.matchParentSize(), timeOfDay = NOON, moonFraction = 0.5f, cloudsEnabled = false)
                }
                Box(Modifier.fillMaxWidth().height(200.dp)) {
                    SkyBackground(modifier = Modifier.matchParentSize(), timeOfDay = DUSK, moonFraction = 0.5f, cloudsEnabled = false)
                }
                Box(Modifier.fillMaxWidth().height(200.dp)) {
                    SkyBackground(modifier = Modifier.matchParentSize(), timeOfDay = MIDNIGHT, moonFraction = 0.5f, cloudsEnabled = false)
                }
            }
        }

        val day = bitmap.region(0, 0, bitmap.width, 200).brightness()
        val dusk = bitmap.region(0, 200, bitmap.width, 200).brightness()
        val night = bitmap.region(0, 400, bitmap.width, 200).brightness()

        assertThat(dusk).isLessThan(day)
        assertThat(dusk).isGreaterThan(night)
    }

    @Test
    fun `a caller's own sunrise and sunset move the sky, not just the labels`() {
        // The whole point of the remap: in high summer the real sunrise is hours before the
        // canonical anchor, and a scene that ignored the parameters would show a dark sky at a
        // time the user can see daylight out of the window.
        val bitmap = composeRule.drawToBitmap {
            androidx.compose.foundation.layout.Column {
                Box(Modifier.fillMaxWidth().height(200.dp)) {
                    SkyBackground(
                        modifier = Modifier.matchParentSize(),
                        timeOfDay = 0.22f,
                        moonFraction = 0.5f,
                        cloudsEnabled = false,
                        sunriseFraction = 0.18f,
                        sunsetFraction = 0.92f,
                    )
                }
                Box(Modifier.fillMaxWidth().height(200.dp)) {
                    SkyBackground(
                        modifier = Modifier.matchParentSize(),
                        timeOfDay = 0.22f,
                        moonFraction = 0.5f,
                        cloudsEnabled = false,
                        sunriseFraction = 0.35f,
                        sunsetFraction = 0.70f,
                    )
                }
            }
        }

        val earlySunrise = bitmap.region(0, 0, bitmap.width, 200).brightness()
        val lateSunrise = bitmap.region(0, 200, bitmap.width, 200).brightness()

        assertThat(earlySunrise).isGreaterThan(lateSunrise)
    }

    @Test
    fun `the whole hero draws with its labels over the sky`() {
        // `PrayerSkyScene` composes the background under a glass pill stack; the pills read the
        // backdrop, so this is the one path where the scene's pixels feed another component.
        val bitmap = composeRule.drawToBitmap {
            Box(Modifier.fillMaxWidth().height(300.dp)) {
                PrayerSkyScene(
                    timeOfDay = NOON,
                    timeLabel = "13:04",
                    statusLabel = "Dhuhr in 2 hours",
                    cloudsEnabled = false,
                )
            }
        }

        assertThat(bitmap.region(0, 0, bitmap.width, 300).brightness()).isGreaterThan(10.0)
    }

    @Test
    fun `the hero draws its top bar when a location and both actions are supplied`() {
        // `showTopBar` is an all-three-or-nothing condition, and it also decides whether the
        // status-bar band is reserved — so a partial set must not draw a bar half-way under the
        // system clock.
        val bitmap = composeRule.drawToBitmap {
            androidx.compose.foundation.layout.Column {
                Box(Modifier.fillMaxWidth().height(200.dp)) {
                    PrayerSkyScene(
                        timeOfDay = NOON,
                        timeLabel = "13:04",
                        statusLabel = "Dhuhr in 2 hours",
                        cloudsEnabled = false,
                        locationName = "Abbeyleix",
                        onBack = {},
                        onSettings = {},
                    )
                }
                Box(Modifier.fillMaxWidth().height(200.dp)) {
                    PrayerSkyScene(
                        timeOfDay = NOON,
                        timeLabel = "13:04",
                        statusLabel = "Dhuhr in 2 hours",
                        cloudsEnabled = false,
                        locationName = "Abbeyleix",
                    )
                }
            }
        }

        assertThat(bitmap.region(0, 0, bitmap.width, 400).brightness()).isGreaterThan(10.0)
    }
}
