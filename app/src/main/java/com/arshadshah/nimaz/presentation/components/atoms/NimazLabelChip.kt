package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.tooling.preview.Preview
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * A small rounded, decorative label pill used across readers for occasion,
 * source/reference, repeat-count and narrator labels. When [highlighted] it
 * uses the primary tint (e.g. occasion / narrator); otherwise a neutral
 * surface tint (e.g. source / reference). Optionally shows a leading [icon].
 */
@Composable
fun NimazLabelChip(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    highlighted: Boolean = false
) {
    val background = if (highlighted) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val foreground = if (highlighted) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(100),
        color = background
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = foreground
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = foreground
            )
        }
    }
}


// ==================== PREVIEWS ====================

/**
 * Shows the neutral vs. highlighted chip, with and without a leading icon.
 */
@Composable
private fun NimazLabelChipShowcase() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NimazLabelChip(text = "Sahih al-Bukhari")
        NimazLabelChip(text = "Eid al-Fitr", highlighted = true)
        NimazLabelChip(text = "Repeat 3×", icon = Icons.Default.Repeat)
        NimazLabelChip(text = "Narrator: Abu Hurairah", icon = Icons.Default.Star, highlighted = true)
    }
}

@Preview(showBackground = true, name = "Label Chip — Light")
@Composable
private fun NimazLabelChipLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        NimazLabelChipShowcase()
    }
}

@Preview(showBackground = true, name = "Label Chip — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun NimazLabelChipDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        NimazLabelChipShowcase()
    }
}
