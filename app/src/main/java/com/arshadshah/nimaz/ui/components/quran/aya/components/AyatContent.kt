package com.arshadshah.nimaz.ui.components.quran.aya.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.data.local.models.LocalAya
import com.arshadshah.nimaz.utils.DisplaySettings

@Composable
fun AyatContent(
    aya: LocalAya,
    displaySettings: DisplaySettings,
    loading: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Arabic Text
            ArabicText(aya = aya, displaySettings = displaySettings, loading = loading)

            // Translation Text
            TranslationText(aya = aya, displaySettings = displaySettings, loading = loading)
        }
    }
}