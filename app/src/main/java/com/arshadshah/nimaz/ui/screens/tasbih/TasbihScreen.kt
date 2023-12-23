package com.arshadshah.nimaz.ui.screens.tasbih

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_TASBIH
import com.arshadshah.nimaz.ui.components.tasbih.Counter
import com.arshadshah.nimaz.ui.components.tasbih.CustomCounter
import com.arshadshah.nimaz.ui.components.tasbih.TasbihRow

@Composable
fun TasbihScreen(
    paddingValues: PaddingValues,
    tasbihId: String = "",
    tasbihArabic: String = "",
    tasbihEnglish: String = "",
    tasbihTranslitration: String = "",
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .padding(paddingValues)
            .testTag(TEST_TAG_TASBIH),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,

        ) {

        if (tasbihArabic.isNotBlank() && tasbihEnglish.isNotBlank() && tasbihTranslitration.isNotBlank() && tasbihId.isNotBlank()) {
            CustomCounter(
                paddingValues,
                tasbihId
            )
            LazyColumn(content = {
                item {
                    TasbihRow(
                        englishName = tasbihEnglish,
                        arabicName = tasbihArabic,
                        translationName = tasbihTranslitration,
                    )
                }
            })
        } else {
            Counter(
                paddingValues,
            )
        }
    }
}