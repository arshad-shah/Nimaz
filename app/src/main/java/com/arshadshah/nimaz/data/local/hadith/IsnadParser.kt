package com.arshadshah.nimaz.data.local.hadith

/**
 * Derives a hadith's chain of narration (isnād) from its embedded Arabic text.
 *
 * Classical hadith open with the transmission chain — "X narrated to us, Y
 * narrated to us, from Z, … from [Companion]" — before the matn (the actual
 * content). This extracts that chain as a newline-separated list of narrators,
 * which the reader renders as a timeline (it splits the string on `\n` / ` عن `).
 *
 * It is a best-effort heuristic over real, diacritised text, so it deliberately
 * errs toward returning `null` rather than emitting a garbled chain. A curated
 * chain shipped via `hadith_fills.json` always takes precedence over this.
 */
object IsnadParser {

    // Diacritics (tashkeel), Quranic annotation marks, and tatweel.
    private val tashkeel = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0640]")

    // Bidi control marks that show up in copied hadith text.
    private val bidiMarks = Regex("[\\u200E\\u200F]")

    // Honorifics that trail narrator names; stripped so each node is just a name.
    private val honorifics = listOf(
        "رضى الله عنهما", "رضي الله عنهما",
        "رضى الله عنهم", "رضي الله عنهم",
        "رضى الله عنها", "رضي الله عنها",
        "رضى الله عنه", "رضي الله عنه",
        "صلى الله عليه وسلم",
        "عليه الصلاة والسلام",
        "عليه السلام",
    )

    // Markers that signal the matn (content) has begun — the chain ends here. The
    // final companion (introduced by "عن …") stays in the chain because the cut
    // happens at the Prophet's mention, which follows the companion.
    private val matnMarkers = listOf(
        "ان رسول الله", "أن رسول الله",
        "قال رسول الله",
        "ان النبي", "أن النبي", "ان النبى",
        "قال النبي", "قال النبى",
        "سمعت رسول الله",
    )

    // Transmission verbs that link narrators. Matched space-delimited so they
    // never fire inside a name (e.g. the "عن" inside "عنه").
    private val connectors = Regex(
        "\\s(?:حدثناه|حدثنا|حدثني|حدثهم|اخبرناه|اخبرنا|اخبرني|انبانا|انبأنا|سمعت|ثنا|وحدثنا|وعن|عن)\\s"
    )

    fun parse(arabic: String?): String? {
        if (arabic.isNullOrBlank()) return null

        // Normalise: drop diacritics/tatweel/bidi marks, turn the "ـ" dash
        // separators into spaces, and collapse whitespace.
        var text = arabic.replace(tashkeel, "").replace(bidiMarks, "")
        text = text.replace("ـ", " ").replace(Regex("\\s+"), " ").trim()
        if (text.isEmpty()) return null

        // Keep only the chain portion, up to the first matn marker.
        val cut = matnMarkers.map { text.indexOf(it) }.filter { it >= 0 }.minOrNull()
        val isnad = if (cut != null && cut > 0) text.substring(0, cut) else text

        // Pad so a connector at position 0 is still space-delimited.
        val narrators = " $isnad ".split(connectors)
            .map { cleanNode(it) }
            .filter { node -> node.length in 3..40 && node.any { it.isLetter() } }

        // Drop consecutive duplicates (a name repeated across two links).
        val deduped = narrators.filterIndexed { i, n -> i == 0 || n != narrators[i - 1] }

        // A single node is just the narrator already shown in the chip — only a
        // genuine multi-link chain is worth a timeline.
        if (deduped.size < 2) return null
        return deduped.joinToString("\n")
    }

    private fun cleanNode(raw: String): String {
        var s = raw.trim()
        honorifics.forEach { s = s.replace(it, "") }
        s = s.removePrefix("و").trim()
        s = s.removeSuffix("قال").trim()
        return s.trim('،', '.', '"', '«', '»', ':', '(', ')', ' ', '؛')
            .trim()
    }
}
