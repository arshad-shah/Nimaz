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
fun rememberPreferenceFloatSettingState(
    key: String,
    defaultValue: Float = 0f,
    preferences: PrivateSharedPreferences = PrivateSharedPreferences(LocalContext.current),
): FloatPreferenceSettingValueState {
    return remember {
        FloatPreferenceSettingValueState(
            key = key,
            preferences = preferences,
            defaultValue = defaultValue
        )
    }
}

class FloatPreferenceSettingValueState(
    private val preferences: PrivateSharedPreferences,
    val key: String,
    val defaultValue: Float = 0f,
) : SettingValueState<Float> {

    private var _value by mutableStateOf(preferences.getDataFloat(key))

    override var value: Float
        set(value) {
            _value = value
            preferences.saveDataFloat(key, value)
        }
        get() = _value

    override fun reset() {
        value = defaultValue
    }
}