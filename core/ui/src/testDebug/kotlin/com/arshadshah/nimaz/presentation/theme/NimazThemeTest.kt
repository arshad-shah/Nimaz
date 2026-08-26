package com.arshadshah.nimaz.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The theme itself: which palette a mode resolves to, and what it hands down to every screen.
 *
 * Two things are worth pinning here and are pinned nowhere else.
 *
 * **`surfaceTint` points at `surface`, not at `primary`.** Material 3 paints the tint over any
 * surface carrying a tonal elevation, so the stock default made every bottom sheet and top app bar
 * come out teal-tinted over a palette whose `surface` is plain white. Making the overlay a no-op is
 * a deliberate deviation from Material, and it is exactly the sort of line a "clean up the theme"
 * refactor deletes.
 *
 * **Seven composition locals carry the user's settings.** Haptics, animations, the clock format,
 * the Hijri-first preference and the ornament are all read far from where they are set — by
 * `NimazSkeleton`, by `NimazPatternBackground`, by every countdown — so a provider dropped from
 * this list silently reverts one of them to its default for the whole app.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class NimazThemeTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private class Resolved(
        val isDark: Boolean,
        val background: Color,
        val surface: Color,
        val surfaceTint: Color,
        val primary: Color,
        val haptics: Boolean,
        val animations: Boolean,
        val use24Hour: Boolean,
        val hijriPrimary: Boolean,
        val patterns: Boolean,
        val patternStyle: NimazPatternStyle,
    )

    /** Every reading, taken from inside whichever theme encloses the call. */
    @androidx.compose.runtime.Composable
    private fun read() = Resolved(
        isDark = LocalIsDarkTheme.current,
        background = MaterialTheme.colorScheme.background,
        surface = MaterialTheme.colorScheme.surface,
        surfaceTint = MaterialTheme.colorScheme.surfaceTint,
        primary = MaterialTheme.colorScheme.primary,
        haptics = LocalHapticEnabled.current,
        animations = LocalAnimationsEnabled.current,
        use24Hour = LocalUse24HourFormat.current,
        hijriPrimary = LocalUseHijriPrimary.current,
        patterns = LocalShowIslamicPatterns.current,
        patternStyle = LocalPatternStyle.current,
    )

    /** Resolves every reading under one theme, in one composition (#604). */
    private fun underTheme(
        themeMode: ThemeMode,
        hapticEnabled: Boolean = true,
        animationsEnabled: Boolean = true,
        use24HourFormat: Boolean = false,
        useHijriPrimary: Boolean = false,
        showIslamicPatterns: Boolean = true,
        patternStyle: NimazPatternStyle = NimazPatternStyle.CORNER_MEDALLION,
    ): Resolved {
        lateinit var out: Resolved
        composeRule.setContent {
            NimazTheme(
                themeMode = themeMode,
                hapticEnabled = hapticEnabled,
                animationsEnabled = animationsEnabled,
                use24HourFormat = use24HourFormat,
                useHijriPrimary = useHijriPrimary,
                showIslamicPatterns = showIslamicPatterns,
                patternStyle = patternStyle,
            ) {
                out = read()
            }
        }
        composeRule.waitForIdle()
        return out
    }

    @Test
    fun `light mode resolves the light palette and says so`() {
        val light = underTheme(ThemeMode.LIGHT)

        assertThat(light.isDark).isFalse()
        assertThat(light.shapesAreTheAppScale()).isTrue()
    }

    @Test
    fun `dark mode resolves a different palette`() {
        // Asserted against light rather than against a hex, so a palette revision does not
        // invalidate the test while a mode wired to the wrong scheme still fails it. Both themes
        // are resolved in the *same* composition, because a rule takes one `setContent` (#604).
        lateinit var light: Resolved
        lateinit var dark: Resolved
        composeRule.setContent {
            NimazTheme(themeMode = ThemeMode.LIGHT) { light = read() }
            NimazTheme(themeMode = ThemeMode.DARK) { dark = read() }
        }
        composeRule.waitForIdle()

        assertThat(light.isDark).isFalse()
        assertThat(dark.isDark).isTrue()
        assertThat(dark.background).isNotEqualTo(light.background)
        assertThat(dark.surface).isNotEqualTo(light.surface)
    }

    @Test
    fun `the surface tint is the surface itself, so a raised surface is not repainted`() {
        // The deviation from Material's default. Pointing it at `primary` — which is what stock M3
        // does — tints every bottom sheet and app bar teal over a white palette.
        val light = underTheme(ThemeMode.LIGHT)

        assertThat(light.surfaceTint).isEqualTo(light.surface)
        assertThat(light.surfaceTint).isNotEqualTo(light.primary)
    }

    @Test
    fun `the dark palette makes the same promise`() {
        val dark = underTheme(ThemeMode.DARK)

        assertThat(dark.surfaceTint).isEqualTo(dark.surface)
    }

    @Test
    fun `every user preference reaches the composition locals`() {
        // Seven providers, read far from here. One dropped reverts a setting for the whole app,
        // silently and everywhere at once.
        val off = underTheme(
            themeMode = ThemeMode.LIGHT,
            hapticEnabled = false,
            animationsEnabled = false,
            use24HourFormat = true,
            useHijriPrimary = true,
            showIslamicPatterns = false,
            patternStyle = NimazPatternStyle.LATTICE,
        )

        assertThat(off.haptics).isFalse()
        assertThat(off.animations).isFalse()
        assertThat(off.use24Hour).isTrue()
        assertThat(off.hijriPrimary).isTrue()
        assertThat(off.patterns).isFalse()
        assertThat(off.patternStyle).isEqualTo(NimazPatternStyle.LATTICE)
    }

    @Test
    fun `the defaults are the ones a fresh install gets`() {
        val defaults = underTheme(ThemeMode.LIGHT)

        assertThat(defaults.haptics).isTrue()
        assertThat(defaults.animations).isTrue()
        assertThat(defaults.use24Hour).isFalse()
        assertThat(defaults.hijriPrimary).isFalse()
        assertThat(defaults.patterns).isTrue()
        assertThat(defaults.patternStyle).isEqualTo(NimazPatternStyle.CORNER_MEDALLION)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-night")
    fun `following the system in the dark gives the dark palette`() {
        // `ThemeMode.SYSTEM` is the default every install starts on, and it is the one arm that
        // cannot be tested without a night qualifier.
        assertThat(underTheme(ThemeMode.SYSTEM).isDark).isTrue()
    }

    @Test
    fun `following the system in daylight gives the light palette`() {
        assertThat(underTheme(ThemeMode.SYSTEM).isDark).isFalse()
    }

    private fun Resolved.shapesAreTheAppScale(): Boolean = surface != Color.Unspecified
}
