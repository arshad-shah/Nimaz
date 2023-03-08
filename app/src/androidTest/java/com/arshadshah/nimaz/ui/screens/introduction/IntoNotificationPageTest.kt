package com.arshadshah.nimaz.ui.screens.introduction

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.activities.Introduction
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class IntoNotificationPageTest
{
	@get:Rule
	val composeTestRule = createAndroidComposeRule<Introduction>()


	@Before
	fun setUp()
	{
		composeTestRule.onNodeWithText("Next").performClick()
		composeTestRule.onNodeWithText("Next").performClick()
		composeTestRule.onNodeWithText("Next").performClick()
		composeTestRule.onNodeWithText("Next").performClick()
	}


	@Test
	fun verifyNotificationPageStructure()
	{
		composeTestRule.onNodeWithText("Adhan Notifications").assertIsDisplayed()
		composeTestRule.onNodeWithText("Enable Adhan Notifications for Nimaz to get Prayer alerts in the form of Adhan.").assertIsDisplayed()
		composeTestRule.onNodeWithText("Enable Notifications").assertIsDisplayed()
		composeTestRule.onNodeWithTag("notification_switch_on_intro_screen").assertIsDisplayed()
		composeTestRule.onNodeWithText("Next").assertIsDisplayed()
		composeTestRule.onNodeWithText("Back").assertIsDisplayed()
	}
}