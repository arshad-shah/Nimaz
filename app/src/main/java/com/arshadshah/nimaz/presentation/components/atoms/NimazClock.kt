package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.arshadshah.nimaz.core.util.CountdownParts
import com.arshadshah.nimaz.core.util.EventProximity
import com.arshadshah.nimaz.core.util.countdownTo
import kotlinx.coroutines.delay
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

/**
 * The app's single clock.
 *
 * ## Why this exists
 *
 * Elapsed time is not state — it is a continuously varying *input*. Modelling
 * it as `StateFlow<HomeUiState>` fields (`timeUntilNextPrayer: String`) meant a
 * ViewModel loop pushed a whole new UI state once a second, every consumer of
 * that state recomposed at 1 Hz, and the tick was coupled to the expensive
 * recompute path — which is what let #319 wedge a suspending resolver inside
 * the tick and flash the loading spinner (#321).
 *
 * Here time flows the other way: one ticker, read by whoever needs it, at
 * whatever resolution *they* need. ViewModels go back to publishing facts
 * (instants), and countdown text is derived at the leaf.
 *
 * ## Replaces
 *
 *  - `HomeViewModel.startTimeUpdates()`        — 1 s `viewModelScope` loop
 *  - `HomeViewModel.startWorshipUpdates()`     — 60 s `viewModelScope` loop
 *  - `PrayerTimesViewModel`'s `applyTick()` loop
 *  - `HomeHero`'s private 30 s `LaunchedEffect` wall-clock loop
 *  - `WidgetsScreen`'s 1 s preview loop
 *
 * Five independent loops become one. Because it is a `produceState` tied to the
 * composition rather than a `viewModelScope` loop, it also removes the
 * endless-loop-in-viewModelScope problem that forced `HomeViewModelTest` to
 * hand-drive a bounded `TestCoroutineScheduler`.
 *
 * ## Two properties that matter
 *
 * **Wall-clock alignment.** `delay(1_000)` in a loop accumulates drift, so the
 * displayed second wanders off the real one and visibly skips. This ticker
 * sleeps only to the *next* boundary, staying locked to the system clock
 * however long a frame took.
 *
 * **Resolution-derived reads.** The source ticks at the finest resolution any
 * consumer needs, but [rememberNow] returns a `derivedStateOf` truncated to the
 * caller's resolution. A card counting whole minutes invalidates once a minute,
 * not sixty times — the difference between one shared ticker being cheap and
 * being a disaster.
 */
enum class TickResolution(val millis: Long) {
    SECONDS(1_000L),
    MINUTES(60_000L),
    ;

    /** Truncate [instant] down to this resolution's boundary. */
    fun truncate(instant: Instant): Instant {
        val ms = instant.toEpochMilliseconds()
        return Instant.fromEpochMilliseconds(ms - Math.floorMod(ms, millis))
    }
}

/**
 * Null when no [ProvideNimazClock] is above us. [rememberNow] then falls back to
 * a local ticker, so previews, tests and one-off screens still animate without
 * needing the provider — they just don't share it.
 */
private val LocalNowSource = compositionLocalOf<State<Instant>?> { null }

/**
 * Install the shared ticker. Wrap this immediately inside `NimazTheme` in
 * `MainActivity` so every screen shares one coroutine.
 *
 * The ticker is suspended below `STARTED`, so a backgrounded app stops ticking
 * rather than churning state behind a screen nobody is looking at. Pair this
 * with migrating screens from `collectAsStateWithLifecycle()` to
 * `collectAsStateWithLifecycle()` — 84 call sites still use the former.
 */
@Composable
fun ProvideNimazClock(
    resolution: TickResolution = TickResolution.SECONDS,
    timeSource: () -> Instant = SystemTimeSource,
    content: @Composable () -> Unit,
) {
    val source = rememberTickingInstant(resolution, timeSource)
    CompositionLocalProvider(LocalNowSource provides source, content = content)
}

/**
 * The real clock. Named rather than inlined as a lambda so it is a stable value: passing
 * `{ Clock.System.now() }` as a default would allocate a new lambda on every recomposition and
 * re-key [rememberTickingInstant], restarting the ticker each time.
 *
 * Tests substitute their own source to drive time deterministically — see `NimazClockTest`. Without
 * that seam nothing can assert the app's timers ever advance, which is how a frozen ticker shipped
 * unnoticed.
 */
val SystemTimeSource: () -> Instant = { Clock.System.now() }

/**
 * The current instant, updating at [resolution].
 *
 * Reading this makes the calling composable — and only it — recompose on each
 * boundary. Keep the read as close to the text that displays it as possible;
 * hoisting it to a screen root reintroduces exactly the whole-tree-per-second
 * problem this replaces.
 */
@Composable
fun rememberNow(resolution: TickResolution = TickResolution.SECONDS): State<Instant> {
    val source = LocalNowSource.current ?: rememberTickingInstant(resolution)
    return remember(source, resolution) {
        derivedStateOf { resolution.truncate(source.value) }
    }
}

/**
 * A countdown to [target] that picks its own tick resolution: whole minutes
 * while the target is far off, seconds once it is within [fineGrainedWithin].
 *
 * This is the ergonomic default for cards — a smooth final approach without
 * paying for 1 Hz recomposition all day.
 *
 * ## Match [fineGrainedWithin] to what you actually render
 *
 * The tick resolution and the *displayed* resolution are two different things, and letting them
 * disagree is what made the Home hero's countdown look frozen: it rendered a seconds digit while
 * this function, with the default 15-minute threshold, only re-derived once a minute. The seconds
 * sat still for 60 s and then jumped — and any unrelated recomposition (navigating back to the
 * screen) made it look like the value only updated when you moved around the app.
 *
 * So: **if the caller shows seconds, it must pass [Duration.INFINITE]** to tick every second at any
 * distance. [NimazCountdownText] derives this from its own `showSeconds` flag so call sites cannot
 * get it wrong; hand-rolled callers must keep the two in step themselves.
 */
@Composable
fun rememberCountdownTo(
    target: Instant,
    fineGrainedWithin: Duration = EventProximity.IMMINENT_THRESHOLD,
): State<CountdownParts> {
    // Coarse read first — on its own this invalidates only once a minute.
    val coarse by rememberNow(TickResolution.MINUTES)
    val needsSeconds = (target - coarse) <= fineGrainedWithin
    val nowState = rememberNow(
        if (needsSeconds) TickResolution.SECONDS else TickResolution.MINUTES
    )
    // Keyed on `nowState` as well as `target`: flipping resolution hands back a
    // different State instance, and the derivation must follow it.
    return remember(target, nowState) {
        derivedStateOf { countdownTo(target, nowState.value) }
    }
}

/**
 * The raw ticker: emits at every wall-clock boundary of [resolution], and only
 * while the lifecycle is at least STARTED.
 */
@Composable
private fun rememberTickingInstant(
    resolution: TickResolution,
    timeSource: () -> Instant = SystemTimeSource,
): State<Instant> {
    val lifecycleOwner = LocalLifecycleOwner.current
    return produceState(timeSource(), resolution, lifecycleOwner, timeSource) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                val now = timeSource()
                value = now
                // Sleep to the next boundary rather than a fixed interval, so
                // the tick never drifts off the system clock.
                val step = resolution.millis
                delay(step - Math.floorMod(now.toEpochMilliseconds(), step))
            }
        }
    }
}
