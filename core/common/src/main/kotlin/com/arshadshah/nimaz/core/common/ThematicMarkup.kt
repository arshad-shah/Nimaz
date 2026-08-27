package com.arshadshah.nimaz.core.common

/**
 * The four-tag markup the thematic layer arrives in, parsed into something Compose can draw.
 *
 * The content artifact normalises three very different upstream dialects onto one small one
 * (arshad-shah/nimaz-data, `data/importers/quran_thematic.py`), and a build-time rule
 * (`thematic.sections-dialect`) refuses to ship anything outside it:
 *
 * ```
 * <p>…</p>   <strong>…</strong>   <em>…</em>
 * <a href="quran:2:153-251">…</a>   <a href="topic:61">…</a>
 * ```
 *
 * That is the whole grammar, which is why this is a hand-written scanner rather than an HTML
 * parser. It is also why the scanner is *lenient*: an unknown tag is dropped and its text kept,
 * so the worst a fifth tag can do on a device is lose its styling. Nothing here throws — the
 * alternative to a mis-styled paragraph must never be a screen that will not open.
 *
 * The links are the point. 446 of them are cross-references the source writes as prose ("see
 * 2:153-251") and 509 are cross-links between topics; both address screens this app has, so
 * they resolve to [ThematicLink] and become taps rather than dead text.
 */
object ThematicMarkup {

    /**
     * *Any* tag, not only the four. A tag outside the dialect has to be consumed here so its
     * markup is dropped and its text kept — matching only the known ones would leave
     * `<span class="ar">` printed literally in the middle of a sentence.
     */
    private val TAG = Regex("<[^>]*>")
    private val KNOWN = Regex("^<(/?)(p|strong|em|a)(\\s[^>]*)?>$", RegexOption.IGNORE_CASE)
    private val HREF = Regex("href\\s*=\\s*\"([^\"]*)\"", RegexOption.IGNORE_CASE)

    /**
     * Split into paragraphs, each a list of styled runs.
     *
     * Text outside any `<p>` is not dropped: the dialect says paragraphs are the only block
     * element, but a source that forgot one should still be readable, so loose text becomes a
     * paragraph of its own.
     */
    fun parse(html: String): List<ThematicParagraph> {
        if (html.isBlank()) return emptyList()

        val paragraphs = mutableListOf<ThematicParagraph>()
        val spans = mutableListOf<ThematicSpan>()
        var bold = 0
        var italic = 0
        var link: ThematicLink? = null
        var cursor = 0

        fun flushText(upTo: Int) {
            if (upTo <= cursor) return
            val text = unescape(html.substring(cursor, upTo))
            if (text.isEmpty()) return
            spans += ThematicSpan(
                text = text,
                bold = bold > 0,
                italic = italic > 0,
                link = link,
            )
        }

        fun flushParagraph() {
            val text = spans.joinToString("") { it.text }
            if (text.isNotBlank()) paragraphs += ThematicParagraph(spans.toList())
            spans.clear()
        }

        for (match in TAG.findAll(html)) {
            flushText(match.range.first)
            cursor = match.range.last + 1

            val known = KNOWN.find(match.value) ?: continue
            val closing = known.groupValues[1] == "/"
            when (known.groupValues[2].lowercase()) {
                "p" -> flushParagraph()
                "strong" -> if (closing) bold = (bold - 1).coerceAtLeast(0) else bold++
                "em" -> if (closing) italic = (italic - 1).coerceAtLeast(0) else italic++
                "a" -> link = if (closing) {
                    null
                } else {
                    HREF.find(known.groupValues[3])?.groupValues?.get(1)?.let(::linkOf)
                }
            }
        }
        flushText(html.length)
        flushParagraph()
        return paragraphs
    }

    /** Plain text — for a preview line, a content description, or anything measuring length. */
    fun plain(html: String): String =
        parse(html).joinToString("\n\n") { paragraph ->
            paragraph.spans.joinToString("") { it.text }.trim()
        }

    /**
     * `quran:2`, `quran:2:255`, `quran:2:153-251`, `topic:61` — and null for anything else.
     *
     * Malformed hrefs cannot reach a device (`thematic.links-resolve` blocks the build on one),
     * so returning null here is about a *future* corpus rather than this one: an unrecognised
     * scheme becomes unstyled text instead of a tap that goes nowhere.
     */
    fun linkOf(href: String): ThematicLink? = when {
        href.startsWith(TOPIC_SCHEME) ->
            href.removePrefix(TOPIC_SCHEME).toIntOrNull()?.let(ThematicLink::Topic)

        href.startsWith(QURAN_SCHEME) -> {
            val parts = href.removePrefix(QURAN_SCHEME).split(':')
            val surah = parts.getOrNull(0)?.toIntOrNull()
            when {
                surah == null || surah !in 1..114 -> null
                parts.size == 1 -> ThematicLink.Verses(surah, null, null)
                else -> {
                    val start = parts[1].substringBefore('-').toIntOrNull()
                    val end = parts[1].substringAfter('-', "").toIntOrNull()
                    if (start == null) null else ThematicLink.Verses(surah, start, end ?: start)
                }
            }
        }

        else -> null
    }

    /**
     * The five XML entities, and nothing more.
     *
     * The corpus is NFC-normalised text with entities already decoded on import; these survive
     * only where the source escaped a literal `&` or angle bracket, which it does in a handful
     * of places. A general entity table would be code for a case that does not occur.
     */
    private fun unescape(raw: String): String =
        if ('&' !in raw) raw else raw
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&amp;", "&")

    private const val QURAN_SCHEME = "quran:"
    private const val TOPIC_SCHEME = "topic:"
}

/** One paragraph of thematic prose. */
data class ThematicParagraph(val spans: List<ThematicSpan>) {
    val text: String get() = spans.joinToString("") { it.text }
}

/** A run of text with uniform styling, and at most one link. */
data class ThematicSpan(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val link: ThematicLink? = null,
)

/** A destination inside the app that a run of prose points at. */
sealed interface ThematicLink {
    /**
     * A verse or a range of them. [from] is null for a whole-surah reference; [to] equals
     * [from] for a single verse, so a caller never has to special-case a one-verse range.
     */
    data class Verses(val surah: Int, val from: Int?, val to: Int?) : ThematicLink

    data class Topic(val id: Int) : ThematicLink
}
