package com.arshadshah.nimaz.ui.components.quran

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
import com.arshadshah.nimaz.ui.components.common.NumberSelector
import com.arshadshah.nimaz.ui.components.settings.state.FloatPreferenceSettingValueState
import com.arshadshah.nimaz.ui.components.settings.state.StringPreferenceSettingValueState
import com.arshadshah.nimaz.viewModel.QuranViewModel


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
        contentHeight = 300.dp,
        dismissButtonText = "Close",
        contentDescription = "Font Settings",
        title = "Font Settings",
        contentToShow = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Arabic Font Size Section
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Arabic Size",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        NumberSelector(
                            value = arabicFontSizeState.value,
                            onValueChange = { newValue ->
                                arabicFontSizeState.value = newValue
                                handleQuranEvents(
                                    QuranViewModel.QuranMenuEvents.Change_Arabic_Font_Size(newValue)
                                )
                            },
                            minValue = if (fontStyleState.value == "IndoPak") 32f else 24f,
                            maxValue = if (fontStyleState.value == "IndoPak") 60f else 46f
                        )
                    }
                }

                // Translation Font Size Section
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Translation Size",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        NumberSelector(
                            value = translationFontSizeState.value,
                            onValueChange = { newValue ->
                                translationFontSizeState.value = newValue
                                handleQuranEvents(
                                    QuranViewModel.QuranMenuEvents.Change_Translation_Font_Size(
                                        newValue
                                    )
                                )
                            },
                            minValue = 16f,
                            maxValue = 40f
                        )
                    }
                }

                // Font Style Dropdown
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    LabelWithDropdownMenu(
                        label = "Arabic Style",
                        items = items3,
                        selectedItem = fontStyleState.value,
                        onItemSelected = { newStyle ->
                            fontStyleState.value = newStyle
                            setFontBasedOnFontStyle(
                                newStyle,
                                arabicFontSizeState,
                                translationFontSizeState
                            )
                            handleQuranEvents(
                                QuranViewModel.QuranMenuEvents.Change_Arabic_Font(
                                    newStyle
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }
        },
        onDismissRequest = { showDialog3(false) },
        onConfirm = { showDialog3(false) },
        onDismiss = { showDialog3(false) }
    )
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

    val scale by animateFloatAsState(
        targetValue = if (expanded) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = if (enabled)
                MaterialTheme.colorScheme.onTertiaryContainer
            else
                MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f)
        )

        Box {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scale),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.elevatedCardElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 4.dp
                ),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (enabled)
                        MaterialTheme.colorScheme.surface
                    else
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { if (enabled) expanded = !expanded },
                    enabled = enabled,
                    interactionSource = interactionSource
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = selectedItem,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (enabled)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
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
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .size(20.dp),
                                    tint = if (enabled)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            if (expanded) {
                Surface(
                    modifier = Modifier
                        .widthIn(max = 200.dp)
                        .heightIn(max = 300.dp),
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 8.dp
                ) {
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        offset = DpOffset(0.dp, 4.dp)
                    ) {
                        items.forEachIndexed { index, item ->
                            Surface(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = if (item == selectedItem)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surface
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = item,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (item == selectedItem)
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            else
                                                MaterialTheme.colorScheme.onSurface
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
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    } else null
                                )
                            }

                            if (index < items.lastIndex) {
                                Surface(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(1.dp)
                                ) {
                                    HorizontalDivider(thickness = 1.dp)
                                }
                            }
                        }
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
    (slideInVertically { height -> height } + fadeIn())
        .togetherWith(slideOutVertically { height -> -height } + fadeOut())

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