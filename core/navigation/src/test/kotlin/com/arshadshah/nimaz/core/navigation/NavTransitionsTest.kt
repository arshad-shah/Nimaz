package com.arshadshah.nimaz.core.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The two properties the screen transitions have to hold, and nothing about how they look.
 *
 * There is no assertion here on the slide distance, the easing or the exact duration — those are
 * taste, and a test that pins them turns every visual adjustment into a test edit. What is pinned
 * is what a user would file a bug about: that the animation setting actually switches them off,
 * and that going deeper does not look identical to coming back, which is the whole reason
 * `NavHost`'s default crossfade was replaced.
 */
class NavTransitionsTest {

    // ── The setting ─────────────────────────────────────────────────────────────

    @Test
    fun `with animations off every transition is a hard cut`() {
        // Not a faster animation — none. Someone who turned this off did so because motion makes
        // them ill or because the device cannot afford it; a 60 ms slide serves neither.
        assertThat(NimazNavTransitions.enter(enabled = false)).isSameInstanceAs(EnterTransition.None)
        assertThat(NimazNavTransitions.popEnter(enabled = false))
            .isSameInstanceAs(EnterTransition.None)
        assertThat(NimazNavTransitions.exit(enabled = false)).isSameInstanceAs(ExitTransition.None)
        assertThat(NimazNavTransitions.popExit(enabled = false))
            .isSameInstanceAs(ExitTransition.None)
    }

    @Test
    fun `with animations on every transition actually animates`() {
        assertThat(NimazNavTransitions.enter(enabled = true))
            .isNotSameInstanceAs(EnterTransition.None)
        assertThat(NimazNavTransitions.popEnter(enabled = true))
            .isNotSameInstanceAs(EnterTransition.None)
        assertThat(NimazNavTransitions.exit(enabled = true))
            .isNotSameInstanceAs(ExitTransition.None)
        assertThat(NimazNavTransitions.popExit(enabled = true))
            .isNotSameInstanceAs(ExitTransition.None)
    }

    // ── Direction ───────────────────────────────────────────────────────────────

    @Test
    fun `going deeper does not look the same as coming back`() {
        // The bug this replaced: `NavHost`'s default is the same crossfade both ways, so the
        // back stack had no visible direction. Forward and pop must differ on both halves.
        assertThat(NimazNavTransitions.enter(enabled = true))
            .isNotEqualTo(NimazNavTransitions.popEnter(enabled = true))
        assertThat(NimazNavTransitions.exit(enabled = true))
            .isNotEqualTo(NimazNavTransitions.popExit(enabled = true))
    }

    @Test
    fun `a transition is shorter than the default it replaces`() {
        // Navigation Compose's fallback is a 700 ms crossfade, which reads as lag on a settings
        // row rather than as a screen change.
        assertThat(NimazNavTransitions.DURATION_MS).isLessThan(700)
    }
}
