package com.arshadshah.nimaz.ui.components.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Text Parser for Mixed Scripts
data class TextSegment(
    val text: String,
    val scriptType: ScriptType
)

data class TextLine(
    val segments: List<TextSegment>,
    val isRtl: Boolean
)

enum class ScriptType {
    ARABIC,
    ENGLISH,
    URDU,
    PUNCTUATION
}

object MixedTextParser {
    // Internal cache to avoid recalculating parsed text
    private val parseCache = mutableMapOf<String, List<TextLine>>()
    private const val MAX_CACHE_SIZE = 100 // Prevent unlimited cache growth

    // Progress tracking for UI feedback
    data class ParseProgress(
        val percentage: Float,
        val stage: String
    )

    suspend fun parseTextAsync(
        text: String,
        onProgress: ((ParseProgress) -> Unit)? = null
    ): List<TextLine> = withContext(Dispatchers.Default) {
        // Check cache first
        parseCache[text]?.let {
            onProgress?.invoke(ParseProgress(100f, "Complete"))
            return@withContext it
        }

        onProgress?.invoke(ParseProgress(0f, "Starting..."))

        val result = parseTextToLinesWithProgress(text, onProgress)

        // Cache the result (with size management)
        if (parseCache.size >= MAX_CACHE_SIZE) {
            // Remove oldest entries (simple FIFO)
            val keysToRemove = parseCache.keys.take(10)
            keysToRemove.forEach { parseCache.remove(it) }
        }
        parseCache[text] = result

        onProgress?.invoke(ParseProgress(100f, "Complete"))
        result
    }

    fun parseTextToLines(text: String): List<TextLine> {
        // Check cache first for synchronous calls too
        parseCache[text]?.let { return it }

        val segments = parseText(text)
        val result = groupSegmentsIntoLines(segments)

        // Cache the result
        if (parseCache.size < MAX_CACHE_SIZE) {
            parseCache[text] = result
        }

        return result
    }

    private suspend fun parseTextToLinesWithProgress(
        text: String,
        onProgress: ((ParseProgress) -> Unit)? = null
    ): List<TextLine> {
        onProgress?.invoke(ParseProgress(10f, "Analyzing text..."))

        val segments = parseTextWithProgress(text, onProgress)

        onProgress?.invoke(ParseProgress(80f, "Organizing lines..."))

        val result = groupSegmentsIntoLines(segments)

        onProgress?.invoke(ParseProgress(95f, "Finalizing..."))

        return result
    }

    private suspend fun parseTextWithProgress(
        text: String,
        onProgress: ((ParseProgress) -> Unit)? = null
    ): List<TextSegment> {
        val segments = mutableListOf<TextSegment>()
        var currentSegment = StringBuilder()
        var currentType: ScriptType? = null

        val textLength = text.length
        var processedChars = 0

        for (char in text) {
            val charType = detectScriptType(char)

            if (currentType == null) {
                currentType = charType
                currentSegment.append(char)
            } else if (currentType == charType || charType == ScriptType.PUNCTUATION) {
                currentSegment.append(char)
            } else {
                if (currentSegment.isNotEmpty()) {
                    segments.add(TextSegment(currentSegment.toString().trim(), currentType))
                }
                currentSegment = StringBuilder()
                currentSegment.append(char)
                currentType = charType
            }

            processedChars++

            // Update progress every 100 characters to avoid too many updates
            if (processedChars % 100 == 0 || processedChars == textLength) {
                val progress = (processedChars.toFloat() / textLength * 70f) + 10f // 10-80% range
                onProgress?.invoke(ParseProgress(progress, "Processing characters... ($processedChars/$textLength)"))

                // Yield to prevent blocking
                if (processedChars % 500 == 0) {
                    kotlinx.coroutines.yield()
                }
            }
        }

        // Add the last segment
        if (currentSegment.isNotEmpty()) {
            segments.add(TextSegment(currentSegment.toString().trim(), currentType ?: ScriptType.ENGLISH))
        }

        return segments.filter { it.text.isNotBlank() }
    }

    // Clear cache method (useful for memory management)
    fun clearCache() {
        parseCache.clear()
    }

    // Get cache stats (useful for debugging)
    fun getCacheStats(): Pair<Int, Set<String>> {
        return parseCache.size to parseCache.keys.toSet()
    }

