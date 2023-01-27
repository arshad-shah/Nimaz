package com.arshadshah.nimaz.ui.components.bLogic.tasbih

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.ui.theme.quranFont
import compose.icons.FeatherIcons
import compose.icons.feathericons.Menu
import compose.icons.feathericons.Minus


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BottomSheetForTasbihList(
	bottomSheetScaffoldState : BottomSheetScaffoldState ,
	paddingValues : PaddingValues
							){
	val resources = LocalContext.current.resources
	//get the arrays
	val englishNames = resources.getStringArray(R.array.tasbeehTransliteration)
	val arabicNames = resources.getStringArray(R.array.tasbeeharabic)
	val translationNames = resources.getStringArray(R.array.tasbeehTranslation)
	BottomSheetScaffold(
			modifier = Modifier.shadow(16.dp , CardDefaults.elevatedShape).padding(paddingValues),
			scaffoldState = bottomSheetScaffoldState ,
			sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp) ,
			sheetElevation = 8.dp ,
			sheetGesturesEnabled = true ,
			sheetBackgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.secondary ,
			sheetContent = {
				//an icon to show where to pull the bottom sheet from
				Icon(
						imageVector = FeatherIcons.Minus ,
						contentDescription = "Pull to expand" ,
						modifier = Modifier
							.align(Alignment.CenterHorizontally)
							.size(48.dp),
						tint = androidx.compose.material3.MaterialTheme.colorScheme.onSecondary
					)
				LazyColumn {
					items(englishNames.size) { index ->
						TasbihRow(
								englishNames[index] ,
								arabicNames[index] ,
								translationNames[index]
								 )
					}
				}

			} , sheetPeekHeight = 200.dp
					   ) {
		Counter()
	}
}

@Composable
fun TasbihRow(
	englishName : String ,
	arabicName : String ,
	translationName : String ,
			 ){
	ElevatedCard(
			modifier = Modifier
				.fillMaxWidth()
				.padding(8.dp) ,
				) {
		Column(
				horizontalAlignment = Alignment.Start ,
				verticalArrangement = Arrangement.Center
			  ) {
			CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
				Text(
						text = arabicName ,
						style = androidx.compose.material3.MaterialTheme.typography.titleLarge ,
						fontSize = 24.sp ,
						fontFamily = quranFont ,
						modifier = Modifier
							.padding(4.dp)
							.fillMaxWidth() ,
						color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface ,
					)
			}
			Text(
					modifier = Modifier
						.padding(4.dp)
						.fillMaxWidth() ,
					text = englishName ,
					style = androidx.compose.material3.MaterialTheme.typography.titleMedium ,
					color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface ,
				)
			Text(
					modifier = Modifier
						.padding(4.dp)
						.fillMaxWidth() ,
					text = translationName ,
					style = androidx.compose.material3.MaterialTheme.typography.titleLarge ,
					color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface ,
				)
		}
	}

}

//preview the tasbih list
@OptIn(ExperimentalMaterialApi::class)
@Preview
@Composable
fun TasbihListPreview(){
	val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
			bottomSheetState = BottomSheetState(BottomSheetValue.Collapsed)
																   )
	NimazTheme {
		BottomSheetForTasbihList(bottomSheetScaffoldState , PaddingValues(0.dp))
	}
}

@Preview(showBackground = true)
@Composable
fun TasbihRowPreview() {
	TasbihRow("English Name" , "Arabic Name" , "Translation Name")
}