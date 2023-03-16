package com.arshadshah.nimaz.ui.screens.tracker

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.data.remote.viewModel.TrackerViewModel
import com.arshadshah.nimaz.ui.components.ui.trackers.rememberChartStyle
import com.arshadshah.nimaz.ui.components.ui.trackers.rememberMarker
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.utils.LocalDataStore
import com.patrykandpatrick.vico.compose.axis.horizontal.bottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.endAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.compose.style.currentChartStyle
import com.patrykandpatrick.vico.core.DefaultDimens
import com.patrykandpatrick.vico.core.chart.column.ColumnChart
import com.patrykandpatrick.vico.core.component.shape.LineComponent
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer

@Composable
fun History() {


	val viewModel = viewModel(
			key = "TrackerViewModel" ,
			initializer = { TrackerViewModel() } ,
			viewModelStoreOwner = LocalContext.current as ComponentActivity
							 )

	LaunchedEffect(key1 = "getTrackerForDate") {
		viewModel.onEvent(TrackerViewModel.TrackerEvent.GET_ALL_TRACKERS)
	}

	val allTrackerData = remember {
		viewModel.multiDataSetChartEntryModelProducer
	}

	Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(start = 6.dp , end = 6.dp , top = 4.dp , bottom = 4.dp) ,
			horizontalAlignment = Alignment.CenterHorizontally
		  ) {
		Spacer(modifier = Modifier.height(16.dp))
		ChartUI(
				chartModelProducer = allTrackerData ,
			 )
	}
}

//charts composable
@Composable
fun ChartUI(
	chartModelProducer : ChartEntryModelProducer ,
		 )
{
	ProvideChartStyle(rememberChartStyle(chartColors)) {
		val defaultColumns = currentChartStyle.columnChart.columns
		Chart(
				chart = columnChart(
						columns = remember(defaultColumns) {
							defaultColumns.mapIndexed { index, defaultColumn ->
								val topCornerRadiusPercent =
									if (index == defaultColumns.lastIndex) DefaultDimens.COLUMN_ROUNDNESS_PERCENT else 0
								val bottomCornerRadiusPercent = if (index == 0) DefaultDimens.COLUMN_ROUNDNESS_PERCENT else 0
								LineComponent(
										defaultColumn.color,
										defaultColumn.thicknessDp,
										Shapes.roundedCornerShape(
												topCornerRadiusPercent,
												topCornerRadiusPercent,
												bottomCornerRadiusPercent,
												bottomCornerRadiusPercent,
																 ),
											 )
							}
						} ,
						mergeMode = ColumnChart.MergeMode.Stack ,
								   ) ,
				chartModelProducer = chartModelProducer ,
				endAxis = endAxis(
						maxLabelCount = START_AXIS_LABEL_COUNT,
									 ) ,
				bottomAxis = bottomAxis() ,
				marker = rememberMarker() ,
			 )
	}
}
private const val START_AXIS_LABEL_COUNT = 5

private val color1 = Color(0xFFC62828)
private val color2 = Color(0xFFF9A825)
private val color3 = Color(0xFF9E9D24)
private val color4 = Color(0xFF558B2F)
private val color5 = Color(0xFF2E7D32)
private val chartColors = listOf(color1, color2, color3, color4, color5)

//preview
@Preview
@Composable
fun HistoryPreview()
{
	LocalDataStore.init(LocalContext.current)
	NimazTheme {
		History()
	}
}