package com.arshadshah.nimaz.ui.components.quran

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Badge
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.utils.PrivateSharedPreferences

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

    val scale by animateFloatAsState(
        targetValue = if (expanded) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Box {
        ElevatedCard(
            modifier = Modifier
                .width(160.dp)
                .height(52.dp)
                .padding(start = 8.dp)
                .scale(scale)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { expanded = !expanded },
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 2.dp,
                pressedElevation = 4.dp
            ),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
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
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = selectedNumber.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

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
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            modifier = Modifier
                                .padding(4.dp)
                                .size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (expanded) {
            Popup(
                alignment = Alignment.TopStart,
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true)
            ) {
                ElevatedCard(
                    modifier = Modifier
                        .width(160.dp)
                        .height(320.dp)
                        .padding(top = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.elevatedCardElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp
                    ),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    val listState = rememberLazyListState(
                        initialFirstVisibleItemIndex = (selectedNumber - 1).coerceAtLeast(0)
                    )

                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(list.size) { index ->
                            QuranSectionItem(
                                number = list[index],
                                label = label,
                                isSelected = list[index] == selectedNumber,
                                onClick = {
                                    selectedNumber = list[index]
                                    getAllAyats(list[index], translation)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
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
    val transition = updateTransition(isSelected, label = "selection")
    val elevation by transition.animateDp { selected ->
        if (selected) 4.dp else 1.dp
    }
    val scale by transition.animateFloat { selected ->
        if (selected) 1.02f else 1f
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .scale(scale)
            .clickable(onClick = onClick),
        color = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        shadowElevation = elevation,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }

            Badge(
                containerColor = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isSelected)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant

            ) {
                Text(
                    text = number.toString(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
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


@Preview
@Composable
fun TopBarMenuPreview() {
    TopBarMenu(
        number = 1,
        isSurah = true,
        getAllAyats = { _, _ -> }
    )
}