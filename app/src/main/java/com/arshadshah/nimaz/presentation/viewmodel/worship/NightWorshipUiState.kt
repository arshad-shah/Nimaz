package com.arshadshah.nimaz.presentation.viewmodel.worship

import androidx.lifecycle.ViewModel
import com.arshadshah.nimaz.presentation.viewmodel.UiError
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
 *
 * `ishaAt` was here too, populated by a **third full astronomical pass** whose result nothing
 * read. Its KDoc described it as "the earliest sensible start for Witr" — a feature that was
 * never built. The field and the pass are gone; if Witr guidance is wanted later, the pass costs
 * nothing to add back next to the two that are used.
 * @param rakahCount in-memory tally for the current visit. Not persisted: we have no data on how
 *   people actually use this yet, and inventing a "completed night" model before that would be
 *   guessing. If the count turns out to be worth keeping, that is an easy follow-up.
 */
data class NightWorshipUiState(
    val isLoading: Boolean = true,
    val lastThirdAt: Instant? = null,
    val fajrAt: Instant? = null,
    val rakahCount: Int = 0,
    val error: UiError? = null,
)
