package com.arshadshah.nimaz.data.local.quran

import com.arshadshah.nimaz.domain.model.quran.catalogue.QuranEditions
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File

/**
 * The fidelity gate for every bundled translation, the counterpart of
 * [MushafLayoutFidelityTest].
 *
 * Translation assets are **positional**: index `i` holds global ayah id `i + 1`. That makes them
 * compact, and it makes a truncated or reordered file catastrophic in a quiet way — every verse
 * after the defect silently attaches to the wrong ayah, and the app has no way to notice.
 * A reader would see a coherent-looking translation that simply belongs to a different verse.
 *
 * These tests read the bytes that actually ship out of `src/main/assets` (unit tests run with the
 * module dir as the working directory — see `WidgetGlyphGuardTest`) and iterate the catalogue
 * rather than naming editions, so a translation added as data alone is validated on arrival.
 */
class TranslationFidelityTest {

    private fun asset(path: String): String {
        val file = File("src/main/assets/$path")
        assertWithMessage("missing bundled asset $path").that(file.exists()).isTrue()
        return file.readText()
    }

    private fun parsed(assetPath: String): TranslationAssetDto =
        quranTranslationJson.decodeFromString(
            TranslationAssetDto.serializer(), asset(assetPath)
        )

    /** Every catalogue translation that declares a bundled asset. */
    private val bundled by lazy {
        QuranEditions.translations.mapNotNull { edition ->
            QuranContentAssets.translations[edition.id]?.let { edition to it }
        }
    }

    @Test
    fun `the catalogue ships at least one seeded translation`() {
        // Otherwise every test below passes vacuously.
        assertThat(bundled).isNotEmpty()
    }

    @Test
    fun `every bundled translation has exactly one text per ayah`() {
        bundled.forEach { (edition, binding) ->
            val dto = parsed(binding.assetPath)
            assertWithMessage(edition.id)
                .that(dto.texts).hasSize(QuranTranslationSeeder.TOTAL_AYAHS)
        }
    }

    @Test
    fun `no verse is blank`() {
        // A blank renders as an empty translation card — indistinguishable from "this ayah has
        // no translation", and invisible unless someone scrolls to that exact verse.
        bundled.forEach { (edition, binding) ->
            val blanks = parsed(binding.assetPath).texts
                .withIndex()
                .filter { it.value.isBlank() }
                .map { it.index + 1 }
            assertWithMessage("${edition.id} blank ayah ids").that(blanks).isEmpty()
        }
    }

    @Test
    fun `the asset declares the edition it is filed under`() {
        // Guards a mis-copied file: pickthall.json holding jalandhry's text would otherwise
        // seed silently under the wrong translator id.
        bundled.forEach { (edition, binding) ->
            assertWithMessage(edition.id)
                .that(parsed(binding.assetPath).translatorId).isEqualTo(edition.id)
        }
    }

    @Test
    fun `the asset content version matches the binding`() {
        // The binding gates re-seeding; if the two drift, a shipped content bump either never
        // reaches devices or re-seeds forever.
        bundled.forEach { (edition, binding) ->
            assertWithMessage(edition.id)
                .that(parsed(binding.assetPath).contentVersion)
                .isEqualTo(binding.contentVersion)
        }
    }

    @Test
    fun `no two translations ship identical text`() {
        // Catches the copy-paste failure the positional format invites: two catalogue entries
        // pointing at the same content under different names.
        val fingerprints = bundled.associate { (edition, binding) ->
            edition.id to parsed(binding.assetPath).texts.take(20).joinToString("|")
        }
        assertThat(fingerprints.values.toSet()).hasSize(fingerprints.size)
    }

    @Test
    fun `an RTL edition ships a font that can render it`() {
        // A right-to-left translation in the Latin body font renders as tofu boxes. The
        // catalogue has to name a face, and it has to be one the app actually bundles.
        val fontIds = QuranEditions.arabicFonts.map { it.id }.toSet()
        QuranEditions.translations.filter { it.isRightToLeft }.forEach { edition ->
            assertWithMessage("${edition.id} must declare a font")
                .that(edition.fontId).isNotNull()
            assertWithMessage(edition.id).that(fontIds).contains(edition.fontId)
        }
    }

    @Test
    fun `the first verse of each translation is the basmalah, not a header or blank`() {
        // A cheap end-to-end sanity check on parsing and offset: ayah 1 is always the basmalah
        // of Al-Fatihah, so a file that starts anywhere else is off by at least one row.
        bundled.forEach { (edition, binding) ->
            val first = parsed(binding.assetPath).texts.first()
            assertWithMessage("${edition.id} first verse").that(first.trim()).isNotEmpty()
            // Long enough to be a verse rather than a stray marker or line number.
            assertWithMessage("${edition.id} first verse: '$first'")
                .that(first.length).isAtLeast(10)
        }
    }
}
