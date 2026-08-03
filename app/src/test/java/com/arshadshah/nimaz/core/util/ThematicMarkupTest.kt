package com.arshadshah.nimaz.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The scanner over the thematic layer's four-tag dialect.
 *
 * The cases that matter are the ones a device can actually be handed: the dialect as the corpus
 * emits it, and everything the scanner has to survive *outside* it — because a mis-parsed
 * paragraph must degrade to plain text, never to a screen that will not open.
 */
class ThematicMarkupTest {

    @Test
    fun `splits on paragraphs`() {
        val paragraphs = ThematicMarkup.parse("<p>One.</p><p>Two.</p>")

        assertEquals(2, paragraphs.size)
        assertEquals("One.", paragraphs[0].text)
        assertEquals("Two.", paragraphs[1].text)
    }

    @Test
    fun `carries bold and italic through as spans`() {
        val spans = ThematicMarkup
            .parse("<p>A <strong>bold</strong> and <em>italic</em> word.</p>")
            .single()
            .spans

        assertEquals("bold", spans.single { it.bold }.text)
        assertEquals("italic", spans.single { it.italic }.text)
    }

    @Test
    fun `resolves a verse range link`() {
        val span = ThematicMarkup
            .parse("""<p>See <a href="quran:2:153-251">these verses</a>.</p>""")
            .single()
            .spans
            .single { it.link != null }

        assertEquals("these verses", span.text)
        assertEquals(ThematicLink.Verses(2, 153, 251), span.link)
    }

    @Test
    fun `a single-verse link reports the same start and end`() {
        assertEquals(ThematicLink.Verses(2, 255, 255), ThematicMarkup.linkOf("quran:2:255"))
    }

    @Test
    fun `a whole-surah link has no verses`() {
        assertEquals(ThematicLink.Verses(2, null, null), ThematicMarkup.linkOf("quran:2"))
    }

    @Test
    fun `resolves a topic link`() {
        assertEquals(ThematicLink.Topic(61), ThematicMarkup.linkOf("topic:61"))
    }

    @Test
    fun `refuses a surah that does not exist`() {
        assertNull(ThematicMarkup.linkOf("quran:115"))
        assertNull(ThematicMarkup.linkOf("quran:0"))
    }

    @Test
    fun `refuses a scheme the app has no screen for`() {
        assertNull(ThematicMarkup.linkOf("https://example.com"))
        assertNull(ThematicMarkup.linkOf("hadith:1:2"))
    }

    /**
     * The corpus cannot contain one — `thematic.sections-dialect` blocks the build — but a
     * future corpus could, and the failure mode has to be a missing style rather than markup
     * printed in the middle of a sentence.
     */
    @Test
    fun `an unknown tag loses its markup and keeps its text`() {
        val paragraph = ThematicMarkup.parse("<p>A <span class=\"ar\">word</span> here.</p>").single()

        assertEquals("A word here.", paragraph.text)
        assertTrue(paragraph.spans.none { it.bold || it.italic })
    }

    @Test
    fun `an anchor with no resolvable target keeps its text and loses its link`() {
        val paragraph = ThematicMarkup
            .parse("""<p>See <a href="quran:900">nothing</a>.</p>""")
            .single()

        assertEquals("See nothing.", paragraph.text)
        assertTrue(paragraph.spans.all { it.link == null })
    }

    @Test
    fun `unclosed tags do not run off the end`() {
        val paragraph = ThematicMarkup.parse("<p>Still <strong>readable").single()

        assertEquals("Still readable", paragraph.text)
    }

    @Test
    fun `text outside a paragraph is still a paragraph`() {
        assertEquals("Loose text.", ThematicMarkup.parse("Loose text.").single().text)
    }

    @Test
    fun `blank and whitespace-only input produce nothing`() {
        assertTrue(ThematicMarkup.parse("").isEmpty())
        assertTrue(ThematicMarkup.parse("   ").isEmpty())
        assertTrue(ThematicMarkup.parse("<p></p><p>  </p>").isEmpty())
    }

    @Test
    fun `decodes the entities the source escapes`() {
        assertEquals("Ibrahim &amp; Musa", ThematicMarkup.parse("<p>Ibrahim &amp;amp; Musa</p>").single().text)
        assertEquals("a < b", ThematicMarkup.parse("<p>a &lt; b</p>").single().text)
    }

    @Test
    fun `plain flattens paragraphs for a preview line`() {
        assertEquals(
            "One.\n\nTwo.",
            ThematicMarkup.plain("<p>One.</p><p><strong>Two.</strong></p>"),
        )
    }

    /**
     * Two links in one paragraph must stay two destinations. They are rendered with a tag
     * derived from the link, and a shared tag would make the second open the first.
     */
    @Test
    fun `two links in one paragraph resolve independently`() {
        val links = ThematicMarkup
            .parse(
                """<p>Compare <a href="quran:2:1-5">these</a> with """ +
                    """<a href="quran:9:1">those</a>.</p>"""
            )
            .single()
            .spans
            .mapNotNull { it.link }

        assertEquals(
            listOf(ThematicLink.Verses(2, 1, 5), ThematicLink.Verses(9, 1, 1)),
            links,
        )
    }
}
