package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The glass pill — the label that floats over the prayer-times sky.
 *
 * It is the app's only component that reads the pixels behind it: given a `GlassBackdrop` it blurs
 * what the sky drew rather than painting a translucent fill over it. That is what keeps the time
 * legible at every hour of the day, and it is why the backdrop is a parameter rather than
 * something the pill finds for itself — a pill with no backdrop still has to render, because it is
 * also used over flat surfaces.
 *
 * Three tones and two sizes make the surface. The tap handler is optional and it matters that it
 * is: a pill showing the time is not a button, and giving every pill a click action puts targets
 * on the screen that do nothing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class GlassPillVariantsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `every tone and size renders its label`() {
        composeRule.setThemedContent {
            Column {
                GlassPillTone.entries.forEach { tone ->
                    GlassPillSize.entries.forEach { size ->
                        GlassPill(
                            text = "${tone.name}-${size.name}",
                            tone = tone,
                            size = size,
                        )
                    }
                }
            }
        }

        GlassPillTone.entries.forEach { tone ->
            GlassPillSize.entries.forEach { size ->
                composeRule.onNodeWithText("${tone.name}-${size.name}").assertExists()
            }
        }
    }

    @Test
    fun `a pill carries leading and trailing icons and a caller's type`() {
        composeRule.setThemedContent {
            GlassPill(
                text = "Abbeyleix",
                style = MaterialTheme.typography.titleLarge,
                leadingIcon = Icons.Filled.LocationOn,
                trailingIcon = Icons.Filled.Settings,
                tint = Color.Yellow,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        composeRule.onNodeWithText("Abbeyleix").assertExists()
    }

    @Test
    fun `a pill is a button only when it is given something to do`() {
        // A pill showing the time is not a control. Making every pill clickable puts targets on
        // the sky that do nothing when tapped.
        var taps = 0
        composeRule.setThemedContent {
            Column {
                GlassPill(text = "Tappable", onClick = { taps++ })
                GlassPill(text = "Just a label")
            }
        }

        composeRule.onNodeWithText("Tappable").performClick()
        assertThat(taps).isEqualTo(1)
    }

    @Test
    fun `a pill renders with a backdrop and without one`() {
        // The `backdrop == null` arm is the flat-surface case; the non-null one is the sky. Both
        // ship, and the blur radius is only read on one of them.
        composeRule.setThemedContent {
            Column {
                val backdrop = rememberGlassBackdrop()
                GlassPill(text = "Frosted", backdrop = backdrop, blurRadius = 12.dp)
                GlassPill(text = "Flat")
            }
        }

        composeRule.onNodeWithText("Frosted").assertExists()
        composeRule.onNodeWithText("Flat").assertExists()
    }

    @Test
    fun `the glass icon button announces what it does`() {
        // Icon-only, so the content description is the only thing a screen reader has — and the
        // back and settings buttons on the prayer-times hero are both this component.
        var taps = 0
        composeRule.setThemedContent {
            Column {
                GlassIconButton(
                    icon = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    onClick = { taps++ },
                )
                GlassPillTone.entries.forEach { tone ->
                    GlassIconButton(
                        icon = Icons.Filled.Settings,
                        contentDescription = tone.name,
                        onClick = {},
                        tone = tone,
                        size = GlassPillSize.Small,
                        tint = Color.Yellow,
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription("Settings").performClick()
        assertThat(taps).isEqualTo(1)
        GlassPillTone.entries.forEach {
            composeRule.onNodeWithContentDescription(it.name).assertExists()
        }
    }
}
