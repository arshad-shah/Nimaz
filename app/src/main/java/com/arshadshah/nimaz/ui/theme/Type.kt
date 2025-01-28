package com.arshadshah.nimaz.ui.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.R

// Google Fonts Provider
val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

// Google Fonts
val nunitoFont = GoogleFont("Nunito Sans")
val robotoFlexFont = GoogleFont("Roboto Flex")
val jetbrainsMono = GoogleFont("JetBrains Mono")
val ibmPlexSerifFont = GoogleFont("IBM Plex Serif")


// Google Font Families
val nunitoFamily = FontFamily(
    androidx.compose.ui.text.googlefonts.Font(
        googleFont = nunitoFont,
        fontProvider = provider,
        weight = FontWeight.SemiBold
    ),
    androidx.compose.ui.text.googlefonts.Font(
        googleFont = nunitoFont,
        fontProvider = provider,
        weight = FontWeight.Bold
    )
)

val bodyFontFamily = FontFamily(
    androidx.compose.ui.text.googlefonts.Font(
        googleFont = robotoFlexFont,
        fontProvider = provider,
        weight = FontWeight.Normal
    ),
    androidx.compose.ui.text.googlefonts.Font(
        googleFont = robotoFlexFont,
        fontProvider = provider,
        weight = FontWeight.Medium
    ),
    androidx.compose.ui.text.googlefonts.Font(
        googleFont = robotoFlexFont,
        fontProvider = provider,
        weight = FontWeight.Bold
    )
)

val monoFontFamily = FontFamily(
    androidx.compose.ui.text.googlefonts.Font(
        googleFont = jetbrainsMono,
        fontProvider = provider,
        weight = FontWeight.Normal
    ),
    androidx.compose.ui.text.googlefonts.Font(
        googleFont = jetbrainsMono,
        fontProvider = provider,
        weight = FontWeight.Medium
    )
)

val amiriGoogleFont = GoogleFont("Amiri")

// Add this with your other FontFamily declarations
val amiri = FontFamily(
    androidx.compose.ui.text.googlefonts.Font(
        googleFont = amiriGoogleFont,
        fontProvider = provider,
        weight = FontWeight.Normal
    ),
    androidx.compose.ui.text.googlefonts.Font(
        googleFont = amiriGoogleFont,
        fontProvider = provider,
        weight = FontWeight.Bold
    ),
    androidx.compose.ui.text.googlefonts.Font(
        googleFont = amiriGoogleFont,
        fontProvider = provider,
        weight = FontWeight.Medium
    )
)

// Existing Special Fonts
val quranFont = FontFamily(Font(R.font.quran_font))
val utmaniQuranFont = FontFamily(Font(R.font.uthman))
val hidayat = FontFamily(Font(R.font.noorehidayat))
val almajeed = FontFamily(Font(R.font.almajeed))
val urduFont = FontFamily(Font(R.font.urdu))
val englishQuranTranslation = FontFamily(
    androidx.compose.ui.text.googlefonts.Font(
        googleFont = ibmPlexSerifFont,
        fontProvider = provider,
        weight = FontWeight.Normal
    ),
    androidx.compose.ui.text.googlefonts.Font(
        googleFont = ibmPlexSerifFont,
        fontProvider = provider,
        weight = FontWeight.Medium
    ),
    androidx.compose.ui.text.googlefonts.Font(
        googleFont = ibmPlexSerifFont,
        fontProvider = provider,
        weight = FontWeight.SemiBold
    )
)


// Material Typography
val TypographyMain = Typography(
    // Display styles
    displayLarge = TextStyle(
        fontFamily = nunitoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 65.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = nunitoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 54.sp
    ),
    displaySmall = TextStyle(
        fontFamily = nunitoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 42.sp
    ),

    // Headline styles
    headlineLarge = TextStyle(
        fontFamily = nunitoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = nunitoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = nunitoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp
    ),

    // Body styles
    bodyLarge = TextStyle(
        fontFamily = bodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = bodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = bodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),

    // Title styles
    titleLarge = TextStyle(
        fontFamily = nunitoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp
    ),
    titleMedium = TextStyle(
        fontFamily = nunitoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp
    ),
    titleSmall = TextStyle(
        fontFamily = nunitoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    ),

    // Label styles
    labelLarge = TextStyle(
        fontFamily = monoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelMedium = TextStyle(
        fontFamily = monoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = bodyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp
    )
)

// Typography for small screens
val TypographySmall = Typography(
    // Display styles
    displayLarge = TextStyle(
        fontFamily = nunitoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 60.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = nunitoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 44.sp
    ),
    displaySmall = TextStyle(
        fontFamily = nunitoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 32.sp
    ),

    // Headline styles
    headlineLarge = TextStyle(
        fontFamily = nunitoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = nunitoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = nunitoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    ),

    // Body styles
    bodyLarge = TextStyle(
        fontFamily = bodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = bodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = bodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),

    // Title styles
    titleLarge = TextStyle(
        fontFamily = nunitoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp
    ),
    titleMedium = TextStyle(
        fontFamily = nunitoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp
    ),
    titleSmall = TextStyle(
        fontFamily = nunitoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    ),

    // Label styles
    labelLarge = TextStyle(
        fontFamily = monoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.5.sp
    ),
    labelMedium = TextStyle(
        fontFamily = monoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = bodyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 0.5.sp
    )
)

@Preview(showBackground = true, name = "Typography Main", device = "id:pixel_8_pro")
@Composable
fun TypographyPreview() {
    Column {
        TypographyMain.apply {
            Text("Display Large", style = displayLarge)
            Text("Display Medium", style = displayMedium)
            Text("Display Small", style = displaySmall)
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Headline Large", style = headlineLarge)
            Text("Headline Medium", style = headlineMedium)
            Text("Headline Small", style = headlineSmall)
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Body Large", style = bodyLarge)
            Text("Body Medium", style = bodyMedium)
            Text("Body Small", style = bodySmall)
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Title Large", style = titleLarge)
            Text("Title Medium", style = titleMedium)
            Text("Title Small", style = titleSmall)
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Label Large", style = labelLarge)
            Text("Label Medium", style = labelMedium)
            Text("Label Small", style = labelSmall)
        }
    }
}

@Preview(showBackground = true, name = "Typography Small", device = "id:pixel_8_pro")
@Composable
fun TypographySmallPreview() {
    Column {
        TypographySmall.apply {
            Text("Display Large", style = displayLarge)
            Text("Display Medium", style = displayMedium)
            Text("Display Small", style = displaySmall)
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Headline Large", style = headlineLarge)
            Text("Headline Medium", style = headlineMedium)
            Text("Headline Small", style = headlineSmall)
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Body Large", style = bodyLarge)
            Text("Body Medium", style = bodyMedium)
            Text("Body Small", style = bodySmall)
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Title Large", style = titleLarge)
            Text("Title Medium", style = titleMedium)
            Text("Title Small", style = titleSmall)
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Label Large", style = labelLarge)
            Text("Label Medium", style = labelMedium)
            Text("Label Small", style = labelSmall)
        }
    }
}