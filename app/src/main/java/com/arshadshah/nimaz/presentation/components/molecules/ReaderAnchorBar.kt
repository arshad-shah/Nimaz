package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButton
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Where you are in the book, said once.
 *
 * The reader used to print `Juz 15 · Page 293` as a badge on **every ayah**, which is the same
 * sentence repeated six times a screen about a fact that changes roughly once a page. It moves
 * here, under the app bar, where it is true of everything below it — and takes the "Go to…"
 * action with it, so the place you are is also the control for changing it.
 *
 * @param subtitle the coordinate: juz and page.
 * @param onGoTo opens the jump control; null hides the affordance where there is nowhere to go.
 */
@Composable
fun ReaderAnchorBar(
    subtitle: String,
    modifier: Modifier = Modifier,
    onGoTo: (() -> Unit)? = null,
    goToContentDescription: String? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // The coordinate only. The surah's name is already in the app bar directly above,
            // and printing it twice, one line apart, is the repetition this bar exists to end.
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (onGoTo != null) {
                NimazIconButton(
                    icon = Icons.Default.MyLocation,
                    onClick = onGoTo,
                    contentDescription = goToContentDescription,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ReaderAnchorBarPreview() {
    NimazTheme {
        ReaderAnchorBar(
            subtitle = "Juz 15 · Page 293",
            onGoTo = {},
        )
    }
}
