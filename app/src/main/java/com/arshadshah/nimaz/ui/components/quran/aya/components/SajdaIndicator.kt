package com.arshadshah.nimaz.ui.components.quran.aya.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.arshadshah.nimaz.R

/**
 * Sajda indicator with popup info.
 * Uses FilledTonalIconButton with tertiaryContainer.
 */
@Composable
fun SajdaIndicator(
    sajdaType: String,
    loading: Boolean,
    modifier: Modifier = Modifier
) {
    var showSajdaInfo by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        FilledTonalIconButton(
            onClick = { showSajdaInfo = true },
            enabled = !loading,
            modifier = Modifier.size(36.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.sajad_icon),
                contentDescription = "Sajda indicator",
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(18.dp)
            )
        }

        if (showSajdaInfo) {
            Popup(
                onDismissRequest = { showSajdaInfo = false },
                alignment = Alignment.BottomCenter,
                offset = IntOffset(0, -100)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 4.dp
                ) {
                    Text(
                        text = "$sajdaType Sujood",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
