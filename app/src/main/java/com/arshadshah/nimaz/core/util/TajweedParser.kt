package com.arshadshah.nimaz.core.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.presentation.theme.NimazColors.TajweedColors
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Parser for pre-parsed tajweed JSON data.
 *
 * Tajweed data is pre-parsed during database generation into a simple JSON format:
 * [{"t":"بِ","r":"g"},{"t":"سْمِ","r":null}]
 *
 * Where:
 * - "t" = text content
 * - "r" = rule code or null for plain text
 *
 * ## V3 rule codes (issue #289)
 *
 * V3 corrects the madd taxonomy: the quran.com source merged Madd Jaiz Munfasil
 * and Madd Wajib Muttasil under one class and mis-labelled Madd 'Aarid. The
 * split is derived by cross-validating with the independent cpfair dataset (see
 * `preparse_tajweed.py`). Beat ("count") values follow the **Hafs 'an 'Asim**
 * reading; reference: Kareema Czerepinski, *Tajweed Rules of the Qur'an*
 * (Parts I–III), and al-Jazari's *al-Muqaddimah al-Jazariyyah*.
 *
 *  Code  | Rule                          | Counts | Colour family
 * -------|-------------------------------|--------|---------------
 *  "g"   | Ghunnah                       | 2      | Green
 *  "if"  | Ikhfa                         | —      | Teal
 *  "is"  | Ikhfa Shafawi                 | —      | Cyan
 *  "dg"  | Idgham with Ghunnah           | —      | Amber
 *  "dn"  | Idgham without Ghunnah        | —      | Brown
 *  "ds"  | Idgham Shafawi                | —      | Amber variant
 *  "dj"  | Idgham Mutajanisayn           | —      | Orange
 *  "dk"  | Idgham Mutaqaribayn           | —      | Orange variant
 *  "qs"  | Qalqalah Sughra (medial)      | —      | Blue
 *  "qk"  | Qalqalah Kubra (stopped)      | —      | Deep Blue
 *  "mn"  | Madd Tabee'i (natural)        | 2      | Rose
 *  "mf"  | Madd Jaiz Munfasil            | 2/4/5  | Pink       (was "mo")
 *  "mt"  | Madd Wajib Muttasil           | 4/5    | Red        (true "mo")
 *  "ma"  | Madd 'Aarid lis-Sukun         | 2/4/6  | Deep Rose  (was "mp")
 *  "ml"  | Madd Lin                      | 2/4/6  | Deep Pink  (populated in #291)
 *  "my"  | Madd Lazim (necessary)        | 6      | Deep Red
 *  "l"   | Iqlab                         | —      | Violet
 *  "ls"  | Lam Shamsiyyah                | —      | Indigo
 *  "sl"  | Silent letters                | —      | Slate
 *  "hw"  | Hamza al-Wasl                 | —      | Light Slate
 *
 * ### Deprecated codes (still parsed for older databases)
 * - v2: `"mo"` → `"mt"`, `"mp"` → `"ma"`, `"q"` → `"qs"`
 * - v1: `"i"` → `"if"`, `"d"` → `"dg"`, `"m"` → `"mn"`, `"s"` → `"sl"`
 */
object TajweedParser {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Canonical description of one tajweed rule — the single source of truth for
     * the code, its display name, a one-line explanation and its light/dark
     * colours. Consumed by [ruleColors] and by the in-app legend (#294).
     */
    data class TajweedRuleInfo(
        val code: String,
        val displayName: String,
        val explanation: String,
        val light: Color,
        val dark: Color,
    ) {
        fun color(isDarkTheme: Boolean): Color = if (isDarkTheme) dark else light
    }

    /**
     * The v3 rule set, in a sensible legend order (nasal/idgham rules, qalqalah,
     * the madd family, then the remaining letter rules).
     */
    val rules: List<TajweedRuleInfo> = listOf(
        TajweedRuleInfo("g", "Ghunnah", "Nasal sound held for 2 counts.",
            TajweedColors.GhunnahLight, TajweedColors.GhunnahDark),
        TajweedRuleInfo("if", "Ikhfa", "Noon sakinah / tanwin hidden with a light nasal sound.",
            TajweedColors.IkhfaLight, TajweedColors.IkhfaDark),
        TajweedRuleInfo("is", "Ikhfa Shafawi", "Meem sakinah hidden with ghunnah before ba.",
            TajweedColors.IkhfaShafawiLight, TajweedColors.IkhfaShafawiDark),
        TajweedRuleInfo("dg", "Idgham with Ghunnah", "Noon sakinah / tanwin merged with nasalisation.",
            TajweedColors.IdghamGhunnahLight, TajweedColors.IdghamGhunnahDark),
        TajweedRuleInfo("dn", "Idgham without Ghunnah", "Noon sakinah / tanwin merged without nasalisation.",
            TajweedColors.IdghamNoGhunnahLight, TajweedColors.IdghamNoGhunnahDark),
        TajweedRuleInfo("ds", "Idgham Shafawi", "Meem sakinah merged into a following meem.",
            TajweedColors.IdghamShafawiLight, TajweedColors.IdghamShafawiDark),
        TajweedRuleInfo("dj", "Idgham Mutajanisayn", "Two letters of the same articulation point merged.",
            TajweedColors.IdghamMutajanisaynLight, TajweedColors.IdghamMutajanisaynDark),
        TajweedRuleInfo("dk", "Idgham Mutaqaribayn", "Two letters of close articulation points merged.",
            TajweedColors.IdghamMutaqaribayLight, TajweedColors.IdghamMutaqaribayDark),
        TajweedRuleInfo("dm", "Idgham Mutamathilayn", "Two identical letters merged, the first silent.",
            TajweedColors.IdghamMutamathilaynLight, TajweedColors.IdghamMutamathilaynDark),
        TajweedRuleInfo("qs", "Qalqalah Sughra", "Light echo on a qalqalah letter in the middle of a word.",
            TajweedColors.QalqalahSughraLight, TajweedColors.QalqalahSughraDark),
        TajweedRuleInfo("qk", "Qalqalah Kubra", "Stronger echo on a qalqalah letter stopped upon.",
            TajweedColors.QalqalahKubraLight, TajweedColors.QalqalahKubraDark),
        TajweedRuleInfo("mn", "Madd Tabee'i", "Natural elongation of 2 counts.",
            TajweedColors.MaddNormalLight, TajweedColors.MaddNormalDark),
        TajweedRuleInfo("mf", "Madd Jaiz Munfasil", "Elongation across a word break — 2, 4 or 5 counts.",
            TajweedColors.MaddMunfasilLight, TajweedColors.MaddMunfasilDark),
        TajweedRuleInfo("mt", "Madd Wajib Muttasil", "Elongation before a hamza in the same word — 4 or 5 counts.",
            TajweedColors.MaddMuttasilLight, TajweedColors.MaddMuttasilDark),
        TajweedRuleInfo("ma", "Madd 'Aarid lis-Sukun", "Elongation before a stop — 2, 4 or 6 counts.",
            TajweedColors.MaddAaridLight, TajweedColors.MaddAaridDark),
        TajweedRuleInfo("ml", "Madd Lin", "Elongation of a layn letter (و/ي) before a stop — 2, 4 or 6 counts.",
            TajweedColors.MaddLinLight, TajweedColors.MaddLinDark),
        TajweedRuleInfo("my", "Madd Lazim", "Obligatory elongation of 6 counts.",
            TajweedColors.MaddNecessaryLight, TajweedColors.MaddNecessaryDark),
        TajweedRuleInfo("l", "Iqlab", "Noon sakinah / tanwin turned into a meem before ba.",
            TajweedColors.IqlabLight, TajweedColors.IqlabDark),
        TajweedRuleInfo("ls", "Lam Shamsiyyah", "The lam of 'al-' assimilated into a sun letter.",
            TajweedColors.LamShamsiyyahLight, TajweedColors.LamShamsiyyahDark),
        TajweedRuleInfo("sl", "Silent", "A letter written but not pronounced.",
            TajweedColors.SilentLight, TajweedColors.SilentDark),
        TajweedRuleInfo("hw", "Hamza al-Wasl", "Connecting hamza, dropped when continuing from before.",
            TajweedColors.HamzaWaslLight, TajweedColors.HamzaWaslDark),
        TajweedRuleInfo("wq", "Waqf sign", "A stop mark: guidance on whether to pause here.",
            TajweedColors.WaqfLight, TajweedColors.WaqfDark),
    )

    /** Lookup of a v3 rule by its code. */
    val ruleInfo: Map<String, TajweedRuleInfo> = rules.associateBy { it.code }

    /** String-annotation tag marking a coloured rule span (for tap-to-explain, #294). */
    const val RULE_TAG = "tajweed_rule"

    /**
     * Resolve a stored rule code (v3, or a legacy v1/v2 code) to its legend
     * entry, or null if the code is unknown/unexplained.
     */
    fun resolveRule(code: String): TajweedRuleInfo? =
        ruleInfo[code] ?: legacyAliases[code]?.let { ruleInfo[it] }

    /**
     * Deprecated code → current code, kept so an older prepackaged DB still
     * renders (no crash, sensible colour). v2 `mo`/`mp`/`q` and v1 single-letter
     * codes map onto their v3 equivalents.
     */
    private val legacyAliases: Map<String, String> = mapOf(
        // v2 → v3
        "mo" to "mt",
        "mp" to "ma",
        "q" to "qs",
        // v1 → v3
        "i" to "if",
        "d" to "dg",
        "m" to "mn",
        "s" to "sl",
    )

    /**
     * Map of rule codes to their light/dark colour pairs — v3 codes plus the
     * deprecated v1/v2 aliases.
     */
    private val ruleColors: Map<String, Pair<Color, Color>> =
        rules.associate { it.code to Pair(it.light, it.dark) } +
            legacyAliases.mapNotNull { (old, current) ->
                ruleInfo[current]?.let { old to Pair(it.light, it.dark) }
            }

    // Malformed-input signatures already reported, so a bad ayah is reported
    // once per process instead of once per composition × ayahs-on-page (#293).
    private val reportedErrors = java.util.Collections.synchronizedSet(HashSet<String>())

    private fun reportOnce(tajweedText: String, e: Exception) {
        if (reportedErrors.add(tajweedText)) {
            CrashReporter.recordException(e)
        }
    }

    /**
     * Parse pre-parsed tajweed JSON and return an AnnotatedString with colored spans.
     *
     * @param tajweedText The pre-parsed JSON string: [{"t":"text","r":"code"},...]
     * @param isDarkTheme Whether the app is in dark theme mode
     * @param defaultColor The default text color for plain text (r=null)
     * @param stripPrefix When non-null, drop this leading text (plus any trailing
     *   whitespace) from the parsed segments — used to strip the bismillah header
     *   from surah-opening ayahs (#293). Matches on the segment text, which since
     *   #290 equals the canonical `text_arabic`.
     * @return AnnotatedString with colored spans for tajweed rules
     */
    fun parse(
        tajweedText: String,
        isDarkTheme: Boolean,
        defaultColor: Color = Color.Unspecified,
        stripPrefix: String? = null,
        annotateRules: Boolean = false,
        underlineRules: Boolean = false
    ): AnnotatedString {
        return try {
            var segments = json.decodeFromString<List<TajweedSegment>>(tajweedText)
            if (stripPrefix != null) {
                segments = stripLeadingPrefix(segments, stripPrefix)
            }
            buildAnnotatedString {
                for (segment in segments) {
                    val startIdx = length
                    append(segment.t)

                    // Apply color if this segment has a rule
                    val ruleCode = segment.r
                    if (ruleCode != null) {
                        val colorPair = ruleColors[ruleCode]
                        val color = if (colorPair != null) {
                            if (isDarkTheme) colorPair.second else colorPair.first
                        } else {
                            // Unknown rule code, use default
                            defaultColor
                        }

                        if (color != Color.Unspecified) {
                            // Colour-blind-friendly mode adds an underline so a rule
                            // is marked by a non-hue channel too, not colour alone
                            // (#294) — critical for the near-neighbour madd/idgham
                            // families that can't be separated by hue.
                            addStyle(
                                style = SpanStyle(
                                    color = color,
                                    textDecoration = if (underlineRules) {
                                        TextDecoration.Underline
                                    } else null
                                ),
                                start = startIdx,
                                end = length
                            )
                        }
                        // Tag the span with its rule so a tap can resolve it to
                        // the legend entry (#294 tap-to-explain). Only for codes
                        // we can explain (resolveRule non-null).
                        if (annotateRules && resolveRule(ruleCode) != null) {
                            addStringAnnotation(
                                tag = RULE_TAG,
                                annotation = ruleCode,
                                start = startIdx,
                                end = length
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            reportOnce(tajweedText, e)
            // If parsing fails, return the raw text without colors
            buildAnnotatedString {
                append(stripJson(tajweedText))
            }
        }
    }

    /**
     * Drop [prefix] (plus any following whitespace) from the front of a segment
     * list, trimming a segment that straddles the boundary. Returns the list
     * unchanged when the concatenated text does not start with [prefix].
     */
    internal fun stripLeadingPrefix(
        segments: List<TajweedSegment>,
        prefix: String
    ): List<TajweedSegment> {
        val full = segments.joinToString("") { it.t }
        if (!full.startsWith(prefix)) return segments
        var cut = prefix.length
        while (cut < full.length && full[cut].isWhitespace()) cut++

        val out = ArrayList<TajweedSegment>(segments.size)
        var pos = 0
        for (seg in segments) {
            val end = pos + seg.t.length
            when {
                end <= cut -> Unit                                    // fully consumed
                pos >= cut -> out.add(seg)                            // fully kept
                else -> out.add(seg.copy(t = seg.t.substring(cut - pos)))  // boundary
            }
            pos = end
        }
        return out
    }

    /**
     * Strip JSON and return plain text.
     * Useful for fallback or when tajweed display is disabled.
     *
     * @param tajweedText The pre-parsed JSON string
     * @return Plain text without tajweed markup
     */
    fun stripTags(tajweedText: String): String {
        return try {
            val segments = json.decodeFromString<List<TajweedSegment>>(tajweedText)
            segments.joinToString("") { it.t }
        } catch (e: Exception) {
            reportOnce(tajweedText, e)
            stripJson(tajweedText)
        }
    }

    /**
     * Check if text is in the pre-parsed JSON format.
     *
     * @param text The text to check
     * @return true if the text appears to be pre-parsed JSON
     */
    fun hasTajweedMarkup(text: String): Boolean {
        return text.startsWith("[") && text.contains("\"t\":")
    }

    /**
     * Fallback to extract text from malformed JSON.
     */
    private fun stripJson(text: String): String {
        // Simple regex to extract "t" values from JSON
        val pattern = Regex(""""t"\s*:\s*"([^"]+)"""")
        val matches = pattern.findAll(text)
        return if (matches.any()) {
            matches.joinToString("") { it.groupValues[1] }
        } else {
            // Not JSON, return as-is
            text
        }
    }
}

/**
 * Data class for a tajweed text segment.
 *
 * @property t The text content
 * @property r The rule code or null for plain text
 */
@Serializable
data class TajweedSegment(
    val t: String,
    val r: String? = null
)
