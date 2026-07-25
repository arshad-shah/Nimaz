package com.arshadshah.nimaz.core.util

import com.arshadshah.nimaz.presentation.components.atoms.BISMILLAH_TEXT
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TajweedParserTest {

    private fun seg(t: String, r: String? = null) = TajweedSegment(t, r)

    @Test
    fun `stripLeadingPrefix drops the bismillah and following space`() {
        // bismillah split across a coloured and a plain segment, then the ayah body
        val hamzaLen = 1
        val bismillahHead = BISMILLAH_TEXT.substring(0, BISMILLAH_TEXT.length - hamzaLen)
        val bismillahTail = BISMILLAH_TEXT.substring(BISMILLAH_TEXT.length - hamzaLen)
        val segments = listOf(
            seg(bismillahHead),
            seg(bismillahTail, "mn"),
            seg(" الم", null),
        )
        val out = TajweedParser.stripLeadingPrefix(segments, BISMILLAH_TEXT)
        assertThat(out.joinToString("") { it.t }).isEqualTo("الم")
    }

    @Test
    fun `stripLeadingPrefix trims a segment that straddles the boundary`() {
        val segments = listOf(seg("$BISMILLAH_TEXT الم", "mn"))
        val out = TajweedParser.stripLeadingPrefix(segments, BISMILLAH_TEXT)
        assertThat(out.joinToString("") { it.t }).isEqualTo("الم")
        assertThat(out.single().r).isEqualTo("mn")  // rule preserved on the tail
    }

    @Test
    fun `stripLeadingPrefix leaves segments unchanged when prefix is absent`() {
        val segments = listOf(seg("الم", "mn"))
        val out = TajweedParser.stripLeadingPrefix(segments, BISMILLAH_TEXT)
        assertThat(out).isEqualTo(segments)
    }

    @Test
    fun `parse with stripPrefix removes the bismillah from the rendered text`() {
        val json = """[{"t":"$BISMILLAH_TEXT الم","r":null}]"""
        val withStrip = TajweedParser.parse(json, isDarkTheme = false, stripPrefix = BISMILLAH_TEXT)
        val withoutStrip = TajweedParser.parse(json, isDarkTheme = false)
        assertThat(withStrip.text).isEqualTo("الم")
        assertThat(withoutStrip.text).isEqualTo("$BISMILLAH_TEXT الم")
    }

    @Test
    fun `parse falls back to plain text on malformed json`() {
        val result = TajweedParser.parse("not json", isDarkTheme = false)
        assertThat(result.text).isEqualTo("not json")
    }

    @Test
    fun `parse preserves the full text across mixed segments`() {
        val json = """[{"t":"بِ","r":"g"},{"t":"سْمِ","r":null},{"t":"ي","r":"mn"}]"""
        assertThat(TajweedParser.parse(json, isDarkTheme = false).text).isEqualTo("بِسْمِي")
    }

    @Test
    fun `parse applies a span style for a known v3 code`() {
        val json = """[{"t":"ن","r":"g"}]"""
        val result = TajweedParser.parse(json, isDarkTheme = false)
        // exactly one coloured span covering the whole segment
        assertThat(result.spanStyles).hasSize(1)
        assertThat(result.spanStyles[0].start).isEqualTo(0)
        assertThat(result.spanStyles[0].end).isEqualTo(1)
    }

    @Test
    fun `parse light and dark colours differ for a rule`() {
        val json = """[{"t":"ن","r":"g"}]"""
        val light = TajweedParser.parse(json, isDarkTheme = false).spanStyles[0].item.color
        val dark = TajweedParser.parse(json, isDarkTheme = true).spanStyles[0].item.color
        assertThat(light).isNotEqualTo(dark)
    }

    @Test
    fun `parse accepts legacy v2 and v1 codes without crashing`() {
        // mo→mt, mp→ma, q→qs (v2) and i→if, d→dg, m→mn, s→sl (v1) all resolve.
        val json = """[{"t":"a","r":"mo"},{"t":"b","r":"mp"},{"t":"c","r":"q"},
            {"t":"d","r":"i"},{"t":"e","r":"d"},{"t":"f","r":"m"},{"t":"g","r":"s"}]"""
        val result = TajweedParser.parse(json, isDarkTheme = false)
        assertThat(result.text).isEqualTo("abcdefg")
        assertThat(result.spanStyles).hasSize(7)  // every legacy code coloured
    }

    @Test
    fun `parse leaves an unknown code uncoloured but keeps the text`() {
        val json = """[{"t":"xyz","r":"totally_unknown"}]"""
        val result = TajweedParser.parse(json, isDarkTheme = false)
        assertThat(result.text).isEqualTo("xyz")
        assertThat(result.spanStyles).isEmpty()  // no colour, text preserved
    }

    @Test
    fun `parse handles empty list and null-rule single segment`() {
        assertThat(TajweedParser.parse("[]", isDarkTheme = false).text).isEqualTo("")
        val single = TajweedParser.parse("""[{"t":"نص","r":null}]""", isDarkTheme = false)
        assertThat(single.text).isEqualTo("نص")
        assertThat(single.spanStyles).isEmpty()
    }

    @Test
    fun `stripTags returns the concatenated text`() {
        val json = """[{"t":"بِ","r":"g"},{"t":"سْمِ","r":null}]"""
        assertThat(TajweedParser.stripTags(json)).isEqualTo("بِسْمِ")
    }

    @Test
    fun `stripTags falls back for malformed json`() {
        assertThat(TajweedParser.stripTags("""[{"t":"abc"}""")).isEqualTo("abc")
    }

    @Test
    fun `hasTajweedMarkup detects the pre-parsed json format`() {
        assertThat(TajweedParser.hasTajweedMarkup("""[{"t":"a","r":"g"}]""")).isTrue()
        assertThat(TajweedParser.hasTajweedMarkup("just arabic")).isFalse()
        // a plain ayah that merely starts with '[' is not tajweed markup
        assertThat(TajweedParser.hasTajweedMarkup("[not tajweed]")).isFalse()
    }

    @Test
    fun `underlineRules adds an underline decoration to coloured spans`() {
        val json = """[{"t":"ن","r":"g"}]"""
        val plain = TajweedParser.parse(json, isDarkTheme = false)
        val underlined = TajweedParser.parse(json, isDarkTheme = false, underlineRules = true)
        assertThat(plain.spanStyles[0].item.textDecoration).isNull()
        assertThat(underlined.spanStyles[0].item.textDecoration)
            .isEqualTo(androidx.compose.ui.text.style.TextDecoration.Underline)
    }

    @Test
    fun `every v3 rule code resolves to a colour`() {
        val codes = listOf("g", "if", "is", "dg", "dn", "ds", "dj", "dk", "dm",
            "qs", "qk", "mn", "mf", "mt", "ma", "ml", "my", "l", "ls", "sl", "hw", "wq")
        for (code in codes) {
            val json = """[{"t":"x","r":"$code"}]"""
            val result = TajweedParser.parse(json, isDarkTheme = false)
            assertThat(result.spanStyles).hasSize(1)
        }
        // and each code has legend info
        for (code in codes) {
            assertThat(TajweedParser.ruleInfo[code]).isNotNull()
        }
    }
}
