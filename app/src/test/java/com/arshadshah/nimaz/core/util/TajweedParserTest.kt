package com.arshadshah.nimaz.core.util

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for [TajweedParser] — decoding the pre-parsed tajweed JSON into
 * plain text and colored spans. Covers the happy path, legacy v1 rule codes,
 * unknown codes, and the malformed-input fallback so the Quran reader never
 * crashes on bad data.
 */
class TajweedParserTest {

    private val bismillah = """[{"t":"بِ","r":"g"},{"t":"سْمِ","r":null}]"""

    // ── hasTajweedMarkup ────────────────────────────────────────────────────

    @Test
    fun `hasTajweedMarkup detects the pre-parsed JSON shape`() {
        assertThat(TajweedParser.hasTajweedMarkup(bismillah)).isTrue()
    }

    @Test
    fun `hasTajweedMarkup is false for plain arabic text`() {
        assertThat(TajweedParser.hasTajweedMarkup("بِسْمِ اللَّهِ")).isFalse()
    }

    @Test
    fun `hasTajweedMarkup requires both a leading bracket and a t key`() {
        assertThat(TajweedParser.hasTajweedMarkup("""{"t":"a"}""")).isFalse() // no leading [
        assertThat(TajweedParser.hasTajweedMarkup("""["x","y"]""")).isFalse() // no "t":
    }

    // ── stripTags ───────────────────────────────────────────────────────────

    @Test
    fun `stripTags concatenates the text of every segment`() {
        assertThat(TajweedParser.stripTags(bismillah)).isEqualTo("بِسْمِ")
    }

    @Test
    fun `stripTags falls back to regex extraction for malformed JSON`() {
        // Truncated/invalid JSON still yields the text values via the fallback.
        val malformed = """[{"t":"بِ","r":"g"},{"t":"سْمِ","""
        assertThat(TajweedParser.stripTags(malformed)).isEqualTo("بِسْمِ")
    }

    @Test
    fun `stripTags returns non-JSON input unchanged`() {
        assertThat(TajweedParser.stripTags("just plain text")).isEqualTo("just plain text")
    }

    // ── parse: text content ─────────────────────────────────────────────────

    @Test
    fun `parse produces an AnnotatedString with the concatenated text`() {
        val result = TajweedParser.parse(bismillah, isDarkTheme = false)
        assertThat(result.text).isEqualTo("بِسْمِ")
    }

    @Test
    fun `parse falls back to plain text when the JSON is malformed`() {
        val malformed = """[{"t":"بِ","r":"g"},{"t":"سْمِ","""
        val result = TajweedParser.parse(malformed, isDarkTheme = false)
        assertThat(result.text).isEqualTo("بِسْمِ")
        assertThat(result.spanStyles).isEmpty()
    }

    // ── parse: colored spans ────────────────────────────────────────────────

    @Test
    fun `parse adds a colored span only for segments carrying a known rule`() {
        // One segment has rule "g" (known), the other is plain (r=null).
        val result = TajweedParser.parse(bismillah, isDarkTheme = false)
        assertThat(result.spanStyles).hasSize(1)
    }

    @Test
    fun `parse colors every segment that has a known rule code`() {
        val json = """[{"t":"a","r":"g"},{"t":"b","r":"if"},{"t":"c","r":"q"}]"""
        val result = TajweedParser.parse(json, isDarkTheme = false)
        assertThat(result.text).isEqualTo("abc")
        assertThat(result.spanStyles).hasSize(3)
    }

    @Test
    fun `parse accepts legacy v1 single-letter rule codes`() {
        // "i" is a legacy alias for Ikhfa and must still produce a span.
        val json = """[{"t":"a","r":"i"}]"""
        val result = TajweedParser.parse(json, isDarkTheme = false)
        assertThat(result.spanStyles).hasSize(1)
    }

    @Test
    fun `parse skips coloring for unknown rule codes`() {
        // Unknown code + default Unspecified color => no span is added.
        val json = """[{"t":"a","r":"zzz"}]"""
        val result = TajweedParser.parse(json, isDarkTheme = false)
        assertThat(result.text).isEqualTo("a")
        assertThat(result.spanStyles).isEmpty()
    }

    @Test
    fun `parse uses a span for unknown codes when an explicit default color is given`() {
        // With a concrete (non-Unspecified) default color, even unknown codes
        // get styled with that default.
        val json = """[{"t":"a","r":"zzz"}]"""
        val result = TajweedParser.parse(json, isDarkTheme = false, defaultColor = Color.Black)
        assertThat(result.spanStyles).hasSize(1)
    }

    @Test
    fun `parse light and dark themes pick different span colors`() {
        val json = """[{"t":"a","r":"g"}]"""
        val light = TajweedParser.parse(json, isDarkTheme = false)
        val dark = TajweedParser.parse(json, isDarkTheme = true)
        assertThat(light.spanStyles).hasSize(1)
        assertThat(dark.spanStyles).hasSize(1)
        // The Ghunnah rule has distinct light/dark colors.
        assertThat(light.spanStyles.first().item.color)
            .isNotEqualTo(dark.spanStyles.first().item.color)
    }
}
