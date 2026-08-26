package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.ui.graphics.Color
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The badge's colour table — every tone crossed with every emphasis.
 *
 * `NimazBadgeDefaults` is where the design system decides what "a warning, softly" and "an error,
 * filled" actually look like, and it is read from far more places than the badge: `NimazErrorState`
 * takes its accent and its wash from here, and the hadith grade chip takes its border from
 * `feature()`. That makes a hole in the table a colour that goes wrong on several unrelated
 * screens at once, and the table is a stack of `when`s with no exhaustiveness to lean on beyond
 * the enum.
 *
 * The properties worth holding are the ones the emphasis names promise: **filled is the loudest**,
 * **outlined carries a border and a transparent container**, and **the content colour is never the
 * container colour** — a badge whose text matched its background would be invisible while every
 * layout assertion passed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class NimazBadgeDefaultsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private fun table(): Map<Pair<NimazTone, NimazBadgeEmphasis>, NimazBadgeColors> {
        val out = mutableMapOf<Pair<NimazTone, NimazBadgeEmphasis>, NimazBadgeColors>()
        composeRule.setThemedContent {
            NimazTone.entries.forEach { tone ->
                NimazBadgeEmphasis.entries.forEach { emphasis ->
                    out[tone to emphasis] = NimazBadgeDefaults.colors(tone, emphasis)
                }
            }
        }
        composeRule.waitForIdle()
        return out
    }

    @Test
    fun `every tone and emphasis resolves to a complete set of colours`() {
        val resolved = table()

        assertThat(resolved)
            .hasSize(NimazTone.entries.size * NimazBadgeEmphasis.entries.size)
        resolved.values.forEach {
            assertThat(it.contentColor).isNotNull()
            assertThat(it.containerColor).isNotNull()
        }
    }

    @Test
    fun `a badge's ink never matches its own container`() {
        // The failure that renders perfectly and shows nothing: text the colour of the pill behind
        // it. `TRANSPARENT` is the one legitimate exception — it has no container to contrast with.
        table()
            .filterKeys { (tone, _) -> tone != NimazTone.TRANSPARENT }
            .forEach { (key, colors) ->
                if (colors.containerColor != Color.Transparent) {
                    assertThat(colors.contentColor).isNotEqualTo(colors.containerColor)
                }
            }
    }

    @Test
    fun `an outlined badge carries a border and a filled one does not need it`() {
        // Outlined is the only emphasis whose shape comes from a stroke rather than a fill, so a
        // dropped border colour leaves an invisible badge on a matching surface.
        val resolved = table()

        NimazTone.entries
            .filter { it != NimazTone.TRANSPARENT }
            .forEach { tone ->
                val outlined = resolved.getValue(tone to NimazBadgeEmphasis.OUTLINED)
                assertThat(outlined.borderColor).isNotEqualTo(Color.Transparent)
            }
    }

    @Test
    fun `a feature badge takes a colour the design system does not know about`() {
        // The escape hatch the hadith grade chip uses — the grade's colour comes from the content.
        // Every emphasis has to honour it, or a da'if chip renders in the neutral tone.
        val featured = mutableMapOf<NimazBadgeEmphasis, NimazBadgeColors>()
        composeRule.setThemedContent {
            NimazBadgeEmphasis.entries.forEach {
                featured[it] = NimazBadgeDefaults.feature(color = Color.Magenta, emphasis = it)
            }
        }

        assertThat(featured).hasSize(NimazBadgeEmphasis.entries.size)
        assertThat(featured.values.map { it.contentColor to it.containerColor }.toSet())
            .hasSize(NimazBadgeEmphasis.entries.size)
    }

    @Test
    fun `every badge size has its own type`() {
        // Three sizes used in three densities — a dense tracker row, a settings value, a detail
        // sheet. Two resolving to one style makes the middle size pointless.
        val styles = mutableListOf<androidx.compose.ui.text.TextStyle>()
        composeRule.setThemedContent {
            NimazBadgeSize.entries.forEach { styles += NimazBadgeDefaults.textStyleFor(it) }
        }

        assertThat(styles).hasSize(NimazBadgeSize.entries.size)
        assertThat(styles.toSet()).hasSize(NimazBadgeSize.entries.size)
    }
}
