package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme

@Composable
fun NimazSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailingText: String? = null,
    showSeeAll: Boolean = false,
    onSeeAllClick: () -> Unit = {},
    trailingContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        when {
            trailingContent != null -> trailingContent()
            showSeeAll -> Text(
                text = "See All",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onSeeAllClick)
            )
            trailingText != null -> Text(
                text = trailingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400, name = "NimazSectionHeader - Default")
@Composable
private fun NimazSectionHeaderPreview() {
    NimazTheme {
        NimazSectionHeader(
            title = "Daily Practice",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "NimazSectionHeader - With See All")
@Composable
private fun NimazSectionHeaderSeeAllPreview() {
    NimazTheme {
        NimazSectionHeader(
            title = "Prayer Times",
            showSeeAll = true,
            onSeeAllClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
