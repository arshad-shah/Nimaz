package com.arshadshah.nimaz.ui.components.quran

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
import com.arshadshah.nimaz.ui.components.common.SliderWithIcons
import com.arshadshah.nimaz.ui.components.settings.state.FloatPreferenceSettingValueState
import com.arshadshah.nimaz.ui.components.settings.state.StringPreferenceSettingValueState
import com.arshadshah.nimaz.viewModel.QuranViewModel
import kotlin.math.roundToInt


@Composable
fun FontSizeDialog(
    showDialog3: (Boolean) -> Unit,
    arabicFontSizeState: FloatPreferenceSettingValueState,
    translationFontSizeState: FloatPreferenceSettingValueState,
    fontStyleState: StringPreferenceSettingValueState,
    items3: List<String>,
    handleQuranEvents: (QuranViewModel.QuranMenuEvents) -> Unit,
) {

    AlertDialogNimaz(
        topDivider = false,
        bottomDivider = false,
        contentHeight = 250.dp,
        dismissButtonText = "Close",
        contentDescription = "Font Settings",
        title = "Font Settings",
        contentToShow = {
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
                    .wrapContentHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Arabic Size", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    //round this value to make it clean and easy to read
                    Text(text = arabicFontSizeState.value.roundToInt().toString())
                }
                SliderWithIcons(
                    value = arabicFontSizeState.value,
                    onValueChange = {
                        //check if the value is in the range
                        //if not then set it to the min or max value
                        //this is to prevent the slider from going out of range
                        if (fontStyleState.value == "IndoPak") {
                            if (it < 32f) arabicFontSizeState.value = 32f
                            if (it > 60f) arabicFontSizeState.value = 60f
                            if (it in 32f..60f) arabicFontSizeState.value = it
                        } else {
                            if (it < 24f) arabicFontSizeState.value = 24f
                            if (it > 46f) arabicFontSizeState.value = 46f
                            if (it in 24f..46f) arabicFontSizeState.value = it
                        }
                        handleQuranEvents(
                            QuranViewModel.QuranMenuEvents.Change_Arabic_Font_Size(
                                arabicFontSizeState.value
                            )
                        )
                    },
                    valueRange = if (fontStyleState.value == "IndoPak") 32f..60f else 24f..46f,
                    leadingIcon = painterResource(id = R.drawable.arabic_font_size_icon),
                    leadingIconSize = 24.dp,
                    trailaingIcon = painterResource(id = R.drawable.arabic_font_size_icon),
                    trailingIconSize = 32.dp,
                    contentDescription1 = "Decrease Arabic Font Size",
                    contentDescription2 = "Increase Arabic Font Size"
                )


                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Translation Size",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    //a text to show the font value of the translation
                    //round this value to make it clean and easy to read
                    Text(text = translationFontSizeState.value.roundToInt().toString())
                }

                SliderWithIcons(
                    value = translationFontSizeState.value,
                    onValueChange = {
                        if (it < 16f) translationFontSizeState.value = 16f
                        if (it > 40f) translationFontSizeState.value = 40f
                        if (it in 16f..40f) translationFontSizeState.value = it
                        handleQuranEvents(
                            QuranViewModel.QuranMenuEvents.Change_Translation_Font_Size(
                                translationFontSizeState.value
                            )
                        )
                    },
                    valueRange = 16f..40f,
                    leadingIcon = painterResource(id = R.drawable.english_font_size_icon),
                    leadingIconSize = 16.dp,
                    trailaingIcon = painterResource(id = R.drawable.english_font_size_icon),
                    trailingIconSize = 24.dp,
                    contentDescription1 = "Decrease Translation Font Size",
                    contentDescription2 = "Increase Translation Font Size"
                )

                LabelWithDropdownMenu(
                    label = "Arabic Style",
                    items = items3,
                    selectedItem = fontStyleState.value,
                    onItemSelected = {
                        fontStyleState.value = it
                        setFontBasedOnFontStyle(
                            it,
                            arabicFontSizeState,
                            translationFontSizeState
                        )
                        handleQuranEvents(
                            QuranViewModel.QuranMenuEvents.Change_Arabic_Font(
                                it
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                )
            }
        },
        onDismissRequest = {
            showDialog3(false)
        },
        onConfirm = {
            showDialog3(false)
        },
        onDismiss = {
            showDialog3(false)
        })
}

//label with a dropdown menu at the end
@Composable
fun LabelWithDropdownMenu(
    label: String,
    items: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val expanded = remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()
            .wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        ElevatedCard(
            modifier = Modifier
                .width(120.dp)
        ) {
            //an elevation card that shows the text and icon
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        expanded.value = !expanded.value
                    },
                content = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        //find the font style from the list of font styles
                        //and then show it in the text
                        Text(
                            text = items[items.indexOf(selectedItem)],
                            modifier = Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Crossfade(
                            targetState = expanded.value,
                            animationSpec = tween(durationMillis = 300)
                        ) { expanded ->
                            if (expanded) {
                                Icon(
                                    painter = painterResource(id = R.drawable.arrow_up_icon),
                                    contentDescription = "dropdown icon",
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp)
                                        .size(18.dp)
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.arrow_down_icon),
                                    contentDescription = "dropdown icon",
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp)
                                        .size(18.dp)
                                )
                            }
                        }
                    }
                    DropdownMenu(
                        offset = DpOffset(5.dp, 0.dp),
                        modifier = Modifier
                            .wrapContentWidth()
                            .wrapContentHeight(),
                        expanded = expanded.value,
                        onDismissRequest = {
                            expanded.value = false
                        },
                        content = {
                            items.forEach { item ->
                                DropdownMenuItem(
                                    onClick = {
                                        onItemSelected(item)
                                        expanded.value = false
                                    },
                                    text = {
                                        Text(
                                            text = item,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                )
                            }
                        }
                    )
                }
            )
        }
    }
}

fun setFontBasedOnFontStyle(
    fontStyle: String,
    arabicFontSizeState: FloatPreferenceSettingValueState,
    translationFontSizeState: FloatPreferenceSettingValueState,
) {
    when (fontStyle) {
        "Default" -> {
            arabicFontSizeState.value = 26f
            translationFontSizeState.value = 16f
        }

        "Quranme" -> {
            arabicFontSizeState.value = 24f
            translationFontSizeState.value = 16f
        }

        "Hidayat" -> {
            arabicFontSizeState.value = 24f
            translationFontSizeState.value = 16f
        }

        "Amiri" -> {
            arabicFontSizeState.value = 24f
            translationFontSizeState.value = 16f
        }

        "IndoPak" -> {
            arabicFontSizeState.value = 32f
            translationFontSizeState.value = 16f
        }
    }
}
