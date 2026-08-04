package com.arshadshah.nimaz.presentation.viewmodel.worship

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.first
import kotlin.time.Instant

/**
 * State for the night worship hub.
 *
 * Deliberately **free of "now"**: it publishes the night's *instants* and lets the screen derive
 * every countdown and open/closed judgement at the leaf from the shared ticker
 * (`rememberNow`). This is the same rule the Home rework established — a ViewModel that pushes
 * elapsed time as state is what produced the frozen countdowns in the first place.
 *
 * @param lastThirdAt when the last third of the *live* night begins (adhan2 `SunnahTimes`).
 * @param fajrAt the Fajr that closes the live night — the morning after it began, which is *today's*
 *   Fajr in the pre-dawn hours and *tomorrow's* once today's Fajr has passed.
 * @param ishaAt the live night's Isha, the earliest sensible start for Witr.
 * @param rakahCount in-memory tally for the current visit. Not persisted: we have no data on how
 *   people actually use this yet, and inventing a "completed night" model before that would be
 *   guessing. If the count turns out to be worth keeping, that is an easy follow-up.
 */
data class NightWorshipUiState(
    val isLoading: Boolean = true,
    val lastThirdAt: Instant? = null,
    val fajrAt: Instant? = null,
    val ishaAt: Instant? = null,
    val rakahCount: Int = 0,
    val error: String? = null,
)
