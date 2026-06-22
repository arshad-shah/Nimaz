package com.arshadshah.nimaz.widget

import org.junit.Test
import java.io.File

/**
 * Regression guard: widget UI must use real vector drawables, never emoji or
 * unicode symbol glyphs. The em-dash (U+2014) is allowed as a text fallback.
 * Runs from the module dir, so source paths are relative to `app/`.
 */
class WidgetGlyphGuardTest {

    private val forbidden = setOf('✓', '✔', '✅', '☆', '★', '→', '←')

    @Test
    fun `widget sources contain no emoji or symbol glyphs`() {
        val dir = File("src/main/java/com/arshadshah/nimaz/widget")
        assert(dir.isDirectory) { "Widget source dir not found at ${dir.absolutePath}" }

        val offenders = mutableListOf<String>()
        dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
            file.readLines().forEachIndexed { i, line ->
                line.forEach { ch ->
                    val code = ch.code
                    val isSymbol = code in 0x2600..0x27BF // misc symbols + dingbats (incl. ✓)
                    val isEmoji = code in 0x1F000..0x1FAFF || Character.isSurrogate(ch)
                    if (ch in forbidden || isSymbol || isEmoji) {
                        offenders += "%s:%d U+%04X '%s'".format(file.name, i + 1, code, ch)
                    }
                }
            }
        }
        assert(offenders.isEmpty()) {
            "Forbidden glyph(s) in widget sources:\n" + offenders.joinToString("\n")
        }
    }
}
