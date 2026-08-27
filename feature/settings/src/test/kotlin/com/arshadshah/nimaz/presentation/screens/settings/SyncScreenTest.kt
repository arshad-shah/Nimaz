package com.arshadshah.nimaz.presentation.screens.settings

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.data.sync.CancelReason
import com.arshadshah.nimaz.data.sync.ConnectionState
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.onRoot
import com.arshadshah.nimaz.data.sync.SyncCategory
import com.arshadshah.nimaz.data.sync.SyncPayload
import com.arshadshah.nimaz.data.sync.categories
import com.arshadshah.nimaz.presentation.viewmodel.UiError
import com.arshadshah.nimaz.presentation.viewmodel.settings.ActivityLogEntry
import com.arshadshah.nimaz.presentation.viewmodel.settings.SyncDataSummary
import com.arshadshah.nimaz.presentation.viewmodel.settings.SyncEvent
import com.arshadshah.nimaz.presentation.viewmodel.settings.SyncMode
import com.arshadshah.nimaz.presentation.viewmodel.settings.SyncUiState
import com.arshadshah.nimaz.presentation.viewmodel.settings.SyncViewModel
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
import org.robolectric.annotation.Config

/**
 * Device-to-device sync, which is one screen wearing seven faces.
 *
 * The `when` that picks between them is ordered, not exhaustive over a sealed type, so the
 * ordering *is* the logic: `Cancelled` is checked before `Connecting`, `error != null` before
 * `Completed`, and everything unmatched falls through to progress. Two consequences are worth
 * pinning because neither is visible from reading one branch:
 *
 * - **A cancelled sync must not show the pairing screen**, even though the state carrying the
 *   cancellation may still hold an endpoint. Reordering those two arms offers "Accept" for a
 *   connection the partner has already dropped.
 * - **An error outranks completion.** A transfer that failed part-way can leave `Completed`
 *   behind it, and reporting "Sync Complete!" over a failed import is the single worst thing
 *   this screen can say — the user walks away believing their data moved.
 *
 * The verification code is the security-relevant one: it exists so both people can confirm they
 * are pairing with each other, so it must be rendered exactly and both answers must reach the
 * *endpoint the state names* rather than a remembered one.
 *
 * Leaving the screen cancels first, on every route out — back arrow, Close, and Done. A route that
 * navigated without cancelling leaves the connection advertising after the screen is gone.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class SyncScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val state = MutableStateFlow(SyncUiState())
    private val events = mutableListOf<SyncEvent>()
    private val viewModel: SyncViewModel = mockk(relaxed = true) {
        every { uiState } returns this@SyncScreenTest.state
        every { onEvent(any()) } answers { events += firstArg<SyncEvent>() }
    }
    private var backs = 0

    private fun setContent(uiState: SyncUiState = SyncUiState()) {
        state.value = uiState
        composeRule.setThemedContent {
            SyncScreen(onNavigateBack = { backs++ }, viewModel = viewModel)
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    // ── Mode selection ───────────────────────────────────────────────────────────────────────

    @Test
    fun `the screen opens on the choice between sending and receiving`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.sync_device_to_device)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.sync_send_data)).assertExists()
        composeRule.onNodeWithText(string(R.string.sync_receive_data)).assertExists()
        composeRule.onNodeWithText(string(R.string.sync_intro)).assertExists()
    }

    @Test
    fun `Send starts sending and Receive starts receiving`() {
        // Two buttons, two events, one block. Crossing them makes the device that meant to
        // receive start advertising its own data instead — and the screen looks identical until
        // the transfer runs the wrong way.
        setContent()

        composeRule.onNodeWithText(string(R.string.sync_send_data)).performClick()
        assertThat(events).containsExactly(SyncEvent.StartSend)

        events.clear()
        composeRule.onNodeWithText(string(R.string.sync_receive_data)).performClick()
        assertThat(events).containsExactly(SyncEvent.StartReceive)
    }

    @Test
    fun `no role badge is shown before a mode is chosen`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.sync_sending)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.sync_receiving)).assertDoesNotExist()
    }

    // ── The pairing code ─────────────────────────────────────────────────────────────────────

    private fun connecting(mode: SyncMode = SyncMode.SEND) = SyncUiState(
        mode = mode,
        connectionState = ConnectionState.Connecting(
            endpointId = "endpoint-7",
            endpointName = "Pixel 8",
            authToken = "4821",
        ),
    )

    @Test
    fun `the verification code is shown exactly, with the partner's name`() {
        // The code exists so two people can confirm they are pairing with each other. A
        // truncated, reformatted or partially-rendered code cannot be compared, which defeats
        // the only check standing between the transfer and the wrong device.
        setContent(connecting())

        composeRule.onNodeWithText("4821").assertExists()
        composeRule.onNodeWithText(string(R.string.sync_connecting_to_format, "Pixel 8"))
            .assertExists()
        composeRule.onNodeWithText(string(R.string.sync_verification_code)).assertExists()
    }

    @Test
    fun `accepting answers the endpoint the state names`() {
        // The id comes from the state rather than from anything the screen remembers, because
        // the state can change under a screen that is waiting for a tap.
        setContent(connecting())

        composeRule.onNodeWithText(string(R.string.sync_accept)).performClick()

        assertThat(events).containsExactly(SyncEvent.AcceptConnection("endpoint-7"))
    }

    @Test
    fun `rejecting answers the same endpoint, and is not an accept`() {
        setContent(connecting())

        composeRule.onNodeWithText(string(R.string.sync_reject)).performClick()

        assertThat(events).containsExactly(SyncEvent.RejectConnection("endpoint-7"))
    }

    @Test
    fun `the pairing screen says which way the data will move`() {
        // "Accept" is asked of both devices, and the two answers mean opposite things. Without
        // the role line there is nothing on the screen that says which one you are.
        setContent(connecting(SyncMode.SEND))

        composeRule.onNodeWithText(string(R.string.sync_role_sending)).assertExists()
        composeRule.onNodeWithText(string(R.string.sync_sending)).assertExists()
    }

    @Test
    fun `the receiving device says it is receiving`() {
        setContent(connecting(SyncMode.RECEIVE))

        composeRule.onNodeWithText(string(R.string.sync_role_receiving)).assertExists()
        composeRule.onNodeWithText(string(R.string.sync_receiving)).assertExists()
    }

    // ── Waiting, progress, completion ────────────────────────────────────────────────────────

    @Test
    fun `waiting for the partner offers a way out`() {
        // The other device may never answer. Without the cancel button the only exit is the
        // back arrow, which is not obviously an exit while a spinner is running.
        composeRule.mainClock.autoAdvance = false
        setContent(
            SyncUiState(
                mode = SyncMode.SEND,
                connectionState = ConnectionState.WaitingForPartnerAccept("e", "Pixel 8"),
            )
        )

        composeRule.onNodeWithText(string(R.string.sync_waiting_partner)).assertExists()
        composeRule.onNodeWithText(string(R.string.cancel)).performClick()

        assertThat(events).containsExactly(SyncEvent.Cancel)
    }

    @Test
    fun `progress reports which step of how many, not just a spinner`() {
        // A transfer with no visible step count is indistinguishable from a stalled one, and
        // this screen asks people to keep two devices awake and near each other while it runs.
        setContent(
            SyncUiState(
                mode = SyncMode.SEND,
                connectionState = ConnectionState.Transferring(0.5f),
                currentStep = "Exporting prayer records",
                stepsCompleted = 3,
                totalSteps = 12,
            )
        )

        composeRule.onNodeWithText("Exporting prayer records").assertExists()
        composeRule.onNodeWithText(string(R.string.sync_step_format, 3, 12)).assertExists()
        composeRule.onNodeWithText("25%").assertExists()
    }

    @Test
    fun `progress with no steps yet does not divide by zero`() {
        // `totalSteps` is 0 for the whole window between choosing a mode and the first step
        // arriving — every sync passes through it.
        setContent(
            SyncUiState(
                mode = SyncMode.RECEIVE,
                connectionState = ConnectionState.Discovering,
                stepsCompleted = 0,
                totalSteps = 0,
            )
        )

        composeRule.onNodeWithText("0%").assertExists()
        composeRule.onNodeWithText(string(R.string.sync_preparing)).assertExists()
    }

    @Test
    fun `a completed send says data was sent, not imported`() {
        // The same screen serves both devices and the two sentences are opposites. Getting it
        // backwards tells the sender their data was replaced.
        setContent(
            SyncUiState(
                mode = SyncMode.SEND,
                connectionState = ConnectionState.Completed(bytesReceived = 4096),
            )
        )

        composeRule.onNodeWithText(string(R.string.sync_complete)).assertExists()
        composeRule.onNodeWithText(string(R.string.sync_sent_success)).assertExists()
        composeRule.onNodeWithText(string(R.string.sync_imported_success)).assertDoesNotExist()
    }

    @Test
    fun `a completed receive says data was imported`() {
        setContent(
            SyncUiState(
                mode = SyncMode.RECEIVE,
                connectionState = ConnectionState.Completed(bytesReceived = 4096),
            )
        )

        composeRule.onNodeWithText(string(R.string.sync_imported_success)).assertExists()
    }

    @Test
    fun `the completion summary names what actually moved`() {
        // "Sync complete" without a manifest is unverifiable. The categories come from
        // `SyncPayload.categories()`, so the label map is what turns a key into something a
        // person can check against what they expected to move.
        setContent(
            SyncUiState(
                mode = SyncMode.RECEIVE,
                connectionState = ConnectionState.Completed(bytesReceived = 4096),
                dataSummary = SyncDataSummary(
                    categories = listOf(
                        SyncCategory(key = "prayerRecords", count = 120),
                        SyncCategory(key = "bookmarks", count = 8),
                    ),
                    totalBytes = 4096,
                ),
            )
        )

        composeRule.onNodeWithText(string(R.string.sync_item_prayer_records)).assertExists()
        composeRule.onNodeWithText(string(R.string.sync_item_bookmarks)).assertExists()
        composeRule.onNodeWithText(string(R.string.sync_data_imported_title)).assertExists()
    }

    @Test
    fun `Done cancels the connection before leaving`() {
        // Leaving without cancelling leaves the device advertising after the screen is gone.
        setContent(
            SyncUiState(
                mode = SyncMode.SEND,
                connectionState = ConnectionState.Completed(bytesReceived = 1),
            )
        )

        composeRule.onNodeWithText(string(R.string.done)).performClick()

        assertThat(events).containsExactly(SyncEvent.Cancel)
        assertThat(backs).isEqualTo(1)
    }

    // ── Cancellation and failure ─────────────────────────────────────────────────────────────

    @Test
    fun `a cancellation says who cancelled it`() {
        // "Sync cancelled" alone leaves the reader wondering whether they did something wrong.
        // Which of the three happened decides whether retrying is worth anything.
        setContent(
            SyncUiState(
                mode = SyncMode.SEND,
                connectionState = ConnectionState.Cancelled(CancelReason.BY_PARTNER),
            )
        )

        composeRule.onNodeWithText(string(R.string.sync_cancelled_by_partner)).assertExists()
        composeRule.onNodeWithText(string(R.string.sync_cancelled_by_you)).assertDoesNotExist()
    }

    @Test
    fun `a cancellation by the user says so`() {
        setContent(
            SyncUiState(
                mode = SyncMode.RECEIVE,
                connectionState = ConnectionState.Cancelled(CancelReason.BY_USER),
            )
        )

        composeRule.onNodeWithText(string(R.string.sync_cancelled_by_you)).assertExists()
    }

    @Test
    fun `a lost connection is reported as lost, not as a cancellation`() {
        // Only this one is worth retrying immediately — the other two need the other person.
        setContent(
            SyncUiState(
                mode = SyncMode.SEND,
                connectionState = ConnectionState.Cancelled(CancelReason.CONNECTION_LOST),
            )
        )

        composeRule.onNodeWithText(string(R.string.sync_connection_lost)).assertExists()
        composeRule.onNodeWithText(string(R.string.sync_connection_lost_msg)).assertExists()
    }

    @Test
    fun `a cancelled sync keeps the transcript of what got through`() {
        // The log is the one thing a reader can use to decide whether to retry or start over.
        setContent(
            SyncUiState(
                mode = SyncMode.SEND,
                connectionState = ConnectionState.Cancelled(CancelReason.BY_USER),
                activityLog = listOf(ActivityLogEntry("Started sending mode")),
            )
        )

        composeRule.onNodeWithText(string(R.string.sync_activity)).assertExists()
        composeRule.onNodeWithText("Started sending mode").assertExists()
    }

    @Test
    fun `an error is shown instead of a completion, even with a completed connection`() {
        // The `when` checks `error != null` before `Completed`, and the ordering is the point:
        // a transfer that failed part-way can leave a completed connection behind it, and
        // "Sync Complete!" over a failed import is the worst thing this screen can say.
        setContent(
            SyncUiState(
                mode = SyncMode.RECEIVE,
                connectionState = ConnectionState.Completed(bytesReceived = 0),
                error = UiError(message = R.string.sync_failed),
            )
        )

        composeRule.onNodeWithText(string(R.string.sync_failed)).assertExists()
        composeRule.onNodeWithText(string(R.string.sync_complete)).assertDoesNotExist()
    }

    @Test
    fun `a cancellation is shown instead of the pairing screen`() {
        // `Cancelled` is checked before `Connecting`. Reordering them offers "Accept" for a
        // connection the partner has already dropped.
        setContent(
            SyncUiState(
                mode = SyncMode.SEND,
                connectionState = ConnectionState.Cancelled(CancelReason.BY_PARTNER),
            )
        )

        composeRule.onNodeWithText(string(R.string.sync_accept)).assertDoesNotExist()
    }

    @Test
    fun `an error keeps the transcript and offers both a retry and a way out`() {
        setContent(
            SyncUiState(
                mode = SyncMode.RECEIVE,
                error = UiError(message = R.string.sync_failed),
                activityLog = listOf(ActivityLogEntry("Received 2 KB of data")),
            )
        )

        composeRule.onNodeWithText("Received 2 KB of data").assertExists()
        composeRule.onNodeWithText(string(R.string.try_again)).performClick()

        assertThat(events).containsExactly(SyncEvent.Cancel)
    }

    @Test
    fun `Close on a failure cancels before leaving`() {
        setContent(
            SyncUiState(mode = SyncMode.RECEIVE, error = UiError(message = R.string.sync_failed))
        )

        composeRule.onNodeWithText(string(R.string.close)).performClick()

        assertThat(events).containsExactly(SyncEvent.Cancel)
        assertThat(backs).isEqualTo(1)
    }

    @Test
    fun `the back arrow cancels the connection before leaving`() {
        setContent(
            SyncUiState(
                mode = SyncMode.SEND,
                connectionState = ConnectionState.Transferring(0.3f),
            )
        )

        composeRule.onNodeWithContentDescription(string(R.string.cd_back)).performClick()

        assertThat(events).containsExactly(SyncEvent.Cancel)
        assertThat(backs).isEqualTo(1)
    }

    // ── The transfer manifest ────────────────────────────────────────────────────────────────

    @Test
    fun `every category a payload can carry has a label of its own`() {
        // The card is driven entirely by `SyncPayload.categories()`, and the key→label map is a
        // `when` with an `else` that renders "No data". A category added to the payload without
        // a label here therefore shows up as a row reading *No data* beside a real count —
        // which reads as a bug in the transfer rather than a missing string. `SyncPayloadCoverage
        // Test` guards the data side; this is the presentation half of the same contract.
        val everyCategory = SyncPayload().categories().map { it.key }
        assertThat(everyCategory).isNotEmpty()

        setContent(
            SyncUiState(
                mode = SyncMode.RECEIVE,
                connectionState = ConnectionState.Completed(bytesReceived = 8192),
                dataSummary = SyncDataSummary(
                    categories = everyCategory.map { SyncCategory(key = it, count = 3) },
                    totalBytes = 8192,
                ),
            )
        )

        val rendered = composeRule.onRoot().fetchSemanticsNode().renderedTexts()
        assertThat(rendered).doesNotContain(string(R.string.sync_no_data))
        assertThat(rendered).containsAtLeast(
            string(R.string.sync_item_bookmarks),
            string(R.string.sync_item_reading_progress),
            string(R.string.sync_item_qaida_cells),
            string(R.string.sync_item_locations),
            string(R.string.sync_item_preferences),
        )
    }

    @Test
    fun `a key the map does not know renders the fallback rather than the raw key`() {
        // The `else` arm. A raw camelCase key in the manifest is worse than "No data": it looks
        // like an internal identifier leaking into the UI.
        setContent(
            SyncUiState(
                mode = SyncMode.RECEIVE,
                connectionState = ConnectionState.Completed(bytesReceived = 1),
                dataSummary = SyncDataSummary(
                    categories = listOf(SyncCategory(key = "somethingNew", count = 3)),
                    totalBytes = 1,
                ),
            )
        )

        composeRule.onNodeWithText(string(R.string.sync_no_data)).assertExists()
        composeRule.onNodeWithText("somethingNew").assertDoesNotExist()
    }

    @Test
    fun `only categories that actually carry something are listed`() {
        // Every payload reports all twenty-four categories, most of them empty. Listing the
        // empty ones would bury the two rows that matter under twenty-two zeroes.
        setContent(
            SyncUiState(
                mode = SyncMode.SEND,
                connectionState = ConnectionState.Completed(bytesReceived = 4096),
                dataSummary = SyncDataSummary(
                    categories = listOf(
                        SyncCategory(key = "bookmarks", count = 12),
                        SyncCategory(key = "zakatHistory", count = 0),
                    ),
                    totalBytes = 4096,
                ),
            )
        )

        composeRule.onNodeWithText(string(R.string.sync_item_bookmarks)).assertExists()
        composeRule.onNodeWithText(string(R.string.sync_item_zakat)).assertDoesNotExist()
        composeRule.onNodeWithText("12").assertExists()
    }

    @Test
    fun `a manifest with nothing in it says so`() {
        // A sync that moved nothing is a real outcome — a fresh install syncing to a fresh
        // install — and an empty card reads as a rendering failure.
        setContent(
            SyncUiState(
                mode = SyncMode.SEND,
                connectionState = ConnectionState.Completed(bytesReceived = 0),
                dataSummary = SyncDataSummary(
                    categories = listOf(SyncCategory(key = "bookmarks", count = 0)),
                    totalBytes = 0,
                ),
            )
        )

        composeRule.onNodeWithText(string(R.string.sync_no_data)).assertExists()
    }

    @Test
    fun `a flag category shows no count, because it has none to show`() {
        // Reading progress and preferences are present-or-absent, not countable. Rendering "1"
        // beside them invites the reader to wonder what the other zero was.
        setContent(
            SyncUiState(
                mode = SyncMode.RECEIVE,
                connectionState = ConnectionState.Completed(bytesReceived = 100),
                dataSummary = SyncDataSummary(
                    categories = listOf(
                        SyncCategory(key = "readingProgress", count = 1, isFlag = true)
                    ),
                    totalBytes = 100,
                ),
            )
        )

        composeRule.onNodeWithText(string(R.string.sync_item_reading_progress)).assertExists()
        composeRule.onNodeWithText("1").assertDoesNotExist()
    }

    @Test
    fun `the total size is shown only once there is one`() {
        setContent(
            SyncUiState(
                mode = SyncMode.SEND,
                connectionState = ConnectionState.Completed(bytesReceived = 0),
                dataSummary = SyncDataSummary(
                    categories = listOf(SyncCategory(key = "bookmarks", count = 4)),
                    totalBytes = 0,
                ),
            )
        )

        composeRule.onNodeWithText(string(R.string.sync_total_size)).assertDoesNotExist()
    }

    // ── Progress detail ──────────────────────────────────────────────────────────────────────

    @Test
    fun `the sender sees the partner's import step count while it runs`() {
        setContent(
            SyncUiState(
                mode = SyncMode.SEND,
                connectionState = ConnectionState.PartnerImporting(
                    step = 5,
                    total = 12,
                    label = "Importing khatam data...",
                ),
                stepsCompleted = 11,
                totalSteps = 13,
            )
        )

        composeRule.onNodeWithText("Importing khatam data...", substring = true).assertExists()
    }

    @Test
    fun `a partner import with no total yet does not divide by zero`() {
        // `ImportStarted` carries (0, 0) — the state every import passes through before the
        // first progress signal arrives.
        setContent(
            SyncUiState(
                mode = SyncMode.SEND,
                connectionState = ConnectionState.PartnerImporting(0, 0, "Starting..."),
                stepsCompleted = 11,
                totalSteps = 13,
            )
        )

        composeRule.onNodeWithText("Starting...", substring = true).assertExists()
    }

    @Test
    fun `a transfer in flight reports its percentage and the size being moved`() {
        setContent(
            SyncUiState(
                mode = SyncMode.SEND,
                connectionState = ConnectionState.Transferring(0.4f),
                stepsCompleted = 6,
                totalSteps = 13,
                dataSummary = SyncDataSummary(
                    categories = listOf(SyncCategory(key = "bookmarks", count = 4)),
                    totalBytes = 4096,
                ),
            )
        )

        composeRule.onNodeWithText(string(R.string.sync_transfer_progress)).assertExists()
        composeRule.onNodeWithText(
            string(R.string.sync_transferred_format, 40) + " (4 KB)"
        ).assertExists()
    }

    @Test
    fun `a transfer whose size is not known yet reports the percentage alone`() {
        // The sender learns the encoded size only after the export finishes, so the first
        // frames of the transfer have a percentage and no bytes.
        setContent(
            SyncUiState(
                mode = SyncMode.RECEIVE,
                connectionState = ConnectionState.Transferring(0.1f),
                dataSummary = null,
            )
        )

        composeRule.onNodeWithText(string(R.string.sync_transferred_format, 10)).assertExists()
    }

    @Test
    fun `the in-flight manifest says which way the data is moving`() {
        // The same card serves both devices, and "being sent" against "being received" is the
        // only thing on it that says which one you are looking at.
        setContent(
            SyncUiState(
                mode = SyncMode.SEND,
                connectionState = ConnectionState.Transferring(0.5f),
                dataSummary = SyncDataSummary(
                    categories = listOf(SyncCategory(key = "bookmarks", count = 4)),
                    totalBytes = 4096,
                ),
            )
        )

        composeRule.onNodeWithText(string(R.string.sync_data_being_sent)).assertExists()
        composeRule.onNodeWithText(string(R.string.sync_data_being_received)).assertDoesNotExist()
    }

    @Test
    fun `the receiving device's in-flight manifest says received`() {
        setContent(
            SyncUiState(
                mode = SyncMode.RECEIVE,
                connectionState = ConnectionState.Transferring(0.5f),
                dataSummary = SyncDataSummary(
                    categories = listOf(SyncCategory(key = "bookmarks", count = 4)),
                    totalBytes = 4096,
                ),
            )
        )

        composeRule.onNodeWithText(string(R.string.sync_data_being_received)).assertExists()
    }

    /** Every string the tree renders, flattened. */
    private fun androidx.compose.ui.semantics.SemanticsNode.renderedTexts(): List<String> =
        config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.Text).orEmpty()
            .map { it.text } + children.flatMap { it.renderedTexts() }
}
