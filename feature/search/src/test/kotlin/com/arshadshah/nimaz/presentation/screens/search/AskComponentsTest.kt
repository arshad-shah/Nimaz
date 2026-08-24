package com.arshadshah.nimaz.presentation.screens.search

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.AiError
import com.arshadshah.nimaz.domain.model.AnswerConfidence
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * What an Ask failure tells the reader, and what it invites them to do about it.
 *
 * Seven error cases share one card, and the card decides two things per case that the ViewModel
 * cannot see: whether this reads as a **pause** or as a **failure**, and whether a retry button is
 * offered at all. Both matter in the same direction. A daily cap rendered in error red, with a
 * retry, invites someone to tap repeatedly against a limit only time lifts — and each tap on a
 * genuinely retryable failure is one billed Worker call, so offering retry where it cannot help is
 * not merely useless, it teaches the habit on the surface where it costs money.
 *
 * The rate-limited case additionally has arithmetic in it: raw seconds are useless to a reader, so
 * they are rounded up into minutes, and minutes into hours. Rounding *down* is the failure that
 * matters — "try again in 0 minutes" against a limit that has not lifted.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class AskComponentsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private var retries = 0

    private fun str(@StringRes id: Int): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id)

    private fun str(@StringRes id: Int, vararg args: Any): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id, *args)

    private fun renderError(error: AiError) {
        composeRule.setThemedContent {
            AskErrorCard(error = error, onRetry = { retries++ })
        }
        composeRule.waitForIdle()
    }

    // ── retry is offered only where retrying can work ────────────────────────

    @Test
    fun `a dropped connection can be retried`() {
        renderError(AiError.Network)

        composeRule.onNodeWithText(str(R.string.ai_error_network_title)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.ai_try_again)).performClick()

        assertThat(retries).isEqualTo(1)
    }

    @Test
    fun `an unexplained failure can be retried`() {
        renderError(AiError.Unknown)

        composeRule.onNodeWithText(str(R.string.ai_error_unknown_title)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.ai_try_again)).performClick()

        assertThat(retries).isEqualTo(1)
    }

    @Test
    fun `the daily limit offers no retry`() {
        renderError(AiError.RateLimited(retryAfterSeconds = null))

        composeRule.onNodeWithText(str(R.string.ai_error_rate_limited)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.ai_try_again)).assertDoesNotExist()
    }

    @Test
    fun `the shared budget pause offers no retry`() {
        renderError(AiError.BudgetExceeded)

        composeRule.onNodeWithText(str(R.string.ai_error_budget_title)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.ai_try_again)).assertDoesNotExist()
    }

    /** An unverified build is not going to verify on the second tap. */
    @Test
    fun `an unverified app copy offers no retry`() {
        renderError(AiError.Unverified)

        composeRule.onNodeWithText(str(R.string.ai_error_unverified_title)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.ai_try_again)).assertDoesNotExist()
    }

    /** Rewording is the fix, so the card says that instead of offering the same question again. */
    @Test
    fun `a rejected question asks for a rewording rather than a retry`() {
        renderError(AiError.Invalid("question too long"))

        composeRule.onNodeWithText(str(R.string.ai_error_invalid)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.ai_try_again)).assertDoesNotExist()
    }

    /**
     * Consent is the one failure with a specific remedy, and it is not on this card — it is in
     * Search settings. Retrying without turning the feature on would fail identically forever.
     */
    @Test
    fun `an ask made without consent points at settings, not at a retry`() {
        renderError(AiError.ConsentRequired)

        composeRule.onNodeWithText(str(R.string.ai_error_consent_title)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.ai_error_consent)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.ai_try_again)).assertDoesNotExist()
    }

    // ── "you can ask again in …" ─────────────────────────────────────────────

    /** 90 seconds is "about 2 minutes", never "1" — rounding down promises a limit that is still on. */
    @Test
    fun `a wait under an hour is rounded up to whole minutes`() {
        renderError(AiError.RateLimited(retryAfterSeconds = 90))

        val wait = str(R.string.ai_retry_minutes_format, 2L)
        composeRule.onNodeWithText(str(R.string.ai_error_rate_limited_retry, wait))
            .assertIsDisplayed()
    }

    @Test
    fun `a wait of over an hour is given in hours`() {
        // 7,200s is exactly 120 minutes → 2 hours, and must not read as "120 minutes".
        renderError(AiError.RateLimited(retryAfterSeconds = 7_200))

        val wait = str(R.string.ai_retry_hours_format, 2L)
        composeRule.onNodeWithText(str(R.string.ai_error_rate_limited_retry, wait))
            .assertIsDisplayed()
    }

    /** A partial hour rounds up too: 61 minutes is "about 2 hours", not "1". */
    @Test
    fun `a partial hour rounds up rather than truncating`() {
        renderError(AiError.RateLimited(retryAfterSeconds = 3_660))

        val wait = str(R.string.ai_retry_hours_format, 2L)
        composeRule.onNodeWithText(str(R.string.ai_error_rate_limited_retry, wait))
            .assertIsDisplayed()
    }

    /** No retry-after from the Worker means no invented number — the generic sentence stands. */
    @Test
    fun `an unknown wait says nothing about when`() {
        renderError(AiError.RateLimited(retryAfterSeconds = null))

        composeRule.onNodeWithText(str(R.string.ai_error_rate_limited)).assertIsDisplayed()
    }

    // ── the answer card's confidence ─────────────────────────────────────────

    private fun renderAnswer(confidence: AnswerConfidence) {
        composeRule.setThemedContent {
            AskAnswerCard(
                answer = "Zakat is due on wealth held for a lunar year.",
                confidence = confidence,
            )
        }
        composeRule.waitForIdle()
    }

    /**
     * Confidence is the reader's cue for how hard to check the answer against its sources, so it
     * has to be stated in words. A card that renders the chip's colour but not its label leaves
     * "low confidence" indistinguishable from "high" to anyone using TalkBack.
     */
    @Test
    fun `a high-confidence answer says so`() {
        renderAnswer(AnswerConfidence.HIGH)

        composeRule.onNodeWithText(str(R.string.ai_confidence_high)).assertIsDisplayed()
        composeRule.onNodeWithText("Zakat is due on wealth held for a lunar year.")
            .assertIsDisplayed()
    }

    @Test
    fun `a medium-confidence answer says so`() {
        renderAnswer(AnswerConfidence.MEDIUM)

        composeRule.onNodeWithText(str(R.string.ai_confidence_medium)).assertIsDisplayed()
    }

    @Test
    fun `a low-confidence answer says so`() {
        renderAnswer(AnswerConfidence.LOW)

        composeRule.onNodeWithText(str(R.string.ai_confidence_low)).assertIsDisplayed()
    }

    /** The answer never stands alone — the note saying it is not a ruling ships with it. */
    @Test
    fun `every answer carries the trust note`() {
        renderAnswer(AnswerConfidence.HIGH)

        composeRule.onNodeWithText(str(R.string.ai_answer_section)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.ai_trust_note)).assertIsDisplayed()
    }

    /** The discovery card's two actions are distinct: one opens settings, one only dismisses. */
    @Test
    fun `the discovery card separates enabling from dismissing`() {
        var opens = 0
        var dismissals = 0
        composeRule.setThemedContent {
            AskDiscoveryCard(onOpenSettings = { opens++ }, onDismiss = { dismissals++ })
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(str(R.string.ai_discover_enable)).performClick()
        assertThat(opens).isEqualTo(1)
        assertThat(dismissals).isEqualTo(0)

        composeRule.onNodeWithText(str(R.string.ai_discover_dismiss)).performClick()
        assertThat(dismissals).isEqualTo(1)
        assertThat(opens).isEqualTo(1)
    }
}
