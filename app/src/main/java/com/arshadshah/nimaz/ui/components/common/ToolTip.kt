package com.arshadshah.nimaz.ui.components.common

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.skydoves.balloon.compose.Balloon
import com.skydoves.balloon.compose.rememberBalloonBuilder

/**
 * An enhanced tooltip that shows a popup with the given text when the user clicks on the icon.
 * Features include:
 * - Customizable animations
 * - Press feedback
 * - Adaptive theming
 * - Accessibility support
 * - Rich content support
 *
 * @param icon The icon to show for the tooltip
 * @param iconTint The tint to apply to the icon
 * @param contentDescription The content description for accessibility
 * @param iconSize The size of the icon in dp
 * @param tipText The text to show in the tooltip
 * @param tipTextStyle The style to apply to the tooltip text
 * @param tooltipBackgroundColor The background color of the tooltip (optional)
 * @param tooltipTextColor The text color of the tooltip (optional)
 */
@Composable
fun ToolTip(
    icon: Painter,
    iconTint: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
    contentDescription: String,
    iconSize: Int = 24,
    tipText: String,
    tipTextStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    tooltipBackgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    tooltipTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    // Interaction source for handling press state
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Animation for press feedback
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    // Enhanced balloon builder with animations and styling
    val builder = rememberBalloonBuilder {
        setArrowSize(16)
        setArrowPosition(0.4f)
        setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
        setWidth(BalloonSizeSpec.WRAP)
        setHeight(BalloonSizeSpec.WRAP)
        setPadding(4)
        setMarginHorizontal(4)
        setCornerRadius(10f)
        setBackgroundColor(tooltipBackgroundColor.hashCode())
        setBalloonAnimation(BalloonAnimation.FADE)
        setDismissWhenTouchOutside(true)
//        setAutoDismissDuration(3000L) // Auto dismiss after 3 seconds
    }

    Balloon(
        builder = builder,
        balloonContent = {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.Transparent,
                modifier = Modifier.padding(4.dp),
            ) {
                Text(
                    text = tipText,
                    style = tipTextStyle,
                    color = tooltipTextColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    ) { balloonWindow ->
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(iconSize.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                    shape = CircleShape
                )
                .clickable {
                    balloonWindow.showAlignBottom()
                }
        ) {
            Icon(
                painter = icon,
                contentDescription = contentDescription,
                tint = iconTint,
                modifier = Modifier.size((iconSize * 0.6).dp)
            )
        }
    }
}

// Preview
@Preview(showBackground = true)
@Composable
fun EnhancedTooltipPreview() {
    MaterialTheme {
        ToolTip(
            icon = painterResource(id = R.drawable.info_icon),
            contentDescription = "Information",
            tipText = "This is a sample tooltip with enhanced styling"
        )
    }
}