package com.arshadshah.nimaz.ui.components.tasbih

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.data.local.models.LocalDua
import com.arshadshah.nimaz.ui.components.common.placeholder.material.PlaceholderHighlight
import com.arshadshah.nimaz.ui.components.common.placeholder.material.placeholder
import com.arshadshah.nimaz.ui.components.common.placeholder.material.shimmer
import com.arshadshah.nimaz.ui.theme.englishQuranTranslation
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
                text = "Reference: ${cleanString(dua.english_reference)}",
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

    Log.d("cleanString", "cleanString: $string")
    //clean any html tags
    val cleanStringFromHtml = string.replace(Regex("<[^>]*>"), "")
    //regex for \r\n
    val cleanAnyMarkers = cleanStringFromHtml.replace("\r\n(", "(")
    Log.d("cleanString Cleaned", "cleanString: $cleanAnyMarkers")
    //clean any \n, \t, \r
    return cleanAnyMarkers
}