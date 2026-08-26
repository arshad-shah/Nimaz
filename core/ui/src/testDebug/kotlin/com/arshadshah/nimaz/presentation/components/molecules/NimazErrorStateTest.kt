package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The app's one failure state, at all three scales.
 *
 * **Why this is worth pinning.** `NimazErrorState` is what every screen in Nimaz shows when
 * something did not work, and the contract it exists to enforce is an *ordering* — what happened,
 * why, what to do next — plus a promise that the technical detail stays hidden until someone asks
 * for it. Both are the kind of thing that survives a refactor visually and breaks silently: a
 * `details` block wired to render unconditionally leaks a stack trace onto a reader's screen, and
 * a `primaryAction` whose lambda stops being wired leaves a "Try again" button that does nothing.
 * `:feature:calendar` (#602) already pins that a failed section read renders as a `SECTION` above
 * content that still draws — this is the component that has to honour that variant.
 *
 * `animated = false` throughout: the medallion carries two `rememberInfiniteTransition`
 * rotations, so a composition that leaves them running never lets the test clock idle (#604).
 * The drawing itself is covered by `NimazErrorStateArtTest`, which needs them frozen anyway.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class NimazErrorStateTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private fun show(content: @androidx.compose.runtime.Composable () -> Unit) {
        composeRule.setThemedContent {
            Box(modifier = Modifier.fillMaxSize()) { content() }
        }
    }

    @Test
    fun `a fullscreen failure says what happened and why`() {
        show {
            NimazErrorState(
                title = "Prayer times couldn't refresh",
                message = "Nimaz is showing yesterday's calculated times.",
                animated = false,
            )
        }

        composeRule.onNodeWithText("Prayer times couldn't refresh").assertIsDisplayed()
        composeRule.onNodeWithText("Nimaz is showing yesterday's calculated times.")
            .assertIsDisplayed()
    }

    @Test
    fun `a title with no message renders on its own`() {
        // The `message == null` arm. A component that assumed a message would blow up here, and
        // the not-found inline case in the showcase passes exactly this shape.
        show { NimazErrorState(title = "No surah matches that search", animated = false) }

        composeRule.onNodeWithText("No surah matches that search").assertExists()
    }

    @Test
    fun `technical detail is hidden until it is asked for`() {
        // The whole point of the details toggle: a reader who cannot use a stack trace never sees
        // one, and a reader filing a bug can still get at it.
        val trace = "java.net.UnknownHostException: Unable to resolve host"
        show {
            NimazErrorState(
                title = "Couldn't reach the server",
                details = trace,
                animated = false,
            )
        }

        composeRule.onNodeWithText(trace).assertDoesNotExist()
        composeRule.onNodeWithText("Show details").performClick()
        composeRule.onNodeWithText(trace).assertExists()
    }

    @Test
    fun `the details toggle flips its own label back`() {
        show {
            NimazErrorState(
                title = "Couldn't reach the server",
                details = "HTTP 503",
                animated = false,
            )
        }

        composeRule.onNodeWithText("Show details").performClick()
        composeRule.onNodeWithText("Hide details").assertExists()
        composeRule.onNodeWithText("Hide details").performClick()
        composeRule.onNodeWithText("Show details").assertExists()
    }

    @Test
    fun `no details means no toggle at all`() {
        show { NimazErrorState(title = "Bookmarks didn't load", animated = false) }

        composeRule.onNodeWithText("Show details").assertDoesNotExist()
    }

    @Test
    fun `the primary action runs the caller's retry`() {
        var retries = 0
        show {
            NimazErrorState(
                title = "Bookmarks didn't load",
                primaryAction = NimazErrorDefaults.retry(onRetry = { retries++ }),
                animated = false,
            )
        }

        composeRule.onNodeWithText("Try again").performClick()
        assertThat(retries).isEqualTo(1)
    }

    @Test
    fun `retry takes a caller's own label`() {
        show {
            NimazErrorState(
                title = "Reciter unavailable",
                primaryAction = NimazErrorDefaults.retry(onRetry = {}, label = "Download again"),
                animated = false,
            )
        }

        composeRule.onNodeWithText("Download again").assertExists()
        composeRule.onNodeWithText("Try again").assertDoesNotExist()
    }

    @Test
    fun `both actions are offered and each runs its own lambda`() {
        var retried = false
        var escaped = false
        show {
            NimazErrorState(
                title = "Prayer times couldn't refresh",
                primaryAction = NimazErrorDefaults.retry(onRetry = { retried = true }),
                secondaryAction = NimazErrorAction(
                    label = "Use offline times",
                    onClick = { escaped = true },
                ),
                animated = false,
            )
        }

        composeRule.onNodeWithText("Use offline times").performClick()
        assertThat(escaped).isTrue()
        assertThat(retried).isFalse()

        composeRule.onNodeWithText("Try again").performClick()
        assertThat(retried).isTrue()
    }

    @Test
    fun `a secondary action alone still renders the action row`() {
        // The `primaryAction == null && secondaryAction != null` arm — the row is gated on
        // *either* being present, and reading the condition the other way loses this case.
        show {
            NimazErrorState(
                title = "Location is off",
                secondaryAction = NimazErrorAction(label = "Go back", onClick = {}),
                animated = false,
            )
        }

        composeRule.onNodeWithText("Go back").assertExists()
    }

    @Test
    fun `a section failure keeps the surrounding content on screen`() {
        // What `:feature:calendar` (#602) pins from the other side: a failed section is a panel in
        // the page, not a takeover.
        show {
            androidx.compose.foundation.layout.Column {
                androidx.compose.material3.Text("Today's prayers")
                NimazErrorState(
                    title = "Bookmarks didn't load",
                    variant = NimazErrorVariant.SECTION,
                    animated = false,
                )
            }
        }

        composeRule.onNodeWithText("Today's prayers").assertIsDisplayed()
        composeRule.onNodeWithText("Bookmarks didn't load").assertIsDisplayed()
    }

    @Test
    fun `a section failure still offers its details and its actions`() {
        show {
            NimazErrorState(
                title = "Bookmarks didn't load",
                message = "Something went wrong reading your saved verses.",
                variant = NimazErrorVariant.SECTION,
                details = "SQLiteException",
                primaryAction = NimazErrorDefaults.retry(onRetry = {}),
                secondaryAction = NimazErrorAction(label = "Dismiss", onClick = {}),
                animated = false,
            )
        }

        composeRule.onNodeWithText("Try again").assertExists()
        composeRule.onNodeWithText("Dismiss").assertExists()
        composeRule.onNodeWithText("Show details").performClick()
        composeRule.onNodeWithText("SQLiteException").assertExists()
    }

    @Test
    fun `an inline failure renders its message and its one action`() {
        var enabled = false
        show {
            NimazErrorState(
                title = "Location is off",
                message = "Qibla needs your location to point correctly.",
                kind = NimazErrorKind.LOCATION,
                variant = NimazErrorVariant.INLINE,
                primaryAction = NimazErrorAction(label = "Enable", onClick = { enabled = true }),
                animated = false,
            )
        }

        composeRule.onNodeWithText("Location is off").assertExists()
        composeRule.onNodeWithText("Qibla needs your location to point correctly.").assertExists()
        composeRule.onNodeWithText("Enable").performClick()
        assertThat(enabled).isTrue()
    }

    @Test
    fun `an inline failure ignores details rather than rendering a toggle it has no room for`() {
        // Documented behaviour: INLINE is one row, so `details` is dropped. A change that started
        // honouring it here would push a stack-trace toggle into a list row.
        show {
            NimazErrorState(
                title = "Couldn't save",
                variant = NimazErrorVariant.INLINE,
                details = "IOException",
                animated = false,
            )
        }

        composeRule.onNodeWithText("Show details").assertDoesNotExist()
        composeRule.onNodeWithText("IOException").assertDoesNotExist()
    }

    @Test
    fun `an inline failure with no action renders just the text`() {
        show {
            NimazErrorState(
                title = "No surah matches that search",
                kind = NimazErrorKind.NOT_FOUND,
                variant = NimazErrorVariant.INLINE,
                animated = false,
            )
        }

        composeRule.onNodeWithText("No surah matches that search").assertExists()
        composeRule.onNodeWithText("Try again").assertDoesNotExist()
    }

    @Test
    fun `every kind renders inline, whatever tone it carries`() {
        // The inline well colours off a `when` over the tone, and the kinds between them reach
        // ERROR, WARNING and MUTED. An arm that fell through would throw here rather than in a
        // screen nobody tests.
        show {
            androidx.compose.foundation.layout.Column {
                NimazErrorKind.entries.forEach { kind ->
                    NimazErrorState(
                        title = kind.name,
                        kind = kind,
                        variant = NimazErrorVariant.INLINE,
                        animated = false,
                    )
                }
            }
        }

        NimazErrorKind.entries.forEach { kind ->
            composeRule.onNodeWithText(kind.name).assertExists()
        }
    }

    @Test
    fun `a caller can override the tone the kind picked`() {
        // The `tone ?: kind.tone` arm, and the inline well's SUCCESS/ACCENT branches that no kind
        // reaches on its own.
        show {
            androidx.compose.foundation.layout.Column {
                NimazErrorState(
                    title = "Recovered",
                    kind = NimazErrorKind.GENERIC,
                    tone = NimazTone.SUCCESS,
                    variant = NimazErrorVariant.INLINE,
                    animated = false,
                )
                NimazErrorState(
                    title = "Accented",
                    tone = NimazTone.ACCENT,
                    variant = NimazErrorVariant.INLINE,
                    animated = false,
                )
                NimazErrorState(
                    title = "Prominent",
                    tone = NimazTone.PROMINENT,
                    variant = NimazErrorVariant.INLINE,
                    animated = false,
                )
                NimazErrorState(
                    title = "Neutral",
                    tone = NimazTone.NEUTRAL,
                    variant = NimazErrorVariant.INLINE,
                    animated = false,
                )
            }
        }

        composeRule.onNodeWithText("Recovered").assertExists()
        composeRule.onNodeWithText("Accented").assertExists()
        composeRule.onNodeWithText("Prominent").assertExists()
        composeRule.onNodeWithText("Neutral").assertExists()
    }

    @Test
    fun `a caller can override the glyph the kind picked`() {
        // `icon ?: kind.icon`. Nothing about the rendered text changes, which is the point: the
        // override has to be exercised for the null-coalesce to have both sides taken.
        show {
            NimazErrorState(
                title = "Custom glyph",
                icon = Icons.Outlined.Warning,
                animated = false,
            )
        }

        composeRule.onNodeWithText("Custom glyph").assertExists()
    }

    @Test
    fun `an action already in flight still renders`() {
        // `loading = true` swaps the label for a spinner, so the clock must not auto-advance —
        // an indeterminate indicator never lets `waitForIdle` return (#604).
        composeRule.mainClock.autoAdvance = false
        show {
            NimazErrorState(
                title = "Reciter unavailable",
                variant = NimazErrorVariant.SECTION,
                primaryAction = NimazErrorDefaults.retry(onRetry = {}, loading = true),
                animated = false,
            )
        }

        composeRule.onNodeWithText("Reciter unavailable").assertExists()
    }

    @Test
    fun `an inline action already in flight still renders`() {
        composeRule.mainClock.autoAdvance = false
        show {
            NimazErrorState(
                title = "Saving",
                variant = NimazErrorVariant.INLINE,
                primaryAction = NimazErrorAction(label = "Retry", onClick = {}, loading = true),
                animated = false,
            )
        }

        composeRule.onNodeWithText("Saving").assertExists()
    }

    @Test
    fun `retry defaults to not loading`() {
        val action = NimazErrorDefaults.retry(onRetry = {})

        assertThat(action.loading).isFalse()
        assertThat(action.icon).isNotNull()
    }
}
