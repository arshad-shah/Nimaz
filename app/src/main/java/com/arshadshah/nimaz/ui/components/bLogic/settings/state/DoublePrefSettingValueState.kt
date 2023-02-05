package com.arshadshah.nimaz.ui.components.bLogic.settings.state

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.arshadshah.nimaz.ui.components.bLogic.settings.SettingValueState
import com.arshadshah.nimaz.utils.PrivateSharedPreferences

@Composable
fun rememberPreferenceDoubleSettingState(
	key : String ,
	defaultValue : Double = 0.0,
	preferences : PrivateSharedPreferences = PrivateSharedPreferences(LocalContext.current) ,
										) : DoublePreferenceSettingValueState
{
	return remember {
		DoublePreferenceSettingValueState(
				key = key ,
				preferences = preferences ,
				defaultValue = defaultValue
										 )
	}
}

class DoublePreferenceSettingValueState(
	private val preferences : PrivateSharedPreferences ,
	val key : String ,
	val defaultValue : Double = 0.0,
									   ) : SettingValueState<Double>
{

	private var _value by mutableStateOf(preferences.getDataDouble(key , defaultValue))

	override var value : Double
		set(value)
		{
			_value = value
			preferences.saveDataDouble(key , value)
		}
		get() = _value

	override fun reset()
	{
		value = defaultValue
	}
}