package com.arshadshah.nimaz.ui.screens.introduction

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performGesture
import androidx.test.espresso.action.ViewActions.swipeLeft
import org.junit.Rule
import org.junit.Test

class IntroPage1KtTest
{
	@get:Rule
	val composeTestRule = createComposeRule()

	@Test
	fun testIntroPage1Composable() {
		// Test the composition of the IntroPage1 composable
		composeTestRule.setContent {
			IntroPage1()
		}
		composeTestRule.onNodeWithTag("introPager").assertIsDisplayed()
		composeTestRule.onNodeWithTag("introPagerIndicator").assertIsDisplayed()
		composeTestRule.onNodeWithTag("pagerScreen").assertIsDisplayed()

		//swipe left and count the number of pages displayed
		val pager = composeTestRule.onNodeWithTag("introPager")
		//make sure that it swipes left 6 times
		pager.performGesture {
			for (i in 0..6) {
				swipeLeft()
				//check that the page is displayed
				composeTestRule.onNodeWithTag("pagerScreen").assertIsDisplayed()
			}
		}
	}
}