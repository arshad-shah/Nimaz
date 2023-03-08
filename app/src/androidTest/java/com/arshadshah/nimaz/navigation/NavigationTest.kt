package com.arshadshah.nimaz.navigation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.arshadshah.nimaz.activities.MainActivity
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_ABOUT
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_ABOUT_PAGE
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_HOME
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_MORE
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_PRAYER_TIMES
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_PRAYER_TIMES_CUSTOMIZATION
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_QURAN
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_QURAN_JUZ
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_QURAN_SURAH
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_SETTINGS
import com.arshadshah.nimaz.ui.navigation.BottomNavItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
class NavigationTest
{

	@get:Rule
	val composeTestRule = createAndroidComposeRule<MainActivity>()

	@Test
	fun  navHost_verify_start_destination(){
		composeTestRule.onNodeWithTag(TEST_TAG_HOME).assertIsDisplayed()
	}

	@Test
	fun navHost_verify_click_on_prayer_opens_prayer_screen(){
		//click on the prayer times button in the bottom navigation bar
		composeTestRule.onNodeWithContentDescription(BottomNavItem.PrayerTimesScreen.title).performClick()
		//verify that the prayer times screen is displayed
		composeTestRule.onNodeWithTag(TEST_TAG_PRAYER_TIMES).assertIsDisplayed()
	}

	@Test
	fun navHost_verify_click_on_quran_opens_quran_screen(){
		//click on the quran button in the bottom navigation bar
		composeTestRule.onNodeWithContentDescription(BottomNavItem.QuranScreen.title).performClick()
		//verify that the quran screen is displayed
		composeTestRule.onNodeWithTag(TEST_TAG_QURAN).assertIsDisplayed()
	}

	//clicking the tab 1 should open the juz screen
	@Test
	fun navHost_verify_click_on_tab_1_opens_juz_screen(){
		//click on the tab 1 in the bottom navigation bar
		composeTestRule.onNodeWithContentDescription(BottomNavItem.QuranScreen.title).performClick()
		composeTestRule.onNodeWithTag("QURAN TAB 1").performClick()
		//verify that the juz screen is displayed
		composeTestRule.onNodeWithTag(TEST_TAG_QURAN_JUZ).assertIsDisplayed()
	}

	@Test
	fun navHost_verify_click_on_tab_0_opens_sura_screen(){
		//click on the tab 2 in the bottom navigation bar
		composeTestRule.onNodeWithContentDescription(BottomNavItem.QuranScreen.title).performClick()
		composeTestRule.onNodeWithTag("QURAN TAB 0").performClick()
		//verify that the sura screen is displayed
		composeTestRule.onNodeWithTag(TEST_TAG_QURAN_SURAH).assertIsDisplayed()
	}

	@Test
	fun navHost_verify_click_on_settings_opens_settings_screen(){
		//click on the settings button in the bottom navigation bar
		composeTestRule.onNodeWithContentDescription(BottomNavItem.SettingsScreen.title).performClick()
		//verify that the settings screen is displayed
		composeTestRule.onNodeWithTag(TEST_TAG_SETTINGS).assertIsDisplayed()
	}
	@Test
	fun navHost_verify_click_on_prayer_times_customization_opens_the_customization_screen(){
		//click on the settings button in the bottom navigation bar
		composeTestRule.onNodeWithContentDescription(BottomNavItem.SettingsScreen.title).performClick()
		//verify that the settings screen is displayed
		composeTestRule.onNodeWithTag(TEST_TAG_SETTINGS).assertIsDisplayed()
		//click on the button
		composeTestRule.onNodeWithTag(AppConstants.TEST_TAG_PRAYER_TIMES_CUSTOMIZATION_BUTTON).performClick()
		composeTestRule.onNodeWithTag(TEST_TAG_PRAYER_TIMES_CUSTOMIZATION).assertIsDisplayed()
	}
	@Test
	fun navHost_verify_click_on_about_opens_the_about_screen(){
		//click on the settings button in the bottom navigation bar
		composeTestRule.onNodeWithContentDescription(BottomNavItem.SettingsScreen.title).performClick()
		//verify that the settings screen is displayed
		composeTestRule.onNodeWithTag(TEST_TAG_SETTINGS).assertIsDisplayed()
		//scroll to the bottom
		composeTestRule.onNodeWithTag(TEST_TAG_ABOUT).performScrollTo()
		//click on the button
		composeTestRule.onNodeWithTag(TEST_TAG_ABOUT).performClick()
		composeTestRule.onNodeWithTag(TEST_TAG_ABOUT_PAGE).assertIsDisplayed()
	}
	@Test
	fun navHost_verify_click_on_home_opens_home_screen(){
		//click on the home button in the bottom navigation bar
		composeTestRule.onNodeWithContentDescription(BottomNavItem.Dashboard.title).performClick()
		//verify that the home screen is displayed
		composeTestRule.onNodeWithTag(TEST_TAG_HOME).assertIsDisplayed()
	}

