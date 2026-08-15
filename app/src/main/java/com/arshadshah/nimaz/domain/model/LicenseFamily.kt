package com.arshadshah.nimaz.domain.model

/**
 * The licence families the Open Source screen groups, filters and colours by.
 *
 * AboutLibraries reports a licence's **name**, and the same licence arrives under several
 * spellings depending on which POM declared it — "Apache License 2.0", "The Apache Software
 * License, Version 2.0" and "Apache-2.0" are all Apache. Grouping by the raw name puts one
 * licence in three sections; grouping by family puts it in one.
 *
 * Deliberately coarse. This is a browsing aid, not a compliance tool: the detail screen still
 * shows the exact declared name and the full text, which is what actually governs use.
 */
enum class LicenseFamily {
    APACHE_2,
    MIT,
    BSD,
    OFL,
    GPL,
    OTHER,
    ;

    companion object {

        /**
         * Places a declared licence name in a family.
         *
         * Order matters. GPL is tested before BSD and MIT because "GNU Lesser General Public
         * License" and friends carry no other marker, and the short names are matched on word
         * boundaries — a bare `contains("mit")` also matches "permitted", which appears in
         * more than one licence *title*.
         */
        fun of(name: String?): LicenseFamily {
            val text = name?.lowercase().orEmpty()
            return when {
                text.isBlank() -> OTHER
                text.contains("apache") -> APACHE_2
                text.contains("open font") || text.matchesWord("ofl") -> OFL
                text.contains("gpl") || text.contains("general public license") -> GPL
                text.matchesWord("bsd") -> BSD
                text.matchesWord("mit") -> MIT
                else -> OTHER
            }
        }

        private fun String.matchesWord(word: String): Boolean =
            Regex("""\b${Regex.escape(word)}\b""").containsMatchIn(this)
    }
}
