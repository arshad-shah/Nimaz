package com.arshadshah.nimaz.ui.components.bLogic.settings.state

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.arshadshah.nimaz.ui.components.bLogic.settings.SettingValueState
import com.arshadshah.nimaz.utils.PrivateSharedPreferences

@Composable
fun rememberPreferenceIntSettingState(
	key : String ,
	defaultValue : Int = - 1 ,
	preferences : PrivateSharedPreferences = PrivateSharedPreferences(LocalContext.current) ,
									 ) : IntPreferenceSettingValueState
{
	return remember {
		IntPreferenceSettingValueState(
				key = key ,
				preferences = preferences ,
				defaultValue = defaultValue
									  )
	}
}

class IntPreferenceSettingValueState(
	private val preferences : PrivateSharedPreferences ,
	val key : String ,
	val defaultValue : Int = 0 ,
									) : SettingValueState<Int>
{

	private var _value by mutableStateOf(preferences.getDataInt(key))

	override var value : Int
		set(value)
		{
			_value = value
			preferences.saveDataInt(key , value)
		}
		get() = _value

	override fun reset()
	{
		value = defaultValue
	}
}