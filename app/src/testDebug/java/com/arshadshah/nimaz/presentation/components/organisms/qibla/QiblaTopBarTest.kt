package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for [com.arshadshah.nimaz.presentation.components.organisms.qibla.QiblaTopBar].
 * Plain Surface + location label + pill tabs — no hardware dependency, so it renders
 * cleanly under Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
class QiblaTopBarTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun rendersLocationNameWhenProvided() {
        composeRule.setThemedContent {
            com.arshadshah.nimaz.presentation.components.organisms.qibla.QiblaTopBar(
                locationName = "London, UK",
                latitude = 51.5074,
                longitude = -0.1278,
                fallbackTitle = "Qibla Compass",
                tabs = listOf("Compass", "AR"),
                selectedIndex = 0,
                onTabSelect = {}
            )
        }

        composeRule.onNodeWithText("London, UK").assertExists()
    }

    @Test
    fun rendersFallbackTitleWhenLocationNameIsNull() {
        composeRule.setThemedContent {
            com.arshadshah.nimaz.presentation.components.organisms.qibla.QiblaTopBar(
                locationName = null,
                latitude = null,
                longitude = null,
                fallbackTitle = "Qibla Compass",
                tabs = listOf("Compass", "AR"),
                selectedIndex = 0,
                onTabSelect = {}
            )
        }

        composeRule.onNodeWithText("Qibla Compass").assertExists()
    }

    @Test
    fun rendersTabLabels() {
        composeRule.setThemedContent {
            com.arshadshah.nimaz.presentation.components.organisms.qibla.QiblaTopBar(
                locationName = "London, UK",
                latitude = 51.5074,
                longitude = -0.1278,
                fallbackTitle = "Qibla Compass",
                tabs = listOf("Compass", "AR"),
                selectedIndex = 0,
                onTabSelect = {}
            )
        }

        composeRule.onNodeWithText("Compass").assertExists()
        composeRule.onNodeWithText("AR").assertExists()
    }

    @Test
    fun tabClickFiresCallbackWithIndex() {
        var selected = -1
        composeRule.setThemedContent {
            com.arshadshah.nimaz.presentation.components.organisms.qibla.QiblaTopBar(
                locationName = "London, UK",
                latitude = 51.5074,
                longitude = -0.1278,
                fallbackTitle = "Qibla Compass",
                tabs = listOf("Compass", "AR"),
                selectedIndex = 0,
                onTabSelect = { selected = it }
            )
        }

        composeRule.onNodeWithText("AR").performClick()

        assertThat(selected).isEqualTo(1)
    }
}
