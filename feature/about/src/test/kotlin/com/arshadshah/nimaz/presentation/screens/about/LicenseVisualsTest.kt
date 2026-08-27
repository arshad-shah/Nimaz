package com.arshadshah.nimaz.presentation.screens.about

import androidx.compose.ui.text.AnnotatedString
import com.arshadshah.nimaz.domain.model.LicenseFamily
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * How a licence family is spoken and coloured, and how a search hit is painted.
 *
 * The family vocabulary has to be **total and distinct**. Total because every dependency the app
 * ships lands in one of these six, and a family with no label renders an empty heading over a
 * section of libraries; distinct because the list groups, filters and colours by family, and two
 * families sharing a label or a tone makes a filter chip select a section a reader cannot find.
 *
 * `plainSummary` is the deliberate exception: [LicenseFamily.OTHER] has none. A licence we cannot
 * place we do not paraphrase — a confident one-sentence gloss of an unrecognised licence is worse
 * than saying nothing, and the detail screen is the one place in the app that is about what
 * someone is legally bound by.
 *
 * `highlighted` marks **every** occurrence rather than the first. The list searches name, author
 * and coordinate at once, so a two-word query that lights up only one of its matches reads as a
 * search that half-worked.
 */
@RunWith(RobolectricTestRunner::class)
class LicenseVisualsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `every family carries its own tone`() {
        val tones: Map<LicenseFamily, NimazTone> =
            LicenseFamily.entries.associateWith { it.tone }

        assertThat(tones.values.toSet()).hasSize(LicenseFamily.entries.size)
    }

    @Test
    fun `every family has its own name`() {
        val labels = mutableMapOf<LicenseFamily, String>()
        composeRule.setThemedContent {
            LicenseFamily.entries.forEach { labels[it] = it.label() }
        }
        composeRule.waitForIdle()

        assertThat(labels.keys).containsExactlyElementsIn(LicenseFamily.entries)
        assertThat(labels.values.map { it.trim() }).containsNoDuplicates()
        assertThat(labels.values.none { it.isBlank() }).isTrue()
    }

    @Test
    fun `every family the app can name is glossed, and the one it cannot is not`() {
        val summaries = mutableMapOf<LicenseFamily, String?>()
        composeRule.setThemedContent {
            LicenseFamily.entries.forEach { summaries[it] = it.plainSummary() }
        }
        composeRule.waitForIdle()

        assertThat(summaries[LicenseFamily.OTHER]).isNull()
        LicenseFamily.entries.filter { it != LicenseFamily.OTHER }.forEach { family ->
            assertThat(summaries[family]).isNotNull()
        }
    }

    @Test
    fun `a blank query paints nothing`() {
        val painted = highlight("androidx.compose.ui:ui", query = "   ")

        assertThat(painted.spanStyles).isEmpty()
        assertThat(painted.text).isEqualTo("androidx.compose.ui:ui")
    }

    @Test
    fun `every occurrence of the query is painted, not just the first`() {
        // "compose" appears twice in a Maven coordinate, and a reader scanning a filtered list
        // is looking for exactly that repetition.
        val painted = highlight("androidx.compose.ui:compose-ui", query = "compose")

        assertThat(painted.spanStyles).hasSize(2)
        assertThat(painted.text).isEqualTo("androidx.compose.ui:compose-ui")
    }

    @Test
    fun `the match ignores case, because nobody types a Maven coordinate in caps`() {
        val painted = highlight("Compose UI", query = "compose")

        assertThat(painted.spanStyles).hasSize(1)
        // The original casing survives: it is a highlight, not a rewrite.
        assertThat(painted.text).isEqualTo("Compose UI")
    }

    @Test
    fun `a query that does not occur leaves the text alone`() {
        val painted = highlight("Adhan", query = "compose")

        assertThat(painted.spanStyles).isEmpty()
        assertThat(painted.text).isEqualTo("Adhan")
    }

    private fun highlight(text: String, query: String): AnnotatedString {
        lateinit var result: AnnotatedString
        composeRule.setThemedContent { result = highlighted(text, query) }
        composeRule.waitForIdle()
        return result
    }
}
