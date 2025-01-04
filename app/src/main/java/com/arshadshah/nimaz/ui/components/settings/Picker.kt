package com.arshadshah.nimaz.ui.components.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationResult
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private fun <T> getItemIndexForOffset(
    range: List<T>,
    value: T,
    offset: Float,
    halfNumbersColumnHeightPx: Float,
): Int {
    val indexOf = range.indexOf(value) - (offset / halfNumbersColumnHeightPx).toInt()
    return maxOf(0, minOf(indexOf, range.count() - 1))
}

@Composable
fun <T> Picker(
    modifier: Modifier = Modifier,
    label: (T) -> String = { it.toString() },
    value: T,
    onValueChange: (T) -> Unit,
    list: List<T>,
    textStyle: TextStyle = LocalTextStyle.current,
) {
    val minimumAlpha = 0.3f
    val verticalMargin = 12.dp
    val numbersColumnHeight = 100.dp
    val halfNumbersColumnHeight = numbersColumnHeight / 2
    val halfNumbersColumnHeightPx = with(LocalDensity.current) { halfNumbersColumnHeight.toPx() }

    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current

    val animatedOffset = remember { Animatable(0f) }
        .apply {
            val index = list.indexOf(value)
            val offsetRange = remember(value, list) {
                -((list.count() - 1) - index) * halfNumbersColumnHeightPx to
                        index * halfNumbersColumnHeightPx
            }
            updateBounds(offsetRange.first, offsetRange.second)
        }

    val coercedAnimatedOffset = animatedOffset.value % halfNumbersColumnHeightPx
    val indexOfElement =
        getItemIndexForOffset(list, value, animatedOffset.value, halfNumbersColumnHeightPx)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        // Background gradients
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(numbersColumnHeight / 3)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(numbersColumnHeight / 3)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        )

        // Selection card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(44.dp)
                .align(Alignment.Center),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                focusedElevation = 0.dp
            )
        ) { }

        Box(
            modifier = Modifier
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { deltaY ->
                        coroutineScope.launch {
                            animatedOffset.snapTo(animatedOffset.value + deltaY)
                        }
                    },
                    onDragStopped = { velocity ->
                        coroutineScope.launch {
                            val endValue = animatedOffset.fling(
                                initialVelocity = velocity,
                                animationSpec = exponentialDecay(frictionMultiplier = 20f),
                                adjustTarget = { target ->
                                    val coercedTarget = target % halfNumbersColumnHeightPx
                                    val coercedAnchors = listOf(
                                        -halfNumbersColumnHeightPx,
                                        0f,
                                        halfNumbersColumnHeightPx
                                    )
                                    val coercedPoint =
                                        coercedAnchors.minByOrNull { abs(it - coercedTarget) }!!
                                    val base =
                                        halfNumbersColumnHeightPx * (target / halfNumbersColumnHeightPx).toInt()
                                    coercedPoint + base
                                }
                            ).endState.value

                            val result = list.elementAt(
                                getItemIndexForOffset(
                                    list,
                                    value,
                                    endValue,
                                    halfNumbersColumnHeightPx
                                )
                            )
                            onValueChange(result)
                            animatedOffset.snapTo(0f)
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }
                )
                .padding(vertical = numbersColumnHeight / 3)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(vertical = verticalMargin)
                    .offset { IntOffset(x = 0, y = coercedAnimatedOffset.roundToInt()) }
            ) {
                val enhancedTextStyle = textStyle.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )

                ProvideTextStyle(enhancedTextStyle) {
                    // Previous number
                    if (indexOfElement > 0) {
                        Label(
                            text = label(list.elementAt(indexOfElement - 1)),
                            modifier = Modifier
                                .offset(y = -halfNumbersColumnHeight)
                                .alpha(
                                    maxOf(
                                        minimumAlpha,
                                        coercedAnimatedOffset / halfNumbersColumnHeightPx
                                    )
                                )
                                .scale(
                                    animateFloatAsState(
                                        targetValue = 0.85f,
                                        animationSpec = spring(
                                            stiffness = Spring.StiffnessVeryLow / 2,
                                            dampingRatio = Spring.DampingRatioMediumBouncy
                                        ),
                                        label = "previousScale"
                                    ).value
                                )
                        )
                    }

                    // Current number
                    Label(
                        text = label(list.elementAt(indexOfElement)),
                        modifier = Modifier
                            .alpha(
                                maxOf(
                                    minimumAlpha,
                                    1 - abs(coercedAnimatedOffset) / halfNumbersColumnHeightPx
                                )
                            )
                            .scale(
                                animateFloatAsState(
                                    targetValue = 1.2f,
                                    animationSpec = spring(
                                        stiffness = Spring.StiffnessLow,
                                        dampingRatio = Spring.DampingRatioMediumBouncy
                                    ),
                                    label = "currentScale"
                                ).value
                            )
                    )

                    // Next number
                    if (indexOfElement < list.count() - 1) {
                        Label(
                            text = label(list.elementAt(indexOfElement + 1)),
                            modifier = Modifier
                                .offset(y = halfNumbersColumnHeight)
                                .alpha(
                                    maxOf(
                                        minimumAlpha,
                                        -coercedAnimatedOffset / halfNumbersColumnHeightPx
                                    )
                                )
                                .scale(
                                    animateFloatAsState(
                                        targetValue = 0.85f,
                                        animationSpec = spring(
                                            stiffness = Spring.StiffnessVeryLow / 2,
                                            dampingRatio = Spring.DampingRatioMediumBouncy
                                        ),
                                        label = "nextScale"
                                    ).value
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Label(text: String, modifier: Modifier) {
    Text(
        modifier = modifier.padding(16.dp),
        text = text,
        textAlign = TextAlign.Center,
    )
}

private suspend fun Animatable<Float, AnimationVector1D>.fling(
    initialVelocity: Float,
    animationSpec: DecayAnimationSpec<Float>,
    adjustTarget: ((Float) -> Float)?,
    block: (Animatable<Float, AnimationVector1D>.() -> Unit)? = null,
): AnimationResult<Float, AnimationVector1D> {
    val targetValue = animationSpec.calculateTargetValue(value, initialVelocity)
    val adjustedTarget = adjustTarget?.invoke(targetValue)

    return if (adjustedTarget != null) {
        animateTo(
            targetValue = adjustedTarget,
            initialVelocity = initialVelocity,
            block = block
        )
    } else {
        animateDecay(
            initialVelocity = initialVelocity,
            animationSpec = animationSpec,
            block = block,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PickerPreview() {
    val list = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    val selectedValue = remember { mutableIntStateOf(5) }
    MaterialTheme {
        Box(
            modifier = Modifier
                .padding(32.dp)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Picker(
                list = list,
                value = selectedValue.intValue,
                onValueChange = { selectedValue.intValue = it }
            )
        }
    }
}