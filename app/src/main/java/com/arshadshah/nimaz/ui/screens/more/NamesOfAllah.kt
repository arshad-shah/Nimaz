package com.arshadshah.nimaz.ui.screens.more

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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
        contentPadding = paddingValues
    ) {
        items(englishNames.size) { index ->
            NamesOfAllahRow(
                index,
                englishNames[index],
                arabicNames[index],
                translationNames[index]
            )
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

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
        ),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier
            .padding(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
        ) {
            Text(
                modifier = Modifier
                    .padding(start = 8.dp)
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
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth(),
                    text = englishName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = arabicName,
                        style = MaterialTheme.typography.headlineLarge,
                        fontFamily = utmaniQuranFont,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth(),
                    text = translationName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NamesOfAllahRowPreview() {
    NamesOfAllahRow(1, "Al 'Aleem", "العليم", "The All Knowing")
}