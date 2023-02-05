package com.arshadshah.nimaz.ui.screens

import android.content.res.Resources
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NamesOfAllahTest {
	@get:Rule
	val composeTestRule = createAndroidComposeRule<ComponentActivity>()

	private lateinit var resources: Resources

	@Before
	fun setup() {
		resources = composeTestRule.activity.resources
	}

	@Test
	fun test_NamesOfAllah_displays_names() {
		composeTestRule.setContent {
			NamesOfAllah(PaddingValues(0.dp))
		}

		composeTestRule.onNodeWithText(resources.getStringArray(R.array.Arabic)[0]).assertExists()
		composeTestRule.onNodeWithText(resources.getStringArray(R.array.English)[1]).assertExists()
		composeTestRule.onNodeWithText(resources.getStringArray(R.array.English)[2]).assertExists()
	}

	@Test
	fun test_NamesOfAllahRow_displays_english_name() {
		composeTestRule.setContent {
			NamesOfAllahRow(0, resources.getStringArray(R.array.English)[0], "", "")
		}

		composeTestRule.onNodeWithText(resources.getStringArray(R.array.English)[0]).assertExists()

	}

	@Test
	fun test_NamesOfAllahRow_displays_arabic_name() {
		composeTestRule.setContent {
			NamesOfAllahRow(0, "", resources.getStringArray(R.array.Arabic)[0], "")
		}

		composeTestRule.onNodeWithText(resources.getStringArray(R.array.Arabic)[0]).assertExists()
	}

	@Test
	fun test_NamesOfAllahRow_displays_translation() {
		composeTestRule.setContent {
			NamesOfAllahRow(0, "", "", resources.getStringArray(R.array.translation)[0])
		}

		composeTestRule.onNodeWithText(resources.getStringArray(R.array.translation)[0]).assertExists()
	}

	//test that names of allah is rederering a list of 99 names it is a list so we need to scroll to see all the names
	//gtet all the names from the resources
	//loop through the names and check that they are displayed
	@Test
	fun test_NamesOfAllah_displays_99_names() {
		composeTestRule.setContent {
			NamesOfAllah(PaddingValues(0.dp))
		}

		val arabicNames = resources.getStringArray(R.array.Arabic)
		val englishNames = resources.getStringArray(R.array.English)
		val translationNames = resources.getStringArray(R.array.translation)

		for (i in 0..98) {
			composeTestRule.onNodeWithTag("NamesOfAllah").performScrollToIndex(i)
			composeTestRule.onNodeWithText(arabicNames[i]).assertExists()
			composeTestRule.onNodeWithText(englishNames[i]).assertExists()
			composeTestRule.onNodeWithText(translationNames[i]).assertExists()
		}
	}
}