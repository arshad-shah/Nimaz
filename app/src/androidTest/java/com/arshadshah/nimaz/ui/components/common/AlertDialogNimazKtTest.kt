package com.arshadshah.nimaz.ui.components.common

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.activities.MainActivity
import com.arshadshah.nimaz.ui.theme.NimazTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class AlertDialogNimazTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    // Test utility function to set up basic dialog
    private fun launchBasicDialog(
        showConfirmButton: Boolean = true,
        showDismissButton: Boolean = true,
        onConfirm: () -> Unit = {},
        onDismiss: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            NimazTheme  {
                AlertDialogNimaz(
                    title = "Test Dialog",
                    contentDescription = "Test Description",
                    contentToShow = { Text("Test Content") },
                    onDismissRequest = {},
                    showConfirmButton = showConfirmButton,
                    showDismissButton = showDismissButton,
                    onConfirm = onConfirm,
                    onDismiss = onDismiss
                )
            }
        }
    }

    @Test
    fun dialogDisplaysTitle() {
        launchBasicDialog()
        composeTestRule.onNodeWithText("Test Dialog").assertExists()
    }

    @Test
    fun dialogDisplaysContent() {
        launchBasicDialog()
        composeTestRule.onNodeWithText("Test Content").assertExists()
    }

    @Test
    fun confirmButtonTriggersCallback() {
        var confirmClicked = false
        launchBasicDialog(onConfirm = { confirmClicked = true })

        composeTestRule.onNodeWithText("Done").performClick()
        assert(confirmClicked)
    }

    @Test
    fun dismissButtonTriggersCallback() {
        var dismissClicked = false
        launchBasicDialog(onDismiss = { dismissClicked = true })

        composeTestRule.onNodeWithText("Cancel").performClick()
        assert(dismissClicked)
    }

    @Test
    fun hideConfirmButtonWhenSpecified() {
        launchBasicDialog(showConfirmButton = false)
        composeTestRule.onNodeWithText("Done").assertDoesNotExist()
    }

    @Test
    fun hideDismissButtonWhenSpecified() {
        launchBasicDialog(showDismissButton = false)
        composeTestRule.onNodeWithText("Cancel").assertDoesNotExist()
    }

    @Test
    fun dialogWithIconDisplaysIcon() {
        composeTestRule.setContent {
            MaterialTheme {
                AlertDialogNimaz(
                    title = "Test Dialog",
                    icon = painterResource(id = R.drawable.mail_icon),
                    contentDescription = "Test Description",
                    contentToShow = { Text("Test Content") },
                    onDismissRequest = {},
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }
        composeTestRule.onNodeWithContentDescription("Test Description").assertExists()
    }

    @Test
    fun dialogWithDescriptionDisplaysDescription() {
        composeTestRule.setContent {
            MaterialTheme {
                AlertDialogNimaz(
                    title = "Test Dialog",
                    contentDescription = "Test Description",
                    description = "Test Description Text",
                    contentToShow = { Text("Test Content") },
                    onDismissRequest = {},
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Test Description Text").assertExists()
    }

    @Test
    fun dialogWithCustomContentHeight() {
        composeTestRule.setContent {
            MaterialTheme {
                AlertDialogNimaz(
                    title = "Test Dialog",
                    contentDescription = "Test Description",
                    contentHeight = 200.dp,
                    contentToShow = { Text("Test Content") },
                    onDismissRequest = {},
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Test Content").assertExists()
    }

    @Test
    fun dialogWithCustomButtonText() {
        composeTestRule.setContent {
            MaterialTheme {
                AlertDialogNimaz(
                    title = "Test Dialog",
                    contentDescription = "Test Description",
                    contentToShow = { Text("Test Content") },
                    onDismissRequest = {},
                    onConfirm = {},
                    onDismiss = {},
                    confirmButtonText = "Custom Confirm",
                    dismissButtonText = "Custom Dismiss"
                )
            }
        }
        composeTestRule.onNodeWithText("Custom Confirm").assertExists()
        composeTestRule.onNodeWithText("Custom Dismiss").assertExists()
    }

    @Test
    fun dialogWithAction() {
        var actionClicked = false
        composeTestRule.setContent {
            MaterialTheme {
                AlertDialogNimaz(
                    title = "Test Dialog",
                    contentDescription = "Test Description",
                    contentToShow = { Text("Test Content") },
                    onDismissRequest = {},
                    onConfirm = {},
                    onDismiss = {},
                    action = {
                        androidx.compose.material3.IconButton(
                            onClick = { actionClicked = true }
                        ) {
                            androidx.compose.material3.Icon(
                                painter = painterResource(id = R.drawable.play_icon),
                                contentDescription = "Action"
                            )
                        }
                    }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Action").performClick()
        assert(actionClicked)
    }

    @Test
    fun dialogWithOutlinedTextField() {
        composeTestRule.setContent {
            MaterialTheme {
                AlertDialogNimaz(
                    title = "Test Dialog",
                    contentDescription = "Test Description",
                    contentToShow = {
                        androidx.compose.material3.OutlinedTextField(
                            value = "Test Input",
                            onValueChange = {},
                            label = { Text("Test Label") }
                        )
                    },
                    cardContent = false,
                    contentHeight = 100.dp,
                    onDismissRequest = {},
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Test Label").assertExists()
        composeTestRule.onNodeWithText("Test Input").assertExists()
    }

    @Test
    fun dialogWithScrollableContent() {
        composeTestRule.setContent {
            MaterialTheme {
                AlertDialogNimaz(
                    title = "Test Dialog",
                    contentDescription = "Test Description",
                    contentToShow = {
                        repeat(20) {
                            Text("Scrollable Content $it")
                        }
                    },
                    onDismissRequest = {},
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }

        // Verify first and last items exist
        composeTestRule.onNodeWithText("Scrollable Content 0").assertExists()
        composeTestRule.onNodeWithText("Scrollable Content 19").assertExists()
    }
}