package com.arshadshah.nimaz.presentation.theme

import com.arshadshah.nimaz.domain.model.QuranTranslation
import com.arshadshah.nimaz.domain.model.TranslationLanguage
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Guards the face translation prose is drawn in.
 *
 * The app's body faces (Outfit / Plus Jakarta Sans) carry no Arabic-script glyphs, so an Urdu
 * translation without its own family falls back to whatever the device provides — usually a
 * Naskh face, which is the wrong script convention for Urdu. These tests pin the mapping so
 * that regression is caught here rather than on a device.
 */
class TranslationFontFamilyTest {

    @Test
    fun `urdu gets a dedicated face`() {
        assertThat(translationFontFamily(TranslationLanguage.URDU))
            .isEqualTo(NotoNastaliqUrduFontFamily)
    }

    @Test
    fun `every shipped urdu translation resolves to the nastaliq face`() {
        // Keyed on the language rather than the translation id, so both Urdu entries — and
        // any future one — get the face without a per-translation edit. This is the specific
        // thing that would break if the mapping were ever keyed on id instead.
        val urduTranslations = QuranTranslation.entries
            .filter { it.language == TranslationLanguage.URDU }

        assertThat(urduTranslations).isNotEmpty()
        assertThat(urduTranslations.map { it.id })
            .containsAtLeast("ur_maududi", "ur_jalandhry")
        urduTranslations.forEach { translation ->
            assertThat(translationFontFamily(translation.language))
                .isEqualTo(NotoNastaliqUrduFontFamily)
        }
    }

    @Test
    fun `left-to-right languages keep the default body font`() {
        // null means "inherit the body font" — deliberately not a Latin family, so the
        // renderer's own typography stays in charge for these.
        listOf(
            TranslationLanguage.ENGLISH,
            TranslationLanguage.FRENCH,
            TranslationLanguage.INDONESIAN,
            TranslationLanguage.RUSSIAN,
        ).forEach { language ->
            assertThat(translationFontFamily(language)).isNull()
        }
    }

    @Test
    fun `only right-to-left languages currently need a dedicated face`() {
        // Bengali and Hindi render fine in the fallback today; if that changes, this test is
        // the reminder that the mapping — not the render sites — is where it gets fixed.
        TranslationLanguage.entries.forEach { language ->
            if (translationFontFamily(language) != null) {
                assertThat(language.isRtl).isTrue()
            }
        }
    }
}
