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
fun rememberPreferenceIntSetSettingState(
	key : String ,
	defaultValue : Set<Int> = emptySet() ,
	delimiter : String = "|" ,
	preferences : PrivateSharedPreferences = PrivateSharedPreferences(LocalContext.current) ,
										) : IntSetPreferenceSettingValueState
{
	return remember {
		IntSetPreferenceSettingValueState(
				 key = key ,
				 preferences = preferences ,
				 defaultValue = defaultValue ,
				 delimiter = delimiter ,
										 )
	}
}

class IntSetPreferenceSettingValueState(
	private val preferences : PrivateSharedPreferences ,
	val key : String ,
	val defaultValue : Set<Int> = emptySet() ,
	val delimiter : String = "" ,
									   ) : SettingValueState<Set<Int>>
{

	private var _value by mutableStateOf(
			 preferences.getIntSet(key , defaultValue.toPrefString(delimiter))
				 .orEmpty()
				 .split(delimiter)
				 .filter { it.isNotEmpty() }
				 .map { it.toInt() }
				 .toMutableSet()
										)

	override var value : Set<Int>
		set(value)
		{
			_value = value.toSortedSet()
			preferences.saveIntSet(key , value)
		}
		get() = _value

	private fun Set<Int>.toPrefString(delimiter : String) =
		joinToString(separator = delimiter) { it.toString() }

	override fun reset()
	{
		value = defaultValue
	}
}