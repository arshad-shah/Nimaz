package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.presentation.theme.NimazTheme

@Composable
fun NimazSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    uppercase: Boolean = true,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.padding(start = 5.dp, top = 24.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (uppercase) text.uppercase() else text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp,
            // A heading, so TalkBack's heading navigation can jump between sections instead of
            // making the user swipe through every element in order. Declared here rather than at
            // each of the call sites, which is the point of the section title being a component.
            modifier = (if (trailingContent != null) Modifier.weight(1f) else Modifier)
                .semantics { heading() }
        )
        trailingContent?.invoke()
    }
}

@Preview(showBackground = true, widthDp = 400, name = "NimazSectionTitle - Default")
@Composable
private fun NimazSectionTitlePreview() {
    NimazTheme {
        NimazSectionTitle(text = "Display Options")
    }
}

@Preview(showBackground = true, widthDp = 400, name = "NimazSectionTitle - Lowercase")
@Composable
private fun NimazSectionTitleLowercasePreview() {
    NimazTheme {
        NimazSectionTitle(text = "Links", uppercase = false)
    }
}
