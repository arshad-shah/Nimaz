package com.arshadshah.nimaz.ui.components.bLogic.settings

import androidx.compose.runtime.*
import kotlin.reflect.KProperty

@Composable
fun rememberBooleanSettingState(defaultValue : Boolean = false) : SettingValueState<Boolean>
{
	return remember { InMemoryBooleanSettingValueState(defaultValue) }
}

@Composable
fun rememberFloatSettingState(defaultValue : Float = 0f) : SettingValueState<Float>
{
	return remember { InMemoryFloatSettingValueState(defaultValue) }
}

@Composable
fun rememberIntSettingState(defaultValue : Int = 0) : SettingValueState<Int>
{
	return remember { InMemoryIntSettingValueState(defaultValue) }
}

@Composable
fun rememberStringSettingState(defaultValue : String = "") : SettingValueState<String>
{
	return remember { InMemoryStringSettingValueState(defaultValue) }
}

@Composable
fun rememberIntSetSettingState(defaultValue : Set<Int> = emptySet()) : SettingValueState<Set<Int>>
{
	return remember { InMemoryIntSetSettingValueState(defaultValue) }
}

@Suppress("NOTHING_TO_INLINE")
inline operator fun <T> SettingValueState<T>.getValue(
	thisObj : Any? ,
	property : KProperty<*> ,
													 ) : T =
	value

@Suppress("NOTHING_TO_INLINE")
inline operator fun <T> SettingValueState<T>.setValue(
	thisObj : Any? ,
	property : KProperty<*> ,
	value : T ,
													 )
{
	this.value = value
}

interface SettingValueState<T>
{

	fun reset()

	var value : T
}

internal class InMemoryBooleanSettingValueState(private val defaultValue : Boolean) :
	SettingValueState<Boolean>
{

	override var value : Boolean by mutableStateOf(defaultValue)
	override fun reset()
	{
		value = defaultValue
	}
}

internal class InMemoryFloatSettingValueState(private val defaultValue : Float) :
	SettingValueState<Float>
{

	override var value : Float by mutableStateOf(defaultValue)
	override fun reset()
	{
		value = defaultValue
	}
}

internal class InMemoryIntSettingValueState(private val defaultValue : Int) :
	SettingValueState<Int>
{

	override var value : Int by mutableStateOf(defaultValue)
	override fun reset()
	{
		value = defaultValue
	}
}

internal class InMemoryStringSettingValueState(private val defaultValue : String) :
	SettingValueState<String>
{

	override var value : String by mutableStateOf(defaultValue)
	override fun reset()
	{
		value = defaultValue
	}
}

internal class InMemoryIntSetSettingValueState(private val defaultValue : Set<Int>) :
	SettingValueState<Set<Int>>
{

	override var value : Set<Int> by mutableStateOf(defaultValue)
	override fun reset()
	{
		value = defaultValue
	}
}