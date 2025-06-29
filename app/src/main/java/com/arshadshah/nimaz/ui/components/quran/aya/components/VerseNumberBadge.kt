package com.arshadshah.nimaz.ui.components.quran.aya.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.components.common.placeholder.material.PlaceholderHighlight
import com.arshadshah.nimaz.ui.components.common.placeholder.material.placeholder
import com.arshadshah.nimaz.ui.components.common.placeholder.material.shimmer

@Composable
fun VerseNumberBadge(number: Int, isLoading: Boolean) {
    Badge(
        modifier = Modifier
            .wrapContentSize(),
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.placeholder(
                visible = isLoading,
                highlight = PlaceholderHighlight.shimmer()
            )
        )
    }
}

@Preview
@Composable
fun PreviewVerseNumberBadge() {
    VerseNumberBadge(
        number = 1111,
        isLoading = false
    )
}
