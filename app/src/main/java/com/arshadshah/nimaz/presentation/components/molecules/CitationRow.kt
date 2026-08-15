package com.arshadshah.nimaz.presentation.components.molecules

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * A cited verse: where it is, and enough of it to know whether to go there.
 *
 * The reference alone is a blind tap. A subject like "Allah" cites 153 verses, and a list of
 * 153 bare references under 153 identical "Open in reader" subtitles is a list you can only
 * work through by opening every row. [preview] is the verse's own text, clipped to a couple of
 * lines — the smallest thing that turns scanning into reading.
 *
 * Degrades to the reference alone when there is no [preview]: a reader whose translation has no
 * text for these verses gets a shorter row, not an empty space where a sentence should be.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitationRow(
    reference: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    preview: String? = null,
) {
    NimazCard(
        style = NimazCardStyle.FILLED,
        tone = NimazTone.TRANSPARENT,
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        elevation = 0.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = reference,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .width(ReferenceGutter)
                    .padding(end = 8.dp, top = 1.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                if (preview != null) {
                    Text(
                        text = preview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private val ReferenceGutter = 56.dp

@Preview(showBackground = true, widthDp = 390, name = "CitationRow — Light")
@Composable
private fun CitationRowLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { CitationSample() }
}

@Preview(
    showBackground = true, widthDp = 390, name = "CitationRow — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun CitationRowDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { CitationSample() }
}

@Composable
private fun CitationSample() {
    Column(modifier = Modifier.padding(16.dp)) {
        CitationRow(
            reference = "2:153",
            preview = "You who believe, seek help through patience and prayer. " +
                    "God is with the patient.",
            onClick = {},
        )
        CitationRow(reference = "3:200", onClick = {})
    }
}
