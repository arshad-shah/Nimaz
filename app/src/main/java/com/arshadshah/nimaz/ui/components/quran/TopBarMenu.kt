package com.arshadshah.nimaz.ui.components.quran

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Badge
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.PopupProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarMenu(
    number: Int,
    isSurah: Boolean,
    getAllAyats: (Int, String) -> Unit
) {
    val translationType = PrivateSharedPreferences(LocalContext.current)
        .getData(key = AppConstants.TRANSLATION_LANGUAGE, s = "English")
    val translation = when (translationType) {
        "English" -> "english"
        "Urdu" -> "urdu"
        else -> "english"
    }

    val list = remember {
        if (isSurah) (1..114).toList() else (1..30).toList()
    }

    var selectedNumber by remember { mutableIntStateOf(number) }
    var expanded by remember { mutableStateOf(false) }
    val label = if (isSurah) "Surah" else "Juz"

    Card(
        modifier = Modifier
            .width(160.dp)
            .height(52.dp)
            .padding(start = 8.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp,
            focusedElevation = 6.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Badge(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Text(
                    text = selectedNumber.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            AnimatedContent(
                targetState = expanded,
                transitionSpec = {
                    rotateAnimation().using(SizeTransform(clip = false))
                },
                label = "Dropdown Arrow"
            ) { isExpanded ->
                Icon(
                    painter = painterResource(
                        id = if (isExpanded) R.drawable.arrow_up_icon
                        else R.drawable.arrow_down_icon
                    ),
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (expanded) {
        Popup(
            alignment = Alignment.TopStart,
            onDismissRequest = { expanded = false },
            properties = PopupProperties(focusable = true)
        ) {
            QuranSectionDropdown(
                list = list,
                label = label,
                selectedNumber = selectedNumber,
                onNumberSelected = { number ->
                    selectedNumber = number
                    getAllAyats(number, translation)
                    expanded = false
                }
            )
        }
    }
}

@Composable
private fun QuranSectionDropdown(
    list: List<Int>,
    label: String,
    selectedNumber: Int,
    onNumberSelected: (Int) -> Unit
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (selectedNumber - 1).coerceAtLeast(0)
    )

    Card(
        modifier = Modifier
            .width(160.dp)
            .height(320.dp)
            .shadow(elevation = 8.dp, shape = MaterialTheme.shapes.medium)
            .clip(MaterialTheme.shapes.medium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(list.size) { index ->
                QuranSectionItem(
                    number = list[index],
                    label = label,
                    isSelected = list[index] == selectedNumber,
                    onClick = { onNumberSelected(list[index]) }
                )
            }
        }
    }
}

@Composable
private fun QuranSectionItem(
    number: Int,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(MaterialTheme.shapes.small)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Text(
            text = "$label $number",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
private fun rotateAnimation(
    duration: Int = 300
): ContentTransform =
    (slideInVertically { height -> height } + fadeIn()).togetherWith(slideOutVertically { height -> -height } + fadeOut())