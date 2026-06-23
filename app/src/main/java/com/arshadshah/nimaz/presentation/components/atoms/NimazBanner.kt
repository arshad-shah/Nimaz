package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme

enum class NimazBannerVariant {
    INFO,
    WARNING,
    UPDATE,
    ERROR
}

@Composable
fun NimazBanner(
    message: String,
    variant: NimazBannerVariant,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    title: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    isLoading: Boolean = false,
    showBorder: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    when (variant) {
        NimazBannerVariant.INFO -> InfoVariant(
            message = message,
            modifier = modifier,
            icon = icon,
            showBorder = showBorder
        )

        NimazBannerVariant.WARNING -> WarningVariant(
            message = message,
            modifier = modifier,
            icon = icon,
            title = title,
            actionLabel = actionLabel,
            onAction = onAction
        )

        NimazBannerVariant.UPDATE -> UpdateVariant(
            message = message,
            modifier = modifier,
            actionLabel = actionLabel,
            onAction = onAction,
            isLoading = isLoading
        )

        NimazBannerVariant.ERROR -> ErrorVariant(
            message = message,
            modifier = modifier,
            icon = icon,
            title = title,
            actionLabel = actionLabel,
            onAction = onAction,
            onClick = onClick
        )
    }
}

@Composable
private fun InfoVariant(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    showBorder: Boolean = false
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    BannerSurface(
        modifier = modifier,
        shape = RoundedCornerShape(if (showBorder) 14.dp else 12.dp),
        color = primaryColor.copy(alpha = 0.1f),
        border = if (showBorder) {
            BorderStroke(1.dp, primaryColor.copy(alpha = 0.3f))
        } else {
            null
        }
    ) {
        InfoContent(message = message, icon = icon)
    }
}

@Composable
private fun InfoContent(
    message: String,
    icon: ImageVector? = null
) {
    Row(
        modifier = Modifier.padding(15.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (icon != null) {
            NimazIcon(
                imageVector = icon,
                contentDescription = null,
                variant = NimazIconVariant.PRIMARY,
                size = NimazIconSize.MEDIUM
            )
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.2
        )
    }
}

@Composable
private fun WarningVariant(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    title: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val warningColor = Color(0xFFF59E0B)

    BannerSurface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = warningColor.copy(alpha = 0.1f)
    ) {
        BannerContentRow(
            modifier = Modifier.padding(16.dp),
            icon = icon,
            iconTint = warningColor,
            iconSpacing = 14.dp,
            title = title,
            titleStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            titleColor = MaterialTheme.colorScheme.onSurface,
            titleBottomSpacing = 4.dp,
            message = message,
            actionVariant = NimazButtonVariant.TONAL,
            actionLabel = actionLabel,
            onAction = onAction
        )
    }
}

@Composable
private fun UpdateVariant(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    isLoading: Boolean = false
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.width(12.dp))
                Card(
                    onClick = onAction,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = actionLabel,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    strokeWidth = 3.dp
                )
            }
        }
    }
}

@Composable
private fun ErrorVariant(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    title: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val errorColor = MaterialTheme.colorScheme.error

    BannerSurface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
        onClick = onClick
    ) {
        BannerContentRow(
            modifier = Modifier.padding(12.dp),
            icon = icon,
            iconTint = errorColor,
            iconSpacing = 8.dp,
            title = title,
            titleStyle = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            titleColor = errorColor,
            titleBottomSpacing = 2.dp,
            message = message,
            actionVariant = NimazButtonVariant.DESTRUCTIVE,
            actionLabel = actionLabel,
            onAction = onAction
        )
    }
}

/**
 * The rounded container shell shared by every variant: a full-width [Surface] with
 * the given [shape], [color] and optional [border]. Passing [onClick] makes the
 * whole banner tappable — this is the one place the clickable-vs-static branch lives,
 * so individual variants never repeat it.
 */
@Composable
private fun BannerSurface(
    modifier: Modifier,
    shape: Shape,
    color: Color,
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = color,
            border = border,
            content = content
        )
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = color,
            border = border,
            content = content
        )
    }
}

