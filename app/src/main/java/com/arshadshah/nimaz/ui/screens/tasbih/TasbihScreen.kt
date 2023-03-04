package com.arshadshah.nimaz.ui.screens.tasbih

import android.os.Vibrator
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_TASBIH
import com.arshadshah.nimaz.ui.components.bLogic.tasbih.Counter
import com.arshadshah.nimaz.ui.components.bLogic.tasbih.CustomCounter
import com.arshadshah.nimaz.ui.components.bLogic.tasbih.TasbihRow

@Composable
fun TasbihScreen(
	paddingValues : PaddingValues ,
	showResetDialog : MutableState<Boolean> ,
	vibrator : Vibrator ,
	vibrationAllowed : MutableState<Boolean> ,
	rOrl : MutableState<Int> ,
	tasbihId : String = "" ,
	tasbihArabic : String = "" ,
	tasbihEnglish : String = "" ,
	tasbihTranslitration : String = "" ,
				)
{
	val context = LocalContext.current

	//reset
	val reset = remember { mutableStateOf(false) }

	Column(
			modifier = Modifier
				.padding(paddingValues)
				.testTag(TEST_TAG_TASBIH) ,
			horizontalAlignment = Alignment.CenterHorizontally ,
			verticalArrangement = Arrangement.Top ,

			) {

		if (tasbihArabic.isNotBlank() && tasbihEnglish.isNotBlank() && tasbihTranslitration.isNotBlank() && tasbihId.isNotBlank())
		{
			CustomCounter(
					vibrator ,
					paddingValues ,
					vibrationAllowed ,
					reset ,
					showResetDialog ,
					rOrl ,
					tasbihId
						 )
			LazyColumn(content = {
				item {
					TasbihRow(
							englishName = tasbihEnglish ,
							arabicName = tasbihArabic ,
							translationName = tasbihTranslitration ,
							 )
				}
			})
		} else
		{
			Counter(
					vibrator ,
					paddingValues ,
					vibrationAllowed ,
					reset ,
					showResetDialog ,
					rOrl ,
				   )
		}
	}
}