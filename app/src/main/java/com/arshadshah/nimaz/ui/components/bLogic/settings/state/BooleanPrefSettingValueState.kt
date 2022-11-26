package com.arshadshah.nimaz.ui.components.bLogic.settings.state

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.arshadshah.nimaz.ui.components.bLogic.settings.SettingValueState
import com.arshadshah.nimaz.utils.PrivateSharedPreferences

@Composable
fun rememberPreferenceBooleanSettingState(
	key : String ,
	defaultValue : Boolean ,
	preferences : PrivateSharedPreferences = PrivateSharedPreferences(LocalContext.current) ,
										 ) : BooleanPreferenceSettingValueState
{
	return remember {
		BooleanPreferenceSettingValueState(
				preferences = preferences ,
				key = key ,
				defaultValue = defaultValue
										  )
	}
}

class BooleanPreferenceSettingValueState(
	private val preferences : PrivateSharedPreferences ,
	val key : String ,
	val defaultValue : Boolean = false ,
										) : SettingValueState<Boolean>
{

	private var _value by mutableStateOf(preferences.getDataBoolean(key , defaultValue))

	override var value : Boolean
		set(value)
		{
			_value = value
			preferences.saveDataBoolean(key , value)
		}
		get() = _value

	override fun reset()
	{
		value = defaultValue
	}
}