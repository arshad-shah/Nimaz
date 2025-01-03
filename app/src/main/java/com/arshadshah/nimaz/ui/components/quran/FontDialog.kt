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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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

import androidx.compose.animation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.PopupProperties


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
                    .padding(4.dp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelWithDropdownMenu(
    label: String,
    items: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()
            .wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = if (enabled)
                MaterialTheme.colorScheme.onBackground
            else
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Card(
            modifier = Modifier.width(140.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (enabled)
                    MaterialTheme.colorScheme.surfaceVariant
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp,
                pressedElevation = 4.dp,
                focusedElevation = 4.dp
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                onClick = { if (enabled) expanded = true },
                enabled = enabled,
                interactionSource = interactionSource
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = selectedItem,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (enabled)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    AnimatedContent(
                        targetState = expanded,
                        transitionSpec = {
                            rotateAnimation().using(SizeTransform(clip = false))
                        },
                        label = "Dropdown Arrow"
                    ) { isExpanded ->
                        Icon(
                            painter = painterResource(
                                id = if (isExpanded)
                                    R.drawable.arrow_up_icon
                                else
                                    R.drawable.arrow_down_icon
                            ),
                            contentDescription = if (isExpanded)
                                "Collapse menu"
                            else
                                "Expand menu",
                            modifier = Modifier.size(20.dp),
                            tint = if (enabled)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .widthIn(max = 200.dp)
                    .heightIn(max = 300.dp),
                offset = DpOffset(0.dp, 4.dp),
                properties = PopupProperties(
                    focusable = true,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true
                )
            ) {
                items.forEachIndexed { index, item ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        onClick = {
                            onItemSelected(item)
                            expanded = false
                        },
                        trailingIcon = if (item == selectedItem) {
                            {
                                Icon(
                                    modifier = Modifier.size(16.dp),
                                    painter = painterResource(id = R.drawable.check_icon),
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else null
                    )

                    if (index < items.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
private fun rotateAnimation(
    duration: Int = 300
): ContentTransform =
    (slideInVertically { height -> height } + fadeIn()).togetherWith(slideOutVertically { height -> -height } + fadeOut())
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
