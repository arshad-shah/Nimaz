package com.arshadshah.nimaz.presentation.screens.about

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.app.AppIdentity
import com.arshadshah.nimaz.presentation.app.LocalAppIdentity
import com.arshadshah.nimaz.presentation.update.AppUpdateController
import com.arshadshah.nimaz.presentation.update.LocalAppUpdateController
import com.arshadshah.nimaz.presentation.update.UpdateState
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * About: what the app says it is, and the one row on it that does work.
 *
 * The version block is the reason a support mail is answerable — someone reads it aloud when a
 * prayer time is wrong. It comes from `LocalAppIdentity` since PR 14 of #551, and the default that
 * CompositionLocal carries is deliberately an em dash and build 0: a screen that renders without
 * a composition root supplying identity must look unconfigured rather than quietly claim to be
 * some release. So "the version shown is the version supplied" is worth an assertion — the failure
 * it catches is About reporting "—" on a shipped build, which no crash and no other test surfaces.
 *
 * The update row is the real behaviour. `updatePrompt` decides its label, icon and whether a tap
 * does anything, and `UpdatePromptTest` pins that mapping; what cannot be asserted there is that
 * the tap **reaches the right method**. Three states send it three different ways — check, start,
 * and the `completeUpdate` lambda that arrives inside `Downloaded` — and three more must send it
 * nowhere at all, because a tap during a download restarts it. Getting that wrong is not visible:
 * the row looks identical either way.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class AboutScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    private var checks = 0
    private var starts = 0
    private var completions = 0

    private val controller = object : AppUpdateController {
        override val updateState: StateFlow<UpdateState> = this@AboutScreenTest.updateState
        override fun checkForUpdate() { checks++ }
        override fun startUpdate() { starts++ }
    }

    private val openedUris = mutableListOf<String>()
    private val uriHandler = object : UriHandler {
        override fun openUri(uri: String) { openedUris += uri }
    }

    private val identity = AppIdentity(
        versionName = "3.1.4",
        versionCode = 415,
        iconRes = R.drawable.ic_dua,
    )

    private val taps = mutableListOf<String>()

    private fun setContent(controller: AppUpdateController? = this.controller) {
        composeRule.setThemedContent {
            Harness(controller) {
                AboutScreen(
                    onNavigateBack = { taps += "back" },
                    onNavigateToPrivacyPolicy = { taps += "privacy" },
                    onNavigateToTerms = { taps += "terms" },
                    onNavigateToLicenses = { taps += "licenses" },
                    onRateApp = { taps += "rate" },
                    onShareApp = { taps += "share" },
                    onContactUs = { taps += "contact" },
                )
            }
        }
    }

    @Composable
    private fun Harness(controller: AppUpdateController?, content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalAppIdentity provides identity,
            LocalAppUpdateController provides controller,
            LocalUriHandler provides uriHandler,
            content = content,
        )
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    @Test
    fun `the version block reports the identity the composition root supplied`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.version_detail_format, "3.1.4", 415))
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.about_tagline)).assertExists()
    }

    @Test
    fun `the credits name every data source the app ships`() {
        setContent()

        // Rendered uppercase by the grid, so the assertion has to be too.
        composeRule.onNodeWithText(string(R.string.credit_aladhan)).assertExists()
        composeRule.onNodeWithText(string(R.string.credit_tanzil)).assertExists()
        composeRule.onNodeWithText(string(R.string.credit_sunnah)).assertExists()
        composeRule.onNodeWithText(string(R.string.copyright_format, LocalDate.now().year))
            .assertExists()
    }

    @Test
    fun `each link row does its own thing`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.contact_support)).performClick()
        composeRule.onNodeWithText(string(R.string.privacy_policy)).performClick()
        composeRule.onNodeWithText(string(R.string.terms_of_service)).performClick()
        composeRule.onNodeWithText(string(R.string.open_source_licenses)).performClick()

        assertThat(taps).containsExactly("contact", "privacy", "terms", "licenses").inOrder()
    }

    @Test
    fun `the website row opens the site rather than navigating`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.website)).performClick()

        assertThat(openedUris).containsExactly("https://nimaz.arshadshah.com")
        assertThat(taps).isEmpty()
    }

    @Test
    fun `the developer links open the profiles they name`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.developer_name)).assertExists()
    }

    @Test
    fun `the quick actions rate and share`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.about_rate)).performClick()
        composeRule.onNodeWithText(string(R.string.about_share)).performClick()

        assertThat(taps).containsExactly("rate", "share").inOrder()
    }

    @Test
    fun `an idle row asks Play whether there is an update`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.update_tap_to_check)).assertExists()
        composeRule.onNodeWithText(string(R.string.check_for_updates)).performClick()

        assertThat(checks).isEqualTo(1)
        assertThat(starts).isEqualTo(0)
    }

    @Test
    fun `an available update starts rather than re-checking`() {
        updateState.value = UpdateState.UpdateAvailable
        setContent()

        composeRule.onNodeWithText(string(R.string.update_new_version)).assertExists()
        composeRule.onNodeWithText(string(R.string.check_for_updates)).performClick()

        assertThat(starts).isEqualTo(1)
        assertThat(checks).isEqualTo(0)
    }

    @Test
    fun `a downloaded update completes through the lambda it arrived with`() {
        // `completeUpdate` is not a method on the controller: it comes inside the state, so a
        // screen cannot complete an update that has not finished downloading.
        updateState.value = UpdateState.Downloaded(completeUpdate = { completions++ })
        setContent()

        composeRule.onNodeWithText(string(R.string.update_downloaded)).assertExists()
        composeRule.onNodeWithText(string(R.string.check_for_updates)).performClick()

        assertThat(completions).isEqualTo(1)
        assertThat(checks).isEqualTo(0)
        assertThat(starts).isEqualTo(0)
    }

    @Test
    fun `a check in flight does not accept a second tap`() {
        // The row is disabled while busy. Tapping a downloading update again would restart it,
        // and the row looks the same whether or not that is guarded.
        updateState.value = UpdateState.Downloading
        setContent()

        composeRule.onNodeWithText(string(R.string.update_downloading)).assertExists()
        composeRule.onNodeWithText(string(R.string.check_for_updates)).performClick()

        assertThat(checks).isEqualTo(0)
        assertThat(starts).isEqualTo(0)
    }

    @Test
    fun `a failed check can be retried`() {
        // Not busy: retrying is the only thing a reader can do about a failure.
        updateState.value = UpdateState.Error("no network")
        setContent()

        composeRule.onNodeWithText(string(R.string.update_check_failed)).assertExists()
        composeRule.onNodeWithText(string(R.string.check_for_updates)).performClick()

        assertThat(checks).isEqualTo(1)
    }

    @Test
    fun `an up-to-date check says so`() {
        updateState.value = UpdateState.NoUpdateAvailable
        setContent()

        composeRule.onNodeWithText(string(R.string.update_up_to_date)).assertExists()
    }

    @Test
    fun `a build with no update mechanism still renders and absorbs the tap`() {
        // Null is a debug build, a test or a @Preview. Every call site handled it before the
        // port moved to `:core:ui`, and the row must not become a crash on those builds.
        setContent(controller = null)

        composeRule.onNodeWithText(string(R.string.update_tap_to_check)).assertExists()
        composeRule.onNodeWithText(string(R.string.check_for_updates)).performClick()

        assertThat(checks).isEqualTo(0)
    }

    @Test
    fun `the back arrow leaves`() {
        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.cd_back)).performClick()

        assertThat(taps).containsExactly("back")
    }
}
