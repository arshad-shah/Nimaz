package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Bottom sheet drag handle indicator.
 */
@Composable
fun BottomSheetHandle(
    modifier: Modifier = Modifier,
    width: Dp = 32.dp,
    height: Dp = 4.dp,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(color)
    )
}

@Preview(showBackground = true, name = "Bottom Sheet Handle")
@Composable
private fun BottomSheetHandlePreview() {
    NimazTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BottomSheetHandle()
        }
    }
}
