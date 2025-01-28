package com.arshadshah.nimaz.ui.components.settings.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arshadshah.nimaz.ui.components.settings.SettingValueState
import com.arshadshah.nimaz.utils.PrivateSharedPreferences

class DoublePreferenceSettingValueState(
    private val preferences: PrivateSharedPreferences,
    val key: String,
    val defaultValue: Double = 0.0,
) : SettingValueState<Double> {

    private var _value by mutableStateOf(preferences.getDataDouble(key, defaultValue))

    override var value: Double
        set(value) {
            _value = value
            preferences.saveDataDouble(key, value)
        }
        get() = _value

    override fun reset() {
        value = defaultValue
    }
}