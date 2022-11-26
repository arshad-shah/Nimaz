package com.arshadshah.nimaz.ui.components.ui.quran

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.arshadshah.nimaz.ui.components.bLogic.settings.SettingValueState
import com.arshadshah.nimaz.ui.components.bLogic.settings.rememberIntSettingState
import com.arshadshah.nimaz.ui.components.bLogic.settings.state.rememberPreferenceStringSettingState
import com.arshadshah.nimaz.utils.PrivateSharedPreferences

@Composable
fun MoreMenu(
    menuOpen: Boolean = false,
    setMenuOpen: (Boolean) -> Unit,
    state: SettingValueState<Int> = rememberIntSettingState(),
) {

    val sharedPreferences = PrivateSharedPreferences(LocalContext.current)

    val pageTypeState =
        rememberPreferenceStringSettingState("PageType", "List", sharedPreferences)
    val translationState =
        rememberPreferenceStringSettingState("Translation", "English", sharedPreferences)

    val items1: List<String> = listOf("List", "Page")
    val items2: List<String> = listOf("English", "Urdu")
    val (showDialog1, setShowDialog1) = remember { mutableStateOf(false) }
    val (showDialog2, setShowDialog2) = remember { mutableStateOf(false) }
    DropdownMenu(
        expanded = menuOpen,
        onDismissRequest = { setMenuOpen(false) },
        content = {
            DropdownMenuItem(onClick = {
                setShowDialog1(true)
                setMenuOpen(false)
            }, text = { Text(text = "Display Type") })
            DropdownMenuItem(onClick = {
                setShowDialog2(true)
                setMenuOpen(false)
            }, text = { Text(text = "Translation") })
        }
    )


    if (showDialog1) {
        CustomDialog(
            title = "Display Type",
            setShowDialog = setShowDialog1,
            state = state,
            valueState = pageTypeState,
            items = items1,
        )
    } else if (showDialog2) {
        CustomDialog(
            title = "Translation",
            setShowDialog = setShowDialog2,
            state = state,
            valueState = translationState,
            items = items2,
        )
    } else {
        return
    }
}