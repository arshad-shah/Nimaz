package com.arshadshah.nimaz.ui.components.tasbih

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.data.local.models.LocalTasbih
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont

// a dropdown item for each tasbih
//to contain annimated visibility delete button and the tasbih name, goal and count
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbihDropdownItem(
    item: LocalTasbih,
    onClick: (LocalTasbih) -> Unit,
    onDelete: (LocalTasbih) -> Unit,
    onEdit: (LocalTasbih) -> Unit,
) {
    val currentItem = rememberUpdatedState(newValue = item)
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.StartToEnd) {
                onEdit(currentItem.value)
            } else if (it == SwipeToDismissBoxValue.EndToStart) {
                onDelete(currentItem.value)
            }
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            SwipeBackground(dismissState = dismissState)
        },
        content = {
            TasbihCard(
                tasbih = item,
                onClick = onClick
            )
        }
    )
}

@Composable
fun TasbihCard(
    tasbih: LocalTasbih,
    onClick: (LocalTasbih) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
        ),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                shape = MaterialTheme.shapes.extraLarge
            )
            .clickable(onClick = { onClick(tasbih) })
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = tasbih.englishName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = tasbih.arabicName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = utmaniQuranFont,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            VerticalDivider(
                modifier = Modifier.height(36.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${tasbih.count}/${tasbih.goal}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (tasbih.count >= tasbih.goal) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { (tasbih.count.toFloat() / tasbih.goal).coerceIn(0f, 1f) },
                    modifier = Modifier.width(80.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SwipeBackground(dismissState: SwipeToDismissBoxState) {
    val direction = dismissState.dismissDirection

    val color by animateColorAsState(
        when (dismissState.targetValue) {
            SwipeToDismissBoxValue.Settled -> MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp)
            SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primary
            SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
        }, label = ""
    )
    val iconTintColor by animateColorAsState(
        when (dismissState.targetValue) {
            SwipeToDismissBoxValue.Settled -> MaterialTheme.colorScheme.surface
            SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.onPrimary
            SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.onErrorContainer
        }, label = ""
    )
    val alignment = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
        SwipeToDismissBoxValue.Settled -> Alignment.Center
    }
    val icon = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> painterResource(id = R.drawable.edit_icon)
        SwipeToDismissBoxValue.EndToStart -> painterResource(id = R.drawable.delete_icon)
        SwipeToDismissBoxValue.Settled -> painterResource(id = R.drawable.edit_icon)
    }
    val scale by animateFloatAsState(
        if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) 0f else 1.2f, label = ""
    )

    val haptic = LocalHapticFeedback.current
    LaunchedEffect(key1 = dismissState.targetValue, block = {
        if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    })

    Box(
        Modifier
            .fillMaxSize()
            .background(color)
            .padding(horizontal = 20.dp),
        contentAlignment = alignment
    ) {
        Icon(
            painter = icon,
            contentDescription = "Localized description",
            modifier = Modifier
                .scale(scale)
                .size(24.dp),
            tint = iconTintColor
        )
    }
}


@Preview(
    showBackground = true
)
@Composable
//MyTasbihDropDownItem
fun TasbihDropDownItemPreview() {
    //tasbih object
    //val id: Int = 0,
    //    val date: String = LocalDate.now().toString(),
    //    val arabicName: String,
    //    val englishName: String,
    //    val translationName: String,
    //    val goal: Int = 0,
    //    val count: Int =
    val tasbih = LocalTasbih(
        arabicName = "الله أكبر",
        englishName = "Allahu Akbar",
        translationName = "God is the greatest",
        goal = 33,
        count = 0
    )
    TasbihDropdownItem(
        item = tasbih,
        onClick = { },
        onDelete = { tasbih ->

        },
    ) { tasbih ->

    }
}