package com.arshadshah.nimaz.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.R


val nunito = FontFamily(Font(R.font.nunito))
val quranFont = FontFamily(Font(R.font.quran_font))
val urduFont = FontFamily(Font(R.font.urdu))

// Set of Material typography styles to start with
val Typography = Typography(
		displayLarge = TextStyle(
				fontFamily = nunito ,
				fontWeight = FontWeight.Bold ,
				fontSize = 96.sp
								) ,
		displayMedium = TextStyle(
				fontFamily = nunito ,
				fontWeight = FontWeight.Bold ,
				fontSize = 60.sp
								 ) ,
		displaySmall = TextStyle(
				fontFamily = nunito ,
				fontWeight = FontWeight.Bold ,
				fontSize = 48.sp
								) ,
		headlineLarge = TextStyle(
				fontFamily = nunito ,
				fontWeight = FontWeight.Bold ,
				fontSize = 34.sp
								 ) ,
		headlineMedium = TextStyle(
				fontFamily = nunito ,
				fontWeight = FontWeight.Bold ,
				fontSize = 24.sp
								  ) ,
		headlineSmall = TextStyle(
				fontFamily = nunito ,
				fontWeight = FontWeight.Bold ,
				fontSize = 20.sp
								 ) ,
		bodyLarge = TextStyle(
				fontFamily = nunito ,
				fontWeight = FontWeight.Normal ,
				fontSize = 20.sp
							 ) ,
		bodyMedium = TextStyle(
				fontFamily = nunito ,
				fontWeight = FontWeight.Normal ,
				fontSize = 16.sp
							  ) ,
		bodySmall = TextStyle(
				fontFamily = nunito ,
				fontWeight = FontWeight.Normal ,
				fontSize = 14.sp
							 ) ,
		titleLarge = TextStyle(
				fontFamily = nunito ,
				fontWeight = FontWeight.Bold ,
				fontSize = 20.sp
							  ) ,
		titleMedium = TextStyle(
				fontFamily = nunito ,
				fontWeight = FontWeight.Bold ,
				fontSize = 16.sp
							   ) ,
		titleSmall = TextStyle(
				fontFamily = nunito ,
				fontWeight = FontWeight.Bold ,
				fontSize = 14.sp
							  ) ,
		labelLarge = TextStyle(
				fontFamily = nunito ,
				fontWeight = FontWeight.Bold ,
				fontSize = 14.sp
							  ) ,
		labelMedium = TextStyle(
				fontFamily = nunito ,
				fontWeight = FontWeight.Bold ,
				fontSize = 12.sp
							   ) ,
		labelSmall = TextStyle(
				fontFamily = nunito ,
				fontWeight = FontWeight.Bold ,
				fontSize = 10.sp
							  ) ,
						   )