package com.arshadshah.nimaz.domain.model

import kotlin.time.Instant

/**
 * Everything a prayer-time calculation needs, resolved once from the user's preferences.
 *
 * Five ViewModels used to assemble this themselves, each injecting the concrete
 * `core/util/PrayerTimeCalculator` and each reading the same six preference flows. Four of them
 * parsed the persisted strings with their own `try { CalculationMethod.valueOf(s) } catch { MWL }`,
 * and the fifth — `FastingViewModel` — did not read them at all: it called `getPrayerTimes(lat, lng)`
 * and took all four defaults, so Fast Tracker's suhoor and iftar ignored every calculation
 * preference the user had set while Home honoured them. One snapshot, resolved in one place, is
 * what makes that class of divergence impossible rather than merely fixed.
 *
 * [location] is already resolved through `resolveLocation`, so it is never the unset (0, 0) — a
 * caller cannot accidentally compute prayer times off the coast of Ghana.
 */
data class PrayerCalculationSettings(
    val location: ResolvedLocation,
    val calculationMethod: CalculationMethod,
    val asrCalculation: AsrCalculation,
    val highLatitudeRule: HighLatitudeRule?,
    val adjustments: Map<PrayerType, Int>,
)

/**
 * The two Sunnah night instants for one night: the middle of the night, and the start of its last
 * third. Both are computed from that evening's Maghrib to the next morning's Fajr, so both land in
 * the early hours of the *following* day.
 *
 * A domain type rather than the calculator's own, so a repository can return it without the domain
 * layer naming anything in `core/util`.
 */
data class SunnahNightTimes(
    val middleOfTheNight: Instant,
    val lastThirdOfTheNight: Instant,
)
