package com.arshadshah.nimaz.domain.model.quran.catalogue

/**
 * One selectable Quran translation.
 *
 * The [id] is what lands in `translations.translator_id` and in the user's DataStore
 * preference, so it must match the id used when the asset was seeded.
 */
data class TranslationEdition(
    override val id: String,
    override val displayName: String,
    /** Credited in About and in shared ayah text; often equals [displayName]. */
    val translatorName: String,
    override val languageTag: String,
    override val isRightToLeft: Boolean = false,
    /**
     * Font to render this translation with, resolved to a `FontFamily` in presentation.
     * `null` uses the default body font — correct for Latin-script translations.
     */
    val fontId: String? = null,
    override val legacyKeys: Set<String> = emptySet()
) : QuranEdition

/**
 * One mushaf edition — the pagination and, for line-accurate editions, the printed line
 * breaks.
 *
 * Two kinds exist and [linesPerPage] tells them apart:
 * - **Flowed** ([linesPerPage] `null`): pagination comes from the `ayahs.page` column that
 *   ships in the prepopulated DB. Only the classic Madani mushaf is stored that way.
 * - **Line-accurate** ([linesPerPage] non-null): pagination *and* line breaks come from this
 *   edition's rows in the `mushaf_layouts` table, seeded from a bundled JSON asset. This is
 *   the property that makes a page usable for hifz, because the reader can reproduce exactly
 *   where the printed mushaf breaks each line.
 */
data class MushafLayoutEdition(
    override val id: String,
    override val displayName: String,
    /** Pages in this edition — the reader's pager bound and jump-to-page validation. */
    val totalPages: Int,
    /** Printed lines per page, or `null` for a flowed edition. See the class KDoc. */
    val linesPerPage: Int?,
    /** Which `ayahs` text column this layout's word positions index into. */
    val textSource: AyahTextSource,
    /**
     * Whether per-letter tajweed spans exist for this edition's text. Only the Uthmani text
     * carries them, so the tajweed toggle is disabled (with a reason) for editions without.
     */
    val supportsTajweed: Boolean,
    val fontId: String,
    override val languageTag: String = "ar",
    override val isRightToLeft: Boolean = true,
    override val legacyKeys: Set<String> = emptySet()
) : QuranEdition {

    /**
     * Whether this edition's pages come from the `mushaf_layouts` table rather than the
     * `ayahs.page` column. Drives both the repository's page-range source and whether the
     * layout asset needs seeding before the edition can be read.
     */
    val hasLineLayout: Boolean get() = linesPerPage != null
}

/** One tafseer (exegesis) source. Its [id] keys `tafseer_texts.tafseer_id`. */
data class TafseerEdition(
    override val id: String,
    override val displayName: String,
    /** The scholar / work credited in About. */
    val authorName: String,
    override val languageTag: String,
    override val isRightToLeft: Boolean = false,
    override val legacyKeys: Set<String> = emptySet()
) : QuranEdition

/**
 * One reciter offered by the audio player.
 *
 * Deliberately carries no CDN identifier or bitrate: which service streams the audio is a
 * data-layer concern that can change without the catalogue moving. That binding lives in
 * [com.arshadshah.nimaz.data.local.quran.QuranContentAssets.reciterAudio].
 */
data class ReciterEdition(
    override val id: String,
    override val displayName: String,
    /** Country the reciter is associated with, shown under the name in the picker. */
    val country: String,
    /** Recitation style, e.g. `Murattal` or `Mujawwad`. */
    val style: String,
    override val languageTag: String = "ar",
    override val isRightToLeft: Boolean = true,
    override val legacyKeys: Set<String> = emptySet()
) : QuranEdition

/**
 * One selectable Arabic font.
 *
 * The catalogue owns the [id] and [displayName]; the actual `FontFamily` stays in
 * presentation ([com.arshadshah.nimaz.presentation.theme.QuranArabicFont]) because a
 * `FontFamily` is a Compose type that domain must not import. ADR-002.
 */
data class ArabicFontEdition(
    override val id: String,
    override val displayName: String,
    override val languageTag: String = "ar",
    override val isRightToLeft: Boolean = true,
    override val legacyKeys: Set<String> = emptySet()
) : QuranEdition
