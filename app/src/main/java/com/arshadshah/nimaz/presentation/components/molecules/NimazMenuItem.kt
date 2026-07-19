package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardTone
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.theme.NimazTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NimazMenuItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    trailingIcon: ImageVector? = Icons.AutoMirrored.Filled.ArrowForward,
    enabled: Boolean = true
) {
    NimazCard(
        style = NimazCardStyle.FILLED,
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        tone = NimazCardTone.TRANSPARENT,
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                NimazIcon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    size = NimazIconSize.LARGE
                )
                Spacer(modifier = Modifier.width(16.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (trailingIcon != null) {
                NimazIcon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    variant = NimazIconVariant.MUTED,
                    iconSize = 18.dp
                )
            }
        }
    }
}

@Composable
fun NimazMenuGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    NimazCard(
        style = NimazCardStyle.FILLED,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = 1.dp
    ) {
        Column(content = content)
    }
}

@Preview(showBackground = true, widthDp = 400, name = "NimazMenuItem")
@Composable
private fun NimazMenuItemPreview() {
    NimazTheme {
        NimazMenuItem(
            title = "Prayer Tracker",
            subtitle = "Track your daily prayers & qada",
            icon = Icons.Default.Schedule,
            onClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "NimazMenuGroup")
@Composable
private fun NimazMenuGroupPreview() {
    NimazTheme {
        NimazMenuGroup(modifier = Modifier.padding(16.dp)) {
            NimazMenuItem(
                title = "Prayer Tracker",
                subtitle = "Track your daily prayers",
                icon = Icons.Default.Schedule,
                onClick = {}
            )
        }
    }
}
