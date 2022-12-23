package com.arshadshah.nimaz.ui.components.bLogic.compass

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.arshadshah.nimaz.data.remote.viewModel.QiblaViewModel
import com.arshadshah.nimaz.ui.components.ui.compass.DialUI
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@Composable
fun Dial(state : State<QiblaViewModel.QiblaState>)
{

	val context = LocalContext.current
	val scope = rememberCoroutineScope()

	var data by remember { mutableStateOf<SensorData?>(null) }

	DisposableEffect(Unit) {
		val dataManager = SensorDataManager(context)
		dataManager.init()

		val job = scope.launch {
			dataManager.data
				.receiveAsFlow()
				.onEach { data = it }
				.collect {
					// do nothing
				}
		}

		onDispose {
			dataManager.cancel()
			job.cancel()
		}
	}
	when (val qiblaState = state.value)
	{
		is QiblaViewModel.QiblaState.Loading ->
		{
			DialUI(0.0 , data)
		}

		is QiblaViewModel.QiblaState.Error ->
		{
			DialUI(0.0 , data)
		}

		is QiblaViewModel.QiblaState.Success ->
		{
			qiblaState.bearing?.let { DialUI(it , data) }
		}
	}
}