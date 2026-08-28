package com.arshadshah.nimaz.core.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.IntOffset

/**
 * The screen transitions every destination animates with.
 *
 * ## Why this exists
 *
 * `NavHost` was declared with no transition arguments at all, so all 94 destinations used
 * Navigation Compose's fallback: a 700 ms crossfade, in both directions, with no sense of
 * direction. Going deeper and coming back looked identical, and at 700 ms a tap on a settings row
 * read as a pause before the screen changed rather than as a screen changing.
 *
 * What replaces it is a **shared axis along X with parallax**: the arriving screen travels further
 * than the departing one, so forward and back are distinguishable at a glance and the stack has a
 * direction. The fade runs at half the slide's duration, which stops the two screens being
 * simultaneously half-visible through each other for the whole transition.
 *
 * ## The animation setting
 *
 * Every function takes `enabled` rather than reading a preference, because the preference lives in
 * `:core:datastore` and the `CompositionLocal` for it lives in `:core:ui` — and this module
 * depends on neither, deliberately (see the note in its build file). The caller reads
 * `LocalAnimationsEnabled` and passes the answer down; `NavGraph` in `:app` is the one caller,
 * and it sits below both modules.
 *
 * With animations off these return [EnterTransition.None] / [ExitTransition.None] — a hard cut,
 * not a shorter animation. Someone who turns animations off has usually done it because motion
 * makes them ill or because the device is slow; a 60 ms version of the same movement serves
 * neither.
 *
 * They are plain functions rather than the `AnimatedContentTransitionScope` lambdas `NavHost`
 * takes, because the receiver is never read — none of the builders below is a member of it — and
 * requiring one would mean a mocked scope in every test for no gain. `NavGraph` wraps them.
 */
object NimazNavTransitions {

    /** Long enough to read as movement, short enough not to be a wait. */
    const val DURATION_MS = 300

    /** The fade finishes early so the crossover is not a half-transparent double image. */
    private const val FADE_MS = DURATION_MS / 2

    /** The arriving screen's travel, as a fraction of the container's width. */
    private const val ARRIVING_TRAVEL = 4

    /** The departing screen's, deliberately smaller — that difference is the parallax. */
    private const val DEPARTING_TRAVEL = 6

    private val slide = tween<IntOffset>(DURATION_MS, easing = FastOutSlowInEasing)
    private val fade = tween<Float>(FADE_MS, easing = FastOutSlowInEasing)

    /** Navigating deeper: the new screen arrives from the trailing edge. */
    fun enter(enabled: Boolean): EnterTransition =
        if (!enabled) {
            EnterTransition.None
        } else {
            slideInHorizontally(slide) { width -> width / ARRIVING_TRAVEL } + fadeIn(fade)
        }

    /** Navigating deeper: the screen behind it slips the other way, and less far. */
    fun exit(enabled: Boolean): ExitTransition =
        if (!enabled) {
            ExitTransition.None
        } else {
            slideOutHorizontally(slide) { width -> -width / DEPARTING_TRAVEL } + fadeOut(fade)
        }

    /** Coming back: the screen being returned to arrives from the leading edge. */
    fun popEnter(enabled: Boolean): EnterTransition =
        if (!enabled) {
            EnterTransition.None
        } else {
            slideInHorizontally(slide) { width -> -width / DEPARTING_TRAVEL } + fadeIn(fade)
        }

    /** Coming back: the screen being left goes out the way it came in. */
    fun popExit(enabled: Boolean): ExitTransition =
        if (!enabled) {
            ExitTransition.None
        } else {
            slideOutHorizontally(slide) { width -> width / ARRIVING_TRAVEL } + fadeOut(fade)
        }
}
