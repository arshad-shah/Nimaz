package com.arshadshah.nimaz.presentation.components.atoms.qibla

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * A compact stat card: a muted uppercase label above a bold tabular-figures value.
 * Used in pairs in [QiblaFactsRow].
 */
@Composable
fun QiblaFactCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    NimazCard(modifier = modifier, style = NimazCardStyle.OUTLINED) {
        Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp)) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 0.07.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum"
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 180, name = "Fact Card")
@Composable
private fun QiblaFactCardPreview() {
    NimazTheme {
        QiblaFactCard(label = "Qibla bearing", value = "118° SE")
    }
}
