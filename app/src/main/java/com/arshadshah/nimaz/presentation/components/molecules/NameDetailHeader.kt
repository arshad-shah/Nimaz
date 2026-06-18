package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.theme.NimazSpacing

/**
 * Calligraphic on-surface header for the names detail screens. Large Amiri Arabic
 * leads, with an accent-ringed number medallion (optional) and an accent divider.
 */
@Composable
fun NameDetailHeader(
    arabicName: String,
    accent: NamesAccent,
    modifier: Modifier = Modifier,
    number: Int? = null,
    primaryLabel: String? = null,
    secondaryLabel: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = NimazSpacing.Large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NimazSpacing.Small)
    ) {
        if (number != null) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .border(BorderStroke(2.dp, accent.rail), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$number",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent.contentTint
                )
            }
        }

        ArabicText(
            text = arabicName,
            size = ArabicTextSize.LARGE,
            color = accent.contentTint,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        if (primaryLabel != null) {
            Text(
                text = primaryLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }

        if (secondaryLabel != null) {
            Text(
                text = secondaryLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Box(
            modifier = Modifier
                .padding(top = NimazSpacing.Small)
                .width(60.dp)
                .height(3.dp)
                .background(accent.rail, RoundedCornerShape(3.dp))
        )
    }
}
