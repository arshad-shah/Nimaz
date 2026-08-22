package com.arshadshah.nimaz.presentation.theme

import androidx.compose.runtime.compositionLocalOf

/**
 * The decorative background treatments a screen can wear.
 *
 * Offered as a choice rather than a single fixed look: a texture that reads as
 * beautiful to one person reads as noise to another, and this is applied to
 * *every* screen. [NONE] is a first-class option, not a failure mode — choosing
 * it is how a reader turns the ornament off entirely.
 *
 * Lives in the theme package (not alongside `NimazPatternBackground`) so
 * [NimazTheme] can provide [LocalPatternStyle] without the theme layer depending
 * on a component.
 */
enum class NimazPatternStyle {
    /** No ornament. Equivalent to switching the feature off. */
    NONE,

    /** One large shamsa medallion bleeding off the top-right corner. */
    CORNER_MEDALLION,

    /** A repeating diamond lattice across the whole surface. */
    LATTICE,

    /** A sparse grid of small diamond buds — paper "tooth" rather than line work. */
    STAR_FIELD,

    /** [STAR_FIELD] plus a corner medallion: texture everywhere, focus in one place. */
    ATELIER;

    companion object {
        /**
         * Resolves a persisted preference key (the enum [name]) back to a style,
         * falling back to [CORNER_MEDALLION] for anything unrecognised so a bad or
         * legacy value can never crash the theme.
         */
        fun fromKey(key: String?): NimazPatternStyle =
            entries.firstOrNull { it.name == key } ?: CORNER_MEDALLION
    }
}

/**
 * The pattern style in force. Screens never read this directly — they wrap in
 * `NimazPatternBackground`, which resolves it. Provided by [NimazTheme] from the
 * user's preference; defaults to [NimazPatternStyle.CORNER_MEDALLION] outside a
 * theme (e.g. in previews).
 */
val LocalPatternStyle = compositionLocalOf { NimazPatternStyle.CORNER_MEDALLION }
