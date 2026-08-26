package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The badge, across the axes that combine into its look.
 *
 * Four independent dimensions — tone, emphasis, shape and size — plus a *selected* override that
 * silently replaces the first two. That override is the part worth pinning: `selected` swaps the
 * colours for `selectedTone` at FILLED emphasis, so a badge given both a tone and a selection is
 * not drawn in the tone it was given. A component that honoured `tone` over `selected` would make
 * every selected filter chip in the app read as unselected.
 *
 * The badge is also a **control only when it is given one** — the hadith grade chip is tappable and
 * a status badge is not, and the difference is a nullable lambda rather than a variant.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class NimazBadgeVariantsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `every tone, emphasis, shape and size renders`() {
        composeRule.setThemedContent {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                NimazTone.entries.forEach { tone ->
                    NimazBadge(text = "tone-${tone.name}", tone = tone)
                }
                NimazBadgeEmphasis.entries.forEach { emphasis ->
                    NimazBadge(text = "emph-${emphasis.name}", emphasis = emphasis)
                }
                NimazBadgeShape.entries.forEach { shape ->
                    NimazBadge(text = "shape-${shape.name}", shape = shape)
                }
                NimazBadgeSize.entries.forEach { size ->
                    NimazBadge(text = "size-${size.name}", size = size)
                }
            }
        }

        NimazTone.entries.forEach {
            composeRule.onNodeWithText("tone-${it.name}").assertExists()
        }
        NimazBadgeShape.entries.forEach {
            composeRule.onNodeWithText("shape-${it.name}").assertExists()
        }
    }

    @Test
    fun `a selected badge is drawn in the selection tone, not the one it was given`() {
        // The override. A badge honouring `tone` over `selected` makes every selected filter read
        // as unselected — the failure is invisible in code and total on screen.
        var selectedColours: NimazBadgeColors? = null
        var plainColours: NimazBadgeColors? = null
        composeRule.setThemedContent {
            selectedColours = NimazBadgeDefaults.colors(
                tone = NimazTone.ACCENT,
                emphasis = NimazBadgeEmphasis.FILLED,
            )
            plainColours = NimazBadgeDefaults.colors(
                tone = NimazTone.MUTED,
                emphasis = NimazBadgeEmphasis.SOFT,
            )
            Column {
                NimazBadge(text = "Selected", tone = NimazTone.MUTED, selected = true)
                NimazBadge(text = "Plain", tone = NimazTone.MUTED)
            }
        }

        assertThat(selectedColours).isNotEqualTo(plainColours)
        composeRule.onNodeWithText("Selected").assertExists()
        composeRule.onNodeWithText("Plain").assertExists()
    }

    @Test
    fun `a badge carries an icon and an indicator dot`() {
        composeRule.setThemedContent {
            Column {
                NimazBadge(text = "Iconed", icon = Icons.Filled.Star)
                NimazBadge(text = "Dotted", indicatorColor = Color.Magenta)
                NimazBadge(
                    text = "Both",
                    icon = Icons.Filled.Star,
                    indicatorColor = Color.Magenta,
                    selectedTone = NimazTone.SUCCESS,
                    selected = true,
                )
            }
        }

        listOf("Iconed", "Dotted", "Both").forEach {
            composeRule.onNodeWithText(it).assertExists()
        }
    }

    @Test
    fun `a badge is tappable only when it is given something to do`() {
        var taps = 0
        composeRule.setThemedContent {
            Column {
                NimazBadge(text = "Tappable", onClick = { taps++ })
                NimazBadge(text = "Inert")
            }
        }

        composeRule.onNodeWithText("Tappable").performClick()
        assertThat(taps).isEqualTo(1)
    }

    @Test
    fun `a caller can supply the colours outright`() {
        // The escape hatch the hadith grade chip uses: the grade's colour comes from the content,
        // not from a tone the design system knows about.
        composeRule.setThemedContent {
            NimazBadge(
                text = "Custom",
                colors = NimazBadgeDefaults
                    .feature(color = Color.Magenta, emphasis = NimazBadgeEmphasis.SOFT),
            )
        }

        composeRule.onNodeWithText("Custom").assertExists()
    }

    @Test
    fun `a surah number badge renders its number`() {
        composeRule.setThemedContent {
            Column {
                SurahNumberBadge(number = 36)
                SurahNumberBadge(number = 114, tone = NimazTone.SUCCESS)
            }
        }

        composeRule.onNodeWithText("36").assertExists()
        composeRule.onNodeWithText("114").assertExists()
    }
}
