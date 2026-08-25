package com.arshadshah.nimaz.presentation.screens.about

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.LibraryLicense
import com.arshadshah.nimaz.domain.model.OpenSourceLibrary
import com.arshadshah.nimaz.presentation.components.molecules.NimazErrorKind
import com.arshadshah.nimaz.presentation.viewmodel.UiError
import com.arshadshah.nimaz.presentation.viewmodel.about.LicenseDetailUiState
import com.arshadshah.nimaz.presentation.viewmodel.about.LicensesEvent
import com.arshadshah.nimaz.presentation.viewmodel.about.LicensesViewModel
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowToast
import org.robolectric.annotation.Config

/**
 * One library's licence, in full.
 *
 * This screen is the only place the app shows the text that actually governs use of a component,
 * and everything above that text is a paraphrase which says so. The assertions that matter are
 * therefore about **what is optional**: AboutLibraries publishes a version for most artifacts, an
 * author for many, a website for some and the licence *text* for fewer still, and each missing
 * field has to leave the layout intact rather than render a heading with nothing under it. A
 * library with no licence text is the sharpest case — the copy button must disappear with it,
 * because copying an empty clipboard entry looks exactly like a working copy.
 *
 * The collapse/expand is the other half. The text is clipped under a fade rather than hidden
 * behind an accordion, so a reader can see what they are being offered before deciding — and the
 * control has to say which way it goes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class LicenseDetailScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val detailState = MutableStateFlow(LicenseDetailUiState())
    private val events = mutableListOf<LicensesEvent>()

    private val viewModel: LicensesViewModel = mockk(relaxed = true) {
        every { this@mockk.detailState } returns this@LicenseDetailScreenTest.detailState
        every { onEvent(any()) } answers { events += firstArg<LicensesEvent>() }
    }

    private var backs = 0

    private fun setContent(libraryId: Int = 1) {
        composeRule.setThemedContent {
            LicenseDetailScreen(
                libraryId = libraryId,
                onNavigateBack = { backs++ },
                viewModel = viewModel,
            )
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    private fun library(
        name: String = "Compose UI",
        coordinate: String = "androidx.compose.ui:ui",
        version: String? = "1.7.0",
        author: String? = "Google",
        website: String? = "https://developer.android.com/jetpack/compose",
        licenses: List<LibraryLicense> = listOf(
            LibraryLicense("Apache License 2.0", url = null, content = LICENCE_TEXT),
        ),
    ) = OpenSourceLibrary(
        id = 1,
        name = name,
        coordinate = coordinate,
        version = version,
        author = author,
        website = website,
        licenses = licenses,
    )

    private fun loaded(library: OpenSourceLibrary = library()) =
        LicenseDetailUiState(library = library, isLoading = false)

    @Test
    fun `opening the screen asks for the library the route named`() {
        detailState.value = loaded()
        setContent(libraryId = 42)

        assertThat(events).contains(LicensesEvent.LoadLibrary(42))
    }

    @Test
    fun `a fully described library reports every field it published`() {
        detailState.value = loaded()
        setContent()

        // The name is on the app bar as well as on the card, so both nodes carry it.
        composeRule.onAllNodesWithText("Compose UI").onFirst().assertExists()
        composeRule.onNodeWithText("1.7.0").assertExists()
        composeRule.onNodeWithText(string(R.string.license_detail_coordinate)).assertExists()
        composeRule.onNodeWithText("androidx.compose.ui").assertExists()
        composeRule.onNodeWithText(string(R.string.license_detail_published_by)).assertExists()
        composeRule.onNodeWithText("Google").assertExists()
        // The scheme is stripped: it is noise in a row a reader scans.
        composeRule.onNodeWithText("developer.android.com/jetpack/compose").assertExists()
        composeRule.onNodeWithText(string(R.string.license_detail_open_home_page)).assertExists()
    }

    @Test
    fun `a library that published nothing but a name still renders`() {
        // The common shape further down the list: no version, no author, no website. Each of
        // those rows has to be absent rather than empty.
        detailState.value = loaded(
            library(
                name = "Adhan",
                coordinate = "adhan",
                version = null,
                author = null,
                website = null,
            ),
        )
        setContent()

        composeRule.onAllNodesWithText("Adhan").onFirst().assertExists()
        composeRule.onNodeWithText(string(R.string.license_detail_published_by)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.license_detail_home_page)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.license_detail_open_home_page)).assertDoesNotExist()
        // A bare artifact id has no Maven group, so the coordinate row goes too.
        composeRule.onNodeWithText(string(R.string.license_detail_coordinate)).assertDoesNotExist()
    }

    @Test
    fun `a library with no name at all still gets a tile`() {
        // The monogram is the library's initial. AboutLibraries occasionally reports an empty
        // display name for an artifact with no POM name, and `first()` on that throws — the
        // tile falls back to "?" instead of taking the screen down.
        detailState.value = loaded(library(name = "", coordinate = "org.unknown:artifact"))
        setContent()

        composeRule.onNodeWithText("?").assertExists()
    }

    @Test
    fun `a recognised family is glossed in plain terms above its text`() {
        detailState.value = loaded()
        setContent()

        composeRule.onNodeWithText(string(R.string.license_plain_title)).assertExists()
        composeRule.onNodeWithText(string(R.string.license_plain_apache)).assertExists()
        // And the disclaimer that the gloss is not what governs use.
        composeRule.onNodeWithText(string(R.string.license_detail_governs_note)).assertExists()
    }

    @Test
    fun `a family the app cannot name is not summarised`() {
        // A licence we cannot place we do not paraphrase — a wrong one-sentence gloss of an
        // unknown licence is worse than none.
        detailState.value = loaded(
            library(licenses = listOf(LibraryLicense("Bespoke EULA", url = null, content = "x"))),
        )
        setContent()

        composeRule.onNodeWithText(string(R.string.license_plain_title)).assertDoesNotExist()
    }

    @Test
    fun `the licence text starts collapsed and the control says how to see the rest`() {
        detailState.value = loaded()
        setContent()

        composeRule.onNodeWithText(
            string(R.string.license_detail_full_text_format, string(R.string.license_family_apache))
        ).assertExists()
        composeRule.onNodeWithText(string(R.string.license_detail_show_full_text)).assertExists()
        composeRule.onNodeWithText(string(R.string.license_detail_collapse)).assertDoesNotExist()
    }

    @Test
    fun `expanding swaps the control to a collapse`() {
        detailState.value = loaded()
        setContent()

        composeRule.onNodeWithText(string(R.string.license_detail_show_full_text)).performClick()

        composeRule.onNodeWithText(string(R.string.license_detail_collapse)).assertExists()
        composeRule.onNodeWithText(string(R.string.license_detail_show_full_text))
            .assertDoesNotExist()
    }

    @Test
    fun `the home page button opens the site the library published`() {
        detailState.value = loaded()
        setContent()

        composeRule.onNodeWithText(string(R.string.license_detail_open_home_page)).performClick()

        val started = shadowOf(ApplicationProvider.getApplicationContext<Application>())
            .nextStartedActivity
        assertThat(started?.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(started?.data?.toString())
            .isEqualTo("https://developer.android.com/jetpack/compose")
    }

    @Test
    fun `copying the licence text confirms it happened`() {
        // The clipboard write is silent, so the toast is the only feedback that the tap did
        // anything — and a copy that reports nothing reads as a copy that failed.
        detailState.value = loaded()
        setContent()

        composeRule.onNodeWithText(string(R.string.action_copy)).performClick()
        composeRule.waitForIdle()

        assertThat(ShadowToast.getTextOfLatestToast())
            .isEqualTo(string(R.string.license_text_copied))
    }

    @Test
    fun `a library that published no licence text says so and offers nothing to copy`() {
        detailState.value = loaded(
            library(licenses = listOf(LibraryLicense("MIT License", url = null, content = null))),
        )
        setContent()

        composeRule.onNodeWithText(string(R.string.license_detail_no_text)).assertExists()
        // Copying an empty entry is indistinguishable from a working copy, so the button goes.
        composeRule.onNodeWithText(string(R.string.action_copy)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.license_detail_show_full_text))
            .assertDoesNotExist()
    }

    @Test
    fun `a dual-licensed library shows both texts and is filed under the first`() {
        detailState.value = loaded(
            library(
                licenses = listOf(
                    LibraryLicense("MIT License", url = null, content = "MIT text"),
                    LibraryLicense("Apache License 2.0", url = null, content = "Apache text"),
                ),
            ),
        )
        setContent()

        // Both texts are shown — a reader needs to see what they are actually bound by.
        composeRule.onNodeWithText(
            string(R.string.license_detail_full_text_format, string(R.string.license_family_mit))
        ).assertExists()
        composeRule.onNodeWithText(
            string(R.string.license_detail_full_text_format, string(R.string.license_family_apache))
        ).assertExists()
        // …but the *family* is the first, which is what keeps the list's section counts
        // agreeing with its library count.
        composeRule.onNodeWithText(string(R.string.license_plain_mit)).assertExists()
    }

    @Test
    fun `a library missing from the bundled list is reported as absent, with a way out`() {
        // NOT_FOUND rather than a failure, and no retry: a library that is not in the bundled
        // list will not be there next time either, so "try again" would be a lie.
        detailState.value = LicenseDetailUiState(
            library = null,
            isLoading = false,
            error = UiError(
                message = R.string.license_detail_library_not_found,
                kind = NimazErrorKind.NOT_FOUND,
            ),
        )
        setContent()

        composeRule.onNodeWithText(string(R.string.license_detail_library_not_found)).assertExists()
        composeRule.onNodeWithText(string(R.string.try_again)).assertDoesNotExist()

        composeRule.onNodeWithText(string(R.string.close)).performClick()

        assertThat(backs).isEqualTo(1)
    }

    @Test
    fun `the back arrow leaves`() {
        detailState.value = loaded()
        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.cd_back)).performClick()

        assertThat(backs).isEqualTo(1)
    }

    @Test
    fun `a detail still loading shows neither the library nor a not-found`() {
        composeRule.mainClock.autoAdvance = false
        detailState.value = LicenseDetailUiState(isLoading = true)
        setContent()

        composeRule.onNodeWithText(string(R.string.license_detail_library_not_found))
            .assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.license_detail_governs_note))
            .assertDoesNotExist()
    }

    private companion object {
        val LICENCE_TEXT = buildString {
            appendLine("Apache License")
            appendLine("Version 2.0, January 2004")
            repeat(40) { appendLine("Line $it of the licence text, long enough to be clipped.") }
        }
    }
}
