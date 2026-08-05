package com.arshadshah.nimaz.presentation.viewmodel.about

import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.util.UpdateState
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The About screen decided all of this inside a composable — which label, which icon,
 * whether a tap does anything — in an inline `when` that also performed the click. None
 * of it could be asserted, and the two easiest things to get wrong are exactly the two
 * that matter: a failed check must stay tappable so it can be retried, and a download in
 * flight must not be.
 */
class UpdatePromptTest {

    @Test
    fun `a failed check reads as an error and stays tappable`() {
        val prompt = updatePrompt(UpdateState.Error("network unreachable"))

        assertThat(prompt.label).isEqualTo(R.string.update_check_failed)
        assertThat(prompt.isError).isTrue()
        // Retrying the check is the only thing a reader can do about it.
        assertThat(prompt.isBusy).isFalse()
    }

    @Test
    fun `work in flight is not tappable`() {
        assertThat(updatePrompt(UpdateState.Checking).isBusy).isTrue()
        assertThat(updatePrompt(UpdateState.Starting).isBusy).isTrue()
        assertThat(updatePrompt(UpdateState.Downloading).isBusy).isTrue()
    }

    @Test
    fun `an available update and an installed one are both highlighted`() {
        assertThat(updatePrompt(UpdateState.UpdateAvailable).isHighlighted).isTrue()
        assertThat(updatePrompt(UpdateState.Downloaded {}).isHighlighted).isTrue()
        // "Up to date" is good news, so it is highlighted too — but it is not an action.
        assertThat(updatePrompt(UpdateState.NoUpdateAvailable).isHighlighted).isTrue()
        assertThat(updatePrompt(UpdateState.NoUpdateAvailable).isActionable).isFalse()
    }

    @Test
    fun `an idle screen invites the check`() {
        val prompt = updatePrompt(UpdateState.Idle)

        assertThat(prompt.label).isEqualTo(R.string.update_tap_to_check)
        assertThat(prompt.isBusy).isFalse()
        assertThat(prompt.isHighlighted).isFalse()
        assertThat(prompt.isError).isFalse()
    }
}
