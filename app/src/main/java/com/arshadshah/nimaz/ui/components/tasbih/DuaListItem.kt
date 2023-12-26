package com.arshadshah.nimaz.ui.components.tasbih

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.data.remote.models.Dua
import com.arshadshah.nimaz.ui.components.common.MarkdownText
import com.arshadshah.nimaz.ui.components.common.placeholder.material.PlaceholderHighlight
import com.arshadshah.nimaz.ui.components.common.placeholder.material.placeholder
import com.arshadshah.nimaz.ui.components.common.placeholder.material.shimmer

@Composable
fun DuaListItem(dua: Dua, loading: Boolean) {
    ElevatedCard(
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MarkdownText(
                    markdown = dua.arabic_dua,
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth(),
                    fontSize = 28.sp,
                    fontResource = R.font.uthman,
                )
            }

            MarkdownText(
                markdown = dua.english_translation,
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
                fontSize = 18.sp,
                fontResource = R.font.nunito,
            )
            Row {
                Text(
                    text = "Reference: ${dua.english_reference}",
                    style = MaterialTheme.typography.titleSmall,
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
}


@Preview
@Composable
fun DuaListItemPreview() {
    val dua = Dua(
        1,
        1,
        0,
        "اللهم صل على محمد وآل محمد",
        "O Allah, <small>send</small> blessings on Muhammad and the family of Muhammad",
        "O Allah, send blessings on Muhammad and the family of Muhammad",
    )
    DuaListItem(dua, false)
}