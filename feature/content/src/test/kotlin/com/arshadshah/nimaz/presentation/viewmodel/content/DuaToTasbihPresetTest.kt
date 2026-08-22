package com.arshadshah.nimaz.presentation.viewmodel.content

import com.arshadshah.nimaz.domain.model.Dua
import com.arshadshah.nimaz.domain.model.TasbihCategory
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Turning a dua into a custom tasbih preset.
 *
 * The mapping used to be a `private fun Dua.toTasbihPreset()` inside `DuaReaderScreen`, reachable
 * only by rendering the screen — so none of the four fallbacks below were tested, and the screen
 * was making product decisions (what to call an untitled dua, how many repetitions when the dua
 * does not say) that belong nowhere near a composable.
 *
 * It came out with the "Add to tasbih" button in PR 17 of #551, when the module boundary showed
 * that button reaching into another feature's ViewModel to do the write.
 */
class DuaToTasbihPresetTest {

    private val now = 1_700_000_000_000L

    private fun dua(
        titleEnglish: String = "Dua for entering the mosque",
        titleArabic: String = "دعاء دخول المسجد",
        textArabic: String = "اللهم افتح لي أبواب رحمتك",
        textTransliteration: String? = "Allahumma iftah li abwaba rahmatik",
        textEnglish: String = "O Allah, open for me the doors of Your mercy",
        reference: String? = "Muslim 713",
        repeatCount: Int? = 3,
    ) = Dua(
        id = "1",
        categoryId = "mosque",
        titleArabic = titleArabic,
        titleEnglish = titleEnglish,
        textArabic = textArabic,
        textTransliteration = textTransliteration,
        textEnglish = textEnglish,
        reference = reference,
        occasion = null,
        benefits = null,
        repeatCount = repeatCount,
        audioUrl = null,
        displayOrder = 0,
    )

    @Test
    fun `an ordinary dua carries its text, reference and repeat count across`() {
        val preset = dua().toTasbihPreset(now)

        assertThat(preset.name).isEqualTo("Dua for entering the mosque")
        assertThat(preset.arabicText).isEqualTo("اللهم افتح لي أبواب رحمتك")
        assertThat(preset.transliteration).isEqualTo("Allahumma iftah li abwaba rahmatik")
        assertThat(preset.translation).isEqualTo("O Allah, open for me the doors of Your mercy")
        assertThat(preset.reference).isEqualTo("Muslim 713")
        assertThat(preset.targetCount).isEqualTo(3)
        assertThat(preset.category).isEqualTo(TasbihCategory.CUSTOM)
        assertThat(preset.isDefault).isFalse()
        assertThat(preset.createdAt).isEqualTo(now)
        assertThat(preset.updatedAt).isEqualTo(now)
    }

    /** A preset the user cannot read the name of is worse than one named in Arabic. */
    @Test
    fun `a blank English title falls back to the Arabic one`() {
        assertThat(dua(titleEnglish = "   ").toTasbihPreset(now).name)
            .isEqualTo("دعاء دخول المسجد")
    }

    /**
     * Long titles are truncated so the tasbih list stays readable.
     *
     * The result is **at most** 41 characters, not exactly: the 40-character prefix is
     * `trimEnd()`-ed before the ellipsis is appended, so a title whose 40th character is a space
     * yields 40. My first version of this test asserted `hasLength(41)` and failed — the
     * assertion was wrong, not the mapping, and asserting the coincidence would have made an
     * unrelated wording change look like a regression.
     */
    @Test
    fun `a title over forty characters is truncated with an ellipsis`() {
        val long = "Dua for entering the mosque and seeking the mercy of Allah"
        val name = dua(titleEnglish = long).toTasbihPreset(now).name

        assertThat(name.length).isAtMost(41)
        assertThat(name).endsWith("…")

        val body = name.dropLast(1)
        assertThat(body).isEqualTo(body.trimEnd())   // no space stranded before the ellipsis
        assertThat(long).startsWith(body)
    }

    /** A title of exactly forty characters is left alone — the rule is "over", not "at". */
    @Test
    fun `a title of exactly forty characters is not truncated`() {
        val exact = "a".repeat(40)

        assertThat(dua(titleEnglish = exact).toTasbihPreset(now).name).isEqualTo(exact)
    }

    /**
     * The conventional tasbih count, used when the dua does not prescribe one.
     *
     * `0` counts as "not prescribed" rather than being carried across — a preset with a target of
     * zero would be complete before it started.
     */
    @Test
    fun `a missing or zero repeat count becomes thirty-three`() {
        assertThat(dua(repeatCount = null).toTasbihPreset(now).targetCount).isEqualTo(33)
        assertThat(dua(repeatCount = 0).toTasbihPreset(now).targetCount).isEqualTo(33)
    }

    /** Blank optional fields become null rather than empty strings, so the UI can omit the row. */
    @Test
    fun `blank optional fields become null`() {
        val preset = dua(
            textArabic = "",
            textTransliteration = "  ",
            textEnglish = "",
            reference = "",
        ).toTasbihPreset(now)

        assertThat(preset.arabicText).isNull()
        assertThat(preset.transliteration).isNull()
        assertThat(preset.translation).isNull()
        assertThat(preset.reference).isNull()
    }
}
