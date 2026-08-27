package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The accordion, in both of the forms it ships.
 *
 * There are two overloads, and they are not interchangeable: one keeps its own expansion state and
 * the other is hoisted. The hoisted one exists because an "only one section open at a time" list
 * cannot be built from self-managing rows — each would open independently and the caller could not
 * close the others. A caller reaching for the wrong one gets an accordion that ignores the state it
 * is handed, which looks like the component being broken rather than the call site being wrong.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class NimazAccordionVariantsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `a self-managing accordion opens and closes on its own`() {
        composeRule.setThemedContent {
            NimazAccordion(
                title = "What is nisab?",
                subtitle = "The zakat threshold",
                leadingIcon = Icons.Filled.Info,
            ) {
                Text("the answer")
            }
        }

        composeRule.onNodeWithText("the answer").assertDoesNotExist()
        composeRule.onNodeWithText("What is nisab?").performClick()
        composeRule.onNodeWithText("the answer").assertExists()
    }

    @Test
    fun `it can start open`() {
        composeRule.setThemedContent {
            NimazAccordion(title = "Open already", initiallyExpanded = true) {
                Text("visible from the start")
            }
        }

        composeRule.onNodeWithText("visible from the start").assertExists()
    }

    @Test
    fun `every style renders`() {
        composeRule.setThemedContent {
            Column {
                NimazAccordionStyle.entries.forEach { style ->
                    NimazAccordion(
                        title = style.name,
                        style = style,
                        initiallyExpanded = true,
                        trailing = { Text("t-${style.name}") },
                    ) {
                        Text("body-${style.name}")
                    }
                }
            }
        }

        NimazAccordionStyle.entries.forEach {
            composeRule.onNodeWithText("body-${it.name}").assertExists()
            composeRule.onNodeWithText("t-${it.name}").assertExists()
        }
    }

    @Test
    fun `the hoisted overload reports its change and honours what it is given`() {
        // The overload an "only one open" list needs. It must not keep its own state: the caller
        // closing it has to close it.
        val changes = mutableListOf<Boolean>()
        composeRule.setThemedContent {
            var expanded by remember { mutableStateOf(false) }
            NimazAccordion(
                title = "Hoisted",
                expanded = expanded,
                onExpandedChange = {
                    changes += it
                    expanded = it
                },
                subtitle = "controlled",
                leadingIcon = Icons.Filled.Info,
            ) {
                Text("hoisted body")
            }
        }

        composeRule.onNodeWithText("hoisted body").assertDoesNotExist()
        composeRule.onNodeWithText("Hoisted").performClick()
        composeRule.onNodeWithText("hoisted body").assertExists()
        composeRule.onNodeWithText("Hoisted").performClick()
        composeRule.onNodeWithText("hoisted body").assertDoesNotExist()

        assertThat(changes).containsExactly(true, false).inOrder()
    }

    @Test
    fun `a hoisted accordion the caller keeps shut stays shut`() {
        // The failure that tells the two overloads apart: an accordion managing its own state
        // would open on the tap regardless of what it was passed.
        composeRule.setThemedContent {
            NimazAccordion(
                title = "Locked open state",
                expanded = false,
                onExpandedChange = {},
            ) {
                Text("never shown")
            }
        }

        composeRule.onNodeWithText("Locked open state").performClick()
        composeRule.onNodeWithText("never shown").assertDoesNotExist()
    }
}
