package com.arshadshah.nimaz.ui.components.tasbih

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DismissValue
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.data.local.models.LocalTasbih

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
            Card(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                        32.dp
                    ),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.38f
                    ),
                    disabledContainerColor = MaterialTheme.colorScheme.surface.copy(
                        alpha = 0.38f
                    ),
                ),
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier
                    .padding(
                        bottom = 4.dp,
                        start = 8.dp,
                        end = 8.dp,
                        top = 4.dp
                    )
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.extraLarge
                    )
                    .clickable { onClick(currentItem.value) }
            ) {

                //a row to contain the tasbih name, goal and count and the delete button
                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentItem.value.count == currentItem.value.goal) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Completed",
                            modifier = Modifier
                                .size(24.dp)
                        )
                    }
                    //name
                    Text(
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp),
                        text = currentItem.value.englishName,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    //divider
                    Divider(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.08f
                        ),
                        thickness = 1.dp,
                    )
                    //goal
                    Text(
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp),
                        text = currentItem.value.goal.toString(),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall
                    )
                    //divider
                    Divider(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.08f
                        ),
                        thickness = 1.dp,
                    )
                    //count
                    Text(
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp),
                        text = currentItem.value.count.toString(),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        })
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
        if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) 0.75f else 1f, label = ""
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