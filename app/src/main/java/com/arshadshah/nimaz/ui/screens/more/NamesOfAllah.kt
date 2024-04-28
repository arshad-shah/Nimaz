package com.arshadshah.nimaz.ui.screens.more

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_NAMES_OF_ALLAH
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont

@Composable
fun NamesOfAllah(paddingValues: PaddingValues) {
    //a scrollable list of allah's names
    //the names are in the arrays file in res folder
    //they are in three different arrays
    //array English contains the english names
    //array Arabic contains the arabic names
    //array translation contains the translation of the arabic names
    //the names are in the same order in all three arrays
    //so the first name in English is the same as the first name in Arabic and the first name in translation
    //get the resources
    val resources = LocalContext.current.resources
    //get the arrays
    val englishNames = resources.getStringArray(R.array.English)
    val arabicNames = resources.getStringArray(R.array.Arabic)
    val translationNames = resources.getStringArray(R.array.translation)

    Card(
        modifier = Modifier
            .padding(4.dp)
            .padding(paddingValues)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        //loop through the arrays and display the names
        //the names are in the same order in all three arrays
        //so the first name in English is the same as the first name in Arabic and the first name in translation
        LazyColumn(
            //assign a tag to the column
            //this is used for testing
            //the tag is used to find the column in the hierarchy
            modifier = Modifier
                .fillMaxSize()
                .testTag(TEST_TAG_NAMES_OF_ALLAH),
        ) {
            items(englishNames.size) { index ->
                NamesOfAllahRow(
                    index,
                    englishNames[index],
                    arabicNames[index],
                    translationNames[index]
                )
                if (index < englishNames.size - 1) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.background,
                        thickness = 2.dp
                    )
                }
            }
        }
    }
}

@Composable
fun NamesOfAllahRow(
    index: Int,
    englishName: String,
    arabicName: String,
    translationName: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
    ) {
        Text(
            modifier = Modifier
                .padding(start = 4.dp)
                .fillMaxWidth()
                .weight(0.15f),
            text = "${index + 1}.",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Column(
            modifier = Modifier
                .padding(horizontal = 0.dp, vertical = 8.dp)
                .fillMaxWidth()
                .weight(0.85f),
        ) {
            Text(
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxWidth(),
                text = translationName,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth(),
                    text = arabicName,
                    textAlign = TextAlign.Center,
                    fontFamily = utmaniQuranFont,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxWidth(),
                text = englishName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NamesOfAllahRowPreview() {
    NamesOfAllahRow(1, "Al 'Aleem", "العليم", "The All Knowing")
}