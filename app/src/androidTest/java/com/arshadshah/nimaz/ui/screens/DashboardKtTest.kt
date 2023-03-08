package com.arshadshah.nimaz.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.activities.MainActivity
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_CALENDER
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_EVENTS_CARD
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_HOME
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_HOME_PRAYER_TIMES_CARD
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_PRAYER_TIMES
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_TRACKERS_CARD
import org.junit.Rule
import org.junit.Test

class DashboardKtTest
{

	@get:Rule
	val composeTestRule = createAndroidComposeRule<MainActivity>()

	@Test
	fun dashboard()
	{
		composeTestRule.onNodeWithTag(TEST_TAG_HOME).assertExists()
	}

	@Test
	fun dashboardPrayerTimesCard()
	{
		composeTestRule.onNodeWithTag(TEST_TAG_HOME_PRAYER_TIMES_CARD).assertIsDisplayed()
	}

	@Test
	fun dashboardEventsCard()
	{
		composeTestRule.onNodeWithTag(TEST_TAG_EVENTS_CARD).assertIsDisplayed()
	}

	//TEST_TAG_TRACKERS_CARD
	@Test
	fun dashboardTrackersCard()
	{
		composeTestRule.onNodeWithTag(TEST_TAG_TRACKERS_CARD).assertIsDisplayed()
	}


	//clicking on prayer times card
	@Test
	fun clickingOnPrayerTimesCard()
	{
		composeTestRule.onNodeWithTag(TEST_TAG_HOME_PRAYER_TIMES_CARD).performClick()
		composeTestRule.onNodeWithTag(TEST_TAG_PRAYER_TIMES).assertIsDisplayed()
	}

	//test click events
	@Test
	fun clickingOnEventsCard()
	{
		composeTestRule.onNodeWithTag(TEST_TAG_EVENTS_CARD).performClick()
		composeTestRule.onNodeWithTag(TEST_TAG_CALENDER).assertIsDisplayed()
	}
}