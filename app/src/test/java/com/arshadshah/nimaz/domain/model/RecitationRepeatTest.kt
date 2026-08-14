package com.arshadshah.nimaz.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The guards that stop `QuranAudioManager` from ever being handed a repeat that cannot work.
 *
 * They live on the domain type, not in the manager, which is why this needs no Android.
 */
class RecitationRepeatTest {

    @Test
    fun `an ayah repeat of one is not a repeat`() {
        // One play is what the reader gets without asking, so "repeat once" is a control that
        // does nothing — a bug at the call site rather than a state to render.
        runCatching { RecitationRepeat.Ayah(1) }.exceptionOrNull().let {
            assertThat(it).isInstanceOf(IllegalArgumentException::class.java)
        }
        runCatching { RecitationRepeat.Ayah(0) }.exceptionOrNull().let {
            assertThat(it).isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Test
    fun `two or more is a repeat`() {
        assertThat(RecitationRepeat.Ayah(2).times).isEqualTo(2)
        assertThat(RecitationRepeat.Ayah(20).times).isEqualTo(20)
    }

    @Test
    fun `the stepper floors at the minimum rather than throwing`() {
        // A button press below the boundary should stop at the boundary; only a programmatic
        // construction out of range is a bug worth an exception.
        assertThat(RecitationRepeat.ayahClamped(0).times).isEqualTo(RecitationRepeat.MIN_TIMES)
        assertThat(RecitationRepeat.ayahClamped(1).times).isEqualTo(RecitationRepeat.MIN_TIMES)
        assertThat(RecitationRepeat.ayahClamped(7).times).isEqualTo(7)
    }

    @Test
    fun `a range must not end before it starts`() {
        runCatching { RecitationRepeat.Range(fromAyah = 10, toAyah = 4) }
            .exceptionOrNull().let {
                assertThat(it).isInstanceOf(IllegalArgumentException::class.java)
            }
        runCatching { RecitationRepeat.Range(fromAyah = 0, toAyah = 4) }
            .exceptionOrNull().let {
                assertThat(it).isInstanceOf(IllegalArgumentException::class.java)
            }
    }

    @Test
    fun `a single-verse range is legal`() {
        val range = RecitationRepeat.Range(fromAyah = 5, toAyah = 5)

        assertThat(range.fromAyah).isEqualTo(5)
        assertThat(range.toAyah).isEqualTo(5)
    }

    @Test
    fun `the offered speeds are the only ones that resolve`() {
        assertThat(RecitationSpeed.of(0.75f)).isEqualTo(RecitationSpeed.SLOWEST)
        assertThat(RecitationSpeed.of(1f)).isEqualTo(RecitationSpeed.NORMAL)
        assertThat(RecitationSpeed.of(1.25f)).isEqualTo(RecitationSpeed.FASTER)
        assertThat(RecitationSpeed.of(1.5f)).isEqualTo(RecitationSpeed.FASTEST)
        // Not a nearest-match: asking for 2x is asking for something the player does not do,
        // and quietly giving back 1.5x is a worse answer than none.
        assertThat(RecitationSpeed.of(2f)).isNull()
        assertThat(RecitationSpeed.of(0f)).isNull()
    }

    @Test
    fun `the default speed is unmodified playback`() {
        assertThat(RecitationSpeed.DEFAULT.multiplier).isEqualTo(1f)
    }
}
