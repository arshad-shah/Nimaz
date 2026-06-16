package com.arshadshah.nimaz.core.util

import android.app.Application
import androidx.compose.ui.graphics.Color
import com.arshadshah.nimaz.presentation.theme.NimazColors.TajweedColors
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [TajweedParser] — turns pre-baked tajweed JSON into a colored
 * [androidx.compose.ui.text.AnnotatedString]. Covers plain-text extraction,
 * markup detection, rule-to-color mapping (light vs dark), the legacy v1 rule
 * codes, and the graceful fallback for malformed input.
 *
 * Runs under Robolectric so the Compose `Color` / `AnnotatedString` types
 * resolve, matching the convention used by the other Compose-touching tests.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class TajweedParserTest {

    private val sampleJson =
        """[{"t":"بِ","r":"g"},{"t":"سْمِ","r":null}]"""

    // ── hasTajweedMarkup ────────────────────────────────────────────

    @Test
    fun `hasTajweedMarkup recognizes pre-parsed json`() {
        assertThat(TajweedParser.hasTajweedMarkup(sampleJson)).isTrue()
    }

    @Test
    fun `hasTajweedMarkup rejects plain text`() {
        assertThat(TajweedParser.hasTajweedMarkup("بِسْمِ اللَّهِ")).isFalse()
        assertThat(TajweedParser.hasTajweedMarkup("")).isFalse()
    }

    // ── stripTags ───────────────────────────────────────────────────

    @Test
    fun `stripTags concatenates the text of all segments`() {
        assertThat(TajweedParser.stripTags(sampleJson)).isEqualTo("بِسْمِ")
    }

    @Test
    fun `stripTags falls back to regex extraction for malformed json`() {
        val malformed = """[{"t":"بِ","r":"g"},{"t":"سْمِ"""" // truncated / invalid
        assertThat(TajweedParser.stripTags(malformed)).isEqualTo("بِسْمِ")
    }

    @Test
    fun `stripTags returns non-json text unchanged`() {
        assertThat(TajweedParser.stripTags("بِسْمِ اللَّهِ")).isEqualTo("بِسْمِ اللَّهِ")
    }

    // ── parse: text content ─────────────────────────────────────────

    @Test
    fun `parse preserves the full concatenated text`() {
        val annotated = TajweedParser.parse(sampleJson, isDarkTheme = false)
        assertThat(annotated.text).isEqualTo("بِسْمِ")
    }

    @Test
    fun `parse falls back to plain text for malformed json`() {
        val malformed = """not really json"""
        val annotated = TajweedParser.parse(malformed, isDarkTheme = false)
        assertThat(annotated.text).isEqualTo("not really json")
        assertThat(annotated.spanStyles).isEmpty()
    }

    // ── parse: coloring ─────────────────────────────────────────────

    @Test
    fun `parse applies a colored span only to segments with a rule`() {
        val annotated = TajweedParser.parse(sampleJson, isDarkTheme = false)
        // Only the first segment ("بِ", rule "g") should be styled.
        assertThat(annotated.spanStyles).hasSize(1)
        val span = annotated.spanStyles.first()
        assertThat(span.start).isEqualTo(0)
        assertThat(span.end).isEqualTo("بِ".length)
        assertThat(span.item.color).isEqualTo(TajweedColors.GhunnahLight)
    }

    @Test
    fun `parse uses dark-theme colors when dark theme is enabled`() {
        val annotated = TajweedParser.parse(sampleJson, isDarkTheme = true)
        assertThat(annotated.spanStyles.first().item.color)
            .isEqualTo(TajweedColors.GhunnahDark)
    }

    @Test
    fun `parse maps legacy v1 rule codes to colors`() {
        // "m" is the legacy code for Madd Normal.
        val legacy = """[{"t":"اللَّه","r":"m"}]"""
        val annotated = TajweedParser.parse(legacy, isDarkTheme = false)
        assertThat(annotated.spanStyles).hasSize(1)
        assertThat(annotated.spanStyles.first().item.color)
            .isEqualTo(TajweedColors.MaddNormalLight)
    }

    @Test
    fun `parse leaves unknown rule codes unstyled when default color is unspecified`() {
        val unknown = """[{"t":"x","r":"zzz"}]"""
        val annotated = TajweedParser.parse(
            unknown, isDarkTheme = false, defaultColor = Color.Unspecified
        )
        assertThat(annotated.text).isEqualTo("x")
        assertThat(annotated.spanStyles).isEmpty()
    }
}
