package com.arshadshah.nimaz.activities

import android.content.Context
import android.os.Bundle
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.components.bLogic.tasbih.Counter
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.ui.theme.quranFont
import compose.icons.FeatherIcons
import compose.icons.feathericons.Minus

class Tasbih : ComponentActivity()
{

	@OptIn(ExperimentalMaterial3Api::class , ExperimentalMaterialApi::class)
	override fun onCreate(savedInstanceState : Bundle?)
	{
		val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
		super.onCreate(savedInstanceState)
		setContent {
			NimazTheme {
				val vibrationAllowed = remember { mutableStateOf(true) }
				val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
						bottomSheetState = BottomSheetState(BottomSheetValue.Collapsed)
																			   )

				val resources = LocalContext.current.resources
				//get the arrays
				val englishNames = resources.getStringArray(R.array.tasbeehTransliteration)
				val arabicNames = resources.getStringArray(R.array.tasbeeharabic)
				val translationNames = resources.getStringArray(R.array.tasbeehTranslation)
				BottomSheetScaffold(
						modifier = Modifier.shadow(16.dp , CardDefaults.elevatedShape).background(MaterialTheme.colorScheme.background) ,
						topBar = {
							TopAppBar(
									title = { androidx.compose.material3.Text(text = "Tasbih") } ,
									navigationIcon = {
										IconButton(onClick = {
											finish()
										}) {
											Icon(
													imageVector = Icons.Filled.ArrowBack ,
													contentDescription = "Back"
												)
										}
									} ,
									actions = {
										//vibration toggle button for tasbih to provide feedback
										IconButton(onClick = {
											vibrationAllowed.value = !vibrationAllowed.value
											//mute the vibration
											if (!vibrationAllowed.value)
											{
												vibrator.cancel()
											}
										}) {
											Icon(
													painter = if (vibrationAllowed.value) painterResource(
															id = R.drawable.vibration
																										 )
													else painterResource(
															id = R.drawable.close
																										 ) ,
													contentDescription = "Vibration"
												)
										}
									}
									 )
						} ,
						scaffoldState = bottomSheetScaffoldState ,
						sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp) ,
						sheetElevation = 8.dp ,
						sheetGesturesEnabled = true ,
						sheetContent = {
							//an icon to show where to pull the bottom sheet from
							Icon(
									imageVector = FeatherIcons.Minus ,
									contentDescription = "Pull to expand" ,
									modifier = Modifier
										.align(Alignment.CenterHorizontally)
										.size(48.dp) ,
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

						} ,
						sheetPeekHeight = 200.dp,
								   ) {it
					Counter(vibrator, it,vibrationAllowed)
				}
			}
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
					androidx.compose.material3.Text(
							text = arabicName ,
							style = MaterialTheme.typography.titleLarge ,
							fontSize = 24.sp ,
							fontFamily = quranFont ,
							modifier = Modifier
								.padding(4.dp)
								.fillMaxWidth() ,
							color = MaterialTheme.colorScheme.onSurface ,
						)
				}
				androidx.compose.material3.Text(
						modifier = Modifier
							.padding(4.dp)
							.fillMaxWidth() ,
						text = englishName ,
						style = MaterialTheme.typography.titleMedium ,
						color = MaterialTheme.colorScheme.onSurface ,
					)
				androidx.compose.material3.Text(
						modifier = Modifier
							.padding(4.dp)
							.fillMaxWidth() ,
						text = translationName ,
						style = MaterialTheme.typography.titleLarge ,
						color = MaterialTheme.colorScheme.onSurface ,
					)
			}
		}

	}
}