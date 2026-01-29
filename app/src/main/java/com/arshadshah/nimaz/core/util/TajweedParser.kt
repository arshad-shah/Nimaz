package com.arshadshah.nimaz.core.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.arshadshah.nimaz.presentation.theme.NimazColors.TajweedColors

/**
 * Parser for tajweed-annotated Quran text.
 *
 * Tajweed text from Quran.com API uses HTML-like tags:
 * `<tajweed class="ghunnah">نّ</tajweed>`
 *
 * This parser converts those tags into Compose AnnotatedString with appropriate colors.
 */
object TajweedParser {

    private val tagPattern = Regex("""<tajweed\s+class="([^"]+)">(.*?)</tajweed>""")

    /**
     * Map of tajweed rule class names to their light/dark color pairs.
     * The Quran.com API uses various class names for different tajweed rules.
     */
    private val ruleColors: Map<String, Pair<Color, Color>> = mapOf(
        // Ghunnah (nasalization) - Green
        "ghunnah" to Pair(TajweedColors.GhunnahLight, TajweedColors.GhunnahDark),
        "ghn" to Pair(TajweedColors.GhunnahLight, TajweedColors.GhunnahDark),

        // Ikhfa (hiding) - Purple
        "ikhfa" to Pair(TajweedColors.IkhfaLight, TajweedColors.IkhfaDark),
        "ikhfa_shafawi" to Pair(TajweedColors.IkhfaLight, TajweedColors.IkhfaDark),
        "ikhf" to Pair(TajweedColors.IkhfaLight, TajweedColors.IkhfaDark),
        "ikhf_shfw" to Pair(TajweedColors.IkhfaLight, TajweedColors.IkhfaDark),

        // Idgham (merging) - Amber/Orange
        "idgham" to Pair(TajweedColors.IdghamLight, TajweedColors.IdghamDark),
        "idgham_ghunnah" to Pair(TajweedColors.IdghamLight, TajweedColors.IdghamDark),
        "idgham_no_ghunnah" to Pair(TajweedColors.IdghamLight, TajweedColors.IdghamDark),
        "idgham_shafawi" to Pair(TajweedColors.IdghamLight, TajweedColors.IdghamDark),
        "idgham_mutajanisayn" to Pair(TajweedColors.IdghamLight, TajweedColors.IdghamDark),
        "idgham_mutaqaribayn" to Pair(TajweedColors.IdghamLight, TajweedColors.IdghamDark),
        "idgh_ghn" to Pair(TajweedColors.IdghamLight, TajweedColors.IdghamDark),
        "idgh_w_ghn" to Pair(TajweedColors.IdghamLight, TajweedColors.IdghamDark),
        "idgh_wo_ghn" to Pair(TajweedColors.IdghamLight, TajweedColors.IdghamDark),
        "idgh_shfw" to Pair(TajweedColors.IdghamLight, TajweedColors.IdghamDark),
        "idgh_mus" to Pair(TajweedColors.IdghamLight, TajweedColors.IdghamDark),

        // Qalqalah (echoing) - Blue
        "qalqalah" to Pair(TajweedColors.QalqalahLight, TajweedColors.QalqalahDark),
        "qlq" to Pair(TajweedColors.QalqalahLight, TajweedColors.QalqalahDark),

        // Madd (elongation) - Red
        "madd" to Pair(TajweedColors.MaddLight, TajweedColors.MaddDark),
        "madd_normal" to Pair(TajweedColors.MaddLight, TajweedColors.MaddDark),
        "madd_permissible" to Pair(TajweedColors.MaddLight, TajweedColors.MaddDark),
        "madd_necessary" to Pair(TajweedColors.MaddLight, TajweedColors.MaddDark),
        "madd_obligatory" to Pair(TajweedColors.MaddLight, TajweedColors.MaddDark),
        "madd_6" to Pair(TajweedColors.MaddLight, TajweedColors.MaddDark),
        "madd_2" to Pair(TajweedColors.MaddLight, TajweedColors.MaddDark),
        "madd_246" to Pair(TajweedColors.MaddLight, TajweedColors.MaddDark),
        "mad" to Pair(TajweedColors.MaddLight, TajweedColors.MaddDark),
        "mad_2" to Pair(TajweedColors.MaddLight, TajweedColors.MaddDark),
        "mad_246" to Pair(TajweedColors.MaddLight, TajweedColors.MaddDark),
        "mad_6" to Pair(TajweedColors.MaddLight, TajweedColors.MaddDark),
        "mad_obligatory" to Pair(TajweedColors.MaddLight, TajweedColors.MaddDark),

        // Iqlab (conversion) - Purple (same family as Ikhfa)
        "iqlab" to Pair(TajweedColors.IqlabLight, TajweedColors.IqlabDark),
        "iqlb" to Pair(TajweedColors.IqlabLight, TajweedColors.IqlabDark),

        // Silent letters - Gray/Slate
        "silent" to Pair(TajweedColors.SilentLight, TajweedColors.SilentDark),
        "slnt" to Pair(TajweedColors.SilentLight, TajweedColors.SilentDark),

        // Lam Shamsiyyah - use Idgham color (merging)
        "lam_shamsiyah" to Pair(TajweedColors.IdghamLight, TajweedColors.IdghamDark),
        "lam_shamsiyyah" to Pair(TajweedColors.IdghamLight, TajweedColors.IdghamDark)
    )

    /**
     * Parse tajweed-annotated text and return an AnnotatedString with colored spans.
     *
     * @param tajweedText The raw tajweed text with HTML-like tags
     * @param isDarkTheme Whether the app is in dark theme mode
     * @param defaultColor The default text color for non-tajweed text
     * @return AnnotatedString with colored spans for tajweed rules
     */
    fun parse(
        tajweedText: String,
        isDarkTheme: Boolean,
        defaultColor: Color = Color.Unspecified
    ): AnnotatedString {
        return buildAnnotatedString {
            var lastIndex = 0
            val matches = tagPattern.findAll(tajweedText)

            for (match in matches) {
                // Append text before this match
                if (match.range.first > lastIndex) {
                    append(tajweedText.substring(lastIndex, match.range.first))
                }

                // Get the tajweed class and text content
                val ruleClass = match.groupValues[1].lowercase()
                val textContent = match.groupValues[2]

                // Get the appropriate color for this rule
                val colorPair = ruleColors[ruleClass]
                val color = if (colorPair != null) {
                    if (isDarkTheme) colorPair.second else colorPair.first
                } else {
                    // Unknown rule, use default color
                    defaultColor
                }

                // Append the text with color styling
                val startIdx = length
                append(textContent)
                if (color != Color.Unspecified) {
                    addStyle(
                        style = SpanStyle(color = color),
                        start = startIdx,
                        end = length
                    )
                }

                lastIndex = match.range.last + 1
            }

            // Append any remaining text after the last match
            if (lastIndex < tajweedText.length) {
                append(tajweedText.substring(lastIndex))
            }
        }
    }

    /**
     * Strip tajweed tags from text, returning plain Arabic text.
     * Useful for fallback or when tajweed display is disabled.
     *
     * @param tajweedText The raw tajweed text with HTML-like tags
     * @return Plain text without tajweed markup
     */
    fun stripTags(tajweedText: String): String {
        return tagPattern.replace(tajweedText) { match ->
            match.groupValues[2] // Return just the text content
        }
    }

    /**
     * Check if text contains tajweed markup.
     *
     * @param text The text to check
     * @return true if the text contains tajweed tags
     */
    fun hasTajweedMarkup(text: String): Boolean {
        return tagPattern.containsMatchIn(text)
    }
}
