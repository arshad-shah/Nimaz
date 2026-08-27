package com.arshadshah.nimaz.presentation.screens.help

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The two lookups that turn shipped help **data** into an icon and a colour.
 *
 * Both are `when` expressions over strings a release writes and this build reads, and both have a
 * silent failure mode: a key that does not match falls through to the default, so a topic whose
 * `iconKey` was renamed in the content release renders the generic question mark and a topic whose
 * `colorKey` moved renders the theme's own primary. Nothing throws, nothing logs, and the screen
 * still lays out — the only symptom is that every help topic starts looking the same.
 *
 * So the assertion that matters is not "this key maps to this icon" — that is restating the
 * `when` — but **"every key the content actually ships is one this build knows"**. The fallbacks
 * are asserted too, because they are the contract for content newer than the app: an unknown key
 * must degrade to something legible rather than crash.
 */
@RunWith(RobolectricTestRunner::class)
class HelpContentUiTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    /**
     * Every `iconKey` the shipped help content uses. Adding a key to `helpIcon` without adding
     * it here is harmless; shipping content with a key `helpIcon` does not know is not.
     */
    private val shippedIconKeys = listOf(
        "schedule", "notifications_active", "explore", "menu_book", "task_alt", "build",
        "tune", "more_time", "rocket_launch", "calculate", "favorite", "auto_stories",
        "widgets", "language", "dark_mode", "import_contacts",
    )

    private val shippedColourKeys =
        listOf("indigo", "gold", "teal", "green", "violet", "orange", "sky")

    @Test
    fun `every shipped icon key resolves to something other than the fallback`() {
        val fallback = Icons.AutoMirrored.Filled.HelpOutline

        val resolved: Map<String, ImageVector> = shippedIconKeys.associateWith { helpIcon(it) }

        assertThat(resolved.filterValues { it == fallback }).isEmpty()
    }

    @Test
    fun `an unknown or absent icon key degrades to the help glyph`() {
        // Content written by a newer release, read by this one. Not a crash, and not a blank
        // box where an icon should be.
        assertThat(helpIcon("a_key_this_build_has_never_heard_of"))
            .isEqualTo(Icons.AutoMirrored.Filled.HelpOutline)
        assertThat(helpIcon(null)).isEqualTo(Icons.AutoMirrored.Filled.HelpOutline)
    }

    @Test
    fun `the icon keys are distinct enough to tell topics apart`() {
        // Two keys deliberately share one glyph — "menu_book" and "import_contacts" are both
        // reading. Everything else is its own, which is what makes the topic grid scannable.
        val icons = shippedIconKeys.map { helpIcon(it) }

        assertThat(icons.toSet()).hasSize(shippedIconKeys.size - 1)
    }

    @Test
    fun `every shipped colour key resolves to its own colour`() {
        val resolved = mutableMapOf<String, Color>()
        composeRule.setThemedContent {
            shippedColourKeys.forEach { resolved[it] = helpColor(it) }
        }
        composeRule.waitForIdle()

        assertThat(resolved.keys).containsExactlyElementsIn(shippedColourKeys)
        assertThat(resolved.values.toSet()).hasSize(shippedColourKeys.size)
    }

    @Test
    fun `an unknown colour key falls back to the theme's own primary`() {
        var unknown: Color? = null
        var themePrimary: Color? = null
        composeRule.setThemedContent {
            unknown = helpColor("chartreuse")
            themePrimary = MaterialTheme.colorScheme.primary
        }
        composeRule.waitForIdle()

        assertThat(unknown).isEqualTo(themePrimary)
    }
}