/**
 * Shared icon · title/message · action layout behind the [NimazBannerVariant.WARNING]
 * and [NimazBannerVariant.ERROR] variants. The action button sits to the right of the
 * weighted text column (not below it), matching both variants' original layout.
 *
 * Container styling (background, shape, border, click) is supplied by [BannerSurface];
 * this composable only lays out the content, taking the inner-padding [modifier] plus
 * the per-variant accent, icon spacing and title style.
 */
@Composable
private fun BannerContentRow(
    modifier: Modifier,
    icon: ImageVector?,
    iconTint: Color,
    iconSpacing: Dp,
    title: String?,
    titleStyle: TextStyle,
    titleColor: Color,
    titleBottomSpacing: Dp,
    message: String,
    actionVariant: NimazButtonVariant,
    actionLabel: String?,
    onAction: (() -> Unit)?
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            NimazIcon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                size = NimazIconSize.LARGE
            )
            Spacer(modifier = Modifier.width(iconSpacing))
        }

        Column(modifier = Modifier.weight(1f)) {
            if (title != null) {
                Text(
                    text = title,
                    style = titleStyle,
                    color = titleColor
                )
                Spacer(modifier = Modifier.height(titleBottomSpacing))
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.width(10.dp))
            NimazButton(
                text = actionLabel,
                onClick = onAction,
                variant = actionVariant,
                size = NimazButtonSize.SMALL
            )
        }
    }
}

// Previews

@Preview(showBackground = true, widthDp = 400, name = "Info Banner")
@Composable
private fun InfoBannerPreview() {
    NimazTheme {
        NimazBanner(
            message = "Your location may be at a high latitude. Prayer times may vary significantly during summer months.",
            variant = NimazBannerVariant.INFO,
            icon = Icons.Default.Info,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Info Banner with Border")
@Composable
private fun InfoBannerWithBorderPreview() {
    NimazTheme {
        NimazBanner(
            message = "Makeup fasts should ideally be completed before the next Ramadan. Fasting on Mondays and Thursdays is recommended.",
            variant = NimazBannerVariant.INFO,
            icon = Icons.Default.Info,
            showBorder = true,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Warning Banner")
@Composable
private fun WarningBannerPreview() {
    NimazTheme {
        NimazBanner(
            message = "Prayer notifications need permission to alert you at prayer times.",
            variant = NimazBannerVariant.WARNING,
            icon = Icons.Default.Notifications,
            title = "Notifications Disabled",
            actionLabel = "Enable",
            onAction = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Warning Banner - Battery")
@Composable
private fun WarningBannerBatteryPreview() {
    NimazTheme {
        NimazBanner(
            message = "Battery optimization may prevent timely prayer notifications.",
            variant = NimazBannerVariant.WARNING,
            icon = Icons.Default.BatteryAlert,
            title = "Battery Optimization Active",
            actionLabel = "Fix",
            onAction = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Update Available Banner")
@Composable
private fun UpdateBannerPreview() {
    NimazTheme {
        NimazBanner(
            message = "A new version of Nimaz is available",
            variant = NimazBannerVariant.UPDATE,
            actionLabel = "Update",
            onAction = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Update Downloading Banner")
@Composable
private fun UpdateBannerDownloadingPreview() {
    NimazTheme {
        NimazBanner(
            message = "Downloading update...",
            variant = NimazBannerVariant.UPDATE,
            isLoading = true,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Update Ready Banner")
@Composable
private fun UpdateBannerReadyPreview() {
    NimazTheme {
        NimazBanner(
            message = "Update ready to install",
            variant = NimazBannerVariant.UPDATE,
            actionLabel = "Restart",
            onAction = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Error Banner")
@Composable
private fun ErrorBannerPreview() {
    NimazTheme {
        NimazBanner(
            message = "Tap here for calibration instructions",
            variant = NimazBannerVariant.ERROR,
            icon = Icons.Default.Warning,
            title = "Calibration Needed",
            onClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Error Banner with Action")
@Composable
private fun ErrorBannerWithActionPreview() {
    NimazTheme {
        NimazBanner(
            message = "Move your phone in a figure-8 pattern to calibrate the compass",
            variant = NimazBannerVariant.ERROR,
            icon = Icons.Default.Warning,
            title = "Calibration Needed",
            actionLabel = "Calibrate",
            onAction = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
