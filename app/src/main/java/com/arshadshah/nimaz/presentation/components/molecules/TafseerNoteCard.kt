package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.TafseerNote
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TafseerNoteCard(
    note: TafseerNote,
    onEdit: (TafseerNote) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    NimazCard(
        style = NimazCardStyle.OUTLINED,
        elevation = 0.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTimestamp(note.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = { onEdit(note) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        NimazIcon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.cd_edit),
                            variant = NimazIconVariant.MUTED,
                            iconSize = 18.dp
                        )
                    }
                    IconButton(
                        onClick = { onDelete(note.id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        NimazIcon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.cd_delete),
                            variant = NimazIconVariant.ERROR,
                            iconSize = 18.dp
                        )
                    }
                }
            }

            Text(
                text = note.text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}


// ==================== PREVIEWS ====================

private val sampleTafseerNote = TafseerNote(
    id = 1L,
    ayahId = 255,
    tafseerId = "ibn_kathir_en",
    text = "This passage emphasises tawakkul — placing complete trust in Allah while " +
            "still taking the means. A point worth revisiting during difficulty.",
    createdAt = 1_700_000_000_000L,
    updatedAt = 1_700_000_000_000L
)

@Composable
private fun TafseerNoteCardShowcase() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        TafseerNoteCard(
            note = sampleTafseerNote,
            onEdit = {},
            onDelete = {}
        )
    }
}

@Preview(showBackground = true, name = "TafseerNoteCard — Light")
@Composable
private fun TafseerNoteCardLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        TafseerNoteCardShowcase()
    }
}

@Preview(showBackground = true, name = "TafseerNoteCard — Dark")
@Composable
private fun TafseerNoteCardDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        TafseerNoteCardShowcase()
    }
}
