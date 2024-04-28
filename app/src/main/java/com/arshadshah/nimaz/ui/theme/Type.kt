package com.arshadshah.nimaz.ui.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.R

@OptIn(ExperimentalTextApi::class)
val nunito = FontFamily(
    Font(
        R.font.nunito,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(700),
        )
    ),
)
val quranFont = FontFamily(Font(R.font.quran_font))
val utmaniQuranFont = FontFamily(Font(R.font.uthman))
val hidayat = FontFamily(Font(R.font.noorehidayat))
val amiri = FontFamily(Font(R.font.amiri))
val almajeed = FontFamily(Font(R.font.almajeed))

val urduFont = FontFamily(Font(R.font.urdu))

val englishQuranTranslation = FontFamily(Font(R.font.english_translation))

// Set of Material typography styles to start with these are the default values
val TypographyMain = Typography(
    displayLarge = TextStyle(
        fontFamily = nunito,
        fontSize = 65.sp
    ),
    displayMedium = TextStyle(
        fontFamily = nunito,
        fontSize = 54.sp
    ),
    displaySmall = TextStyle(
        fontFamily = nunito,
        fontSize = 42.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = nunito,
        fontSize = 30.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = nunito,
        fontSize = 24.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = nunito,
        fontSize = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = nunito,
        fontSize = 18.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = nunito,
        fontSize = 16.sp
    ),
    bodySmall = TextStyle(
        fontFamily = nunito,
        fontSize = 14.sp
    ),
    titleLarge = TextStyle(
        fontFamily = nunito,
        fontSize = 22.sp
    ),
    titleMedium = TextStyle(
        fontFamily = nunito,
        fontSize = 18.sp
    ),
    titleSmall = TextStyle(
        fontFamily = nunito,
        fontSize = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = nunito,
        fontSize = 16.sp
    ),
    labelMedium = TextStyle(
        fontFamily = nunito,
        fontSize = 14.sp
    ),
    labelSmall = TextStyle(
        fontFamily = nunito,
        fontSize = 12.sp
    ),
)

// Set of Material typography styles to start with these are the values for small screens
val TypographySmall = Typography(
    displayLarge = TextStyle(
        fontFamily = nunito,
        fontSize = 60.sp
    ),
    displayMedium = TextStyle(
        fontFamily = nunito,
        fontSize = 44.sp
    ),
    displaySmall = TextStyle(
        fontFamily = nunito,
        fontSize = 32.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = nunito,
        fontSize = 20.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = nunito,
        fontSize = 18.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = nunito,
        fontSize = 14.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = nunito,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = nunito,
        fontSize = 14.sp
    ),
    bodySmall = TextStyle(
        fontFamily = nunito,
        fontSize = 12.sp
    ),
    titleLarge = TextStyle(
        fontFamily = nunito,
        fontSize = 20.sp
    ),
    titleMedium = TextStyle(
        fontFamily = nunito,
        fontSize = 18.sp
    ),
    titleSmall = TextStyle(
        fontFamily = nunito,
        fontSize = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = nunito,
        fontSize = 14.sp
    ),
    labelMedium = TextStyle(
        fontFamily = nunito,
        fontSize = 12.sp
    ),
    labelSmall = TextStyle(
        fontFamily = nunito,
        fontSize = 10.sp
    ),
)

@Preview(showBackground = true, name = "Typography Main", device = "id:pixel_8_pro")
@Composable
//a preview of the typography showing the different text styles
fun TypographyPreview() {
    Column {
        TypographyMain.apply {
            //display styles
            Text("Display Large", style = displayLarge)
            Text("Display Medium", style = displayMedium)
            Text("Display Small", style = displaySmall)
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            //headline styles
            Text("Headline Large", style = headlineLarge)
            Text("Headline Medium", style = headlineMedium)
            Text("Headline Small", style = headlineSmall)
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            //body styles
            Text("Body Large", style = bodyLarge)
            Text("Body Medium", style = bodyMedium)
            Text("Body Small", style = bodySmall)
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            //title styles
            Text("Title Large", style = titleLarge)
            Text("Title Medium", style = titleMedium)
            Text("Title Small", style = titleSmall)
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            //label styles
            Text("Label Large", style = labelLarge)
            Text("Label Medium", style = labelMedium)
            Text("Label Small", style = labelSmall)
        }
    }
}

//typography for small screens
@Preview(showBackground = true, name = "Typography Small", device = "id:pixel_8_pro")
@Composable
fun TypographySmallPreview() {
    Column {
        TypographySmall.apply {
            //display styles
            Text("Display Large", style = displayLarge)
            Text("Display Medium", style = displayMedium)
            Text("Display Small", style = displaySmall)
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            //headline styles
            Text("Headline Large", style = headlineLarge)
            Text("Headline Medium", style = headlineMedium)
            Text("Headline Small", style = headlineSmall)
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            //body styles
            Text("Body Large", style = bodyLarge)
            Text("Body Medium", style = bodyMedium)
            Text("Body Small", style = bodySmall)
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            //title styles
            Text("Title Large", style = titleLarge)
            Text("Title Medium", style = titleMedium)
            Text("Title Small", style = titleSmall)
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            //label styles
            Text("Label Large", style = labelLarge)
            Text("Label Medium", style = labelMedium)
            Text("Label Small", style = labelSmall)
        }
    }
}