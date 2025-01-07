package com.arshadshah.nimaz.ui.components.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.Badge
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R

@Composable
fun <T> FeaturesDropDown(
    modifier: Modifier = Modifier,
    items: List<T>,
    label: String,
    showBadge: Boolean = true,
    dropDownItem: @Composable (T) -> Unit,
    shape: CornerBasedShape = MaterialTheme.shapes.medium
) {
    val (isExpanded, setExpanded) = remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
        shape = shape,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.clip(shape)) {
            DropdownHeader(
                label = label,
                itemCount = items.size,
                isExpanded = isExpanded,
                showBadge = showBadge,
                onExpandClick = { setExpanded(!isExpanded) }
            )

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                if (items.isEmpty()) {
                    EmptyStateCard(text = "No $label available")
                } else {
                    LazyColumn(
                        modifier = Modifier.height(200.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(items) { item -> dropDownItem(item) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DropdownHeader(
    label: String,
    itemCount: Int,
    isExpanded: Boolean,
    showBadge: Boolean,
    onExpandClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(12.dp),
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onExpandClick)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (showBadge && itemCount > 0) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = itemCount.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            FilledIconButton(
                onClick = onExpandClick,
                shape = MaterialTheme.shapes.small,
            ) {
                Icon(
                    painter = painterResource(
                        if (isExpanded) R.drawable.arrow_up_icon
                        else R.drawable.arrow_down_icon
                    ),
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun <T> DropdownListItem(
    item: T,
    onClick: (T) -> Unit,
    content: @Composable (T) -> Unit,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = {}
) {
    Row(
        modifier = Modifier
            .padding(12.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable {
                onClick(item)
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        leadingIcon?.invoke()
        Box(modifier = Modifier.weight(1f)) { content(item) }
        trailingIcon?.invoke()
    }
}

@Composable
fun EmptyStateCard(text: String) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(24.dp)
        )
    }
}