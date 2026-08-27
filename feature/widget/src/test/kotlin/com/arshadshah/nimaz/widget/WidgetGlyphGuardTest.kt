package com.arshadshah.nimaz.widget

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/**
 * Regression guard: widget UI must use real vector drawables, never emoji or unicode symbol
 * glyphs. The em-dash (U+2014) is allowed as a text fallback.
 *
 * Runs from the module dir, so the path is relative to `feature/widget/`.
 *
 * **This test moved in PR 13 of #551, and would have gone green scanning nothing if it hadn't.**
 * It pointed at `src/main/java/com/arshadshah/nimaz/widget` — a path that ceased to exist in
 * `:app` the moment the widget sources became `:feature:widget`. That is the seventh guard in
 * this epic to be green against exactly the code it exists to catch, and the second of that shape
 * specifically: a scan whose only floor is *"the directory is there"* passes the day the
 * directory is empty and fails only on the day it is gone. Hence [MINIMUM_FILES] below, which
 * asserts on what was actually read rather than on where it was looked for.
 */
class WidgetGlyphGuardTest {

    private val forbidden = setOf('✓', '✔', '✅', '☆', '★', '→', '←')

    @Test
    fun `widget sources contain no emoji or symbol glyphs`() {
        val dir = File(SOURCE_ROOT)
        assertThat(dir.isDirectory).isTrue()

        val files = dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

        // The floor, not the directory. A guard that only checks its root exists reports success
        // for an empty tree; this one has to have read something.
        assertThat(files.size).isAtLeast(MINIMUM_FILES)

        val offenders = mutableListOf<String>()
        files.forEach { file ->
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
        assertThat(offenders).isEmpty()
    }

    private companion object {
        const val SOURCE_ROOT = "src/main/kotlin/com/arshadshah/nimaz/widget"

        /**
         * The module ships 39 widget sources. Set well below that so an ordinary deletion is not
         * a failure, but far enough above zero that a move like the one above cannot pass.
         */
        const val MINIMUM_FILES = 30
    }
}
