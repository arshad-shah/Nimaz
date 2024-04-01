package com.arshadshah.nimaz.ui.components.tasbih

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.data.local.models.LocalDua
import com.arshadshah.nimaz.ui.components.common.MarkdownText
import com.arshadshah.nimaz.ui.components.common.placeholder.material.PlaceholderHighlight
import com.arshadshah.nimaz.ui.components.common.placeholder.material.placeholder
import com.arshadshah.nimaz.ui.components.common.placeholder.material.shimmer
import com.arshadshah.nimaz.ui.theme.englishQuranTranslation
import com.arshadshah.nimaz.ui.theme.urduFont
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont

@Composable
fun DuaListItem(dua: LocalDua, loading: Boolean) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = cleanString(dua.arabic_dua),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = utmaniQuranFont,
                   modifier = Modifier
                       .fillMaxWidth()
                       .padding(horizontal = 4.dp)
                       .placeholder(
                           visible = loading,
                           color = MaterialTheme.colorScheme.outline,
                           shape = RoundedCornerShape(4.dp),
                           highlight = PlaceholderHighlight.shimmer(
                               highlightColor = Color.White,
                           )
                       )
                )
            }
            Text(
                text = cleanString(dua.english_translation),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(4.dp)
            )
            Row {
                Text(
                    text = "Reference: ${dua.english_reference}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = englishQuranTranslation,
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth()
                        .placeholder(
                            visible = loading,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(4.dp),
                            highlight = PlaceholderHighlight.shimmer(
                                highlightColor = Color.White,
                            )
                        ),
                )
            }
        }
}

//function to clean \n and \t and \r from the string if it exists
fun cleanString(string: String): String {
    return string.replace("\n", "").replace("\t", "").replace("\r", "")
}