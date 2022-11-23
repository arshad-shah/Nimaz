package com.arshadshah.nimaz.ui.components.ui.quran

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.data.remote.models.Juz
import com.arshadshah.nimaz.ui.theme.quranFont
import com.arshadshah.nimaz.utils.PrivateSharedPreferences

@Composable
fun JuzListUI(
    juz: ArrayList<Juz>,
    onNavigateToAyatScreen: (String, Boolean, Boolean) -> Unit
) {
    LazyColumn(userScrollEnabled = true) {
        items(juz.size) { index ->
            JuzListItemUI(
                juzNumber = juz[index].number.toString(),
                name = juz[index].name,
                tname = juz[index].tname,
                onNavigateToAyatScreen = onNavigateToAyatScreen
            )
        }
    }
}

@Composable
fun JuzListItemUI(
    juzNumber: String,
    name: String,
    tname: String,
    onNavigateToAyatScreen: (String, Boolean, Boolean) -> Unit,
    context: Context = LocalContext.current
) {
    ElevatedCard(
        modifier = Modifier
            .padding(4.dp)
            .shadow(8.dp, clip = true, shape = RoundedCornerShape(8.dp))
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(8.dp)
    ) {
        //get the translation type from shared preferences
        val translationType = PrivateSharedPreferences(context).getData(key ="Translation", s = "English")
        var isEnglishType = true
        if(translationType != "English"){
            isEnglishType = false
        }
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .clickable(
                    enabled = true
                ) {
                    onNavigateToAyatScreen(juzNumber, false, isEnglishType)
                }
        ) {

            Text(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .weight(0.10f),
                text = "$juzNumber.",
                style = MaterialTheme.typography.bodyLarge
            )

            Column(
                modifier = Modifier
                    .padding(16.dp, 0.dp)
                    .align(Alignment.CenterVertically)
                    .weight(0.80f)
            ) {
                //apply quran font
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = quranFont
                )
                Text(text = tname, style = MaterialTheme.typography.titleSmall)
            }
            //an arrow right icon
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowRight,
                contentDescription = "Clear",
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .weight(0.10f)
                    .fillMaxWidth()
            )
        }
    }
}