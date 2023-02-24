package com.arshadshah.nimaz.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_HOME
import com.arshadshah.nimaz.ui.components.bLogic.prayerTimes.DashboardPrayertimesCard
import com.arshadshah.nimaz.ui.components.bLogic.prayerTimes.RamadanCard
import com.arshadshah.nimaz.ui.components.ui.trackers.DashboardFastTracker
import com.arshadshah.nimaz.ui.components.ui.trackers.DashboardPrayerTracker
import com.arshadshah.nimaz.ui.theme.NimazTheme

@Composable
fun Dashboard() {

	LazyColumn(
			modifier = Modifier.testTag(TEST_TAG_HOME)
			  ) {
		item{
			DashboardPrayertimesCard()
//			//horizontally scrollable
			ElevatedCard(
					modifier = Modifier
						.fillMaxWidth()
						.padding(8.dp) ,
						) {
				Text(
						text = "Trackers" ,
						modifier = Modifier
							.padding(8.dp)
							.fillMaxWidth() ,
						textAlign = TextAlign.Center ,
						style = MaterialTheme.typography.titleMedium
					)
			LazyRow(content = {
				item{
						Row(
								content = {
									DashboardPrayerTracker()
									DashboardFastTracker()
								})
					}
			})
		}
			RamadanCard()
		}
	}

}


@Preview
@Composable
fun DashboardPreview() {
	NimazTheme(
			darkTheme = true
			  ) {
		Dashboard()
	}
}