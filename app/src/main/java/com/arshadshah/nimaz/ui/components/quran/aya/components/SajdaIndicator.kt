package com.arshadshah.nimaz.ui.components.quran.aya.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.arshadshah.nimaz.R

@Composable
fun SajdaIndicator(
    sajdaType: String,
    loading: Boolean
) {
    var showSajdaInfo by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { showSajdaInfo = true },
            enabled = !loading
        ) {
            Icon(
                painter = painterResource(id = R.drawable.sajad_icon),
                contentDescription = "Sajda indicator",
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(24.dp)
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
                    tonalElevation = 2.dp
                ) {
                    Text(
                        text = "$sajdaType sujood",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}
