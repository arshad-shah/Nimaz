package com.arshadshah.nimaz.presentation.theme

import androidx.compose.ui.unit.Dp
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The adaptive layout table — what a phone, a foldable and a tablet each get.
 *
 * Every one of these is a `when` over the window size class, and they are read from dozens of
 * screens. `Dp.Unspecified` is the compact answer throughout, and it is not a "no value" sentinel
 * — it means *do not clamp*, so a phone screen fills its width while a tablet's content stays
 * inside a readable measure. Returning a real number there would put a 600dp column in the middle
 * of a 411dp phone; returning `Unspecified` on a tablet would stretch a settings form across
 * thirteen inches. Neither throws, and neither shows up on the device most people develop on.
 *
 * Robolectric's `@Config(qualifiers = …)` is per-method here, because the whole point is that the
 * same call returns different answers at different widths — one class-level width could only ever
 * test one third of the table.
 */
@RunWith(RobolectricTestRunner::class)
class AdaptiveSpacingTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private class Resolved(
        val screenPadding: Dp,
        val maxContent: Dp,
        val maxReadable: Dp,
        val maxForm: Dp,
        val maxSearchBar: Dp,
        val sectionSpacing: Dp,
        val cardCorner: Dp,
        val statsColumnsForTen: Int,
        val statsColumnsForTwo: Int,
    )

    /** Every value in one composition — a rule takes one `setContent` (#604). */
    private fun resolve(): Resolved {
        lateinit var out: Resolved
        composeRule.setThemedContent {
            out = Resolved(
                screenPadding = AdaptiveSpacing.screenPadding(),
                maxContent = AdaptiveSpacing.maxContentWidth(),
                maxReadable = AdaptiveSpacing.maxReadableWidth(),
                maxForm = AdaptiveSpacing.maxFormWidth(),
                maxSearchBar = AdaptiveSpacing.maxSearchBarWidth(),
                sectionSpacing = AdaptiveSpacing.sectionSpacing(),
                cardCorner = AdaptiveSpacing.cardCornerRadius(),
                statsColumnsForTen = AdaptiveSpacing.statsGridColumns(10),
                statsColumnsForTwo = AdaptiveSpacing.statsGridColumns(2),
            )
        }
        composeRule.waitForIdle()
        return out
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `a phone is never clamped and gets the tighter spacing`() {
        val phone = resolve()

        // `Unspecified` means "do not clamp" — a real number here centres a narrow column on a
        // phone with dead margins either side.
        assertThat(phone.maxContent).isEqualTo(Dp.Unspecified)
        assertThat(phone.maxReadable).isEqualTo(Dp.Unspecified)
        assertThat(phone.maxForm).isEqualTo(Dp.Unspecified)
        assertThat(phone.maxSearchBar).isEqualTo(Dp.Unspecified)
        assertThat(phone.screenPadding.value).isEqualTo(20f)
        assertThat(phone.sectionSpacing.value).isEqualTo(16f)
        assertThat(phone.cardCorner.value).isEqualTo(14f)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `a phone shows at most three stats across`() {
        // Four stat tiles at phone width are unreadable; the cap is what turns a wide grid into
        // rows. `coerceAtMost` also has to leave a smaller set alone — two stats must stay two.
        val phone = resolve()

        assertThat(phone.statsColumnsForTen).isEqualTo(3)
        assertThat(phone.statsColumnsForTwo).isEqualTo(2)
    }

    @Test
    @Config(qualifiers = "w700dp-h1200dp")
    fun `a medium window clamps to the middle measure`() {
        // The arm reached by a foldable and a small tablet — neither compact nor expanded, and the
        // one an `if/else` over `isCompact` alone would silently drop. 700dp, because Material's
        // expanded-width breakpoint is 840dp and a qualifier at or above it lands in the other arm.
        val medium = resolve()

        assertThat(medium.maxContent.value).isEqualTo(700f)
        assertThat(medium.maxReadable.value).isEqualTo(700f)
        assertThat(medium.maxForm.value).isEqualTo(600f)
        assertThat(medium.statsColumnsForTen).isEqualTo(4)
        assertThat(medium.screenPadding.value).isEqualTo(32f)
    }

    @Test
    @Config(qualifiers = "w1400dp-h1000dp")
    fun `an expanded window gets the widest content measure`() {
        val expanded = resolve()

        assertThat(expanded.maxContent.value).isEqualTo(800f)
        assertThat(expanded.statsColumnsForTen).isEqualTo(6)
        // Reading measure does *not* widen with the window — prose past ~700dp is hard to track
        // back to the next line, which is why it is a separate value from the content width.
        assertThat(expanded.maxReadable.value).isEqualTo(700f)
        assertThat(expanded.maxForm.value).isEqualTo(600f)
    }

    @Test
    @Config(qualifiers = "w1400dp-h1000dp")
    fun `a stat grid never invents columns it has no data for`() {
        val expanded = resolve()

        assertThat(expanded.statsColumnsForTwo).isEqualTo(2)
    }
}
