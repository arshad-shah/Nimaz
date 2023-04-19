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