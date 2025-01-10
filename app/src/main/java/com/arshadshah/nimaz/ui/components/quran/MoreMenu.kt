package com.arshadshah.nimaz.ui.components.quran

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
    val (showDialog3, setShowDialog3) = remember { mutableStateOf(false) }

    val pageTypeState = rememberPreferenceStringSettingState(AppConstants.PAGE_TYPE, "List")
    val translationState = rememberPreferenceStringSettingState(
        AppConstants.TRANSLATION_LANGUAGE,
        "English"
    )
    val arabicFontSizeState = rememberPreferenceFloatSettingState(
        key = AppConstants.ARABIC_FONT_SIZE,
        defaultValue = 26f
    )
    val translationFontSizeState = rememberPreferenceFloatSettingState(
        key = AppConstants.TRANSLATION_FONT_SIZE,
        defaultValue = 16f
    )
    val fontStyleState = rememberPreferenceStringSettingState(
        key = AppConstants.FONT_STYLE,
        defaultValue = "Default"
    )

    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        )
    ) {
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { setMenuOpen(false) }
        ) {
            // Translation Option
            Surface(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                DropdownMenuItem(
                    onClick = {
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
                    },
                    text = {
                        Text(
                            text = "Translation",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (pageTypeState.value == "Page (Experimental)")
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    },
                    colors = MenuDefaults.itemColors(
                        textColor = MaterialTheme.colorScheme.onSurface,
                        leadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        trailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                )
            }

            // Font Option
            Surface(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                DropdownMenuItem(
                    onClick = {
                        setShowDialog3(true)
                        setMenuOpen(false)
                    },
                    text = {
                        Text(
                            text = "Font",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    colors = MenuDefaults.itemColors(
                        textColor = MaterialTheme.colorScheme.onSurface,
                        leadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        trailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }

    if (showDialog2) {
            CustomDialog(
                title = "Translation",
                items = items2,
                setShowDialog = setShowDialog2,
                state = state,
                valueState = translationState
            ) {
                handleEvents(QuranViewModel.QuranMenuEvents.Change_Translation(it))
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
    }
}