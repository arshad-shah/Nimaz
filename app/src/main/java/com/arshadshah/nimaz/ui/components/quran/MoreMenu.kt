package com.arshadshah.nimaz.ui.components.quran

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.QURAN_VIEWMODEL_KEY
import com.arshadshah.nimaz.ui.components.settings.SettingValueState
import com.arshadshah.nimaz.ui.components.settings.rememberIntSettingState
import com.arshadshah.nimaz.ui.components.settings.state.rememberPreferenceFloatSettingState
import com.arshadshah.nimaz.ui.components.settings.state.rememberPreferenceStringSettingState
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.viewModel.QuranViewModel
import es.dmoral.toasty.Toasty

@Composable
fun MoreMenu(
    menuOpen: Boolean = false,
    setMenuOpen: (Boolean) -> Unit,
    state: SettingValueState<Int> = rememberIntSettingState(),
) {

    val context = LocalContext.current

    val items1: List<String> = listOf("List", "Page (Experimental)")
    val items2: List<String> = listOf("English", "Urdu")
    val items3: List<String> = listOf("Default", "Quranme", "Hidayat", "Amiri", "IndoPak")
    val (showDialog1, setShowDialog1) = remember { mutableStateOf(false) }
    val (showDialog2, setShowDialog2) = remember { mutableStateOf(false) }

    //a dialog with two sliders to control the font size of quran
    val (showDialog3, setShowDialog3) = remember { mutableStateOf(false) }
    val sharedPreferencesRepository = remember { PrivateSharedPreferences(context) }
    val viewModel = viewModel(
        key = QURAN_VIEWMODEL_KEY,
        initializer = { QuranViewModel(sharedPreferencesRepository) },
        viewModelStoreOwner = context as ComponentActivity
    )

    viewModel.handleQuranMenuEvents(QuranViewModel.QuranMenuEvents.Initialize_Quran)

    val pageTypeState =
        rememberPreferenceStringSettingState(AppConstants.PAGE_TYPE, "List")
    val translationState =
        rememberPreferenceStringSettingState(
            AppConstants.TRANSLATION_LANGUAGE,
            "English",
        )

    val arabicFontSizeState = rememberPreferenceFloatSettingState(
        key = AppConstants.ARABIC_FONT_SIZE,
        defaultValue = 26f
    )
    val translationFontSizeState = rememberPreferenceFloatSettingState(
        key = AppConstants.TRANSLATION_FONT_SIZE,
        defaultValue = 16f
    )
    //font style
    val fontStyleState = rememberPreferenceStringSettingState(
        key = AppConstants.FONT_STYLE,
        defaultValue = "Default",
    )

    //log the initial state of the font size
    Log.d("MoreMenu", "arabicFontSizeState.value = ${arabicFontSizeState.value}")
    Log.d("MoreMenu", "translationFontSizeState.value = ${translationFontSizeState.value}")
    Log.d("MoreMenu", "fontStyleState.value = ${fontStyleState.value}")

    DropdownMenu(
        expanded = menuOpen,
        onDismissRequest = { setMenuOpen(false) },
        content = {
            DropdownMenuItem(onClick = {
                setShowDialog1(true)
                setMenuOpen(false)
            }, text = { Text(text = "Display Type") })
            //disable the translation option if the page type is page
            DropdownMenuItem(onClick = {
                //if translation is disabled and the user clicks on the translation option
                //then show a toast message
                if (pageTypeState.value == "Page (Experimental)") {
                    Toasty.info(
                        context,
                        "Translation is disabled for Page View",
                        Toasty.LENGTH_SHORT,
                        true
                    ).show()
                } else {
                    setShowDialog2(true)
                    setMenuOpen(false)
                }
            }, text = {
                Text(
                    text = "Translation"
                    //if page type is page then show text color as grey
                    //else show it as black
                    ,
                    color = if (pageTypeState.value == "Page (Experimental)") Color.Gray else MaterialTheme.colorScheme.onBackground
                )
            })
            DropdownMenuItem(onClick = {
                setShowDialog3(true)
                setMenuOpen(false)
            }, text = { Text(text = "Font") })
        }
    )


    if (showDialog1) {
        CustomDialog(
            title = "Display Type",
            setShowDialog = setShowDialog1,
            state = state,
            valueState = pageTypeState,
            items = items1
        ) {
            viewModel.handleQuranMenuEvents(
                QuranViewModel.QuranMenuEvents.Change_Display_Mode(
                    it
                )
            )
        }
    } else if (showDialog2) {
        CustomDialog(
            title = "Translation",
            items = items2,
            setShowDialog = setShowDialog2,
            state = state,
            valueState = translationState
        ) {
            viewModel.handleQuranMenuEvents(
                QuranViewModel.QuranMenuEvents.Change_Translation(
                    it
                )
            )
        }
    } else if (showDialog3) {
        FontSizeDialog(
            setShowDialog3,
            arabicFontSizeState,
            translationFontSizeState,
            fontStyleState,
            items3,
            viewModel::handleQuranMenuEvents
        )
    } else {
        return
    }
}