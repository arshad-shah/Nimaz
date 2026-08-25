package com.arshadshah.nimaz.presentation.components.molecules.qibla

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The one element that tells a reader whether they are facing the Kaaba.
 *
 * It has three states and they are decided by two booleans in combination, not by an enum:
 * `isFacingQibla` wins outright, `isCompassReady` only decides whether a *turn hint* is offered
 * beside the bearing, and neither is `false ⇒ nothing`. The failure that matters is the middle
 * state: a capsule that shows "Turn right 12°" while the compass has not settled is an
 * instruction to turn away from the Kaaba, and a capsule that hides the hint once the compass
 * *has* settled leaves a bearing with nothing to do about it.
 *
 * The turn direction is the other half. `rotationToQibla` is signed, and the sign is the whole
 * message — a capsule that reads the magnitude and drops the sign sends every reader the wrong
 * way half the time, while still looking entirely plausible.
 */
@RunWith(RobolectricTestRunner::class)
class QiblaStatusCapsuleTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private fun str(@StringRes id: Int, vararg args: Any): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id, *args)

    private fun render(
        qiblaBearing: Int = 119,
        isFacingQibla: Boolean = false,
        rotationToQibla: Float = 12f,
        isCompassReady: Boolean = true,
        onCamera: Boolean = false,
    ) {
        composeRule.setThemedContent {
            QiblaStatusCapsule(
                qiblaBearing = qiblaBearing,
                isFacingQibla = isFacingQibla,
                rotationToQibla = rotationToQibla,
                isCompassReady = isCompassReady,
                onCamera = onCamera,
            )
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `the bearing is always shown, with its cardinal direction`() {
        render(qiblaBearing = 119)

        // 119° is SE. A bearing printed without its cardinal is a number with no way to sanity
        // check it against the sun.
        composeRule.onNodeWithText("119° SE").assertIsDisplayed()
    }

    @Test
    fun `before the compass settles there is a bearing and no instruction`() {
        render(isCompassReady = false, rotationToQibla = 40f)

        composeRule.onNodeWithText("119° SE").assertIsDisplayed()
        // Neither hint: an unsettled magnetometer's rotation is noise, and rendering it as
        // "Turn right 40°" is a confident instruction built on nothing.
        composeRule.onNodeWithText(str(R.string.turn_right_format, 40)).assertDoesNotExist()
        composeRule.onNodeWithText(str(R.string.facing_qibla)).assertDoesNotExist()
    }

    @Test
    fun `a settled compass off the bearing says which way to turn`() {
        render(isCompassReady = true, rotationToQibla = 12f)

        composeRule.onNodeWithText(str(R.string.turn_right_format, 12)).assertIsDisplayed()
    }

    @Test
    fun `a negative rotation turns the other way`() {
        render(isCompassReady = true, rotationToQibla = -8f)

        // The sign is the instruction. Dropping it — `abs` applied one call too early — is a
        // capsule that is confidently wrong half the time.
        composeRule.onNodeWithText(str(R.string.turn_left_format, 8)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.turn_right_format, 8)).assertDoesNotExist()
    }

    @Test
    fun `facing the qibla replaces the instruction rather than adding to it`() {
        render(isFacingQibla = true, rotationToQibla = 1f)

        composeRule.onNodeWithText(str(R.string.facing_qibla)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.turn_right_format, 1)).assertDoesNotExist()
        composeRule.onNodeWithText("119° SE").assertIsDisplayed()
    }

    @Test
    fun `facing the qibla is announced even before the compass reports itself ready`() {
        render(isFacingQibla = true, isCompassReady = false, rotationToQibla = 0f)

        // `showLeft = isFacingQibla || isCompassReady`: the alignment is the stronger fact, and
        // an `&&` here would blank the one message the reader is waiting for.
        composeRule.onNodeWithText(str(R.string.facing_qibla)).assertIsDisplayed()
    }

    @Test
    fun `over the camera the capsule still carries the same words`() {
        render(onCamera = true, rotationToQibla = -8f)

        // `onCamera` only swaps the surface it is drawn on — the legibility treatment must not
        // become a second rendering of the message.
        composeRule.onNodeWithText(str(R.string.turn_left_format, 8)).assertIsDisplayed()
        composeRule.onNodeWithText("119° SE").assertIsDisplayed()
    }

    @Test
    fun `a northern bearing reads as N, not as an out-of-range cardinal`() {
        render(qiblaBearing = 350)

        composeRule.onNodeWithText("350° N").assertIsDisplayed()
    }
}
