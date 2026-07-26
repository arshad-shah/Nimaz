package com.arshadshah.nimaz.core.navigation

import com.arshadshah.nimaz.domain.model.WorshipReminderType

/**
 * Where a Home "Next Worship" card goes when tapped.
 *
 * ## Why this is a pure function and not a `when` inside the screen
 *
 * The mapping is the whole feature — a card that navigates somewhere unhelpful is worse than one
 * that does nothing, because a misfire teaches you to stop tapping and costs you the destinations
 * that *are* good. Keeping it here makes every row assertable in a plain JVM test
 * (`WorshipDestinationsTest`), with no device, no Compose and no navigation host.
 *
 * ## The mapping, and why each row is what it is
 *
 * | Reminder                      | Destination            | Rationale                                |
 * |-------------------------------|------------------------|------------------------------------------|
 * | Morning / Evening Adhkar      | `DuaCategory(1 / 2)`   | The exact adhkar being reminded about.    |
 * | Suhoor, Iftar                 | `FastingTracker`       | Where the fast is logged and timed.       |
 * | Mon/Thu, White days, Arafah   | `FastingTracker`       | Same — log the intended fast.             |
 * | Tahajjud, Witr                | `NightWorship`         | The hub built for exactly these two.      |
 * | Taraweeh, Laylatul Qadr       | `DuaCategory(37)`      | "Fasting & Ramadan". Thin but honest —    |
 * |                               |                        | both are Ramadan-gated, so they are       |
 * |                               |                        | invisible ~11 months a year. Revisit with |
 * |                               |                        | real usage rather than guessing now.      |
 *
 * The category ids are the seeded ones in `assets/duas/duas.json` and are stable — the seeder keys
 * off them, so they cannot be renumbered without a content migration.
 */
fun worshipCardDestination(type: WorshipReminderType): Route = when (type) {
    WorshipReminderType.ADHKAR_MORNING -> Route.DuaCategory(DUA_CATEGORY_MORNING_ADHKAR)
    WorshipReminderType.ADHKAR_EVENING -> Route.DuaCategory(DUA_CATEGORY_EVENING_ADHKAR)

    WorshipReminderType.TAHAJJUD,
    WorshipReminderType.WITR -> Route.NightWorship

    WorshipReminderType.SUHOOR,
    WorshipReminderType.IFTAR,
    WorshipReminderType.MONDAY_THURSDAY_FAST,
    WorshipReminderType.WHITE_DAYS_FAST,
    WorshipReminderType.ARAFAH_ASHURA_FAST -> Route.FastingTracker

    WorshipReminderType.TARAWEEH,
    WorshipReminderType.LAYLATUL_QADR -> Route.DuaCategory(DUA_CATEGORY_FASTING_RAMADAN)
}

/** Seeded dua category ids referenced by [worshipCardDestination]. */
private const val DUA_CATEGORY_MORNING_ADHKAR = "1"
private const val DUA_CATEGORY_EVENING_ADHKAR = "2"
private const val DUA_CATEGORY_FASTING_RAMADAN = "37"

/** The seeded category holding the Witr / night-prayer duas, linked from the night worship hub. */
const val DUA_CATEGORY_WITR_AND_NIGHT_PRAYER = "35"
