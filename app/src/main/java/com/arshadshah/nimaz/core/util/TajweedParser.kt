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
 * Tajweed data is pre-parsed during database generation into a simple JSON format:
 * [{"t":"بِ","r":"g"},{"t":"سْمِ","r":null}]
 *
 * Where:
 * - "t" = text content
 * - "r" = rule code or null for plain text
 *
 * V2 rule codes (each sub-type is distinct):
 *
 *  Code  | Rule                           | Colour family
 * -------|--------------------------------|---------------
 *  "g"   | Ghunnah (nasalisation)         | Green
 *  "if"  | Ikhfa (concealment)            | Teal
 *  "is"  | Ikhfa Shafawi (labial hiding)  | Cyan
 *  "dg"  | Idgham with Ghunnah            | Amber
 *  "dn"  | Idgham without Ghunnah         | Brown
 *  "ds"  | Idgham Shafawi                 | Amber variant
 *  "dj"  | Idgham Mutajanisayn            | Orange
 *  "dk"  | Idgham Mutaqaribayn            | Orange variant
 *  "q"   | Qalqalah (echoing)             | Blue
 *  "mn"  | Madd Normal (2 beats)          | Rose
 *  "mp"  | Madd Permissible (2-4-5 beats) | Pink
 *  "mo"  | Madd Obligatory (4-5 beats)    | Red
 *  "my"  | Madd Necessary (6 beats)       | Dark Rose
 *  "l"   | Iqlab (conversion)             | Violet
 *  "ls"  | Lam Shamsiyyah                 | Indigo
 *  "sl"  | Silent letters                 | Slate
 *  "hw"  | Hamza Al-Wasl                  | Light Slate
 *
 * Legacy single-letter codes (v1) are still accepted for backwards compatibility.
 */
object TajweedParser {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Map of rule codes to their light/dark colour pairs.
     * Includes both v2 granular codes and v1 legacy codes.
     */
    private val ruleColors: Map<String, Pair<Color, Color>> = mapOf(
        // ── V2 granular codes ──

        // Ghunnah
        "g" to Pair(TajweedColors.GhunnahLight, TajweedColors.GhunnahDark),

        // Ikhfa
        "if" to Pair(TajweedColors.IkhfaLight, TajweedColors.IkhfaDark),
        "is" to Pair(TajweedColors.IkhfaShafawiLight, TajweedColors.IkhfaShafawiDark),

        // Idgham
        "dg" to Pair(TajweedColors.IdghamGhunnahLight, TajweedColors.IdghamGhunnahDark),
        "dn" to Pair(TajweedColors.IdghamNoGhunnahLight, TajweedColors.IdghamNoGhunnahDark),
        "ds" to Pair(TajweedColors.IdghamShafawiLight, TajweedColors.IdghamShafawiDark),
        "dj" to Pair(TajweedColors.IdghamMutajanisaynLight, TajweedColors.IdghamMutajanisaynDark),
        "dk" to Pair(TajweedColors.IdghamMutaqaribayLight, TajweedColors.IdghamMutaqaribayDark),

        // Qalqalah
        "q" to Pair(TajweedColors.QalqalahLight, TajweedColors.QalqalahDark),

        // Madd (each sub-type distinct)
        "mn" to Pair(TajweedColors.MaddNormalLight, TajweedColors.MaddNormalDark),
        "mp" to Pair(TajweedColors.MaddPermissibleLight, TajweedColors.MaddPermissibleDark),
        "mo" to Pair(TajweedColors.MaddObligatoryLight, TajweedColors.MaddObligatoryDark),
        "my" to Pair(TajweedColors.MaddNecessaryLight, TajweedColors.MaddNecessaryDark),

        // Iqlab
        "l" to Pair(TajweedColors.IqlabLight, TajweedColors.IqlabDark),

        // Lam Shamsiyyah
        "ls" to Pair(TajweedColors.LamShamsiyyahLight, TajweedColors.LamShamsiyyahDark),

        // Silent / Hamza Wasl
        "sl" to Pair(TajweedColors.SilentLight, TajweedColors.SilentDark),
        "hw" to Pair(TajweedColors.HamzaWaslLight, TajweedColors.HamzaWaslDark),

        // ── V1 legacy codes (backwards compatibility with old databases) ──
        "i" to Pair(TajweedColors.IkhfaLight, TajweedColors.IkhfaDark),
        "d" to Pair(TajweedColors.IdghamGhunnahLight, TajweedColors.IdghamGhunnahDark),
        "m" to Pair(TajweedColors.MaddNormalLight, TajweedColors.MaddNormalDark),
        "s" to Pair(TajweedColors.SilentLight, TajweedColors.SilentDark),
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
 * @property r The rule code or null for plain text
 */
@Serializable
data class TajweedSegment(
    val t: String,
    val r: String? = null
)
