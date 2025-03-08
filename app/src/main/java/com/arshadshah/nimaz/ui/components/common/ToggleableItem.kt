package com.arshadshah.nimaz.ui.components.common

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconToggleButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.components.common.placeholder.material.PlaceholderHighlight
import com.arshadshah.nimaz.ui.components.common.placeholder.material.placeholder
import com.arshadshah.nimaz.ui.components.common.placeholder.material.shimmer

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ToggleableItemRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .animateContentSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedIconToggleButton(
            enabled = enabled,
            colors = IconButtonDefaults.outlinedIconToggleButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                checkedContainerColor = iconTint,
                checkedContentColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.errorContainer,
                disabledContentColor = MaterialTheme.colorScheme.error
            ),
            checked = checked,
            onCheckedChange = { onCheckedChange(it) },
            border = BorderStroke(
                1.dp,
                if (checked) iconTint else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        ) {
            AnimatedContent(
                targetState = checked,
                transitionSpec = { (fadeIn() + scaleIn()).togetherWith(fadeOut() + scaleOut()) },
                label = "Toggle Animation"
            ) { isChecked ->
                Icon(
                    painter = painterResource(
                        id = if (isChecked) R.drawable.check_icon else R.drawable.cross_icon
                    ),
                    contentDescription = if (isChecked) "Checked" else "Unchecked",
                    modifier = Modifier.padding(if (isChecked) 8.dp else 10.dp)
                )
            }
        }

        AnimatedContent(
            targetState = checked,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "Text Animation"
        ) { isChecked ->
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = when {
                    !enabled -> MaterialTheme.colorScheme.error
                    isChecked -> iconTint
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                },
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ToggleableItemColumn(
    text: String,
    selectedText: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .animateContentSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        OutlinedIconToggleButton(
            enabled = enabled,
            colors = IconButtonDefaults.outlinedIconToggleButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                checkedContainerColor = iconTint,
                checkedContentColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.errorContainer,
                disabledContentColor = MaterialTheme.colorScheme.error
            ),
            checked = checked,
            onCheckedChange = { onCheckedChange(it) },
            border = BorderStroke(
                1.dp,
                if (checked) iconTint else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        ) {
            AnimatedContent(
                targetState = checked,
                transitionSpec = { (fadeIn() + scaleIn()).togetherWith(fadeOut() + scaleOut()) },
                label = "Toggle Animation"
            ) { isChecked ->
                Icon(
                    painter = painterResource(
                        id = if (isChecked) R.drawable.check_icon else R.drawable.cross_icon
                    ),
                    contentDescription = if (isChecked) "Checked" else "Unchecked",
                    modifier = Modifier.padding(if (isChecked) 8.dp else 10.dp)
                )
            }
        }

        AnimatedContent(
            targetState = checked,
            transitionSpec = {
                (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
            },
            label = "Text Animation"
        ) { isChecked ->
            Text(
                text = if (isChecked) selectedText ?: text else text,
                style = MaterialTheme.typography.titleLarge,
                color = when {
                    !enabled -> MaterialTheme.colorScheme.error
                    isChecked -> iconTint
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun ToggleableItemRowPreview() {
    val items = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")

    var isChecked by remember { mutableStateOf(false) }
    ElevatedCard(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                ToggleableItemRow(
                    text = item,
                    checked = isChecked,
                    onCheckedChange = {
                        Log.d("ToggleableItemPreview", "onCheckedChange: $it")
                        isChecked = it
                    },
                    modifier = Modifier
                        .placeholder(
                            visible = false,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(4.dp),
                            highlight = PlaceholderHighlight.shimmer(
                                highlightColor = Color.White,
                            )
                        ),
                )
            }
        }
    }
}

@Preview
@Composable
fun ToggleableItemColumnPreview() {
    val items = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")

    var isChecked by remember { mutableStateOf(false) }
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            items.forEachIndexed { index, item ->
                ToggleableItemColumn(
                    text = item,
                    checked = isChecked,
                    onCheckedChange = {
                        Log.d("ToggleableItemPreview", "onCheckedChange: $it")
                        isChecked = it
                    },
                    modifier = Modifier
                        .placeholder(
                            visible = false,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(4.dp),
                            highlight = PlaceholderHighlight.shimmer(
                                highlightColor = Color.White,
                            )
                        ),
                )
            }
        }
    }
}