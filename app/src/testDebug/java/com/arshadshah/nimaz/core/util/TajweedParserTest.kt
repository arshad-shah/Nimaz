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
}