    private fun groupSegmentsIntoLines(segments: List<TextSegment>): List<TextLine> {
        val lines = mutableListOf<TextLine>()
        var currentLineSegments = mutableListOf<TextSegment>()

        for (segment in segments) {
            when (segment.scriptType) {
                ScriptType.ARABIC, ScriptType.URDU -> {
                    // Check if this is continuing an existing Arabic/Urdu line
                    val hasRtlInCurrentLine = currentLineSegments.any {
                        it.scriptType == ScriptType.ARABIC || it.scriptType == ScriptType.URDU
                    }

                    if (hasRtlInCurrentLine) {
                        // Continue on same RTL line
                        currentLineSegments.add(segment)
                    } else {
                        // Start new line for Arabic/Urdu text
                        if (currentLineSegments.isNotEmpty()) {
                            lines.add(TextLine(currentLineSegments.toList(), false))
                            currentLineSegments.clear()
                        }
                        currentLineSegments.add(segment)
                    }
                }

                ScriptType.ENGLISH -> {
                    // Check if current line has RTL content
                    val hasRtlInCurrentLine = currentLineSegments.any {
                        it.scriptType == ScriptType.ARABIC || it.scriptType == ScriptType.URDU
                    }

                    if (hasRtlInCurrentLine) {
                        // Complete the RTL line and start new LTR line
                        lines.add(TextLine(currentLineSegments.toList(), true))
                        currentLineSegments.clear()
                        currentLineSegments.add(segment)
                    } else {
                        // Continue on current LTR line
                        currentLineSegments.add(segment)
                    }
                }

                ScriptType.PUNCTUATION -> {
                    // Punctuation stays with current line context
                    currentLineSegments.add(segment)
                }
            }
        }

        // Add remaining segments as final line
        if (currentLineSegments.isNotEmpty()) {
            val isRtl = currentLineSegments.any {
                it.scriptType == ScriptType.ARABIC || it.scriptType == ScriptType.URDU
            }
            lines.add(TextLine(currentLineSegments.toList(), isRtl))
        }

        return lines.filter { it.segments.isNotEmpty() }
    }

    fun parseText(text: String): List<TextSegment> {
        val segments = mutableListOf<TextSegment>()
        var currentSegment = StringBuilder()
        var currentType: ScriptType? = null

        for (char in text) {
            val charType = detectScriptType(char)

            if (currentType == null) {
                currentType = charType
                currentSegment.append(char)
            } else if (currentType == charType || charType == ScriptType.PUNCTUATION) {
                // Continue with same type or add punctuation to current segment
                currentSegment.append(char)
            } else {
                // Script type changed, save current segment and start new one
                if (currentSegment.isNotEmpty()) {
                    segments.add(TextSegment(currentSegment.toString().trim(), currentType))
                }
                currentSegment = StringBuilder()
                currentSegment.append(char)
                currentType = charType
            }
        }

        // Add the last segment
        if (currentSegment.isNotEmpty()) {
            segments.add(TextSegment(currentSegment.toString().trim(), currentType ?: ScriptType.ENGLISH))
        }

        return segments.filter { it.text.isNotBlank() }
    }

    private fun detectScriptType(char: Char): ScriptType {
        val englishPunctuationList = listOf(
            ' ', '.', ',', ';', ':', '!', '?', '(', ')', '[', ']', '{', '}', '"', '\'', '—', '–'
        )

        return when {
            // Arabic Unicode ranges (including special symbols like ﷺ)
            char.code in 0x0600..0x06FF || // Arabic
                    char.code in 0x0750..0x077F || // Arabic Supplement
                    char.code in 0x08A0..0x08FF || // Arabic Extended-A
                    char.code in 0xFB50..0xFDFF || // Arabic Presentation Forms-A (includes ﷺ)
                    char.code in 0xFE70..0xFEFF    // Arabic Presentation Forms-B
                -> {
                // Check if it's a special Arabic symbol that shouldn't trigger new lines
                if (isArabicInlineSymbol(char)) {
                    ScriptType.PUNCTUATION
                } else {
                    ScriptType.ARABIC
                }
            }

            // Urdu specific characters (some overlap with Arabic but context matters)
            char.code in 0x0600..0x06FF && isUrduSpecific(char) -> ScriptType.URDU

            // Whitespace
            char.isWhitespace() -> ScriptType.PUNCTUATION

            // English punctuation that should NOT be used with Arabic
            char in englishPunctuationList -> ScriptType.ENGLISH

            // Arabic punctuation and symbols
            char.code in 0x2000..0x206F || // General Punctuation
                    isArabicPunctuation(char) -> ScriptType.PUNCTUATION

            // Default to English for Latin script and others
            else -> ScriptType.ENGLISH
        }
    }

