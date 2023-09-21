package com.arshadshah.nimaz.ui.components.settings.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.arshadshah.nimaz.ui.components.settings.SettingValueState
import com.arshadshah.nimaz.utils.PrivateSharedPreferences

@Composable
fun rememberPreferenceStringSettingState(
	key : String ,
	defaultValue : String = "" ,
	preferences : PrivateSharedPreferences = PrivateSharedPreferences(LocalContext.current) ,
										) : StringPreferenceSettingValueState
{
	return remember {
		StringPreferenceSettingValueState(
				 key = key ,
				 preferences = preferences ,
				 defaultValue = defaultValue
										 )
	}
}

class StringPreferenceSettingValueState(
	private val preferences : PrivateSharedPreferences ,
	val key : String ,
	val defaultValue : String = "" ,
									   ) : SettingValueState<String>
{

	private var _value by mutableStateOf(preferences.getData(key , defaultValue))

	override var value : String
		set(value)
		{
			_value = value
			preferences.saveData(key , value)
		}
		get() = _value

	override fun reset()
	{
		value = defaultValue
	}
}