package com.arshadshah.nimaz.core.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.arshadshah.nimaz.presentation.theme.NimazColors.TajweedColors
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Parser for pre-parsed tajweed JSON data.
 *
 * Tajweed data is now pre-parsed during database generation into a simple JSON format:
 * [{"t":"بِ","r":"g"},{"t":"سْمِ","r":null}]
 *
 * Where:
 * - "t" = text content
 * - "r" = rule code (single letter) or null for plain text
 *
 * Rule codes:
 * - "g" = Ghunnah (nasalization) - Green
 * - "i" = Ikhfa (hiding) - Purple
 * - "d" = Idgham (merging) - Amber/Orange
 * - "q" = Qalqalah (echoing) - Blue
 * - "m" = Madd (elongation) - Red
 * - "l" = Iqlab (conversion) - Purple
 * - "s" = Silent letters - Gray/Slate
 */
object TajweedParser {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Map of single-letter rule codes to their light/dark color pairs.
     */
    private val ruleColors: Map<String, Pair<Color, Color>> = mapOf(
        // Ghunnah (nasalization) - Green
        "g" to Pair(TajweedColors.GhunnahLight, TajweedColors.GhunnahDark),

        // Ikhfa (hiding) - Purple
        "i" to Pair(TajweedColors.IkhfaLight, TajweedColors.IkhfaDark),

        // Idgham (merging) - Amber/Orange
        "d" to Pair(TajweedColors.IdghamLight, TajweedColors.IdghamDark),

        // Qalqalah (echoing) - Blue
        "q" to Pair(TajweedColors.QalqalahLight, TajweedColors.QalqalahDark),

        // Madd (elongation) - Red
        "m" to Pair(TajweedColors.MaddLight, TajweedColors.MaddDark),

        // Iqlab (conversion) - Purple
        "l" to Pair(TajweedColors.IqlabLight, TajweedColors.IqlabDark),

        // Silent letters - Gray/Slate
        "s" to Pair(TajweedColors.SilentLight, TajweedColors.SilentDark)
    )

    /**
     * Parse pre-parsed tajweed JSON and return an AnnotatedString with colored spans.
     *
     * @param tajweedText The pre-parsed JSON string: [{"t":"text","r":"code"},...]
     * @param isDarkTheme Whether the app is in dark theme mode
     * @param defaultColor The default text color for plain text (r=null)
     * @return AnnotatedString with colored spans for tajweed rules
     */
    fun parse(
        tajweedText: String,
        isDarkTheme: Boolean,
        defaultColor: Color = Color.Unspecified
    ): AnnotatedString {
        return try {
            val segments = json.decodeFromString<List<TajweedSegment>>(tajweedText)
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
                            addStyle(
                                style = SpanStyle(color = color),
                                start = startIdx,
                                end = length
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // If parsing fails, return the raw text without colors
            buildAnnotatedString {
                append(stripJson(tajweedText))
            }
        }
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
 * @property r The rule code (single letter) or null for plain text
 */
@Serializable
data class TajweedSegment(
    val t: String,
    val r: String? = null
)
