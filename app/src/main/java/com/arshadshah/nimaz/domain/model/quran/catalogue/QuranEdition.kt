package com.arshadshah.nimaz.domain.model.quran.catalogue

/**
 * The common shape of every selectable piece of Quran content — a translation, a mushaf
 * layout, a tafseer, a reciter or an Arabic font.
 *
 * ## Why this exists
 * Before the registry, each axis kept its own ad-hoc list and three of them lived in
 * `presentation/` (the translation picker's `listOf("Sahih International" to …)`, the reciter
 * screen's `popularReciters`, and the font enum), while the reciter metadata was in fact
 * *triplicated* across the picker, [com.arshadshah.nimaz.data.audio.QuranAudioManager]'s CDN
 * map and its display-name `when` — which had already drifted. Adding an edition therefore
 * meant editing screens, and forgetting one copy shipped a half-working edition.
 *
 * A catalogue entry is pure metadata: no Android types, no Compose types, no asset paths.
 * Those bindings live one layer out (see
 * [com.arshadshah.nimaz.data.local.quran.QuranContentAssets] for asset/CDN wiring and
 * [com.arshadshah.nimaz.presentation.theme.QuranArabicFont] for `fontId` → `FontFamily`), and
 * `QuranEditionRegistryTest` fails the build if the id sets ever disagree. See ADR-002.
 */
interface QuranEdition {
    /** Stable, snake_case, persisted in DataStore and in DB rows. Never reused or renamed. */
    val id: String

    /** Shown in pickers and credits. Safe to change — it is never persisted. */
    val displayName: String

    /** BCP-47, e.g. `en`, `ur`, `ar`, `bn`. Drives the picker's language label. */
    val languageTag: String

    /** Whether this edition's text reads right-to-left. */
    val isRightToLeft: Boolean

    /**
     * Values this edition was persisted as before the registry existed — e.g. the
     * `MushafScript` enum *name* `INDOPAK_16`. Resolution falls back to these so an upgrade
     * never silently resets a user's selection; the stored value is resolved, never
     * rewritten. See ADR-003.
     */
    val legacyKeys: Set<String>
}

/**
 * Which `ayahs` text column a mushaf layout's word positions index into.
 *
 * A layout table stores only word-position *ranges*; the glyphs are reconstructed by slicing
 * the ayah text on spaces. That is only valid against the exact text the layout was
 * tokenised from, so the column is part of the layout's identity rather than a global
 * setting.
 */
enum class AyahTextSource(
    /** The `ayahs` column this source reads, matching the Room entity's `@ColumnInfo`. */
    val columnName: String
) {
    UTHMANI("text_uthmani"),
    INDOPAK("text_indopak")
}

/** Resolution shared by every axis: exact id, then legacy alias, then the axis default. */
internal fun <T : QuranEdition> List<T>.resolveEdition(id: String?, fallback: T): T =
    firstOrNull { it.id == id }
        ?: firstOrNull { id != null && id in it.legacyKeys }
        ?: fallback
