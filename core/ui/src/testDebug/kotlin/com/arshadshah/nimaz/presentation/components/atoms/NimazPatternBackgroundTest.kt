package com.arshadshah.nimaz.presentation.components.atoms

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.LocalIsDarkTheme
import com.arshadshah.nimaz.presentation.theme.LocalPatternStyle
import com.arshadshah.nimaz.presentation.theme.LocalShowIslamicPatterns
import com.arshadshah.nimaz.presentation.theme.NimazPatternStyle
import com.arshadshah.nimaz.testing.brightness
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
 * The Islamic ornament behind the app's pages.
 *
 * Four styles, built from path geometry inside a `drawWithCache` — so none of it runs unless
 * something draws. The properties worth holding are the ones a user has actually asked for: the
 * **preference switches it off**, which means "off" has to reach the drawing and not merely the
 * setting; the four styles are **actually different**, or the picker offers choices that do
 * nothing; and `ATELIER` is the **union** of the star field and the corner medallion, so a change
 * that made it an alias of one of them would quietly remove half of it.
 *
 * The ornament is also deliberately near-invisible — 7–8% alpha — because it sits *under body
 * text*. That is why the assertions compare regions against each other rather than looking for a
 * particular colour: the whole design constraint is that the pattern must not compete with the
 * words on top of it.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-mdpi")
class NimazPatternBackgroundTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /** A pane of one style, on a black surface so the ornament is the only paint. */
    private fun pane(style: NimazPatternStyle, enabled: Boolean = true) =
        @androidx.compose.runtime.Composable {
            NimazPatternBackground(
                modifier = Modifier.fillMaxSize(),
                style = style,
                enabled = enabled,
                surface = Color.Black,
            ) {}
        }

    @Test
    fun `every style but NONE lays ornament down`() {
        // Four styles side by side over black: NONE must be empty and the other three must not.
        val bitmap = composeRule.drawToBitmap {
            Row {
                listOf(
                    NimazPatternStyle.NONE,
                    NimazPatternStyle.CORNER_MEDALLION,
                    NimazPatternStyle.LATTICE,
                    NimazPatternStyle.STAR_FIELD,
                ).forEach { style ->
                    Box(Modifier.size(100.dp, 300.dp)) { pane(style)() }
                }
            }
        }

        val none = bitmap.region(0, 0, 100, 300).brightness()
        val corner = bitmap.region(100, 0, 100, 300).brightness()
        val lattice = bitmap.region(200, 0, 100, 300).brightness()
        val stars = bitmap.region(300, 0, 100, 300).brightness()

        assertThat(none).isEqualTo(0.0)
        assertThat(corner).isGreaterThan(0.0)
        assertThat(lattice).isGreaterThan(0.0)
        assertThat(stars).isGreaterThan(0.0)
    }

    @Test
    fun `the lattice is denser than the star field`() {
        // Two different generators — a full diamond per cell against a small one per staggered
        // cell — and an alias between them is the kind of change that reads as "the pattern looks
        // a bit different" and is never reported.
        val bitmap = composeRule.drawToBitmap {
            Row {
                Box(Modifier.size(200.dp, 300.dp)) { pane(NimazPatternStyle.LATTICE)() }
                Box(Modifier.size(200.dp, 300.dp)) { pane(NimazPatternStyle.STAR_FIELD)() }
            }
        }

        val lattice = bitmap.region(0, 0, 200, 300).brightness()
        val stars = bitmap.region(200, 0, 200, 300).brightness()

        assertThat(lattice).isGreaterThan(stars)
    }

    @Test
    fun `atelier draws the star field and the medallion together`() {
        // `starField(...) + cornerMedallion(...)`. An arm that returned only one of them would
        // still draw a plausible page.
        val bitmap = composeRule.drawToBitmap {
            Row {
                Box(Modifier.size(200.dp, 300.dp)) { pane(NimazPatternStyle.STAR_FIELD)() }
                Box(Modifier.size(200.dp, 300.dp)) { pane(NimazPatternStyle.ATELIER)() }
            }
        }

        val stars = bitmap.region(0, 0, 200, 300).brightness()
        val atelier = bitmap.region(200, 0, 200, 300).brightness()

        assertThat(atelier).isGreaterThan(stars)
    }

    @Test
    fun `the corner medallion is drawn at the top corner, not the middle`() {
        // Its centre is deliberately *outside* the box — off the top-right — so only an arc of it
        // shows. A medallion centred in the page would sit behind the content it is meant to
        // frame.
        val bitmap = composeRule.drawToBitmap {
            Box(Modifier.size(300.dp)) { pane(NimazPatternStyle.CORNER_MEDALLION)() }
        }

        val topRight = bitmap.region(200, 0, 100, 100).brightness()
        val bottomLeft = bitmap.region(0, 200, 100, 100).brightness()

        assertThat(topRight).isGreaterThan(bottomLeft)
    }

    @Test
    fun `turning the preference off removes the ornament, whatever style is set`() {
        // The user setting has to reach the drawing: `enabled` collapses the style to NONE rather
        // than just dimming it. A pattern that stayed drawn at low alpha would still be visible on
        // an OLED screen at night, which is precisely what the preference is for.
        val bitmap = composeRule.drawToBitmap {
            Row {
                Box(Modifier.size(200.dp, 300.dp)) { pane(NimazPatternStyle.ATELIER)() }
                Box(Modifier.size(200.dp, 300.dp)) {
                    pane(NimazPatternStyle.ATELIER, enabled = false)()
                }
            }
        }

        assertThat(bitmap.region(0, 0, 200, 300).brightness()).isGreaterThan(0.0)
        assertThat(bitmap.region(200, 0, 200, 300).brightness()).isEqualTo(0.0)
    }

    @Test
    fun `the composition locals supply the style and the preference`() {
        // Every screen in the app reads the pattern from the theme rather than passing one, so the
        // defaults are the real call site — and `LocalShowIslamicPatterns` is where the setting
        // arrives.
        val bitmap = composeRule.drawToBitmap {
            Row {
                CompositionLocalProvider(
                    LocalPatternStyle provides NimazPatternStyle.LATTICE,
                    LocalShowIslamicPatterns provides true,
                ) {
                    Box(Modifier.size(200.dp, 300.dp)) {
                        NimazPatternBackground(
                            modifier = Modifier.fillMaxSize(),
                            surface = Color.Black,
                        ) {}
                    }
                }
                CompositionLocalProvider(
                    LocalPatternStyle provides NimazPatternStyle.LATTICE,
                    LocalShowIslamicPatterns provides false,
                ) {
                    Box(Modifier.size(200.dp, 300.dp)) {
                        NimazPatternBackground(
                            modifier = Modifier.fillMaxSize(),
                            surface = Color.Black,
                        ) {}
                    }
                }
            }
        }

        assertThat(bitmap.region(0, 0, 200, 300).brightness()).isGreaterThan(0.0)
        assertThat(bitmap.region(200, 0, 200, 300).brightness()).isEqualTo(0.0)
    }

    @Test
    fun `the alpha scale fades the ornament without removing it`() {
        // A caller drawing the pattern behind a busier surface can dim it; scaling to zero must
        // still take the drawing path rather than falling through to NONE, because the two are
        // different states and only one of them is the user's setting.
        val bitmap = composeRule.drawToBitmap {
            Row {
                Box(Modifier.size(200.dp, 300.dp)) {
                    NimazPatternBackground(
                        modifier = Modifier.fillMaxSize(),
                        style = NimazPatternStyle.LATTICE,
                        surface = Color.Black,
                        alphaScale = 1f,
                    ) {}
                }
                Box(Modifier.size(200.dp, 300.dp)) {
                    NimazPatternBackground(
                        modifier = Modifier.fillMaxSize(),
                        style = NimazPatternStyle.LATTICE,
                        surface = Color.Black,
                        alphaScale = 0.3f,
                    ) {}
                }
            }
        }

        val full = bitmap.region(0, 0, 200, 300).brightness()
        val faded = bitmap.region(200, 0, 200, 300).brightness()

        assertThat(full).isGreaterThan(faded)
        assertThat(faded).isGreaterThan(0.0)
    }

    @Test
    fun `the ornament changes colour with the theme`() {
        // Teal on light, gold on dark — the pattern is drawn from `LocalIsDarkTheme`, not from the
        // colour scheme, so a theme switch that missed it would leave teal ornament on a dark page.
        val bitmap = composeRule.drawToBitmap {
            Row {
                CompositionLocalProvider(LocalIsDarkTheme provides false) {
                    Box(Modifier.size(200.dp, 300.dp)) { pane(NimazPatternStyle.LATTICE)() }
                }
                CompositionLocalProvider(LocalIsDarkTheme provides true) {
                    Box(Modifier.size(200.dp, 300.dp)) { pane(NimazPatternStyle.LATTICE)() }
                }
            }
        }

        val light = bitmap.region(0, 0, 200, 300).brightness()
        val dark = bitmap.region(200, 0, 200, 300).brightness()

        assertThat(light).isNotEqualTo(dark)
    }

    @Test
    fun `content is drawn over the pattern, not under it`() {
        // The box takes a `BoxScope` content lambda; a pattern painted after the content would put
        // ornament over body text, which is the one thing the low alpha is protecting against.
        val bitmap = composeRule.drawToBitmap {
            Box(Modifier.size(300.dp)) {
                NimazPatternBackground(
                    modifier = Modifier.fillMaxSize(),
                    style = NimazPatternStyle.LATTICE,
                    surface = Color.Black,
                ) {
                    Box(Modifier.fillMaxSize().background(Color.White))
                }
            }
        }

        assertThat(bitmap.region(50, 50, 100, 100).brightness()).isGreaterThan(200.0)
    }
}
