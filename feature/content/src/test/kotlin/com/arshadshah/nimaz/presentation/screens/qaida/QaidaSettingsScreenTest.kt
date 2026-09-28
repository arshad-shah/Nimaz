package com.arshadshah.nimaz.presentation.screens.qaida

import androidx.compose.ui.test.*
import com.arshadshah.nimaz.data.qaida.QaidaLearningSettings
import com.arshadshah.nimaz.presentation.viewmodel.content.*
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

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h1200dp")
class QaidaSettingsScreenTest {
    @get:Rule val rule = createComponentComposeRule()
    private val preferences = MutableStateFlow(QaidaLearningSettings())
    private val events = mutableListOf<QaidaReaderEvent>()
    private var resets = 0
    private val vm: QaidaReaderViewModel = mockk(relaxed = true) {
        every { settings } returns preferences
        every { cacheBytes } returns MutableStateFlow(1048576L)
        every { onEvent(any()) } answers { events += firstArg<QaidaReaderEvent>() }
    }
    private fun show() = rule.setThemedContent { QaidaSettingsScreen({}, vm, { resets++ }) }
    @Test fun `preferences send explicit changes and reflect persisted values`() {
        show()
        rule.onNodeWithText("Transliteration reminders").performClick()
        rule.onNodeWithText("Slower audio").performClick()
        assertThat(events).containsExactly(QaidaReaderEvent.SetTransliteration(false), QaidaReaderEvent.SetSlow(true))
        rule.runOnIdle { preferences.value = QaidaLearningSettings(false, true) }
        rule.onAllNodes(isToggleable()).filter(isOn()).assertCountEquals(1)
    }
    @Test fun `removing audio needs confirmation and does not reset progress`() {
        show()
        rule.onNodeWithText("Remove downloaded audio").performClick()
        assertThat(events).isEmpty()
        rule.onAllNodesWithText("Remove downloaded audio").filter(hasClickAction()).onLast().performClick()
        assertThat(events).containsExactly(QaidaReaderEvent.ClearAudio)
        assertThat(resets).isEqualTo(0)
    }
    @Test fun `confirmed reset exits an active reader session`() {
        show()
        rule.onNodeWithText("Reset journey").performClick()
        assertThat(events).isEmpty()
        rule.onNodeWithText("Reset").performClick()
        assertThat(events).containsExactly(QaidaReaderEvent.ResetJourney)
        assertThat(resets).isEqualTo(1)
    }
}
