package com.arshadshah.nimaz.ui.screens.introduction

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.activities.Introduction
import org.junit.Rule
import org.junit.Test

class IntroPage1KtTest
{
	@get:Rule
	val composeTestRule = createAndroidComposeRule<Introduction>()

	@Test
	fun testIntroPage1Composable() {
		composeTestRule.onNodeWithTag("introPager").assertIsDisplayed()
		composeTestRule.onNodeWithTag("introPagerIndicator").assertIsDisplayed()
		composeTestRule.onNodeWithTag("introButtons").assertIsDisplayed()

		composeTestRule.onNodeWithTag("pagerScreen 0").assertIsDisplayed()
		composeTestRule.onNodeWithTag("pagerScreen 1").assertDoesNotExist()
		composeTestRule.onNodeWithTag("pagerScreen 2").assertDoesNotExist()
		composeTestRule.onNodeWithTag("pagerScreen 3").assertDoesNotExist()
		composeTestRule.onNodeWithTag("pagerScreen 4").assertDoesNotExist()
		composeTestRule.onNodeWithTag("pagerScreen 5").assertDoesNotExist()
		composeTestRule.onNodeWithTag("pagerScreen 6").assertDoesNotExist()
	}

	@Test
	fun test_intro_pages_next_button_moves_to_next_page(){
		composeTestRule.onNodeWithTag("introPager").assertIsDisplayed()
		composeTestRule.onNodeWithTag("introPagerIndicator").assertIsDisplayed()
		composeTestRule.onNodeWithTag("introButtons").assertIsDisplayed()

		composeTestRule.onNodeWithText("Next").performClick()
		composeTestRule.onNodeWithTag("pagerScreen 1").assertIsDisplayed()
	}

	@Test
	fun test_intro_pages_back_button_moves_to_back_page(){
		composeTestRule.onNodeWithTag("introPager").assertIsDisplayed()
		composeTestRule.onNodeWithTag("introPagerIndicator").assertIsDisplayed()
		composeTestRule.onNodeWithTag("introButtons").assertIsDisplayed()

		composeTestRule.onNodeWithText("Next").performClick()
		composeTestRule.onNodeWithText("Back").performClick()
		composeTestRule.onNodeWithTag("pagerScreen 0").assertIsDisplayed()
	}

	@Test
	fun test_intro_pages_get_started_button_moves_to_main_activity()
	{
		composeTestRule.onNodeWithTag("introPager").assertIsDisplayed()
		composeTestRule.onNodeWithTag("introPagerIndicator").assertIsDisplayed()
		composeTestRule.onNodeWithTag("introButtons").assertIsDisplayed()

		//click next 6 times to get to the last page
		composeTestRule.onNodeWithText("Next").performClick()
		composeTestRule.onNodeWithText("Next").performClick()
		composeTestRule.onNodeWithText("Next").performClick()
		composeTestRule.onNodeWithText("Next").performClick()
		composeTestRule.onNodeWithText("Next").performClick()
		composeTestRule.onNodeWithText("Next").performClick()

		composeTestRule.onNodeWithText("Let's Get Started").assertIsDisplayed()
		composeTestRule.onNodeWithText("Let's Get Started").performClick()

		//check that the main activity is displayed
		composeTestRule.onNodeWithTag("mainActivity").assertIsDisplayed()
	}

}