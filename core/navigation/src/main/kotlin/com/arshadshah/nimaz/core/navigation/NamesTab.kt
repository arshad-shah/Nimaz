package com.arshadshah.nimaz.core.navigation

/**
 * The three name catalogues, in the order a reader would name them.
 *
 * The ordinal is what [Route.Names] carries, so the order is part of the deep link and reordering
 * these would silently repoint every saved link and every announcement.
 *
 * ## Why it lives here rather than with the screen
 *
 * It was declared inside `NamesScreen.kt`, which made it look like presentation. It is not: the
 * sentence above is a *navigation* contract, and `AnnouncementRoutes` — the mapping from an FCM
 * route key to a `Route` — needs it. That was the single `presentation` import standing between
 * `core/navigation` and its own module (#562).
 *
 * The one thing that genuinely was presentation stayed behind: the `@StringRes label` each
 * constant used to carry. `NamesScreen` now maps tab → label itself. Keeping the label here would
 * have forced a `:core:navigation` → `:core:ui` dependency for three strings, and that edge is
 * hard to undo once eleven feature modules depend on both.
 */
enum class NamesTab {
    ASMA_UL_HUSNA,
    ASMA_UN_NABI,
    PROPHETS;

    companion object {
        /** [ordinal] back to a tab, tolerating an index from a build that had more of them. */
        fun fromOrdinal(index: Int): NamesTab = entries.getOrElse(index) { ASMA_UL_HUSNA }
    }
}
