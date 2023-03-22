package com.arshadshah.nimaz.ui.screens

import android.content.res.Resources
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.activities.MainActivity
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.navigation.BottomNavItem
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NamesOfAllahTest {

	@get:Rule
	val composeTestRule = createAndroidComposeRule<MainActivity>()

	private lateinit var resources: Resources

	@Before
	fun setup() {
		resources = composeTestRule.activity.resources
	}

	@Test
	fun test_NamesOfAllah_displays_names() {
		//click on the more button in the bottom navigation bar
		composeTestRule.onNodeWithContentDescription(BottomNavItem.MoreScreen.title).performClick()

		//click on the tasbih button in the more screen
		composeTestRule.onNodeWithTag("More Link Names of Allah").performClick()
		//verify that the more screen is displayed
		composeTestRule.onNodeWithTag(AppConstants.TEST_TAG_NAMES_OF_ALLAH).assertIsDisplayed()

		composeTestRule.onNodeWithText(resources.getStringArray(R.array.Arabic)[0]).assertExists()
		composeTestRule.onNodeWithText(resources.getStringArray(R.array.English)[1]).assertExists()
		composeTestRule.onNodeWithText(resources.getStringArray(R.array.English)[2]).assertExists()
	}

	@Test
	fun test_NamesOfAllahRow_displays_english_name() {
		//click on the more button in the bottom navigation bar
		composeTestRule.onNodeWithContentDescription(BottomNavItem.MoreScreen.title).performClick()

		//click on the tasbih button in the more screen
		composeTestRule.onNodeWithTag("More Link Names of Allah").performClick()
		//verify that the more screen is displayed
		composeTestRule.onNodeWithTag(AppConstants.TEST_TAG_NAMES_OF_ALLAH).assertIsDisplayed()
		composeTestRule.onNodeWithText(resources.getStringArray(R.array.English)[0]).assertExists()

	}

	@Test
	fun test_NamesOfAllahRow_displays_arabic_name() {
		//click on the more button in the bottom navigation bar
		composeTestRule.onNodeWithContentDescription(BottomNavItem.MoreScreen.title).performClick()

		//click on the tasbih button in the more screen
		composeTestRule.onNodeWithTag("More Link Names of Allah").performClick()
		//verify that the more screen is displayed
		composeTestRule.onNodeWithTag(AppConstants.TEST_TAG_NAMES_OF_ALLAH).assertIsDisplayed()
		composeTestRule.onNodeWithText(resources.getStringArray(R.array.Arabic)[0]).assertExists()
	}

	@Test
	fun test_NamesOfAllahRow_displays_translation() {
		//click on the more button in the bottom navigation bar
		composeTestRule.onNodeWithContentDescription(BottomNavItem.MoreScreen.title).performClick()

		//click on the tasbih button in the more screen
		composeTestRule.onNodeWithTag("More Link Names of Allah").performClick()
		//verify that the more screen is displayed
		composeTestRule.onNodeWithTag(AppConstants.TEST_TAG_NAMES_OF_ALLAH).assertIsDisplayed()
		composeTestRule.onNodeWithText(resources.getStringArray(R.array.translation)[0]).assertExists()
	}

	@Test
	fun test_NamesOfAllah_displays_99_names() {
//click on the more button in the bottom navigation bar
		composeTestRule.onNodeWithContentDescription(BottomNavItem.MoreScreen.title).performClick()

		//click on the tasbih button in the more screen
		composeTestRule.onNodeWithTag("More Link Names of Allah").performClick()
		//verify that the more screen is displayed
		composeTestRule.onNodeWithTag(AppConstants.TEST_TAG_NAMES_OF_ALLAH).assertIsDisplayed()

		val arabicNames = resources.getStringArray(R.array.Arabic)
		val englishNames = resources.getStringArray(R.array.English)
		val translationNames = resources.getStringArray(R.array.translation)

		for (i in 0..98) {
			composeTestRule.onNodeWithTag(AppConstants.TEST_TAG_NAMES_OF_ALLAH).performScrollToIndex(i)
			composeTestRule.onNodeWithText(arabicNames[i]).assertExists()
			composeTestRule.onNodeWithText(englishNames[i]).assertExists()
			composeTestRule.onNodeWithText(translationNames[i]).assertExists()
		}
	}

	@Test
	fun test_NamesOfAllahRow_play_audio()
	{
		//click on the more button in the bottom navigation bar
		composeTestRule.onNodeWithContentDescription(BottomNavItem.MoreScreen.title).performClick()

		composeTestRule.onNodeWithTag("More Link Names of Allah").performClick()

		//verify that the more screen is displayed
		composeTestRule.onNodeWithTag(AppConstants.TEST_TAG_NAMES_OF_ALLAH).assertIsDisplayed()

		//click on the tasbih button in the more screen
		composeTestRule.onNodeWithContentDescription("Stop playing").assertDoesNotExist()
		//click on the tasbih button in the more screen
		composeTestRule.onNodeWithContentDescription("Play").assertExists()
		composeTestRule.onNodeWithContentDescription("Play").performClick()
		composeTestRule.onNodeWithContentDescription("Stop playing").assertExists()
		composeTestRule.onNodeWithContentDescription("Pause playing").assertExists()
	}

}