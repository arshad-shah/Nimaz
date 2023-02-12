package com.arshadshah.nimaz.ui.screens.introduction

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class PagerScreenKtTest
{
	@get:Rule
	val composeTestRule = createAndroidComposeRule<ComponentActivity>()

	//oboarding object
	private val onboarding = OnBoardingPage.First
	private val title = onboarding.title
	private val description = onboarding.description

	private val onboardingwithextra = OnBoardingPage.Seventh
	private val titlewithextra = onboardingwithextra.title
	private val descriptionwithextra = onboardingwithextra.description

	@Test
	fun testPagerScreen() {
		composeTestRule.setContent {
			PagerScreen(onboarding)
		}

		composeTestRule.onNodeWithTag("pagerScreen").assertExists().assertIsDisplayed()
		composeTestRule.onNodeWithTag("pagerScreenTitle").assertExists().assertIsDisplayed()
		composeTestRule.onNodeWithTag("pagerScreenImage").assertExists().assertIsDisplayed()
		composeTestRule.onNodeWithTag("pagerScreenDescription").assertExists().assertIsDisplayed()

		composeTestRule.onNodeWithTag("pagerScreenTitle").assertTextEquals(title)
		composeTestRule.onNodeWithTag("pagerScreenDescription").assertTextEquals(description)
	}

	@Test
	fun testPagerScreenWithExtra() {
		composeTestRule.setContent {
			PagerScreen(onboardingwithextra)
		}

		composeTestRule.onNodeWithTag("pagerScreen").assertExists().assertIsDisplayed()
		composeTestRule.onNodeWithTag("pagerScreenTitle").assertExists().assertIsDisplayed()
		composeTestRule.onNodeWithTag("pagerScreenImage").assertExists().assertIsDisplayed()
		composeTestRule.onNodeWithTag("pagerScreenDescription").assertExists().assertIsDisplayed()
		composeTestRule.onNodeWithTag("pagerScreenExtra").assertExists().assertIsDisplayed()

		composeTestRule.onNodeWithTag("pagerScreenTitle").assertTextEquals(titlewithextra)
		composeTestRule.onNodeWithTag("pagerScreenDescription").assertTextEquals(descriptionwithextra)
	}

}