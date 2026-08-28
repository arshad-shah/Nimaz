package com.arshadshah.nimaz.presentation.components.molecules

import android.content.Context
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The invite sheet: a scannable code and a link to send.
 *
 * Both halves have to be there. The QR is for somebody sitting across a table; the link is for
 * somebody in a chat — and the app-invite share is the one content share that deliberately keeps
 * its plain text alongside the image, because a link the recipient can tap is the whole point.
 * A sheet that offered only the code would be useless remotely and a sheet that offered only the
 * button would waste the thing it was built for.
 *
 * The sheet slides in, and with the clock left running that animation completes; the sheet's
 * content is asserted with `assertExists` because a bottom sheet parks its content below the
 * viewport while the slide is in flight (#604).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class ShareAppSheetTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `the sheet explains itself and offers both ways to share`() {
        composeRule.setThemedContent {
            ShareAppSheet(onDismiss = {}, onShareLink = {})
        }

        composeRule.onNodeWithText(context.getString(R.string.share_app_title)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.share_app_subtitle)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.share_app_scan_hint)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.share_app_link_button)).assertExists()
    }

    @Test
    fun `the link button sends the invite`() {
        var shared = 0
        composeRule.setThemedContent {
            ShareAppSheet(onDismiss = {}, onShareLink = { shared++ })
        }

        composeRule.onNodeWithText(context.getString(R.string.share_app_link_button)).performClick()
        assertThat(shared).isEqualTo(1)
    }
}
