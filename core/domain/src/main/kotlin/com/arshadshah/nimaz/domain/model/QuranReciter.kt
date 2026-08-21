package com.arshadshah.nimaz.domain.model

/**
 * How a reciter recites — the tag shown next to their name in the picker.
 *
 * Kept as an enum rather than a display string so the label can be localised in the
 * presentation layer; the domain has no business holding English prose.
 */
enum class RecitationStyle {
    /** Measured, everyday recitation. */
    MURATTAL,

    /** Slow, melodically elaborated recitation. */
    MUJAWWAD
}

/**
 * Every reciter the app can play — the single source of truth, in the spirit of
 * [QuranTranslation] and the `QuranArabicFont` catalogue in the theme layer.
 *
 * Before this existed the catalogue was written out three times: a `popularReciters` list in
 * `SelectReciterScreen`, a `RECITER_CDN_MAP` plus a `getReciterDisplayName` `when` in
 * `QuranAudioManager`, and a *third* `when` in `QuranSettingsScreen`. They disagreed — the
 * settings screen matched on ids (`alafasy`, `ghamdi`, `muaiqly`) that the picker never
 * writes, so choosing any of eight reciters left the settings row showing the raw id
 * ("hussary") instead of a name.
 *
 * ## Why [id] is load-bearing
 * [id] is persisted as the user's `quran_reciter_id` preference, so renaming one strands
 * that user on a reciter that no longer resolves. Treat these as frozen. [aliases] carries
 * the ids older builds wrote for the same reciter, so those preferences still resolve.
 *
 * ## Adding one
 * Add an entry here **and** its CDN edition in `QuranAudioManager.RECITER_CDN_MAP` — that map
 * is keyed by this enum, so a missing entry is a lookup that falls back to the default rather
 * than a compile error. The picker, the settings row, and the now-playing title all derive
 * from this enum.
 */
enum class QuranReciter(
    val id: String,
    val displayName: String,
    val country: String,
    val style: RecitationStyle,
    /** Ids earlier builds persisted for this same reciter. Resolved by [fromId]. */
    val aliases: List<String> = emptyList()
) {
    MISHARY(
        id = "mishary",
        displayName = "Mishary Rashid Alafasy",
        country = "Kuwait",
        style = RecitationStyle.MURATTAL,
        aliases = listOf("alafasy")
    ),
    SUDAIS(
        id = "sudais",
        displayName = "Abdul Rahman Al-Sudais",
        country = "Saudi Arabia",
        style = RecitationStyle.MURATTAL
    ),
    ABDULBASIT(
        id = "abdulbasit",
        displayName = "Abdul Basit Abdul Samad",
        country = "Egypt",
        style = RecitationStyle.MUJAWWAD
    ),
    MAHER(
        id = "maher",
        displayName = "Maher Al-Muaiqly",
        country = "Saudi Arabia",
        style = RecitationStyle.MURATTAL,
        aliases = listOf("muaiqly")
    ),
    MINSHAWI(
        id = "minshawi",
        displayName = "Muhammad Siddiq Al-Minshawi",
        country = "Egypt",
        style = RecitationStyle.MUJAWWAD
    ),
    HUSSARY(
        id = "hussary",
        displayName = "Mahmoud Khalil Al-Hussary",
        country = "Egypt",
        style = RecitationStyle.MURATTAL
    ),
    AJAMY(
        id = "ajamy",
        displayName = "Ahmed Al-Ajamy",
        country = "Saudi Arabia",
        style = RecitationStyle.MURATTAL
    ),
    SHURAIM(
        id = "shuraim",
        displayName = "Saud Al-Shuraim",
        country = "Saudi Arabia",
        style = RecitationStyle.MURATTAL
    ),
    SHAATREE(
        id = "shaatree",
        displayName = "Abu Bakr Al-Shaatree",
        country = "Saudi Arabia",
        style = RecitationStyle.MURATTAL
    ),
    HUDHAIFY(
        id = "hudhaify",
        displayName = "Ali Al-Hudhaify",
        country = "Saudi Arabia",
        style = RecitationStyle.MURATTAL
    ),

    // These three already had working CDN editions and display names in QuranAudioManager but
    // were missing from the picker's hardcoded list, so nothing could ever select them.
    AYYOUB(
        id = "ayyoub",
        displayName = "Muhammad Ayyoub",
        country = "Saudi Arabia",
        style = RecitationStyle.MURATTAL
    ),
    JIBREEL(
        id = "jibreel",
        displayName = "Muhammad Jibreel",
        country = "Saudi Arabia",
        style = RecitationStyle.MURATTAL
    ),
    BASFAR(
        id = "basfar",
        displayName = "Abdullah Basfar",
        country = "Saudi Arabia",
        style = RecitationStyle.MURATTAL
    );

    companion object {
        /** The reciter a user has if they never picked one. */
        val DEFAULT = MISHARY

        /**
         * Resolve a stored id — including the [aliases] older builds wrote — falling back to
         * [DEFAULT] for null / unknown values so a preference from another build never leaves
         * the reader without audio.
         */
        fun fromId(id: String?): QuranReciter =
            entries.find { it.id == id || id in it.aliases } ?: DEFAULT

        /** Case-insensitive match on name or country, for the picker's search field. */
        fun search(query: String): List<QuranReciter> =
            if (query.isBlank()) entries.toList()
            else entries.filter {
                it.displayName.contains(query, ignoreCase = true) ||
                        it.country.contains(query, ignoreCase = true)
            }
    }
}
