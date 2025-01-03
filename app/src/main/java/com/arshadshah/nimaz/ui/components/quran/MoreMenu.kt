package com.arshadshah.nimaz.ui.components.quran

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.components.settings.SettingValueState
import com.arshadshah.nimaz.ui.components.settings.rememberIntSettingState
import com.arshadshah.nimaz.ui.components.settings.state.rememberPreferenceFloatSettingState
import com.arshadshah.nimaz.ui.components.settings.state.rememberPreferenceStringSettingState
import com.arshadshah.nimaz.viewModel.QuranViewModel
import es.dmoral.toasty.Toasty
import kotlin.reflect.KFunction1

@Composable
fun MoreMenu(
    menuOpen: Boolean = false,
    setMenuOpen: (Boolean) -> Unit,
    state: SettingValueState<Int> = rememberIntSettingState(),
    handleEvents: KFunction1<QuranViewModel.QuranMenuEvents, Unit>,
) {

    val context = LocalContext.current
    val items2: List<String> = listOf("English", "Urdu")
    val items3: List<String> = listOf("Default", "Quranme", "Hidayat", "Amiri", "IndoPak")
    val (showDialog2, setShowDialog2) = remember { mutableStateOf(false) }

    //a dialog with two sliders to control the font size of quran
    val (showDialog3, setShowDialog3) = remember { mutableStateOf(false) }
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

    DropdownMenu(
        expanded = menuOpen,
        onDismissRequest = { setMenuOpen(false) },
        content = {
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
            HorizontalDivider()
            DropdownMenuItem(onClick = {
                setShowDialog3(true)
                setMenuOpen(false)
            }, text = { Text(text = "Font") })
        }
    )


    if (showDialog2) {
        CustomDialog(
            title = "Translation",
            items = items2,
            setShowDialog = setShowDialog2,
            state = state,
            valueState = translationState
        ) {
            handleEvents(
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
            handleEvents
        )
    } else {
        return
    }
}