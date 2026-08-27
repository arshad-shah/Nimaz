package com.arshadshah.nimaz.presentation.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.domain.model.TranslationLanguage
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * How a translation is typeset, and the Arabic face the reader can choose.
 *
 * `TranslationFontFamilyTest` already pins *which* face Urdu gets. What is untested is the leading
 * that goes with it, and that is the half that actually breaks: Nastaʿlīq descends steeply and
 * needs roughly 2.1× its point size to stop one line's tail colliding with the next line's head,
 * where Latin needs 1.5×. Applying the Latin figure to Urdu produces text that renders, passes
 * every layout assertion, and is unreadable.
 *
 * `asTranslationText` also forces `TextDirection.Content` and `TextAlign.Start`, so an Urdu
 * translation lays out right-to-left inside a left-to-right screen without the caller knowing the
 * language — which is the whole reason this is a `TextStyle` extension rather than a per-screen
 * decision.
 *
 * `QuranArabicFont.fromId` is the persistence side of the reader's font setting: an id that no
 * longer resolves must fall back rather than throw, because it arrives from a preference written
 * by an older build.
 */
class TranslationTypographyTest {

    private val base = TextStyle(fontSize = 16.sp, lineHeight = 20.sp)

    @Test
    fun `only Urdu gets a dedicated face`() {
        assertThat(translationFontFamily(TranslationLanguage.URDU)).isNotNull()
        TranslationLanguage.entries
            .filter { it != TranslationLanguage.URDU }
            .forEach { assertThat(translationFontFamily(it)).isNull() }
    }

    @Test
    fun `Urdu is given the taller Nastaliq leading`() {
        // 2.1x against 1.5x. The number is the difference between legible Nastaʿlīq and lines that
        // collide — and nothing else in the app asserts it.
        val urdu = base.asTranslationText(TranslationLanguage.URDU)
        val english = base.asTranslationText(TranslationLanguage.ENGLISH)

        assertThat(urdu.lineHeight.value).isGreaterThan(english.lineHeight.value)
        assertThat(urdu.lineHeight.value).isWithin(0.01f).of(16f * 2.1f)
        assertThat(english.lineHeight.value).isWithin(0.01f).of(16f * 1.5f)
    }

    @Test
    fun `a caller's font size overrides the style's own, and the leading follows it`() {
        // The reader's translation-size slider arrives here. A leading computed from the *base*
        // size rather than the requested one leaves large text overlapping.
        val larger = base.asTranslationText(TranslationLanguage.URDU, fontSize = 24.sp)

        assertThat(larger.fontSize).isEqualTo(24.sp)
        assertThat(larger.lineHeight.value).isWithin(0.01f).of(24f * 2.1f)
    }

    @Test
    fun `a style with no size of its own keeps its own leading`() {
        // `size != TextUnit.Unspecified` — a style that never declared a size cannot have a
        // leading derived from one, and computing `Unspecified * 2.1` is how a NaN reaches layout.
        val unsized = TextStyle(lineHeight = 22.sp).asTranslationText(TranslationLanguage.URDU)

        assertThat(unsized.fontSize).isEqualTo(TextUnit.Unspecified)
        assertThat(unsized.lineHeight).isEqualTo(22.sp)
    }

    @Test
    fun `a translation lays out from the start in its own direction`() {
        // Set for every language, not just Urdu — it is what lets one composable render an English
        // and an Urdu translation without asking which it has.
        TranslationLanguage.entries.forEach { language ->
            val style = base.asTranslationText(language)
            assertThat(style.textDirection).isEqualTo(TextDirection.Content)
            assertThat(style.textAlign).isEqualTo(TextAlign.Start)
        }
    }

    @Test
    fun `a non-Urdu translation keeps whatever face it already had`() {
        // `face ?: fontFamily` — the translation face is an *override*, so a caller that already
        // chose a family for English must not have it replaced by null.
        val withFamily = base.copy(fontFamily = OutfitFontFamily)

        assertThat(withFamily.asTranslationText(TranslationLanguage.ENGLISH).fontFamily)
            .isEqualTo(OutfitFontFamily)
    }

    @Test
    fun `a language label is left alone unless it needs the Nastaliq face`() {
        // The early `?: return this` — a label for a language with no dedicated face must come
        // back untouched, including its direction, or every list heading in the app changes.
        val english = base.asLanguageLabel(TranslationLanguage.ENGLISH)

        assertThat(english).isEqualTo(base)
    }

    @Test
    fun `an Urdu label gets the face and the leading`() {
        val urdu = base.asLanguageLabel(TranslationLanguage.URDU)

        assertThat(urdu.fontFamily).isEqualTo(NotoNastaliqUrduFontFamily)
        assertThat(urdu.lineHeight.value).isWithin(0.01f).of(16f * 2.1f)
        assertThat(urdu.textDirection).isEqualTo(TextDirection.Content)
    }

    @Test
    fun `an unsized Urdu label keeps its own leading`() {
        val unsized = TextStyle(lineHeight = 18.sp).asLanguageLabel(TranslationLanguage.URDU)

        assertThat(unsized.lineHeight).isEqualTo(18.sp)
    }

    @Test
    fun `every Arabic face round-trips through its stored id`() {
        // The id is what the preference holds; the display name is what the picker shows. A
        // duplicate id would make two faces indistinguishable to the store.
        QuranArabicFont.entries.forEach { font ->
            assertThat(QuranArabicFont.fromId(font.id)).isEqualTo(font)
        }
        assertThat(QuranArabicFont.entries.map { it.id }.toSet())
            .hasSize(QuranArabicFont.entries.size)
    }

    @Test
    fun `an unknown or missing font id falls back to the default`() {
        // A preference written by an older build, or a face removed from the app. Throwing here
        // is a crash on opening the reader.
        assertThat(QuranArabicFont.fromId(null)).isEqualTo(QuranArabicFont.DEFAULT)
        assertThat(QuranArabicFont.fromId("")).isEqualTo(QuranArabicFont.DEFAULT)
        assertThat(QuranArabicFont.fromId("uthmani")).isEqualTo(QuranArabicFont.DEFAULT)
    }

    @Test
    fun `the locale typography is the app's one type scale`() {
        // `typographyForLocale` exists as the seam for a locale-specific scale and currently
        // returns the one table; a caller that started getting a *different* Typography per locale
        // would change every screen's metrics at once.
        assertThat(typographyForLocale("en")).isSameInstanceAs(NimazTypography)
        assertThat(typographyForLocale("ur")).isSameInstanceAs(NimazTypography)
    }
}
