package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Switch size presets
 */
enum class NimazSwitchSize(
    val trackWidth: Dp,
    val trackHeight: Dp,
    val thumbSize: Dp,
    val thumbPadding: Dp
) {
    SMALL(44.dp, 24.dp, 18.dp, 3.dp),
    MEDIUM(52.dp, 28.dp, 22.dp, 3.dp),
    LARGE(60.dp, 32.dp, 26.dp, 3.dp)
}

/**
 * Primary switch component for Nimaz app.
 */
@Composable
fun NimazSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors()
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = colors
    )
}

/**
 * Custom animated switch with more control over appearance.
 */
@Composable
fun AnimatedSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    size: NimazSwitchSize = NimazSwitchSize.MEDIUM,
    enabled: Boolean = true,
    checkedTrackColor: Color = MaterialTheme.colorScheme.primary,
    uncheckedTrackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    checkedThumbColor: Color = MaterialTheme.colorScheme.onPrimary,
    uncheckedThumbColor: Color = MaterialTheme.colorScheme.outline,
    checkedBorderColor: Color = Color.Transparent,
    uncheckedBorderColor: Color = MaterialTheme.colorScheme.outline
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) checkedTrackColor else uncheckedTrackColor,
        animationSpec = tween(durationMillis = 200),
        label = "trackColor"
    )

    val thumbColor by animateColorAsState(
        targetValue = if (checked) checkedThumbColor else uncheckedThumbColor,
        animationSpec = tween(durationMillis = 200),
        label = "thumbColor"
    )

    val borderColor by animateColorAsState(
        targetValue = if (checked) checkedBorderColor else uncheckedBorderColor,
        animationSpec = tween(durationMillis = 200),
        label = "borderColor"
    )

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) {
            size.trackWidth - size.thumbSize - size.thumbPadding * 2
        } else {
            0.dp
        },
        animationSpec = tween(durationMillis = 200),
        label = "thumbOffset"
    )

    Box(
        modifier = modifier
            .width(size.trackWidth)
            .height(size.trackHeight)
            .clip(RoundedCornerShape(size.trackHeight / 2))
            .background(
                color = if (enabled) trackColor else trackColor.copy(alpha = 0.5f)
            )
            .border(
                width = 2.dp,
                color = if (enabled) borderColor else borderColor.copy(alpha = 0.5f),
                shape = RoundedCornerShape(size.trackHeight / 2)
            )
            .clickable(
                enabled = enabled,
                onClick = { onCheckedChange(!checked) },
                role = Role.Switch,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
            .padding(size.thumbPadding),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(size.thumbSize)
                .clip(CircleShape)
                .background(
                    color = if (enabled) thumbColor else thumbColor.copy(alpha = 0.5f)
                )
        )
    }
}

/**
 * Prayer notification switch with visual feedback.
 */
@Composable
fun PrayerNotificationSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    prayerColor: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true
) {
    AnimatedSwitch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        checkedTrackColor = prayerColor,
        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
        checkedThumbColor = Color.White,
        uncheckedThumbColor = MaterialTheme.colorScheme.outline
    )
}

/**
 * Toggle with icon indicator.
 */
@Composable
fun IconToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checkedColor: Color = MaterialTheme.colorScheme.primary,
    uncheckedColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    NimazSwitch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedTrackColor = checkedColor,
            uncheckedTrackColor = uncheckedColor
        )
    )
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true, name = "NimazSwitch")
@Composable
private fun NimazSwitchPreview() {
    NimazTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            var checked1 by remember { mutableStateOf(false) }
            var checked2 by remember { mutableStateOf(true) }

            NimazSwitch(checked = checked1, onCheckedChange = { checked1 = it })
            NimazSwitch(checked = checked2, onCheckedChange = { checked2 = it })
            NimazSwitch(checked = false, onCheckedChange = null, enabled = false)
        }
    }
}

@Preview(showBackground = true, name = "Animated Switch Sizes")
@Composable
private fun AnimatedSwitchSizesPreview() {
    NimazTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            var small by remember { mutableStateOf(true) }
            var medium by remember { mutableStateOf(true) }
            var large by remember { mutableStateOf(true) }

            AnimatedSwitch(
                checked = small,
                onCheckedChange = { small = it },
                size = NimazSwitchSize.SMALL
            )
            AnimatedSwitch(
                checked = medium,
                onCheckedChange = { medium = it },
                size = NimazSwitchSize.MEDIUM
            )
            AnimatedSwitch(
                checked = large,
                onCheckedChange = { large = it },
                size = NimazSwitchSize.LARGE
            )
        }
    }
}

@Preview(showBackground = true, name = "Animated Switch States")
@Composable
private fun AnimatedSwitchStatesPreview() {
    NimazTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnimatedSwitch(checked = false, onCheckedChange = {})
            AnimatedSwitch(checked = true, onCheckedChange = {})
            AnimatedSwitch(checked = false, onCheckedChange = {}, enabled = false)
            AnimatedSwitch(checked = true, onCheckedChange = {}, enabled = false)
        }
    }
}

@Preview(showBackground = true, name = "Prayer Notification Switch")
@Composable
private fun PrayerNotificationSwitchPreview() {
    NimazTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            var fajr by remember { mutableStateOf(true) }
            var dhuhr by remember { mutableStateOf(false) }

            PrayerNotificationSwitch(
                checked = fajr,
                onCheckedChange = { fajr = it },
                prayerColor = Color(0xFF5C6BC0)
            )
            PrayerNotificationSwitch(
                checked = dhuhr,
                onCheckedChange = { dhuhr = it },
                prayerColor = Color(0xFFFFB74D)
            )
        }
    }
}

@Preview(showBackground = true, name = "Icon Toggle Switch")
@Composable
private fun IconToggleSwitchPreview() {
    NimazTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            var checked by remember { mutableStateOf(true) }
            IconToggleSwitch(checked = checked, onCheckedChange = { checked = it })
            IconToggleSwitch(checked = false, onCheckedChange = {})
        }
    }
}
