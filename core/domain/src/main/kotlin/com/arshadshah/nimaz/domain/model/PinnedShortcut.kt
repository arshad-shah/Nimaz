package com.arshadshah.nimaz.domain.model

/**
 * A destination that can be pinned to the top of More.
 *
 * Capped deliberately: a pin row that holds everything is a second menu, and the whole point
 * of pinning is that the few things you actually use are reachable without scrolling.
 *
 * [key] is a **stable wire identifier**, not a route name and not the enum's own `name`. It is
 * written into DataStore and travels on the device-sync wire, so renaming an enum constant must
 * not silently unpin somebody's shortcuts; only the key is load-bearing.
 */
enum class PinnedShortcut(val key: String) {
    TASBIH("tasbih"),
    PRAYER_TRACKER("prayer_tracker"),
    KHATAM("khatam"),
    ZAKAT("zakat"),
    QIBLA("qibla"),
    FASTING("fasting"),
    NIGHT_WORSHIP("night_worship"),
    QAIDA("qaida"),
    ISLAMIC_CALENDAR("islamic_calendar");

    companion object {
        const val MAX_PINS = 5

        val DEFAULTS: List<PinnedShortcut> = listOf(TASBIH, PRAYER_TRACKER, KHATAM, ZAKAT)

        /**
         * Order is the whole point of a pin row, and a `Set` cannot hold it — which is why the
         * stored value is a delimited string rather than a `stringSetPreferencesKey`.
         *
         * Every [key] is lowercase letters and underscores, so a pipe can never appear inside
         * one and the split can never be ambiguous. It also stays legible in a preferences
         * dump, which a control character would not.
         */
        private const val SEPARATOR = "|"

        fun fromKey(key: String): PinnedShortcut? = entries.firstOrNull { it.key == key }

        /**
         * The stored form of [shortcuts] — capped here as well as on read, so the cap holds even
         * if a caller passes more. Enforcing it in only one direction means a bug in the sheet
         * silently writes six pins and the read quietly hides the sixth.
         */
        fun encode(shortcuts: List<PinnedShortcut>): String =
            shortcuts.distinct().take(MAX_PINS).joinToString(SEPARATOR) { it.key }

        /**
         * The pins in [stored], or [DEFAULTS] when nothing has been saved.
         *
         * An unknown key is **dropped, not fatal**. A future build may pin a destination this
         * one does not have, and device sync will hand that string straight to an older install;
         * the older build showing four pins instead of five is a far better outcome than the
         * More screen throwing.
         *
         * A saved-but-empty value means *deliberately no pins* and is honoured as such. Falling
         * back to the defaults there would make unpinning the last shortcut impossible — the row
         * would spring back to four the moment you closed the sheet.
         */
        fun decode(stored: String?): List<PinnedShortcut> {
            if (stored == null) return DEFAULTS
            return stored.split(SEPARATOR)
                .filter { it.isNotBlank() }
                .mapNotNull { fromKey(it) }
                .distinct()
                .take(MAX_PINS)
        }
    }
}