	//more screen
	@Test
	fun navHost_verify_click_on_more_opens_more_screen(){
		//click on the more button in the bottom navigation bar
		composeTestRule.onNodeWithContentDescription(BottomNavItem.MoreScreen.title).performClick()
		//verify that the more screen is displayed
		composeTestRule.onNodeWithTag(TEST_TAG_MORE).assertIsDisplayed()
	}

	@Test
	fun navHost_verify_click_on_Tasbih_in_more_screen_opens_tasbih(){
		//click on the more button in the bottom navigation bar
		composeTestRule.onNodeWithContentDescription(BottomNavItem.MoreScreen.title).performClick()

		//click on the tasbih button in the more screen
		composeTestRule.onNodeWithTag("More Link Tasbih").performClick()
		//verify that the more screen is displayed
		composeTestRule.onNodeWithTag(AppConstants.TEST_TAG_TASBIH).assertIsDisplayed()
	}

	@Test
	fun navHost_verify_click_on_Azkar_in_more_screen_opens_azkar(){
		//click on the more button in the bottom navigation bar
		composeTestRule.onNodeWithContentDescription(BottomNavItem.MoreScreen.title).performClick()

		//click on the tasbih button in the more screen
		composeTestRule.onNodeWithTag("More Link Tasbih List").performClick()
		//verify that the more screen is displayed
		composeTestRule.onNodeWithTag(AppConstants.TEST_TAG_TASBIH_LIST).assertIsDisplayed()
	}

	@Test
	fun navHost_verify_click_on_Qibla_in_more_screen_opens_qibla(){
		//click on the more button in the bottom navigation bar
		composeTestRule.onNodeWithContentDescription(BottomNavItem.MoreScreen.title).performClick()

		//click on the tasbih button in the more screen
		composeTestRule.onNodeWithTag("More Link Qibla").performClick()
		//verify that the more screen is displayed
		composeTestRule.onNodeWithTag(AppConstants.TEST_TAG_QIBLA).assertIsDisplayed()
	}

	@Test
	fun navHost_verify_click_on_namesOfAllah_in_more_screen_opens_namesOfAllah(){
		//click on the more button in the bottom navigation bar
		composeTestRule.onNodeWithContentDescription(BottomNavItem.MoreScreen.title).performClick()

		//click on the tasbih button in the more screen
		composeTestRule.onNodeWithTag("More Link Names of Allah").performClick()
		//verify that the more screen is displayed
		composeTestRule.onNodeWithTag(AppConstants.TEST_TAG_NAMES_OF_ALLAH).assertIsDisplayed()
	}
	@Test
	fun navHost_verify_click_on_duas_in_more_screen_opens_duas(){
		//click on the more button in the bottom navigation bar
		composeTestRule.onNodeWithContentDescription(BottomNavItem.MoreScreen.title).performClick()

		//click on the tasbih button in the more screen
		composeTestRule.onNodeWithTag("More Link Duas").performClick()
		//verify that the more screen is displayed
		composeTestRule.onNodeWithTag(AppConstants.TEST_TAG_CHAPTERS).assertIsDisplayed()
	}
	@Test
	fun navHost_verify_click_on_trackers_in_more_screen_opens_trackers(){
		//click on the more button in the bottom navigation bar
		composeTestRule.onNodeWithContentDescription(BottomNavItem.MoreScreen.title).performClick()

		//click on the tasbih button in the more screen
		composeTestRule.onNodeWithTag("More Link Trackers").performClick()
		//verify that the more screen is displayed
		composeTestRule.onNodeWithTag(AppConstants.TEST_TAG_PRAYER_TRACKER).assertIsDisplayed()
	}
	@Test
	fun navHost_verify_click_on_calender_in_more_screen_opens_calender(){
		//click on the more button in the bottom navigation bar
		composeTestRule.onNodeWithContentDescription(BottomNavItem.MoreScreen.title).performClick()

		//click on the tasbih button in the more screen
		composeTestRule.onNodeWithTag("More Link Calender").performClick()
		//verify that the more screen is displayed
		composeTestRule.onNodeWithTag(AppConstants.TEST_TAG_CALENDER).assertIsDisplayed()
	}
	@Test
	fun navHost_verify_click_on_shahadah_in_more_screen_opens_shahadah(){
		//click on the more button in the bottom navigation bar
		composeTestRule.onNodeWithContentDescription(BottomNavItem.MoreScreen.title).performClick()

		//click on the tasbih button in the more screen
		composeTestRule.onNodeWithTag("More Link Calender").performClick()
		//verify that the more screen is displayed
		composeTestRule.onNodeWithTag(AppConstants.TEST_TAG_CALENDER).assertIsDisplayed()
	}

}