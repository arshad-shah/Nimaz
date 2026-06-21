package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Centered full-size loading spinner.
 *
 * Replaces the `Box(fillMaxSize, contentAlignment = Center) { CircularProgressIndicator() }`
 * block that was copy-pasted across ~30 screens. Pass a [Modifier] with the
 * scaffold padding (e.g. `Modifier.padding(paddingValues)`) when used inside a
 * Scaffold body.
 */
@Composable
fun NimazLoadingState(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = color)
    }
}

@Preview(showBackground = true, widthDp = 300, heightDp = 200, name = "NimazLoadingState")
@Composable
private fun NimazLoadingStatePreview() {
    NimazTheme {
        NimazLoadingState()
    }
}
