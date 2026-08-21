package com.arshadshah.nimaz.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The pin rule, tested where it runs on every commit.
 *
 * The cap, the ordering and the unknown-key behaviour are the parts that matter, and none of them
 * need a device — so none of them live only behind the instrumented suite. A broken cap ships the
 * moment an emulator run is skipped, and "pinning is capped at 5" is the decision the whole
 * feature rests on.
 */
class PinnedShortcutCodecTest {

    @Test
    fun `order survives the round trip`() {
        // A Set would lose this, which is why the stored value is a delimited string.
        val order = listOf(
            PinnedShortcut.ZAKAT,
            PinnedShortcut.TASBIH,
            PinnedShortcut.KHATAM,
        )
        assertThat(PinnedShortcut.decode(PinnedShortcut.encode(order)))
            .containsExactlyElementsIn(order).inOrder()
    }

    @Test
    fun `the cap is applied on write, not only on read`() {
        // Enforcing it one-way means a bug in the sheet writes nine pins and the read quietly
        // hides four of them — the stored state and the shown state stop agreeing.
        val encoded = PinnedShortcut.encode(PinnedShortcut.entries.toList())
        assertThat(encoded.split("|")).hasSize(PinnedShortcut.MAX_PINS)
        assertThat(PinnedShortcut.decode(encoded)).hasSize(PinnedShortcut.MAX_PINS)
    }

    @Test
    fun `the cap keeps the first five, not an arbitrary five`() {
        val encoded = PinnedShortcut.encode(PinnedShortcut.entries.toList())
        assertThat(PinnedShortcut.decode(encoded))
            .containsExactlyElementsIn(PinnedShortcut.entries.take(PinnedShortcut.MAX_PINS))
            .inOrder()
    }

    @Test
    fun `nothing saved gives the defaults`() {
        assertThat(PinnedShortcut.decode(null)).isEqualTo(PinnedShortcut.DEFAULTS)
    }

    @Test
    fun `saved-but-empty means deliberately no pins`() {
        // Falling back to the defaults here would make unpinning the last shortcut impossible:
        // the row would spring back to four the moment the sheet closed.
        assertThat(PinnedShortcut.decode("")).isEmpty()
        assertThat(PinnedShortcut.encode(emptyList())).isEmpty()
    }

    @Test
    fun `an unknown key is dropped rather than fatal`() {
        // A newer build can pin something this one has never heard of, and device sync hands the
        // string straight over. Four pins beats a crash on the More screen.
        assertThat(PinnedShortcut.decode("tasbih|not_a_screen|zakat"))
            .containsExactly(PinnedShortcut.TASBIH, PinnedShortcut.ZAKAT).inOrder()
    }

    @Test
    fun `duplicates collapse instead of consuming pin slots`() {
        val encoded = PinnedShortcut.encode(
            listOf(PinnedShortcut.ZAKAT, PinnedShortcut.ZAKAT, PinnedShortcut.TASBIH)
        )
        assertThat(PinnedShortcut.decode(encoded))
            .containsExactly(PinnedShortcut.ZAKAT, PinnedShortcut.TASBIH).inOrder()
        assertThat(PinnedShortcut.decode("zakat|zakat|tasbih"))
            .containsExactly(PinnedShortcut.ZAKAT, PinnedShortcut.TASBIH).inOrder()
    }

    @Test
    fun `blank segments from a mangled value do not become pins`() {
        assertThat(PinnedShortcut.decode("|tasbih||zakat|"))
            .containsExactly(PinnedShortcut.TASBIH, PinnedShortcut.ZAKAT).inOrder()
    }

    @Test
    fun `the defaults fit under the cap`() {
        // Otherwise a fresh install starts in a state the sheet cannot represent.
        assertThat(PinnedShortcut.DEFAULTS.size).isAtMost(PinnedShortcut.MAX_PINS)
    }

    @Test
    fun `every key is unique and pipe-safe`() {
        // The separator choice depends on it: a key containing "|" would split into nonsense.
        val keys = PinnedShortcut.entries.map { it.key }
        assertThat(keys).containsNoDuplicates()
        assertThat(keys.none { it.contains("|") }).isTrue()
    }
}
