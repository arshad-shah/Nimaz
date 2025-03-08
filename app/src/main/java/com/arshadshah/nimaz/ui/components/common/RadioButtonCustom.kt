package com.arshadshah.nimaz.ui.components.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun RadioButtonCustom(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: RadioButtonColors = RadioButtonDefaults.colors()
) {
    val transition = updateTransition(selected, label = "selection")
    val scale by transition.animateFloat(
        targetValueByState = { if (it) 1.1f else 1f },
    )

    Box(
        modifier = modifier
            .size(40.dp)
            .scale(scale)
            .clip(CircleShape)
            .clickable(
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = if (selected) colors.selectedColor else colors.unselectedColor,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = selected,
                enter = scaleIn(animationSpec = spring(stiffness = Spring.StiffnessHigh)) +
                        fadeIn(animationSpec = tween(100)),
                exit = scaleOut() + fadeOut()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(1.dp),
                    tint = colors.checkmarkColor
                )
            }
        }
    }
}

object RadioButtonDefaults {
    @Composable
    fun colors(
        selectedColor: Color = MaterialTheme.colorScheme.primary,
        unselectedColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
        checkmarkColor: Color = MaterialTheme.colorScheme.onPrimary
    ) = RadioButtonColors(selectedColor, unselectedColor, checkmarkColor)
}

data class RadioButtonColors(
    val selectedColor: Color,
    val unselectedColor: Color,
    val checkmarkColor: Color
)

//a preview
@Preview(showBackground = true)
@Composable
fun RadioButtonCustomPreview() {
    val selected = remember {
        mutableStateOf(true)
    }
    RadioButtonCustom(selected = selected.value, onClick = {
        selected.value = !selected.value
    })
}

@Preview
@Composable
fun RadioButtonCustomPreviewUnselected() {
    val selected = remember {
        mutableStateOf(false)
    }
    RadioButtonCustom(selected = selected.value, onClick = {
        selected.value = !selected.value
    })
}