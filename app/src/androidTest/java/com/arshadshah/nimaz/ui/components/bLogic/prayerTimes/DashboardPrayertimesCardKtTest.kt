package com.arshadshah.nimaz.ui.components.bLogic.prayerTimes

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_NEXT_PRAYER_ICON_DASHBOARD
import com.arshadshah.nimaz.ui.components.dashboard.DashboardPrayertimesCard
import com.arshadshah.nimaz.viewModel.PrayerTimesViewModel
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.util.*

class DashboardPrayertimesCardKtTest
{

	@get:Rule
	val composeTestRule = createAndroidComposeRule<ComponentActivity>()

	@Test
	fun dashboardPrayerTimesCard_rendersWithoutCrashing() {
		composeTestRule.setContent {
			DashboardPrayertimesCard()
		}
	}
	@Test
	fun dashboardPrayerTimesCard_displaysNextPrayerInfo() {
		val context = InstrumentationRegistry.getInstrumentation().targetContext
		val viewModel = PrayerTimesViewModel(context)
		viewModel.handleEvent(context, PrayerTimesViewModel.PrayerTimesEvent.RELOAD)
		composeTestRule.setContent {
			DashboardPrayertimesCard()
		}
		val nextPrayerName = viewModel.nextPrayerName.value
		val nextPrayerTime = viewModel.nextPrayerTime.value
		val timer = viewModel.timer.value
		composeTestRule.onNodeWithText(nextPrayerName!!.replaceFirstChar {
			if (it.isLowerCase()) it.titlecase(
					Locale.getDefault()
											  ) else it.toString()
		}).assertIsDisplayed()
		composeTestRule.onNodeWithText(nextPrayerTime!!.format(DateTimeFormatter.ofPattern("hh:mm a"))).assertIsDisplayed()
	}

	@Test
	fun dashboardPrayerTimesCard_displaysDates() {
		composeTestRule.setContent {
			DashboardPrayertimesCard()
		}
		composeTestRule.onNodeWithText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))).assertIsDisplayed()
		composeTestRule.onNodeWithText(HijrahDate.from(LocalDate.now()).format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))).assertIsDisplayed()
	}


	@Test
	fun dashboardPrayerTimesCard_displaysNextPrayerIcon() {
		composeTestRule.setContent {
			DashboardPrayertimesCard()
		}

		composeTestRule.onNodeWithTag(TEST_TAG_NEXT_PRAYER_ICON_DASHBOARD, useUnmergedTree = true).assertIsDisplayed()
	}

	//correct phase of moon icon is shown
	@Test
	fun dashboardPrayerTimesCard_displaysMoonIcon() {
		composeTestRule.setContent {
			DashboardPrayertimesCard()
		}

		composeTestRule.onNodeWithTag("moon_phase", useUnmergedTree = true).assertIsDisplayed()
	}
}