    private fun isArabicInlineSymbol(char: Char): Boolean {
        // Arabic symbols that should stay inline and not trigger new lines
        val inlineSymbols = setOf(
            '\uFDFA', // ﷺ (SALLALLAAHU ALAYHE WASALLAM)
            '\uFDFB', // ﷻ (JALLAJALALOUHOU)
            '\u06DD', // ۝ (Arabic End of Ayah)
            '\u06DE', // ۞ (Arabic Start of Rub El Hizb)
            '\u06E9', // ۩ (Arabic Place of Sajdah)
            '\u06FD', // ۽ (Arabic Sign Sindhi Ampersand)
            '\u06FE'  // ۾ (Arabic Sign Sindhi Postposition Men)
        )
        return char in inlineSymbols
    }

    private fun isArabicPunctuation(char: Char): Boolean {
        // Arabic-specific punctuation marks
        val arabicPunctuation = setOf(
            '\u060C', // ، (Arabic Comma)
            '\u061B', // ؛ (Arabic Semicolon)
            '\u061F', // ؟ (Arabic Question Mark)
            '\u0640', // ـ (Arabic Tatweel)
            '\u066A', // ٪ (Arabic Percent Sign)
            '\u066B', // ٫ (Arabic Decimal Separator)
            '\u066C', // ٬ (Arabic Thousands Separator)
            '\u066D', // ٭ (Arabic Five Pointed Star)
            '\u06D4'  // ۔ (Arabic Full Stop)
        )
        return char in arabicPunctuation
    }

    private fun isUrduSpecific(char: Char): Boolean {
        // Add specific Urdu characters that should use Urdu font
        val urduSpecificChars = setOf(
            '\u0679', '\u067E', '\u0686', '\u0688', '\u0691',
            '\u06A9', '\u06AF', '\u06BA', '\u06BB', '\u06BE',
            '\u06C1', '\u06C2', '\u06C3', '\u06CC', '\u06D2'
        )
        return char in urduSpecificChars
    }
}

// Composable for rendering mixed text with async parsing and RTL support
@Composable
fun MixedScriptText(
    text: String,
    arabicFontFamily: FontFamily,
    englishFontFamily: FontFamily,
    urduFontFamily: FontFamily,
    arabicFontSize: TextUnit,
    englishFontSize: TextUnit,
    color: androidx.compose.ui.graphics.Color,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    textAlign: TextAlign = TextAlign.Start,
    modifier: Modifier = Modifier,
    loadingText: String = "Loading...",
    showLoadingForShortTexts: Boolean = false
) {
    var textLines by remember(text) { mutableStateOf<List<TextLine>?>(null) }
    var isLoading by remember(text) { mutableStateOf(true) }

    LaunchedEffect(text) {
        isLoading = true
        textLines = null

        // For very short texts, you might want to parse synchronously
        if (!showLoadingForShortTexts && text.length < 100) {
            textLines = MixedTextParser.parseTextToLines(text)
        } else {
            textLines = MixedTextParser.parseTextAsync(text)
        }

        isLoading = false
    }

    when {
        isLoading && showLoadingForShortTexts -> {
            Text(
                text = loadingText,
                style = style,
                textAlign = textAlign,
                modifier = modifier,
                color = color.copy(alpha = 0.6f)
            )
        }

        textLines != null -> {
            Column(modifier = modifier) {
                textLines!!.forEach { line ->
                    Text(
                        text = buildAnnotatedString {
                            line.segments.forEach { segment ->
                                when (segment.scriptType) {
                                    ScriptType.ARABIC -> {
                                        withStyle(
                                            style = SpanStyle(
                                                fontFamily = arabicFontFamily,
                                                fontSize = arabicFontSize,
                                                color = color
                                            )
                                        ) {
                                            append(segment.text)
                                        }
                                    }

                                    ScriptType.URDU -> {
                                        withStyle(
                                            style = SpanStyle(
                                                fontFamily = urduFontFamily,
                                                fontSize = englishFontSize,
                                                color = color
                                            )
                                        ) {
                                            append(segment.text)
                                        }
                                    }

                                    ScriptType.ENGLISH, ScriptType.PUNCTUATION -> {
                                        withStyle(
                                            style = SpanStyle(
                                                fontFamily = englishFontFamily,
                                                fontSize = englishFontSize,
                                                color = color
                                            )
                                        ) {
                                            append(segment.text)
                                        }
                                    }
                                }

                                // Add space between segments if needed
                                if (segment != line.segments.last() &&
                                    !segment.text.endsWith(" ") &&
                                    !line.segments[line.segments.indexOf(segment) + 1].text.startsWith(" ")) {
                                    append(" ")
                                }
                            }
                        },
                        style = style.copy(
                            textDirection = if (line.isRtl) TextDirection.Rtl else TextDirection.Ltr
                        ),
                        textAlign = if (line.isRtl) TextAlign.End else textAlign,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}