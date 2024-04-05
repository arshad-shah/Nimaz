package com.arshadshah.nimaz.ui.components.common

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonColors
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.data.local.models.LocalAya
import com.arshadshah.nimaz.ui.theme.NimazTheme

@Composable
fun <T> FeaturesDropDown(
    modifier: Modifier = Modifier,
    items: List<T>,
    label: String,
    showBadge: Boolean = true,
    dropDownItem: @Composable (T) -> Unit,
    shape: CornerBasedShape = MaterialTheme.shapes.medium
) {
    val (
        isExpanded,
        setIsExpanded
    ) = remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(elevation = 6.dp),
        modifier = modifier.fillMaxWidth(),
        shape = shape,
    ) {
        Column(
            modifier = Modifier
                .clip(shape),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(elevation = 32.dp),
                modifier = modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
            ) {
                Header(label, items.size, isExpanded, showBadge, setIsExpanded)
            }
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                DropdownContent(items, label, dropDownItem)
            }
        }
    }
}

@Composable
fun Header(
    label: String,
    itemCount: Int,
    isExpanded: Boolean,
    showBadge: Boolean,
    setIsExpanded: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable { setIsExpanded(!isExpanded) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            modifier = Modifier
                .weight(1f)
                .padding(8.dp),
            textAlign = TextAlign.Start,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleLarge
        )
        if (showBadge && itemCount > 0) {
            Badge(
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    modifier = Modifier.padding(2.dp),
                    text = itemCount.toString(),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        FilledIconButton(
            onClick = { setIsExpanded(!isExpanded) },
            shape = MaterialTheme.shapes.small,
            colors = IconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(elevation = 1.dp),
                contentColor = MaterialTheme.colorScheme.onSurface,
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f),
                disabledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp)
                    .copy(alpha = 0.32f)
            )
        ) {
            Icon(
                painter = if (isExpanded) painterResource(id = R.drawable.arrow_up_icon) else painterResource(
                    id = R.drawable.arrow_down_icon
                ),
                contentDescription = "Dropdown",
                modifier = Modifier
                    .padding(8.dp)
                    .size(24.dp)
            )
        }
    }
}

@Composable
fun <T> DropdownContent(
    items: List<T>,
    label: String,
    dropDownItem: @Composable (T) -> Unit
) {
    if (items.isEmpty()) {
        DropdownPlaceholder(text = "No $label available")
    } else {
        LazyColumn(
            modifier = Modifier
                .height(200.dp)
        ) { // Allows for scrolling through many items
            items(items) { item ->
                dropDownItem(item)
            }
        }
    }
}

@Composable
fun DropdownPlaceholder(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceColorAtElevation(32.dp),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    disabledContentColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
    disabledContainerColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
    shape: Shape = MaterialTheme.shapes.medium,
    cardPadding: Dp = 8.dp,
    contentPadding: PaddingValues = PaddingValues(8.dp),
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    textColor: Color = contentColor.copy(alpha = 0.6f),
    textAlign: TextAlign = TextAlign.Center,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    maxLines: Int = 2,
    verticalArrangement: Arrangement.Vertical = Arrangement.Center,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContentColor = disabledContentColor,
            disabledContainerColor = disabledContainerColor,
        ),
        shape = shape,
        modifier = modifier
            .padding(cardPadding)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(contentPadding),
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment
        ) {
            Text(
                text = text,
                style = textStyle,
                color = textColor,
                textAlign = textAlign,
                overflow = overflow,
                maxLines = maxLines,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
        }
    }
}

@Composable
fun <T> FeatureDropdownItem(
    item: T,
    onClick: (T) -> Unit,
    itemContent: @Composable (T) -> Unit,
    iconPainter: Painter = painterResource(id = R.drawable.angle_small_right_icon), // Default icon
    iconDescription: String? = null, // Accessibility description for the icon
    iconSize: Dp = 24.dp, // Icon size, default to 24.dp
    padding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 4.dp), // Card padding
    contentPadding: PaddingValues = PaddingValues(8.dp), // Content padding inside the card
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), // Border color
    borderWidth: Dp = 2.dp, // Border width
    showIcon: Boolean = true, // Control the visibility of the icon
    shape: CornerBasedShape = MaterialTheme.shapes.medium // Card shape
) {
    Card(
        modifier = Modifier
            .padding(padding)
            .fillMaxWidth()
            .border(
                BorderStroke(borderWidth, borderColor),
                shape = shape
            )
            .clip(shape)
            .clickable { onClick(item) },
        shape = shape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(32.dp),
            contentColor = MaterialTheme.colorScheme.onSurface,
        )
    ) {
        Row(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            itemContent(item)
            if (showIcon) {
                Icon(
                    painter = iconPainter,
                    contentDescription = iconDescription,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(iconSize)
                )
            }
        }
    }
}