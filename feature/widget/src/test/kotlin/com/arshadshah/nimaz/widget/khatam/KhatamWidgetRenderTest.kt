package com.arshadshah.nimaz.widget.khatam

import com.arshadshah.nimaz.widget.support.WidgetRenderer
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The khatam widget has two success layouts — a progress card and an empty prompt — chosen by
 * `hasActiveKhatam`, and a pace line assembled from two optional parts. Both are places where a
 * wrong branch draws something plausible but false.
 */
@RunWith(RobolectricTestRunner::class)
class KhatamWidgetRenderTest {

    private val widget = KhatamWidget()

    private val active = KhatamWidgetData(
        hasActiveKhatam = true,
        name = "Ramadan Khatam",
        progressPercent = 43,
        currentJuz = 13,
        remainingAyahs = 3_540,
        dailyTarget = 20,
        currentStreak = 12,
    )

    private suspend fun render(data: KhatamWidgetData) =
        WidgetRenderer.render(widget, KhatamWidgetState.Success(data))

    @Test
    fun `an active khatam shows its name, juz medallion and percentage`() = runTest {
        val rendered = render(active)

        assertThat(rendered.hasText("Ramadan Khatam")).isTrue()
        assertThat(rendered.hasText("13")).isTrue()
        assertThat(rendered.hasText("43%")).isTrue()
        assertThat(rendered.containsText("3540")).isTrue()
    }

    @Test
    fun `the pace line joins the daily target and the streak`() = runTest {
        val rendered = render(active)

        val pace = rendered.texts.single { it.contains("·") }
        assertThat(pace).contains("20")
        assertThat(pace).contains("12")
    }

    /**
     * A khatam with no daily target and no streak has nothing to say about pace. It used to say
     * it with an empty string or a stray separator; it falls back to the juz position instead.
     */
    @Test
    fun `with neither a target nor a streak the pace line falls back to the juz position`() =
        runTest {
            val rendered = render(active.copy(dailyTarget = 0, currentStreak = 0))

            assertThat(rendered.texts.any { it.contains("·") }).isFalse()
            assertThat(rendered.containsText("Juz 13")).isTrue()
        }

    @Test
    fun `one of the two pace parts alone is shown without a separator`() = runTest {
        val targetOnly = render(active.copy(currentStreak = 0))
        val streakOnly = render(active.copy(dailyTarget = 0))

        assertThat(targetOnly.texts.any { it.contains("·") }).isFalse()
        assertThat(streakOnly.texts.any { it.contains("·") }).isFalse()
        assertThat(targetOnly.containsText("20")).isTrue()
        assertThat(streakOnly.containsText("12")).isTrue()
    }

    /** The ayah count is a plural — one ayah must not read "1 ayahs remaining". */
    @Test
    fun `the remaining-ayahs line is pluralised`() = runTest {
        val one = render(active.copy(remainingAyahs = 1))
        val many = render(active.copy(remainingAyahs = 2))

        assertThat(one.containsText("1 ayah remaining")).isTrue()
        assertThat(many.containsText("2 ayahs remaining")).isTrue()
    }

    /**
     * `Success` with no active khatam is also the never-loaded default, so this is the first
     * frame every reader sees. It must offer the way in rather than an empty progress card.
     */
    @Test
    fun `no active khatam draws the start prompt instead of the progress card`() = runTest {
        val rendered = render(KhatamWidgetData())

        assertThat(rendered.hasText("43%")).isFalse()
        assertThat(rendered.hasText("Ramadan Khatam")).isFalse()
        assertThat(rendered.texts).hasSize(2)
    }

    @Test
    fun `loading and error draw their own frames`() = runTest {
        val loading = WidgetRenderer.render(widget, KhatamWidgetState.Loading)
        val error = WidgetRenderer.render(widget, KhatamWidgetState.Error("db"))

        assertThat(loading.texts).hasSize(1)
        // The error frame carries both the failure line and the retry line.
        assertThat(error.texts).hasSize(2)
    }

    /**
     * "No active khatam" is indistinguishable from never-loaded, so only a real khatam is worth
     * keeping on screen through a failed refresh.
     */
    @Test
    fun `only an active khatam survives a failed refresh`() {
        assertThat(KhatamWidgetState.Success(active).hasData).isTrue()
        assertThat(KhatamWidgetState.Success(KhatamWidgetData()).hasData).isFalse()
        assertThat(KhatamWidgetState.Loading.hasData).isFalse()
        assertThat(KhatamWidgetState.Error(null).hasData).isFalse()
    }
}
