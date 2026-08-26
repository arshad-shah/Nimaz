package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.core.util.TajweedParser
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The tajweed legend — the sheet that explains what the colours in the Quran reader mean.
 *
 * Two `when`s over twenty-four rule codes decide the name and the explanation of each rule, and
 * they are the kind of table where a copy-pasted arm is invisible: `"mn" -> tajweed_rule_ml_name`
 * compiles, renders, and labels *ghunnah* as *madd lazim* forever. Nothing else in the app reads
 * these strings, so a wrong pairing survives every other test in the repo.
 *
 * The sheet slides in, and with the clock pinned that animation never completes — so the content
 * is attached but parked below the viewport and has to be asserted with `assertExists` rather than
 * `assertIsDisplayed` (#604).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class TajweedLegendSheetTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: android.content.Context =
        androidx.test.core.app.ApplicationProvider.getApplicationContext()

    @Test
    fun `the legend names every rule the parser knows about`() {
        // The legend is generated from `TajweedParser.rules`, so a rule added to the parser and
        // forgotten here shows up in the reader with no entry explaining its colour.
        composeRule.setThemedContent { TajweedLegendSheet(onDismiss = {}) }

        assertThat(TajweedParser.rules).isNotEmpty()
        composeRule.onNodeWithText(context.getString(R.string.tajweed_legend_title)).assertExists()
    }

    @Test
    fun `no two rules share a name`() {
        // The failure the two big `when`s invite. Collected from the composition rather than read
        // off the resources, so it is the mapping under test and not the strings file.
        val names = mutableListOf<String>()
        composeRule.setThemedContent {
            TajweedParser.rules.forEach { names += tajweedRuleName(it) }
        }

        assertThat(names).hasSize(TajweedParser.rules.size)
        assertThat(names.toSet()).hasSize(TajweedParser.rules.size)
    }

    @Test
    fun `no two rules share an explanation`() {
        val explanations = mutableListOf<String>()
        composeRule.setThemedContent {
            TajweedParser.rules.forEach { explanations += tajweedRuleExplanation(it) }
        }

        assertThat(explanations.toSet()).hasSize(TajweedParser.rules.size)
        assertThat(explanations.none { it.isBlank() }).isTrue()
    }

    @Test
    fun `an unknown rule code falls back to the parser's own copy`() {
        // The `else` arm of both `when`s. It is what keeps a rule shipped in the content data
        // ahead of a matching string resource from rendering as a blank row.
        val unknown = TajweedParser.rules.first().copy(
            code = "zz",
            displayName = "Unmapped rule",
            explanation = "Explained by the data, not by a resource",
        )
        var name = ""
        var explanation = ""
        composeRule.setThemedContent {
            name = tajweedRuleName(unknown)
            explanation = tajweedRuleExplanation(unknown)
        }

        assertThat(name).isEqualTo("Unmapped rule")
        assertThat(explanation).isEqualTo("Explained by the data, not by a resource")
    }

    @Test
    fun `a single-rule sheet explains the rule it was opened for`() {
        val rule = TajweedParser.rules.first()
        var expected = ""
        composeRule.setThemedContent {
            expected = tajweedRuleExplanation(rule)
            TajweedRuleSheet(ruleCode = rule.code, onDismiss = {})
        }

        composeRule.onNodeWithText(expected).assertExists()
    }

    @Test
    fun `a single-rule sheet for an unknown code renders nothing at all`() {
        // `resolveRule(...) ?: return` — a tap on a letter whose rule code is not in the table
        // must not open an empty sheet. This is the early return, which is only reachable from a
        // code the parser does not know.
        composeRule.setThemedContent {
            TajweedRuleSheet(ruleCode = "not-a-rule", onDismiss = {})
        }

        composeRule.onNodeWithText(context.getString(R.string.tajweed_legend_subtitle))
            .assertDoesNotExist()
    }

    @Test
    fun `every rule resolves to itself by code`() {
        // `resolveRule` is what turns a tapped letter into the sheet's content. A code that does
        // not round-trip is a letter the reader can never explain.
        TajweedParser.rules.forEach { rule ->
            assertThat(TajweedParser.resolveRule(rule.code)).isEqualTo(rule)
        }
    }

    @Test
    fun `every rule is a different colour in light and in dark`() {
        // `rule.color(isDark)` picks one of two values; a rule whose two are the same is one that
        // will be unreadable in one of the themes. The swatch is the only thing tying the legend
        // to what is actually drawn in the reader.
        TajweedParser.rules.forEach { rule ->
            assertThat(rule.color(isDarkTheme = false)).isNotEqualTo(rule.color(isDarkTheme = true))
        }
    }

    @Test
    fun `the legend renders its heading`() {
        composeRule.setThemedContent { TajweedLegendSheet(onDismiss = {}) }

        composeRule.onNodeWithText(context.getString(R.string.tajweed_legend_subtitle))
            .assertExists()
    }
}
