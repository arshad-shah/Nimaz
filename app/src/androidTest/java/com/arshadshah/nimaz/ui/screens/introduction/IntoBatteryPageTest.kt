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

class IntoBatteryPageTest
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
		composeTestRule.onNodeWithText("Next").performClick()
		composeTestRule.onNodeWithText("Next").performClick()
	}


	@Test
	fun verifyBatteryExemptionPageStructure()
	{
		composeTestRule.onNodeWithText("Battery Exemption").assertIsDisplayed()
		composeTestRule.onNodeWithText("Nimaz needs to be exempted from battery optimization to show adhan notifications Correctly.").assertIsDisplayed()
		composeTestRule.onNodeWithText("Enable Battery Exemption").assertIsDisplayed()
		composeTestRule.onNodeWithTag("BatteryExemptionSwitch").assertIsDisplayed()
		composeTestRule.onNodeWithText("Next").assertIsDisplayed()
		composeTestRule.onNodeWithText("Back").assertIsDisplayed()
	}
}