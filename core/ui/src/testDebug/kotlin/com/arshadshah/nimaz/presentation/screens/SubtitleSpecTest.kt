package com.arshadshah.nimaz.presentation.screens

import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * A screen's subtitle, described as data and resolved where it is drawn.
 *
 * The point of `SubtitleSpec` is that a ViewModel can say "42 bookmarks" without holding a
 * `Context` — it names a resource and its arguments, and the composable resolves them. The two
 * branches that matter are the plural one and the nested-resource one: `quantity` decides between
 * `pluralStringResource` and `stringResource`, and picking the wrong one either crashes on a
 * plurals id or renders "%d bookmarks" literally. Neither is caught by a compiler.
 */
@RunWith(RobolectricTestRunner::class)
class SubtitleSpecTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    /**
     * Resolves every [specs] entry in **one** composition.
     *
     * One call per resolve would mean calling `setContent` twice in a test that compares two
     * results, which a compose rule refuses outright ("has already set content", #604).
     */
    private fun resolveAll(vararg specs: SubtitleSpec?): List<String?> {
        val out = MutableList<String?>(specs.size) { null }
        composeRule.setThemedContent {
            specs.forEachIndexed { index, spec -> out[index] = spec.resolve() }
        }
        composeRule.waitForIdle()
        return out
    }

    private fun resolved(spec: SubtitleSpec?): String? = resolveAll(spec).single()

    @Test
    fun `no subtitle resolves to nothing`() {
        // The null receiver, which is how a screen says "no subtitle" without a second state.
        assertThat(resolved(null)).isNull()
    }

    @Test
    fun `a plain string resource resolves to its text`() {
        assertThat(resolved(SubtitleSpec(R.string.read))).isNotEmpty()
    }

    @Test
    fun `a count argument is substituted`() {
        val text = resolved(
            SubtitleSpec(
                res = R.string.share_zakat_line_format,
                args = listOf(SubtitleArg.Text("Assets"), SubtitleArg.Count(42)),
            )
        )

        assertThat(text).contains("42")
        assertThat(text).contains("Assets")
    }

    @Test
    fun `a nested resource argument is resolved before it is substituted`() {
        // `SubtitleArg.Resource` is the one arm that needs the composition — a ViewModel naming
        // another string id rather than a literal is exactly why this seam exists.
        val (expectedInner, text) = resolveAll(
            SubtitleSpec(R.string.read),
            SubtitleSpec(
                res = R.string.share_zakat_line_format,
                args = listOf(SubtitleArg.Resource(R.string.read), SubtitleArg.Text("now")),
            ),
        )

        assertThat(text).contains(requireNotNull(expectedInner))
    }

    @Test
    fun `a quantity picks the plural form rather than the singular resource`() {
        // `quantity != null` switches the whole lookup to `pluralStringResource`. Reading it the
        // other way passes a plurals id to `stringResource`, which does not fail at compile time.
        val (one, many) = resolveAll(
            SubtitleSpec(
                res = R.plurals.qaida_a11y_lesson_complete_format,
                args = listOf(SubtitleArg.Count(1), SubtitleArg.Text("Alif"), SubtitleArg.Count(1)),
                quantity = 1,
            ),
            SubtitleSpec(
                res = R.plurals.qaida_a11y_lesson_complete_format,
                args = listOf(SubtitleArg.Count(1), SubtitleArg.Text("Alif"), SubtitleArg.Count(3)),
                quantity = 3,
            ),
        )

        assertThat(one).isNotEmpty()
        assertThat(many).isNotEmpty()
        assertThat(one).isNotEqualTo(many)
    }
}
