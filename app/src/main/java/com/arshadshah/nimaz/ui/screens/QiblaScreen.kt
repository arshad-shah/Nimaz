package com.arshadshah.nimaz.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.arshadshah.nimaz.data.remote.viewModel.QiblaViewModel
import com.arshadshah.nimaz.ui.components.bLogic.compass.Dial

@Composable
fun QiblaScreen(paddingValues : PaddingValues)
{
	val context = LocalContext.current
	val viewModel = QiblaViewModel(context)

	val state = remember { viewModel.qiblaState }.collectAsState()


	Column {
		Dial(state = state)
	}
}