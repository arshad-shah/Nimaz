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

class IntoLocationPageTest
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
		composeTestRule.onNodeWithText("Next").performClick()
	}


	@Test
	fun verifyLocationPageStructure()
	{
		composeTestRule.onNodeWithText("Location").assertIsDisplayed()
		composeTestRule.onNodeWithText("Nimaz needs your location to get accurate prayer times and calculate Qibla direction. You can also use manual location.").assertIsDisplayed()
		composeTestRule.onNodeWithTag("LocationSwitch").assertIsDisplayed()
		composeTestRule.onNodeWithText("Next").assertIsDisplayed()
		composeTestRule.onNodeWithText("Back").assertIsDisplayed()
	}
}